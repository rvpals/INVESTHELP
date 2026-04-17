# Invest Help

Android investment tracking app built with Kotlin, Jetpack Compose, and Material 3.

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Min SDK:** 29, Target SDK: 35
- **Architecture:** MVVM + Repository pattern
- **DI:** Hilt (KSP)
- **Database:** Room, version 12
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
- `ui/` - Compose screens organized by feature (dashboard, account, item, transaction, transfer, simulation, sqlexplorer, performance)
- `ui/components/` - Reusable UI components (CollapsibleCard, ConfirmDeleteDialog, DateRangePicker)

## Key Design Decisions
- Merged InvestmentItem + Position into single `investment_items` table with composite PK (ticker + accountId)
- Metadata (name, type, currentPrice) denormalized per-row; `updatePriceByTicker` DAO query updates all rows atomically
- Total shares computed on the fly via `SUM(quantity) WHERE ticker = ?` (no numShares syncing)
- Account current value computed from sum of item values (refreshes with live prices)
- Transaction table references ticker directly (not FK) — simpler model
- Transaction time is optional (nullable), totalAmount for verification, note field
- Navigation routes use ticker strings (not Long IDs) for item detail, form, and statistics
- DatabaseProvider pattern: DB opens lazily on first access
- CASCADE deletes: removing account removes associated items, transactions, and bank transfers
- Bank transfers table tracks fund transfers to investment accounts (date, amount, account, note)
- Items screen combines pie chart + STOCK/ETF tabs with Refresh All toolbar action
- Item cards show company logo (from companiesmarketcap.com CDN) with letter-avatar fallback
- Company full name fetched from Yahoo Finance `shortName` during price refresh
- Auto-create InvestmentItem when transaction references a new ticker (defaults to Stock type, changeable via type selector)
- Dates stored as epoch days for simple SQL range queries
- Yahoo Finance v8/v10 API for live prices, historical data, and analysis info
- Global top bar: portfolio value 3D button (refreshes all prices + navigates to Dashboard) + hamburger menu (Accounts, Performance, Settings, SQL Explorer, About)
- Top bar shows spinner while refreshing prices
- Bottom nav: Dashboard, Items, Transfer, Transaction, Simulation (3D gradient icons with shadow)
- Icon3D composable: renders icons inside gradient-filled rounded boxes with drop shadow; used for bottom nav and hamburger menu icons
- Simulation time ranges: 1W, 2W, 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, MAX (grouped in Week/Month/Year rows)
- Simulation chart supports tap-to-select with tooltip (price + date)
- Dashboard market index cards: horizontal row of small cards (NASDAQ, S&P 500, Dow, Gold + more) showing price and daily change; clickable to open Yahoo Finance page for the index; customizable in Settings > Preferences
- Dashboard pie chart shows all items by ticker with shares labels inside slices
- Dashboard pie chart legend uses grid-line table with Ticker, Shares, % columns; clicking a ticker row navigates to item detail
- Transaction form: "Analyze Price" button next to Price field opens price analysis screen
- Analyze Price screen: current price, transaction avg/max/min, historic high/low (week/month/year/max)
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
- Dashboard cards (Market Indices, Daily Glance, Positions) all use CollapsibleCard with per-card pin state persisted via `pin_card_*` keys
- Dashboard "Daily Glance" card: "Overall Daily" section at top showing Stock and ETF total daily change in $ and %, separated by HorizontalDivider; then top 5 gainers and top 5 losers today; each row shows ticker, name, gain/loss $ and %; clickable to item detail
- Dashboard: no accounts section (removed account cards and FAB; accounts accessible via hamburger menu)
- Dashboard positions pie chart: collapsible card, legend limited to top 20 with "More" button to show all
- Top bar portfolio button: total value row shows daily change amount in parentheses (e.g. "(+$123.45)") color-coded green/red; hidden when zero
- Top bar portfolio button: second row shows (Day: ±X.XX%  All: ±X.XX%) color-coded green/red
- Settings: "Warn before delete" toggle (default: on) — when off, skips confirmation dialogs for delete actions
- Settings: "Dashboard Market Indices" section with toggles for 8 indices (NASDAQ, S&P 500, Dow, Gold, Russell 2K, Silver, Oil, Bitcoin); default: first 4 enabled
- Settings: Preferences tab scrollable to accommodate all content
- Transaction form: "Simulate" button calculates days since transaction date and opens simulation with custom range
- Simulation: supports custom day ranges from transaction simulation (auto-runs on navigation)
- SQL Explorer: accessible from hamburger menu, runs raw SQL via Room's SupportSQLiteDatabase
- SQL Explorer: detects SELECT/PRAGMA/EXPLAIN queries vs DML/DDL statements
- SQL Explorer: CSV export via FileProvider + share intent
- SQL Explorer: table browser lists all database tables with expandable column details (name, type, PK/NN indicators)
- SQL Explorer: clicking a result row opens record detail dialog showing all field values untruncated
- SQL Explorer: "Open" button on each table row runs `SELECT * FROM <table>` and shows results in grid
- Settings: backup folder URI persisted to SharedPreferences; restored on ViewModel init
- Backup format v2: includes full merged entity fields; v1 backward compat on restore
- Transaction list: each card shows G/L = (currentPrice - pricePerShare) * numberOfShares; green for positive, red for negative
- Settings Data Management: "Import Data" section with CSV position import; column mapping dialog with 3-row preview, auto-mapping, account selector, progress bar; upserts into investment_items
- AppLog: in-memory application log (up to 200 entries) capturing price fetch results, refresh summaries, and errors
- About dialog: "Show Log" button opens scrollable log viewer (newest first) with clear button; logs include timestamps
- Item detail transactions: each card shows days since transaction date (e.g. "123d") and G/L = (currentPrice - pricePerShare) * shares; green for gain, red for loss
- Account Performance: `account_performance` table (id, accountId, totalValue, dateTime, note) with CASCADE delete on account
- Account Performance: accessible from hamburger menu; add record form with account selector, total value + "Pull from App" button, optional note field, auto-timestamp
- Account Performance: edit note dialog on existing records (pencil icon); note displayed on record cards when present
- Account Performance: line chart (Canvas-drawn) with multi-account overlay; FilterChip multi-select; each account gets distinct color; pinch-to-zoom (1x–5x) with two-finger pan; double-tap resets zoom; tap-to-select tooltip shows account name + value + date; clipRect for zoomed data area; viewport-aware x-axis labels
- Database migration v10 -> v11: creates account_performance table
- Database migration v11 -> v12: adds note column to account_performance
- LocalDateTime stored as epoch seconds (UTC) via TypeConverter

## Build
Open in Android Studio and sync Gradle. Requires JDK 17+.
Set `JAVA_HOME` to JDK 17 path if building from CLI:
```
JAVA_HOME="E:/Prog/Java/jdk-17" ./gradlew assembleRelease
```
