# Invest Help - Testing Checklist

## PWA - Recent Changes
- [ ] Sharpe Ratio: menu item visible in hamburger menu
- [ ] Sharpe Ratio: computes on page load with loading state
- [ ] Sharpe Ratio: risk-free rate field accepts decimal input (e.g. 4.5)
- [ ] Sharpe Ratio: lookback period chips (6 months, 1 year, 2 years) trigger recompute
- [ ] Sharpe Ratio: result shows Sharpe value with color-coded interpretation label
- [ ] Sharpe Ratio: metrics card shows annualized return, volatility, period, date
- [ ] Sharpe Ratio: daily returns chart renders green fills above zero / red below
- [ ] Sharpe Ratio: chart hover tooltip shows date and return %
- [ ] Sharpe Ratio: skipped tickers section shown when any ticker fails to fetch
- [ ] Dividend tab: shows on Positions screen as 4th tab
- [ ] Dividend tab: total annual income card shows correct sum
- [ ] Dividend tab: Stock and ETF pie charts render with exploding largest slice
- [ ] Dividend tab: sort buttons (Annual, Div/Share, Ticker, Shares) change table order
- [ ] Dividend tab: clicking a row navigates to item detail
- [ ] Dividend tab: only tickers with dividendRate > 0 appear
- [ ] About dialog: "What's New" section visible with recent features listed
- [ ] Help: Dividend Tab section present
- [ ] Service worker caching: verify new JS/CSS loads without manual refresh
- [ ] About > "Refresh App" button: clears cache and reloads with latest code
- [ ] snapshot.html generated after Refresh All (check PWA/public/snapshot.html)
- [ ] Dashboard Daily Glance: verify red color on negative daily change (Stock/ETF totals)
- [ ] Dividend rate: verify non-zero for dividend-paying tickers (AAPL, MSFT, VYM) after Refresh All
- [ ] Dividend display: "Div: $X.XX/yr" on positions list and item detail cards
- [ ] Item detail action buttons: Edit, Delete, Yahoo, Simulate, Watch List, Report all functional
- [ ] Full Yahoo Report dialog: tabs load data (Market Data, Valuation, Financials, Profile)
- [ ] Settings > Server Log tab: shows server log entries, clear button works
- [ ] Settings Preferences: collapsible cards (General, Theme, NDA, Dashboard Cards, Market Indices)
- [ ] Settings Data Management: collapsible cards (Backup & Restore, Import Data)
- [ ] Next Day Actions > Run Scan: completes without crash
- [ ] Backup import from Android: watch lists import correctly
- [ ] Tab styling: visible borders, hover highlight, active tab highlight

## Android - Recent Changes
- [ ] Sharpe Ratio: menu item visible in hamburger menu (bar chart icon)
- [ ] Sharpe Ratio: auto-computes on screen open with per-ticker progress updates
- [ ] Sharpe Ratio: risk-free rate text field accepts decimal input; defaults to 5.0
- [ ] Sharpe Ratio: lookback period chips (6 months, 1 year, 2 years) selectable
- [ ] Sharpe Ratio: Calculate button triggers recompute with current settings
- [ ] Sharpe Ratio: result shows large Sharpe value with colored interpretation chip
- [ ] Sharpe Ratio: metrics card shows annualized return, volatility, risk-free rate, period, date
- [ ] Sharpe Ratio: daily returns chart renders green fill above zero / red below zero line
- [ ] Sharpe Ratio: chart tap shows tooltip with date and return %
- [ ] Sharpe Ratio: skipped tickers card shows when any ticker fails to fetch
- [ ] Positions screen: 4 tabs visible (STOCK, ETF, Analysis, Dividend) in flat Row layout with icons
- [ ] Positions tabs: equal width, selected tab highlighted with primaryContainer background
- [ ] Dividend tab: total annual dividend income card shows correct sum (blue, bold)
- [ ] Dividend tab: Stock and ETF exploding pie charts render correctly (largest slice offset)
- [ ] Dividend tab: sort buttons toggle sort direction; sort options: Annual Dividend, Div/Share, Ticker, Shares
- [ ] Dividend tab: data table rows clickable → navigate to item detail
- [ ] Dividend tab: only dividend-paying tickers with quantity > 0 appear
- [ ] Dividend tab: empty state shown when no dividend-paying positions exist
- [ ] About dialog: "What's New" section visible with recent features listed
- [ ] Help screen: Dividend Tab section present under Items
- [ ] APK filename: output is investhelp_v<version>.apk (not app-release.apk)
- [ ] Dividend rate: shows on positions list and item detail when dividendRate > 0
- [ ] CSV Performance import: account name mapping dialog works correctly
- [ ] Build: `build_apk.bat` succeeds with env.bat config
- [ ] Build: works behind corporate proxy (gradle.properties proxy settings)
- [ ] Build: graceful when keystore.properties is missing (debug build still works)
- [ ] `run_once.bat`: creates keystore.properties, local.properties, and keystore
- [ ] `create_signature.bat`: generates keystore and writes correct storeFile path

## Backup/Restore v6 (Generic Table-Based)
- [ ] Export backup from Android: exported JSON has `"version":6` and a `"tables"` object
- [ ] Export backup from Android: all tables present in JSON (accounts, positions, transactions, performance, watch_lists, watch_list_items, change_history, definitions, sql_library, ai_library, csv_import_mappings, csv_named_mappings)
- [ ] Export backup from Android: BLOB columns (e.g. logo) are base64-encoded strings, not binary
- [ ] Restore backup on Android: all rows re-inserted across all tables (verify counts before vs after)
- [ ] Restore backup on Android: DB transaction atomicity — if restore fails partway, DB is not partially modified
- [ ] Export backup from PWA: exported JSON has `"version":6` and all expected tables
- [ ] Restore backup on PWA: all tables cleared and re-populated correctly
- [ ] Auto-backup on Android (onStop): file written to selected backup folder using v6 format
- [ ] Android auto-backup: oldest files pruned when count exceeds the configured limit
- [ ] Legacy restore on Android: importing a v5 backup still restores data correctly via legacy path
- [ ] Legacy restore on Android: importing a v1-v4 backup still restores data correctly via legacy path
- [ ] Legacy restore on PWA: importing a v5 backup still restores data correctly
- [ ] PWA auto-refresh auto-backup: uses shared exportAllTablesGeneric() (same output as manual export)

## Cross-Platform
- [ ] Backup export from Android, import into PWA: all tables restored
- [ ] Backup export from PWA, import into Android: all tables restored
- [ ] Dividend rate preserved in backup/restore cycle
- [ ] v6 backup exported from Android imports cleanly into PWA (and vice versa) with no data loss
