# Invest Help - Test Checklist

## Items (Ticker-Only Primary Key)
- [x] Items screen shows pie chart with allocation by ticker value
- [x] Pie chart section collapses/expands on tap
- [x] STOCK/ETF tabs filter items by type
- [x] Company name updates from Yahoo Finance after Refresh All
- [x] Add new item via FAB — type selector dropdown works (Stock, ETF, Bond, MutualFund, Crypto, Other)
- [x] Add item: selecting existing ticker from autocomplete auto-fills the type
- [x] Edit existing item — type dropdown shows current type and allows changing
- [x] Delete item from detail screen
- [x] Refresh All updates live prices and recalculates values for all items
- [ ] Only one item record per ticker (no duplicates across accounts)
- [ ] Migration v14->v15: existing duplicate tickers merged correctly (quantities/costs summed)

## Items Screen Layout
- [ ] Sort-by dropdown visible above items list with options: Ticker, Total Value (default), Current Price
- [ ] Changing sort option re-orders the items list correctly
- [ ] Sort selection persists across tab switches (rememberSaveable)
- [ ] Items displayed in a card with alternating row backgrounds (every other row has subtle highlight)
- [ ] Each row shows: TickerIcon3D on left, ticker info in middle, shares/G/L on right, edit button
- [ ] Ticker name in bold titleSmall font; company name below in smaller italic (when different from ticker)
- [ ] Company name truncated with ellipsis if too long (maxLines = 1)
- [ ] Shares count and Total G/L displayed in bold titleSmall font on right side
- [ ] Total G/L color-coded: green for positive, red for negative
- [ ] Secondary row below shows Price, Value, and Day G/L in smaller muted text
- [ ] Day G/L color-coded: green for positive, red for negative
- [ ] No account name shown on item rows (items are not tied to accounts)
- [ ] Clicking a row navigates to Item detail screen
- [ ] Edit button (pencil icon) on each row opens edit dialog
- [ ] No Delete button visible in the table rows
- [ ] Delete button visible in the Edit dialog (only when editing existing item, not when adding new)
- [ ] Delete from Edit dialog respects "Warn before delete" setting
- [ ] TickerIcon3D: gradient-filled rounded box with shadow; color varies per ticker
- [ ] TickerIcon3D: company logo overlays when loaded from CDN; white letter fallback when not
- [ ] Spinner shown in place of edit button when ticker is refreshing
- [ ] Light dividers between rows (not after last row)

## Item Detail Screen
- [x] Card header: type chip + "Current Price: $X.XX" (renamed from "Price:")
- [x] Card row 1 (big font, titleMedium): Total Shares, Total Value, Total Cost, Total G/L — all same size
- [x] Card row 2 (medium font, bodyMedium): Daily G/L, Daily G/L/Share, Daily Min, Daily Max — all same size
- [x] Total G/L color: green (primary) for positive, red (error) for negative
- [x] Daily G/L and Daily G/L/Share color: green for positive, red for negative
- [x] Daily Min/Max show dayLow/dayHigh from Yahoo Finance (0.00 before first refresh)
- [ ] No per-account breakdown section (items are not tied to accounts)
- [x] Analysis Info and Yahoo Finance buttons on same row
- [x] Analysis Info button fetches Yahoo Finance data and shows bottom sheet
- [x] Yahoo Finance link opens browser to correct ticker page
- [x] Simulate button navigates to simulation with ticker and shares pre-filled
- [x] Collapsible "<TICKER> Stats" section expands/collapses on tap
- [x] Stats section: date range selector works (start/end dates)
- [x] Stats section: buy statistics show average/max/min prices
- [x] Stats section: sell statistics show average/max/min prices
- [x] Stats section: N/A shown when no transactions in range
- [x] Collapsible "Transactions" section expands/collapses on tap
- [x] Transactions section lists all transactions for the ticker
- [x] Each transaction card shows days since transaction date (e.g. "Apr 01, 2026  (12d)")
- [x] Each transaction card shows G/L: (currentPrice - pricePerShare) * shares
- [x] Positive G/L displayed in primary color, negative in error color
- [x] G/L updates when prices are refreshed

## Portfolio Value
- [ ] Portfolio value = sum of all item values (not per-account)
- [x] Refresh items (live prices) — portfolio value in top bar should update

## Account Detail
- [ ] Account detail shows summary card and transactions only (no positions tab)
- [x] Transactions tab shows all transactions for the account
- [x] Transactions ordered by date DESC, time DESC
- [x] Tapping a transaction navigates to edit form

