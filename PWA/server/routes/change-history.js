const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/', (req, res) => {
  res.json(db.prepare('SELECT * FROM change_history ORDER BY date DESC').all());
});

router.post('/', (req, res) => {
  const { date, etfValue, stockValue, totalValue, dailyChangeEtf, dailyChangeStock, dailyChangeTotal } = req.body;
  db.prepare(`
    INSERT INTO change_history (date, etfValue, stockValue, totalValue, dailyChangeEtf, dailyChangeStock, dailyChangeTotal)
    VALUES (?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(date) DO UPDATE SET
      etfValue=excluded.etfValue, stockValue=excluded.stockValue, totalValue=excluded.totalValue,
      dailyChangeEtf=excluded.dailyChangeEtf, dailyChangeStock=excluded.dailyChangeStock, dailyChangeTotal=excluded.dailyChangeTotal
  `).run(date, etfValue || 0, stockValue || 0, totalValue || 0, dailyChangeEtf || 0, dailyChangeStock || 0, dailyChangeTotal || 0);
  res.json({ ok: true });
});

router.delete('/:id', (req, res) => {
  db.prepare('DELETE FROM change_history WHERE id = ?').run(req.params.id);
  res.json({ ok: true });
});

module.exports = router;
