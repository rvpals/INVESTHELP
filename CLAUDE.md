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
- **Charts:** Custom Canvas-drawn (pie chart, line chart) — no external chart library
- **Images:** Coil 2.7.0 for async image loading (company logos)

## Package Structure
- `auth/` - Authentication (PasswordManager, BiometricHelper, AuthManager)
- `data/local/` - Room database, entities, DAOs, DatabaseProvider
- `data/remote/` - StockPriceService (Yahoo Finance API integration)
- `data/repository/` - Repository interfaces and implementations
- `di/` - Hilt modules (DatabaseModule, RepositoryModule, AuthModule)
- `model/` - Domain models and enums
- `ui/` - Compose screens organized by feature (auth, dashboard, account, item, transaction, transfer, simulation, sqlexplorer)

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
- Item cards show company logo (from companiesmarketcap.com CDN) with letter-avatar fallback
- Company full name fetched from Yahoo Finance `shortName` during price refresh
- Auto-create InvestmentItem when transaction references a new ticker (defaults to Stock type, changeable via type selector)
- Dates stored as epoch days for simple SQL range queries
- Yahoo Finance v8/v10 API for live prices, historical data, and analysis info
- Global top bar: portfolio value 3D button (refreshes all prices + navigates to Dashboard) + hamburger menu (Accounts, Settings, SQL Explorer, About)
- Top bar shows spinner while refreshing prices
- Bottom nav: Dashboard, Items, Transfer, Transaction, Simulation (colorful icons with shadow)
- Simulation time ranges: 1W, 2W, 1M, 3M, 6M, 1Y, 2Y, 5Y, 10Y, MAX (grouped in Week/Month/Year rows)
- Simulation chart supports tap-to-select with tooltip (price + date)
- Dashboard pie chart shows all items by ticker with shares labels inside slices
- Dashboard pie chart legend uses grid-line table with Ticker, Shares, % columns
- Transaction form: "Analyze Price" button next to Price field opens price analysis screen
- Analyze Price screen: current price, transaction avg/max/min, historic high/low (week/month/year/max)
- Clicking a price in Analyze Price copies it back to the transaction form Price field
- Transaction form: "View" button next to Ticker opens item detail; form state preserved via rememberSaveable
- Transaction form: auto-selects first account for new transactions
- Item detail: "Analysis Info" and "Yahoo Finance" buttons on same row
- Item detail: collapsible "<TICKER> Stats" section (replaces separate statistics screen)
- Item detail: collapsible "Transactions" section
- Bank Transfers screen: total amount summary grouped by account at top
- **Image loading:** Coil 2.7.0 for company logos
- Item add/edit dialog: type selector dropdown (Stock, ETF, Bond, MutualFund, Crypto, Other); auto-fills type when selecting existing ticker
- Item detail: "Daily/Share" column shows per-share daily price change
- Dashboard positions pie chart: collapsible card, legend limited to top 20 with "More" button to show all
- Settings: "Warn before delete" toggle (default: on) — when off, skips confirmation dialogs for delete actions
- Transaction form: "Simulate" button calculates days since transaction date and opens simulation with custom range
- Simulation: supports custom day ranges from transaction simulation (auto-runs on navigation)
- SQL Explorer: accessible from hamburger menu, runs raw SQL via Room's SupportSQLiteDatabase
- SQL Explorer: detects SELECT/PRAGMA/EXPLAIN queries vs DML/DDL statements
- SQL Explorer: CSV export via FileProvider + share intent
- Backup format v2: includes full merged entity fields; v1 backward compat on restore
- Transaction list: each card shows G/L = (currentPrice - pricePerShare) * numberOfShares; green for positive, red for negative
- Settings Data Management: "Import Data" section with CSV position import; column mapping dialog with 3-row preview, auto-mapping, account selector, progress bar; upserts into investment_items

## Build
Open in Android Studio and sync Gradle. Requires JDK 17+.
Set `JAVA_HOME` to JDK 17 path if building from CLI:
```
JAVA_HOME="E:/Prog/Java/jdk-17" ./gradlew assembleRelease
```