## Transaction Form
- [x] Date defaults to today
- [x] Time is optional — "Now" button populates current time, "Clear" removes it
- [x] Ticker field auto-uppercases input
- [x] Calculated total (price * shares) displays correctly
- [x] Copy button copies calculated total to Total Amount field
- [x] Note field accepts multi-line text
- [x] Simulate button opens simulation with ticker, shares, and days from transaction date
- [x] Simulate button disabled when transaction date is today (0 days)
- [x] Simulate button disabled when ticker or shares are empty
- [x] Simulation auto-runs and shows profit/loss from transaction date to now
- [x] Create and Update both work correctly

## Auto-Create Investment Item
- [x] Adding a transaction with a new ticker auto-creates an item (Stock type)
- [x] Adding a transaction with an existing ticker does NOT create a duplicate
- [x] Auto-created item appears in Items list with correct ticker and price
- [x] Auto-update: saving transaction updates the item's quantity and value

## Global Top Bar
- [x] Portfolio value button visible on all screens
- [x] Tapping portfolio button navigates to Dashboard
- [x] Portfolio value auto-refreshes when item values change
- [x] Hamburger menu opens dropdown with Accounts, Settings, SQL Explorer, About
- [x] Each menu item navigates to correct screen
- [x] About dialog shows app name, version, description

## Bottom Navigation
- [x] 5 items: Dashboard, Items, Transfer, Transaction, Simulation
- [x] Each icon has distinct color
- [x] Selected item shows full color, unselected faded
- [x] Navigation works correctly between all screens
- [x] Shadow/elevation visible on nav bar

## 3D Icons (Icon3D)
- [ ] Bottom nav icons rendered as gradient-filled rounded boxes with drop shadow
- [ ] Selected bottom nav icons slightly larger (36dp) than unselected (32dp)
- [ ] White icon on gradient background (light-to-dark of the base color)
- [ ] Hamburger menu button uses 3D icon style with primary color
- [ ] Dropdown menu items (Accounts, Performance, Settings, SQL Explorer, About) each have 3D icons with distinct colors
- [ ] Icons look consistent and not clipped across different screen sizes

## Dashboard Market Index Cards - Drag-and-Drop Reorder
- [ ] Long-press a market index card to start dragging
- [ ] Card floats above others while dragging (elevated z-index)
- [ ] Dragging past half a card width swaps with the neighbor
- [ ] Releasing the card persists the new order to SharedPreferences
- [ ] New order reflected on next app launch
- [ ] New order reflected in Settings > Preferences (arrow buttons match)
- [ ] Drag cancel (e.g. second finger) restores card to original position

## Dashboard Market Index Cards
- [x] Market index cards row appears at top of dashboard when indices are enabled
- [x] Default 4 cards: NASDAQ, S&P 500, Dow, Gold
- [x] Each card shows index label, current price, and daily change (amount + percentage)
- [x] Positive change displayed in green, negative in red
- [x] Cards show "---" placeholder while loading
- [x] Cards horizontally scrollable when more than 4 are enabled
- [x] Clicking a market index card opens Yahoo Finance page for that index in browser
- [x] Indices refresh automatically on app start
- [x] Indices refresh when tapping portfolio value button (Refresh All)
- [x] Settings > Preferences: "Dashboard Market Indices" section visible
- [x] All 8 market index toggles visible and accessible (scrollable preferences)
- [x] Each of 8 indices has a toggle (NASDAQ, S&P 500, Dow, Gold, Russell 2K, Silver, Oil, Bitcoin)
- [x] Toggling an index on/off persists after app restart
- [ ] Up/down arrow buttons visible next to each index in Settings > Preferences
- [ ] First index has disabled up arrow; last index has disabled down arrow
- [ ] Clicking up arrow moves index one position up; clicking down moves it down
- [ ] Reorder persists across app restarts
- [ ] Dashboard reflects the custom order from Settings
- [x] Disabling all indices hides the cards row on dashboard
- [x] Enabling additional indices adds them to the dashboard row

## Global Top Bar - Daily Change Amount
- [ ] Portfolio button shows daily change amount in parentheses after total value (e.g. "(+$123.45)")
- [ ] Positive daily change shown in green
- [ ] Negative daily change shown in red
- [ ] Daily change amount hidden when value is zero (before price refresh)
- [ ] Daily change updates after price refresh

