const express = require('express');
const router = express.Router();
const db = require('../db');
const yahoo = require('../services/yahoo-finance');

// ── Constants ─────────────────────────────────────────────────────────────────

const TRADING_DAYS_PER_YEAR = 252;
const MINIMUM_RETURN_OBSERVATIONS = 30;

// Calendar days → Yahoo Finance range string
function lookbackToYahooRange(calendarDays) {
  if (calendarDays <= 180) return '6mo';
  if (calendarDays <= 365) return '1y';
  if (calendarDays <= 730) return '2y';
  if (calendarDays <= 1825) return '5y';
  return '10y';
}

// ── Pure math ─────────────────────────────────────────────────────────────────

function calculateDailyReturns(closes) {
  const returns = [];
  for (let i = 1; i < closes.length; i++) {
    if (closes[i - 1] !== 0) returns.push((closes[i] - closes[i - 1]) / closes[i - 1]);
  }
  return returns;
}

function alignPriceSeriesForAllTickers(priceMap) {
  const tickers = Object.keys(priceMap);
  if (tickers.length === 0) return { timestamps: [], pricesByTicker: {} };

  const tsSetsByTicker = tickers.map(t => new Set(priceMap[t].map(p => p.timestamp)));
  let commonTs = [...tsSetsByTicker[0]];
  for (let i = 1; i < tsSetsByTicker.length; i++) {
    commonTs = commonTs.filter(ts => tsSetsByTicker[i].has(ts));
  }
  commonTs.sort((a, b) => a - b);

  const pricesByTicker = {};
  for (const ticker of tickers) {
    const closeByTs = new Map(priceMap[ticker].map(p => [p.timestamp, p.close]));
    pricesByTicker[ticker] = commonTs.map(ts => closeByTs.get(ts)).filter(c => c != null);
  }

  return { timestamps: commonTs, pricesByTicker };
}

function calculatePortfolioWeights(holdings, currentPrices) {
  const positionValues = {};
  let total = 0;
  for (const ticker of Object.keys(holdings)) {
    const shares = holdings[ticker];
    const price = currentPrices[ticker];
    if (!price || shares <= 0 || price <= 0) continue;
    positionValues[ticker] = shares * price;
    total += positionValues[ticker];
  }
  if (total <= 0) return {};
  const weights = {};
  for (const ticker of Object.keys(positionValues)) {
    weights[ticker] = positionValues[ticker] / total;
  }
  return weights;
}

function calculatePortfolioDailyReturns(tickerDailyReturns, weights) {
  const activeTickers = Object.keys(tickerDailyReturns).filter(t => weights[t] != null);
  if (activeTickers.length === 0) return [];
  const length = Math.min(...activeTickers.map(t => tickerDailyReturns[t].length));
  if (length === 0) return [];
  return Array.from({ length }, (_, day) =>
    activeTickers.reduce((sum, ticker) => sum + (weights[ticker] * tickerDailyReturns[ticker][day]), 0)
  );
}

function annualizeReturn(dailyReturns) {
  if (dailyReturns.length === 0) return 0;
  const mean = dailyReturns.reduce((s, v) => s + v, 0) / dailyReturns.length;
  return mean * TRADING_DAYS_PER_YEAR;
}

function calculateDailyRiskFreeRate(annualRate) {
  return annualRate / TRADING_DAYS_PER_YEAR;
}

function calculateExcessReturns(portfolioDailyReturns, dailyRiskFreeRate) {
  return portfolioDailyReturns.map(r => r - dailyRiskFreeRate);
}

function annualizeStandardDeviation(excessReturns) {
  const n = excessReturns.length;
  if (n < 2) return 0;
  const mean = excessReturns.reduce((s, v) => s + v, 0) / n;
  const variance = excessReturns.reduce((s, v) => s + (v - mean) ** 2, 0) / (n - 1);
  return Math.sqrt(variance) * Math.sqrt(TRADING_DAYS_PER_YEAR);
}

function calculateSharpeRatio(annualizedReturn, riskFreeRate, annualizedStdDev) {
  if (annualizedStdDev === 0) return null;
  const raw = (annualizedReturn - riskFreeRate) / annualizedStdDev;
  return Math.round(raw * 100) / 100;
}

// ── Fetch with one retry — always forces daily interval ────────────────────────

async function fetchWithRetry(ticker, range) {
  try {
    const history = await yahoo.fetchPriceHistory(ticker, range, '1d');
    if (history && history.length > 0) return history;
    return null;
  } catch {
    try {
      const history = await yahoo.fetchPriceHistory(ticker, range, '1d');
      return (history && history.length > 0) ? history : null;
    } catch (retryErr) {
      console.warn(`Sharpe: ${ticker} fetch failed after retry — ${retryErr.message}`);
      return null;
    }
  }
}

// ── Cache helpers ─────────────────────────────────────────────────────────────

