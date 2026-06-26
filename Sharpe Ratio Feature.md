# Sharpe Ratio Feature — Implementation Reference

This document is the canonical reference for the Sharpe Ratio feature.  
It was distilled from the Android (Kotlin/Compose) implementation and drives the PWA port.

---

## What it does

Computes the **Sharpe Ratio** for the entire portfolio — a single number that measures
risk-adjusted return.  Higher is better.  Uses each position's price history over a
configurable lookback window.

**No value is stored in the database.**  The result is always computed on demand.

---

## Inputs

| Input | Default | Notes |
|---|---|---|
| Holdings | All Stock + ETF positions with `quantity > 0` | Loaded from `investment_positions` |
| Risk-free rate | 5% annual (0.05) | User-configurable, stored as decimal |
| Lookback window | 365 calendar days | Options: 180 / 365 / 730 days |

---

## The 9-Step Algorithm

Each step is a single-responsibility function.  All inputs and outputs are in **decimal**
form (0.15 = 15%) unless noted.

### Step 1 — Fetch historical prices
```
fetchHistoricalPrices(ticker, lookbackDays) → List<{timestamp: Long, close: Double}>
```
- Source: Yahoo Finance v8 chart API — `range={lookbackDays}d&interval=1d`
- Returns a list of `{timestamp (epoch-seconds), close}` sorted chronologically
- On network failure: retry once, then skip the ticker (never fail silently)
- On empty result or fewer than 31 prices: skip ticker with reason logged

### Step 2 — Daily returns per ticker
```
calculateDailyReturns(closes: List<Double>) → List<Double>
```
- Formula: `(close[i] - close[i-1]) / close[i-1]`
- Returns a list of length `n - 1`
- Returns empty list if fewer than 2 prices

### Step 3 — Align all tickers to shared trading days
```
alignPriceSeriesForAllTickers(priceMap) → AlignedSeries {timestamps, pricesByTicker}
```
- Inner-join all tickers on their timestamps (keep only days where ALL tickers have data)
- Sort common timestamps chronologically
- Tickers that produce zero aligned prices after intersection are skipped
- **The return-series timestamps are `alignedTimestamps.drop(1)`** (one entry per return, offset by 1 from prices)

### Step 4 — Portfolio weights
```
calculatePortfolioWeights(holdings, currentPrices) → Map<ticker, weight>
```
- `positionValue = shares × currentPrice`
- `weight = positionValue / totalPortfolioValue`
- Tickers with zero/negative shares or price are excluded
- Returns empty map if total portfolio value ≤ 0

### Step 5 — Weighted portfolio daily returns
```
calculatePortfolioDailyReturns(tickerDailyReturns, weights) → List<Double>
```
- Formula: `Σ(weight_i × dailyReturn_i)` for each aligned day
- All return series must be equal-length (guaranteed when built from aligned prices)

### Step 6 — Annualise return
```
annualizeReturn(dailyReturns) → Double
```
- Formula: `mean(dailyReturns) × 252`

### Step 7 — Daily risk-free rate
```
calculateDailyRiskFreeRate(annualRate) → Double
```
- Formula: `annualRate / 252`

### Step 8 — Excess returns
```
calculateExcessReturns(portfolioDailyReturns, dailyRiskFreeRate) → List<Double>
```
- Formula: `portfolioReturn_d - dailyRiskFreeRate` for each day

### Step 9a — Annualise standard deviation
```
annualizeStandardDeviation(excessReturns) → Double
```
- Uses **sample** standard deviation (divide by `n - 1`, not `n`)
- Formula: `sqrt(variance(excessReturns, ddof=1)) × sqrt(252)`
- Returns 0.0 if fewer than 2 observations

### Step 9b — Sharpe Ratio
```
calculateSharpeRatio(annualizedReturn, riskFreeRate, annualizedStdDev) → Double | null
```
- Formula: `(annualizedReturn - riskFreeRate) / annualizedStdDev`
- **Returns null** (not 0) when `annualizedStdDev == 0` — never divide by zero
- Round to 2 decimal places

---

## Orchestrator flow

```
compute(riskFreeRate, lookbackDays):
  positions ← DB query (Stock or ETF, quantity > 0)
  if empty → error "No positions found"

  for each position:
    history ← fetchHistoricalPrices(ticker, lookbackDays)   // with 1 retry
    if null or < 31 prices → skip, record reason

  if all skipped → error

  weights ← calculatePortfolioWeights(holdings, currentPrices)
  aligned ← alignPriceSeriesForAllTickers(validPriceMap)
  tickerReturns ← calculateDailyReturns per aligned ticker
  portfolioReturns ← calculatePortfolioDailyReturns(tickerReturns, weights)
  returnTimestamps ← alignedTimestamps.drop(1)

  if portfolioReturns.size < 30 → return SharpeResult(sharpeRatio=null, reason=…)

  annReturn    ← annualizeReturn(portfolioReturns)
  dailyRf      ← calculateDailyRiskFreeRate(riskFreeRate)
  excessRet    ← calculateExcessReturns(portfolioReturns, dailyRf)
  annVol       ← annualizeStandardDeviation(excessReturns)
  sharpe       ← calculateSharpeRatio(annReturn, riskFreeRate, annVol)   // null if vol=0

  return SharpeResult {
    sharpeRatio, annualizedReturn, annualizedVolatility,
    riskFreeRate, lookbackDays, calculationDate,
    skippedTickers, skipReasons, insufficientDataReason
  }
```

