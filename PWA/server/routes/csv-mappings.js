const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/:importType', (req, res) => {
  const row = db.prepare('SELECT * FROM csv_import_mappings WHERE importType = ?').get(req.params.importType);
  res.json(row || { importType: req.params.importType, mappingsJson: '{}', dateFormatJson: '{}' });
});

router.put('/:importType', (req, res) => {
  const { mappingsJson, dateFormatJson } = req.body;
  db.prepare(`INSERT INTO csv_import_mappings (importType, mappingsJson, dateFormatJson) VALUES (?, ?, ?) ON CONFLICT(importType) DO UPDATE SET mappingsJson=excluded.mappingsJson, dateFormatJson=excluded.dateFormatJson`)
    .run(req.params.importType, mappingsJson || '{}', dateFormatJson || '{}');
  res.json({ ok: true });
});

router.get('/named/:importType', (req, res) => {
  res.json(db.prepare('SELECT * FROM csv_named_mappings WHERE importType = ?').all(req.params.importType));
});

router.post('/named', (req, res) => {
  const { name, importType, mappingsJson, dateFormatJson } = req.body;
  const r = db.prepare('INSERT INTO csv_named_mappings (name, importType, mappingsJson, dateFormatJson) VALUES (?, ?, ?, ?)')
    .run(name, importType, mappingsJson || '{}', dateFormatJson || '{}');
  res.json({ id: r.lastInsertRowid });
});

router.delete('/named/:id', (req, res) => {
  db.prepare('DELETE FROM csv_named_mappings WHERE id = ?').run(req.params.id);
  res.json({ ok: true });
});

module.exports = router;