## Global Top Bar - Portfolio Percentages
- [x] Portfolio button shows second row: "(Day: ±X.XX%  All: ±X.XX%)"
- [x] Daily % is green when positive, red when negative
- [x] All-time % is green when positive, red when negative
- [x] Daily % = totalDayGainLoss / (totalPortfolioValue - totalDayGainLoss) * 100
- [x] All-time % = (totalPortfolioValue - totalCost) / totalCost * 100
- [x] Shows 0.00% when no items exist (no division by zero)
- [x] Percentages update after price refresh

## CollapsibleCard Component
- [ ] CollapsibleCard shows HorizontalDivider between header and content when expanded
- [ ] Divider not visible when card is collapsed
- [x] CollapsibleCard shows title, pin button, and expand/collapse arrow
- [x] Tapping header row toggles collapse/expand with animation
- [x] Pin button (right corner): filled icon when pinned, outlined rotated icon when unpinned
- [x] Pinned icon uses primary color; unpinned uses onSurfaceVariant
- [x] Clicking pin toggles pin state
- [x] Pinning a collapsed card auto-expands it
- [x] Pinned cards start expanded on screen load
- [x] Unpinned cards start collapsed on screen load
- [x] Pin state persists after app restart (SharedPreferences)
- [x] Expand/collapse state survives configuration changes (rememberSaveable)

## Dashboard Market Indices (CollapsibleCard)
- [x] Market Indices section wrapped in CollapsibleCard with "Market Indices" title
- [x] Pin button visible and functional
- [x] Unpinned: starts collapsed; pinned: starts expanded
- [x] Pin state persists after app restart
- [x] Content (LazyRow of index cards) shows when expanded
- [x] All existing market index card behavior unchanged (prices, colors, click-to-Yahoo)

## Dashboard Daily Glance
- [x] "Daily Glance" card appears when any items have non-zero dayGainLoss
- [x] Card uses CollapsibleCard with pin button
- [x] Unpinned: starts collapsed; pinned: starts expanded
- [x] "Top Gainers" section header in green, shows up to 5 items with positive dayGainLoss
- [x] "Top Losers" section header in red, shows up to 5 items with negative dayGainLoss
- [x] Items sorted by dayGainLoss (gainers descending, losers ascending by loss)
- [x] Each row shows: ticker (bold) + company name on left, gain/loss $ and % on right
- [x] Positive values in green with "+" prefix; negative in red
- [x] Clicking a row navigates to item detail for that ticker
- [x] Percentages calculated per-ticker across all accounts
- [x] Card hidden when all items have zero dayGainLoss (e.g. before first price refresh)
- [x] Data updates after price refresh

## Dashboard Daily Glance - Overall Daily
- [ ] "Overall Daily" section appears at top of Daily Glance card (above Top Gainers)
- [ ] Shows one row per investment type (ETF, Stock) with daily change in $ and %
- [ ] Positive values shown in green with "+" prefix; negative in red
- [ ] HorizontalDivider separates Overall Daily from Top Gainers/Losers
- [ ] Section hidden when no Stock or ETF items exist
- [ ] Values aggregate across all accounts per type
- [ ] Data updates after price refresh

## Dashboard - No Accounts Section
- [ ] Dashboard no longer shows "Accounts" title or account cards
- [ ] Dashboard no longer shows FAB (add account button)
- [ ] Accounts still accessible from hamburger menu

## Dashboard Positions (CollapsibleCard)
- [x] Positions section wrapped in CollapsibleCard with "Positions" title
- [x] Pin button visible and functional
- [x] Unpinned: starts collapsed; pinned: starts expanded
- [x] Pin state persists after app restart
- [x] Pie chart appears when items exist
- [x] Slices represent aggregated items by ticker (across all accounts)
- [x] Number of shares displayed inside each slice (white bold text)
- [x] Legend shows top 20 items by default with Ticker, Shares, % columns
- [x] Clicking a ticker row in the legend navigates to item detail screen for that ticker
- [x] "More (N remaining)" button appears when >20 items, shows all when clicked
- [x] "Show Less" button collapses back to top 20
- [x] No chart shown when no items exist

## Settings
- [x] "Warn before delete" toggle visible under General section in Preferences tab
- [x] Toggle defaults to ON (checked)
- [x] With toggle ON: deleting items/accounts/transactions/transfers shows confirmation dialog
- [x] With toggle OFF: deleting items/accounts/transactions/transfers happens immediately (no dialog)
- [x] Setting persists after app restart

