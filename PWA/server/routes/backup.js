const express = require('express');
const router = express.Router();
const db = require('../db');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 50 * 1024 * 1024 } });

const BACKUP_DIR = path.join(__dirname, '..', '..', 'backups');

router.get('/export', (req, res) => {
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

  // Map to Android-compatible field names
  const data = {
    version: 5,
    accounts: accounts.map(a => ({ id: a.id, name: a.name, description: a.description, initialValue: a.initialValue })),
    items: positions.map(p => ({
      ticker: p.ticker, name: p.name, type: p.type, currentPrice: p.currentPrice,
      quantity: p.quantity, dayGainLoss: p.dayGainLoss, value: p.value, dayHigh: p.dayHigh, dayLow: p.dayLow,
      dividendRate: p.dividendRate || 0
    })),
    transactions: transactions.map(t => ({
      id: t.id, dateEpochDay: t.date, timeSecondOfDay: t.time, action: t.action,
      ticker: t.ticker, numberOfShares: t.numberOfShares, pricePerShare: t.pricePerShare,
      totalAmount: t.totalAmount, note: t.note
    })),
    performanceRecords: performanceRecords.map(p => ({
      id: p.id, accountId: p.accountId, totalValue: p.totalValue, dateEpochDay: p.date, note: p.note
    })),
    watchLists: watchLists.map(w => ({ id: w.id, name: w.name })),
    watchListItems: watchListItems.map(i => ({
      id: i.id, watchListId: i.watchListId, ticker: i.ticker, shares: i.shares,
      priceWhenAdded: i.priceWhenAdded, addedDateEpochDay: i.addedDate,
      reminderDateTimeEpochMs: i.reminderDateTime, reminderMessage: i.reminderMessage
    })),
    changeHistory: changeHistory.map(c => ({
      id: c.id, dateEpochDay: c.date, etfValue: c.etfValue, stockValue: c.stockValue,
      totalValue: c.totalValue, dailyChangeEtf: c.dailyChangeEtf,
      dailyChangeStock: c.dailyChangeStock, dailyChangeTotal: c.dailyChangeTotal
    })),
    definitions: definitions.map(d => ({ id: d.id, name: d.name, description: d.description })),
    sqlLibrary: sqlLibrary.map(s => ({ id: s.id, name: s.name, description: s.description, category: s.category, sql: s.sql })),
    aiLibrary: aiLibrary.map(a => ({ id: a.id, name: a.name, description: a.description, promptText: a.promptText })),
    exportedAt: new Date().toISOString()
  };

  const filename = `invest_help_backup_${new Date().toISOString().slice(0, 10)}.json`;
  res.set('Content-Disposition', `attachment; filename="${filename}"`);
  res.set('Content-Type', 'application/json');
  res.send(JSON.stringify(data, null, 2));
});

