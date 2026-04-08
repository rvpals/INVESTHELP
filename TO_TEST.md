# Invest Help - Test Checklist

## Position Composite Key
- [ ] Add same ticker (e.g. AAPL) to two different accounts - should succeed
- [ ] Edit a position - should update correct ticker+account combo
- [ ] Delete a position - should only delete the specific ticker+account
- [ ] Delete an account - positions under that account should cascade delete

## Account Value from Positions
- [ ] Add positions to an account, verify account value = sum of position values
- [ ] Refresh positions (live prices) - account values on dashboard should update
- [ ] Account with no positions should show $0.00 value

## Account Detail Tabs
- [ ] Positions tab shows all positions for the account
- [ ] Transactions tab shows all transactions for the account
- [ ] Transactions ordered by date DESC, time DESC
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
- [ ] Adding a transaction with a new ticker auto-creates an InvestmentItem (Stock type)
- [ ] Adding a transaction with an existing ticker does NOT create a duplicate item
- [ ] Auto-created item appears in Items list with correct ticker and price
- [ ] Updating a transaction with a new ticker also auto-creates item

## Investment Item - Analysis Info
- [ ] "Analysis Info" button fetches Yahoo Finance data and shows bottom sheet
- [ ] Bottom sheet shows key metrics, price range, financials, business summary
- [ ] Loading spinner shows while fetching
- [ ] Error message displays on failure
- [ ] Button disabled when no ticker set

## Investment Item - Yahoo Finance Link
- [ ] "Yahoo Finance" button opens browser to correct ticker page
- [ ] Button only shown when ticker is set

## Positions Pie Chart (Positions Screen)
- [ ] Chart section visible when positions have value > 0
- [ ] Pie chart slices match ticker values
- [ ] Legend shows ticker, percentage
- [ ] Total value displayed
- [ ] Section collapses/expands on tap

## Dashboard Pie Chart
- [ ] Pie chart appears when positions exist
- [ ] Slices represent aggregated positions by ticker (across all accounts)
- [ ] Number of shares displayed inside each slice (white bold text)
- [ ] Legend shows ticker, shares count, and percentage
- [ ] No chart shown when no positions exist

## Global Top Bar
- [ ] Portfolio value button visible on all screens when unlocked
- [ ] Tapping portfolio button navigates to Dashboard
- [ ] Portfolio value updates as positions change
- [ ] Hamburger menu opens dropdown with Accounts, Items, Settings, About
- [ ] Each menu item navigates to correct screen
- [ ] About dialog shows app name, version, description

## Bottom Navigation
- [ ] 5 items: Dashboard, Positions, Transfer, Transaction, Simulation
- [ ] Each icon has distinct color
- [ ] Selected item shows full color, unselected faded
- [ ] Navigation works correctly between all screens
- [ ] Shadow/elevation visible on nav bar

## Simulation
- [ ] Time range chips displayed in 3 rows: Week, Month, Year
- [ ] All ranges work: 1W, 2W, 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, MAX
- [x] MAX range works without timeout (uses `range=max`)
- [ ] Chart tap-to-select shows tooltip with price and date
- [ ] Tapping same point again dismisses tooltip
- [ ] Vertical crosshair line appears at selected point
- [ ] Summary card shows correct profit/loss calculation
- [ ] Error handling for invalid ticker

## Database Migration v5 -> v6
- [ ] Fresh install works correctly
- [ ] Upgrade from v5 to v6 migrates transactions (investmentItemId -> ticker)
- [ ] Existing transaction data preserved after migration

## App Icon & Splash
- [ ] App icon appears correctly on home screen / app drawer
- [ ] Adaptive icon renders properly on API 26+ (round, squircle, etc.)
- [ ] Splash screen shows icon on app cold start
- [ ] Splash screen transitions smoothly to auth screen
