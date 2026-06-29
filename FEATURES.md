# InvestHelp — Feature Reference

Investment tracking app available as an Android native app and a Progressive Web App (PWA).
Both share the same SQLite database schema and v6 backup format, making data fully portable between platforms.

---

## Part 1 — Android App

### Navigation
- **Top bar** (persistent across all screens):
  - **Left:** Search icon — opens Search Ticker dialog with live autocomplete (matches ticker symbol or company name, up to 8 results, tap suggestion to navigate directly)
  - **Center:** Dashboard / Positions / Transaction nav buttons as 3D gradient icon buttons; selected tab shows full color, unselected is dimmed
  - **Right:** Refresh button (triggers full price refresh for all tickers), Watch List star, hamburger menu
- **Hamburger menu:** Accounts, Performance, Simulation, Settings, Next-Day Actions, SQL Explorer, Volatility Analysis, Correlation Matrix, Sharpe Ratio, Help, About
- **Refresh progress:** LinearProgressIndicator appears below the top bar during refresh; a status bar below that shows "Updating [TICKER]" with live price, change $, and change % per ticker (color-coded, auto-hides on completion)
- No bottom navigation bar — all primary navigation lives in the top bar

---

### Dashboard
- **Portfolio Summary card:** total portfolio value in large bold text; daily and all-time gain/loss percentages; mini line chart of historical total value from change history (2+ records required); tapping the mini chart opens a full-screen Change History dialog
- **Change History dialog:** full-screen zoomable multi-series line chart (Total / ETF / Stock lines) with pinch-to-zoom, pan, and tap-to-select; grid data table below (Date, ETF, Stock, Total columns); "Change Value This Week So Far" summary card showing week-to-date daily change totals, color-coded green/red
- **Market Indices card:** horizontal scrollable row of small cards — NASDAQ, S&P 500, Dow, Gold, Russell 2K, Silver, Oil, Bitcoin (configurable in Settings); each shows label, current price, and daily change; tap to open Yahoo Finance page; long-press to drag-and-drop reorder
- **Daily Glance card:** "Overall Daily" section showing total Stock and ETF daily change $ and %; "By Per Share" checkbox toggles between total-value and per-share sorting; top 5 daily gainers and top 5 losers with ticker, name, gain/loss $ and %; tap any row to navigate to item detail
- **Positions pie chart card:** pie chart of all holdings by total value; shares labels inside slices; legend table (Ticker, Shares, %) with grid lines; top 20 shown with "More" button to expand; tap legend row to navigate to item detail
- **Position Details card:** horizontally scrollable table with ticker 3D icon, shares, current price, total value; tap row to navigate to item detail
- All dashboard sections use collapsible cards with a **pin button** — pinned cards default expanded, unpinned default collapsed; pin state persisted to SharedPreferences

---

### Positions Screen
Four tabs in a flat equal-width row layout:

- **STOCK tab:** pie chart of stock holdings + list of stock positions
- **ETF tab:** pie chart of ETF holdings + list of ETF positions
- **Analysis tab:** side-by-side exploding pie charts for Stocks and ETFs (largest slice offset outward)
- **Dividend tab:**
  - "Total Annual Dividend Income" summary card (blue) at the top
  - Separate Stock and ETF dividend sections, each with an exploding pie chart, color-coded legend with percentage per ticker
  - Sortable data table (columns: Ticker, Shares, Div/Share, Annual $, %); sort by Annual Dividend / Div per Share / Ticker / Shares
  - Only tickers with a dividend rate > 0 and shares > 0 are shown; tap any row to navigate to item detail

**Position list rows** (brokerage-style):
- Left: 3D ticker icon (gradient box, company logo overlay via Coil) + ticker symbol (bold) + uppercase company name
- Center: current price + daily change $ and % below
- Right: total position value + daily gain/loss badge (green/red chip)
- Annual dividend income line ("Div: $X.XX/yr") shown in blue when dividendRate > 0
- Edit button per row (no delete in list — delete is inside the edit dialog)

**Toolbar:** Sort dropdown (Ticker / Total Value / Current Price, defaults to Total Value desc) + Refresh All button

---

### Item Detail
Three-tab screen (ScrollableTabRow):

