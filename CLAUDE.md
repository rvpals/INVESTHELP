# Invest Help

Investment tracking app with Android native + PWA web app.

## Project Structure
- `ANDROID_APP/` - Android native app (Kotlin + Jetpack Compose)
- `PWA/` - Progressive Web App (Node.js + Express + vanilla JS)
- `*.md` files at root - Project documentation

## Android Tech Stack (ANDROID_APP/)
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Min SDK:** 29, Target SDK: 35
- **Architecture:** MVVM + Repository pattern
- **DI:** Hilt (KSP)
- **Database:** Room, version 30
- **Navigation:** Compose Navigation (type-safe routes)
- **Splash:** AndroidX SplashScreen API (core-splashscreen 1.0.1)
- **Charts:** Custom Canvas-drawn (pie chart, line chart) — no external chart library
- **Images:** Coil 2.7.0 for async image loading (company logos)

## Android Package Structure (ANDROID_APP/app/src/main/java/com/investhelp/app/)
- `data/local/` - Room database, entities, DAOs, DatabaseProvider
- `data/remote/` - StockPriceService (Yahoo Finance API integration)
- `data/repository/` - Repository interfaces and implementations
- `di/` - Hilt modules (DatabaseModule, RepositoryModule)
- `model/` - Domain models and enums
- `ui/` - Compose screens organized by feature (dashboard, account, item, transaction, simulation, sqlexplorer, performance, watchlist, help)
- `ui/components/` - Reusable UI components (CollapsibleCard, ConfirmDeleteDialog, DateRangePicker)

