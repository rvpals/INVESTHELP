const db = require('../db');
const yahoo = require('./yahoo-finance');
const fs = require('fs');
const path = require('path');
const { generateSnapshot } = require('./snapshot');

let refreshInterval = null;
let isRefreshing = false;
let currentTicker = '';
let refreshProgress = { done: 0, total: 0 };

function startAutoRefresh(intervalMs) {
  stopAutoRefresh();
  refreshInterval = setInterval(() => refreshAll(), intervalMs);
  console.log(`Auto-refresh started: every ${intervalMs / 1000}s`);
}

function stopAutoRefresh() {
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = null;
    console.log('Auto-refresh stopped');
  }
}

function initFromSettings() {
  const enabled = db.prepare("SELECT value FROM settings WHERE key = 'auto_refresh_enabled'").get();
  if (enabled?.value === 'true') {
    const interval = db.prepare("SELECT value FROM settings WHERE key = 'auto_refresh_interval'").get();
    const ms = { '5m': 300000, '30m': 1800000, '1h': 3600000, '5h': 18000000, 'market_close': 86400000 };
    startAutoRefresh(ms[interval?.value] || 1800000);
  }
}

async function refreshAll() {
  if (isRefreshing) return { ok: false, message: 'Already refreshing' };
  isRefreshing = true;
  currentTicker = '';
  const positions = db.prepare('SELECT ticker, quantity FROM investment_positions').all();
  refreshProgress = { done: 0, total: positions.length };

  const update = db.prepare(`
    UPDATE investment_positions
    SET currentPrice = ?, dayGainLoss = ?, value = ?, dayHigh = ?, dayLow = ?,
        name = CASE WHEN ? != '' THEN ? ELSE name END,
        dividendRate = ?
    WHERE ticker = ?
  `);

  const results = [];
  for (const pos of positions) {
    currentTicker = pos.ticker;
    try {
      const q = await yahoo.fetchQuote(pos.ticker);
      const value = q.price * (pos.quantity || 0);
      const dayGL = (q.price - q.previousClose) * (pos.quantity || 0);
      update.run(q.price, dayGL, value, q.dayHigh, q.dayLow, q.shortName || '', q.shortName || '', q.dividendRate || 0, pos.ticker);

      // Fetch logo if missing
      const existing = db.prepare('SELECT logo FROM investment_positions WHERE ticker = ?').get(pos.ticker);
      if (!existing?.logo) {
        const logo = await yahoo.fetchLogo(pos.ticker);
        if (logo) db.prepare('UPDATE investment_positions SET logo = ? WHERE ticker = ?').run(logo, pos.ticker);
      }

      results.push({ ticker: pos.ticker, price: q.price, ok: true });
    } catch (err) {
      results.push({ ticker: pos.ticker, error: err.message, ok: false });
    }
    refreshProgress.done++;
  }

  // Record change history if enabled
  const autoHistory = db.prepare("SELECT value FROM settings WHERE key = 'auto_update_change_history'").get();
  if (autoHistory?.value === 'true') {
    recordChangeHistory();
  }

  // Auto backup if enabled
  const autoBackup = db.prepare("SELECT value FROM settings WHERE key = 'auto_backup_on_refresh'").get();
  if (autoBackup?.value === 'true') {
    performAutoBackup();
  }

  // Generate static snapshot
  generateSnapshot();

  isRefreshing = false;
  currentTicker = '';
  return { ok: true, results };
}

function recordChangeHistory() {
  const all = db.prepare('SELECT type, value, dayGainLoss FROM investment_positions').all();
  const etfValue = all.filter(r => r.type === 'ETF').reduce((s, r) => s + r.value, 0);
  const stockValue = all.filter(r => r.type === 'Stock').reduce((s, r) => s + r.value, 0);
  const totalValue = all.reduce((s, r) => s + r.value, 0);
  const dailyEtf = all.filter(r => r.type === 'ETF').reduce((s, r) => s + r.dayGainLoss, 0);
  const dailyStock = all.filter(r => r.type === 'Stock').reduce((s, r) => s + r.dayGainLoss, 0);
  const dailyTotal = all.reduce((s, r) => s + r.dayGainLoss, 0);
  const today = Math.floor(Date.now() / 86400000);

  db.prepare(`
    INSERT INTO change_history (date, etfValue, stockValue, totalValue, dailyChangeEtf, dailyChangeStock, dailyChangeTotal)
    VALUES (?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(date) DO UPDATE SET
      etfValue=excluded.etfValue, stockValue=excluded.stockValue, totalValue=excluded.totalValue,
      dailyChangeEtf=excluded.dailyChangeEtf, dailyChangeStock=excluded.dailyChangeStock, dailyChangeTotal=excluded.dailyChangeTotal
  `).run(today, etfValue, stockValue, totalValue, dailyEtf, dailyStock, dailyTotal);
}