---

## SharpeResult shape

```json
{
  "sharpeRatio":         1.87,        // null when insufficient data or zero volatility
  "annualizedReturn":    0.152,        // decimal (0.152 = 15.2%)
  "annualizedVolatility":0.108,
  "riskFreeRate":        0.05,
  "lookbackDays":        365,
  "calculationDate":     "2026-06-25",
  "skippedTickers":      ["XYZ"],
  "skipReasons":         {"XYZ": "Only 12 trading days returned"},
  "insufficientDataReason": null,       // non-null explains why sharpeRatio is null
  "portfolioReturnSeries": [
    { "timestamp": 1700000000, "return": 0.0082 },
    { "timestamp": 1700086400, "return": -0.0031 }
  ]
}
```

---

## Error handling

| Scenario | Behaviour |
|---|---|
| Ticker fetch fails | Retry once → skip with reason; never crash |
| Fewer than 31 days of data | Skip ticker with reason |
| All tickers skipped | Return HTTP 400 / error state with message |
| No overlapping trading days | Return error state |
| Fewer than 30 portfolio return observations | Return `sharpeRatio: null` with reason |
| `annualizedVolatility == 0` | Return `sharpeRatio: null` with reason |

---

## Interpretation labels

| Sharpe Ratio | Label | Display colour |
|---|---|---|
| < 1.0 | Subpar | Red `#C62828` |
| 1.0 – 2.0 | Good | Green `#2E7D32` |
| 2.0 – 3.0 | Very Good | Blue `#0D47A1` |
| ≥ 3.0 | Exceptional | Purple `#6A1B9A` |

---

## UI specification

### Screen layout (top → bottom, scrollable)

1. **Parameters card**
   - Text input: "Risk-Free Rate" + "%" suffix (default `5.0`)
   - Period selector: chip group — `6 months` | `1 year` ✓ | `2 years`
   - "Calculate" button (right-aligned)

2. **Loading card** (replaces result area while fetching)
   - Spinner + progress text: `Fetching prices…  3 / 10  (AAPL)`

3. **Result card**
   - If `sharpeRatio != null`:
     - Large number (`displaySmall`/`h1`) + coloured interpretation chip side by side
     - Subtext "Sharpe Ratio" in muted colour
   - If null:
     - Info icon + "Unable to compute" title
     - `insufficientDataReason` text below

4. **Metrics card** (table, alternating dividers)
   - Annualized Return: `15.2%`
   - Annualized Volatility: `10.8%`
   - Risk-Free Rate Used: `5.0%`
   - Period: `Last 365 calendar days`
   - As of Date: `2026-06-25`

5. **Daily Portfolio Returns chart** (canvas, 200–220 dp/px tall)
   - X-axis: dates (5 evenly-spaced labels)
   - Y-axis: return % (symmetric ±range, min ±0.5%, 5 grid levels)
   - Zero line: medium grey, slightly thicker
   - Fill **above** zero: green `#2E7D32` at 25–30% opacity (clipped to upper half)
   - Fill **below** zero: red `#C62828` at 25–30% opacity (clipped to lower half)
   - Return line: primary colour, 1.5 px
   - Tap / click: vertical crosshair + coloured dot + tooltip (`MMM d: +0.123%`)

6. **Skipped tickers card** (shown only when `skippedTickers.length > 0`)
   - Header: "Excluded from calculation (N)"
   - Rows: ticker | reason (right-aligned, muted)

### States
- **Idle** — nothing shown below the parameters card (transient; auto-computes on load)
- **Loading** — spinner card replaces result area
- **Success** — result + metrics + chart + optional skipped
- **Error** — error message card with Retry button

---

## Yahoo Finance API usage

| Purpose | Endpoint |
|---|---|
| Historical daily closes | `v8/finance/chart/{ticker}?range={range}&interval=1d` |

Range mapping: `180d` → 6 months, `365d` → 1 year, `730d` → 2 years.

Response path for closes: `data.chart.result[0].indicators.quote[0].close`  
Response path for timestamps: `data.chart.result[0].timestamp`

---

## Key design decisions

- **No DB storage** — Sharpe is a derived metric; always computed on demand.
- **No new dependencies** — pure math (`sqrt`, `mean`, `stddev`) in language primitives only.
- **Fetch on server side** (PWA) — avoids CORS; consistent with all other Yahoo Finance calls.
- **Minimum 30 observations** — below this, std dev is unreliable; return `null` with reason.
- **Current prices from DB** — used for weight calculation; avoids an extra API call per ticker.
- **`currentPrices` source** — `investment_positions.currentPrice` (set during last Refresh All).
- **Lookback is in calendar days** — Yahoo Finance `range` param accepts calendar days; the actual
  number of trading days returned is fewer (typically ~252 per 365 calendar days).
- **Retry once on fetch failure** — balances reliability vs. rate limiting.
- **Sample std dev (ddof=1)** — correct for a sample; `n - 1` denominator, not `n`.
