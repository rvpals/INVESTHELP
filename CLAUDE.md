# Invest Help

Android investment tracking app built with Kotlin, Jetpack Compose, and Material 3.

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Min SDK:** 29, Target SDK: 35
- **Architecture:** MVVM + Repository pattern
- **DI:** Hilt (KSP)
- **Database:** Room + SQLCipher (encrypted), version 9
- **Auth:** Biometric + EncryptedSharedPreferences
- **Navigation:** Compose Navigation (type-safe routes)
- **Splash:** AndroidX SplashScreen API (core-splashscreen 1.0.1)
- **Charts:** Custom Canvas-drawn (pie chart, line chart) — no external library

## Package Structure
- `auth/` - Authentication (PasswordManager, BiometricHelper, AuthManager)
- `data/local/` - Room database, entities, DAOs, DatabaseProvider
- `data/remote/` - StockPriceService (Yahoo Finance API integration)
- `data/repository/` - Repository interfaces and implementations
- `di/` - Hilt modules (DatabaseModule, RepositoryModule, AuthModule)
- `model/` - Domain models and enums
- `ui/` - Compose screens organized by feature (auth, dashboard, account, item, transaction, transfer, simulation)

## Key Design Decisions
- Merged InvestmentItem + Position into single `investment_items` table with composite PK (ticker + accountId)
- Metadata (name, type, currentPrice) denormalized per-row; `updatePriceByTicker` DAO query updates all rows atomically
- Total shares computed on the fly via `SUM(quantity) WHERE ticker = ?` (no numShares syncing)
- Account current value computed from sum of item values (refreshes with live prices)
- Transaction table references ticker directly (not FK) — simpler model
- Transaction time is optional (nullable), totalAmount for verification, note field
- Navigation routes use ticker strings (not Long IDs) for item detail, form, and statistics
- DatabaseProvider pattern: DB opens lazily after authentication
- CASCADE deletes: removing account removes associated items, transactions, and bank transfers
- Bank transfers table tracks fund transfers to investment accounts (date, amount, account, note)
- Items screen combines pie chart + STOCK/ETF tabs with Refresh All toolbar action
- Auto-create InvestmentItem when transaction references a new ticker (defaults to Stock type)
- Dates stored as epoch days for simple SQL range queries
- Yahoo Finance v8/v10 API for live prices, historical data, and analysis info
- Global top bar: portfolio value 3D button (navigates to Dashboard) + hamburger menu (Accounts, Settings, About)
- Bottom nav: Dashboard, Items, Transfer, Transaction, Simulation (colorful icons with shadow)
- Simulation time ranges: 1W, 2W, 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, MAX (grouped in Week/Month/Year rows)
- Simulation chart supports tap-to-select with tooltip (price + date)
- Dashboard pie chart shows all items by ticker with shares labels inside slices
- Backup format v2: includes full merged entity fields; v1 backward compat on restore

## Build
Open in Android Studio and sync Gradle. Requires JDK 17+.
Set `JAVA_HOME` to JDK 17 path if building from CLI:
```
JAVA_HOME="E:/Prog/Java/jdk-17" ./gradlew assembleRelease
```
