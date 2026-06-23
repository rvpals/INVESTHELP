# Changelog

## v1.64 (Build 65) - 2026-06-22

### Improved
- **Correlation Matrix layout** (Android + PWA):
  - **Sticky column header**: column ticker labels now pin to the top of the screen as matrix rows scroll past (Android: `LazyColumn` + `stickyHeader`; PWA: CSS `position:sticky; top:0`)
  - **Row separators**: 4dp / 5px surfaceVariant band between every ticker row for clear visual separation
  - **Cell ticker hint**: each non-diagonal cell now shows the column ticker name (9sp/9px, above the value) so the column is identifiable without scrolling back to the header
  - Android: `SuccessContent` refactored from `Column + verticalScroll` to `LazyColumn`; `MatrixGrid` composable removed (rendering moved inline)
  - PNG export updated to render column ticker hint above value in each cell
  - PWA: matrix container changed to `overflow:auto; max-height:calc(100vh - 220px)` to enable both H and V scroll within the div; corner cell sticky on both axes (z-index:4)
- **Backup import error surfacing** (PWA): `api.js` `backup.import` now checks `r.ok` and throws a meaningful error (e.g. HTTP 413 from nginx reverse proxy) instead of silently showing "0 accounts imported"
- **Server restart scripts**: `restart_server.sh` (NAS/Linux) and `RESTART_APP.bat` (Windows) use `sudo pkill` to kill root-owned Node processes; default port restored to 3000

## v1.62 (Build 63) - 2026-06-21

### Added
- **Correlation Matrix screen** (Android + PWA): new analytics screen showing pairwise Pearson correlation of 1-year daily returns for all Stock/ETF positions, plus S&P 500 market sensitivity row
  - **Android:** accessible from hamburger menu → "Correlation Matrix" (teal GridOn icon)
    - New files: `CorrelationMatrixScreen.kt`, `CorrelationMatrixViewModel.kt`, `CorrelationUtils.kt`, `CorrelationCacheDao.kt`, `CorrelationCacheEntity.kt`
    - DB v32: new `correlation_cache` table (single row id=1; full N×N matrix + market correlation stored as JSON strings)
    - Color-coded N×N grid: red ≥0.75, orange ≥0.50, yellow ≥0.25, green ≥0.00, blue <0.00; diagonal dark gray
    - 68dp cells (up from 52dp) with horizontal scroll snap: snaps to nearest column boundary when user lifts finger
    - **Filter toggle** (FilterChip, `rememberSaveable`): "Highlight ≥ 0.75 only" dims other cells to 20% opacity
    - Collapsible explainer card with colour legend, what-to-look-for guidance, and diagonal note
    - Tap any non-diagonal cell → AlertDialog with value, label, and plain-English explanation
    - Market sensitivity row: colour-coded chips showing each ticker's Pearson vs SPY
    - Portfolio insights card: average correlation, most correlated pair, most diversifying ticker, high-corr warning (avg > 0.70)
    - **Share / Export PNG**: Share icon in TopAppBar → `renderMatrixBitmap` draws 80px-cell Canvas → saved via `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` → `Intent.ACTION_SEND` with `FLAG_GRANT_READ_URI_PERMISSION`; no `WRITE_EXTERNAL_STORAGE` needed (minSdk=29)
    - Last calculated timestamp + Refresh button (clears cache, re-fetches in parallel)
    - Failed tickers banner for positions with insufficient price history
    - JUnit tests: `CorrelationUtilsTest.kt` — 14 tests covering `dailyReturns`, `pearson`, `alignPriceSeries`, `buildMatrix`, `averageCorrelation`, `mostCorrelatedPair`, `mostDiversifyingTicker`
  - **PWA:** accessible from hamburger menu → "⊡ Correlation Matrix" at route `#/correlation`
    - New files: `PWA/server/routes/correlation.js`, `PWA/public/js/screens/correlation.js`
    - `POST /api/correlation/compute`: fetches Yahoo Finance 1-year daily history for all positions + SPY server-side (parallel Promise.all), inner-joins timestamps, computes Pearson matrix, caches in DB; returns JSON
    - `GET /api/correlation/cache`: returns cached result or `{ noCache: true }`
    - `DELETE /api/correlation/cache`: clears cache for forced recompute
    - PWA SQLite: new `correlation_cache` table; excluded from backup exports (computed data)
    - CSS `scroll-snap-type: x mandatory` for column snap; sticky row labels via `position: sticky; left: 0`
    - Filter toggle pill button: dims cells with v < 0.75 to 20% opacity when active
    - Cell click → modal dialog with value and explanation
    - **Download PNG**: renders matrix on an off-screen HTML Canvas with `roundRect`, colour legend, rotated column headers → `canvas.toDataURL('image/png')` → `<a download>` click

## v1.61 (Build 62) - 2026-06-20

### Added
- **Volatility Analysis caching** (Android): results now persisted to new `volatility_cache` Room table (DB v31)
  - On app open, cached results load instantly without network calls
  - "Last calculated on MMM d, yyyy h:mm a" banner shown below tab row when cache is present
  - Refresh button clears DB cache and forces full re-fetch
  - `volatility_cache` excluded from backup exports (cached data, not user data)