function readCache() {
  try {
    return db.prepare('SELECT * FROM sharpe_ratio_cache WHERE id = 1').get() || null;
  } catch {
    return null;
  }
}

function writeCache(data) {
  db.prepare(`
    INSERT OR REPLACE INTO sharpe_ratio_cache
      (id, riskFreeRate, lookbackDays, sharpeRatio, annualizedReturn, annualizedVolatility,
       alignedTradingDays, meanDailyReturn, dailyRiskFreeRateUsed, calculationDate,
       tickerDetailsJson, portfolioReturnSeriesJson, skippedTickersJson, skipReasonsJson,
       insufficientDataReason, calculatedAt)
    VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `).run(
    data.riskFreeRate, data.lookbackDays, data.sharpeRatio ?? null,
    data.annualizedReturn, data.annualizedVolatility,
    data.alignedTradingDays, data.meanDailyReturn, data.dailyRiskFreeRateUsed,
    data.calculationDate,
    JSON.stringify(data.tickerDetails || []),
    JSON.stringify(data.portfolioReturnSeries || []),
    JSON.stringify(data.skippedTickers || []),
    JSON.stringify(data.skipReasons || {}),
    data.insufficientDataReason ?? null,
    Math.floor(Date.now() / 1000)
  );
}

function cacheRowToResult(row) {
  return {
    sharpeRatio:           row.sharpeRatio,
    annualizedReturn:      row.annualizedReturn,
    annualizedVolatility:  row.annualizedVolatility,
    riskFreeRate:          row.riskFreeRate,
    lookbackDays:          row.lookbackDays,
    calculationDate:       row.calculationDate,
    skippedTickers:        JSON.parse(row.skippedTickersJson || '[]'),
    skipReasons:           JSON.parse(row.skipReasonsJson    || '{}'),
    insufficientDataReason: row.insufficientDataReason,
    tickerDetails:         JSON.parse(row.tickerDetailsJson  || '[]'),
    portfolioReturnSeries: JSON.parse(row.portfolioReturnSeriesJson || '[]'),
    alignedTradingDays:    row.alignedTradingDays,
    meanDailyReturn:       row.meanDailyReturn,
    dailyRfRate:           row.dailyRiskFreeRateUsed,
    fromCache:             true,
    cachedAt:              row.calculatedAt,
  };
}

// ── GET /api/sharpe/cached ─────────────────────────────────────────────────────

router.get('/cached', (req, res) => {
  const row = readCache();
  if (!row) return res.json(null);
  res.json(cacheRowToResult(row));
});

// ── POST /api/sharpe/compute ──────────────────────────────────────────────────