function performAutoBackup() {
  try {
    const BACKUP_DIR = path.join(__dirname, '..', '..', 'backups');
    if (!fs.existsSync(BACKUP_DIR)) fs.mkdirSync(BACKUP_DIR, { recursive: true });

    const accounts = db.prepare('SELECT * FROM investment_accounts').all();
    const positions = db.prepare('SELECT ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow, dividendRate FROM investment_positions').all();
    const transactions = db.prepare('SELECT * FROM investment_transactions').all();
    const performanceRecords = db.prepare('SELECT * FROM account_performance').all();
    const watchLists = db.prepare('SELECT * FROM watch_lists').all();
    const watchListItems = db.prepare('SELECT * FROM watch_list_items').all();
    const changeHistory = db.prepare('SELECT * FROM change_history').all();
    const definitions = db.prepare('SELECT * FROM definitions').all();
    const sqlLibrary = db.prepare('SELECT * FROM sql_library').all();
    const aiLibrary = db.prepare('SELECT * FROM ai_library').all();

    const data = {
      version: 5,
      accounts: accounts.map(a => ({ id: a.id, name: a.name, description: a.description, initialValue: a.initialValue })),
      items: positions.map(p => ({ ticker: p.ticker, name: p.name, type: p.type, currentPrice: p.currentPrice, quantity: p.quantity, dayGainLoss: p.dayGainLoss, value: p.value, dayHigh: p.dayHigh, dayLow: p.dayLow, dividendRate: p.dividendRate || 0 })),
      transactions: transactions.map(t => ({ id: t.id, dateEpochDay: t.date, timeSecondOfDay: t.time, action: t.action, ticker: t.ticker, numberOfShares: t.numberOfShares, pricePerShare: t.pricePerShare, totalAmount: t.totalAmount, note: t.note })),
      performanceRecords: performanceRecords.map(p => ({ id: p.id, accountId: p.accountId, totalValue: p.totalValue, dateEpochDay: p.date, note: p.note })),
      watchLists: watchLists.map(w => ({ id: w.id, name: w.name })),
      watchListItems: watchListItems.map(i => ({ id: i.id, watchListId: i.watchListId, ticker: i.ticker, shares: i.shares, priceWhenAdded: i.priceWhenAdded, addedDateEpochDay: i.addedDate, reminderDateTimeEpochMs: i.reminderDateTime, reminderMessage: i.reminderMessage })),
      changeHistory: changeHistory.map(c => ({ id: c.id, dateEpochDay: c.date, etfValue: c.etfValue, stockValue: c.stockValue, totalValue: c.totalValue, dailyChangeEtf: c.dailyChangeEtf, dailyChangeStock: c.dailyChangeStock, dailyChangeTotal: c.dailyChangeTotal })),
      definitions: definitions.map(d => ({ id: d.id, name: d.name, description: d.description })),
      sqlLibrary: sqlLibrary.map(s => ({ id: s.id, name: s.name, description: s.description, category: s.category, sql: s.sql })),
      aiLibrary: aiLibrary.map(a => ({ id: a.id, name: a.name, description: a.description, promptText: a.promptText })),
      exportedAt: new Date().toISOString()
    };

    const now = new Date();
    const stamp = now.toISOString().replace(/[:.]/g, '-').slice(0, 19);
    const filename = `invest_help_backup_${stamp}.json`;
    fs.writeFileSync(path.join(BACKUP_DIR, filename), JSON.stringify(data, null, 2));

    // Prune old backups beyond keep count
    const keepCount = parseInt(db.prepare("SELECT value FROM settings WHERE key = 'auto_backup_keep_count'").get()?.value || '10');
    const files = fs.readdirSync(BACKUP_DIR)
      .filter(f => f.startsWith('invest_help_backup_') && f.endsWith('.json'))
      .map(f => ({ name: f, mtime: fs.statSync(path.join(BACKUP_DIR, f)).mtime }))
      .sort((a, b) => b.mtime - a.mtime);
    if (files.length > keepCount) {
      for (const old of files.slice(keepCount)) {
        fs.unlinkSync(path.join(BACKUP_DIR, old.name));
      }
    }

    console.log(`Auto-backup saved: ${filename} (keeping ${keepCount})`);
  } catch (err) {
    console.error('Auto-backup failed:', err.message);
  }
}

function getStatus() {
  return { isRefreshing, currentTicker, progress: refreshProgress };
}

module.exports = { startAutoRefresh, stopAutoRefresh, initFromSettings, refreshAll, getStatus, recordChangeHistory };
