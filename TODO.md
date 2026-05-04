# Invest Help - TODO

## Completed
- [x] Composite PK for positions (ticker + accountId)
- [x] Account value computed from position values
- [x] Custom app icon from invest_help_icon.png
- [x] Splash screen with app icon
- [x] Database migration v4 -> v5
- [x] Transaction table redesign: ticker field, optional time, totalAmount, note (migration v5 -> v6)
- [x] Account detail screen with Positions and Transactions tabs
- [x] Analysis Info button on Item detail (Yahoo Finance quoteSummary)
- [x] Yahoo Finance web link on Item detail
- [x] Pie chart on Positions screen (collapsible, by ticker value)
- [x] Simulation screen (2-week historical chart, profit/loss calculation)
- [x] Auto-create InvestmentItem when adding transaction with new ticker
- [x] Move Transaction/Transfer buttons from Items screen to main navigation
- [x] Global top bar with portfolio value 3D button + hamburger menu
- [x] Bottom nav with 5 items: Dashboard, Positions, Transfer, Transaction, Simulation
- [x] Colorful bottom nav icons with shadow/elevation
- [x] Hamburger menu with Accounts, Items, Settings, About
- [x] Simulation time ranges expanded: 1W, 2W, 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, MAX
- [x] Simulation time range chips grouped in 3 rows (Week, Month, Year)
- [x] Simulation chart tap-to-select with tooltip (price + date)
- [x] Fix MAX time range using Yahoo Finance `range=max` parameter
- [x] Dashboard positions pie chart with shares labels inside slices
- [x] Merge InvestmentItem + Position into single table with composite PK (ticker + accountId)
- [x] Combine Items and Positions screens into unified Items screen with pie chart + tabs
- [x] Ticker-based navigation routes (replacing Long ID-based routes)
- [x] Database migration v8 -> v9 (merge positions into investment_items)
- [x] Backup format v2 with merged entity fields and v1 backward compatibility
- [x] Portfolio value top bar auto-refreshes when item values change
- [x] Bottom nav updated: Dashboard, Items, Transfer, Transaction, Simulation
- [x] Hamburger menu simplified: Accounts, Settings, About

- [x] Top bar portfolio button triggers full price refresh + navigates to Dashboard
- [x] Top bar shows loading spinner during price refresh
- [x] Transaction form: "Analyze Price" button with price analysis screen (current, avg/max/min, historic high/low)
- [x] Analyze Price: select a price to copy it back to transaction form
- [x] Transaction form: "View" button next to Ticker opens item detail screen
- [x] Transaction form state preserved during navigation (rememberSaveable)
- [x] Transaction form auto-selects first account for new transactions
- [x] Item detail: "Analysis Info" and "Yahoo Finance" on same row
- [x] Item detail: collapsible Stats section (inlined from separate statistics screen)
- [x] Item detail: collapsible Transactions section
- [x] Removed separate Statistics screen (merged into item detail)
- [x] Bank Transfers screen: total amount summary grouped by account
- [x] Dashboard pie chart legend: grid-line table with Ticker, Shares, % columns
- [x] Items screen: company logos via Coil (companiesmarketcap.com CDN with letter fallback)
- [x] Items screen: full company name from Yahoo Finance shortName (updated on price refresh)
- [x] Added Coil 2.7.0 image loading dependency
- [x] Item add/edit form: type selector dropdown (Stock/ETF/Bond/MutualFund/Crypto/Other)
- [x] Item add/edit form: auto-fills type when selecting existing ticker from autocomplete
- [x] savePosition accepts optional type parameter and syncs type across accounts
- [x] Item detail: Daily/Share column showing per-share daily price change
- [x] Dashboard positions pie chart: collapsible with expand/collapse toggle
- [x] Dashboard positions legend: top 20 limit with "More/Show Less" button
- [x] Settings: "Warn before delete" toggle under General section (default: on)
- [x] Delete without confirmation when "Warn before delete" is unchecked (items, accounts, transactions, transfers)
- [x] Transaction form: "Simulate" button opens simulation from transaction date to today
- [x] Simulation: custom day range support with auto-run and human-readable label
- [x] SQL Explorer screen accessible from hamburger menu
- [x] SQL Explorer: raw SQL query execution (SELECT/PRAGMA/EXPLAIN + DML/DDL)
- [x] SQL Explorer: result table with horizontal scrolling
- [x] SQL Explorer: CSV export via FileProvider share intent
- [x] FileProvider registered in AndroidManifest for cache directory
- [x] Transaction list: gain/loss per transaction (currentPrice - pricePerShare) * shares
- [x] Settings Data Management: CSV import for positions with column mapping dialog
- [x] SQL Explorer: table browser with expandable column details per table
- [x] SQL Explorer: row detail dialog showing full field values on row click

