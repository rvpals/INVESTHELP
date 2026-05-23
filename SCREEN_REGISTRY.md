# Screen & Component Registry

Use this to reference any screen or component by ID. Example: "In Screen #01, Component C3, I want to change the sort order."

---

## Global Elements (always visible)

### G1: Top App Bar
- Portfolio value 3D button (refreshes prices + navigates to Dashboard)
- Daily change amount in parentheses (color-coded)
- Day/All percentage row
- Spinner during refresh
- Hamburger menu (Accounts, Performance, Watch List, Settings, SQL Explorer, Help, About)

### G2: Refresh Status Bar
- Below top bar, shows "Updating [TICKER]" with price, change $, change %
- Color-coded, auto-hides on completion

### G3: Bottom Navigation Bar
- Tab 1: Dashboard (blue)
- Tab 2: Positions (green)
- Tab 3: Performance (dark green)
- Tab 4: Transaction (purple)
- Tab 5: Simulation (red)
- Icon3D gradient-filled rounded boxes with shadow

---

## Screen #01: Dashboard

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Portfolio Summary Card | Collapsible; total value (headlineLarge bold centered), day/all percentages, mini line chart of change_history, click chart opens Change History dialog |
| C2 | Market Indices Row | Horizontally scrollable small cards (NASDAQ, S&P 500, Dow, Gold, etc.); shows price + daily change; long-press drag to reorder; click opens Yahoo Finance |
| C3 | Daily Glance Card | Collapsible; "Overall Daily" section (Stock/ETF totals), per-share toggle, top 5 gainers, top 5 losers; clickable rows to item detail |
| C4 | Watch List Card | Collapsible; watch list sections with table (Ticker, Shares, Added Price); "View All Watch Lists" button |
| C5 | Positions Pie Chart Card | Collapsible; pie chart of all items by ticker; legend grid (top 20 + "More" button); clickable rows to item detail |
| C6 | Position Details Card | Collapsible; horizontally scrollable table (icon, shares, price, cost, value, change $, change %); clickable rows to item detail |
| C7 | Change History Dialog | Full-screen; zoomable multi-series chart (Total/ETF/Stock lines); "Change Value This Week" summary; data table (Date, ETF, Stock, Total, daily changes) |

---

## Screen #02: Account List

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | Title "Accounts", back navigation |
| C2 | Add Account FAB | Floating action button |
| C3 | Empty State | Shown when no accounts exist |
| C4 | Account Cards | Collapsible cards with: name, description, current value, last value, performance mini chart, last 10 records table, "Performance Detail" button, edit/delete buttons |
| C5 | Confirm Delete Dialog | Delete confirmation |

---

## Screen #03: Account Detail

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | Account name title, back nav, edit icon |
| C2 | Account Info Card | Description, initial value, last value |
| C3 | Performance Chart | Interactive zoomable/pannable line chart with grid lines, data points, tap tooltips |
| C4 | Performance Records Table | Header (Date, Value, Note), alternating row colors, vertical dividers |

---

## Screen #04: Account Form

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | "New Account" or "Edit Account", back nav |
| C2 | Name Field | Account name text input |
| C3 | Description Field | Multiline text input |
| C4 | Initial Value Field | Decimal keyboard |
| C5 | Save Button | Create/Update |

---

## Screen #05: Items List

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Sort Dropdown | Sort by: Ticker, Total Value, Current Price (above list) |
| C2 | Type Tabs | STOCK / ETF filter tabs |
| C3 | Add Item FAB | Floating action button |
| C4 | Item Rows | Brokerage-style cards: TickerIcon3D + ticker (bold) + company name; current price with day change $ and %; total value with colored gain/loss badge; Edit button per row |
| C5 | Refresh All | Toolbar action to refresh all prices |

---

## Screen #06: Item Detail

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | Ticker title, back nav, edit/watchlist/delete/refresh icons |
| C2 | Header Card | TickerIcon3D, ticker, company name, type badge |
| C3 | Stats Row 1 (big font) | Total Shares, Total Value, Total Cost, Total G/L |
| C4 | Stats Row 2 (medium font) | Daily G/L, Daily G/L/Share, Daily Min Price, Daily Max Price |
| C5 | Tab Bar | ScrollableTabRow: Details, Price History, Analysis Info, Transactions |
| C6 | Details Tab | Header card only (ticker info, prices, daily stats) |
| C7 | Price History Tab | Radio timeframe selector (Hourly/Daily/Monthly/Yearly); interval selector for Hourly; line chart with pinch-to-zoom/pan/tap-to-select; summary cards (Avg/Max/Min); grid table of prices |
| C8 | Analysis Info Tab | Auto-fetches Yahoo Finance quoteSummary; Key Metrics, Price Range, Financials, About sections; clickable labels show definition popup |
| C9 | Transactions Tab - Stats Panel | "Transactions & Stats" collapsible (default expanded); date range filter, buy/sell statistics, per-transaction G/L cards |
| C10 | Transactions Tab - Performance Panel | "Investing Performance for <TICKER>" collapsible (default expanded); line chart with transaction dots (bold red) + market dots (gray) + current price dot (tertiary); pinch-to-zoom, tap-to-select; data table with highlighted transaction rows |

