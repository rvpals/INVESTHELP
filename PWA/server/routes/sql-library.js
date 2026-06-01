const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/', (req, res) => {
  res.json(db.prepare('SELECT * FROM sql_library ORDER BY category, name').all());
});

router.get('/categories', (req, res) => {
  res.json(db.prepare('SELECT DISTINCT category FROM sql_library ORDER BY category').all().map(r => r.category));
});

router.post('/', (req, res) => {
  const { name, description, category, sql } = req.body;
  const r = db.prepare('INSERT INTO sql_library (name, description, category, sql) VALUES (?, ?, ?, ?)').run(name, description || '', category || '', sql || '');
  res.json({ id: r.lastInsertRowid });
});

router.delete('/:id', (req, res) => {
  db.prepare('DELETE FROM sql_library WHERE id = ?').run(req.params.id);
  res.json({ ok: true });
});

module.exports = router;