- **Volatility Analysis caching** (PWA): same behavior — cache stored in `volatility_cache` SQLite table
  - `GET /api/volatility/cache/all` returns all cached rows + `lastCalculatedAt` epoch
  - `POST /api/volatility/cache/bulk` bulk-upserts after live fetch completes
  - `DELETE /api/volatility/cache/all` clears cache on Refresh
  - Refresh button and "Last calculated on" banner match Android behavior
- **Backup diagnostics**: export success message now includes "| CSV mappings: X active, Y named" row counts so users can confirm their column mappings were captured
- **Restore diagnostics**: restore success message shows the same CSV mapping counts

### Fixed
- `VolatilityAnalysisViewModel`: `async`/`await` used inside `suspend fun` without `coroutineScope` — replaced with simple sequential for loop (compile error on release build)

## v1.60 (Build 61) - 2026-06-19

### Added
- **Volatility Analysis screen** (Android): new menu item in hamburger menu (purple BarChart icon)
  - Two tabs: Stocks and ETFs, showing all positions of each type
  - Fetches 365-day price history, analysis info, and live quote concurrently per ticker
  - Groups positions by volatility level: Low (<15%), Moderate (15–30%), High (30–60%), Very High (>60%)
  - Color-coded group headers (green/orange/red-orange/dark red) with count badge
  - Each row: gradient ticker icon + ticker + company name | vol% badge + position value
  - Live progress bar while fetching (X/Y tickers); loading and error rows shown at bottom
  - Clicking any row navigates to item detail
  - 1-hour in-memory cache per ticker; Refresh button force-clears cache
  - New files: `VolatilityAnalysisViewModel.kt`, `VolatilityAnalysisScreen.kt`, `VolatilityAnalysisRoute`
- **Volatility Analysis screen** (PWA): same feature at `#/volatility-analysis`
  - Sequential fetch with live progress bar and tab switching without re-fetching
  - Reuses existing `/api/volatility/:ticker` server route
  - Added to hamburger menu and route registered in `app.js`
  - New file: `PWA/public/js/screens/volatility-analysis.js`
- **build_android_apk.md**: troubleshooting guide for JDK setup, pause-hang issue, signing problems, and locked file fixes

### Fixed
- `env.bat` JAVA_HOME corrected from `C:\Program Files\jdk-17.0.2` to `E:\Prog\Java\jdk-17`

## v1.51 (Build 52) - 2026-06-18

### Added
- **52-Week Volatility screen** (Android): new screen accessible via BarChart icon in Item Detail top bar
  - Position value card (primary-container) showing current price × shares
  - 52-week range bar (Canvas-drawn): gray track, primary fill, white dot with border at current price position
  - Annualized volatility card: large % in volatility-label color, labeled badge (Low/Moderate/High/Very High), daily std dev stat row, trading session count, method note, 4-cell volatility scale legend
  - Math: log returns `ln(close[i]/close[i-1])`, sample σ (÷ n-1), annualized `× √252 × 100`; label thresholds: <15% Low, <30% Moderate, <60% High, else Very High
  - 1-hour in-memory cache per ticker; Refresh button bypasses cache
  - New files: `VolatilityCalculator.kt`, `VolatilityViewModel.kt`, `VolatilityScreen.kt`, `VolatilityRoute` nav entry
- **52-Week Volatility screen** (PWA): equivalent web screen at `#/volatility/:ticker/:shares`
  - Server route `GET /api/volatility/:ticker` with 1-hour Map-based cache and `?force=true` bypass
  - HiDPI canvas range bar using CSS `--primary` / `--surface-variant` custom properties
  - `📊 Volatility` button added to Item Detail screen
  - New files: `PWA/server/routes/volatility.js`, `PWA/public/js/screens/volatility.js`
  - `volatility` export added to `api.js`; route registered in `app.js` and `server/index.js`

### Changed
- **Dashboard Watch List card** (Android): columns updated from "Ticker / Shares / Added Price" to "Ticker / Chg% / Chg$ / Added$"
  - Live price fetched per ticker after watch lists load; shows "--" in `onSurfaceVariant` while loading
  - Change % = `(currentPrice - priceWhenAdded) / priceWhenAdded × 100`; Change $ = `(currentPrice - priceWhenAdded) × shares`
  - Values color-coded green (gain) / red (loss)
  - `DashboardWatchListItem` data class added to `DashboardViewModel`; `fetchWatchListPrices()` runs as cancellable coroutine job

## PWA v1.0 - 2026-06-01

