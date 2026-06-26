const express = require('express');
const router = express.Router();
const db = require('../db');
const yahoo = require('../services/yahoo-finance');

// ── Constants ─────────────────────────────────────────────────────────────────

const TRADING_DAYS_PER_YEAR = 252;
const MINIMUM_RETURN_OBSERVATIONS = 30;

// Range → Yahoo Finance API range string
function lookbackToYahooRange(calendarDays) {
  if (calendarDays <= 180) return '6mo';
  if (calendarDays <= 365) return '1y';
  return '2y';
}

// ── Pure math ─────────────────────────────────────────────────────────────────

// Step 2: (close[i] - close[i-1]) / close[i-1]
function calculateDailyReturns(closes) {
  const returns = [];
  for (let i = 1; i < closes.length; i++) {
    if (closes[i - 1] !== 0) returns.push((closes[i] - closes[i - 1]) / closes[i - 1]);
  }
  return returns;
}

// Step 3: inner-join all tickers on shared timestamps, return aligned closes + sorted timestamps
function alignPriceSeriesForAllTickers(priceMap) {
  const tickers = Object.keys(priceMap);
  if (tickers.length === 0) return { timestamps: [], pricesByTicker: {} };

  // Start with all timestamps of the first ticker, then intersect with each subsequent one
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

// Step 4: weight = positionValue / totalPortfolioValue
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

// Step 5: Σ(weight_i × dailyReturn_i) for each day
function calculatePortfolioDailyReturns(tickerDailyReturns, weights) {
  const activeTickers = Object.keys(tickerDailyReturns).filter(t => weights[t] != null);
  if (activeTickers.length === 0) return [];
  const length = Math.min(...activeTickers.map(t => tickerDailyReturns[t].length));
  if (length === 0) return [];
  return Array.from({ length }, (_, day) =>
    activeTickers.reduce((sum, ticker) => sum + (weights[ticker] * tickerDailyReturns[ticker][day]), 0)
  );
}

// Step 6: mean(dailyReturns) × 252
function annualizeReturn(dailyReturns) {
  if (dailyReturns.length === 0) return 0;
  const mean = dailyReturns.reduce((s, v) => s + v, 0) / dailyReturns.length;
  return mean * TRADING_DAYS_PER_YEAR;
}

// Step 7: annualRate / 252
function calculateDailyRiskFreeRate(annualRate) {
  return annualRate / TRADING_DAYS_PER_YEAR;
}

// Step 8: portfolio_return_d - dailyRiskFreeRate
function calculateExcessReturns(portfolioDailyReturns, dailyRiskFreeRate) {
  return portfolioDailyReturns.map(r => r - dailyRiskFreeRate);
}

// Step 9a: sqrt(sampleVariance(excessReturns)) × sqrt(252), returns 0 if < 2 observations
function annualizeStandardDeviation(excessReturns) {
  const n = excessReturns.length;
  if (n < 2) return 0;
  const mean = excessReturns.reduce((s, v) => s + v, 0) / n;
  const variance = excessReturns.reduce((s, v) => s + (v - mean) ** 2, 0) / (n - 1);
  return Math.sqrt(variance) * Math.sqrt(TRADING_DAYS_PER_YEAR);
}

// Step 9b: (annReturn - rf) / annVol, null if annVol === 0, rounded to 2dp
function calculateSharpeRatio(annualizedReturn, riskFreeRate, annualizedStdDev) {
  if (annualizedStdDev === 0) return null;
  const raw = (annualizedReturn - riskFreeRate) / annualizedStdDev;
  return Math.round(raw * 100) / 100;
}

// ── Fetch with one retry ───────────────────────────────────────────────────────

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
      // Emit a server-sent event or just log — clients poll for the final result
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

    // Build weights using current prices already in DB (no extra API call)
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

    // Align price series to shared trading days
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

    // Per-ticker daily returns from aligned prices
    const tickerDailyReturns = {};
    for (const ticker of Object.keys(pricesByTicker)) {
      tickerDailyReturns[ticker] = calculateDailyReturns(pricesByTicker[ticker]);
    }

    // Return timestamps are offset by 1 (each return = day i vs day i-1)
    const returnTimestamps = alignedTimestamps.slice(1);

    const portfolioDailyReturns = calculatePortfolioDailyReturns(tickerDailyReturns, weights);
    const portfolioReturnSeries = returnTimestamps.map((ts, i) => ({
      timestamp: ts,
      return: portfolioDailyReturns[i],
    }));

    if (portfolioDailyReturns.length < MINIMUM_RETURN_OBSERVATIONS) {
      return res.json({
        sharpeRatio: null,
        annualizedReturn: 0,
        annualizedVolatility: 0,
        riskFreeRate,
        lookbackDays,
        calculationDate: new Date().toISOString().slice(0, 10),
        skippedTickers,
        skipReasons,
        insufficientDataReason:
          `Only ${portfolioDailyReturns.length} aligned trading days ` +
          `(minimum ${MINIMUM_RETURN_OBSERVATIONS} required)`,
        portfolioReturnSeries,
      });
    }

    const annualizedReturn   = annualizeReturn(portfolioDailyReturns);
    const dailyRf            = calculateDailyRiskFreeRate(riskFreeRate);
    const excessReturns      = calculateExcessReturns(portfolioDailyReturns, dailyRf);
    const annualizedVolatility = annualizeStandardDeviation(excessReturns);
    const sharpeRatio        = calculateSharpeRatio(annualizedReturn, riskFreeRate, annualizedVolatility);

    const insufficientDataReason = sharpeRatio === null
      ? 'Annualized volatility is zero — portfolio has no measurable price variation in this period'
      : null;

    console.log(
      `Sharpe: ratio=${sharpeRatio}, ` +
      `return=${(annualizedReturn * 100).toFixed(2)}%, ` +
      `vol=${(annualizedVolatility * 100).toFixed(2)}%, ` +
      `days=${portfolioDailyReturns.length}, ` +
      `skipped=${skippedTickers.length}`
    );

    res.json({
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
    });
  } catch (err) {
    console.error('Sharpe compute error:', err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
