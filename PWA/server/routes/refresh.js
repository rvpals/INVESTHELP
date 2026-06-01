const express = require('express');
const router = express.Router();
const autoRefresh = require('../services/auto-refresh');

router.post('/', async (req, res) => {
  try {
    const result = await autoRefresh.refreshAll();
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/status', (req, res) => {
  res.json(autoRefresh.getStatus());
});

module.exports = router;
