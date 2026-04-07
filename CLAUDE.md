# Invest Help

Android investment tracking app built with Kotlin, Jetpack Compose, and Material 3.

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Min SDK:** 29, Target SDK: 35
- **Architecture:** MVVM + Repository pattern
- **DI:** Hilt (KSP)
- **Database:** Room + SQLCipher (encrypted), version 8
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
- `ui/` - Compose screens organized by feature (auth, dashboard, account, item, transaction, transfer, position, simulation)

## Key Design Decisions
- numShares on InvestmentItem synced from positions across all accounts via "Update All"
- currentValue and sharesOwned are computed from transactions, never stored
- Position table uses composite PK (ticker + accountId) - same ticker allowed on different accounts
- Account current value is computed from sum of position values (refreshes with live prices)
- Transaction table references ticker directly (not investmentItemId FK) — simpler model
- Transaction time is optional (nullable), totalAmount for verification, note field
- DatabaseProvider pattern: DB opens lazily after authentication
- CASCADE deletes: removing account removes associated positions, transactions, and bank transfers
- Bank transfers table tracks fund transfers to investment accounts (date, amount, account, note)
- Investment Items screen has STOCK/ETF tabs, toolbar buttons for Transactions and Transfers
- Dates stored as epoch days for simple SQL range queries
- Yahoo Finance v8/v10 API for live prices, historical data, and analysis info

## Build
Open in Android Studio and sync Gradle. Requires JDK 17+.
Set `JAVA_HOME` to JDK 17 path if building from CLI:
```
JAVA_HOME="E:/Prog/Java/jdk-17" ./gradlew assembleRelease
```
