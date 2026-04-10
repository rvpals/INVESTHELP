# Invest Help - Requirements

## Overview
Android app to track personal investments.

## Storage & Security
- SQLite for local storage
- Database encryption with password (SQLCipher)
- Biometric authentication support

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

## Features

### Navigation
- **Global top bar** — persistent across all screens when unlocked:
  - Portfolio value 3D button (tap to navigate to Dashboard, auto-refreshes)
  - Hamburger menu (Accounts, Settings, SQL Explorer, About)
- **Bottom nav** — Dashboard, Items, Transfer, Transaction, Simulation (colorful icons with shadow)

### Dashboard
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
- Shows aggregate info across all accounts: total quantity, value, cost, gain/loss
- Per-account breakdown section
- Daily change per share displayed in header card
- Transactions list for the ticker
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
- Execute raw SQL queries against the encrypted database
- Auto-detects query type: SELECT/PRAGMA/EXPLAIN show results table; other statements show success message
- Result table with column headers, horizontal scrolling, monospace font
- Export results to CSV via share intent (FileProvider)
- Error display for invalid SQL

### Backup & Restore
- Export all data to JSON file (v2 format with full merged entity fields)
- Restore from JSON backup file (supports both v1 and v2 formats)
- v1 backward compatibility: assigns items to first account, maps numShares to quantity

### App Branding
- Custom app icon (invest_help_icon.png)
- Splash screen with app icon on startup (AndroidX SplashScreen API)