- [x] Item detail card redesign: row 1 (big font) Total Shares/Value/Cost/G/L; row 2 (medium font) Daily G/L, G/L/Share, Min, Max
- [x] Renamed "Price:" to "Current Price:" in item detail card
- [x] Added dayHigh/dayLow fields to StockQuote, InvestmentItemEntity, and BackupItem
- [x] Parse regularMarketDayHigh/regularMarketDayLow from Yahoo Finance v8 chart API
- [x] Database migration v9 -> v10 (add dayHigh, dayLow columns)
- [x] Removed SQLCipher encryption — database is now plain Room
- [x] Removed biometric authentication and password-based auth flow
- [x] Deleted auth package (PasswordManager, BiometricHelper, AuthManager), auth UI (SetupScreen, UnlockScreen, AuthViewModel), AuthModule
- [x] App now opens directly to Dashboard — no setup/unlock screens
- [x] Removed sqlcipher, biometric, security-crypto dependencies

- [x] Dashboard market index cards: horizontal scrollable row showing NASDAQ, S&P 500, Dow, Gold (+ more) with price and daily change
- [x] Market index settings: customizable in Settings > Preferences with toggles for 8 indices
- [x] Market indices auto-refresh on app start and when refreshing all prices

- [x] Settings Preferences tab: made scrollable so all market indices toggles are visible
- [x] Dashboard market index cards: clickable to open Yahoo Finance page for the index
- [x] Application log: AppLog singleton captures price fetch results, refresh summaries, and errors (up to 200 entries)
- [x] About dialog: "Show Log" button opens scrollable log viewer (newest first) with clear button

- [x] Item detail transactions: each card shows days since transaction date (e.g. "123d") and G/L using current price vs transaction price

- [x] Account Performance screen: new `account_performance` table (id, accountId, totalValue, dateTime) with FK CASCADE
- [x] Database migration v10 -> v11 (create account_performance table)
- [x] LocalDateTime TypeConverter (epoch seconds via UTC)
- [x] Account Performance: add record form with account selector, total value field, "Pull from App" button
- [x] Account Performance: "Pull from App" computes current account value from items
- [x] Account Performance: auto-timestamps records on insert
- [x] Account Performance: records list with account name, datetime, value, delete button
- [x] Account Performance: line chart showing total value over time per account (Canvas-drawn, tap-to-select tooltip)
- [x] Account Performance: accessible from hamburger menu (between Accounts and Settings)

- [x] Settings: backup folder URI persisted to SharedPreferences (was lost on app restart)
- [x] Account Performance chart: multi-account overlay with FilterChip multi-select
- [x] Account Performance chart: each account gets distinct color from 8-color palette
- [x] Account Performance chart: pinch-to-zoom (1x–5x) with two-finger pan; double-tap resets zoom
- [x] Account Performance chart: time-based x-axis shared across all series
- [x] Account Performance chart: clipRect for zoomed content; viewport-aware x-axis labels
- [x] Account Performance chart: tooltip shows account name, value, and date

- [x] Account Performance: note field added to `account_performance` table (migration v11 -> v12)
- [x] Account Performance: optional note text field in add-record form
- [x] Account Performance: edit note dialog (pencil icon) on existing record cards
- [x] Account Performance: note displayed on record cards when present
- [x] Dashboard pie chart legend: clicking a ticker row navigates to item detail screen

