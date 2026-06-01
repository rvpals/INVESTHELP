const express = require('express');
const router = express.Router();
const db = require('../db');

router.post('/execute', (req, res) => {
  const { sql } = req.body;
  if (!sql?.trim()) return res.status(400).json({ error: 'No SQL provided' });

  try {
    const trimmed = sql.trim();
    const isSelect = /^(SELECT|PRAGMA|EXPLAIN)/i.test(trimmed);

    if (isSelect) {
      const stmt = db.prepare(trimmed);
      const rows = stmt.all();
      const columns = rows.length > 0 ? Object.keys(rows[0]) : [];
      res.json({ type: 'query', columns, rows: rows.map(r => columns.map(c => r[c])) });
    } else {
      const result = db.prepare(trimmed).run();
      res.json({ type: 'statement', changes: result.changes });
    }
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/tables', (req, res) => {
  const tables = db.prepare("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' AND name != 'android_metadata' ORDER BY name").all();
  const result = tables.map(t => {
    const cols = db.prepare(`PRAGMA table_info('${t.name}')`).all();
    return {
      name: t.name,
      columns: cols.map(c => ({ name: c.name, type: c.type, pk: c.pk === 1, notnull: c.notnull === 1 })),
    };
  });
  res.json(result);
});

module.exports = router;
