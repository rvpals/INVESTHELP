# Invest Help - Development Guide

## Prerequisites

- **JDK 17+** (required for Gradle build)
- **Android Studio** (latest stable recommended)
- **Android SDK** with API 35 installed

## Setup

1. Clone the repository
2. Open in Android Studio and sync Gradle
3. If building from CLI, set JAVA_HOME:
   ```bash
   JAVA_HOME="E:/Prog/Java/jdk-17" ./gradlew assembleRelease
   ```
   > **Note:** On this machine, JDK 17 is at `E:\Prog\Java\jdk-17`. The system default Java (in PATH) is Java 8 which is too old. Always build from Git Bash or set JAVA_HOME explicitly. See `build_android_apk.md` for full troubleshooting guide.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (clean)
JAVA_HOME="E:/Prog/Java/jdk-17" ./gradlew clean assembleRelease

# Release build via batch file (Windows)
build_apk.bat

# APK output location
app/build/outputs/apk/release/app-release.apk
```

## Versioning

Version is managed via `version.properties` at the project root:

```properties
VERSION_MAJOR=1
VERSION_MINOR=20
VERSION_CODE=21
```

- **Auto-increment**: Minor version and version code increment by 1 after each `assembleDebug` or `assembleRelease`
- **Manual major bump**: Edit `VERSION_MAJOR` and reset `VERSION_MINOR` to 0
- The built APK uses the pre-increment version; the increment happens after the build completes
- About dialog reads version dynamically from BuildConfig

## Database Migrations

When modifying the Room database schema:

1. Increment the database version in `AppDatabase.kt`
2. Add a migration object (e.g., `MIGRATION_16_17`)
3. Register the migration in the database builder
4. Update `CLAUDE.md` and `ISSUES.md` with migration notes

Current version: **31**

## Key Conventions

### Entity Design
- `InvestmentItem` uses `ticker` as sole primary key (one record per ticker)
- Transactions reference tickers directly (no FK, account-independent)
- CASCADE deletes on account-dependent tables (performance)

### UI Patterns
- All dashboard sections use `CollapsibleCard` with per-card pin persistence
- Tables use both horizontal and vertical gridlines (`HorizontalDivider` + `VerticalDivider`)
- Table dividers use `outline` color (not `outlineVariant`) for better visibility
- All tables use alternating row background colors (`surfaceVariant.copy(alpha = 0.3f)` on odd rows)
- Color-coded gain/loss: green (`0xFF2E7D32`) for positive, red (`0xFFC62828`) for negative
- 3D icons (`Icon3D`, `TickerIcon3D`) use gradient-filled rounded boxes with shadow
- Charts are custom Canvas-drawn (no external chart library)

### Data Patterns
- `LocalDate` stored as epoch days
- `LocalDateTime` stored as epoch seconds (UTC)
- Numeric formatting: `DecimalFormat("#,##0.##")` for shares, `NumberFormat.getCurrencyInstance` for currency
- Yahoo Finance API calls are in `StockPriceService.kt`

### Image Loading
- Coil 2.7.0 for async image loading
- Company logos cached as BLOB in `investment_positions.logo` column (fetched from multiple CDN sources: companiesmarketcap.com, parqet.com, iexcloud)
- Logos auto-fetched on items screen load for any items missing logos
- UI uses stored bytes when available; falls back to network URL
- White letter fallback when logo unavailable

## Project Dependencies

| Dependency | Purpose |
|-----------|---------|
| Jetpack Compose + Material 3 | UI framework |
| Hilt (KSP) | Dependency injection |
| Room | Local database (SQLite) |
| Compose Navigation | Screen navigation |
| Coil 2.7.0 | Image loading |
| WorkManager + hilt-work | Background periodic refresh |
| AndroidX SplashScreen | Splash screen API |
| kotlinx.coroutines | Async operations |

## Known Deprecation Warnings

- `Icons.Filled.OpenInNew` -> `Icons.AutoMirrored.Filled.OpenInNew`
- `Icons.Filled.ShowChart` / `TrendingUp` -> AutoMirrored versions
- `Icons.Filled.HelpOutline` -> `Icons.AutoMirrored.Filled.HelpOutline`
- `statusBarColor` deprecated in Theme.kt