## Simulation
- [x] Time range chips displayed in 3 rows: Week, Month, Year
- [x] All ranges work: 1W, 2W, 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, MAX
- [x] MAX range works without timeout (uses `range=max`)
- [x] Chart tap-to-select shows tooltip with price and date
- [x] Tapping same point again dismisses tooltip
- [x] Vertical crosshair line appears at selected point
- [x] Summary card shows correct profit/loss calculation
- [x] Custom day range from transaction: auto-runs on navigation, shows human-readable label (e.g. "1y 3m")
- [x] Error handling for invalid ticker

## Backup & Restore
- [x] Backup folder selection persists after closing and reopening Settings
- [x] Backup folder selection persists after app restart
- [x] Previously selected folder displays folder name (not "No folder selected") on return
- [x] Export/Restore buttons enabled immediately when returning to Settings with saved folder
- [ ] Export creates JSON file with v3 format (items without accountId)
- [ ] Restore v3 backup correctly recreates all items (ticker-only PK)
- [x] Restore v1 backup assigns items to first account, maps numShares to quantity
- [ ] Restore v2 backup: ignores accountId, items created with ticker-only PK
- [x] Restored data matches original (accounts, items, transactions, bank transfers)

## Database Migration v9 -> v10
- [x] Fresh install works correctly (version 10)
- [x] Upgrade from v9 to v10 adds dayHigh, dayLow columns with default 0.0
- [x] Existing items retain all data after migration
- [x] dayHigh/dayLow populate correctly after price refresh

## No Auth / Direct Launch
- [x] App opens directly to Dashboard (no setup or unlock screen)
- [x] Top bar and bottom nav visible immediately on launch
- [x] All screens accessible without authentication
- [x] Backup restore works on fresh install (Settings > Restore)

## SQL Explorer
- [x] SQL Explorer accessible from hamburger menu
- [x] Text field accepts SQL input (monospace, multi-line)
- [x] Run button executes SELECT queries and shows result table
- [x] Run button executes DML/DDL statements and shows success message
- [x] PRAGMA queries work (e.g., `PRAGMA table_info(investment_items)`)
- [x] Result table shows header row with column names (bold)
- [x] Result table scrolls horizontally for wide results
- [x] Error messages displayed in red monospace text for invalid SQL
- [x] Loading spinner shown while query is running
- [x] Export CSV button enabled only when results have columns
- [x] Export CSV opens share sheet with CSV file
- [x] CSV file contains correct headers and data
- [x] Tables section lists all database tables (excludes sqlite_*, room_*, android_* internal tables)
- [x] Clicking table name expands to show columns with name, type, PK/NN badges
- [x] Clicking expanded table name collapses it
- [x] Clicking different table switches expansion
- [x] Clicking a result row opens record detail dialog
- [x] Record detail dialog shows all column names with full untruncated values
- [x] Record detail dialog scrollable for many fields
- [x] Record detail dialog Close button dismisses it

## SQL Explorer - Open Table
- [ ] "Open" button visible on each table row in the table browser
- [ ] Clicking "Open" sets SQL field to `SELECT * FROM <table>` and executes immediately
- [ ] Results displayed in the result grid below
- [ ] Clicking a result row opens the record detail dialog (existing behavior)
- [ ] "Open" button works for all listed tables

## SQL Explorer - Erase Table
- [ ] "Erase" button (red text) visible on each table row in the table browser (next to "Open")
- [ ] Clicking "Erase" shows confirmation dialog ("Delete all entries from <table>?")
- [ ] Confirmation dialog respects "Warn before delete" setting (skips dialog when disabled)
- [ ] Confirming erase deletes all rows from the table
- [ ] Table structure remains intact after erase (only data deleted)
- [ ] Success/error feedback shown after erase operation

## Transaction List Gain/Loss
- [x] Each transaction card shows G/L line: (currentPrice - pricePerShare) * numberOfShares
- [x] Positive G/L displayed in primary color with + prefix
- [x] Negative G/L displayed in red (error color)
- [x] G/L not shown when ticker has no current price (item not in database)
- [x] G/L updates reactively when prices are refreshed
- [x] Both Buy and Sell transactions show G/L correctly

