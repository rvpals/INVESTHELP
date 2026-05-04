# Changelog

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