**Details tab:**
- Header card: ticker info, current price, 0-share indicator if no shares held
- Card row 1 (large): Total Shares, Total Value
- Card row 2 (medium): Daily G/L, Daily G/L per Share, Daily Low, Daily High
- Card row 3 (shown when dividendRate > 0, blue): Dividend per Share, Annual Dividend
- **Analysis Info** collapsible card (pinnable): auto-fetches Yahoo Finance quoteSummary on load; shows Key Metrics, Price Range, Financials, About sections; tap any metric label to see a definition popup
- **News** collapsible card (pinnable): fetches news from Yahoo Finance; shows title, publisher, time ago; tap to open in browser; article count configurable in Settings (5 / 10 / 20)
- **ℹ button** in top bar: opens full Yahoo Finance report dialog (market data, valuation, financials, key stats, profile, events, analyst recommendations, fund holdings for ETFs)

**Price History tab:**
- Timeframe selector: Hourly / Daily / Monthly / Yearly (radio buttons with hint text)
  - Hourly: today's market hours with interval selector (1m / 5m / 15m / 30m / Every Hour)
  - Daily: last 60 days; Monthly: last 13 months; Yearly: last 15 years
- Canvas line chart with pinch-to-zoom, pan, and tap-to-select tooltip
- Summary cards: Average, Max, Min; grid data table of prices below

**Transactions tab:**
- **Transactions & Stats** collapsible panel: date range filter, buy/sell statistics, per-transaction G/L cards showing days since transaction ("123d") and gain/loss (current price vs transaction price); delete button (X) per card with confirmation dialog
- **Investing Performance** collapsible panel: fetches Yahoo Finance prices 1 day before/after each transaction; current price added as last data point; Canvas line chart with price labels on each point, pinch-to-zoom (1x–5x), pan, tap-to-select tooltip, double-tap to reset zoom; transaction dots (bold red) vs market dots (gray) vs current price dot (tertiary color); data table with highlighted transaction rows and alternating row colors
- Fullscreen button: opens Investing Performance chart in a full-screen dialog (400dp height)
- Save to PNG button: renders chart as 1200×600 bitmap saved to Pictures/InvestHelp/

---

### Transactions
**Transaction List:**
- Each card shows: date, action (Buy/Sell), ticker, shares, price per share, gain/loss (current price − transaction price × shares), days since transaction; G/L color-coded green/red
- **Multi-select mode:** long-press any card to enter selection; checkboxes on each card; contextual top bar with count, Select All, and Delete; bulk delete respects "Warn before delete" setting

**Transaction Form:**
- Fields: date, time (optional), action (Buy/Sell), ticker (auto-uppercased), shares, price per share, total amount (calculated), note
- **Analyze Price button** next to Price field: opens price analysis screen showing current price, transaction avg/max/min, and historic high/low (week/month/year/YTD/max) in a grid table; tap any price to copy it back to the form
- **View button** next to Ticker: navigates to item detail for that ticker; form state preserved on return
- **Simulate button:** calculates days from transaction date to today and opens Simulation pre-filled with ticker, shares, and that custom day range; simulation auto-runs on navigation
- New ticker: auto-creates a position stub (defaults to Stock type)
- Duplicate prevention: unique constraint on (date, action, ticker, totalAmount)

---

### Simulation
- Enter ticker symbol and number of shares
- Time range selection in grouped rows: Week (1W, 2W) / Month (1M, 3M, 6M) / Year (1Y, 2Y, 5Y, 10Y, MAX)
- Results: summary card (start price vs current price, profit/loss amount and %), Canvas line chart with filled area and start-price reference line, tap-to-select tooltip with price and date
- Large ranges (5Y+) use weekly interval; MAX uses Yahoo Finance `range=max`
- **Custom day ranges** from transaction simulation: human-readable label (e.g. "1y 3m", "2m 15d"), auto-runs on navigation
- **Scenario Simulation card:** enter shares, ticker, and a hypothetical buy date; calculates gain/loss at today's price via Yahoo Finance historical lookup

---