## Transaction List Multi-Select
- [ ] Long-press a transaction card enters selection mode
- [ ] In selection mode: checkboxes appear on each card, selected cards highlighted with secondaryContainer
- [ ] Tap a card in selection mode toggles its selection
- [ ] Top bar changes to contextual bar showing "N selected" with close, select all, and delete actions
- [ ] Close button (X) exits selection mode and clears selection
- [ ] Select All toggles all visible (account-filtered) transactions
- [ ] Delete button deletes all selected transactions
- [ ] Bulk delete respects "Warn before delete" setting (shows confirmation dialog)
- [ ] Account filter works correctly with selection (only selects visible transactions)
- [ ] Single delete button (X icon) hidden during selection mode
- [ ] Exiting selection mode restores normal top bar with "Transactions" title and Add button

## CSV Import System (3 Import Types)
- [x] "Import Position CSV" button visible in Data Management tab under "Import Data" section
- [x] File picker opens and accepts CSV files
- [x] After selecting CSV: mapping dialog appears with column headers
- [x] 3 rows of preview data shown under each column header
- [x] Each column has a dropdown to map to: Skip, ticker, name, type, currentPrice, quantity, cost, dayGainLoss, totalGainLoss, value
- [x] Auto-maps columns whose headers match field names (case-insensitive)
- [x] Same app field cannot be mapped to two different CSV columns (reassigns)
- [x] Account selector dropdown in mapping dialog
- [x] Import button disabled until ticker is mapped and account is selected
- [x] Cancel button dismisses the dialog
- [x] Progress dialog shows row count and linear progress bar during import
- [x] Imported positions appear in Items screen with correct data
- [x] Existing items are updated (upserted) — unmapped fields keep existing values
- [x] Rows with blank ticker are skipped
- [x] CSV with quoted fields (commas inside quotes) parsed correctly
- [x] Success snackbar shows count of imported positions
- [x] Error snackbar shown if CSV is empty or unreadable

## CSV Import - Enhanced Parsing & Auto-Mapping
- [ ] Numeric values with commas (e.g. "92,150.62") import correctly for all numeric fields (price, quantity, cost, value, G/L)
- [ ] Auto-mapping recognizes common brokerage aliases: Price→currentPrice, Description→name, Symbol→ticker, Unrealized G/L Amt.→totalGainLoss, Shares→quantity, Today's Value Change→dayGainLoss
- [ ] Auto-mapping prevents duplicate field assignments (first matching column wins)
- [ ] FOOTNOTES sections at bottom of brokerage CSVs are skipped (rows with fewer columns than header)
- [ ] Blank lines in CSV are skipped
- [ ] Position import shows confirmation dialog: "Position details will be refreshed with imported CSV file. Are you sure?"
- [ ] Confirmation dialog "Import" button proceeds with import
- [ ] Confirmation dialog "Cancel" button aborts import
- [ ] Transaction and Performance imports do NOT show the confirmation dialog (proceed directly)

## CSV Import - Reusable Mapping System
- [ ] Data Management shows 3 import types: Transaction Records, Position Details, Performance Records
- [ ] Each import type has "Define Mapping" and "Start Import" buttons
- [ ] Shared account selector above import types
- [ ] "Define Mapping" opens column mapping dialog for that import type
- [ ] Transaction mapping fields: date, time, action, accountName, ticker, numberOfShares, pricePerShare, totalAmount, note
- [ ] Position mapping fields: ticker, name, type, currentPrice, quantity, cost, dayGainLoss, totalGainLoss, value
- [ ] Performance mapping fields: accountName, totalValue, date, note
- [ ] Date format selector available for date columns
- [ ] Saved mappings persist across app restarts (stored in csv_import_mappings table)
- [ ] "Start Import" uses saved mapping to process CSV file
- [ ] Previously saved mapping auto-loads when reopening Define Mapping dialog

## Application Log (About > Show Log)
- [x] About dialog shows "Show Log" button
- [x] Clicking "Show Log" opens log viewer dialog
- [x] Log viewer shows "No log entries yet." when log is empty
- [x] After refreshing prices, log entries appear with timestamps (newest first)
- [x] Log shows per-ticker failure details (ticker name + error message)
- [x] Log shows summary messages (e.g. "Updated 5 tickers, 1 failed")
- [x] Market index fetch results appear in log
- [x] Portfolio refresh results appear in log
- [x] Clear button (trash icon) in log viewer title clears all entries
- [x] Log viewer is scrollable for many entries
- [x] Log entries capped at 200 (oldest removed when exceeded)

