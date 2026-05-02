# Invest Help - Known Issues

## Resolved
- Settings Preferences tab: market index toggles beyond the first 4 were not visible (fixed: made column scrollable)
- Settings: backup folder selection not persisted across app restarts (fixed: save/restore URI in SharedPreferences)
- Dashboard accounts section cluttered the view (removed: accounts accessible via hamburger menu instead)

## Resolved
- CSV position import failed on brokerage CSVs with comma-formatted numbers (e.g. "92,150.62") — fixed: `parseNumeric()` strips commas before parsing
- CSV position import included FOOTNOTES rows from brokerage exports — fixed: rows with too few columns now filtered out

## Minor
- Deprecation warning: `Icons.Filled.OpenInNew` should use `Icons.AutoMirrored.Filled.OpenInNew` (ItemDetailScreen.kt)
- Deprecation warning: `Icons.Filled.ShowChart` / `Icons.Filled.TrendingUp` should use AutoMirrored versions (DashboardScreen.kt)
- Deprecation warning: `Icons.Filled.HelpOutline` should use `Icons.AutoMirrored.Filled.HelpOutline` (MainActivity.kt)
- Deprecation warning: `statusBarColor` deprecated in Java (Theme.kt:51)

## Notes
- Build requires JAVA_HOME set to JDK 17+ (system default is JDK 8)
- Migration 4->5 drops existing positions table (fresh install or re-entry needed after upgrade)
- Migration 5->6 recreates transactions table; existing transactions map investmentItemId to ticker via items table
- Migration 8->9 merges positions into investment_items table; items without positions are dropped during migration
- Migration 9->10 adds dayHigh/dayLow columns to investment_items
- Migration 10->11 creates account_performance table
- Migration 11->12 adds note column to account_performance table
- Migration 12->13 creates watch_lists and watch_list_items tables
- Migration 13->14 creates csv_import_mappings table for persistent CSV column mappings
- Migration 14->15 removes accountId from investment_items, makes ticker sole PK; merges duplicate tickers by summing quantity/cost/value and taking MAX of dayHigh/dayLow
- Migration 15->16 recreates account_performance table: converts dateTime (epoch seconds) to date (epoch days), adds unique index on (accountId, date)
- Yahoo Finance API (v8/v10) is undocumented and may change without notice; no API key required
- Yahoo Finance historical data for large ranges (5Y+) uses weekly interval to reduce data volume
- Encryption removed: existing users with an SQLCipher-encrypted database must uninstall and reinstall, then restore from a JSON backup
