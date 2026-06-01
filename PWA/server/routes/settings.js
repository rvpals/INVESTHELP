const express = require('express');
const router = express.Router();
const db = require('../db');
const autoRefresh = require('../services/auto-refresh');

router.get('/', (req, res) => {
  const rows = db.prepare('SELECT key, value FROM settings').all();
  const obj = {};
  for (const r of rows) obj[r.key] = r.value;
  res.json(obj);
});

router.put('/:key', (req, res) => {
  const { value } = req.body;
  db.prepare('INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value=excluded.value')
    .run(req.params.key, String(value ?? ''));

  // Handle auto-refresh toggle
  if (req.params.key === 'auto_refresh_enabled') {
    if (value === 'true') {
      const interval = db.prepare("SELECT value FROM settings WHERE key = 'auto_refresh_interval'").get();
      const ms = { '5m': 300000, '30m': 1800000, '1h': 3600000, '5h': 18000000, 'market_close': 86400000 };
      autoRefresh.startAutoRefresh(ms[interval?.value] || 1800000);
    } else {
      autoRefresh.stopAutoRefresh();
    }
  }
  if (req.params.key === 'auto_refresh_interval') {
    const enabled = db.prepare("SELECT value FROM settings WHERE key = 'auto_refresh_enabled'").get();
    if (enabled?.value === 'true') {
      const ms = { '5m': 300000, '30m': 1800000, '1h': 3600000, '5h': 18000000, 'market_close': 86400000 };
      autoRefresh.startAutoRefresh(ms[value] || 1800000);
    }
  }

  res.json({ ok: true });
});

module.exports = router;
