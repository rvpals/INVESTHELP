const db = require('../db');
const yahoo = require('./yahoo-finance');

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
        name = CASE WHEN ? != '' THEN ? ELSE name END
    WHERE ticker = ?
  `);

  const results = [];
  for (const pos of positions) {
    currentTicker = pos.ticker;
    try {
      const q = await yahoo.fetchQuote(pos.ticker);
      const value = q.price * (pos.quantity || 0);
      const dayGL = (q.price - q.previousClose) * (pos.quantity || 0);
      update.run(q.price, dayGL, value, q.dayHigh, q.dayLow, q.shortName || '', q.shortName || '', pos.ticker);

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

function getStatus() {
  return { isRefreshing, currentTicker, progress: refreshProgress };
}

module.exports = { startAutoRefresh, stopAutoRefresh, initFromSettings, refreshAll, getStatus, recordChangeHistory };
