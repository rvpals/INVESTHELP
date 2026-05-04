# Invest Help

Android investment tracking app built with Kotlin, Jetpack Compose, and Material 3.

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Min SDK:** 29, Target SDK: 35
- **Architecture:** MVVM + Repository pattern
- **DI:** Hilt (KSP)
- **Database:** Room, version 16
- **Navigation:** Compose Navigation (type-safe routes)
- **Splash:** AndroidX SplashScreen API (core-splashscreen 1.0.1)
- **Charts:** Custom Canvas-drawn (pie chart, line chart) — no external chart library
- **Images:** Coil 2.7.0 for async image loading (company logos)

## Package Structure
- `data/local/` - Room database, entities, DAOs, DatabaseProvider
- `data/remote/` - StockPriceService (Yahoo Finance API integration)
- `data/repository/` - Repository interfaces and implementations
- `di/` - Hilt modules (DatabaseModule, RepositoryModule)
- `model/` - Domain models and enums
- `ui/` - Compose screens organized by feature (dashboard, account, item, transaction, transfer, simulation, sqlexplorer, performance, watchlist, help)
- `ui/components/` - Reusable UI components (CollapsibleCard, ConfirmDeleteDialog, DateRangePicker)

## Key Design Decisions
- InvestmentItem is a unique entity with `ticker` as sole primary key (no accountId); one record per ticker
- Metadata (name, type, currentPrice) stored per ticker; `updatePriceByTicker` DAO query updates the single row
- Account current value: no longer per-account (items are not tied to accounts); portfolio value is sum of all items
- Transaction table references ticker directly (not FK) — simpler model
- Transaction time is optional (nullable), totalAmount for verification, note field
- Navigation routes use ticker strings (not Long IDs) for item detail, form, and statistics
- DatabaseProvider pattern: DB opens lazily on first access
- CASCADE deletes: removing account removes associated transactions and bank transfers (items are not tied to accounts)
- Bank transfers table tracks fund transfers to investment accounts (date, amount, account, note)
- Items screen combines pie chart + STOCK/ETF tabs with Refresh All toolbar action
- Items screen: sort-by dropdown (Ticker, Total Value, Current Price) above items list; defaults to Total Value descending
- Items screen: card-style rows with alternating background colors; each row shows TickerIcon3D + ticker (bold titleSmall) with company name (smaller italic) on left, shares count + Total G/L (bold titleSmall, color-coded) on right, secondary line with Price/Value/Day G/L in muted bodySmall
- Items screen: only Edit button per row (no Delete in table); Delete available in Edit dialog
- TickerIcon3D: gradient-filled rounded-corner (10dp) box with shadow; color derived from ticker hash; white letter fallback; company logo overlay from companiesmarketcap.com CDN via Coil
- Company full name fetched from Yahoo Finance `shortName` during price refresh
- Auto-create InvestmentItem when transaction references a new ticker (defaults to Stock type, changeable via type selector)
- Dates stored as epoch days for simple SQL range queries
- Yahoo Finance v8/v10 API for live prices, historical data, and analysis info
- Global top bar: portfolio value 3D button (refreshes all prices + navigates to Dashboard) + hamburger menu (Accounts, Performance, Watch List, Settings, SQL Explorer, Help, About)
- Top bar shows spinner while refreshing prices
- Bottom nav: Dashboard, Items, Transfer, Transaction, Simulation (3D gradient icons with shadow)
- Icon3D composable: renders icons inside gradient-filled rounded boxes with drop shadow; used for bottom nav and hamburger menu icons
- Simulation time ranges: 1W, 2W, 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, MAX (grouped in Week/Month/Year rows)
- Simulation chart supports tap-to-select with tooltip (price + date)
- Dashboard market index cards: horizontal row of small cards (NASDAQ, S&P 500, Dow, Gold + more) showing price and daily change; clickable to open Yahoo Finance page for the index; customizable in Settings > Preferences
- Dashboard pie chart shows all items by ticker with shares labels inside slices
- Dashboard pie chart legend uses grid-line table with Ticker, Shares, % columns with both horizontal and vertical dividers; clicking a ticker row navigates to item detail
- Transaction form: "Analyze Price" button next to Price field opens price analysis screen
- Analyze Price screen: current price, transaction avg/max/min, historic high/low (week/month/year/max) with grid-line table (horizontal and vertical dividers) for historic prices
- Clicking a price in Analyze Price copies it back to the transaction form Price field
- Transaction form: "View" button next to Ticker opens item detail; form state preserved via rememberSaveable
- Transaction form: auto-selects first account for new transactions
- Item detail: "Analysis Info" and "Yahoo Finance" buttons on same row
- Item detail: collapsible "<TICKER> Stats" section (replaces separate statistics screen)
- Item detail: collapsible "Transactions" section
- Bank Transfers screen: total amount summary grouped by account at top
- **Image loading:** Coil 2.7.0 for company logos
- Item add/edit dialog: type selector dropdown (Stock, ETF, Bond, MutualFund, Crypto, Other); auto-fills type when selecting existing ticker
- Item detail card row 1 (big font): Total Shares, Total Value, Total Cost, Total G/L
- Item detail card row 2 (medium font): Daily G/L, Daily G/L/Share, Daily Min Price, Daily Max Price
- Item detail: dayHigh/dayLow fetched from Yahoo Finance `regularMarketDayHigh`/`regularMarketDayLow`
- CollapsibleCard: reusable component (`ui/components/CollapsibleCard.kt`) with title, pin button (persisted to SharedPreferences), HorizontalDivider between header and content, and AnimatedVisibility collapse/expand; unpinned cards default collapsed, pinned cards default expanded
- Dashboard cards (Market Indices, Daily Glance, Positions, Position Details) all use CollapsibleCard with per-card pin state persisted via `pin_card_*` keys
- Dashboard "Position Details" card: horizontally scrollable table with ticker icon, shares, current price, total cost, total value, change $ and change %; change computed as currentValue - totalCost; clickable rows navigate to item detail
- Dashboard "Daily Glance" card: "Overall Daily" section at top showing Stock and ETF total daily change in $ and %, separated by HorizontalDivider; "By Per Share" checkbox toggles sorting and display between total value and per-share change; then top 5 gainers and top 5 losers today; each row shows ticker, name, gain/loss $ and %; clickable to item detail
- Dashboard: no accounts section (removed account cards and FAB; accounts accessible via hamburger menu)
- Dashboard positions pie chart: collapsible card, legend limited to top 20 with "More" button to show all
- Top bar portfolio button: total value row shows daily change amount in parentheses (e.g. "(+$123.45)") color-coded green/red; hidden when zero
- Top bar portfolio button: second row shows (Day: ±X.XX%  All: ±X.XX%) color-coded green/red
- Settings: "Warn before delete" toggle (default: on) — when off, skips confirmation dialogs for delete actions
- Settings: "Dashboard Market Indices" section with toggles for 8 indices (NASDAQ, S&P 500, Dow, Gold, Russell 2K, Silver, Oil, Bitcoin); default: first 4 enabled; up/down arrow buttons to reorder indices; order persisted via `market_indices_order` SharedPreferences key
- Dashboard Market Indices: long-press drag-and-drop reorder on index cards; swaps on half-slot-width threshold; persists order to SharedPreferences; syncs with Settings arrow reorder
- Settings: Preferences tab scrollable to accommodate all content
- Transaction form: "Simulate" button calculates days since transaction date and opens simulation with custom range
- Simulation: supports custom day ranges from transaction simulation (auto-runs on navigation)
- SQL Explorer: accessible from hamburger menu, runs raw SQL via Room's SupportSQLiteDatabase
- SQL Explorer: detects SELECT/PRAGMA/EXPLAIN queries vs DML/DDL statements
- SQL Explorer: CSV export via FileProvider + share intent
- SQL Explorer: table browser lists all database tables with expandable column details (name, type, PK/NN indicators)
- SQL Explorer: result grid has both horizontal and vertical gridlines (VerticalDivider between columns, HorizontalDivider between rows)
- SQL Explorer: clicking a result row opens record detail dialog showing all field values untruncated
- SQL Explorer: "Open" button on each table row runs `SELECT * FROM <table>` and shows results in grid
- Settings: backup folder URI persisted to SharedPreferences; restored on ViewModel init
- Backup format v3: items no longer include accountId; v1/v2 backward compat on restore
- Transaction list: each card shows G/L = (currentPrice - pricePerShare) * numberOfShares; green for positive, red for negative
- Settings Data Management: "Import Data" section with CSV position import; column mapping dialog with 3-row preview, auto-mapping with brokerage aliases (Price→currentPrice, Description→name, Symbol→ticker, etc.), account selector, progress bar; upserts into investment_items
- CSV Import: `parseNumeric()` strips commas from numbers (handles brokerage formats like "92,150.62"); non-data rows (blank lines, FOOTNOTES) filtered out during import
- CSV Position Import: confirmation dialog before proceeding ("Position details will be refreshed with imported CSV file. Are you sure?")
- AppLog: in-memory application log (up to 200 entries) capturing price fetch results, refresh summaries, and errors
- About dialog: "Show Log" button opens scrollable log viewer (newest first) with clear button; logs include timestamps
- Item detail transactions: each card shows days since transaction date (e.g. "123d") and G/L = (currentPrice - pricePerShare) * shares; green for gain, red for loss
- Account Performance: `account_performance` table (id, accountId, totalValue, date, note) with CASCADE delete on account; unique constraint on (accountId, date)
- Account Performance: accessible from hamburger menu; add record form with account selector, total value + "Pull from App" button, optional note field, auto-timestamp
- Account Performance: edit note dialog on existing records (pencil icon); note displayed on record cards when present
- Account Performance: line chart (Canvas-drawn) with multi-account overlay; FilterChip multi-select; each account gets distinct color; pinch-to-zoom (1x–5x) with two-finger pan; tap-to-select tooltip shows account name + value + date; clipRect for zoomed data area; viewport-aware x-axis labels
- Account Performance chart: double-tap inline chart opens full-screen dialog (Dialog with usePlatformDefaultWidth=false, Scaffold with close button); full-screen chart supports zoom/pan/tap-to-select; double-tap in full-screen resets zoom
- Account Performance chart: data points with notes rendered bold (white outer circle radius 9 + colored inner circle radius 7); normal points use radius 4
- Account Performance chart: tapping a noted data point shows two-line tooltip — value/date on line 1, note text in bold on line 2; tooltip auto-sizes for both lines
- Watch List: accessible from hamburger menu; multiple named watch lists via FilterChip selector; add/rename/delete watch lists
- Watch List: add ticker with shares count and price-when-added; "Fetch" button fetches current price from Yahoo Finance
- Watch List: table shows ticker, shares, current price, added price, change $ (currentValue - costBasis), change %, added date, delete button
- Watch List: `watch_lists` table (id, name) and `watch_list_items` table (id, watchListId, ticker, shares, priceWhenAdded, addedDate) with CASCADE delete
- Database migration v10 -> v11: creates account_performance table
- Database migration v11 -> v12: adds note column to account_performance
- Database migration v12 -> v13: creates watch_lists and watch_list_items tables
- Database migration v13 -> v14: creates csv_import_mappings table
- Database migration v14 -> v15: removes accountId from investment_items, makes ticker sole PK, merges duplicate tickers by summing quantity/cost/value
- Database migration v15 -> v16: converts account_performance dateTime (epoch seconds) to date (epoch days), adds unique index on (accountId, date)
- Database version 16
- CSV Import: reusable mapping system for Transaction, Position, Performance imports; mappings persisted in `csv_import_mappings` table; supports date format options per column
- CSV Transaction Import: does NOT auto-update share counts on items; only creates item stub if ticker doesn't exist
- Settings Data Management: 3 import types (Transaction Records, Position Details, Performance Records) each with "Define Mapping" and "Start Import" buttons; shared account selector
- Transaction list: multi-select mode via long-press; contextual top bar with selection count, Select All, and Delete; bulk delete respects "Warn before delete" setting; account filter works with select-all
- LocalDateTime stored as epoch seconds (UTC) via TypeConverter; LocalDate stored as epoch days
- Help screen: accessible from hamburger menu; loads `assets/help.html` via WebView; styled HTML with dark/light theme support; covers all features with navigation overview grid, per-section guides, and tips
- About dialog: version displayed dynamically from BuildConfig (versionName + versionCode)
- Build: `buildConfig = true` enabled in build features for BuildConfig access
- Build: auto-increment versioning via `version.properties` (VERSION_MAJOR, VERSION_MINOR, VERSION_CODE); minor version and version code increment after each assembleDebug/assembleRelease

## Build
Open in Android Studio and sync Gradle. Requires JDK 17+.
Set `JAVA_HOME` to JDK 17 path if building from CLI:
```
JAVA_HOME="E:/Prog/Java/jdk-17" ./gradlew assembleRelease
```

## Versioning
- Version managed via `version.properties` at project root (VERSION_MAJOR, VERSION_MINOR, VERSION_CODE)
- After each `assembleDebug` or `assembleRelease`, minor version and version code auto-increment by 1
- `versionName` and `versionCode` in `app/build.gradle.kts` read from `version.properties`
- To bump major version: edit VERSION_MAJOR in `version.properties` and reset VERSION_MINOR to 0
- About dialog shows dynamic version via BuildConfig
