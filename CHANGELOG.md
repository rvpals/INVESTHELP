# Changelog

## v1.19 (Build 20) - 2026-05-14

### Fixed
- Item Detail: Yahoo Finance, Simulate, and Add to Watch List buttons now visible even when ticker doesn't exist in the database yet
- Transaction form: ticker field now editable (allows typing new tickers) with filtered dropdown suggestions; previously was dropdown-only, blocking new ticker entry

## v1.17 (Build 18) - 2026-05-14

### Added
- Item Detail Price History: "1 Minute" interval option for Hourly timeframe
- Build: new `build_apk.bat` script with JAVA_HOME and clean step (replaces `build_apk_release.bat`)

### Fixed
- Item Detail Price History: "10 Minutes" interval replaced with "15 Minutes" — Yahoo Finance does not support 10m interval (caused HTTP 400 error)
- Item Detail Price History: interval radio buttons split into two rows so all labels are fully visible (previously "5 Minutes" label was cut off)

### Changed
- Hourly intervals now: Every Hour (1h), 30 Minutes (30m), 15 Minutes (15m), 5 Minutes (5m), 1 Minute (1m)

## v1.16 (Build 17) - 2026-05-13

### Added
- Item Detail Price History: hint text below timeframe selector showing what each option means (e.g., "Last 60 days (1d interval)")
- Item List: auto-fetches missing company logos in background on screen load
- Logo fetching: tries 3 CDN sources (companiesmarketcap.com, parqet.com, iexcloud) for better coverage
- SQL Explorer: alternating row colors in result grid for improved readability

### Changed
- SQL Explorer result grid: already had gridlines; added alternating row background color (every other row highlighted)

## v1.15 (Build 16) - 2026-05-13

### Added
- Item Detail: delete button (trash icon, red) in top app bar; respects "Warn before delete" setting with confirmation dialog
- Item Detail Price History: line chart above the data table plotting all price points; supports pinch-to-zoom (1x–5x), pan when zoomed, tap-to-select with tooltip (price + date), double-tap to reset; filled area under curve; Y-axis price labels and X-axis date labels

### Changed
- Transaction table: removed `accountId` column entirely — transactions are no longer tied to accounts
- Transaction form: removed account dropdown selector
- Transaction list: removed account filter dropdown and account name display
- Account Detail screen: removed transactions section (accounts no longer have associated transactions)
- Database version bumped to 21 (migration v20→v21 recreates transactions table without accountId/FK)
- Backup format bumped to v4 (transactions no longer export accountId; v3 backups still restore correctly)
- CSV Transaction Import: no longer maps accountId field

## v1.14 (Build 15) - 2026-05-13

### Added
- Settings: collapsible panels — "Themes" and "Dashboard Market Indices" sections now collapse/expand with animated arrow toggle (default collapsed)
- Refresh status bar: temporary status bar below top bar during price refresh showing "Updating [TICKER]" with current price, change amount, and change percent (color-coded green/red); auto-hides when refresh completes
- Watch List: clicking a ticker in the table navigates to Item Detail screen (ticker text colored as link)
- Watch List: each watch list displayed as its own collapsible panel (replacing chip-selector approach); all lists visible simultaneously with expand/collapse per list
- Watch List: "Refresh All" button in header refreshes prices across all watch lists
- Item Detail: new "Price History" tab with radio button timeframe selector (Hourly, Daily, Monthly, Yearly)
- Item Detail Price History: fetches data from Yahoo Finance — Hourly shows market hours for today, Daily shows last 60 days, Monthly shows last 13 months, Yearly shows last 15 years
- Item Detail Price History: summary cards (Average, Max, Min) above the price table
- Item Detail Price History: grid table with row number, date/time, and closing price with gridlines
- StockPriceService: new `fetchPriceHistory(ticker, range, interval)` method for flexible Yahoo Finance chart queries

### Changed
- Watch List screen: replaced FilterChip selector with collapsible panels showing all watch lists at once; each panel has Add Ticker, Rename, Delete actions inline
- Item Detail screen: restructured with TabRow (Details, Price History) tabs; existing content moved to Details tab

