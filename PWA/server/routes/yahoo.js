const express = require('express');
const router = express.Router();
const yahoo = require('../services/yahoo-finance');

router.get('/quote/:ticker', async (req, res) => {
  try {
    const quote = await yahoo.fetchQuote(req.params.ticker);
    res.json(quote);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/history/:ticker', async (req, res) => {
  try {
    const { range, interval } = req.query;
    const data = await yahoo.fetchPriceHistory(req.params.ticker, range || '1d', interval || '1m');
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/history-period/:ticker', async (req, res) => {
  try {
    const { period1, period2, interval } = req.query;
    const data = await yahoo.fetchPriceHistoryByPeriod(req.params.ticker, period1, period2, interval || '1d');
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/analysis/:ticker', async (req, res) => {
  try {
    const data = await yahoo.fetchAnalysisInfo(req.params.ticker);
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/news/:ticker', async (req, res) => {
  try {
    const count = parseInt(req.query.count) || 5;
    const data = await yahoo.fetchNews(req.params.ticker, count);
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/scan/:ticker', async (req, res) => {
  try {
    const data = await yahoo.fetchScanData(req.params.ticker);
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/logo/:ticker', async (req, res) => {
  try {
    const logo = await yahoo.fetchLogo(req.params.ticker);
    if (!logo) return res.status(404).end();
    res.set('Content-Type', 'image/webp');
    res.set('Cache-Control', 'public, max-age=604800');
    res.send(logo);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/report/:ticker', async (req, res) => {
  try {
    const data = await yahoo.fetchFullReport(req.params.ticker);
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.get('/events/:ticker', async (req, res) => {
  try {
    const data = await yahoo.fetchCorporateEvents(req.params.ticker);
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
