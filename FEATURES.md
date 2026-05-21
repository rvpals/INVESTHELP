# Invest Help - Features

## Navigation

### Global Top Bar
- Portfolio value 3D button with daily change amount in parentheses (color-coded green/red)
- Second row shows Day % and All-time % gain/loss (color-coded)
- Tapping refreshes all prices and navigates to Dashboard
- Spinner shown during price refresh
- Refresh status bar: temporary bar below top bar showing "Updating [TICKER]" with price/share, change $ and change % (color-coded green/red); auto-hides when complete
- Hamburger menu: Accounts, Watch List, Settings, SQL Explorer, Help, About

### Dashboard Portfolio Summary
- Collapsible "Portfolio Summary" card with pin persistence
- Total portfolio value displayed in headlineLarge bold (left-aligned)
- Today's gain/loss: amount + percentage + "Today's gain/loss" label (color-coded green/red)
- Mini line chart (140dp) with dashed horizontal grid lines and right-side Y-axis labels ($XXK format)
- Date range labels below chart (full date format: "MMM dd, yyyy")
- All-time percentage centered below chart (e.g., "+19.00% all time")
- "Refreshed: MMM dd, h:mm a" label showing last price refresh time (persisted across app restarts)
- Click chart opens full-screen Change History dialog with zoomable multi-series chart (Total/ETF/Stock lines) + "Change Value This Week So Far" summary + grid data table with daily change columns

### Bottom Navigation
- 5 tabs: Dashboard, Items, Performance, Transaction, Simulation
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
- Brokerage-style card rows with thin dividers (inspired by Chase app layout)
  - Left: 3D ticker icon + ticker (bold) + uppercase company name
  - Below ticker: current price with day change $ and % (color-coded)
  - Right: total position value + gain/loss badge (green/red chip)
- Edit button per row; Delete available in Edit dialog
- Add/edit via form with type selector (Stock, ETF, Bond, MutualFund, Crypto, Other); quantity field has up/down arrows for +1/-1 adjustment
- Refresh All updates live prices for all items
- One record per ticker (ticker is sole primary key)

## Item Detail

Two tabs: **Details** and **Price History**

### Details Tab
- Header: company logo icon (48dp, cached from DB) + ticker (bold) + company name + type chip + current price
- Card row 1 (large): Total Shares, Total Value, Total Cost, Total G/L
- Card row 2 (medium): Daily G/L, Daily G/L/Share, Daily Min, Daily Max
- Collapsible Stats section: average/max/min buy/sell prices with date range filter
- Collapsible Transactions section: each card shows days since date and G/L
- Collapsible "Analysis Info" panel: auto-fetches Yahoo Finance quoteSummary (sector, P/E, EPS, 52-week range, profit margins, business summary) on screen load; displayed inline
- Yahoo Finance link: opens ticker page in browser (full-width button)
- Simulate button: opens simulation with ticker and shares

### Price History Tab
- Timeframe radio buttons: Hourly, Daily, Monthly, Yearly
- Hint text below selector showing what each timeframe means (e.g., "Today's market hours (Every Hour)")
- Interval radio buttons displayed in two rows for full visibility
- Hourly intervals: Every Hour (1h), 30 Minutes (30m), 15 Minutes (15m), 5 Minutes (5m), 1 Minute (1m)
- Hourly: market hours for today (user-selected interval)
- Daily: last 60 days (1d interval)
- Monthly: last 13 months (1mo interval)
- Yearly: last 15 years (1mo interval)
- Summary cards: Average, Max, Min of the result prices
- Line chart: Canvas-drawn with all data points; pinch-to-zoom (1x–5x) with pan; tap-to-select with tooltip (price + date); double-tap to reset; filled area under curve; Y-axis price labels, X-axis date labels
- Grid table: row number, date/time, closing price with horizontal and vertical gridlines

### Delete Item
- Delete button (trash icon, red tint) in top app bar
- Respects "Warn before delete" setting (shows confirmation dialog when enabled)

## Transactions

### Transaction Form
- Fields: date, time (optional), action (Buy/Sell), ticker (editable with filtered dropdown suggestions), shares, price, total, note
- Transactions are not tied to accounts (account-independent)
- Analyze Price button: current price, avg/max/min, historic high/low (week/month/year/max)
- View button: opens item detail for the ticker
- Update/Create and Simulate buttons fixed at bottom of screen (not scrollable)
- Simulate button: opens simulation from transaction date to today
- Form state preserved via rememberSaveable

### Transaction List
- Each card shows G/L: (currentPrice - pricePerShare) * shares (color-coded)
- Multi-select mode: long-press to enter, checkboxes, select all, bulk delete
- Bulk delete respects "Warn before delete" setting

## Simulation

