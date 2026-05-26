# Invest Help - Architecture

## Overview

The app follows **MVVM + Repository** pattern with **Hilt** dependency injection and **Jetpack Compose** for the UI layer.

```
┌─────────────────────────────────────────┐
│              UI Layer                    │
│  Compose Screens + ViewModels           │
│  (ui/ package)                          │
├─────────────────────────────────────────┤
│           Repository Layer              │
│  Repository interfaces + implementations│
│  (data/repository/)                     │
├─────────────────────────────────────────┤
│            Data Layer                   │
│  Room DAOs + Entities │ Remote Services │
│  (data/local/)        │ (data/remote/)  │
└─────────────────────────────────────────┘
```

## Package Structure

```
com.investhelp.app/
├── MainActivity.kt              # Single activity, top bar, bottom nav, navigation host, auto-backup on exit
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt       # Room database (version 27) with all DAOs and migrations
│   │   ├── DatabaseProvider.kt  # Lazy database initialization pattern
│   │   ├── dao/                 # Room DAO interfaces
│   │   └── entity/              # Room entity classes
│   ├── remote/
│   │   └── StockPriceService.kt # Yahoo Finance v8/v10 API client
│   └── repository/
│       ├── *Repository.kt       # Repository interfaces
│       └── *RepositoryImpl.kt   # Repository implementations
├── di/
│   ├── DatabaseModule.kt        # Hilt module for database and DAOs
│   └── RepositoryModule.kt      # Hilt module for repositories
├── model/
│   └── *.kt                     # Domain models and enums
└── ui/
    ├── account/                 # AccountListScreen, AccountDetailScreen, AccountViewModel
    ├── components/              # CollapsibleCard, ConfirmDeleteDialog, DateRangePicker
    ├── dashboard/               # DashboardScreen, DashboardViewModel
    ├── help/                    # HelpScreen (WebView)
    ├── item/                    # ItemListScreen, ItemDetailScreen, ItemViewModel
    ├── navigation/              # NavRoutes, NavGraph
    ├── performance/             # AccountPerformanceScreen, AccountPerformanceViewModel
    ├── settings/                # SettingsScreen, SettingsViewModel
    ├── simulation/              # SimulationScreen, SimulationViewModel
    ├── sqlexplorer/             # SqlExplorerScreen, SqlExplorerViewModel
    ├── theme/                   # Theme.kt, AppTheme.kt, ThemePreferences.kt, Color.kt, Type.kt
    ├── transaction/             # TransactionListScreen, TransactionFormScreen, AnalyzePriceScreen
    └── watchlist/               # WatchListScreen, WatchListViewModel
```

## Database

### Room Database (version 27)

**Entities:**
| Table | Primary Key | Description |
|-------|-------------|-------------|
| `investment_accounts` | `id` (auto) | Brokerage accounts (with lastValue/lastUpdatedOn) |
| `investment_positions` | `ticker` | Holdings (one per ticker, with cached logo BLOB) |
| `investment_transactions` | `id` (auto) | Buy/sell records |
| `account_performance` | `id` (auto) | Account value snapshots |
| `watch_lists` | `id` (auto) | Named watch list groups |
| `watch_list_items` | `id` (auto) | Watch list ticker entries (with optional reminder) |
| `csv_import_mappings` | `importType` | Persistent CSV column maps |
| `csv_named_mappings` | `id` (auto) | Named/reusable CSV mapping profiles |
| `change_history` | `id` (auto) | Daily portfolio value snapshots (ETF/Stock/Total) + daily change values |
| `definitions` | `term` | Metric definitions for analysis info popups |

**Key Relationships:**
- Transactions reference tickers directly (no FK, account-independent)
- Account performance references accounts by FK (CASCADE delete), unique (accountId, date)
- Watch list items reference watch lists by FK (CASCADE delete)
- Investment items are independent (ticker-only PK, not tied to accounts)

