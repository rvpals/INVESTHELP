# Invest Help - Requirements

## Overview
Android app to track personal investments.

## Storage
- Room (SQLite) for local storage, version 16
- No encryption — database opens directly on app launch

## Data Objects

### Investment Account
- Name
- Description
- Initial value
- Current value: not per-account (items are not tied to accounts); portfolio value is sum of all items

### Investment Item (merged with Position)
- Ticker (sole primary key — one record per ticker, not per account)
- Name
- Type (ETF, Stock, Bond, MutualFund, Crypto, Other)
- Current price (updated atomically per-ticker)
- Quantity (shares held)
- Cost basis
- Current value (from live price refresh)
- Day gain/loss
- Total gain/loss
- Day high price (from Yahoo Finance regularMarketDayHigh)
- Day low price (from Yahoo Finance regularMarketDayLow)
- Auto-created when a transaction references a new ticker (defaults to Stock type)

### Investment Transaction
- Date (defaults to today)
- Time of transaction (optional — button to populate current time)
- Action (Buy / Sell)
- Associated Account (dropdown)
- Ticker (direct text input, auto-uppercased)
- Number of shares
- Price per share
- Total Amount (calculated display with copy button for verification)
- Note (optional, multi-line)

### Bank Transfer
- Date
- Amount
- Associated Account
- Note (optional)

### Account Performance
- Account (FK to investment_accounts, CASCADE delete)
- Total Value (user-entered or pulled from app)
- Date (auto-set to today on record creation; date-only, no time component)
- Note (optional text)
- Unique constraint: one record per account per date (accountId + date)

### Watch List
- Name (user-defined)
- Multiple watch lists supported

### Watch List Item
- Watch List (FK, CASCADE delete)
- Ticker
- Number of shares
- Price when added (can be fetched from Yahoo Finance)
- Date added (auto-set to today)

### CSV Import Mapping
- Import type (PK: Transaction, Position, Performance)
- Column mappings (JSON: CSV column index → app field name)
- Date format mappings (JSON: field name → date format string)

## Features

### Navigation
- **Global top bar** — persistent across all screens:
  - Portfolio value 3D button (tap to navigate to Dashboard, auto-refreshes); shows daily % and all-time % gain/loss below the total value, color-coded green/red
  - Hamburger menu (Accounts, Performance, Watch List, Settings, SQL Explorer, Help, About)
  - About dialog includes "Show Log" button for viewing application log; version displayed dynamically from BuildConfig
- **Bottom nav** — Dashboard, Items, Transfer, Transaction, Simulation (3D gradient icons with shadow)

### Dashboard
- All dashboard sections (Market Indices, Daily Glance, Positions, Position Details) use **CollapsibleCard** — reusable component with title, pin button (top-right), HorizontalDivider between header and content, and expand/collapse toggle; unpinned cards default collapsed, pinned cards default expanded; pin state persisted to SharedPreferences
- **Portfolio button** (top bar) — shows total value on first row with daily change amount in parentheses (e.g. "(+$123.45)") color-coded green/red (hidden when zero); daily % and all-time % gain/loss on second row, color-coded green/red
- **Market index cards** — horizontal scrollable row of small cards; default: NASDAQ (^IXIC), S&P 500 (^GSPC), Dow (^DJI), Gold (GC=F); also available: Russell 2K, Silver, Oil, Bitcoin; each card shows label, price, daily change with percentage; clicking a card opens Yahoo Finance page for the index; customizable in Settings > Preferences; auto-refreshes on app start and with Refresh All
- **Daily Glance** — "Overall Daily" section at top showing Stock and ETF total daily change in $ and %, separated by HorizontalDivider; "By Per Share" checkbox toggles between total value and per-share sorting/display; then top 5 performing and top 5 losing assets today; each row shows ticker, company name, gain/loss $ and %; clickable to navigate to item detail; aggregated per-ticker across all accounts
- **Pie chart** — positions card; shows all items by ticker value with shares labels inside slices; legend limited to top 20 with "More" button to show all; clicking a ticker row in the legend navigates to item detail
- **Position Details** — collapsible card with pin; horizontally scrollable table showing ticker (with 3D icon), shares, current price, total cost, total value, change $ and change %; change computed as currentValue - totalCost; clickable rows navigate to item detail; sortable column headers

### Account Detail
- **Transactions list** — lists all transactions for the account (ordered by date DESC, time DESC)
- No positions tab (items are not tied to accounts)

