# Invest Help - Known Issues

## Minor
- Deprecation warning: `Icons.Filled.ShowChart` should use `Icons.AutoMirrored.Filled.ShowChart` (MainActivity.kt:55)
- Deprecation warning: `statusBarColor` deprecated in Java (Theme.kt:51)

## Notes
- Build requires JAVA_HOME set to JDK 17+ (system default is JDK 8)
- Migration 4->5 drops existing positions table (fresh install or re-entry needed after upgrade)