---

## Screen #07: Item Form (Add/Edit Dialog)

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Ticker Field | Text input, auto-fills type for existing ticker |
| C2 | Name Field | Company name |
| C3 | Type Selector | Dropdown: Stock, ETF, Bond, MutualFund, Crypto, Other |
| C4 | Current Price Field | Decimal input |
| C5 | Quantity Field | Number of shares |
| C6 | Cost Field | Total cost basis |
| C7 | Save Button | Create/Update |

---

## Screen #08: Item Statistics

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | "[Ticker] Statistics", back nav |
| C2 | Date Range Selector | From/To filter chips with date pickers |
| C3 | Buy Statistics | Cards: Average, Min, Max, Count |
| C4 | Sell Statistics | Cards: Average, Min, Max, Count |

---

## Screen #09: Transaction List

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | "Transactions"; in multi-select mode: count, Select All, Delete, Close |
| C2 | Add Transaction FAB | Floating action button |
| C3 | Transaction Cards | Ticker, action (Buy/Sell), shares, price, date/time, total value, G/L color-coded, days since ("123d"); long-press for multi-select; click to edit |
| C4 | Confirm Delete Dialog | Single and bulk delete confirmation |

---

## Screen #10: Transaction Form

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | "New Transaction" or "Edit Transaction", back nav |
| C2 | Date Picker | FilterChip, opens date picker dialog |
| C3 | Time Picker | Optional, opens time picker dialog |
| C4 | Action Dropdown | Buy / Sell |
| C5 | Ticker Field | With autocomplete suggestions |
| C6 | Shares Field | Number of shares |
| C7 | Price Field | Price per share |
| C8 | Total Amount Field | For verification |
| C9 | Note Field | Optional text |
| C10 | Analyze Price Button | Opens Analyze Price screen |
| C11 | View Item Button | Opens item detail (preserves form state) |
| C12 | Simulate Button | Calculates days since date, opens simulation with custom range |
| C13 | Save Button | Create/Update |

---

## Screen #11: Analyze Price

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | "Analyzing Stock Price [ticker]", back nav |
| C2 | Current Price Display | Large price text |
| C3 | Transaction Stats | Average, Max, Min from user's transactions |
| C4 | Historic Prices Table | Grid table: timeframe (Week/Month/Year/Max), High, Low; horizontal + vertical dividers; clickable prices copy back to transaction form |
| C5 | Loading Indicator | While fetching |

---

## Screen #12: Simulation

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | "Simulation", back nav, fullscreen chart button |
| C2 | Ticker Input | Text field |
| C3 | Shares Input | Number field |
| C4 | Time Range Chips | Grouped: Week (1W, 2W), Month (1M, 3M, 6M), Year (1Y, 2Y, 5Y, 10Y, MAX); supports custom day range |
| C5 | Interactive Chart | Line chart, color-coded, zoomable/pannable, tap-to-select with tooltip (price + date) |
| C6 | Statistics Card | Current price, High/Low, Change % |
| C7 | Fullscreen Chart Dialog | Expanded chart view |

---

## Screen #13: Account Performance

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | "Account Performance" |
| C2 | Add Record Section | Account selector, total value field + "Pull" button + "Recent" button, optional note, save button; mini chart (150dp) when 2+ records |
| C3 | Account Filter Chips | Multi-select chips for chart overlay |
| C4 | Performance Chart | Multi-account line chart, distinct colors per account; pinch-to-zoom (1x-5x), two-finger pan, tap-to-select tooltip; bold points for noted records; double-tap opens fullscreen |
| C5 | Chart Data Panel | Collapsible; tabular data (Account, Date, Value) for chart-selected accounts |
| C6 | Records Grid Table | Header (Account, Date, Value, actions); alternating rows; edit note pencil icon; delete icon |
| C7 | Edit Note Dialog | Pencil icon opens note editor |
| C8 | Fullscreen Chart Dialog | Full-screen with zoom/pan/tap-to-select; double-tap resets zoom |