## Key Design Decisions
- InvestmentItem entity stored in `investment_positions` table with `ticker` as sole primary key (no accountId); one record per ticker
- Metadata (name, type, currentPrice) stored per ticker; `updatePriceByTicker` DAO query updates the single row
- Account current value: no longer per-account (items are not tied to accounts); portfolio value is sum of all items
- Transaction table references ticker directly (no FK, no accountId) — simpler model
- Transaction time is optional (nullable), totalAmount for verification, note field
- Transaction table: unique constraint on (date, action, ticker, totalAmount) to prevent duplicate CSV imports; `insertTransactionIfNotExists()` uses IGNORE conflict strategy
- Navigation routes use ticker strings (not Long IDs) for item detail, form, and statistics
- DatabaseProvider pattern: DB opens lazily on first access
- CASCADE deletes: removing account removes associated performance records (transactions and items are not tied to accounts)
- Items screen: 4 tabs in a flat Row layout — STOCK (ShowChart), ETF (TrendingUp), Analysis (Analytics), Dividend (Payments); all tabs always visible with equal width, selected tab highlighted with primaryContainer background; STOCK/ETF tabs have pie chart + item list; Analysis tab has Stock and ETF exploding pie chart cards; Dividend tab has total annual income summary + Stock/ETF dividend cards with exploding pie charts and sortable tables
- Items screen: Refresh All toolbar action
- Items screen: sort-by dropdown (Ticker, Total Value, Current Price) above items list; defaults to Total Value descending
- Items screen: brokerage-style card rows with thin dividers; each row shows TickerIcon3D + ticker (bold) + uppercase company name on left, current price with day change $ and % below, total position value on right with daily gain/loss badge (green/red chip)
- Items screen: annual dividend income line ("Div: $X.XX/yr") shown below price when dividendRate > 0; blue color (#1565C0)
- Items screen Dividend tab: "Total Annual Dividend Income" summary card at top (blue, bold); separate "Stock" and "ETF" DividendPieCard sections; each card has exploding pie chart (largest slice offset), color-coded legend with % per ticker, sort buttons (Annual Dividend, Div/Share, Ticker, Shares), and data table (Ticker, Shares, Div/Share, Annual, %); only shows tickers with dividendRate > 0 and quantity > 0; clickable rows navigate to item detail
- Items screen: only Edit button per row (no Delete in table); Delete available in Edit dialog
- TickerIcon3D: gradient-filled rounded-corner (10dp) box with shadow; color derived from ticker hash; white letter fallback; company logo overlay from companiesmarketcap.com CDN via Coil
- Company full name fetched from Yahoo Finance `shortName` during price refresh
- Auto-create InvestmentItem when transaction references a new ticker (defaults to Stock type, changeable via type selector)
- Dates stored as epoch days for simple SQL range queries
- Yahoo Finance v8/v10 API for live prices, historical data, and analysis info
- Global top bar: portfolio value 3D button (refreshes all prices + navigates to Dashboard) + Watch List icon button (star, purple) + hamburger menu (Accounts, Performance, Simulation, Settings, SQL Explorer, Help, About)
- Top bar shows spinner while refreshing prices; refresh status bar below top bar shows "Updating [TICKER]" with price, change $, change % (color-coded, auto-hides on completion)
- Bottom nav: Dashboard, Positions, Transaction (3D gradient icons with shadow)
- Icon3D composable: renders icons inside gradient-filled rounded boxes with drop shadow; used for bottom nav and hamburger menu icons
- Simulation time ranges: 1W, 2W, 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, MAX (grouped in Week/Month/Year rows)
- Simulation chart supports tap-to-select with tooltip (price + date)
- Dashboard market index cards: horizontal row of small cards (NASDAQ, S&P 500, Dow, Gold + more) showing price and daily change; clickable to open Yahoo Finance page for the index; customizable in Settings > Preferences
- Dashboard pie chart shows all items by ticker with shares labels inside slices
- Dashboard pie chart legend uses grid-line table with Ticker, Shares, % columns with both horizontal and vertical dividers; clicking a ticker row navigates to item detail
- Transaction form: "Analyze Price" button next to Price field opens price analysis screen
- Analyze Price screen: current price, transaction avg/max/min, historic high/low (week/month/year/YTD/max) with grid-line table (horizontal and vertical dividers) for historic prices
- Clicking a price in Analyze Price copies it back to the transaction form Price field
- Transaction form: "View" button next to Ticker opens item detail; form state preserved via rememberSaveable
- Transaction form: no account field (transactions are not tied to accounts)
- Item detail: ScrollableTabRow with 3 tabs: "Details", "Price History", "Transactions"
- Item detail Details tab: header card (ticker info, prices, daily stats, 0-share indicator), "Analysis Info" collapsible card (with pin), "News on <TICKER>" collapsible card (with pin, configurable article count)
- Item detail Price History tab: radio button timeframe selector (Hourly, Daily, Monthly, Yearly) with hint text below showing meaning; Hourly = today's market hours with interval selector (Every Hour/30m/15m/5m/1m in two rows), Daily = last 60 days, Monthly = last 13 months, Yearly = last 15 years; line chart with pinch-to-zoom/pan/tap-to-select; summary cards (Average, Max, Min) above grid table of prices
- Item detail Analysis Info card: auto-fetches Yahoo Finance quoteSummary on screen load; displays Key Metrics, Price Range, Financials, About sections; clickable metric labels show definition popup
- Item detail News card: fetches news from Yahoo Finance search API; shows title, publisher, time ago; tap opens in browser; max article count from settings (default 5)
- Item detail Transactions tab: "Transactions & Stats" collapsible panel (default expanded) combining date range filter, buy/sell statistics, and per-transaction G/L cards
- Item detail Transactions tab: delete button (X) on each transaction card with confirmation dialog (respects "Warn before delete" setting)
- Item detail Transactions tab: "Investing Performance for <TICKER>" collapsible panel (default expanded); fetches Yahoo Finance prices 1 day before/after each transaction; current price added as last data point (tertiary color); line chart with price labels on each point, pinch-to-zoom (1x–5x) with pan, tap-to-select tooltip, double-tap to reset; bold red transaction dots vs gray market dots vs tertiary current price dot; data table with highlighted transaction rows and alternating row colors
- Item detail Investing Performance chart: fullscreen view button (opens chart in full-screen dialog at 400dp height)
- Item detail Investing Performance chart: save-to-PNG button (renders chart as 1200x600 bitmap, saves to Pictures/InvestHelp/)
- All tables app-wide: alternating row background color (surfaceVariant alpha 0.3f on odd rows) for readability; HorizontalDivider uses `outline` color (not `outlineVariant`) for visible row separation
- **Image loading:** Coil 2.7.0 for company logos; logos cached as BLOB in investment_positions table, fetched from multiple CDN sources (companiesmarketcap.com, parqet.com, iexcloud) during price refresh or on items screen load (if logo is null), UI falls back to network URL if not cached
- Item add/edit dialog: type selector dropdown (Stock, ETF, Bond, MutualFund, Crypto, Other); auto-fills type when selecting existing ticker
- Item Form screen: handles both existing positions (edit) and new tickers (create); auto-fetches full Yahoo data (price, name, dayHigh, dayLow, previousClose, logo) for non-existent tickers; auto-detects type (Stock/ETF/MutualFund/Crypto) from Yahoo `quoteType`; Save requires non-blank ticker (0 shares allowed); `itemLoaded` flag gates LaunchedEffects to prevent race condition
- Item detail: info button (ℹ) in top bar opens full Yahoo Finance report dialog with all available data (market data, valuation, financials, key stats, profile, events, analyst recs, fund profile/holdings for ETFs)
- Item detail card row 1 (big font): Total Shares, Total Value
- Item detail card row 2 (medium font): Daily G/L, Daily G/L/Share, Daily Min Price, Daily Max Price
- Item detail card row 3 (conditional, shown when dividendRate > 0): Dividend/Share, Annual Dividend (dividendRate × quantity); blue color (#1565C0)
- Item detail: dayHigh/dayLow fetched from Yahoo Finance `regularMarketDayHigh`/`regularMarketDayLow`
- Item detail: dividendRate fetched from Yahoo Finance `trailingAnnualDividendRate` (v8 chart meta on price refresh, v10 summaryDetail on analysis fetch); stored in investment_positions table
- CollapsibleCard: reusable component (`ui/components/CollapsibleCard.kt`) with title, pin button (persisted to SharedPreferences), HorizontalDivider between header and content, and AnimatedVisibility collapse/expand; unpinned cards default collapsed, pinned cards default expanded
- Dashboard cards (Market Indices, Daily Glance, Positions, Position Details) all use CollapsibleCard with per-card pin state persisted via `pin_card_*` keys
- Dashboard "Position Details" card: horizontally scrollable table with ticker icon, shares, current price, total value; clickable rows navigate to item detail
- Dashboard "Daily Glance" card: "Overall Daily" section at top showing Stock and ETF total daily change in $ and %, separated by HorizontalDivider; "By Per Share" checkbox toggles sorting and display between total value and per-share change; then top 5 gainers and top 5 losers today; each row shows ticker, name, gain/loss $ and %; clickable to item detail
- Dashboard: no accounts section (removed account cards and FAB; accounts accessible via hamburger menu)
- Dashboard positions pie chart: collapsible card, legend limited to top 20 with "More" button to show all
- Top bar portfolio button: total value row shows daily change amount in parentheses (e.g. "(+$123.45)") color-coded green/red; hidden when zero
- Top bar portfolio button: second row shows (Day: ±X.XX%) color-coded green/red
- Dashboard: "Portfolio Summary" collapsible card with pin persistence; total value change in headlineLarge (3x bigger) bold centered; Day/All percentages in bodyMedium centered below; mini line chart of total_value from change_history (shown when 2+ records); click mini chart opens full-screen Change History dialog with zoomable multi-series chart (Total/ETF/Stock lines) + grid data table (Date, ETF, Stock, Total columns)
- Settings: "Warn before delete" toggle (default: on) — when off, skips confirmation dialogs for delete actions
- Settings: "Max # of News articles on ticker" dropdown (5, 10, 20; default: 5) — controls how many news articles to fetch and display in Item Detail News card
- Settings: "Dashboard Market Indices" section with toggles for 8 indices (NASDAQ, S&P 500, Dow, Gold, Russell 2K, Silver, Oil, Bitcoin); default: first 4 enabled; up/down arrow buttons to reorder indices; order persisted via `market_indices_order` SharedPreferences key
- Dashboard Market Indices: long-press drag-and-drop reorder on index cards; swaps on half-slot-width threshold; persists order to SharedPreferences; syncs with Settings arrow reorder
- Settings: Preferences tab scrollable to accommodate all content; "Themes" and "Dashboard Market Indices" sections in collapsible panels (default collapsed)
- Transaction form: "Simulate" button calculates days since transaction date and opens simulation with custom range
- Simulation: supports custom day ranges from transaction simulation (auto-runs on navigation)
- Simulation: "Scenario Simulation" collapsible card — enter shares, ticker, and buy date to calculate hypothetical gain/loss at today's price via Yahoo Finance historical lookup
- SQL Explorer: accessible from hamburger menu; SQL text box + Run button (navigates to SQL Result screen) + Save SQL to Library button
- SQL Explorer: table browser lists all database tables with expandable column details (name, type, PK/NN indicators); clicking table/column name inserts into SQL text box
- SQL Explorer: "SQL Library" collapsible card with saved queries, category filter, name search, Run/Delete per entry
- SQL Explorer: "Open" button on each table row opens SQL Result screen with `SELECT * FROM <table>`
- SQL Result screen: full screen with editable SQL query card, result grid (vertical+horizontal scroll, clickable cells for full-screen detail), Export to CSV button; auto-executes on load
- SQL Library: `sql_library` table (id, name, description, category, sql) for persisting reusable queries
- Settings: backup folder URI persisted to SharedPreferences; restored on ViewModel init
- Backup format v5: exports all 10 tables (accounts, positions, transactions, performance records, watch lists + items, change history, definitions, SQL library, AI library); v1/v2/v3/v4 backward compat on restore; compatible between Android app and PWA
- Transaction list: each card shows G/L = (currentPrice - pricePerShare) * numberOfShares; green for positive, red for negative
- Settings Data Management: "Import Data" section with CSV position import; column mapping dialog with 3-row preview, auto-mapping with brokerage aliases (Price→currentPrice, Description→name, Symbol→ticker, etc.), account selector, progress bar; upserts into investment_items
- CSV Import: `parseNumeric()` strips commas from numbers (handles brokerage formats like "92,150.62"); non-data rows (blank lines, FOOTNOTES) filtered out during import
- CSV Position Import: confirmation dialog before proceeding ("Position details will be refreshed with imported CSV file. Are you sure?")
- CSV Position Import: "Define Mapping" opens full-screen mapping editor with Save, Save As (named), and Load buttons; saved mappings stored in `csv_named_mappings` table
- CSV Position Import: "Start Import" prompts to select a saved mapping (or default active mapping), then imports with detailed per-ticker log showing NEW/UPDATED/SKIPPED status and field changes
- AppLog: in-memory application log (up to 200 entries) capturing price fetch results, refresh summaries, and errors
- About dialog: "Show Log" button opens scrollable log viewer (newest first) with clear button; logs include timestamps
- Item detail transactions: each card shows days since transaction date (e.g. "123d") and G/L = (currentPrice - pricePerShare) * shares; green for gain, red for loss
- Account Performance: `account_performance` table (id, accountId, totalValue, date, note) with CASCADE delete on account; unique constraint on (accountId, date)
- Account Performance: accessible from hamburger menu; add record form with account selector, total value + "Pull" button + "Recent" button (loads latest record value for account), optional note field, auto-timestamp
- Account Performance: mini chart (150dp) in Add Record section shows selected account's history when 2+ records exist
- Account Performance: "Chart Data" collapsible panel below chart shows tabular data (Account, Date, Value) for chart-selected accounts; sorted by account name then date
- Account Performance: edit note dialog on existing records (pencil icon); note displayed on record cards when present
- Account Performance: Records list uses grid table with horizontal/vertical gridlines, header row (Account, Date, Value, actions), and alternating row colors
- Account Performance: line chart (Canvas-drawn) with multi-account overlay; FilterChip multi-select; each account gets distinct color; pinch-to-zoom (1x–5x) with two-finger pan; tap-to-select tooltip shows account name + value + date; clipRect for zoomed data area; viewport-aware x-axis labels
- Account Performance chart: double-tap inline chart opens full-screen dialog (Dialog with usePlatformDefaultWidth=false, Scaffold with close button); full-screen chart supports zoom/pan/tap-to-select; double-tap in full-screen resets zoom
- Account Performance chart: data points with notes rendered bold (white outer circle radius 9 + colored inner circle radius 7); normal points use radius 4
- Account Performance chart: tapping a noted data point shows two-line tooltip — value/date on line 1, note text in bold on line 2; tooltip auto-sizes for both lines
- Watch List: accessible from hamburger menu; each watch list displayed as its own collapsible panel (all visible simultaneously); add/rename/delete watch lists; ticker text clickable to navigate to Item Detail
- Watch List: add ticker with shares count and price-when-added; "Fetch" button fetches current price from Yahoo Finance
- Watch List: table shows ticker, shares, current price, added price, change $ (currentValue - costBasis), change %, added date, delete button
- Watch List: `watch_lists` table (id, name) and `watch_list_items` table (id, watchListId, ticker, shares, priceWhenAdded, addedDate, reminderDateTime, reminderMessage) with CASCADE delete
- Watch List Reminders: optional reminder per watch list item (date/time + message); scheduled via AlarmManager; notification via BroadcastReceiver; bell icon in table (colored when active); set during add or edit via dedicated dialog with date picker, time picker, and message field; "Clear" option to remove existing reminder
- Database migration v10 -> v11: creates account_performance table
- Database migration v11 -> v12: adds note column to account_performance
- Database migration v12 -> v13: creates watch_lists and watch_list_items tables
- Database migration v13 -> v14: creates csv_import_mappings table
- Database migration v14 -> v15: removes accountId from investment_items, makes ticker sole PK, merges duplicate tickers by summing quantity/cost/value
- Database migration v15 -> v16: converts account_performance dateTime (epoch seconds) to date (epoch days), adds unique index on (accountId, date)
- Database migration v16 -> v17: drops bank_transfers table (feature removed)
- Database migration v17 -> v18: creates change_history table (id, date, etfValue, stockValue, totalValue) with unique index on date
- Database migration v18 -> v19: adds reminderDateTime and reminderMessage columns to watch_list_items
- Database migration v19 -> v20: adds logo BLOB column to investment_items for cached company logo
- Database migration v20 -> v21: removes accountId from investment_transactions (recreates table without FK/index)
- Database migration v21 -> v22: adds dailyChangeEtf, dailyChangeStock, dailyChangeTotal columns to change_history
- Database migration v22 -> v23: creates csv_named_mappings table (id, name, importType, mappingsJson, dateFormatJson)
- Database migration v23 -> v24: adds lastUpdatedOn (INTEGER, epoch seconds) and lastValue (REAL) columns to investment_accounts
- Database migration v24 -> v25: creates definitions table (term PK, definition TEXT) for metric definition popups
- Database migration v25 -> v26: adds unique index on investment_transactions (date, action, ticker, totalAmount) to prevent duplicate CSV imports
- Database migration v26 -> v27: renames investment_items to investment_positions, removes cost and totalGainLoss columns
- Database migration v27 -> v28: creates sql_library table (id, name, description, category, sql) for saved SQL queries
- Database migration v28 -> v29: creates ai_library table (id, name, description, promptText) with 3 seed prompts for AI-powered ticker analysis
- Database migration v29 -> v30: adds dividendRate REAL column to investment_positions (trailing annual dividend per share from Yahoo Finance)
- Database version 30
- WatchListDao: `getAllWatchListsSnapshot()`, `getAllItemsSnapshot()`, `deleteAllItems()`, `deleteAllLists()` for backup
- DefinitionDao: `getAllDefinitionsSnapshot()` for backup
- SqlLibraryDao: `getAllSnapshot()`, `deleteAll()` for backup
- AiLibraryDao: `getAllSnapshot()`, `deleteAll()` for backup
- Change History: `change_history` table records daily portfolio values by type (ETF, Stock, Total) plus daily change values (dailyChangeEtf, dailyChangeStock, dailyChangeTotal); one row per day, overwritten on re-refresh
- Change History dialog: "Change Value This Week So Far" summary card above data table showing sum of daily changes for ETF, Stock, and Total since Monday; color-coded green/red
- Settings: "Auto Update Change History when refresh" toggle (default: off) — when on, automatically records ETF/Stock/Total values to change_history after price refresh; overwrites existing entry for today
- Settings: "Auto Refresh All" toggle (default: off) — when on, shows interval dropdown (5 min, 30 min, 1 hr, 5 hr, Market close daily); uses WorkManager periodic background work with foreground notification; completion notification shows ticker count; respects "Auto Update Change History" setting
- Settings Data Management: "Automatic Back Up when quitting" toggle (default: off) — when on, automatically exports backup JSON to selected backup folder on app backgrounding (onStop with 30-minute cooldown guard); uses same format as Export Data
- Settings Data Management: "Last Auto Backup completed on" note displayed below auto backup options showing date/time of most recent successful auto backup
- Settings Data Management: "Number of automatic backup to keep" (default: 10, shown when auto-backup enabled) — oldest `invest_help_backup_*.json` files deleted when count exceeds limit before writing new backup
- CSV Import: reusable mapping system for Transaction, Position, Performance imports; mappings persisted in `csv_import_mappings` table; supports date format options per column
- CSV Transaction Import: does NOT auto-update share counts on items; only creates item stub if ticker doesn't exist
- CSV Performance Import: account name mapping dialog — when accountName column is mapped, scans CSV for unique account names and shows mapping dialog where each CSV name maps to an existing app account via dropdown; pre-selects via case-insensitive match; resolution order: (1) explicit user mapping, (2) case-insensitive name match, (3) default account
- Settings Data Management: 3 import types (Transaction Records, Position Details, Performance Records) each with "Define Mapping" and "Start Import" buttons; shared account selector
- Transaction list: multi-select mode via long-press; contextual top bar with selection count, Select All, and Delete; bulk delete respects "Warn before delete" setting
- LocalDateTime stored as epoch seconds (UTC) via TypeConverter; LocalDate stored as epoch days
- Help screen: accessible from hamburger menu; loads `assets/help.html` via WebView; styled HTML with dark/light theme support; covers all features with navigation overview grid, per-section guides, and tips
- About dialog: version displayed dynamically from BuildConfig (versionName + versionCode); "What's New" section with recent feature changelog
- Build: `buildConfig = true` enabled in build features for BuildConfig access
- Build: auto-increment versioning via `version.properties` (VERSION_MAJOR, VERSION_MINOR, VERSION_CODE); minor version and version code increment after each assembleDebug/assembleRelease

## PWA Web App
Located in `PWA/` folder. Node.js + Express + better-sqlite3 server with vanilla JS frontend.
- **Server:** 16 REST API routes, Yahoo Finance proxy service, auto-refresh cron, CSV parser
- **Frontend:** 18 screens, 11 components, HTML5 Canvas charts, hash-based SPA router
- **Database:** Same SQLite schema as Android Room v29 (12 tables + settings)
- **No build step:** vanilla JS modules, no framework
- **Yahoo Finance:** Server-side direct fetch calls (no CORS issues since server-side); v10 summaryDetail with crumb auth for dividend rate
- **Service Worker:** Network-first for JS/CSS/HTML, cache-first for assets; "Refresh App" button in About to force cache bust
- **Snapshot:** Static `snapshot.html` generated after every Refresh All — offline-viewable portfolio summary
- **Server Log:** In-memory log capture (500 entries); viewable in Settings > Server Log tab
- **Backup:** Same v5 JSON format — data portable between Android and PWA
- **Run:** `START_APP.bat` or `npm start` from PWA/ folder → http://localhost:3000
- **Dependencies:** express, better-sqlite3, multer (installed via `npm install`)

### PWA Folder Structure
All PWA code is inside the `PWA/` folder:
- `PWA/server/` - Express server
  - `index.js` - Server entry point
  - `db.js` - SQLite database setup and schema (better-sqlite3)
  - `routes/` - REST API route handlers
    - `accounts.js` - Account CRUD
    - `positions.js` - Position CRUD + logo endpoint
    - `transactions.js` - Transaction CRUD
    - `performance.js` - Account performance records
    - `csv-import.js` - CSV import for transactions, positions, performance
    - `csv-mappings.js` - CSV column mapping CRUD
    - `settings.js` - App settings key/value store
    - `change-history.js` - Daily portfolio value tracking
    - `watch-lists.js` - Watch list CRUD
    - `definitions.js` - Metric definitions
    - `sql-library.js` - Saved SQL queries
    - `ai-library.js` - AI prompt library
    - `backup.js` - Backup export/import (v5 JSON)
    - `sql-explorer.js` - Raw SQL execution
    - `yahoo.js` - Yahoo Finance proxy routes
  - `services/` - Business logic services
    - `yahoo-finance.js` - Yahoo Finance API (direct fetch, crumb auth for v10)
    - `csv-parser.js` - CSV parsing with auto-mapping aliases
    - `auto-refresh.js` - Periodic price refresh, change history, auto-backup, snapshot generation
    - `snapshot.js` - Static HTML portfolio snapshot generator
    - `app-log.js` - In-memory server log capture (intercepts console.log/error/warn)
- `PWA/public/` - Frontend (vanilla JS, no build step)
  - `index.html` - SPA shell
  - `css/styles.css` - Global styles (dark/light theme)
  - `js/app.js` - App initialization
  - `js/router.js` - Hash-based SPA router
  - `js/api.js` - API client (fetch wrappers for all server routes)
  - `js/preferences.js` - localStorage preferences with defaults
  - `js/screens/` - Screen modules (one per route)
    - `dashboard.js` - Dashboard with collapsible cards (Portfolio Summary, Market Indices, Daily Glance, Positions, Position Details)
    - `items.js` - Positions list with Stock/ETF/Analysis/Dividend tabs
    - `item-detail.js` - Single ticker detail (Details, Price History, Transactions tabs + Investing Performance chart)
    - `item-form.js` - Add/edit position form
    - `transaction-list.js` - Transaction list with multi-select
    - `transaction-form.js` - Add/edit transaction form
    - `accounts.js` - Account management
    - `performance.js` - Account performance chart + records
    - `settings.js` - Settings (Preferences, Data Management, Dashboard Cards, Market Indices, NDA Thresholds)
    - `simulation.js` - Price simulation with time ranges
    - `sql-explorer.js` - SQL query editor + table browser
    - `sql-result.js` - SQL result grid with CSV export
    - `watch-list.js` - Watch lists with reminders
    - `next-day-actions.js` - Portfolio scanner (5-signal: STOP_LOSS, TRIM_PROFIT, REBALANCE, STRONG_BUY, HOLD)
    - `help.js` - Help documentation
    - `about.js` - About dialog with version + app log
    - `change-history.js` - Change history dialog
    - `analyze-price.js` - Price analysis for transactions
  - `js/components/` - Reusable UI components
    - `collapsible-card.js` - CollapsibleCard with pin persistence
    - `ticker-icon.js` - Gradient icon with logo overlay (Coil-style)
    - `confirm-dialog.js` - Confirmation dialog
    - `date-range-picker.js` - Date range picker
    - `line-chart.js` - Canvas line chart (zoom, pan, tap-to-select)
    - `pie-chart.js` - Canvas pie chart
    - `toast.js` - Toast notifications
  - `js/utils/` - Utility modules
    - `format.js` - Currency, percent, date formatting
  - `sw.js` - Service worker (network-first caching, force-refresh via message)
- `PWA/package.json` - Node.js dependencies
- `PWA/START_APP.bat` - Windows launcher script
- `PWA/start_server.sh` - Linux/NAS launcher (stop, pull, restart)
- `PWA/full_reset_server.sh` - Full reset (backup DB, hard reset, restore DB, restart)

## Build (Android)
Open `ANDROID_APP/` in Android Studio and sync Gradle. Requires JDK 17+.

### Batch Scripts (in ANDROID_APP/)
All scripts source `env.bat` for shared config (JAVA_HOME, proxy settings).
- `env.bat` - Shared environment config (JAVA_HOME path, corporate proxy)
- `build_apk.bat` - Clean + assembleRelease, opens output folder
- `create_signature.bat` - Generate signing keystore + keystore.properties
- `run_once.bat` - First-time setup (keystore.properties, local.properties, keystore generation)
- `install_dependency.bat` - Download/install JDK 17 + Android SDK command-line tools
- `start_emulator.bat` - Launch emulator, build debug, install and run app

### Building from CLI
```
cd ANDROID_APP
build_apk.bat
```
Or manually:
```
cd ANDROID_APP
set JAVA_HOME=C:\Program Files\jdk-17.0.2
gradlew assembleRelease
```

### First-Time Setup on New Machine
Run `run_once.bat` to create gitignored config files, or manually:
1. Set JAVA_HOME in `env.bat`
2. Run `create_signature.bat` to generate keystore
3. Run `install_dependency.bat` to install Android SDK
4. Run `build_apk.bat` to build

### Corporate Proxy
Proxy configured in two places:
- `env.bat` — `PROXY` variable used by batch scripts (curl downloads)
- `gradle.properties` — `systemProp.http(s).proxyHost/Port` used by Gradle wrapper (Java networking)

## Versioning
- Version managed via `ANDROID_APP/version.properties` (VERSION_MAJOR, VERSION_MINOR, VERSION_CODE)
- After each `assembleDebug` or `assembleRelease`, minor version and version code auto-increment by 1
- `versionName` and `versionCode` in `ANDROID_APP/app/build.gradle.kts` read from `version.properties`
- To bump major version: edit VERSION_MAJOR in `version.properties` and reset VERSION_MINOR to 0
- About dialog shows dynamic version via BuildConfig