### Account Performance
- Accessible from hamburger menu
- Tracks total account value over time for trending analysis
- **Add Record form:** account selector, total value field, Pull button (computes current portfolio value), Recent button (fills with latest saved value for the selected account), optional note, auto-timestamps to today
- Mini line chart (150dp) below the form shows history for the selected account (2+ records required)
- **Multi-account line chart:** Canvas-drawn overlay chart; FilterChip multi-select for accounts; each account gets a distinct color; pinch-to-zoom (1x–5x), two-finger pan; tap-to-select tooltip showing account name, value, and date; x-axis labels update to reflect the visible viewport
- **Full-screen chart:** double-tap inline chart to open full-screen dialog; supports zoom/pan/tap-to-select; double-tap in full-screen resets zoom
- **Note indicators:** data points with notes rendered as bold larger circles; tapping shows a two-line tooltip (value/date + note text)
- **Chart Data panel:** tabular data (Account, Date, Value) for all displayed chart series; sorted by account then date
- **Records grid table:** horizontal/vertical gridlines, header row, alternating row colors; edit note via pencil icon dialog; delete respects "Warn before delete" setting

---

### Watch List
- Accessible from hamburger menu (star icon) or top bar star button
- Create multiple named watch lists; each displayed as its own collapsible panel
- **Add ticker:** dialog with ticker, shares count, price-when-added; Fetch button pulls current price from Yahoo Finance
- **Table view** (horizontally scrollable): Ticker, Shares, Current Price, Added Price, Change $, Change %, Added Date, bell icon (reminders), delete button
- Change $ = (currentPrice × shares) − (priceWhenAdded × shares)
- **Reminders:** optional per-item reminder with date, time, and message; scheduled via AlarmManager, fires as a notification via BroadcastReceiver; bell icon colored when active; set/edit via dedicated dialog with date picker, time picker, message field, and Clear option
- Manage lists: create, rename, delete (CASCADE deletes all items in the list)
- Tap ticker text to navigate to item detail

---

### Next Day Actions
- Accessible from hamburger menu
- Scans all positions with shares using a 5-signal tiered decision engine:
  - **STOP LOSS** — price closed below 20-day SMA (Tier A)
  - **TRIM PROFITS** — total return exceeds configured profit target % (Tier A)
  - **REBALANCE** — allocation exceeds concentration cap for type (Tier B: Stock cap / ETF cap)
  - **STRONG BUY** — closing volume ≥ 1.5× 20-day average volume (Tier C)
  - **HOLD** — all thresholds healthy
- Summary count badges per signal type at top; data table with Ticker, Shares, Price, Value, Alloc%, Return%, Action, Reasoning
- **Explanation card** (toggleable via Settings "Show Explanation"): describes each signal and shows configured thresholds
- Detail log cards: expand per ticker to see full step-by-step evaluation with raw numbers
- Thresholds configurable in Settings (NDA Thresholds tab): profit target %, stock concentration cap %, ETF concentration cap %

---

### Volatility Analysis
- Accessible from hamburger menu
- Calculates 52-week annualized volatility and daily standard deviation for all positions using Yahoo Finance 1-year daily price history
- Positions grouped into 4 volatility bands: **Low** (<15%) / **Moderate** (15–30%) / **High** (30–50%) / **Very High** (>50%), each color-coded
- Progress bar shown per ticker during calculation
- Results cached to SQLite (`volatility_cache` table); "Last calculated on" banner shown on subsequent opens; Refresh button clears cache and recalculates
- Per-ticker detail: annualized vol %, daily std dev %, 52-week low/high, position within 52-week range (range bar)
- **Explanation card** (toggleable via Settings "Show Explanation"): describes what the % means, what each band means, and how to interpret the range bar

---

### Correlation Matrix
- Accessible from hamburger menu
- Computes pairwise Pearson correlation coefficients using 1-year daily returns for all stock/ETF positions
- Full N×N matrix display with color-coded cells (green = high positive, red = high negative, gray = low)
- Market sensitivity section: correlation of each ticker vs SPY
- Portfolio insights: identifies strongly correlated pairs (≥0.75 threshold), filter toggle to highlight only high-correlation cells
- Results cached to SQLite (`correlation_cache` singleton); "Last calculated on" banner; Refresh clears cache
- PNG download button: saves matrix as image
- **Explanation card** (toggleable via Settings "Show Explanation"): what correlation means, how to read the matrix, what values indicate

---

### Sharpe Ratio
- Accessible from hamburger menu
- Calculates portfolio Sharpe Ratio (risk-adjusted return) using combined daily portfolio returns
- Configurable: risk-free rate (default 5%), lookback period (6M / 1Y / 2Y / 5Y / 10Y chips)
- Results: Sharpe Ratio, annualized return, annualized volatility, aligned trading days, mean daily return, daily risk-free rate used
- Canvas daily returns chart with green/red area fills
- Per-ticker breakdown table showing contribution details
- Results cached to SQLite (`sharpe_ratio_cache` singleton); "Cached at" banner on instant load; Refresh (↻) button recomputes
- **About Sharpe Ratio card** (toggleable via Settings "Show Explanation"): formula, components, interpretation table (poor/acceptable/good/excellent ranges)
- **Calculation Detail card** (toggleable via Settings "Show Explanation"): inputs used, per-ticker data points, step-by-step calculation