### Items (unified screen combining item metadata + position tracking)
- **Pie chart** — collapsible chart section showing allocation by ticker value
- **STOCK/ETF tabs** — filter items by type
- **Sort dropdown** — sort items by Ticker (A-Z), Total Value (descending), or Current Price (descending); defaults to Total Value
- **Items list** — card-style rows with alternating background colors; each row shows:
  - Left: 3D ticker icon + Ticker (bold, larger) with company name (smaller, italic) below
  - Right: shares count + Total G/L (bold, larger, color-coded green/red)
  - Secondary line: Price, Value, Day G/L (smaller, muted)
  - Edit button only (no delete in table)
- Add/edit items via form dialog with type selector dropdown (Stock, ETF, Bond, MutualFund, Crypto, Other)
- **Delete** — available in the Edit dialog (red "Delete" button next to "Cancel"); respects "Warn before delete" setting
- **Refresh All** — updates live prices and recalculates values for all items
- One record per ticker (ticker is sole primary key)

### Item Detail
- Header card: type chip + "Current Price: $X.XX"
- Card row 1 (big font): Total Shares, Total Value, Total Cost, Total G/L
- Card row 2 (medium font): Daily G/L, Daily G/L/Share, Daily Min Price, Daily Max Price
- Transactions list for the ticker — each card shows days since transaction date and G/L (current price vs transaction price), colored green/red
- **Analysis Info** — fetches Yahoo Finance quoteSummary (sector, industry, P/E, EPS, 52-week range, profit margins, business summary) displayed in a bottom sheet
- **Yahoo Finance link** — opens ticker page in browser
- **View Statistics** — average/max/min buy/sell prices filterable by date range
- **Simulate** — navigates to simulation pre-filled with ticker and shares

### Transaction Form
- Date, time (optional), account, action (Buy/Sell), ticker, shares, price, total amount, note
- "Analyze Price" button opens price analysis screen (current, avg/max/min, historic high/low)
- "View" button opens item detail for the ticker
- **"Simulate" button** — opens simulation with ticker, shares, and custom day range from transaction date to today; auto-runs on navigation
- Auto-selects first account for new transactions; form state preserved via rememberSaveable

### Transaction List
- Each transaction card shows gain/loss: (current price - transaction price) * shares
- Positive G/L shown in primary color with + prefix; negative in red
- **Multi-select mode** — long-press to enter selection; checkboxes on each card; contextual top bar with selection count, Select All, and Delete actions; bulk delete respects "Warn before delete" setting; account filter works with select-all (only visible transactions selected)

### Settings
- **Preferences tab:**
  - Auto-update position shares toggle
  - Warn before delete toggle (default: on) — when off, skips confirmation dialogs for all delete actions
  - Dashboard Market Indices toggles — choose which market indices to show on the dashboard (8 available, 4 default)
- **Data Management tab:**
  - **Import Data** — 3 import types (Transaction Records, Position Details, Performance Records); each has "Define Mapping" and "Start Import" buttons; shared account selector; column mapping dialog with 3-row preview, auto-mapping with common brokerage aliases (Price→currentPrice, Description→name, etc.), date format options per column, progress bar during import; mappings persisted to database for reuse
  - Position import shows confirmation dialog before overwriting existing data
  - Numeric values with commas (e.g. "92,150.62") handled correctly; non-data rows (blank lines, FOOTNOTES) filtered out
  - Backup folder selection, export, restore

### Simulation
- Accessible from bottom navigation bar, item detail, or transaction form simulate button
- Enter ticker, number of shares
- Time range selection in 3 rows: Week (1W, 2W), Month (1M, 3M, 6M), Year (1Y, 2Y, 5Y, 10Y, MAX)
- **Run Sim** — fetches historical prices, shows:
  - Summary card with start price vs current price, profit/loss amount and percentage
  - Trending line chart with filled area, start price reference line, date labels
  - Tap-to-select on chart points shows tooltip with price and date
- Large ranges (5Y+) use weekly interval; MAX uses Yahoo Finance `range=max`
- **Custom day ranges** from transaction simulation: auto-runs with human-readable label (e.g. "1y 3m", "2m 15d")

