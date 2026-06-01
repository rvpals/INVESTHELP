const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/', (req, res) => {
  res.json(db.prepare('SELECT * FROM investment_accounts ORDER BY name').all());
});

router.get('/:id', (req, res) => {
  const row = db.prepare('SELECT * FROM investment_accounts WHERE id = ?').get(req.params.id);
  if (!row) return res.status(404).json({ error: 'Not found' });
  res.json(row);
});

router.post('/', (req, res) => {
  const { name, description, initialValue } = req.body;
  const r = db.prepare('INSERT INTO investment_accounts (name, description, initialValue) VALUES (?, ?, ?)').run(name, description || '', initialValue || 0);
  res.json({ id: r.lastInsertRowid });
});

router.put('/:id', (req, res) => {
  const { name, description, initialValue } = req.body;
  db.prepare('UPDATE investment_accounts SET name = ?, description = ?, initialValue = ? WHERE id = ?').run(name, description || '', initialValue || 0, req.params.id);
  res.json({ ok: true });
});

router.delete('/:id', (req, res) => {
  db.prepare('DELETE FROM investment_accounts WHERE id = ?').run(req.params.id);
  res.json({ ok: true });
});

module.exports = router;
