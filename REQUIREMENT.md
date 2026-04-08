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
  - Hamburger menu (Accounts, Settings, About)
- **Bottom nav** — Dashboard, Items, Transfer, Transaction, Simulation (colorful icons with shadow)

### Dashboard
- Accounts list with current values
- **Pie chart** — shows all items by ticker value with shares labels inside slices and legend below
- FAB to add accounts

### Account Detail with Tabs
- **Positions tab** — lists all items for the account (ticker, value, quantity, cost, day/total gain/loss)
- **Transactions tab** — lists all transactions for the account (ordered by date DESC, time DESC)

### Items (unified screen combining item metadata + position tracking)
- **Pie chart** — collapsible chart section showing allocation by ticker value
- **STOCK/ETF tabs** — filter items by type
- Item cards show ticker, name, account, quantity, cost, value, day/total gain/loss
- Add/edit items via form dialog
- **Refresh All** — updates live prices and recalculates values for all items
- Same ticker allowed on different accounts (composite key: ticker + account)
- Account values auto-update from item values

### Item Detail
- Shows aggregate info across all accounts: total quantity, value, cost, gain/loss
- Per-account breakdown section
- Transactions list for the ticker
- **Analysis Info** — fetches Yahoo Finance quoteSummary (sector, industry, P/E, EPS, 52-week range, profit margins, business summary) displayed in a bottom sheet
- **Yahoo Finance link** — opens ticker page in browser
- **View Statistics** — average/max/min buy/sell prices filterable by date range
- **Simulate** — navigates to simulation pre-filled with ticker and shares

### Simulation
- Accessible from bottom navigation bar or item detail
- Enter ticker, number of shares
- Time range selection in 3 rows: Week (1W, 2W), Month (1M, 3M, 6M), Year (1Y, 2Y, 5Y, 10Y, MAX)
- **Run Sim** — fetches historical prices, shows:
  - Summary card with start price vs current price, profit/loss amount and percentage
  - Trending line chart with filled area, start price reference line, date labels
  - Tap-to-select on chart points shows tooltip with price and date
- Large ranges (5Y+) use weekly interval; MAX uses Yahoo Finance `range=max`

### Backup & Restore
- Export all data to JSON file (v2 format with full merged entity fields)
- Restore from JSON backup file (supports both v1 and v2 formats)
- v1 backward compatibility: assigns items to first account, maps numShares to quantity

### App Branding
- Custom app icon (invest_help_icon.png)
- Splash screen with app icon on startup (AndroidX SplashScreen API)