- [x] Top bar portfolio button: shows daily % and all-time % gain/loss in parentheses, color-coded green/red
- [x] CollapsibleCard reusable component (`ui/components/CollapsibleCard.kt`) with title, pin button, and AnimatedVisibility
- [x] Dashboard Market Indices section: wrapped in CollapsibleCard with pin persistence
- [x] Dashboard Positions pie chart: wrapped in CollapsibleCard with pin persistence
- [x] Dashboard "Daily Glance" card: top 5 gainers and top 5 losers today with gain/loss $ and %
- [x] Pin state persisted to SharedPreferences per card (`pin_card_market_indices`, `pin_card_positions`, `pin_card_daily_glance`)
- [x] Unpinned cards default collapsed; pinned cards default expanded

- [x] Top bar portfolio button: daily change amount shown in parentheses next to total value, color-coded green/red, hidden when zero
- [x] CollapsibleCard: HorizontalDivider between header row and collapsible content

- [x] Dashboard Daily Glance: "Overall Daily" section showing Stock and ETF total daily change in $ and %
- [x] Dashboard: removed Accounts section (title, empty state, account cards, FAB)
- [x] 3D icons (Icon3D composable): bottom nav icons and hamburger menu icons use gradient-filled rounded boxes with shadow
- [x] SQL Explorer: "Open" button on each table row runs `SELECT * FROM <table>` and shows results in grid
- [x] Items screen: redesigned from individual cards to unified table with grid lines (header row + HorizontalDivider between rows)
- [x] Items screen: TickerIcon3D — gradient-filled rounded-corner box with shadow, color derived from ticker hash, logo overlay via Coil

- [x] SQL Explorer: result grid with both horizontal and vertical gridlines
- [x] Dashboard Daily Glance: "By Per Share" checkbox to toggle sorting/display between total value and per-share change
- [x] Dashboard "Position Details" collapsible card with pin: table showing ticker (with icon), shares, current price, total cost, total value, change $ and change %
- [x] Position Details: change computed as currentValue - totalCost (removed time range dropdown and historical price fetching)

- [x] Watch List screen: accessible from hamburger menu; multiple named watch lists via FilterChip selector
- [x] Watch List: create, rename, delete watch lists
- [x] Watch List: add ticker with shares count and price-when-added; "Fetch" button fetches current price from Yahoo Finance
- [x] Watch List: table shows ticker, shares, current price, added price, change $ (currentValue - costBasis), change %, added date, delete button
- [x] Watch List: `watch_lists` and `watch_list_items` tables with CASCADE delete (migration v12 -> v13)
- [x] Database version bumped to 13

- [x] CSV Import: reusable mapping system for Transaction, Position, Performance imports with persistent column mappings
- [x] CSV Import: mappings persisted in `csv_import_mappings` table with date format options per column
- [x] Settings Data Management: 3 import types (Transaction Records, Position Details, Performance Records) each with "Define Mapping" and "Start Import" buttons
- [x] Database migration v13 -> v14 (create csv_import_mappings table)
- [x] Database version bumped to 14

- [x] InvestmentItem: ticker-only primary key (removed accountId from composite PK)
- [x] Database migration v14 -> v15: recreates investment_items with ticker-only PK, merges duplicate tickers via GROUP BY with SUM/MAX aggregates
- [x] Removed accountId from InvestmentItemEntity, DAO, repository, all ViewModels, and all UI screens
- [x] Account value no longer per-account; portfolio value is sum of all items
- [x] Backup format v3: items no longer include accountId; v1/v2 backward compat on restore
- [x] CSV transaction import: does NOT auto-update share counts on items; only creates item stub if ticker doesn't exist
- [x] Database version bumped to 15

- [x] Transaction list: multi-select mode with long-press to enter selection, checkboxes, select all, contextual top bar with bulk delete
- [x] Transaction bulk delete: respects "Warn before delete" setting
- [x] Added deleteTransactions(List) to DAO, Repository, and ViewModel

- [x] SQL Explorer: "Erase" button on each table row to delete all entries from a table
- [x] SQL Explorer: erase confirmation dialog (respects "Warn before delete" setting)

