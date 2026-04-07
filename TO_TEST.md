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

## Investment Item - Analysis Info
- [ ] "Analysis Info" button fetches Yahoo Finance data and shows bottom sheet
- [ ] Bottom sheet shows key metrics, price range, financials, business summary
- [ ] Loading spinner shows while fetching
- [ ] Error message displays on failure
- [ ] Button disabled when no ticker set

## Investment Item - Yahoo Finance Link
- [ ] "Yahoo Finance" button opens browser to correct ticker page
- [ ] Button only shown when ticker is set

## Positions Pie Chart
- [ ] Chart section visible when positions have value > 0
- [ ] Pie chart slices match ticker values
- [ ] Legend shows ticker, percentage
- [ ] Total value displayed
- [ ] Section collapses/expands on tap

## Simulation
- [ ] Accessible from Dashboard toolbar icon
- [ ] "Get Price" fetches current price and auto-fills cost field
- [ ] "Run Sim" fetches 2 weeks of history and shows results
- [ ] Summary card shows correct profit/loss calculation
- [ ] Chart shows daily price trend with buy price reference line
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
