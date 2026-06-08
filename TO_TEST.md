# Invest Help - Testing Checklist

## PWA - Recent Changes
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
- [ ] Dividend rate: shows on positions list and item detail when dividendRate > 0
- [ ] CSV Performance import: account name mapping dialog works correctly
- [ ] Build: `build_apk.bat` succeeds with env.bat config
- [ ] Build: works behind corporate proxy (gradle.properties proxy settings)
- [ ] Build: graceful when keystore.properties is missing (debug build still works)
- [ ] `run_once.bat`: creates keystore.properties, local.properties, and keystore
- [ ] `create_signature.bat`: generates keystore and writes correct storeFile path

## Cross-Platform
- [ ] Backup export from Android, import into PWA: all tables restored
- [ ] Backup export from PWA, import into Android: all tables restored
- [ ] Dividend rate preserved in backup/restore cycle