**Migration History:**
- v4->v5: Drop/recreate positions table
- v5->v6: Recreate transactions with ticker reference
- v8->v9: Merge positions into investment_items
- v9->v10: Add dayHigh/dayLow columns
- v10->v11: Create account_performance table
- v11->v12: Add note column to account_performance
- v12->v13: Create watch_lists and watch_list_items tables
- v13->v14: Create csv_import_mappings table
- v14->v15: Remove accountId from items, ticker-only PK, merge duplicates
- v15->v16: Convert performance dateTime to date, add unique index
- v16->v17: Drop bank_transfers table (feature removed)
- v17->v18: Create change_history table with unique date index
- v18->v19: Add reminderDateTime and reminderMessage columns to watch_list_items
- v19->v20: Add logo BLOB column to investment_items (cached company logo)
- v20->v21: Remove accountId from transactions (recreate table without FK/index)
- v21->v22: Add dailyChangeEtf, dailyChangeStock, dailyChangeTotal columns to change_history
- v22->v23: Create csv_named_mappings table for named mapping profiles
- v23->v24: Add lastUpdatedOn and lastValue columns to investment_accounts
- v24->v25: Add definitions table for metric definitions
- v25->v26: Add unique index on investment_transactions (date, action, ticker, totalAmount)
- v26->v27: Rename investment_items to investment_positions, remove cost and totalGainLoss columns

### DatabaseProvider
Lazy initialization pattern: database opens on first access, not at app startup.

## Remote Data

### Yahoo Finance Integration (StockPriceService)
- **v8 Chart API**: Live prices, historical data, day high/low, price history (flexible range/interval)
- **v10 QuoteSummary API**: Sector, industry, P/E, EPS, 52-week range, business summary
- No API key required (undocumented public endpoints)
- `fetchPriceHistory(ticker, range, interval)`: flexible chart queries (e.g., "1d"/"1h" for hourly, "60d"/"1d" for daily, "13mo"/"1mo" for monthly, "15y"/"1mo" for yearly)
- Historical data uses weekly interval for 5Y+ ranges in simulation

## UI Components

### CollapsibleCard
Reusable composable with:
- Title, pin button (SharedPreferences persistence), expand/collapse toggle
- HorizontalDivider between header and content
- AnimatedVisibility for smooth collapse/expand
- Pinned cards default expanded; unpinned default collapsed

### TickerIcon3D
Gradient-filled rounded-corner box with shadow:
- Color derived from ticker hash
- Company logo from cached BLOB in database (fetched from multiple CDN sources during refresh or on items screen load); falls back to network URL via Coil
- White letter fallback when logo unavailable

### Icon3D
Gradient-filled rounded boxes with drop shadow for bottom nav and menu icons.

### Charts (Custom Canvas)
- **Pie chart**: Dashboard positions allocation with labels inside slices
- **Line chart**: Account performance with multi-account overlay, zoom, pan, tooltips
- **Price history line chart**: Item detail Price History tab with zoom, pan, tap-to-select tooltips
- **Investing performance line chart**: Item detail Transactions tab with zoom, pan, price labels on data points, tap-to-select tooltips

## Data Storage Patterns

- **Dates**: `LocalDate` stored as epoch days for simple SQL range queries
- **DateTimes**: `LocalDateTime` stored as epoch seconds (UTC) via TypeConverter
- **Preferences**: SharedPreferences for pin states, market index order, filter/sort selections, backup folder URI, last refreshed timestamp, auto-refresh settings, last auto backup timestamp
- **Background Work**: WorkManager for periodic auto-refresh with HiltWorkerFactory; foreground service type DATA_SYNC
- **Backup**: JSON format (v4 current; v1/v2/v3 backward compatible); auto-backup triggers on `onStop()` with 30-minute cooldown guard

## Navigation

Type-safe Compose Navigation with ticker strings as route parameters (not Long IDs).

Routes defined in `NavRoutes.kt`; navigation graph in `NavGraph.kt`.
