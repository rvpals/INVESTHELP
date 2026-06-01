const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/', (req, res) => {
  res.json(db.prepare('SELECT * FROM definitions ORDER BY name').all());
});

router.post('/', (req, res) => {
  const { name, description } = req.body;
  const r = db.prepare('INSERT INTO definitions (name, description) VALUES (?, ?)').run(name, description || '');
  res.json({ id: r.lastInsertRowid });
});

router.put('/:id', (req, res) => {
  const { name, description } = req.body;
  db.prepare('UPDATE definitions SET name = ?, description = ? WHERE id = ?').run(name, description || '', req.params.id);
  res.json({ ok: true });
});

router.delete('/:id', (req, res) => {
  db.prepare('DELETE FROM definitions WHERE id = ?').run(req.params.id);
  res.json({ ok: true });
});

module.exports = router;