---

### SQL Explorer
- Accessible from hamburger menu
- SQL text editor + Run button (navigates to SQL Result screen) + Save to Library button
- **Table browser:** lists all app database tables with expandable column details (name, type, PK/NN indicators); tap table or column name to insert into the SQL editor; Open button runs `SELECT * FROM <table>` directly
- **SQL Library card:** saved queries with name, description, category filter, name search, Run and Delete per entry
- **SQL Result screen:** full-screen with editable SQL query card, result grid (vertical + horizontal scroll, clickable cells for full-screen untruncated detail), Export to CSV button; auto-executes on load

---

### Settings

**Preferences tab:**
- **Theme:** 10 color themes (Default Green, Ocean Blue, Sunset Orange, Midnight Purple, Forest Moss, Ruby Red, Arctic Ice, Gold Rush, Sakura Pink, Charcoal Dark); instant apply, persisted to SharedPreferences
- **Warn before delete** (default: on) — when off, skips confirmation dialogs for all delete actions app-wide
- **Show Explanation** (default: on) — controls visibility of educational explanation cards on Sharpe Ratio, Correlation Matrix, Volatility Analysis, and Next Day Actions screens
- **Max news articles per ticker** dropdown (5 / 10 / 20; default: 5)
- **Auto Update Change History when refresh** (default: off) — when on, automatically records ETF/Stock/Total daily values to change_history after every price refresh
- **Auto Refresh All** (default: off) — background periodic price refresh via WorkManager; interval options: 5 min / 30 min / 1 hr / 5 hr / Market close daily; foreground notification shown during refresh; completion notification shows ticker count
- **Dashboard Market Indices:** toggles for 8 indices; up/down arrow reorder (syncs with long-press drag-and-drop on Dashboard)

**Data Management tab:**
- **Backup folder selection** (persisted across restarts)
- **Export Data:** exports all tables as v6 generic JSON (auto-discovers tables via `sqlite_master`; BLOB columns base64-encoded)
- **Restore Data:** imports v6 JSON (topological FK-safe delete/insert order, full transaction); backward-compatible with v1–v5 formats
- **Automatic backup when quitting** (default: off): exports on app backgrounding with 30-minute cooldown guard; "Last Auto Backup completed on" timestamp shown; configurable max backup count (default: 10, oldest files pruned)
- **CSV Import** — 3 import types:
  - *Transaction Records:* maps CSV columns to transaction fields; does not auto-update share counts; creates position stub for new tickers
  - *Position Details:* maps CSV columns to position fields; confirmation dialog before overwrite; auto-strips commas from brokerage-formatted numbers (e.g. "92,150.62")
  - *Performance Records:* account name mapping dialog to resolve CSV account names to app accounts
  - Each import type has "Define Mapping" (full-screen editor with Save / Save As / Load) and "Start Import" (select saved mapping, imports with per-row NEW/UPDATED/SKIPPED log)
  - Column mapping dialog shows 3-row preview with auto-mapping using common brokerage aliases

**NDA Thresholds tab:**
- Profit Target % (used by Next Day Actions TRIM PROFITS signal)
- Stock Concentration Cap % and ETF Concentration Cap % (used by REBALANCE signal)

---

### Help
- Accessible from hamburger menu
- Full HTML help guide loaded via WebView from bundled `assets/help.html`
- Covers all features: navigation overview grid, per-section guides, and tips
- Styled with dark/light theme support via CSS `prefers-color-scheme`

---

### About Dialog
- Version number (dynamic from BuildConfig: versionName + versionCode)
- "What's New" section with recent feature changelog
- **Show Log button:** opens scrollable in-memory application log (up to 200 entries, newest first, with timestamps); clear button to wipe entries; logs include price fetch results, refresh summaries, and per-ticker errors

---

## Part 2 — PWA App

