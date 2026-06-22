const express = require('express');
const router = express.Router();
const db = require('../db');
const yahoo = require('../services/yahoo-finance');

// ── Pearson correlation math ───────────────────────────────────────────────────

function dailyReturns(closes) {
  const ret = [];
  for (let i = 1; i < closes.length; i++) {
    if (closes[i - 1] !== 0) ret.push((closes[i] - closes[i - 1]) / closes[i - 1]);
  }
  return ret;
}

function pearson(a, b) {
  const n = Math.min(a.length, b.length);
  if (n < 2) return NaN;
  let sumA = 0, sumB = 0;
  for (let i = 0; i < n; i++) { sumA += a[i]; sumB += b[i]; }
  const mA = sumA / n, mB = sumB / n;
  let num = 0, varA = 0, varB = 0;
  for (let i = 0; i < n; i++) {
    const da = a[i] - mA, db = b[i] - mB;
    num += da * db; varA += da * da; varB += db * db;
  }
  const denom = Math.sqrt(varA * varB);
  if (denom === 0) return NaN;
  return Math.max(-1, Math.min(1, num / denom));
}

// ── GET /api/correlation/cache ────────────────────────────────────────────────

router.get('/cache', (req, res) => {
  try {
    const row = db.prepare('SELECT * FROM correlation_cache WHERE id = 1').get();
    if (!row) return res.json({ noCache: true });
    res.json({
      tickers:           JSON.parse(row.tickersJson),
      matrix:            JSON.parse(row.matrixJson),
      marketCorrelation: JSON.parse(row.marketCorrelationJson),
      failedTickers:     JSON.parse(row.failedTickersJson),
      calculatedAt:      row.calculatedAt,
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── POST /api/correlation/compute ─────────────────────────────────────────────

router.post('/compute', async (req, res) => {
  try {
    const positions = db.prepare(
      "SELECT ticker FROM investment_positions WHERE quantity > 0 AND (type = 'Stock' OR type = 'ETF')"
    ).all();

    if (positions.length < 2) {
      return res.status(400).json({
        error: 'Need at least 2 Stock or ETF positions to compute a correlation matrix.',
      });
    }

    const tickers = positions.map(p => p.ticker);
    const failedTickers = [];
    const priceHistories = {};

    // Fetch 1 year of daily closes for all tickers in parallel
    await Promise.all(tickers.map(async (ticker) => {
      try {
        const history = await yahoo.fetchPriceHistory(ticker, '1y', '1d');
        if (!history || history.length < 10) throw new Error('Insufficient data');
        priceHistories[ticker] = history;
      } catch (err) {
        console.warn(`Correlation: skipping ${ticker} — ${err.message}`);
        failedTickers.push(ticker);
      }
    }));

    // Fetch SPY as market benchmark (non-fatal)
    let marketHistory = [];
    try {
      marketHistory = await yahoo.fetchPriceHistory('SPY', '1y', '1d');
    } catch {
      console.warn('Correlation: could not fetch SPY benchmark');
    }

    const validTickers = tickers.filter(t => priceHistories[t]);
    if (validTickers.length < 2) {
      return res.status(400).json({
        error: 'Not enough tickers with sufficient price history to compute the matrix.',
      });
    }

    // Build timestamp → close maps
    const maps = {};
    for (const ticker of validTickers) {
      maps[ticker] = new Map(priceHistories[ticker].map(p => [p.timestamp, p.close]));
    }
    const marketMap = new Map(marketHistory.map(p => [p.timestamp, p.close]));

    // Inner-join timestamps across all valid tickers
    let commonTs = [...maps[validTickers[0]].keys()];
    for (let i = 1; i < validTickers.length; i++) {
      const m = maps[validTickers[i]];
      commonTs = commonTs.filter(ts => m.has(ts));
    }
    commonTs.sort((a, b) => a - b);

    // Daily return series per ticker (aligned to common timestamps)
    const returnSeries = {};
    for (const ticker of validTickers) {
      const closes = commonTs.map(ts => maps[ticker].get(ts));
      returnSeries[ticker] = dailyReturns(closes);
    }

    // N×N symmetric correlation matrix
    const n = validTickers.length;
    const matrix = Array.from({ length: n }, () => new Array(n).fill(0));
    for (let i = 0; i < n; i++) {
      for (let j = 0; j < n; j++) {
        if (i === j) { matrix[i][j] = 1.0; }
        else if (j < i) { matrix[i][j] = matrix[j][i]; }
        else {
          const ri = returnSeries[validTickers[i]];
          const rj = returnSeries[validTickers[j]];
          const len = Math.min(ri.length, rj.length);
          matrix[i][j] = pearson(ri.slice(0, len), rj.slice(0, len));
        }
      }
    }

    // Market correlation vs SPY
    const marketCorrelation = {};
    for (const ticker of validTickers) {
      const commonForMkt = commonTs.filter(ts => marketMap.has(ts));
      if (commonForMkt.length < 5) { marketCorrelation[ticker] = null; continue; }
      const closesT = commonForMkt.map(ts => maps[ticker].get(ts));
      const closesM = commonForMkt.map(ts => marketMap.get(ts));
      const retT = dailyReturns(closesT);
      const retM = dailyReturns(closesM);
      const len = Math.min(retT.length, retM.length);
      const r = pearson(retT.slice(0, len), retM.slice(0, len));
      marketCorrelation[ticker] = isNaN(r) ? null : r;
    }

    const calculatedAt = Math.floor(Date.now() / 1000);

    db.prepare(`
      INSERT OR REPLACE INTO correlation_cache
        (id, tickersJson, matrixJson, marketCorrelationJson, failedTickersJson, calculatedAt)
      VALUES (1, ?, ?, ?, ?, ?)
    `).run(
      JSON.stringify(validTickers),
      JSON.stringify(matrix),
      JSON.stringify(marketCorrelation),
      JSON.stringify(failedTickers),
      calculatedAt
    );

    res.json({ tickers: validTickers, matrix, marketCorrelation, failedTickers, calculatedAt });
  } catch (err) {
    console.error('Correlation compute error:', err);
    res.status(500).json({ error: err.message });
  }
});

// ── DELETE /api/correlation/cache ─────────────────────────────────────────────

router.delete('/cache', (req, res) => {
  try {
    db.prepare('DELETE FROM correlation_cache').run();
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
