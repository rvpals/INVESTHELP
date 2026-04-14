# Invest Help - Known Issues

## Resolved
- Settings Preferences tab: market index toggles beyond the first 4 were not visible (fixed: made column scrollable)
- Settings: backup folder selection not persisted across app restarts (fixed: save/restore URI in SharedPreferences)

## Minor
- Deprecation warning: `Icons.Filled.OpenInNew` should use `Icons.AutoMirrored.Filled.OpenInNew` (ItemDetailScreen.kt)
- Deprecation warning: `statusBarColor` deprecated in Java (Theme.kt:51)

## Notes
- Build requires JAVA_HOME set to JDK 17+ (system default is JDK 8)
- Migration 4->5 drops existing positions table (fresh install or re-entry needed after upgrade)
- Migration 5->6 recreates transactions table; existing transactions map investmentItemId to ticker via items table
- Migration 8->9 merges positions into investment_items table; items without positions are dropped during migration
- Migration 9->10 adds dayHigh/dayLow columns to investment_items
- Migration 10->11 creates account_performance table
- Yahoo Finance API (v8/v10) is undocumented and may change without notice; no API key required
- Yahoo Finance historical data for large ranges (5Y+) uses weekly interval to reduce data volume
- Encryption removed: existing users with an SQLCipher-encrypted database must uninstall and reinstall, then restore from a JSON backup