## v1.13 (Build 14) - 2026-05-12

### Added
- Watch List notification tap: clicking a reminder notification now opens the Item Detail screen for the ticker
- Company logo stored in database: `logo` BLOB column on `investment_items` table; fetched from companiesmarketcap.com CDN during price refresh and cached locally
- Item Detail: company logo icon (48dp) in header card with ticker name and company name
- Database migration v19→v20: adds `logo` column to investment_items
- Logo fetch logging in AppLog for debugging CDN responses

### Fixed
- Company logo URLs: fixed 404 errors by lowercasing ticker in CDN URL (companiesmarketcap.com uses lowercase filenames)
- Logo display: use ByteBuffer.wrap() for Coil 2.x compatibility (raw ByteArray not supported)
- All ticker icon composables (Items list, Dashboard, Item Detail) now use stored logo bytes when available, falling back to network URL

## v1.10 (Build 11) - 2026-05-12

### Added
- 6 new themes: Navy Marine, Tropical Mint, Wine Burgundy, Desert Sand, Nordic Pine, Chase (21 total)
- Chase theme inspired by Chase brokerage app: deep navy blue primary, white surfaces, corporate look

### Changed
- Items list screen redesigned to brokerage-style layout: individual card rows, ticker + uppercase company name on left, current price with day change/percentage below, total value on right with colored gain/loss badge (green/red chip)
- Items list: removed alternating row colors in favor of thin dividers between rows

### Fixed
- Watch List: selecting today's date for reminder notification now works correctly (fixed Material 3 DatePicker UTC timezone conversion issue)
- Watch List: reminder notifications now properly fire on Android 13+ (added POST_NOTIFICATIONS runtime permission request and canScheduleExactAlarms() check)

## v1.9 (Build 10) - 2026-05-11

### Added
- Data Management: "Clear" (x) button per CSV import type (Transaction Records, Position Details, Performance Records) to erase all entries from the corresponding table with confirmation dialog showing table name
- Dashboard Portfolio Summary: last refreshed timestamp now persisted to SharedPreferences (survives app restart)

## v1.8 (Build 9) - 2026-05-11

### Fixed
- App icon: fixed distortion caused by naive resize of non-square source image; properly crops to content area, preserves aspect ratio, centers within adaptive icon safe zone using LANCZOS resampling at all density buckets

## v1.7 (Build 8) - 2026-05-11

### Added
- Change History: `change_history` table recording daily ETF/Stock/Total values; auto-records on price refresh when toggle enabled in Settings
- Dashboard Portfolio Summary: miniature line chart of total_value history; click opens full-screen chart with zoomable multi-series (Total/ETF/Stock) + grid data table
- Dashboard Portfolio Summary: "Refreshed: MMM dd, h:mm a" label showing last price refresh time
- Watch List Reminders: optional reminder per watch list item with date/time + message; scheduled via AlarmManager; notification via BroadcastReceiver; bell icon in table row (highlighted when active); set during add or edit via dedicated dialog
- 5 new themes: Lavender Fields, Copper Bronze, Emerald Gem, Slate Blue, Mocha Coffee (15 total)
- Simulation chart: thicker lines (5f stroke) with labels (1W, 3M, etc.) drawn at end of each line
- New app icon

### Changed
- Transaction form: Update/Create and Simulate buttons moved to fixed bottom bar (no longer scroll with form content); Update button placed first
- Transaction form: ticker field now dropdown-only (selects from existing items)
- Database version bumped to 19 (migrations v17→v18 for change_history, v18→v19 for watch list reminders)

### Fixed
- CSV Import: ticker field no longer includes trailing " - <shares>" content from brokerage exports (e.g., "MSFT - 50" now correctly imports as "MSFT")

## v1.6 (Build 7) - 2026-05-10

### Added
- Dashboard: "Portfolio Summary" collapsible card wrapping daily change amount and Day/All percentages; total value change displayed in 3x larger font (headlineLarge)
- Performance screen: Records list converted from cards to grid table with horizontal and vertical gridlines, header row, and alternating row colors