- Ticker and shares input
- Time range chips in 3 rows: Week (1W, 2W), Month (1M, 3M, 6M), Year (1Y, 2Y, 5Y, 10Y, MAX)
- Summary card: start price vs current, profit/loss amount and percentage
- Line chart with filled area, start price reference line
- Tap-to-select with tooltip (price + date)
- Custom day ranges from transaction simulation (auto-runs)
- Large ranges (5Y+) use weekly interval; MAX uses Yahoo Finance `range=max`

## Accounts

- Account list with name, description, initial value, and last value (from most recent performance record)
- Account detail screen with:
  - Info card showing initial value and last value
  - Interactive performance line chart (pinch-to-zoom, pan, tap-to-select tooltip, double-tap reset); shown when 2+ performance records exist
  - Performance records table (Date, Value, Note columns) with grid lines, alternating row colors, newest first
- lastValue and lastUpdatedOn auto-updated when a new performance record is saved

## Account Performance

- Four CollapsibleCards: Add Record, Charts, Chart Data, Records
- Add record: account selector, total value, "Pull from App" button, "Recent" button (loads latest record value), optional note
- Add record: mini chart below form fields showing selected account's history (when 2+ records exist)
- Chart Data: collapsible table showing Account, Date, Value for all data points in the chart
- One record per account per date (unique constraint)
- Multi-account overlay line chart (Canvas-drawn)
  - FilterChip multi-select in FlowRow
  - Distinct colors per account (8-color palette)
  - Pinch-to-zoom (1x-5x) with two-finger pan
  - Tap-to-select tooltip with account name, value, date
  - "Smooth Curve" checkbox for cubic Bezier smoothing
  - Note indicators: bold data points with white outline
  - Double-tap opens full-screen chart dialog
- Records list: grid table with horizontal and vertical gridlines, header row, alternating row colors; filterable by account, sortable by Account/Date/Value/Note

## Watch List

- Each watch list displayed as its own collapsible panel (all visible simultaneously)
- Create, rename, delete watch lists (CASCADE)
- Add ticker: shares, price-when-added, "Fetch" button for current price, optional reminder
- Table with gridlines: Ticker (clickable, navigates to Item Detail), Shares, Price, Added @, Change $, Change %, Date, Delete, Reminder bell
- Reminder bell icon: highlighted when reminder is active; tap to set/edit/clear reminder
- Reminders: date/time picker + message; scheduled notification via AlarmManager; fires even in doze mode
- Notification tap: opens Item Detail screen for the ticker
- Refresh All button in header refreshes prices across all watch lists

## Settings

### Preferences
- **Themes** (collapsible panel): 22 selectable color themes (Default Green, Ocean Blue, Sunset Orange, Midnight Purple, Forest Moss, Ruby Red, Arctic Ice, Gold Rush, Sakura Pink, Charcoal Dark, Lavender Fields, Copper Bronze, Emerald Gem, Slate Blue, Mocha Coffee, Navy Marine, Tropical Mint, Wine Burgundy, Desert Sand, Nordic Pine, Fidelity, Chase); each theme defines full light and dark color schemes; selection persisted to SharedPreferences; instant apply without restart
- **Auto Update Change History when refresh**: toggle (default: off); when on, records ETF/Stock/Total values and daily change values to change_history table after price refresh
- **Auto Refresh All**: toggle (default: off); when on, shows interval selector
  - Interval options: 5 min, 30 min, 1 hr, 5 hr, Market close daily
  - Uses WorkManager for reliable periodic background refresh
  - Foreground notification shown during refresh ("Refreshing prices...")
  - Completion notification shows ticker count and failures
  - Respects "Auto Update Change History" setting
- Auto-update position shares toggle
- Warn before delete toggle (default: on)
- **Dashboard Market Indices** (collapsible panel): toggles for 8 indices, up/down arrow reorder

### Data Management
- CSV Import: 3 types (Transaction, Position, Performance)
  - Column mapping dialog with 3-row preview (Transaction, Performance)
  - Auto-mapping with brokerage aliases
  - Date format options per column
  - Persistent mappings (csv_import_mappings table)
  - Clear (x) button per type: erases all entries from the corresponding table with confirmation dialog
- **Position Import (enhanced)**:
  - Full-screen mapping editor (replaces dialog) with back navigation
  - Top bar actions: Load, Save As, Save
  - "Save As" prompts for a name to store the mapping as a reusable profile
  - "Load" shows all saved named mappings with delete option
  - Saved mappings list shown at bottom of the mapping screen (clickable to load)
  - "Start Import" prompts user to select a mapping (Default active or a saved named mapping)
  - Detailed import result log after completion: summary counts (New/Updated/Skipped) + per-ticker entries showing status and field changes
- Named mapping profiles stored in `csv_named_mappings` table
- Backup folder selection (persisted)
- Export to JSON (v4 format)
- Restore from JSON (v1/v2/v3/v4 compatible)

## SQL Explorer

- Raw SQL query execution against Room database
- Auto-detects query type (SELECT/PRAGMA vs DML/DDL)
- Result table with horizontal and vertical gridlines and alternating row colors
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