## Account Performance
- [x] "Performance" menu item visible in hamburger menu (between Accounts and Settings)
- [x] Tapping "Performance" navigates to Account Performance screen
- [x] Account selector dropdown lists all accounts
- [x] First account auto-selected on load
- [x] Total Value text field accepts decimal input
- [x] "Pull from App" button populates total value from current account value (sum of items)
- [x] "Pull from App" disabled when no account selected
- [x] "Add Record" button disabled until account selected and valid value entered
- [x] Note text field visible in add-record form (optional)
- [x] Adding a record auto-sets current date
- [x] Adding a record with a note stores the note correctly
- [x] Adding a record without a note stores empty note (no error)
- [x] Record appears in records list after adding
- [x] Records list shows account name, date, and total value
- [x] Note text displayed on record card when present (below date)
- [x] No note line shown on record card when note is empty
- [x] Edit button (pencil icon) visible on each record card
- [x] Clicking edit button opens "Edit Note" dialog pre-filled with current note
- [x] Editing and saving a note updates the record correctly
- [x] Clearing the note text and saving removes the note from the record
- [x] Cancel button in edit dialog discards changes
- [x] Delete button on each record removes it
- [x] Delete respects "Warn before delete" setting (confirmation dialog when enabled)
- [x] Performance Chart: FilterChip with FlowRow for multi-account selection (wrapping layout)
- [x] First account auto-selected in chart filter chips on load
- [x] Tapping a chip toggles account on/off for chart display
- [x] Each chip shows colored dot (matching chart line) when selected
- [x] Chart overlays multiple account lines with distinct colors (8-color palette)
- [x] Chart legend row shows colored dots with account names
- [x] Chart shows line graph when at least one selected account has 2+ records
- [x] Chart shows "Need at least 2 records" message when no selected accounts qualify
- [x] Chart Y-axis shows dollar amounts (shared scale across all accounts)
- [x] Chart X-axis shows dates (shared time axis across all accounts)
- [x] Tap on chart point shows tooltip with account name, value, and date
- [x] Tapping same point again dismisses tooltip
- [x] Vertical crosshair line at selected point
- [x] Pinch-to-zoom horizontally (1x to 5x)
- [x] Two-finger pan when zoomed in
- [x] Double-tap resets zoom to 1x
- [x] Zoomed chart clips lines to data area (no overflow into y-axis labels)
- [x] X-axis date labels update to reflect visible viewport when zoomed
- [x] Chart data reloads when toggling account chips
- [x] Deleting an account cascades to delete its performance records

## Account Performance - Full-Screen Chart
- [ ] Double-tap inline chart opens full-screen chart dialog
- [ ] Full-screen dialog has close button (X icon) in top bar
- [ ] Full-screen chart fills entire screen
- [ ] Legend and smooth curve toggle visible in full-screen mode
- [ ] Pinch-to-zoom works in full-screen chart (1x–5x)
- [ ] Two-finger pan works in full-screen chart
- [ ] Tap-to-select with tooltip works in full-screen chart
- [ ] Double-tap in full-screen resets zoom (does not close dialog)
- [ ] Closing full-screen returns to normal performance screen

## Account Performance - Note Indicators on Chart
- [ ] Data points with notes appear larger (bold) on the chart — white outer ring + colored inner circle
- [ ] Data points without notes appear as normal small dots
- [ ] Tapping a bold (noted) data point shows tooltip with two lines: value line + note line (bold)
- [ ] Tapping a normal (no-note) data point shows single-line tooltip (unchanged behavior)
- [ ] Note indicator visibility consistent across zoom levels

## Account Performance - CollapsibleCards
- [ ] Performance screen has three CollapsibleCards: "Add Performance Record", "Performance Charts", "Records (N)"
- [ ] Each card has a pin button; pin states persisted to SharedPreferences
- [ ] "Add Performance Record" default unpinned (collapsed); "Performance Charts" and "Records" default pinned (expanded)
- [ ] Records card title shows count of filtered records (e.g. "Records (15)")

## Account Performance - Smooth Curve
- [ ] "Smooth Curve" checkbox visible in Performance Charts card
- [ ] When unchecked, chart draws straight lines between data points (default)
- [ ] When checked, chart draws smooth cubic Bezier curves between data points
- [ ] Smooth curve toggle applies to all visible account lines simultaneously
- [ ] Toggle state preserved during screen recompositions (rememberSaveable)