### Changed
- Item detail: "Analysis Info" now displayed as an inline collapsible panel (auto-fetches on screen load) instead of a button that opens a bottom sheet
- Item detail: removed "Analysis Info" button; Yahoo Finance button now full-width on its own row

## v1.5 (Build 6) - 2026-05-10

### Added
- Performance screen: "Chart Data" collapsible panel below chart showing tabular data (Account, Date, Value columns) for selected chart accounts
- Performance screen: "Recent" button in Add Record form pulls value from latest record for the selected account
- Performance screen: mini chart in Add Record section showing selected account's performance history (150dp, appears when 2+ records exist)

### Changed
- Performance screen: "Pull from App" button shortened to "Pull" to accommodate the new "Recent" button

## v1.4 (Build 5) - 2026-05-07

### Added
- Theme system: 10 selectable color themes (Default Green, Ocean Blue, Sunset Orange, Midnight Purple, Forest Moss, Ruby Red, Arctic Ice, Gold Rush, Sakura Pink, Charcoal Dark)
- Theme selection in Settings > Preferences with color preview swatch and instant apply
- Performance button moved to bottom navigation bar for quick access

### Removed
- Bank Transfer feature removed entirely (Transfer screen, entity, DAO, repository, migration drops `bank_transfers` table)
- Performance entry removed from hamburger menu (now in bottom nav)

### Changed
- Bottom nav: Dashboard, Items, **Performance**, Transaction, Simulation (replaced Transfer)
- Database version bumped to 17 (migration v16→v17 drops bank_transfers table)
- Theme no longer uses Android dynamic colors — uses app-defined theme selection instead

## v1.2 (Build 3) - 2026-05-03

### Added
- Gridlines on all result tables: added vertical dividers to Dashboard Positions legend table and Analyze Price historic prices table for consistent table styling across the app

### Changed
- All tabular grids now have both horizontal and vertical gridlines (Dashboard Position Details, Dashboard Positions Legend, Watch List, SQL Explorer, Analyze Price Historic Prices)

## v1.1 (Build 2) - 2026-05-03

### Added
- Account Performance chart: double-tap inline chart opens full-screen dialog with zoom/pan/tap-to-select
- Account Performance chart: data points with notes rendered bold (larger radius with white outline)
- Account Performance chart: tapping a noted data point shows note text in tooltip (bold second line)
- Dashboard Market Indices: user-configurable display order via Settings up/down arrows
- Dashboard Market Indices: long-press drag-and-drop reorder on dashboard cards with SharedPreferences persistence

## v1.0 (Build 1) - 2026-05-02

### Added
- Help screen: HTML-based guide loaded via WebView with dark/light theme support
- About dialog: dynamic version display from BuildConfig
- Auto-increment versioning via `version.properties`

## v0.x (Pre-release) - 2026-04 to 2026-05

### Features developed
- Dashboard with collapsible cards (Market Indices, Daily Glance, Positions, Position Details)
- Items screen with pie chart, STOCK/ETF tabs, sort dropdown, card-style rows with 3D ticker icons
- Transaction management with Analyze Price, Simulate, multi-select bulk delete
- Bank transfer tracking with per-account summary
- Simulation with historical price charts (1W-MAX), tap-to-select tooltips
- Account Performance with multi-account overlay chart, pinch-to-zoom, full-screen mode
- Watch Lists with multiple named lists, price tracking, change calculations
- CSV Import system for positions, transactions, and performance records
- SQL Explorer with raw query execution, table browser, CSV export
- Backup & Restore (JSON v1/v2/v3 format support)
- Yahoo Finance integration (v8/v10 API) for live prices, historical data, and analysis info
- 3D gradient icons for bottom nav and hamburger menu
- CollapsibleCard component with pin persistence
- Custom Canvas-drawn pie chart and line chart (no external chart library)
- Application log (AppLog) with up to 200 entries, viewable from About dialog
- Settings: warn-before-delete toggle, market indices customization, backup folder persistence
- Splash screen with custom app icon
- Room database version 16 with migrations v4-v16