router.post('/compute', async (req, res) => {
  try {
    const riskFreeRate = parseFloat(req.body.riskFreeRate ?? 0.05);
    const lookbackDays = parseInt(req.body.lookbackDays ?? 365, 10);
    const yahooRange = lookbackToYahooRange(lookbackDays);

    const allPositions = db.prepare(
      "SELECT ticker, quantity, currentPrice FROM investment_positions " +
      "WHERE quantity > 0 AND (type = 'Stock' OR type = 'ETF')"
    ).all();

    if (allPositions.length === 0) {
      return res.status(400).json({ error: 'No stock or ETF positions found.' });
    }

    const priceMap = {};
    const skippedTickers = [];
    const skipReasons = {};

    for (let i = 0; i < allPositions.length; i++) {
      const { ticker } = allPositions[i];
      console.log(`Sharpe: fetching ${i + 1}/${allPositions.length} ${ticker}`);

      const history = await fetchWithRetry(ticker, yahooRange);

      if (!history) {
        skippedTickers.push(ticker);
        skipReasons[ticker] = 'Price history unavailable';
      } else if (history.length < MINIMUM_RETURN_OBSERVATIONS + 1) {
        skippedTickers.push(ticker);
        skipReasons[ticker] = `Only ${history.length} trading days returned`;
      } else {
        priceMap[ticker] = history;
      }
    }

    if (Object.keys(priceMap).length === 0) {
      return res.status(400).json({
        error: `No price data available for any position. Skipped: ${skippedTickers.join(', ')}`,
        skippedTickers,
        skipReasons,
      });
    }

    const validPositions = allPositions.filter(p => priceMap[p.ticker]);
    const holdings = {};
    const currentPrices = {};
    for (const p of validPositions) {
      holdings[p.ticker] = p.quantity;
      currentPrices[p.ticker] = p.currentPrice;
    }
    const weights = calculatePortfolioWeights(holdings, currentPrices);

    if (Object.keys(weights).length === 0) {
      return res.status(400).json({
        error: 'Unable to compute portfolio weights — all position values are zero.',
      });
    }

    const filteredPriceMap = {};
    for (const ticker of Object.keys(weights)) {
      if (priceMap[ticker]) filteredPriceMap[ticker] = priceMap[ticker];
    }

    const { timestamps: alignedTimestamps, pricesByTicker } =
      alignPriceSeriesForAllTickers(filteredPriceMap);

    if (alignedTimestamps.length === 0) {
      return res.status(400).json({
        error: 'No overlapping trading days across positions after alignment.',
        skippedTickers,
        skipReasons,
      });
    }

    const tickerDailyReturns = {};
    for (const ticker of Object.keys(pricesByTicker)) {
      tickerDailyReturns[ticker] = calculateDailyReturns(pricesByTicker[ticker]);
    }

    const returnTimestamps = alignedTimestamps.slice(1);
    const portfolioDailyReturns = calculatePortfolioDailyReturns(tickerDailyReturns, weights);
    const portfolioReturnSeries = returnTimestamps.map((ts, i) => ({
      timestamp: ts,
      return: portfolioDailyReturns[i],
    }));

    if (portfolioDailyReturns.length < MINIMUM_RETURN_OBSERVATIONS) {
      const insufficientReason =
        `Only ${portfolioDailyReturns.length} aligned trading days ` +
        `(minimum ${MINIMUM_RETURN_OBSERVATIONS} required)`;
      const responseData = {
        sharpeRatio: null,
        annualizedReturn: 0,
        annualizedVolatility: 0,
        riskFreeRate,
        lookbackDays,
        calculationDate: new Date().toISOString().slice(0, 10),
        skippedTickers,
        skipReasons,
        insufficientDataReason: insufficientReason,
        portfolioReturnSeries,
        tickerDetails: [],
        alignedTradingDays: portfolioDailyReturns.length,
        meanDailyReturn: 0,
        dailyRfRate: calculateDailyRiskFreeRate(riskFreeRate),
      };
      writeCache(responseData);
      return res.json(responseData);
    }

    const annualizedReturn    = annualizeReturn(portfolioDailyReturns);
    const dailyRf             = calculateDailyRiskFreeRate(riskFreeRate);
    const excessReturns       = calculateExcessReturns(portfolioDailyReturns, dailyRf);
    const annualizedVolatility = annualizeStandardDeviation(excessReturns);
    const sharpeRatio         = calculateSharpeRatio(annualizedReturn, riskFreeRate, annualizedVolatility);
    const meanDailyReturn     = portfolioDailyReturns.reduce((s, v) => s + v, 0) / portfolioDailyReturns.length;

    const insufficientDataReason = sharpeRatio === null
      ? 'Annualized volatility is zero — portfolio has no measurable price variation in this period'
      : null;

    // Per-ticker breakdown
    const tickerDetails = Object.entries(weights).map(([ticker, weight]) => {
      const pos = validPositions.find(p => p.ticker === ticker);
      const tickerReturns = tickerDailyReturns[ticker] || [];
      const tickerExcess = tickerReturns.map(r => r - dailyRf);
      const tickerAnnReturn = tickerReturns.length > 0
        ? (tickerReturns.reduce((s, v) => s + v, 0) / tickerReturns.length) * TRADING_DAYS_PER_YEAR
        : 0;
      let tickerAnnVol = 0;
      if (tickerExcess.length >= 2) {
        const mean = tickerExcess.reduce((s, v) => s + v, 0) / tickerExcess.length;
        const variance = tickerExcess.reduce((s, v) => s + (v - mean) ** 2, 0) / (tickerExcess.length - 1);
        tickerAnnVol = Math.sqrt(variance) * Math.sqrt(TRADING_DAYS_PER_YEAR);
      }
      return {
        ticker,
        shares: pos ? pos.quantity : 0,
        currentPrice: pos ? pos.currentPrice : 0,
        positionValue: pos ? pos.quantity * pos.currentPrice : 0,
        weight,
        annualizedReturn: tickerAnnReturn,
        annualizedVolatility: tickerAnnVol,
        tradingDays: tickerReturns.length,
      };
    }).sort((a, b) => b.weight - a.weight);

    console.log(
      `Sharpe: ratio=${sharpeRatio}, ` +
      `return=${(annualizedReturn * 100).toFixed(2)}%, ` +
      `vol=${(annualizedVolatility * 100).toFixed(2)}%, ` +
      `days=${portfolioDailyReturns.length}, ` +
      `skipped=${skippedTickers.length}`
    );

    const responseData = {
      sharpeRatio,
      annualizedReturn,
      annualizedVolatility,
      riskFreeRate,
      lookbackDays,
      calculationDate: new Date().toISOString().slice(0, 10),
      skippedTickers,
      skipReasons,
      insufficientDataReason,
      portfolioReturnSeries,
      tickerDetails,
      alignedTradingDays: portfolioDailyReturns.length,
      meanDailyReturn,
      dailyRfRate: dailyRf,
    };

    writeCache(responseData);
    res.json(responseData);
  } catch (err) {
    console.error('Sharpe compute error:', err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