### Added
- **PWA Web App** (`PWA/` folder): full web version of InvestHelp
  - Node.js + Express server with better-sqlite3 (same SQLite schema as Android Room v29)
  - 64 files: 16 API routes, 18 screens, 11 components, 3 CSS themes, 3 services
  - Vanilla HTML/CSS/JS frontend — no framework, no build step
  - Hash-based SPA router with 20+ routes
  - HTML5 Canvas charts (line chart with zoom/pan/tap, pie chart, mini sparkline)
  - 22 color themes with dark mode support via CSS custom properties
  - `START_APP.bat` for Windows launch; `npm start` for CLI
  - Yahoo Finance calls server-side (no CORS issues)
  - Configurable Yahoo Finance proxy URL (Settings > Preferences) with test button
  - Server-side auto-refresh cron (works with browser closed)
  - Full SQL Explorer with direct SQLite access
  - CSV import (Position, Transaction, Performance) with preview and auto-mapping
  - Backup export/import compatible with Android app (v5 JSON format)
- **Backup format v5**: exports/imports all 10 tables (previously only 3)
  - Added: performance records, watch lists + items, change history, definitions, SQL library, AI library
  - Full data roundtrip between Android and PWA
  - Backward compatible with v1-v4 backups on restore
- Snapshot queries added to WatchListDao, DefinitionDao, SqlLibraryDao, AiLibraryDao
- deleteAll() added to WatchListDao, SqlLibraryDao, AiLibraryDao

### Changed
- SettingsViewModel now injects WatchListDao, ChangeHistoryDao, SqlLibraryDao, AiLibraryDao for full backup coverage

## v1.50 (Build 51) - 2026-05-30

### Fixed
- "Detail on the Analysis" card now shows all tickers (was only showing first due to non-scrollable outer Column)
- Converted Next-Day Actions screen to verticalScroll layout so all content (Explanation, Detail, grid) scrolls properly

## v1.49 (Build 50) - 2026-05-30

### Added
- **"Explanation" card** on Next-Day Actions screen: collapsible card explaining how each action type (STOP LOSS, TRIM PROFITS, REBALANCE, STRONG BUY, HOLD) is calculated, including formulas and logic
- **"Detail on the Analysis" card** on Next-Day Actions screen: collapsible card showing per-ticker breakdown with actual numbers — current price, cost basis, return calculation, 20-day SMA, volume ratio, and which tier checks triggered or passed

## v1.48 (Build 49) - 2026-05-30

### Added
- **Next-Day Actions screen**: post-market portfolio scanner accessible from hamburger menu
  - Tier A (Risk): flags positions closing below 20-day SMA (stop loss) or exceeding profit target %
  - Tier B (Concentration): flags stocks/ETFs exceeding allocation caps
  - Tier C (Momentum): flags volume spikes (1.5x+ 20-day average volume)
  - Color-coded action grid: STRONG BUY (green), TRIM PROFITS (orange), REBALANCE (blue), STOP LOSS (red), HOLD (gray)
  - Summary chips showing count per action type
  - Fetches 20-day price/volume data from Yahoo Finance for each position
- **Configurable thresholds** in Settings > Preferences > "Next-Day Actions Thresholds": Trailing Stop %, Profit Target %, Stock Cap %, ETF Cap %
- **fetchScanData()** in StockPriceService: 1-month daily data for 20-day SMA and volume analysis

## v1.47 (Build 48) - 2026-05-30

### Changed
- AI screen: "Send to Gemini" now copies prompt to clipboard and launches installed Gemini app (or website fallback); removed embedded WebView approach
- Investing Performance data table: added "Shares" column showing number of shares for transaction rows
- Updated help.html with AI section and updated SQL Explorer section

## v1.45 (Build 46) - 2026-05-29

### Added
- **AI Screen**: "Artificial Intelligence for \<TICKER\>" with embedded WebView showing Gemini response in-app
  - AI Library collapsible card with search; click entry to auto-fill prompt with `[TICKER]` substituted
  - Multi-line prompt editor
  - "Send to Gemini" button opens Gemini web app in embedded WebView (500dp card)
  - User signs into Google once in WebView, stays logged in
- **AI Library table** (`ai_library`): pre-seeded with 3 analysis prompts (Forensic Deep-Dive, Earnings Summarizer, ETF Deconstruction)
- **AI Settings tab**: toggle to enable/disable AI, API key field
- **Yahoo Finance 1-minute pricing**: `fetchQuote` now uses `interval=1m` for ~1-2 min delay during market hours
- **Price display**: Item Detail header shows price in 3D gradient box with "per share · as of \<time\>"
- **Investing Performance chart**: transaction points show "3 @ $299.00" (shares @ price)
- Removed ticker title from Item Detail top bar
- Database migration v28→v29 (creates ai_library table with 3 seed prompts)

## v1.44 (Build 45) - 2026-05-29

### Added
- **AI Screen**: full-screen "Artificial Intelligence for \<TICKER\>" accessible via sparkle icon in Item Detail top bar
  - AI Library collapsible card with search; clicking an entry auto-fills prompt with ticker substituted
  - Multi-line prompt editor with Send button
  - Progress bar + "Sending request to AI..." during request
  - AI response card with ticker name highlighted (bold, primary color background)
  - Powered by Google Gemini 2.0 Flash API
- **AI Library table** (`ai_library`): name, description, promptText; pre-seeded with 3 prompts:
  - Forensic Ticker Deep-Dive
  - 10-K/10-Q Earnings Summarizer
  - ETF "Under the Hood" Deconstruction
