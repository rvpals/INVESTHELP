# Invest Help - Test Checklist

## Items (Merged Item + Position)
- [ ] Items screen shows pie chart with allocation by ticker value
- [ ] Pie chart section collapses/expands on tap
- [ ] STOCK/ETF tabs filter items by type
- [ ] Item card shows company logo (or letter fallback), ticker, full name, account, quantity, cost, value, day/total G/L
- [ ] Company name updates from Yahoo Finance after Refresh All
- [ ] Clicking item card navigates to Item detail screen
- [ ] Add new item via FAB — type selector dropdown works (Stock, ETF, Bond, MutualFund, Crypto, Other)
- [ ] Add item: selecting existing ticker from autocomplete auto-fills the type
- [ ] Edit existing item — type dropdown shows current type and allows changing
- [ ] Type change syncs across all accounts for the same ticker
- [ ] Delete item from detail screen
- [ ] Refresh All updates live prices and recalculates values for all items
- [ ] Same ticker on two different accounts shows as separate rows
- [ ] Delete an account — items under that account cascade delete

## Item Detail Screen
- [ ] Shows aggregate info: total quantity, total value, total cost, daily/share, day G/L, total G/L across all accounts
- [ ] Daily/Share column shows per-share daily price change with correct color (green/red)
- [ ] Per-account breakdown section shows individual account rows
- [ ] Analysis Info and Yahoo Finance buttons on same row
- [ ] Analysis Info button fetches Yahoo Finance data and shows bottom sheet
- [ ] Yahoo Finance link opens browser to correct ticker page
- [ ] Simulate button navigates to simulation with ticker and shares pre-filled
- [ ] Collapsible "<TICKER> Stats" section expands/collapses on tap
- [ ] Stats section: date range selector works (start/end dates)
- [ ] Stats section: buy statistics show average/max/min prices
- [ ] Stats section: sell statistics show average/max/min prices
- [ ] Stats section: N/A shown when no transactions in range
- [ ] Collapsible "Transactions" section expands/collapses on tap
- [ ] Transactions section lists all transactions for the ticker

## Account Value from Items
- [ ] Add items to an account, verify account value = sum of item values
- [ ] Refresh items (live prices) — account values on dashboard should update
- [ ] Account with no items should show $0.00 value

## Account Detail Tabs
- [ ] Positions tab shows all items for the account
- [ ] Transactions tab shows all transactions for the account
- [ ] Transactions ordered by date DESC, time DESC
- [ ] Tapping a position row navigates to Item detail
- [ ] Tapping a transaction navigates to edit form

## Transaction Form
- [ ] Date defaults to today
- [ ] Time is optional — "Now" button populates current time, "Clear" removes it
- [ ] Ticker field auto-uppercases input
- [ ] Calculated total (price * shares) displays correctly
- [ ] Copy button copies calculated total to Total Amount field
- [ ] Note field accepts multi-line text
- [ ] Simulate button opens simulation with ticker, shares, and days from transaction date
- [ ] Simulate button disabled when transaction date is today (0 days)
- [ ] Simulate button disabled when ticker or shares are empty
- [ ] Simulation auto-runs and shows profit/loss from transaction date to now
- [ ] Create and Update both work correctly

## Auto-Create Investment Item
- [ ] Adding a transaction with a new ticker auto-creates an item (Stock type)
- [ ] Adding a transaction with an existing ticker does NOT create a duplicate
- [ ] Auto-created item appears in Items list with correct ticker and price
- [ ] Auto-update: saving transaction updates the item's quantity and value

## Global Top Bar
- [ ] Portfolio value button visible on all screens when unlocked
- [ ] Tapping portfolio button navigates to Dashboard
- [ ] Portfolio value auto-refreshes when item values change
- [ ] Hamburger menu opens dropdown with Accounts, Settings, About
- [ ] Each menu item navigates to correct screen
- [ ] About dialog shows app name, version, description

## Bottom Navigation
- [ ] 5 items: Dashboard, Items, Transfer, Transaction, Simulation
- [ ] Each icon has distinct color
- [ ] Selected item shows full color, unselected faded
- [ ] Navigation works correctly between all screens
- [ ] Shadow/elevation visible on nav bar

## Dashboard Pie Chart
- [ ] Pie chart appears when items exist
- [ ] Positions card collapses/expands on header tap
- [ ] Slices represent aggregated items by ticker (across all accounts)
- [ ] Number of shares displayed inside each slice (white bold text)
- [ ] Legend shows top 20 items by default with Ticker, Shares, % columns
- [ ] "More (N remaining)" button appears when >20 items, shows all when clicked
- [ ] "Show Less" button collapses back to top 20
- [ ] No chart shown when no items exist

## Settings
- [ ] "Warn before delete" toggle visible under General section in Preferences tab
- [ ] Toggle defaults to ON (checked)
- [ ] With toggle ON: deleting items/accounts/transactions/transfers shows confirmation dialog
- [ ] With toggle OFF: deleting items/accounts/transactions/transfers happens immediately (no dialog)
- [ ] Setting persists after app restart

## Simulation
- [ ] Time range chips displayed in 3 rows: Week, Month, Year
- [ ] All ranges work: 1W, 2W, 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, MAX
- [x] MAX range works without timeout (uses `range=max`)
- [ ] Chart tap-to-select shows tooltip with price and date
- [ ] Tapping same point again dismisses tooltip
- [ ] Vertical crosshair line appears at selected point
- [ ] Summary card shows correct profit/loss calculation
- [ ] Custom day range from transaction: auto-runs on navigation, shows human-readable label (e.g. "1y 3m")
- [ ] Error handling for invalid ticker

## Backup & Restore
- [ ] Export creates JSON file with v2 format (includes all merged entity fields)
- [ ] Restore v2 backup correctly recreates all items with full data
- [ ] Restore v1 backup assigns items to first account, maps numShares to quantity
- [ ] Restored data matches original (accounts, items, transactions, bank transfers)

## Database Migration v8 -> v9
- [ ] Fresh install works correctly
- [ ] Upgrade from v8 to v9 merges positions into investment_items
- [ ] Position data (quantity, cost, value, G/L) preserved after migration
- [ ] Item metadata (name, type, price) preserved after migration
- [ ] Items without positions are dropped (cannot assign accountId)

## App Icon & Splash
- [ ] App icon appears correctly on home screen / app drawer
- [ ] Adaptive icon renders properly on API 26+ (round, squircle, etc.)
- [ ] Splash screen shows icon on app cold start
- [ ] Splash screen transitions smoothly to auth screen