### Overview
- Node.js + Express server + better-sqlite3 database
- Vanilla JS / HTML5 / CSS3 frontend — no framework, no build step
- Hash-based SPA router (`#/route` format)
- Same SQLite schema and v6 backup format as Android — data fully portable between platforms
- Run: `START_APP.bat` (Windows) or `npm start` from the `PWA/` folder → http://localhost:3000
- Dark/light theme: auto-detected from system preference via CSS `prefers-color-scheme`

---

### Navigation
- **Top bar:** Portfolio value button (tap to refresh all prices + navigate to Dashboard), Search Ticker button, Watch List star, hamburger menu
- **Search Ticker dialog:** type partial ticker or company name; async autocomplete dropdown (up to 10 results) drawn from live positions list; tap suggestion to navigate directly
- **Hamburger menu:** Accounts, Performance, Simulation, Next Day Actions, Volatility Analysis, Correlation Matrix, Sharpe Ratio, Settings, SQL Explorer, Help, About
- **Refresh status bar:** shows live "Refreshing prices…" feedback; success/failure count on completion; auto-hides after 3–4 seconds

---

### Dashboard
- **Portfolio Summary card:** total portfolio value with daily change $ and %; mini line chart of historical total value; tap mini chart to open Change History dialog
- **Change History dialog:** multi-series Canvas line chart (Total / ETF / Stock lines) with zoom/pan/tap-to-select; grid data table; "Change Value This Week So Far" summary
- **Market Indices card:** horizontal scrollable row of index cards (NASDAQ, S&P 500, Dow, Gold + more); configurable in Settings; tap to open Yahoo Finance page
- **Daily Glance card:** overall Stock/ETF daily change totals; top 5 gainers and top 5 losers; tap to navigate to item detail
- **Positions pie chart card:** Canvas pie chart by total value; legend with Ticker, Shares, %; tap legend row to navigate to item detail
- **Position Details card:** scrollable table with ticker icon, shares, current price, total value; tap row to navigate
- All sections use collapsible cards with pin persistence (localStorage)

---

### Positions Screen
Four tabs:

- **STOCK tab:** pie chart + stock position list
- **ETF tab:** pie chart + ETF position list
- **Analysis tab:** exploding pie charts for Stocks and ETFs
- **Dividend tab:** total annual income summary; Stock and ETF sections with exploding pie charts, sortable dividend tables (Annual / Div per Share / Ticker / Shares), tappable rows

**Position list rows:** ticker icon, symbol (bold), company name, current price, daily change, total value, daily G/L badge, annual dividend income line (when dividendRate > 0)

Sort dropdown: Ticker / Total Value / Current Price

---

### Item Detail
Three tabs:

**Details tab:**
- Header: ticker, type, current price, day high/low, shares summary
- Metric cards: Total Value, Daily G/L, Dividend info (when applicable)
- Analysis Info card: Yahoo Finance quoteSummary data (Key Metrics, Price Range, Financials, About); metric definition popups
- News card: Yahoo Finance news articles (title, publisher, time ago, tap to open in browser); article count configurable
- Full Yahoo Finance report button

**Price History tab:**
- Timeframe: Hourly / Daily / Monthly / Yearly
- Hourly interval selector (1m / 5m / 15m / 30m / 1hr)
- Canvas line chart with zoom, pan, tap-to-select; Average / Max / Min summary cards; data table

**Transactions tab:**
- Transactions & Stats panel: date range filter, buy/sell statistics, per-transaction G/L cards with days elapsed
- Investing Performance chart: Canvas line chart overlaying transaction prices with market prices; transaction dots, market dots, current price dot; zoom/pan/tap-to-select; data table with highlighted transaction rows

---

### Transactions
**Transaction List:**
- Cards with date, action, ticker, shares, price, G/L (current vs transaction price × shares)
- Multi-select via long-press: checkboxes, Select All, bulk Delete

**Transaction Form:**
- Fields: date, time (optional), action, ticker, shares, price, total amount, note
- Analyze Price button: historic high/low grid with price copy-back
- View button: navigate to item detail
- Simulate button: opens Simulation with transaction date range

---

### Simulation
- Enter ticker and shares; time range chips: 1W / 2W / 1M / 3M / 6M / 1Y / 2Y / 5Y / 10Y / MAX
- Results: summary (start vs current price, profit/loss), Canvas line chart with area fill and reference line, tap-to-select tooltip
- Custom day ranges from transaction simulation: auto-runs with label
- Scenario Simulation card: hypothetical buy-date gain/loss calculator