- **AI Settings tab**: toggle to enable/disable AI, API key field (default pre-filled)
- **Yahoo Finance real-time pricing**: changed `fetchQuote` to use 1-minute intervals for ~1-2 min delay during market hours
- **Price display redesign**: Item Detail header shows current price in 3D gradient box with "per share · as of \<time\>" label
- **Investing Performance chart**: transaction points now show "3 @ $299.00" (shares @ price) instead of just price
- Removed ticker title from Item Detail top bar to save space
- Database migration v28→v29 (creates ai_library table with 3 seed prompts)

## v1.43 (Build 44) - 2026-05-29

### Added
- **SQL Result screen**: new full screen for viewing query results with editable SQL query card, result grid with both vertical and horizontal scroll, clickable cells for full-screen detail view, and "Export to CSV file" button
- **SQL Library**: new `sql_library` table (name, description, category, sql) for saving reusable SQL queries; collapsible card in SQL Explorer with category filter dropdown and name search; Run/Delete actions per entry
- **Save SQL to Library**: button in SQL Explorer prompts for name, description, and category (with autocomplete from existing categories)
- **Table/Column click-to-insert**: clicking a table name or column name in the table browser inserts it into the SQL text box
- Database migration v27→v28 (creates sql_library table)

### Changed
- SQL Explorer redesigned: Run button now navigates to dedicated SQL Result screen instead of showing results inline
- Removed Export CSV button from SQL Explorer (moved to SQL Result screen)
- Table browser column names now clickable (primary color) to insert into SQL text box

## v1.42 (Build 43) - 2026-05-29

### Added
- **Yahoo Finance Full Report**: info button (ℹ) in Item Detail top bar fetches comprehensive data from Yahoo Finance and displays in a scrollable dialog with sections: Market Data, Valuation & Trading, Financials, Key Statistics, Company Profile, Upcoming Events, Analyst Recommendations, Fund Profile (ETFs), Top Holdings (ETFs); each field shows name, value, and description
- **Auto-detect ticker type**: Item Form automatically sets type (Stock/ETF/MutualFund/Crypto) from Yahoo Finance `quoteType` when fetching a new ticker
- **Positions screen vertical scroll**: Stock/ETF table now scrollable to show all items

### Fixed
- Positions screen not showing all items: removed `quantity > 0` filter that excluded 0-share positions
- Entity `equals()` only compared ticker, preventing Compose from detecting data changes; now compares all fields
- Positions screen not reflecting newly added items due to `WhileSubscribed(5000)` disconnecting; changed to `Eagerly`

## v1.41 (Build 42) - 2026-05-29

### Fixed
- Positions screen not showing all items: removed `quantity > 0` filter that excluded 0-share positions from Stock/ETF tabs
- Entity `equals()` only compared `ticker` field, preventing Compose from detecting data changes (e.g. quantity updates); now compares all fields including `logo` via `contentEquals`
- Positions screen not reflecting newly added items due to `SharingStarted.WhileSubscribed(5000)` disconnecting after navigation; changed to `SharingStarted.Eagerly`

## v1.39 (Build 40) - 2026-05-26

### Added
- **News on ticker**: collapsible card in Item Detail > Details tab showing news articles fetched from Yahoo Finance; each article shows title, publisher, time ago, and opens in browser on tap
- **Analysis Info card**: moved Analysis Info from its own tab into a collapsible card in the Details tab for easier access alongside other item information
- **Analysis tab on Positions screen**: third tab with Stock and ETF pie chart cards featuring exploding slice for the highest-value ticker
- **Settings: Max # of News articles**: configurable count (5, 10, 20) in Preferences > General; defaults to 5
- Modern icons on all 3 Positions screen tabs (ShowChart, TrendingUp, Analytics)

### Changed
- Item Detail screen reduced from 4 tabs to 3 tabs (Details, Price History, Transactions); Analysis Info content now lives as a collapsible card within the Details tab
- Item Form: fetches full Yahoo Finance data (price, name, dayHigh, dayLow, previousClose, logo) when adding or searching for a new ticker
- Item Form: allows saving a ticker with 0 shares
- Item Detail: shows "You don't own any shares of this ticker" indicator when quantity is 0

### Fixed
- Item Form: quantity field showed blank when editing existing positions due to race condition between Flow emission timing and LaunchedEffect; added `itemLoaded` gate flag

## v1.38 (Build 39) - 2026-05-26

### Changed
- Renamed database table `investment_items` to `investment_positions`
- Removed `cost` and `totalGainLoss` fields from positions table (no longer tracked at the position level)
- Removed Total Cost, Total G/L, Change $, Change % columns from Dashboard Position Details table
- Removed Total Cost and Total G/L cards from Item Detail screen
- Item Detail card row 1 now shows: Total Shares, Total Value (2 cards)
- Gain/loss badge on Items screen now shows daily gain/loss instead of total gain/loss
- CSV Position Import: removed cost-related mappable fields (cost basis, gain/loss, unrealized g/l)
- Database version bumped to 27 (migration v26→v27)