## Account Performance - FlowRow Account Chips
- [ ] Account selector chips in Performance Charts wrap to next line when they overflow (FlowRow)
- [ ] All account chips visible without horizontal scrolling
- [ ] Chip layout adjusts correctly on different screen widths

## Account Performance - Duplicate Record Prevention
- [ ] Adding a record for the same account and date as an existing record shows error dialog
- [ ] Error dialog message indicates the duplicate date
- [ ] Dismissing error dialog allows user to change date or account and retry
- [ ] Records with same account but different dates are allowed
- [ ] Records with same date but different accounts are allowed

## Account Performance - Records Filter & Sort
- [ ] Account filter dropdown visible in Records card header area
- [ ] Filter dropdown shows "Select All" and "Select None" options at top
- [ ] Filter dropdown shows individual account checkboxes
- [ ] Checking/unchecking accounts updates the filtered records list immediately
- [ ] Default filter: all accounts selected
- [ ] "Order By" dropdown visible next to filter (options: Account, Date, Total Value, Note)
- [ ] Default order: Date
- [ ] Asc/Desc dropdown visible next to Order By
- [ ] Default direction: Desc (latest first)
- [ ] Changing sort field or direction re-orders the records list correctly
- [ ] Filter and sort selections persist across app sessions (SharedPreferences)
- [ ] Restored filter/sort selections applied correctly on screen load

## Database Migration v10 -> v11
- [x] Fresh install works correctly (version 11)
- [x] Upgrade from v10 to v11 creates account_performance table
- [x] Existing data (accounts, items, transactions, transfers) retained after migration

## Database Migration v11 -> v12
- [x] Fresh install works correctly (version 16)
- [x] Upgrade from v11 to v12 adds note column to account_performance table
- [x] Existing performance records retained after migration with empty note
- [x] New records can be created with a note after migration

## SQL Explorer - Result Grid Lines
- [ ] Result table has vertical dividers between each column (header and data rows)
- [ ] Result table has horizontal dividers between each row (existing behavior)
- [ ] Both horizontal and vertical gridlines use outlineVariant color
- [ ] Header row has thicker (2dp) bottom divider
- [ ] Grid lines visible when scrolling horizontally
- [ ] Clicking a row still opens record detail dialog

## Dashboard Daily Glance - By Per Share
- [ ] "By Per Share" checkbox visible below Overall Daily section
- [ ] Checkbox unchecked by default, label shows "By Total Value"
- [ ] When checked, label changes to "By Per Share"
- [ ] When checked, gainers re-sorted by per-share change (descending)
- [ ] When checked, losers re-sorted by per-share change (ascending)
- [ ] When checked, each row displays per-share $ amount instead of total amount
- [ ] Percentage column unchanged regardless of checkbox state
- [ ] Clicking the label text also toggles the checkbox
- [ ] Checkbox state preserved during configuration changes (rememberSaveable)

## Dashboard Position Details
- [ ] "Position Details" collapsible card visible on Dashboard (after Positions card)
- [ ] Card has pin button; pin state persisted to SharedPreferences (`pin_card_position_details`)
- [ ] Unpinned: starts collapsed; pinned: starts expanded
- [ ] Table shows all tickers with columns: Ticker (icon + name), Shares, Price, Cost, Value, Change $, Change %
- [ ] Ticker column shows 3D icon with company logo (same style as Items screen)
- [ ] Change $ = currentValue - totalCost; Change % = changeAmount / totalCost * 100
- [ ] Positive changes shown in green, negative in red
- [ ] Table horizontally scrollable for all columns
- [ ] Vertical dividers between columns, horizontal dividers between rows
- [ ] Clicking a row navigates to item detail for that ticker
- [ ] Column headers are sortable (click to sort ascending/descending)