---

### Account Performance
- Multi-account Canvas line chart with pinch-to-zoom, pan, tap-to-select tooltip; account FilterChips
- Full-screen chart on double-tap; zoom resets on double-tap in full-screen
- Note indicators on data points; two-line tooltip when tapped (value/date + note)
- Add Record form: account selector, value field, Pull and Recent buttons, optional note
- Mini chart in Add Record section (2+ records required)
- Chart Data table; Records grid with edit note (pencil icon) and delete

---

### Watch List
- Multiple named watch lists with collapsible panels
- Add ticker: shares, price-when-added, Fetch button for current price
- Table: Ticker, Shares, Current Price, Added Price, Change $, Change %, Added Date, bell (reminders), delete
- Reminders: date/time/message; browser Notification API (permission requested on first use)
- Manage: create, rename, delete lists

---

### Next Day Actions
- Same 5-signal scanner as Android (STOP LOSS / TRIM PROFITS / REBALANCE / STRONG BUY / HOLD)
- Live progress bar per ticker during scan
- Summary count badges; results table; expandable detail log per ticker
- **Explanation card** (toggleable via Settings "Show Explanation")
- Thresholds configurable in Settings (NDA Thresholds tab)

---

### Volatility Analysis
- 52-week annualized volatility for all positions; grouped into 4 color-coded bands
- Live progress bar during calculation; results cached to SQLite; "Last calculated on" banner
- Per-ticker: annualized vol %, daily std dev %, 52-week range bar
- **Explanation card** (toggleable via Settings "Show Explanation")

---

### Correlation Matrix
- Pairwise Pearson correlation matrix; color-coded cells; market sensitivity vs SPY
- Filter toggle for high-correlation pairs (≥0.75); portfolio insights
- Cached to SQLite; PNG download
- **Explanation card** (toggleable via Settings "Show Explanation")

---

### Sharpe Ratio
- Portfolio Sharpe Ratio with configurable risk-free rate and lookback period (6M / 1Y / 2Y / 5Y / 10Y)
- Canvas daily returns chart; per-ticker breakdown table
- Cached to SQLite; instant load with "Cached at" banner; Refresh (↻) recomputes
- **About Sharpe Ratio card** and **Calculation Detail card** (both toggleable via Settings "Show Explanation")

---

### SQL Explorer
- SQL editor + Run button + Save to Library
- Table browser with column details; Open button per table
- SQL Library with category filter, name search, Run/Delete per entry
- SQL Result screen: editable SQL, scrollable result grid, Export to CSV

---

### Settings

**Preferences tab:**
- **Warn before delete** (default: on)
- **Show Explanation** (default: on) — controls explanation cards on Sharpe Ratio, Correlation, Volatility, NDA screens
- **Max news articles per ticker** (5 / 10 / 20; default: 5)
- **Auto Update Change History** (default: off)
- **Auto Refresh interval:** configurable cron-based background price refresh; completion logged
- **Dashboard Market Indices:** toggles for 8 indices with reorder support
- **Yahoo Finance Proxy:** configurable proxy URL for restricted networks

**Data Management tab:**
- **Export / Import backup** (v6 generic JSON; backward-compatible v1–v5 restore)
- **Auto backup** (default: off): exports on auto-refresh completion; configurable max backup count
- **CSV Import** — Transaction, Position, and Performance record imports with the same mapping system as Android (Define Mapping / Start Import, brokerage alias auto-mapping, per-row import log)

**Dashboard Cards tab:** toggle visibility of individual dashboard cards

**NDA Thresholds tab:** profit target %, stock cap %, ETF cap %

**Server Log tab:** scrollable in-memory server log (500 entries); timestamps; clear button

---

### Offline Snapshot
- Static `snapshot.html` generated automatically after every Refresh All
- Offline-viewable portfolio summary (no server required to view)
- Saved in the PWA server directory and served as a static file

---

### Service Worker & PWA Shell
- Network-first caching strategy for JS/CSS/HTML; cache-first for static assets
- Installable as a PWA (add to home screen / desktop)
- **Refresh App button** (About dialog): clears all caches, posts `force-refresh` message to service worker, hard-reloads to pick up latest code

---

### About
- App version string
- "What's New" changelog with per-version bullet points
- **Show Log / Server Log button:** in-memory server log viewer (500 entries, newest first); clear button
- Refresh App button (force cache bust)