---

## Screen #14: Watch List

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | "Watch Lists", refresh button |
| C2 | Watch List Panels | Each watch list as collapsible panel; add/rename/delete per list |
| C3 | Add Ticker Section | Ticker input, shares input, price field + "Fetch" button, add button |
| C4 | Items Table | Columns: Ticker, Shares, Current Price, Added Price, Change $, Change %, Added Date, bell icon (reminder), delete button; ticker text clickable to item detail |
| C5 | Reminder Dialog | Date picker, time picker, message field, "Clear" option |
| C6 | Create/Rename Dialog | Name input for watch list |

---

## Screen #15: Settings

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Top Bar | "Settings" |
| C2 | Tab Row | Preferences, Data Management (tabs) |
| C3 | Preferences - Warn Before Delete | Toggle (default: on) |
| C4 | Preferences - Auto Update Change History | Toggle (default: off); records ETF/Stock/Total to change_history after refresh |
| C5 | Preferences - Auto Refresh All | Toggle (default: off); interval dropdown (5 min, 30 min, 1 hr, 5 hr, Market close daily) |
| C6 | Preferences - Themes | Collapsible panel; theme selection |
| C7 | Preferences - Dashboard Market Indices | Collapsible panel; toggles for 8 indices; up/down arrow reorder; order persisted |
| C8 | Data Management - Backup/Restore | Backup folder selector, backup button, restore button |
| C9 | Data Management - Import Section | 3 import types (Transaction, Position, Performance); each with "Define Mapping" and "Start Import"; shared account selector |
| C10 | CSV Mapping Editor | Full-screen dialog; column mapping with 3-row preview; auto-mapping with aliases; Save, Save As, Load buttons |

---

## Screen #16: SQL Explorer

| ID | Component | Description |
|----|-----------|-------------|
| C1 | SQL Query Input | Multiline monospace text field |
| C2 | Action Buttons | Run (play icon), Share/Export CSV |
| C3 | Table Browser | Expandable list of all tables; column details (name, type, PK/NN); "Open" button per table |
| C4 | Results Grid | Horizontally scrollable; header row; data rows with alternating colors; vertical + horizontal gridlines; clickable rows open detail dialog |
| C5 | Record Detail Dialog | All field values untruncated |
| C6 | Error Display | Error message area |

---

## Screen #17: Positions (Bottom Nav Tab)

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Type Tabs | Stocks (with count), ETF (with count) |
| C2 | Pie Chart | Positions by value |
| C3 | Position Table | Sortable columns: Ticker, Shares, Price, Cost, Value, Change $, Change %, Day G/L; alternating rows; clickable to item detail |

---

## Screen #18: Help

| ID | Component | Description |
|----|-----------|-------------|
| C1 | WebView | Loads assets/help.html; dark/light theme support; navigation grid, per-section guides, tips |

---

## Screen #19: About Dialog

| ID | Component | Description |
|----|-----------|-------------|
| C1 | Version Display | Dynamic from BuildConfig (versionName + versionCode) |
| C2 | Show Log Button | Opens scrollable log viewer (newest first) with clear button |

---

## Reusable Components

| ID | Component | File | Description |
|----|-----------|------|-------------|
| RC1 | CollapsibleCard | ui/components/CollapsibleCard.kt | Expandable card with pin button (SharedPreferences), title, divider, AnimatedVisibility |
| RC2 | ConfirmDeleteDialog | ui/components/ConfirmDeleteDialog.kt | AlertDialog with customizable title/message, confirm (error color) + cancel |
| RC3 | DateRangePicker | ui/components/DateRangePicker.kt | Two filter chips (from/to), Material 3 date pickers |
| RC4 | TickerIcon3D | (inline) | Gradient-filled rounded box (10dp) with shadow; color from ticker hash; white letter fallback; company logo overlay via Coil |
| RC5 | Icon3D | (inline) | Icon inside gradient-filled rounded box with drop shadow; used in bottom nav and hamburger menu |

---

## How to Use This Registry

**Format:** `Screen #XX, CY` — e.g., "Screen #06, C7" = Item Detail Price History Tab

**Examples:**
- "In Screen #01, C2, add a new market index card" = Dashboard > Market Indices Row
- "Screen #09, C3 — change the card layout" = Transaction List > Transaction Cards
- "Screen #15, C7 — add a new index toggle" = Settings > Dashboard Market Indices section
- "Screen #06, C10 — fix the chart zoom" = Item Detail > Transactions Tab Performance Panel
