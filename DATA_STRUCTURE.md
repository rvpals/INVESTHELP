# Data Structure — Room Database (v33)

## Tables

### investment_accounts
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| name | String | |
| description | String | |
| initialValue | Double | |
| lastUpdatedOn | LocalDateTime? | nullable, stored as epoch seconds |
| lastValue | Double? | nullable |

### investment_positions
| Column | Type | Constraints |
|--------|------|-------------|
| ticker | String | PK |
| name | String | |
| type | InvestmentType | enum: Stock, ETF, Bond, MutualFund, Crypto, Other |
| currentPrice | Double | |
| quantity | Double | |
| dayGainLoss | Double | |
| value | Double | |
| dayHigh | Double | default 0.0 |
| dayLow | Double | default 0.0 |
| logo | ByteArray? | nullable BLOB — cached company logo |
| dividendRate | Double | default 0.0 — trailing annual dividend per share (Yahoo Finance) |

### investment_transactions
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| date | LocalDate | stored as epoch days |
| time | LocalTime? | nullable, stored as epoch seconds |
| action | TransactionAction | enum: Buy, Sell |
| ticker | String | indexed |
| numberOfShares | Double | |
| pricePerShare | Double | |
| totalAmount | Double | default 0.0 |
| note | String | default "" |

**Unique constraint:** (date, action, ticker, totalAmount) — prevents duplicate CSV imports

### account_performance
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| accountId | Long | FK → investment_accounts (CASCADE), indexed |
| totalValue | Double | |
| date | LocalDate | stored as epoch days |
| note | String | default "" |

**Unique constraint:** (accountId, date)

### change_history
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| date | LocalDate | unique, stored as epoch days |
| etfValue | Double | |
| stockValue | Double | |
| totalValue | Double | |
| dailyChangeEtf | Double | default 0.0 |
| dailyChangeStock | Double | default 0.0 |
| dailyChangeTotal | Double | default 0.0 |

### watch_lists
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| name | String | |

### watch_list_items
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| watchListId | Long | FK → watch_lists (CASCADE), indexed |
| ticker | String | |
| shares | Double | |
| priceWhenAdded | Double | |
| addedDate | LocalDate | stored as epoch days |
| reminderDateTime | LocalDateTime? | nullable, stored as epoch seconds |
| reminderMessage | String? | nullable |

### csv_import_mappings
| Column | Type | Constraints |
|--------|------|-------------|
| importType | String | PK |
| mappingsJson | String | JSON: column index → field name |
| dateFormatJson | String | default "", JSON: field → date format |

### csv_named_mappings
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| name | String | |
| importType | String | |
| mappingsJson | String | JSON: column index → field name |
| dateFormatJson | String | default "" |

### definitions
| Column | Type | Constraints |
|--------|------|-------------|
| term | String | PK |
| definition | String | |

### sql_library
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| name | String | |
| description | String | |
| category | String | |
| sql | String | |

### ai_library
| Column | Type | Constraints |
|--------|------|-------------|
| id | Long | PK, auto-generated |
| name | String | |
| description | String | |
| promptText | String | |

### volatility_cache
| Column | Type | Constraints |
|--------|------|-------------|
| ticker | String | PK |
| annualizedVolPct | Double | |
| dailyStdDevPct | Double | |
| volatilityLabel | String | Low / Moderate / High / Very High |
| low52w | Double | |
| high52w | Double | |
| rangePositionPct | Double | |
| sampleCount | Int | |
| calculatedAt | Long | epoch seconds |

### correlation_cache
Singleton row (id=1).

| Column | Type | Constraints |
|--------|------|-------------|
| id | Int | PK (always 1) |
| tickersJson | String | JSON array of ticker strings |
| matrixJson | String | JSON 2D array of correlation coefficients |
| marketCorrelationJson | String | JSON: ticker → S&P 500 correlation |
| failedTickersJson | String | JSON array of tickers that failed to fetch |
| calculatedAt | Long | epoch seconds |

### sharpe_ratio_cache
Singleton row (id=1).

| Column | Type | Constraints |
|--------|------|-------------|
| id | Int | PK (always 1) |
| riskFreeRate | Double | |
| lookbackDays | Int | |
| sharpeRatio | Double | |
| annualizedReturn | Double | |
| annualizedVolatility | Double | |
| alignedTradingDays | Int | |
| meanDailyReturn | Double | |
| dailyRiskFreeRateUsed | Double | |
| calculationDate | String | ISO date string |
| tickerDetailsJson | String | JSON array of per-ticker breakdown |
| portfolioReturnSeriesJson | String | JSON array of daily return data points |
| skippedTickersJson | String | JSON array of skipped tickers |
| skipReasonsJson | String | JSON: ticker → skip reason |
| insufficientDataReason | String | |
| calculatedAt | Long | epoch seconds |

## Relationships

```
investment_accounts ──< account_performance  (CASCADE delete)
watch_lists         ──< watch_list_items     (CASCADE delete)
```

Positions and transactions are not tied to accounts. Ticker is the shared key between `investment_positions` and `investment_transactions` (no FK constraint). Singleton cache tables (correlation_cache, sharpe_ratio_cache) always use id=1 — INSERT OR REPLACE overwrites.

## Enums

- **InvestmentType:** Stock, ETF, Bond, MutualFund, Crypto, Other
- **TransactionAction:** Buy, Sell

## Migration History

| Migration | Change |
|-----------|--------|
| v10 → v11 | Creates account_performance table |
| v11 → v12 | Adds note column to account_performance |
| v12 → v13 | Creates watch_lists and watch_list_items tables |
| v13 → v14 | Creates csv_import_mappings table |
| v14 → v15 | Removes accountId from investment_items; ticker becomes sole PK; merges duplicate tickers |
| v15 → v16 | Recreates account_performance: dateTime → date (epoch days); unique index on (accountId, date) |
| v16 → v17 | Drops bank_transfers table (feature removed) |
| v17 → v18 | Creates change_history table |
| v18 → v19 | Adds reminderDateTime, reminderMessage to watch_list_items |
| v19 → v20 | Adds logo BLOB column to investment_items |
| v20 → v21 | Removes accountId from investment_transactions (recreates table) |
| v21 → v22 | Adds dailyChangeEtf, dailyChangeStock, dailyChangeTotal to change_history |
| v22 → v23 | Creates csv_named_mappings table |
| v23 → v24 | Adds lastUpdatedOn, lastValue columns to investment_accounts |
| v24 → v25 | Creates definitions table |
| v25 → v26 | Adds unique index on investment_transactions (date, action, ticker, totalAmount) |
| v26 → v27 | Renames investment_items → investment_positions; removes cost and totalGainLoss columns |
| v27 → v28 | Creates sql_library table |
| v28 → v29 | Creates ai_library table with 3 seed prompts |
| v29 → v30 | Adds dividendRate column to investment_positions |
| v30 → v31 | Creates volatility_cache table |
| v31 → v32 | Creates correlation_cache table |
| v32 → v33 | Creates sharpe_ratio_cache table |
