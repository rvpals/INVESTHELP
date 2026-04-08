# Invest Help - Test Checklist

## Items (Merged Item + Position)
- [ ] Items screen shows pie chart with allocation by ticker value
- [ ] Pie chart section collapses/expands on tap
- [ ] STOCK/ETF tabs filter items by type
- [ ] Item card shows ticker, name, account, quantity, cost, value, day/total G/L
- [ ] Clicking item card navigates to Item detail screen
- [ ] Add new item via FAB — name, ticker, type, price fields work
- [ ] Edit existing item metadata (name, type, price)
- [ ] Delete item from detail screen
- [ ] Refresh All updates live prices and recalculates values for all items
- [ ] Same ticker on two different accounts shows as separate rows
- [ ] Delete an account — items under that account cascade delete

## Item Detail Screen
- [ ] Shows aggregate info: total quantity, total value, total cost, total G/L across all accounts
- [ ] Per-account breakdown section shows individual account rows
- [ ] Transactions section lists all transactions for the ticker
- [ ] Analysis Info button fetches Yahoo Finance data and shows bottom sheet
- [ ] Yahoo Finance link opens browser to correct ticker page
- [ ] View Statistics navigates to statistics screen
- [ ] Simulate button navigates to simulation with ticker and shares pre-filled

## Item Statistics
- [ ] Date range selector works (start/end dates)
- [ ] Buy statistics show average/max/min prices
- [ ] Sell statistics show average/max/min prices
- [ ] N/A shown when no transactions in range

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
- [ ] Slices represent aggregated items by ticker (across all accounts)
- [ ] Number of shares displayed inside each slice (white bold text)
- [ ] Legend shows ticker, shares count, and percentage
- [ ] No chart shown when no items exist

## Simulation
- [ ] Time range chips displayed in 3 rows: Week, Month, Year
- [ ] All ranges work: 1W, 2W, 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, MAX
- [x] MAX range works without timeout (uses `range=max`)
- [ ] Chart tap-to-select shows tooltip with price and date
- [ ] Tapping same point again dismisses tooltip
- [ ] Vertical crosshair line appears at selected point
- [ ] Summary card shows correct profit/loss calculation
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
