const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/', (req, res) => {
  const { accountId } = req.query;
  if (accountId) {
    res.json(db.prepare('SELECT * FROM account_performance WHERE accountId = ? ORDER BY date DESC').all(accountId));
  } else {
    res.json(db.prepare('SELECT * FROM account_performance ORDER BY date DESC').all());
  }
});

router.post('/', (req, res) => {
  const { accountId, totalValue, date, note } = req.body;
  const existing = db.prepare('SELECT COUNT(*) as n FROM account_performance WHERE accountId = ? AND date = ?').get(accountId, date);
  if (existing.n > 0) return res.status(409).json({ error: 'Record already exists for this account and date' });
  const r = db.prepare('INSERT INTO account_performance (accountId, totalValue, date, note) VALUES (?, ?, ?, ?)').run(accountId, totalValue || 0, date, note || '');
  db.prepare('UPDATE investment_accounts SET lastValue = ?, lastUpdatedOn = ? WHERE id = ?').run(totalValue, Math.floor(Date.now() / 1000), accountId);
  res.json({ id: r.lastInsertRowid });
});

router.put('/:id', (req, res) => {
  const { note } = req.body;
  db.prepare('UPDATE account_performance SET note = ? WHERE id = ?').run(note || '', req.params.id);
  res.json({ ok: true });
});

router.delete('/:id', (req, res) => {
  db.prepare('DELETE FROM account_performance WHERE id = ?').run(req.params.id);
  res.json({ ok: true });
});

module.exports = router;
