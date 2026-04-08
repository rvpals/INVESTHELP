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
- Current value (computed from sum of position values)

### Investment Item
- Name
- Ticker symbol (optional)
- Type (ETF, Stock, Bond, MutualFund, Crypto, Other)
- Current price
- Auto-created when a transaction references a new ticker (defaults to Stock type)

### Position
- Ticker (part of composite PK)
- Account (part of composite PK) - same ticker can exist on multiple accounts
- Quantity
- Cost basis
- Current value (from live price refresh)
- Day gain/loss
- Total gain/loss

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

## Features

### Navigation
- **Global top bar** — persistent across all screens when unlocked:
  - Portfolio value 3D button (tap to navigate to Dashboard)
  - Hamburger menu (Accounts, Items, Settings, About)
- **Bottom nav** — Dashboard, Positions, Transfer, Transaction, Simulation (colorful icons with shadow)

### Dashboard
- Accounts list with current values
- **Positions pie chart** — shows all positions by ticker value with shares labels inside slices and legend below
- FAB to add accounts

### Account Detail with Tabs
- **Positions tab** — lists all positions for the account (ticker, value, quantity, cost, day/total gain/loss)
- **Transactions tab** — lists all transactions for the account (ordered by date DESC, time DESC)

### Position Tracking
- Add/edit/delete positions per account
- Same ticker allowed on different accounts (composite key: ticker + account)
- Refresh all positions with live market prices
- Account values auto-update from position values
- **Pie chart** — collapsible chart section showing allocation by ticker value

### Investment Item Detail
- Shows item type, current price, shares owned, total value
- **Analysis Info** — fetches Yahoo Finance quoteSummary (sector, industry, P/E, EPS, 52-week range, profit margins, business summary) displayed in a bottom sheet
- **Yahoo Finance link** — opens ticker page in browser
- **View Statistics** — average/max/min buy/sell prices filterable by date range

### Simulation
- Accessible from bottom navigation bar
- Enter ticker, number of shares
- Time range selection in 3 rows: Week (1W, 2W), Month (1M, 3M, 6M), Year (1Y, 2Y, 5Y, 10Y, MAX)
- **Run Sim** — fetches historical prices, shows:
  - Summary card with start price vs current price, profit/loss amount and percentage
  - Trending line chart with filled area, start price reference line, date labels
  - Tap-to-select on chart points shows tooltip with price and date
- Large ranges (5Y+) use weekly interval; MAX uses Yahoo Finance `range=max`

### Backup & Restore
- Export all data to JSON file
- Restore from JSON backup file

### App Branding
- Custom app icon (invest_help_icon.png)
- Splash screen with app icon on startup (AndroidX SplashScreen API)
