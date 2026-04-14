# Invest Help - Requirements

## Overview
Android app to track personal investments.

## Storage
- Room (SQLite) for local storage
- No encryption — database opens directly on app launch

## Data Objects

### Investment Account
- Name
- Description
- Initial value
- Current value (computed from sum of item values)

### Investment Item (merged with Position)
- Ticker (composite PK part 1)
- Account (composite PK part 2) — same ticker can exist on multiple accounts
- Name (denormalized per-row for same ticker)
- Type (ETF, Stock, Bond, MutualFund, Crypto, Other)
- Current price (denormalized, updated atomically per-ticker)
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
- Date/Time (auto-set on record creation)

## Features

### Navigation
- **Global top bar** — persistent across all screens:
  - Portfolio value 3D button (tap to navigate to Dashboard, auto-refreshes)
  - Hamburger menu (Accounts, Performance, Settings, SQL Explorer, About)
  - About dialog includes "Show Log" button for viewing application log
- **Bottom nav** — Dashboard, Items, Transfer, Transaction, Simulation (colorful icons with shadow)

### Dashboard
- **Market index cards** — horizontal scrollable row of small cards at the top; default: NASDAQ (^IXIC), S&P 500 (^GSPC), Dow (^DJI), Gold (GC=F); also available: Russell 2K, Silver, Oil, Bitcoin; each card shows label, price, daily change with percentage; clicking a card opens Yahoo Finance page for the index; customizable in Settings > Preferences; auto-refreshes on app start and with Refresh All
- Accounts list with current values
- **Pie chart** — collapsible positions card; shows all items by ticker value with shares labels inside slices; legend limited to top 20 with "More" button to show all
- FAB to add accounts

### Account Detail with Tabs
- **Positions tab** — lists all items for the account (ticker, value, quantity, cost, day/total gain/loss)
- **Transactions tab** — lists all transactions for the account (ordered by date DESC, time DESC)

### Items (unified screen combining item metadata + position tracking)
- **Pie chart** — collapsible chart section showing allocation by ticker value
- **STOCK/ETF tabs** — filter items by type
- Item cards show ticker, name, account, quantity, cost, value, day/total gain/loss
- Add/edit items via form dialog with type selector dropdown (Stock, ETF, Bond, MutualFund, Crypto, Other)
- **Refresh All** — updates live prices and recalculates values for all items
- Same ticker allowed on different accounts (composite key: ticker + account)
- Account values auto-update from item values

### Item Detail
- Header card: type chip + "Current Price: $X.XX"
- Card row 1 (big font): Total Shares, Total Value, Total Cost, Total G/L
- Card row 2 (medium font): Daily G/L, Daily G/L/Share, Daily Min Price, Daily Max Price
- Per-account breakdown section
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

### Settings
- **Preferences tab:**
  - Auto-update position shares toggle
  - Warn before delete toggle (default: on) — when off, skips confirmation dialogs for all delete actions
  - Dashboard Market Indices toggles — choose which market indices to show on the dashboard (8 available, 4 default)
- **Data Management tab:**
  - **Import Data** — Import position CSV: opens file picker, shows column mapping dialog with 3 preview rows, account selector, auto-maps matching headers, progress bar during import; upserts into investment_items table
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
- Result table with column headers, horizontal scrolling, monospace font
- Export results to CSV via share intent (FileProvider)
- Error display for invalid SQL
- **Table browser** — lists all database tables (excludes internal sqlite/room/android tables); click to expand column details (name, type, PK/NN indicators); animated expand/collapse
- **Row detail dialog** — click any result row to view all field values untruncated in a scrollable dialog

### Account Performance
- Accessible from hamburger menu in top bar
- Tracks account total value over time for trending analysis
- **Add Record** form: account selector dropdown, total value text field, "Pull from App" button (computes current value from items), "Add Record" button
- Records auto-timestamped with current date/time on creation
- **Records list** — all records ordered newest first, showing account name, date/time, total value, delete button
- **Performance Chart** — multi-account overlay line chart (Canvas-drawn); FilterChip multi-select for accounts; each account gets distinct color from 8-color palette; time-based shared x-axis; tap-to-select tooltip with account name, value, and date; requires 2+ records per account to display series
- **Zoom** — pinch-to-zoom (1x–5x) with two-finger pan; double-tap resets zoom; clipRect clips zoomed lines to data area; x-axis labels update to reflect visible viewport
- Delete respects "Warn before delete" setting
- Deleting an account cascades to delete its performance records

### Backup & Restore
- Export all data to JSON file (v2 format with full merged entity fields)
- Restore from JSON backup file (supports both v1 and v2 formats)
- v1 backward compatibility: assigns items to first account, maps numShares to quantity

### Application Log
- In-memory log (AppLog singleton) captures price fetch results, refresh summaries, and per-ticker errors
- Up to 200 entries; oldest entries removed when limit exceeded
- Viewable from About dialog via "Show Log" button
- Log viewer shows entries newest-first with timestamps (MM-dd HH:mm:ss format)
- Clear button to wipe all log entries

### App Branding
- Custom app icon (invest_help_icon.png)
- Splash screen with app icon on startup (AndroidX SplashScreen API)
