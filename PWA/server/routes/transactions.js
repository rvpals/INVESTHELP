const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/', (req, res) => {
  const { ticker } = req.query;
  if (ticker) {
    res.json(db.prepare('SELECT * FROM investment_transactions WHERE ticker = ? ORDER BY date DESC, time DESC').all(ticker));
  } else {
    res.json(db.prepare('SELECT * FROM investment_transactions ORDER BY date DESC, time DESC').all());
  }
});

router.get('/stats/:ticker', (req, res) => {
  const { startDate, endDate, action } = req.query;
  let sql = 'SELECT * FROM investment_transactions WHERE ticker = ?';
  const params = [req.params.ticker];
  if (startDate) { sql += ' AND date >= ?'; params.push(Number(startDate)); }
  if (endDate) { sql += ' AND date <= ?'; params.push(Number(endDate)); }
  if (action) { sql += ' AND action = ?'; params.push(action); }
  const rows = db.prepare(sql).all(...params);
  const prices = rows.map(r => r.pricePerShare);
  res.json({
    count: rows.length,
    avg: prices.length ? prices.reduce((a, b) => a + b, 0) / prices.length : 0,
    max: prices.length ? Math.max(...prices) : 0,
    min: prices.length ? Math.min(...prices) : 0,
    transactions: rows,
  });
});

router.get('/:id', (req, res) => {
  const row = db.prepare('SELECT * FROM investment_transactions WHERE id = ?').get(req.params.id);
  if (!row) return res.status(404).json({ error: 'Not found' });
  res.json(row);
});

router.post('/', (req, res) => {
  const { date, time, action, ticker, numberOfShares, pricePerShare, totalAmount, note } = req.body;
  const r = db.prepare(`INSERT INTO investment_transactions (date, time, action, ticker, numberOfShares, pricePerShare, totalAmount, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`)
    .run(date, time ?? null, action || 'Buy', ticker, numberOfShares || 0, pricePerShare || 0, totalAmount || 0, note || '');
  res.json({ id: r.lastInsertRowid });
});

router.post('/if-not-exists', (req, res) => {
  const { date, time, action, ticker, numberOfShares, pricePerShare, totalAmount, note } = req.body;
  const r = db.prepare(`INSERT OR IGNORE INTO investment_transactions (date, time, action, ticker, numberOfShares, pricePerShare, totalAmount, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`)
    .run(date, time ?? null, action || 'Buy', ticker, numberOfShares || 0, pricePerShare || 0, totalAmount || 0, note || '');
  res.json({ id: r.lastInsertRowid, inserted: r.changes > 0 });
});

router.put('/:id', (req, res) => {
  const { date, time, action, ticker, numberOfShares, pricePerShare, totalAmount, note } = req.body;
  db.prepare(`UPDATE investment_transactions SET date=?, time=?, action=?, ticker=?, numberOfShares=?, pricePerShare=?, totalAmount=?, note=? WHERE id=?`)
    .run(date, time ?? null, action, ticker, numberOfShares || 0, pricePerShare || 0, totalAmount || 0, note || '', req.params.id);
  res.json({ ok: true });
});

router.delete('/:id', (req, res) => {
  db.prepare('DELETE FROM investment_transactions WHERE id = ?').run(req.params.id);
  res.json({ ok: true });
});

router.post('/bulk-delete', (req, res) => {
  const { ids } = req.body;
  if (!ids?.length) return res.json({ ok: true, deleted: 0 });
  const placeholders = ids.map(() => '?').join(',');
  const r = db.prepare(`DELETE FROM investment_transactions WHERE id IN (${placeholders})`).run(...ids);
  res.json({ ok: true, deleted: r.changes });
});

module.exports = router;
