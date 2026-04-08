# Invest Help - Known Issues

## Minor
- Deprecation warning: `Icons.Filled.OpenInNew` should use `Icons.AutoMirrored.Filled.OpenInNew` (ItemDetailScreen.kt)
- Deprecation warning: `statusBarColor` deprecated in Java (Theme.kt:51)

## Notes
- Build requires JAVA_HOME set to JDK 17+ (system default is JDK 8)
- Migration 4->5 drops existing positions table (fresh install or re-entry needed after upgrade)
- Migration 5->6 recreates transactions table; existing transactions map investmentItemId to ticker via items table
- Migration 8->9 merges positions into investment_items table; items without positions are dropped during migration
- Yahoo Finance API (v8/v10) is undocumented and may change without notice; no API key required
- Yahoo Finance historical data for large ranges (5Y+) uses weekly interval to reduce data volume
- Uninstalling app clears all data (encrypted DB); signature mismatch requires uninstall before reinstall
