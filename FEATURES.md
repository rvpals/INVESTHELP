# Invest Help - Features

## Navigation

### Global Top Bar
- Portfolio value 3D button with daily change amount in parentheses (color-coded green/red)
- Second row shows Day % and All-time % gain/loss (color-coded)
- Tapping refreshes all prices and navigates to Dashboard
- Spinner shown during price refresh
- Hamburger menu: Accounts, Performance, Watch List, Settings, SQL Explorer, Help, About

### Bottom Navigation
- 5 tabs: Dashboard, Items, Transfer, Transaction, Simulation
- 3D gradient icons with shadow/elevation effect
- Selected icon slightly larger than unselected

## Dashboard

### Market Indices
- Horizontal scrollable row of cards (NASDAQ, S&P 500, Dow, Gold, Russell 2K, Silver, Oil, Bitcoin)
- Each card shows label, current price, daily change with percentage
- Click to open Yahoo Finance page for the index
- Long-press drag-and-drop reorder; order persisted to SharedPreferences
- Customizable in Settings > Preferences (toggle indices, reorder with arrows)
- Auto-refresh on app start and with Refresh All
- Wrapped in CollapsibleCard with pin persistence

### Daily Glance
- Overall Daily section: Stock and ETF total daily change in $ and %
- "By Per Share" checkbox toggles between total value and per-share display
- Top 5 gainers (green) and top 5 losers (red) today
- Each row: ticker, company name, gain/loss $ and %
- Clickable rows navigate to item detail
- Wrapped in CollapsibleCard with pin persistence

### Positions Pie Chart
- All items by ticker value with shares labels inside slices
- Legend table with gridlines: Ticker, Shares, % columns
- Top 20 limit with "More" button to show all
- Clicking legend rows navigates to item detail
- Wrapped in CollapsibleCard with pin persistence

### Position Details
- Horizontally scrollable table with gridlines
- Columns: Ticker (3D icon), Shares, Price, Cost, Value, Change $, Change %
- Sortable column headers (click to sort asc/desc)
- Clickable rows navigate to item detail
- Wrapped in CollapsibleCard with pin persistence

## Items

- Pie chart section (collapsible) showing allocation by ticker value
- STOCK/ETF tabs filter by type
- Sort dropdown: Ticker, Total Value (default), Current Price
- Card-style rows with alternating backgrounds
  - Left: 3D ticker icon + ticker (bold) + company name (italic)
  - Right: shares count + Total G/L (color-coded)
  - Secondary line: Price, Value, Day G/L
- Edit button per row; Delete available in Edit dialog
- Add/edit via form with type selector (Stock, ETF, Bond, MutualFund, Crypto, Other)
- Refresh All updates live prices for all items
- One record per ticker (ticker is sole primary key)

## Item Detail

- Header: type chip + current price
- Card row 1 (large): Total Shares, Total Value, Total Cost, Total G/L
- Card row 2 (medium): Daily G/L, Daily G/L/Share, Daily Min, Daily Max
- Collapsible Stats section: average/max/min buy/sell prices with date range filter
- Collapsible Transactions section: each card shows days since date and G/L
- Analysis Info button: Yahoo Finance quoteSummary (sector, P/E, EPS, 52-week range, etc.)
- Yahoo Finance link: opens ticker page in browser
- Simulate button: opens simulation with ticker and shares

## Transactions

### Transaction Form
- Fields: date, time (optional), account, action (Buy/Sell), ticker, shares, price, total, note
- Analyze Price button: current price, avg/max/min, historic high/low (week/month/year/max)
- View button: opens item detail for the ticker
- Simulate button: opens simulation from transaction date to today
- Auto-selects first account; form state preserved via rememberSaveable

### Transaction List
- Each card shows G/L: (currentPrice - pricePerShare) * shares (color-coded)
- Multi-select mode: long-press to enter, checkboxes, select all, bulk delete
- Account filter; bulk delete respects "Warn before delete" setting

## Bank Transfers

- Total transfers by account summary card at top
- Individual transfer cards with date, amount, account, note
- Add/edit transfer form

## Simulation

- Ticker and shares input
- Time range chips in 3 rows: Week (1W, 2W), Month (1M, 3M, 6M), Year (1Y, 2Y, 5Y, 10Y, MAX)
- Summary card: start price vs current, profit/loss amount and percentage
- Line chart with filled area, start price reference line
- Tap-to-select with tooltip (price + date)
- Custom day ranges from transaction simulation (auto-runs)
- Large ranges (5Y+) use weekly interval; MAX uses Yahoo Finance `range=max`

## Account Performance

- Three CollapsibleCards: Add Record, Charts, Records
- Add record: account selector, total value, "Pull from App" button, optional note
- One record per account per date (unique constraint)
- Multi-account overlay line chart (Canvas-drawn)
  - FilterChip multi-select in FlowRow
  - Distinct colors per account (8-color palette)
  - Pinch-to-zoom (1x-5x) with two-finger pan
  - Tap-to-select tooltip with account name, value, date
  - "Smooth Curve" checkbox for cubic Bezier smoothing
  - Note indicators: bold data points with white outline
  - Double-tap opens full-screen chart dialog
- Records list: filterable by account, sortable by Account/Date/Value/Note

## Watch List

- Multiple named watch lists via FilterChip selector
- Create, rename, delete watch lists (CASCADE)
- Add ticker: shares, price-when-added, "Fetch" button for current price
- Table with gridlines: Ticker, Shares, Price, Added @, Change $, Change %, Date, Delete
- Refresh all tickers in selected list

## Settings

### Preferences
- Auto-update position shares toggle
- Warn before delete toggle (default: on)
- Dashboard Market Indices: toggles for 8 indices, up/down arrow reorder

### Data Management
- CSV Import: 3 types (Transaction, Position, Performance)
  - Column mapping dialog with 3-row preview
  - Auto-mapping with brokerage aliases
  - Date format options per column
  - Persistent mappings (csv_import_mappings table)
- Backup folder selection (persisted)
- Export to JSON (v3 format)
- Restore from JSON (v1/v2/v3 compatible)

## SQL Explorer

- Raw SQL query execution against Room database
- Auto-detects query type (SELECT/PRAGMA vs DML/DDL)
- Result table with horizontal and vertical gridlines
- CSV export via share intent
- Table browser: list tables, expand column details, Open/Erase buttons
- Row detail dialog for untruncated field values

## Help

- HTML-based guide via WebView (assets/help.html)
- Dark/light theme support
- Navigation overview grid, per-section guides, tips

## Application Log

- In-memory log (up to 200 entries)
- Captures price fetch results, refresh summaries, errors
- Viewable from About dialog > Show Log
- Newest-first with timestamps; clear button