### Fixed
- Item Form screen broken when opening with a ticker not in the positions table: form fields were disabled and Save button permanently greyed out
- Item Form now correctly handles non-existent tickers: auto-fetches price and name from Yahoo Finance, allows editing all fields, and saves as a new position

## v1.37 (Build 38) - 2026-05-24

### Fixed
- Auto backup on exit never ran: `isFinishing` check is almost always false on modern Android (back button doesn't finish the activity since Android 12+); replaced with `onStop()` trigger with a 30-minute time guard to prevent excessive backups
- Dashboard Watch List card not reflecting changes: used `.first()` (one-shot) for item loading instead of continuous Flow observation; now uses `collectLatest` + `combine` to reactively observe both watch list and item changes

### Added
- Settings Data Management: "Last Auto Backup completed on \<date & time\>" note shown below the auto backup options when a previous backup has been recorded

## v1.36 (Build 37) - 2026-05-23

### Added
- Settings Data Management: "Automatic Back Up when quitting" toggle — automatically exports a backup JSON file to the selected backup folder when exiting the app
- Settings Data Management: "Number of automatic backup to keep" option (default: 10) — automatically deletes the oldest backup files when the count exceeds the configured limit
- Auto-backup runs during activity `onStop()` when `isFinishing` is true, using the same export format as manual Export Data

## v1.35 (Build 36) - 2026-05-22

### Fixed
- App crash on launch: Room database migration 25→26 created index with wrong name (`index_investment_transactions_unique` instead of expected `index_investment_transactions_date_action_ticker_totalAmount`), causing schema validation failure on startup

## v1.34 (Build 35) - 2026-05-22

### Added
- Screen Registry: `SCREEN_REGISTRY.md` — numbered reference of all 19 screens and their components for quick communication
- Item Detail Transactions tab: delete button (X) on each transaction card with confirmation dialog (respects "Warn before delete" setting)
- Item Detail Investing Performance chart: fullscreen view button (opens chart in full-screen dialog at 400dp height)
- Item Detail Investing Performance chart: save-to-PNG button (renders chart as 1200x600 bitmap, saves to Pictures/InvestHelp/)
- Analyze Price: Year-To-Date (YTD) row added to Historic Prices table showing high/low since Jan 1
- Simulation: "Scenario Simulation" collapsible card — enter shares, ticker, and buy date to calculate hypothetical gain/loss at today's price
- Transaction table: unique constraint on (date, action, ticker, totalAmount) to prevent duplicate CSV imports
- Transaction DAO: `insertTransactionIfNotExists()` method using IGNORE conflict strategy for CSV imports
- G1 Top Bar: Watch List icon button (star, purple) for quick access to Watch Lists

### Changed
- Bottom navigation: reduced to 3 tabs (Dashboard, Positions, Transaction); Performance and Simulation moved to hamburger menu
- Hamburger menu: added Performance and Simulation entries; removed Watch List (now accessible via top bar icon)
- Database version bumped to 26 (migration v25→v26 adds unique index on investment_transactions)

## v1.33 (Build 34) - 2026-05-21

### Changed
- Help screen: completely rewritten help.html with comprehensive coverage of all current features
- Help guide now documents: Portfolio Summary, Market Indices, Daily Glance, all 4 Item Detail tabs, Investing Performance chart, chart gestures, Watch List reminders, Auto Refresh, CSV named mappings, and more
- Added Chart Interactions section explaining shared pinch-to-zoom/pan/tap/double-tap gestures
- Updated Tips & Shortcuts with 14 practical tips

## v1.32 (Build 33) - 2026-05-21

### Added
- Investing Performance chart: current price per share added as last data point (tertiary color, labeled "Current" in table)
- Investing Performance chart: price labels displayed above/below each data point on the chart
- Investing Performance chart: pinch-to-zoom (1x–5x) with pan and double-tap to reset (matches Price History chart behavior)
- All tables app-wide: alternating row background colors (surfaceVariant at 30% alpha on odd rows) for improved readability

### Changed
- All table HorizontalDivider colors changed from `outlineVariant` to `outline` for better visibility between rows
- Investing Performance table: current price row highlighted with tertiary container color and bold "Current" type label

## v1.31 (Build 32) - 2026-05-21

### Added
- Item Detail Transactions tab: "Investing Performance for <TICKER>" collapsible panel (default expanded)
- Investing Performance: fetches Yahoo Finance closing prices for 1 day before and 1 day after each transaction date
- Investing Performance chart: line chart showing transaction prices (bold red dots) vs market prices (gray dots); tap-to-select with tooltip
- Investing Performance table: data grid with Date, Price, Type columns; transaction rows highlighted with different background color and bold text
- StockPriceService: `fetchPriceHistoryByPeriod()` method for fetching prices between specific epoch timestamps

### Changed
- Item Detail Transactions tab: combined Stats and Transactions into single "Transactions & Stats" collapsible panel

## v1.30 (Build 31) - 2026-05-21

### Changed
- Item Detail: restructured from 2 tabs to 4 tabs using ScrollableTabRow (Details, Price History, Analysis Info, Transactions)
- Item Detail "Details" tab: now only shows the header card with ticker info, prices, and daily stats (cleaner layout)
- Item Detail "Analysis Info": moved from collapsible panel in Details tab to its own dedicated tab; content displayed directly without collapse wrapper
- Item Detail "Transactions": new tab containing the `<TICKER> Stats` collapsible section and the Transactions collapsible section (moved from Details tab)

## v1.27 (Build 28) - 2026-05-20

### Added
- Account Detail: performance records table (Date, Value, Note columns) with grid lines and alternating row colors
- Account Detail: interactive line chart showing account performance history (pinch-to-zoom, pan, tap-to-select tooltip, double-tap reset); displayed when 2+ records exist
- Account entity: `lastUpdatedOn` (datetime) and `lastValue` (currency) columns; auto-updated when a new performance record is saved
- Account List: displays "Last: $X,XXX.XX" on each account card when lastValue is set
- Item Form: up/down arrow buttons next to Quantity field to increment/decrement by 1

### Changed
- App icon updated with new investment-themed illustration (Stock/ETF/charts motif)
- Database version bumped to 24 (migration v23→v24 adds lastUpdatedOn and lastValue columns to investment_accounts)

## v1.26 (Build 27) - 2026-05-19

### Added
- Settings: "Auto Refresh All" toggle with configurable interval (5 min, 30 min, 1 hr, 5 hr, Market close daily)
- Background auto-refresh via WorkManager with foreground notification during refresh and completion notification
- Interval dropdown only appears when auto-refresh is enabled
- Auto-refresh respects "Auto Update Change History" setting
- Fidelity theme: warm forest green color scheme inspired by the Fidelity brokerage app
- Dashboard Portfolio Summary redesign: large total value at top, today's gain/loss with label, taller chart with Y-axis labels and dashed grid lines, all-time % below chart

### Changed
- Portfolio Summary mini chart now 140dp tall with right-side Y-axis labels ($XXK format) and dashed horizontal grid lines
- Portfolio Summary layout: left-aligned total value (headlineLarge bold), followed by gain/loss line, then chart, then all-time %

## v1.23 (Build 24) - 2026-05-19

### Added
- Position Import: full-screen mapping editor (replaces dialog) with Save, Save As, and Load buttons
- Position Import: named mapping profiles — save column mappings with custom names and reuse them later
- Position Import: mapping selection dialog when starting import — choose from saved named mappings or default active mapping
- Position Import: detailed import result log showing per-ticker status (NEW/UPDATED/SKIPPED) with field change details (e.g., "price: 150.0 → 155.0")
- New database table `csv_named_mappings` for storing named mapping profiles

### Changed
- Database version bumped to 23 (migration v22→v23 creates csv_named_mappings table)

## v1.20 (Build 21) - 2026-05-14

### Added
- Change History: daily change value columns (dailyChangeEtf, dailyChangeStock, dailyChangeTotal) — automatically populated during price refresh
- Change History dialog: "Change Value This Week So Far" summary card above data table showing sum of daily changes for ETF, Stock, and Total since Monday (color-coded green/red)
- Change History data table: 3 new columns (Δ ETF, Δ Stock, Δ Total) with color-coded values; table is now horizontally scrollable

### Changed
- Database version bumped to 22 (migration v21→v22 adds daily change columns to change_history table)

## v1.19 (Build 20) - 2026-05-14

### Fixed
- Item Detail: Yahoo Finance, Simulate, and Add to Watch List buttons now visible even when ticker doesn't exist in the database yet
- Transaction form: ticker field now editable (allows typing new tickers) with filtered dropdown suggestions; previously was dropdown-only, blocking new ticker entry

## v1.17 (Build 18) - 2026-05-14

### Added
- Item Detail Price History: "1 Minute" interval option for Hourly timeframe
- Build: new `build_apk.bat` script with JAVA_HOME and clean step (replaces `build_apk_release.bat`)

### Fixed
- Item Detail Price History: "10 Minutes" interval replaced with "15 Minutes" — Yahoo Finance does not support 10m interval (caused HTTP 400 error)
- Item Detail Price History: interval radio buttons split into two rows so all labels are fully visible (previously "5 Minutes" label was cut off)

### Changed
- Hourly intervals now: Every Hour (1h), 30 Minutes (30m), 15 Minutes (15m), 5 Minutes (5m), 1 Minute (1m)

## v1.16 (Build 17) - 2026-05-13

### Added
- Item Detail Price History: hint text below timeframe selector showing what each option means (e.g., "Last 60 days (1d interval)")
- Item List: auto-fetches missing company logos in background on screen load
- Logo fetching: tries 3 CDN sources (companiesmarketcap.com, parqet.com, iexcloud) for better coverage
- SQL Explorer: alternating row colors in result grid for improved readability

### Changed
- SQL Explorer result grid: already had gridlines; added alternating row background color (every other row highlighted)

## v1.15 (Build 16) - 2026-05-13

### Added
- Item Detail: delete button (trash icon, red) in top app bar; respects "Warn before delete" setting with confirmation dialog
- Item Detail Price History: line chart above the data table plotting all price points; supports pinch-to-zoom (1x–5x), pan when zoomed, tap-to-select with tooltip (price + date), double-tap to reset; filled area under curve; Y-axis price labels and X-axis date labels

### Changed
- Transaction table: removed `accountId` column entirely — transactions are no longer tied to accounts
- Transaction form: removed account dropdown selector
- Transaction list: removed account filter dropdown and account name display
- Account Detail screen: removed transactions section (accounts no longer have associated transactions)
- Database version bumped to 21 (migration v20→v21 recreates transactions table without accountId/FK)
- Backup format bumped to v4 (transactions no longer export accountId; v3 backups still restore correctly)
- CSV Transaction Import: no longer maps accountId field

## v1.14 (Build 15) - 2026-05-13

### Added
- Settings: collapsible panels — "Themes" and "Dashboard Market Indices" sections now collapse/expand with animated arrow toggle (default collapsed)
- Refresh status bar: temporary status bar below top bar during price refresh showing "Updating [TICKER]" with current price, change amount, and change percent (color-coded green/red); auto-hides when refresh completes
- Watch List: clicking a ticker in the table navigates to Item Detail screen (ticker text colored as link)
- Watch List: each watch list displayed as its own collapsible panel (replacing chip-selector approach); all lists visible simultaneously with expand/collapse per list
- Watch List: "Refresh All" button in header refreshes prices across all watch lists
- Item Detail: new "Price History" tab with radio button timeframe selector (Hourly, Daily, Monthly, Yearly)
- Item Detail Price History: fetches data from Yahoo Finance — Hourly shows market hours for today, Daily shows last 60 days, Monthly shows last 13 months, Yearly shows last 15 years
- Item Detail Price History: summary cards (Average, Max, Min) above the price table
- Item Detail Price History: grid table with row number, date/time, and closing price with gridlines
- StockPriceService: new `fetchPriceHistory(ticker, range, interval)` method for flexible Yahoo Finance chart queries

### Changed
- Watch List screen: replaced FilterChip selector with collapsible panels showing all watch lists at once; each panel has Add Ticker, Rename, Delete actions inline
- Item Detail screen: restructured with TabRow (Details, Price History) tabs; existing content moved to Details tab

## v1.13 (Build 14) - 2026-05-12

### Added
- Watch List notification tap: clicking a reminder notification now opens the Item Detail screen for the ticker
- Company logo stored in database: `logo` BLOB column on `investment_items` table; fetched from companiesmarketcap.com CDN during price refresh and cached locally
- Item Detail: company logo icon (48dp) in header card with ticker name and company name
- Database migration v19→v20: adds `logo` column to investment_items
- Logo fetch logging in AppLog for debugging CDN responses

### Fixed
- Company logo URLs: fixed 404 errors by lowercasing ticker in CDN URL (companiesmarketcap.com uses lowercase filenames)
- Logo display: use ByteBuffer.wrap() for Coil 2.x compatibility (raw ByteArray not supported)
- All ticker icon composables (Items list, Dashboard, Item Detail) now use stored logo bytes when available, falling back to network URL

## v1.10 (Build 11) - 2026-05-12

### Added
- 6 new themes: Navy Marine, Tropical Mint, Wine Burgundy, Desert Sand, Nordic Pine, Chase (21 total)
- Chase theme inspired by Chase brokerage app: deep navy blue primary, white surfaces, corporate look

### Changed
- Items list screen redesigned to brokerage-style layout: individual card rows, ticker + uppercase company name on left, current price with day change/percentage below, total value on right with colored gain/loss badge (green/red chip)
- Items list: removed alternating row colors in favor of thin dividers between rows

### Fixed
- Watch List: selecting today's date for reminder notification now works correctly (fixed Material 3 DatePicker UTC timezone conversion issue)
- Watch List: reminder notifications now properly fire on Android 13+ (added POST_NOTIFICATIONS runtime permission request and canScheduleExactAlarms() check)

## v1.9 (Build 10) - 2026-05-11

### Added
- Data Management: "Clear" (x) button per CSV import type (Transaction Records, Position Details, Performance Records) to erase all entries from the corresponding table with confirmation dialog showing table name
- Dashboard Portfolio Summary: last refreshed timestamp now persisted to SharedPreferences (survives app restart)

## v1.8 (Build 9) - 2026-05-11

### Fixed
- App icon: fixed distortion caused by naive resize of non-square source image; properly crops to content area, preserves aspect ratio, centers within adaptive icon safe zone using LANCZOS resampling at all density buckets

## v1.7 (Build 8) - 2026-05-11

### Added
- Change History: `change_history` table recording daily ETF/Stock/Total values; auto-records on price refresh when toggle enabled in Settings
- Dashboard Portfolio Summary: miniature line chart of total_value history; click opens full-screen chart with zoomable multi-series (Total/ETF/Stock) + grid data table
- Dashboard Portfolio Summary: "Refreshed: MMM dd, h:mm a" label showing last price refresh time
- Watch List Reminders: optional reminder per watch list item with date/time + message; scheduled via AlarmManager; notification via BroadcastReceiver; bell icon in table row (highlighted when active); set during add or edit via dedicated dialog
- 5 new themes: Lavender Fields, Copper Bronze, Emerald Gem, Slate Blue, Mocha Coffee (15 total)
- Simulation chart: thicker lines (5f stroke) with labels (1W, 3M, etc.) drawn at end of each line
- New app icon

### Changed
- Transaction form: Update/Create and Simulate buttons moved to fixed bottom bar (no longer scroll with form content); Update button placed first
- Transaction form: ticker field now dropdown-only (selects from existing items)
- Database version bumped to 19 (migrations v17→v18 for change_history, v18→v19 for watch list reminders)

### Fixed
- CSV Import: ticker field no longer includes trailing " - <shares>" content from brokerage exports (e.g., "MSFT - 50" now correctly imports as "MSFT")

## v1.6 (Build 7) - 2026-05-10

### Added
- Dashboard: "Portfolio Summary" collapsible card wrapping daily change amount and Day/All percentages; total value change displayed in 3x larger font (headlineLarge)
- Performance screen: Records list converted from cards to grid table with horizontal and vertical gridlines, header row, and alternating row colors

### Changed
- Item detail: "Analysis Info" now displayed as an inline collapsible panel (auto-fetches on screen load) instead of a button that opens a bottom sheet
- Item detail: removed "Analysis Info" button; Yahoo Finance button now full-width on its own row

## v1.5 (Build 6) - 2026-05-10

### Added
- Performance screen: "Chart Data" collapsible panel below chart showing tabular data (Account, Date, Value columns) for selected chart accounts
- Performance screen: "Recent" button in Add Record form pulls value from latest record for the selected account
- Performance screen: mini chart in Add Record section showing selected account's performance history (150dp, appears when 2+ records exist)

### Changed
- Performance screen: "Pull from App" button shortened to "Pull" to accommodate the new "Recent" button

## v1.4 (Build 5) - 2026-05-07

### Added
- Theme system: 10 selectable color themes (Default Green, Ocean Blue, Sunset Orange, Midnight Purple, Forest Moss, Ruby Red, Arctic Ice, Gold Rush, Sakura Pink, Charcoal Dark)
- Theme selection in Settings > Preferences with color preview swatch and instant apply
- Performance button moved to bottom navigation bar for quick access

### Removed
- Bank Transfer feature removed entirely (Transfer screen, entity, DAO, repository, migration drops `bank_transfers` table)
- Performance entry removed from hamburger menu (now in bottom nav)

### Changed
- Bottom nav: Dashboard, Items, **Performance**, Transaction, Simulation (replaced Transfer)
- Database version bumped to 17 (migration v16→v17 drops bank_transfers table)
- Theme no longer uses Android dynamic colors — uses app-defined theme selection instead

## v1.2 (Build 3) - 2026-05-03

### Added
- Gridlines on all result tables: added vertical dividers to Dashboard Positions legend table and Analyze Price historic prices table for consistent table styling across the app

### Changed
- All tabular grids now have both horizontal and vertical gridlines (Dashboard Position Details, Dashboard Positions Legend, Watch List, SQL Explorer, Analyze Price Historic Prices)

## v1.1 (Build 2) - 2026-05-03

### Added
- Account Performance chart: double-tap inline chart opens full-screen dialog with zoom/pan/tap-to-select
- Account Performance chart: data points with notes rendered bold (larger radius with white outline)
- Account Performance chart: tapping a noted data point shows note text in tooltip (bold second line)
- Dashboard Market Indices: user-configurable display order via Settings up/down arrows
- Dashboard Market Indices: long-press drag-and-drop reorder on dashboard cards with SharedPreferences persistence

## v1.0 (Build 1) - 2026-05-02

### Added
- Help screen: HTML-based guide loaded via WebView with dark/light theme support
- About dialog: dynamic version display from BuildConfig
- Auto-increment versioning via `version.properties`

## v0.x (Pre-release) - 2026-04 to 2026-05

### Features developed
- Dashboard with collapsible cards (Market Indices, Daily Glance, Positions, Position Details)
- Items screen with pie chart, STOCK/ETF tabs, sort dropdown, card-style rows with 3D ticker icons
- Transaction management with Analyze Price, Simulate, multi-select bulk delete
- Bank transfer tracking with per-account summary
- Simulation with historical price charts (1W-MAX), tap-to-select tooltips
- Account Performance with multi-account overlay chart, pinch-to-zoom, full-screen mode
- Watch Lists with multiple named lists, price tracking, change calculations
- CSV Import system for positions, transactions, and performance records
- SQL Explorer with raw query execution, table browser, CSV export
- Backup & Restore (JSON v1/v2/v3 format support)
- Yahoo Finance integration (v8/v10 API) for live prices, historical data, and analysis info
- 3D gradient icons for bottom nav and hamburger menu
- CollapsibleCard component with pin persistence
- Custom Canvas-drawn pie chart and line chart (no external chart library)
- Application log (AppLog) with up to 200 entries, viewable from About dialog
- Settings: warn-before-delete toggle, market indices customization, backup folder persistence
- Splash screen with custom app icon
- Room database version 16 with migrations v4-v16
