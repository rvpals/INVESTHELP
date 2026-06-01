const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/summary', (req, res) => {
  const all = db.prepare('SELECT type, value, dayGainLoss FROM investment_positions').all();
  const totalValue = all.reduce((s, r) => s + r.value, 0);
  const dayGainLoss = all.reduce((s, r) => s + r.dayGainLoss, 0);
  const etfValue = all.filter(r => r.type === 'ETF').reduce((s, r) => s + r.value, 0);
  const stockValue = all.filter(r => r.type === 'Stock').reduce((s, r) => s + r.value, 0);
  const dayPercent = totalValue > 0 ? (dayGainLoss / (totalValue - dayGainLoss)) * 100 : 0;
  res.json({ totalValue, dayGainLoss, dayPercent, etfValue, stockValue });
});

router.get('/', (req, res) => {
  res.json(db.prepare('SELECT ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow FROM investment_positions ORDER BY value DESC').all());
});

router.get('/:ticker', (req, res) => {
  const row = db.prepare('SELECT * FROM investment_positions WHERE ticker = ?').get(req.params.ticker);
  if (!row) return res.status(404).json({ error: 'Not found' });
  const { logo, ...rest } = row;
  res.json({ ...rest, hasLogo: !!logo });
});

router.get('/:ticker/logo', (req, res) => {
  const row = db.prepare('SELECT logo FROM investment_positions WHERE ticker = ?').get(req.params.ticker);
  if (!row?.logo) return res.status(404).end();
  res.set('Content-Type', 'image/webp');
  res.set('Cache-Control', 'public, max-age=86400');
  res.send(row.logo);
});

router.post('/', (req, res) => {
  const { ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow } = req.body;
  db.prepare(`
    INSERT INTO investment_positions (ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(ticker) DO UPDATE SET
      name=excluded.name, type=excluded.type, currentPrice=excluded.currentPrice,
      quantity=excluded.quantity, dayGainLoss=excluded.dayGainLoss, value=excluded.value,
      dayHigh=excluded.dayHigh, dayLow=excluded.dayLow
  `).run(ticker, name || '', type || 'Stock', currentPrice || 0, quantity || 0, dayGainLoss || 0, value || 0, dayHigh || 0, dayLow || 0);
  res.json({ ok: true });
});

router.put('/:ticker', (req, res) => {
  const { name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow } = req.body;
  db.prepare(`UPDATE investment_positions SET name=?, type=?, currentPrice=?, quantity=?, dayGainLoss=?, value=?, dayHigh=?, dayLow=? WHERE ticker=?`)
    .run(name || '', type || 'Stock', currentPrice || 0, quantity || 0, dayGainLoss || 0, value || 0, dayHigh || 0, dayLow || 0, req.params.ticker);
  res.json({ ok: true });
});

router.delete('/:ticker', (req, res) => {
  db.prepare('DELETE FROM investment_positions WHERE ticker = ?').run(req.params.ticker);
  res.json({ ok: true });
});

module.exports = router;
