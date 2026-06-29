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

## Resolved
- Android backup format (v1-v4) only exported accounts, positions, and transactions — missing 7 tables added in later migrations (performance records, watch lists, change history, definitions, SQL/AI library). Fixed in v5 backup format.
- Backup export/restore was hardcoded per-table — adding a new table required code changes in both Android and PWA backup logic. Fixed in v6 with generic table-based export/restore via `sqlite_master` auto-discovery; new tables are automatically included without code changes.
- Android: Dividend tab not visible on Positions screen — tab was added to `ItemListScreen.kt` (dead code, never navigated to) instead of `PositionDetailScreen.kt` (the actual screen rendered by `PositionDetailRoute` via bottom nav). Fixed by adding Dividend tab to the correct file and deleting dead code.
- Android: Dead screens `ItemListScreen.kt` and `ItemStatisticsScreen.kt` existed in the codebase with no navigation routes wired to them. Deleted along with unused route definitions (`ItemListRoute`, `ItemStatisticsRoute`).

## Resolved
- PWA: NDA scan crash "Cannot read properties of undefined (reading 'toLocaleString')" — `scanData.avgVolume20Day` and `closingVolume` undefined from Yahoo. Fixed by defaulting to 0.
- PWA: Yahoo Analysis 500 error — v10 quoteSummary needs crumb+cookie auth. Fixed with 3-tier fallback (v10 with crumb → v10 without → v8 chart).
- PWA: Watch list tables not imported from Android backup — INSERT changed to INSERT OR REPLACE.
- PWA: Browser not refreshing new features — service worker was caching old static files. Removed SW entirely initially, then re-added with network-first strategy + force refresh button.
- PWA: Daily Glance gain/loss always green — color was based on total position value (always positive) instead of actual daily change amount. Fixed.
- PWA: Dividend rate always 0 — v8 chart meta often omits `trailingAnnualDividendRate`. Added v10 summaryDetail fallback with crumb auth.
- Android: Release build crash when keystore.properties missing — `null` cast to non-null String. Fixed by guarding signingConfigs behind `keystorePropertiesFile.exists()`.
- Android: Keystore file not found after ANDROID_APP folder move — `file()` resolves relative to `app/`, not root. Fixed by using `rootProject.file()`.
- Android: Gradle wrapper download timeout behind corporate proxy — Java ignores system proxy settings. Fixed by adding proxy config to `gradle.properties`.
- Android: install_dependency.bat "was unexpected at this time" error — paths with spaces (Program Files) broke `%variable%` expansion in if/else blocks. Fixed with `enabledelayedexpansion` and `!variable!` syntax.

## Resolved
- Android: Release build failed with "Unable to delete directory...lint-cache/lintVitalAnalyzeRelease/migrated-jars" during `clean assembleRelease` — Gradle lint-cache JARs locked by a prior daemon process. Fixed by killing all Java/Gradle processes (`Get-Process -Name "java","gradle" | Stop-Process -Force`) then building with `assembleRelease --rerun-tasks` (no `clean`). See `build_android_apk.md` Problem #4.

## Notes
- Build requires JAVA_HOME set to JDK 17+ — configure in `ANDROID_APP/env.bat`
- Migration 16->17 drops bank_transfers table (Bank Transfer feature removed); existing bank transfer data is lost on upgrade
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
- Yahoo Finance v10 endpoints require crumb+cookie auth; PWA fetches crumb on startup and retries on 401/403
- Yahoo Finance may be unreachable from some networks; PWA app has configurable proxy setting (Settings > Preferences > Yahoo Finance Proxy)
- Yahoo Finance historical data for large ranges (5Y+) uses weekly interval to reduce data volume
- Corporate networks may block Gradle wrapper downloads; configure proxy in `ANDROID_APP/gradle.properties`
- Encryption removed: existing users with an SQLCipher-encrypted database must uninstall and reinstall, then restore from a JSON backup
- Batch scripts on Windows: paths with spaces/parentheses (e.g. `C:\Program Files`) require `enabledelayedexpansion` and `!var!` syntax inside if/else blocks
