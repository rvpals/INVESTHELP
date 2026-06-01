const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/', (req, res) => {
  res.json(db.prepare('SELECT * FROM ai_library ORDER BY name').all());
});

router.post('/', (req, res) => {
  const { name, description, promptText } = req.body;
  const r = db.prepare('INSERT INTO ai_library (name, description, promptText) VALUES (?, ?, ?)').run(name, description || '', promptText || '');
  res.json({ id: r.lastInsertRowid });
});

router.delete('/:id', (req, res) => {
  db.prepare('DELETE FROM ai_library WHERE id = ?').run(req.params.id);
  res.json({ ok: true });
});

module.exports = router;