## Watch List
- [ ] "Watch List" menu item visible in hamburger menu (between Performance and Settings)
- [ ] Tapping "Watch List" navigates to Watch List screen
- [ ] "+" button at top creates a new watch list (name dialog)
- [ ] Created watch list appears as a FilterChip
- [ ] Tapping a FilterChip selects that watch list and loads its items
- [ ] First watch list auto-selected on load
- [ ] Rename button (pencil icon) opens rename dialog with current name pre-filled
- [ ] Renaming a watch list updates the chip label
- [ ] Delete button (trash icon) deletes the watch list (respects "Warn before delete" setting)
- [ ] Deleting a watch list removes it and all its items (CASCADE)
- [ ] "Add Ticker" button opens add ticker dialog
- [ ] Ticker field auto-uppercases input
- [ ] "Fetch" button fetches current price from Yahoo Finance and fills "Price When Added"
- [ ] Shares and Price fields accept decimal input
- [ ] "Add" button disabled until ticker, shares, and price are all valid
- [ ] Added ticker appears in the table with correct data
- [ ] Table shows: Ticker, Shares, Price (current), Added @ (price when added), Change $, Change %, Date, Delete button
- [ ] Change $ = (currentPrice * shares) - (priceWhenAdded * shares)
- [ ] Change % = changeAmount / costBasis * 100
- [ ] Positive changes shown in green with "+" prefix; negative in red
- [ ] Date column shows the date the ticker was added (MM/dd/yy format)
- [ ] Delete button on each row removes the ticker (respects "Warn before delete" setting)
- [ ] Refresh button fetches latest prices for all tickers in the list
- [ ] Loading spinner shown while refreshing prices
- [ ] Table horizontally scrollable
- [ ] Empty state shown when watch list has no tickers
- [ ] Empty state shown when no watch lists exist (with "Create Watch List" button)
- [ ] Multiple watch lists can coexist with different tickers

## Database Migration v12 -> v13
- [ ] Fresh install works correctly (version 14)
- [ ] Upgrade from v12 to v13 creates watch_lists and watch_list_items tables
- [ ] Existing data (accounts, items, transactions, transfers, performance) retained after migration
- [ ] New watch lists and items can be created after migration

## Database Migration v13 -> v14
- [ ] Fresh install works correctly (version 15)
- [ ] Upgrade from v13 to v14 creates csv_import_mappings table
- [ ] Existing data (accounts, items, transactions, transfers, performance, watch lists) retained after migration
- [ ] New CSV import mappings can be created and persisted after migration

## Database Migration v15 -> v16
- [ ] Fresh install works correctly (version 16)
- [ ] Upgrade from v15 to v16 converts account_performance dateTime (epoch seconds) to date (epoch days)
- [ ] Upgrade from v15 to v16 adds unique index on (accountId, date)
- [ ] Existing performance records retained after migration with dates converted correctly
- [ ] Duplicate records (same account, same day) resolved via INSERT OR REPLACE during migration

## Database Migration v14 -> v15
- [ ] Fresh install works correctly (version 16)
- [ ] Upgrade from v14 to v15 recreates investment_items table with ticker-only PK
- [ ] Duplicate tickers (same ticker across different accounts) merged: quantity, cost, value summed; dayHigh/dayLow take MAX
- [ ] Items with unique tickers retained with all data intact
- [ ] accountId column no longer exists in investment_items table
- [ ] All other tables (accounts, transactions, transfers, performance, watch lists, csv_import_mappings) retained after migration

## Help Screen
- [ ] "Help" menu item visible in hamburger menu (between SQL Explorer and About)
- [ ] Tapping "Help" navigates to Help screen
- [ ] Help screen loads HTML content from assets/help.html via WebView
- [ ] Navigation overview grid shows all 10 screens with correct labels
- [ ] All feature sections present: Dashboard, Items, Transactions, Transfers, Simulation, Accounts, Performance, Watch List, Settings, SQL Explorer, Top Bar, Tips
- [ ] Dark theme: dark background with light text and colored section borders
- [ ] Light theme: light background with dark text and colored section borders
- [ ] Content scrollable for full page
- [ ] No JavaScript enabled (security)

## About Dialog - Dynamic Version
- [ ] About dialog shows "Version X.Y (Z)" where X.Y is versionName and Z is versionCode
- [ ] Version matches values in version.properties file

## Auto-Increment Versioning
- [ ] version.properties exists at project root with VERSION_MAJOR, VERSION_MINOR, VERSION_CODE
- [ ] After assembleRelease, VERSION_MINOR increments by 1
- [ ] After assembleRelease, VERSION_CODE increments by 1
- [ ] VERSION_MAJOR unchanged after build (manual edit only)
- [ ] Built APK uses the pre-increment version (increment happens after build)

## App Icon & Splash
- [x] App icon appears correctly on home screen / app drawer
- [x] Adaptive icon renders properly on API 26+ (round, squircle, etc.)
- [x] Splash screen shows icon on app cold start
- [x] Splash screen transitions smoothly to Dashboard
