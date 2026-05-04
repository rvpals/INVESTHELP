# Invest Help

Android investment tracking app built with Kotlin, Jetpack Compose, and Material 3.

**Version:** 1.2 (Build 3)

## Overview

Invest Help is a personal investment portfolio tracker for Android. It supports multiple brokerage accounts, real-time price updates via Yahoo Finance, performance charting, watch lists, and CSV data import from common brokerages.

## Key Features

- **Dashboard** with market indices, daily gainers/losers, pie chart allocation, and position details table
- **Portfolio tracking** with real-time prices from Yahoo Finance (stocks, ETFs, bonds, crypto, mutual funds)
- **Transaction management** with buy/sell records, price analysis, and gain/loss tracking
- **Bank transfer tracking** to monitor fund flows into investment accounts
- **Simulation** with historical price charts (1W to MAX range), tap-to-select tooltips
- **Account Performance** charting with multi-account overlay, pinch-to-zoom, and full-screen mode
- **Watch Lists** for tracking potential investments with current vs. added price comparison
- **CSV Import** for positions, transactions, and performance records from brokerage exports
- **SQL Explorer** for raw database queries with CSV export
- **Backup & Restore** with JSON export/import (v1/v2/v3 format compatibility)
- **Help screen** with comprehensive HTML-based feature guide

## Screenshots

*(Coming soon)*

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository pattern |
| DI | Hilt (KSP) |
| Database | Room (SQLite), version 16 |
| Navigation | Compose Navigation (type-safe routes) |
| Charts | Custom Canvas-drawn (pie chart, line chart) |
| Images | Coil 2.7.0 |
| Min SDK | 29 (Android 10) |
| Target SDK | 35 (Android 15) |

## Build

Requires JDK 17+.

### Android Studio
Open the project and sync Gradle. Build via `Build > Make Project` or run on device/emulator.

### Command Line
```bash
JAVA_HOME="/path/to/jdk-17" ./gradlew assembleRelease
```

The release APK is output to `app/build/outputs/apk/release/app-release.apk`.

## Versioning

Version is managed via `version.properties` at the project root. Minor version and build code auto-increment after each `assembleDebug` or `assembleRelease` build.

To bump the major version, edit `VERSION_MAJOR` in `version.properties` and reset `VERSION_MINOR` to 0.

## Project Structure

```
app/src/main/java/com/investhelp/app/
├── data/
│   ├── local/          # Room database, entities, DAOs, DatabaseProvider
│   ├── remote/         # StockPriceService (Yahoo Finance API)
│   └── repository/     # Repository interfaces and implementations
├── di/                 # Hilt modules (DatabaseModule, RepositoryModule)
├── model/              # Domain models and enums
└── ui/
    ├── account/        # Account list and detail screens
    ├── components/     # Reusable components (CollapsibleCard, ConfirmDeleteDialog, DateRangePicker)
    ├── dashboard/      # Dashboard with market indices, daily glance, positions
    ├── help/           # Help screen (WebView)
    ├── item/           # Item list, detail, and form screens
    ├── navigation/     # Navigation routes and graph
    ├── performance/    # Account performance tracking and charts
    ├── settings/       # Settings, backup/restore, CSV import
    ├── simulation/     # Price simulation with historical charts
    ├── sqlexplorer/    # SQL Explorer for raw database queries
    ├── theme/          # Material 3 theme configuration
    ├── transaction/    # Transaction list, form, and price analysis
    ├── transfer/       # Bank transfer list and form
    └── watchlist/      # Watch list management
```

## License

Private project. All rights reserved.