### SQL Explorer
- Accessible from hamburger menu in top bar
- Execute raw SQL queries against the database
- Auto-detects query type: SELECT/PRAGMA/EXPLAIN show results table; other statements show success message
- Result table with column headers, horizontal and vertical gridlines, horizontal scrolling, monospace font
- Export results to CSV via share intent (FileProvider)
- Error display for invalid SQL
- **Table browser** — lists all database tables (excludes internal sqlite/room/android tables); click to expand column details (name, type, PK/NN indicators); animated expand/collapse; "Open" button on each table row runs `SELECT * FROM <table>` and shows results; "Erase" button (red) on each table row to delete all entries from that table with confirmation dialog (respects "Warn before delete" setting)
- **Row detail dialog** — click any result row to view all field values untruncated in a scrollable dialog

### Account Performance
- Accessible from hamburger menu in top bar
- Tracks account total value over time for trending analysis
- Screen organized into three **CollapsibleCards** with pin persistence: "Add Performance Record" (default unpinned), "Performance Charts" (default pinned), "Records (N)" (default pinned)
- **Add Record** form: account selector dropdown, total value text field, optional note field, "Pull from App" button (computes current value from items), "Add Record" button
- Records auto-set to current date on creation (date-only, no time component)
- Unique constraint: one record per account per date; duplicate attempts show error dialog
- **Records list** — filterable and sortable; account filter dropdown (Select All/None/individual checkboxes); "Order By" dropdown (Account, Date, Total Value, Note) with Asc/Desc toggle; default: all accounts, Date descending; filter and sort selections persisted to SharedPreferences
- **Edit Note** — dialog to edit the note on an existing record; pre-fills with current note
- **Performance Chart** — multi-account overlay line chart (Canvas-drawn); FilterChip multi-select for accounts in FlowRow (wrapping layout); each account gets distinct color from 8-color palette; time-based shared x-axis; tap-to-select tooltip with account name, value, and date; requires 2+ records per account to display series; "Smooth Curve" checkbox enables cubic Bezier curve smoothing
- **Zoom** — pinch-to-zoom (1x–5x) with two-finger pan; double-tap resets zoom; clipRect clips zoomed lines to data area; x-axis labels update to reflect visible viewport
- Delete respects "Warn before delete" setting
- Deleting an account cascades to delete its performance records

### Watch List
- Accessible from hamburger menu in top bar
- Create multiple named watch lists; switch between them via FilterChip selector
- **Manage lists** — create new, rename, delete (CASCADE deletes all items in the list)
- **Add ticker** — dialog with ticker input (auto-uppercased), shares count, price-when-added; "Fetch" button fetches current price from Yahoo Finance
- **Table view** — horizontally scrollable table showing ticker, shares, current price, added price, change $, change %, added date, delete button
- Change $ = (currentPrice × shares) − (priceWhenAdded × shares); Change % = changeAmount / costBasis × 100
- **Refresh** — button fetches latest prices for all tickers in the selected watch list
- Delete items respects "Warn before delete" setting

### Backup & Restore
- Export all data to JSON file (v3 format; items without accountId)
- Restore from JSON backup file (supports v1, v2, and v3 formats)
- v1 backward compatibility: assigns items to first account, maps numShares to quantity
- v2 backward compatibility: ignores accountId field on items

### Application Log
- In-memory log (AppLog singleton) captures price fetch results, refresh summaries, and per-ticker errors
- Up to 200 entries; oldest entries removed when limit exceeded
- Viewable from About dialog via "Show Log" button
- Log viewer shows entries newest-first with timestamps (MM-dd HH:mm:ss format)
- Clear button to wipe all log entries

### Help
- Accessible from hamburger menu (between SQL Explorer and About)
- HTML-based help guide loaded via WebView from `assets/help.html`
- Covers all features: navigation overview grid, per-section guides (Dashboard, Items, Transactions, Transfers, Simulation, Accounts, Performance, Watch List, Settings, SQL Explorer), and tips
- Styled with dark/light theme support via CSS `prefers-color-scheme`
- Color-coded section borders matching app icon colors

### App Branding
- Custom app icon (invest_help_icon.png)
- Splash screen with app icon on startup (AndroidX SplashScreen API)

### Versioning
- Version managed via `version.properties` at project root (VERSION_MAJOR, VERSION_MINOR, VERSION_CODE)
- Minor version and version code auto-increment after each assembleDebug/assembleRelease build
- `versionName` and `versionCode` in build.gradle.kts read from version.properties
- About dialog shows dynamic version via BuildConfig
