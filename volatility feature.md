# 52-Week Volatility Feature

Analysis screen showing annualized volatility and 52-week price range for a single holding.

---

## Entry Point

Accessed from the **Item Detail screen** via a toolbar/action button (bar chart icon). The button passes `ticker` and `shares` (quantity owned) to the volatility screen.

---

## Data to Fetch

Three sequential API calls (can be parallelized):

### 1. Historical Daily Closes — 1 year
**Yahoo Finance v8 chart API**
```
GET https://query1.finance.yahoo.com/v8/finance/chart/{ticker}?range=365d&interval=1d
```
- Parse: `chart.result[0].timestamp[]` and `chart.result[0].indicators.quote[0].close[]`
- Zip timestamps + closes; skip null close values
- Result: array of `{ timestamp: epochSeconds, close: number }`
- Used for: volatility calculation

### 2. 52-Week High / Low — from quoteSummary
**Yahoo Finance v10 quoteSummary API** (requires crumb cookie — already handled by `StockPriceService.fetchAnalysisInfo`)
```
GET https://query2.finance.yahoo.com/v10/finance/quoteSummary/{ticker}?modules=summaryDetail,defaultKeyStatistics,...
```
- Parse: `quoteSummary.result[0].summaryDetail.fiftyTwoWeekHigh.raw` and `fiftyTwoWeekLow.raw`
- These are **official intraday-based** 52-week highs/lows from Yahoo Finance
- Fallback: derive from `max(closes)` / `min(closes)` if API fails

### 3. Current Price
**Yahoo Finance v8 chart API (1-day, 1-min interval)**
```
GET https://query1.finance.yahoo.com/v8/finance/chart/{ticker}?range=1d&interval=1m
```
- Parse: `chart.result[0].meta.regularMarketPrice`
- Also available: `shortName` / `longName` for company display name

---

## Volatility Math

Implemented in `VolatilityCalculator` (standalone, testable).

### Step 1 — Log Returns
```
logReturn[i] = ln(close[i] / close[i-1])
```
Skip or guard against zero/negative prices before division.

### Step 2 — Sample Standard Deviation
```
mean = average(logReturns)
variance = sum((r - mean)^2) / (n - 1)    ← sample variance (n-1, not n)
dailyStdDev = sqrt(variance)
```

### Step 3 — Annualize
```
annualizedVol = dailyStdDev * sqrt(252) * 100   ← result in percent
```
`252` = approximate trading days per year.

### Volatility Labels
| Range | Label |
|-------|-------|
| < 15% | Low |
| 15–30% | Moderate |
| 30–60% | High |
| > 60% | Very High |

### 52-Week Range Position
```
rangePositionPct = (currentPrice - low52w) / (high52w - low52w) * 100
```
Clamped to 0–100.

---

## Caching

- Cache results **in memory** keyed by ticker
- TTL: **1 hour** (3,600,000 ms)
- On cache hit: return cached data immediately without API calls
- Expose a `refresh()` function that clears the cache entry and re-fetches

```js
// Example cache structure
const cache = {};   // ticker → { data, fetchedAt }
const CACHE_TTL_MS = 3_600_000;

function isCacheValid(ticker) {
  const entry = cache[ticker];
  return entry && (Date.now() - entry.fetchedAt) < CACHE_TTL_MS;
}
```

---

## Data Model

```js
{
  ticker: "AAPL",
  companyName: "Apple Inc.",   // nullable
  shares: 125,
  currentPrice: 185.20,
  positionValue: 23150.00,     // currentPrice * shares
  low52w: 142.00,
  high52w: 199.62,
  rangePositionPct: 74.2,      // 0–100
  annualizedVolPct: 24.8,
  dailyStdDevPct: 1.56,
  volatilityLabel: "Moderate",
  sampleCount: 252             // number of trading sessions used
}
```

---

## Screen Layout

```
┌─────────────────────────────────────┐
│ ← Back    52-Week Volatility   [↻]  │  TopAppBar
├─────────────────────────────────────┤
│                                     │
│  ┌─ Position Value (accent card) ─┐ │
│  │         AAPL                   │ │
│  │       Apple Inc.               │ │
│  │   Position Value               │ │
│  │     $23,150.00                 │ │
│  │  $185.20 × 125 shares          │ │
│  └────────────────────────────────┘ │
│                                     │
│  ┌─ 52-Week Range ────────────────┐ │
│  │                                │ │
│  │  ●══════════════●─────────○    │ │  range bar
│  │  $142.00              $199.62  │ │  low / high labels
│  │  ──────────────────────────── │ │
│  │  Current: $185.20   [74.2% of range] │
│  └────────────────────────────────┘ │
│                                     │
│  ┌─ Annualized Volatility ────────┐ │
│  │                                │ │
│  │         24.8%                  │ │  large
│  │      [MODERATE]                │ │  colored badge
│  │  ──────────────────────────── │ │
│  │  Daily Std Dev    1.56%        │ │
│  │  Annualized       24.8%        │ │
│  │  Trading sessions 252          │ │
│  │  Method           Log returns  │ │
│  │  ──────────────────────────── │ │
│  │  Volatility Scale              │ │
│  │  [Low][Moderate*][High][VHigh] │ │  4-cell legend
│  └────────────────────────────────┘ │
└─────────────────────────────────────┘
```

### States

| State | Display |
|-------|---------|
| Loading | Centered spinner + "Fetching market data…" |
| Error | Error message + Retry button |
| Success | Full 3-card layout (scrollable) |

---

## Range Bar (Visual Component)

Draw a horizontal bar:
- **Track**: full-width rounded rect, surface-variant color, 8px tall
- **Fill**: rounded rect from left edge to `rangePositionPct`% of width, primary color
- **Dot**: circle at the fill endpoint — white fill + primary color border

For PWA implementation, draw on an HTML5 `<canvas>` element (or use CSS with a positioned pseudo-element).

---

## Volatility Scale Legend

4 equal-width cells side by side, each showing label + range:
- Active cell (matching current label): colored background + border
- Inactive cells: muted/surface-variant background

Colors:
| Label | Color |
|-------|-------|
| Low | #388E3C (green) |
| Moderate | #F57C00 (amber) |
| High | #E64A19 (deep orange) |
| Very High | #B71C1C (deep red) |

---

## Android Implementation Reference

| File | Purpose |
|------|---------|
| `util/VolatilityCalculator.kt` | Pure math: log returns, sample σ, annualization, range position |
| `ui/volatility/VolatilityViewModel.kt` | StateFlow state, 1-hour cache, 3 API calls |
| `ui/volatility/VolatilityScreen.kt` | Composable screen: 3 cards + Canvas range bar |
| `ui/navigation/NavRoutes.kt` | `VolatilityRoute(ticker, shares)` |
| `ui/navigation/InvestHelpNavHost.kt` | composable registration + navigation call |
| `ui/item/ItemDetailScreen.kt` | `onVolatility` callback + BarChart icon button in TopAppBar |

---

## PWA Implementation Notes

- **Route**: `#/volatility/:ticker` — read `shares` from DB position for the ticker
- **Server route**: `/api/volatility/:ticker` — fetches all 3 data sources, computes metrics, returns JSON; or do 3 separate calls on the client side using existing `/api/yahoo/*` proxy routes
- **Cache**: In the route handler or a module-level Map, store `{ data, fetchedAt }` per ticker
- **Range bar**: `<canvas>` element, draw on mount and on resize
- **Shares**: query `investment_positions` table for `quantity` where `ticker = :ticker`
- **Entry point**: Add a "Volatility" button/link in the item-detail screen toolbar or action menu
