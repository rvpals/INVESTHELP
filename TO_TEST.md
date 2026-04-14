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
- [ ] Card header: type chip + "Current Price: $X.XX" (renamed from "Price:")
- [ ] Card row 1 (big font, titleMedium): Total Shares, Total Value, Total Cost, Total G/L — all same size
- [ ] Card row 2 (medium font, bodyMedium): Daily G/L, Daily G/L/Share, Daily Min, Daily Max — all same size
- [ ] Total G/L color: green (primary) for positive, red (error) for negative
- [ ] Daily G/L and Daily G/L/Share color: green for positive, red for negative
- [ ] Daily Min/Max show dayLow/dayHigh from Yahoo Finance (0.00 before first refresh)
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
- [ ] Each transaction card shows days since transaction date (e.g. "Apr 01, 2026  (12d)")
- [ ] Each transaction card shows G/L: (currentPrice - pricePerShare) * shares
- [ ] Positive G/L displayed in primary color, negative in error color
- [ ] G/L updates when prices are refreshed

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
- [ ] Portfolio value button visible on all screens
- [ ] Tapping portfolio button navigates to Dashboard
- [ ] Portfolio value auto-refreshes when item values change
- [ ] Hamburger menu opens dropdown with Accounts, Settings, SQL Explorer, About
- [ ] Each menu item navigates to correct screen
- [ ] About dialog shows app name, version, description

## Bottom Navigation
- [ ] 5 items: Dashboard, Items, Transfer, Transaction, Simulation
- [ ] Each icon has distinct color
- [ ] Selected item shows full color, unselected faded
- [ ] Navigation works correctly between all screens
- [ ] Shadow/elevation visible on nav bar

## Dashboard Market Index Cards
- [ ] Market index cards row appears at top of dashboard when indices are enabled
- [ ] Default 4 cards: NASDAQ, S&P 500, Dow, Gold
- [ ] Each card shows index label, current price, and daily change (amount + percentage)
- [ ] Positive change displayed in green, negative in red
- [ ] Cards show "---" placeholder while loading
- [ ] Cards horizontally scrollable when more than 4 are enabled
- [ ] Clicking a market index card opens Yahoo Finance page for that index in browser
- [ ] Indices refresh automatically on app start
- [ ] Indices refresh when tapping portfolio value button (Refresh All)
- [ ] Settings > Preferences: "Dashboard Market Indices" section visible
- [ ] All 8 market index toggles visible and accessible (scrollable preferences)
- [ ] Each of 8 indices has a toggle (NASDAQ, S&P 500, Dow, Gold, Russell 2K, Silver, Oil, Bitcoin)
- [ ] Toggling an index on/off persists after app restart
- [ ] Disabling all indices hides the cards row on dashboard
- [ ] Enabling additional indices adds them to the dashboard row

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

## Database Migration v9 -> v10
- [ ] Fresh install works correctly (version 10)
- [ ] Upgrade from v9 to v10 adds dayHigh, dayLow columns with default 0.0
- [ ] Existing items retain all data after migration
- [ ] dayHigh/dayLow populate correctly after price refresh

## No Auth / Direct Launch
- [ ] App opens directly to Dashboard (no setup or unlock screen)
- [ ] Top bar and bottom nav visible immediately on launch
- [ ] All screens accessible without authentication
- [ ] Backup restore works on fresh install (Settings > Restore)

## SQL Explorer
- [ ] SQL Explorer accessible from hamburger menu
- [ ] Text field accepts SQL input (monospace, multi-line)
- [ ] Run button executes SELECT queries and shows result table
- [ ] Run button executes DML/DDL statements and shows success message
- [ ] PRAGMA queries work (e.g., `PRAGMA table_info(investment_items)`)
- [ ] Result table shows header row with column names (bold)
- [ ] Result table scrolls horizontally for wide results
- [ ] Error messages displayed in red monospace text for invalid SQL
- [ ] Loading spinner shown while query is running
- [ ] Export CSV button enabled only when results have columns
- [ ] Export CSV opens share sheet with CSV file
- [ ] CSV file contains correct headers and data
- [ ] Tables section lists all database tables (excludes sqlite_*, room_*, android_* internal tables)
- [ ] Clicking table name expands to show columns with name, type, PK/NN badges
- [ ] Clicking expanded table name collapses it
- [ ] Clicking different table switches expansion
- [ ] Clicking a result row opens record detail dialog
- [ ] Record detail dialog shows all column names with full untruncated values
- [ ] Record detail dialog scrollable for many fields
- [ ] Record detail dialog Close button dismisses it

## Transaction List Gain/Loss
- [ ] Each transaction card shows G/L line: (currentPrice - pricePerShare) * numberOfShares
- [ ] Positive G/L displayed in primary color with + prefix
- [ ] Negative G/L displayed in red (error color)
- [ ] G/L not shown when ticker has no current price (item not in database)
- [ ] G/L updates reactively when prices are refreshed
- [ ] Both Buy and Sell transactions show G/L correctly

## CSV Position Import
- [ ] "Import Position CSV" button visible in Data Management tab under "Import Data" section
- [ ] File picker opens and accepts CSV files
- [ ] After selecting CSV: mapping dialog appears with column headers
- [ ] 3 rows of preview data shown under each column header
- [ ] Each column has a dropdown to map to: Skip, ticker, name, type, currentPrice, quantity, cost, dayGainLoss, totalGainLoss, value
- [ ] Auto-maps columns whose headers match field names (case-insensitive)
- [ ] Same app field cannot be mapped to two different CSV columns (reassigns)
- [ ] Account selector dropdown in mapping dialog
- [ ] Import button disabled until ticker is mapped and account is selected
- [ ] Cancel button dismisses the dialog
- [ ] Progress dialog shows row count and linear progress bar during import
- [ ] Imported positions appear in Items screen with correct data
- [ ] Existing items are updated (upserted) — unmapped fields keep existing values
- [ ] Rows with blank ticker are skipped
- [ ] CSV with quoted fields (commas inside quotes) parsed correctly
- [ ] Success snackbar shows count of imported positions
- [ ] Error snackbar shown if CSV is empty or unreadable

## Application Log (About > Show Log)
- [ ] About dialog shows "Show Log" button
- [ ] Clicking "Show Log" opens log viewer dialog
- [ ] Log viewer shows "No log entries yet." when log is empty
- [ ] After refreshing prices, log entries appear with timestamps (newest first)
- [ ] Log shows per-ticker failure details (ticker name + error message)
- [ ] Log shows summary messages (e.g. "Updated 5 tickers, 1 failed")
- [ ] Market index fetch results appear in log
- [ ] Portfolio refresh results appear in log
- [ ] Clear button (trash icon) in log viewer title clears all entries
- [ ] Log viewer is scrollable for many entries
- [ ] Log entries capped at 200 (oldest removed when exceeded)

## App Icon & Splash
- [ ] App icon appears correctly on home screen / app drawer
- [ ] Adaptive icon renders properly on API 26+ (round, squircle, etc.)
- [ ] Splash screen shows icon on app cold start
- [ ] Splash screen transitions smoothly to Dashboard
