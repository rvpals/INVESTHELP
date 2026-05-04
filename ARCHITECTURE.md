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
├── MainActivity.kt              # Single activity, top bar, bottom nav, navigation host
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt       # Room database (version 16) with all DAOs and migrations
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
    ├── theme/                   # Theme.kt, Color.kt, Type.kt
    ├── transaction/             # TransactionListScreen, TransactionFormScreen, AnalyzePriceScreen
    ├── transfer/                # BankTransferListScreen, BankTransferFormScreen
    └── watchlist/               # WatchListScreen, WatchListViewModel
```

## Database

### Room Database (version 16)

**Entities:**
| Table | Primary Key | Description |
|-------|-------------|-------------|
| `investment_accounts` | `id` (auto) | Brokerage accounts |
| `investment_items` | `ticker` | Holdings (one per ticker) |
| `investment_transactions` | `id` (auto) | Buy/sell records |
| `bank_transfers` | `id` (auto) | Fund transfer records |
| `account_performance` | `id` (auto) | Account value snapshots |
| `watch_lists` | `id` (auto) | Named watch list groups |
| `watch_list_items` | `id` (auto) | Watch list ticker entries |
| `csv_import_mappings` | `importType` | Persistent CSV column maps |

**Key Relationships:**
- Transactions reference accounts by FK (CASCADE delete)
- Bank transfers reference accounts by FK (CASCADE delete)
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

### DatabaseProvider
Lazy initialization pattern: database opens on first access, not at app startup.

## Remote Data

### Yahoo Finance Integration (StockPriceService)
- **v8 Chart API**: Live prices, historical data, day high/low
- **v10 QuoteSummary API**: Sector, industry, P/E, EPS, 52-week range, business summary
- No API key required (undocumented public endpoints)
- Historical data uses weekly interval for 5Y+ ranges

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
- Company logo overlay via Coil (companiesmarketcap.com CDN)
- White letter fallback when logo unavailable

### Icon3D
Gradient-filled rounded boxes with drop shadow for bottom nav and menu icons.

### Charts (Custom Canvas)
- **Pie chart**: Dashboard positions allocation with labels inside slices
- **Line chart**: Account performance with multi-account overlay, zoom, pan, tooltips

## Data Storage Patterns

- **Dates**: `LocalDate` stored as epoch days for simple SQL range queries
- **DateTimes**: `LocalDateTime` stored as epoch seconds (UTC) via TypeConverter
- **Preferences**: SharedPreferences for pin states, market index order, filter/sort selections, backup folder URI
- **Backup**: JSON format (v3 current; v1/v2 backward compatible)

## Navigation

Type-safe Compose Navigation with ticker strings as route parameters (not Long IDs).

Routes defined in `NavRoutes.kt`; navigation graph in `NavGraph.kt`.