router.post('/import', upload.single('file'), (req, res) => {
  try {
    const text = req.file ? req.file.buffer.toString('utf8') : req.body.data;
    if (!text) return res.status(400).json({ error: 'No file provided' });
    const data = JSON.parse(text);

    const tx = db.transaction(() => {
      // Clear all tables in reverse dependency order
      db.prepare('DELETE FROM ai_library').run();
      db.prepare('DELETE FROM sql_library').run();
      db.prepare('DELETE FROM definitions').run();
      db.prepare('DELETE FROM change_history').run();
      db.prepare('DELETE FROM watch_list_items').run();
      db.prepare('DELETE FROM watch_lists').run();
      db.prepare('DELETE FROM account_performance').run();
      db.prepare('DELETE FROM investment_transactions').run();
      db.prepare('DELETE FROM investment_positions').run();
      db.prepare('DELETE FROM investment_accounts').run();

      // Accounts
      const insAcct = db.prepare('INSERT INTO investment_accounts (id, name, description, initialValue) VALUES (?, ?, ?, ?)');
      for (const a of (data.accounts || [])) {
        insAcct.run(a.id, a.name, a.description || '', a.initialValue || 0);
      }

      // Positions (v1 uses numShares, v2+ uses quantity; field may be "items" or "positions")
      const insPos = db.prepare('INSERT INTO investment_positions (ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow, dividendRate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)');
      for (const p of (data.positions || data.items || [])) {
        const ticker = (p.ticker || '').trim() || 'UNKNOWN';
        insPos.run(ticker, p.name || '', p.type || 'Stock', p.currentPrice || 0,
          p.quantity || p.numShares || 0, p.dayGainLoss || 0, p.value || 0, p.dayHigh || 0, p.dayLow || 0, p.dividendRate || 0);
      }

      // Transactions (v5 uses dateEpochDay; v4 PWA-native uses date directly)
      const insTx = db.prepare('INSERT OR IGNORE INTO investment_transactions (date, time, action, ticker, numberOfShares, pricePerShare, totalAmount, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
      for (const t of (data.transactions || [])) {
        const date = t.dateEpochDay ?? t.date;
        const time = t.timeSecondOfDay ?? t.time ?? null;
        insTx.run(date, time, t.action || 'Buy', t.ticker, t.numberOfShares || 0, t.pricePerShare || 0, t.totalAmount || 0, t.note || '');
      }

      // Performance records (v5)
      if (data.performanceRecords?.length) {
        const insPerf = db.prepare('INSERT OR IGNORE INTO account_performance (id, accountId, totalValue, date, note) VALUES (?, ?, ?, ?, ?)');
        for (const p of data.performanceRecords) {
          insPerf.run(p.id, p.accountId, p.totalValue || 0, p.dateEpochDay ?? p.date, p.note || '');
        }
      }

      // Watch lists (v5)
      if (data.watchLists?.length) {
        const insWL = db.prepare('INSERT INTO watch_lists (id, name) VALUES (?, ?)');
        for (const w of data.watchLists) {
          insWL.run(w.id, w.name);
        }
      }

      // Watch list items (v5)
      if (data.watchListItems?.length) {
        const insWLI = db.prepare('INSERT INTO watch_list_items (id, watchListId, ticker, shares, priceWhenAdded, addedDate, reminderDateTime, reminderMessage) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
        for (const i of data.watchListItems) {
          insWLI.run(i.id, i.watchListId, i.ticker, i.shares || 0, i.priceWhenAdded || 0,
            i.addedDateEpochDay ?? i.addedDate, i.reminderDateTimeEpochMs ?? i.reminderDateTime ?? null, i.reminderMessage || '');
        }
      }

      // Change history (v5)
      if (data.changeHistory?.length) {
        const insCH = db.prepare('INSERT OR REPLACE INTO change_history (id, date, etfValue, stockValue, totalValue, dailyChangeEtf, dailyChangeStock, dailyChangeTotal) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
        for (const c of data.changeHistory) {
          insCH.run(c.id, c.dateEpochDay ?? c.date, c.etfValue || 0, c.stockValue || 0, c.totalValue || 0,
            c.dailyChangeEtf || 0, c.dailyChangeStock || 0, c.dailyChangeTotal || 0);
        }
      }

      // Definitions (v5)
      if (data.definitions?.length) {
        const insDef = db.prepare('INSERT INTO definitions (id, name, description) VALUES (?, ?, ?)');
        for (const d of data.definitions) {
          insDef.run(d.id, d.name, d.description || '');
        }
      }

      // SQL library (v5)
      if (data.sqlLibrary?.length) {
        const insSQL = db.prepare('INSERT INTO sql_library (id, name, description, category, sql) VALUES (?, ?, ?, ?, ?)');
        for (const s of data.sqlLibrary) {
          insSQL.run(s.id, s.name, s.description || '', s.category || '', s.sql || '');
        }
      }

      // AI library (v5)
      if (data.aiLibrary?.length) {
        const insAI = db.prepare('INSERT INTO ai_library (id, name, description, promptText) VALUES (?, ?, ?, ?)');
        for (const a of data.aiLibrary) {
          insAI.run(a.id, a.name, a.description || '', a.promptText || '');
        }
      }
    });
    tx();

    const counts = {
      accounts: (data.accounts || []).length,
      positions: (data.positions || data.items || []).length,
      transactions: (data.transactions || []).length,
      performanceRecords: (data.performanceRecords || []).length,
      watchLists: (data.watchLists || []).length,
      watchListItems: (data.watchListItems || []).length,
      changeHistory: (data.changeHistory || []).length,
      definitions: (data.definitions || []).length,
      sqlLibrary: (data.sqlLibrary || []).length,
      aiLibrary: (data.aiLibrary || []).length,
    };
    res.json({ ok: true, ...counts });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/list', (req, res) => {
  if (!fs.existsSync(BACKUP_DIR)) return res.json([]);
  const files = fs.readdirSync(BACKUP_DIR)
    .filter(f => f.endsWith('.json'))
    .map(f => ({ name: f, size: fs.statSync(path.join(BACKUP_DIR, f)).size, modified: fs.statSync(path.join(BACKUP_DIR, f)).mtime }))
    .sort((a, b) => new Date(b.modified) - new Date(a.modified));
  res.json(files);
});

router.delete('/:filename', (req, res) => {
  const filepath = path.join(BACKUP_DIR, req.params.filename);
  if (fs.existsSync(filepath)) fs.unlinkSync(filepath);
  res.json({ ok: true });
});

module.exports = router;
