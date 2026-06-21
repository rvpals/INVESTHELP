const express = require('express');
const router = express.Router();
const yahoo = require('../services/yahoo-finance');
const db = require('../db');

// In-memory cache: ticker → { data, fetchedAt }
const cache = new Map();
const CACHE_TTL_MS = 3_600_000; // 1 hour

function computeVolatility(closes) {
  if (closes.length < 2) return { annualizedVolPct: 0, dailyStdDevPct: 0 };
  const logReturns = [];
  for (let i = 1; i < closes.length; i++) {
    if (closes[i - 1] > 0) logReturns.push(Math.log(closes[i] / closes[i - 1]));
  }
  if (logReturns.length < 2) return { annualizedVolPct: 0, dailyStdDevPct: 0 };
  const mean = logReturns.reduce((s, v) => s + v, 0) / logReturns.length;
  const variance = logReturns.reduce((s, v) => s + (v - mean) ** 2, 0) / (logReturns.length - 1);
  const dailyStdDev = Math.sqrt(variance);
  return {
    dailyStdDevPct: dailyStdDev * 100,
    annualizedVolPct: dailyStdDev * Math.sqrt(252) * 100,
  };
}

function volatilityLabel(pct) {
  if (pct < 15) return 'Low';
  if (pct < 30) return 'Moderate';
  if (pct < 60) return 'High';
  return 'Very High';
}

// --- DB cache routes (must be declared before /:ticker) ---

router.get('/cache/all', (req, res) => {
  try {
    const rows = db.prepare('SELECT * FROM volatility_cache ORDER BY type, annualizedVolPct ASC').all();
    const lastCalculatedAt = rows.length
      ? Math.max(...rows.map(r => r.calculatedAt))
      : null;
    res.json({ items: rows, lastCalculatedAt });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.post('/cache/bulk', (req, res) => {
  try {
    const { items } = req.body;
    if (!Array.isArray(items) || items.length === 0) return res.json({ saved: 0 });
    const now = Math.floor(Date.now() / 1000);
    const stmt = db.prepare(`
      INSERT OR REPLACE INTO volatility_cache
        (ticker, companyName, type, shares, currentPrice,
         annualizedVolPct, dailyStdDevPct, volatilityLabel,
         low52w, high52w, rangePositionPct, sampleCount, calculatedAt)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);
    const tx = db.transaction(() => {
      for (const item of items) {
        stmt.run(
          item.ticker, item.companyName ?? null, item.type, item.shares,
          item.currentPrice, item.annualizedVolPct, item.dailyStdDevPct,
          item.volatilityLabel, item.low52w, item.high52w,
          item.rangePositionPct, item.sampleCount, now
        );
      }
    });
    tx();
    res.json({ saved: items.length, calculatedAt: now });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.delete('/cache/all', (req, res) => {
  try {
    db.prepare('DELETE FROM volatility_cache').run();
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// --- Per-ticker live fetch ---

router.get('/:ticker', async (req, res) => {
  const key = req.params.ticker.toUpperCase();
  const force = req.query.force === 'true';

  if (!force) {
    const cached = cache.get(key);
    if (cached && Date.now() - cached.fetchedAt < CACHE_TTL_MS) {
      return res.json(cached.data);
    }
  }

  try {
    const [history, analysis, quote] = await Promise.all([
      yahoo.fetchPriceHistory(key, '1y', '1d'),
      yahoo.fetchAnalysisInfo(key).catch(() => ({})),
      yahoo.fetchQuote(key),
    ]);

    const closes = history.map(p => p.close).filter(v => v != null && v > 0);
    const vol = computeVolatility(closes);

    const low52w = (analysis.fiftyTwoWeekLow ?? null) !== null
      ? analysis.fiftyTwoWeekLow
      : (closes.length ? Math.min(...closes) : 0);
    const high52w = (analysis.fiftyTwoWeekHigh ?? null) !== null
      ? analysis.fiftyTwoWeekHigh
      : (closes.length ? Math.max(...closes) : 0);

    const rangePositionPct = high52w > low52w
      ? Math.max(0, Math.min(100, (quote.price - low52w) / (high52w - low52w) * 100))
      : 50;

    const data = {
      ticker: key,
      companyName: quote.shortName || analysis.shortName || null,
      currentPrice: quote.price,
      low52w,
      high52w,
      rangePositionPct,
      annualizedVolPct: vol.annualizedVolPct,
      dailyStdDevPct: vol.dailyStdDevPct,
      volatilityLabel: volatilityLabel(vol.annualizedVolPct),
      sampleCount: closes.length,
    };

    cache.set(key, { data, fetchedAt: Date.now() });
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
