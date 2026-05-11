# Changelog

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
