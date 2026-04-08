# Invest Help - TODO

## Completed
- [x] Composite PK for positions (ticker + accountId)
- [x] Account value computed from position values
- [x] Custom app icon from invest_help_icon.png
- [x] Splash screen with app icon
- [x] Database migration v4 -> v5
- [x] Transaction table redesign: ticker field, optional time, totalAmount, note (migration v5 -> v6)
- [x] Account detail screen with Positions and Transactions tabs
- [x] Analysis Info button on Item detail (Yahoo Finance quoteSummary)
- [x] Yahoo Finance web link on Item detail
- [x] Pie chart on Positions screen (collapsible, by ticker value)
- [x] Simulation screen (2-week historical chart, profit/loss calculation)
- [x] Auto-create InvestmentItem when adding transaction with new ticker
- [x] Move Transaction/Transfer buttons from Items screen to main navigation
- [x] Global top bar with portfolio value 3D button + hamburger menu
- [x] Bottom nav with 5 items: Dashboard, Positions, Transfer, Transaction, Simulation
- [x] Colorful bottom nav icons with shadow/elevation
- [x] Hamburger menu with Accounts, Items, Settings, About
- [x] Simulation time ranges expanded: 1W, 2W, 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, MAX
- [x] Simulation time range chips grouped in 3 rows (Week, Month, Year)
- [x] Simulation chart tap-to-select with tooltip (price + date)
- [x] Fix MAX time range using Yahoo Finance `range=max` parameter
- [x] Dashboard positions pie chart with shares labels inside slices

## Pending
- [ ] Increment versionCode/versionName for next release
- [ ] Fix deprecation warning: Icons.Filled.ShowChart -> Icons.AutoMirrored.Filled.ShowChart
- [ ] Fix deprecation warning: statusBarColor in Theme.kt