- [x] Account Performance: `dateTime` field changed to `date` (LocalDate, epoch days) — simpler date-only tracking
- [x] Account Performance: unique constraint on (accountId, date) — one record per account per day
- [x] Account Performance: duplicate record prevention with user-friendly error dialog
- [x] Database migration v15 -> v16 (recreate account_performance with date field, convert epoch seconds to epoch days, add unique index)
- [x] Database version bumped to 16

- [x] Account Performance screen: three CollapsibleCards (Add Performance Record, Performance Charts, Records) with pin persistence
- [x] Account Performance chart: "Smooth Curve" checkbox for cubic Bezier curve smoothing
- [x] Account Performance chart: FlowRow for account selector chips (wrapping instead of horizontal scroll)
- [x] Account Performance Records: account filter dropdown (Select All/None/individual checkboxes)
- [x] Account Performance Records: "Order By" dropdown (Account, Date, Total Value, Note) with Asc/Desc toggle
- [x] Account Performance Records: filter and sort selections persisted to SharedPreferences across sessions

- [x] CSV Import: numeric values with commas (e.g. "92,150.62") parsed correctly via `parseNumeric()` helper
- [x] CSV Import: enhanced auto-mapping with common brokerage aliases (Price→currentPrice, Description→name, Symbol→ticker, Unrealized G/L→totalGainLoss, Shares→quantity, etc.)
- [x] CSV Import: non-data rows (blank lines, FOOTNOTES sections) filtered out during import
- [x] CSV Import: Position import confirmation dialog ("Position details will be refreshed with imported CSV file. Are you sure?")
- [x] Items screen redesign: modern card-style row layout replacing cramped table grid
- [x] Items screen: sort-by dropdown (Ticker, Total Value, Current Price) above items list
- [x] Items screen: alternating row background colors for readability
- [x] Items screen: each row shows Ticker (bold, larger) with company name (smaller, italic) on left
- [x] Items screen: shares count and Total G/L displayed in bold, larger font on right side of each row
- [x] Items screen: secondary row with Price, Value, and Day G/L in smaller muted text
- [x] Items screen: only Edit button per row (no Delete button in table)
- [x] Items screen: Delete button added to Edit dialog (red "Delete" next to "Cancel", only shown when editing existing item)
- [x] Items screen: Delete from edit dialog respects "Warn before delete" setting

- [x] Help screen: HTML-based help guide loaded via WebView from `assets/help.html`; covers all features with navigation overview, per-section guides, and tips; dark/light theme support
- [x] Help menu item added to hamburger menu (between SQL Explorer and About)
- [x] About dialog: dynamic version from BuildConfig (versionName + versionCode)
- [x] Auto-increment versioning: `version.properties` tracks VERSION_MAJOR, VERSION_MINOR, VERSION_CODE; minor version and code auto-increment after each assembleDebug/assembleRelease
- [x] `buildConfig = true` enabled in build features for BuildConfig access

- [x] Account Performance chart: double-tap inline chart opens full-screen chart dialog with zoom/pan/tap-to-select
- [x] Account Performance chart: data points with notes rendered bold (larger radius with white outline)
- [x] Account Performance chart: tapping a noted data point shows the note text in the tooltip (bold second line)
- [x] Dashboard Market Indices: user-configurable order via Settings up/down arrows
- [x] Dashboard Market Indices: drag-and-drop reorder via long-press on dashboard cards; order persisted to SharedPreferences

- [x] Dashboard pie chart legend: vertical dividers added between Ticker, Shares, % columns; consistent gridlines with other tables
- [x] Analyze Price historic prices table: full gridlines (horizontal + vertical) between Period, High, Low columns

## Pending
- [ ] Fix deprecation warning: Icons.Filled.OpenInNew -> Icons.AutoMirrored.Filled.OpenInNew (ItemDetailScreen.kt)
- [ ] Fix deprecation warning: Icons.Filled.ShowChart/TrendingUp -> AutoMirrored versions (DashboardScreen.kt)
- [ ] Fix deprecation warning: Icons.Filled.HelpOutline -> Icons.AutoMirrored.Filled.HelpOutline (MainActivity.kt)
- [ ] Fix deprecation warning: statusBarColor in Theme.kt
