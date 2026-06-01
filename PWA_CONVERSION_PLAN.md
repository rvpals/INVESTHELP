# InvestHelp — Android to PWA Conversion Plan

## Philosophy

Simplest setup, fewest dependencies. A thin Node.js server with SQLite handles all data and Yahoo Finance calls (no CORS issues). The frontend is vanilla JS — no framework, no build step. The server is the single source of truth; the browser only renders and calls the API.

---

## Tech Stack (Minimal)

| Layer | Android | PWA Replacement |
|-------|---------|-----------------|
| **Backend** | — | Node.js + Express (or built-in `http`) |
| **Database** | Room (SQLite v29, 12 tables) | `better-sqlite3` (same SQLite, server-side) |
| **Yahoo Finance** | OkHttp direct | Server-side `fetch()` — no CORS problem |
| **UI Framework** | Jetpack Compose + Material 3 | Vanilla HTML/CSS + plain JS modules |
| **Styling** | Material 3 ColorScheme | CSS custom properties (variables) per theme |
| **State Management** | ViewModel + StateFlow | Fetch from API on screen load; localStorage for UI-only prefs |
| **Navigation** | Compose Navigation (type-safe routes) | Hash-based routing (`#/dashboard`, `#/item/AAPL`) — no library needed |
| **Charts** | Custom Canvas-drawn (pie, line) | HTML5 Canvas API (port drawing logic directly) |
| **Image Loading** | Coil 2.7.0 | `<img>` pointing at server `/api/logo/:ticker` endpoint |
| **Background Work** | WorkManager | `node-cron` or `setInterval` on server |
| **Notifications** | NotificationCompat + AlarmManager | Web Notifications API (triggered by server polling or SSE) |
| **File I/O** | FileProvider, MediaStore, SAF | Server generates files; browser downloads via Blob URL |
| **Preferences (UI)** | SharedPreferences | `localStorage` for UI state (pin, theme, card order) |
| **Preferences (data)** | SharedPreferences | `settings` table in SQLite for data-affecting prefs |
| **DI** | Hilt | ES module imports (no DI needed) |
| **Install** | APK | `manifest.json` + Service Worker = Add to Home Screen |

**Server dependencies: 2** — `express`, `better-sqlite3`
**Client dependencies: 0** — vanilla JS, no build step

---

## Architecture Overview

```
┌──────────────────────────────────────────────────┐
│  Browser (PWA)                                    │
│                                                   │
│  index.html + vanilla JS modules                  │
│  ├── Hash router (#/dashboard, #/item/AAPL, ...)  │
│  ├── fetch('/api/...') for all data               │
│  ├── HTML5 Canvas charts                          │
│  ├── localStorage for UI prefs only               │
│  └── Service Worker for offline static caching    │
│                                                   │
└──────────────┬───────────────────────────────────┘
               │ HTTP (JSON)
               ▼
┌──────────────────────────────────────────────────┐
│  Node.js Server (Express)                         │
│                                                   │
│  ├── /api/*          REST endpoints (CRUD)        │
│  ├── /api/yahoo/*    Yahoo Finance proxy          │
│  ├── /api/refresh    Refresh all prices           │
│  ├── /api/backup     Export/import JSON           │
│  ├── /api/csv        CSV import/export            │
│  ├── /api/sql        SQL Explorer (raw queries)   │
│  ├── /api/logo/:t    Serve cached logo blobs      │
│  ├── Static files    Serve frontend (/ → index.html) │
│  └── node-cron       Auto-refresh scheduler       │
│                                                   │
│  SQLite via better-sqlite3 (investhelp.db)        │
│  └── 12 tables (same schema as Room v29)          │
│                                                   │
└──────────────────────────────────────────────────┘
```

**Why this is simpler than client-side DB:**
- Yahoo Finance calls happen server-side → zero CORS issues, crumb/cookie auth works natively
- SQLite on the server = exact same schema as Android Room, SQL Explorer works for free
- Auto-refresh runs as a server-side cron → works even when browser is closed
- Backup files live on server disk → no browser storage limits
- Logo blobs served as images → no IndexedDB blob management
- One `better-sqlite3` file = trivial to back up, copy, or migrate

---

## Project Structure

```
investhelp-pwa/
├── server/
│   ├── index.js                # Express server entry point
│   ├── db.js                   # SQLite setup, schema creation, seed data
│   ├── routes/
│   │   ├── accounts.js         # /api/accounts CRUD
│   │   ├── positions.js        # /api/positions CRUD
│   │   ├── transactions.js     # /api/transactions CRUD
│   │   ├── performance.js      # /api/performance CRUD
│   │   ├── watchlist.js        # /api/watchlists CRUD
│   │   ├── change-history.js   # /api/change-history CRUD
│   │   ├── definitions.js      # /api/definitions CRUD
│   │   ├── csv-mappings.js     # /api/csv-mappings CRUD
│   │   ├── sql-library.js      # /api/sql-library CRUD
│   │   ├── ai-library.js       # /api/ai-library CRUD
│   │   ├── yahoo.js            # /api/yahoo/* (quote, history, analysis, news, logo)
│   │   ├── refresh.js          # /api/refresh (refresh all prices)
│   │   ├── backup.js           # /api/backup (export/import JSON)
│   │   ├── csv-import.js       # /api/csv-import (parse + import CSV)
│   │   ├── sql-explorer.js     # /api/sql (execute raw SQL)
│   │   └── settings.js         # /api/settings (server-side prefs)
│   ├── services/
│   │   ├── yahoo-finance.js    # Yahoo Finance API client (all endpoints)
│   │   ├── auto-refresh.js     # Cron-based auto-refresh scheduler
│   │   └── csv-parser.js       # CSV parsing + mapping logic
│   └── investhelp.db           # SQLite database file (created on first run)
├── public/                     # Static files served by Express
│   ├── index.html              # Single page shell
│   ├── manifest.json           # PWA manifest
│   ├── sw.js                   # Service worker (offline static caching)
│   ├── css/
│   │   ├── base.css            # Reset, typography, layout
│   │   ├── themes.css          # 22+ theme definitions as CSS variables
│   │   └── components.css      # Reusable component styles
│   ├── js/
│   │   ├── app.js              # Entry point, router, init
│   │   ├── router.js           # Hash-based SPA router
│   │   ├── api.js              # API client (fetch wrapper for all /api/* calls)
│   │   ├── preferences.js      # localStorage for UI-only prefs (theme, pins, card order)
│   │   ├── screens/
│   │   │   ├── dashboard.js
│   │   │   ├── positions.js
│   │   │   ├── item-detail.js
│   │   │   ├── item-form.js
│   │   │   ├── transaction-list.js
│   │   │   ├── transaction-form.js
│   │   │   ├── analyze-price.js
│   │   │   ├── simulation.js
│   │   │   ├── account-list.js
│   │   │   ├── account-detail.js
│   │   │   ├── account-form.js
│   │   │   ├── performance.js
│   │   │   ├── watchlist.js
│   │   │   ├── settings.js
│   │   │   ├── sql-explorer.js
│   │   │   ├── ai-ticker.js
│   │   │   ├── next-day-actions.js
│   │   │   └── help.js
│   │   ├── components/
│   │   │   ├── top-bar.js      # Portfolio value button + hamburger
│   │   │   ├── bottom-nav.js   # Dashboard / Positions / Transaction tabs
│   │   │   ├── collapsible-card.js
│   │   │   ├── confirm-dialog.js
│   │   │   ├── date-range-picker.js
│   │   │   ├── pie-chart.js    # Canvas pie chart
│   │   │   ├── line-chart.js   # Canvas line chart (zoom/pan/tap)
│   │   │   ├── mini-chart.js   # Small sparkline chart
│   │   │   ├── data-table.js   # Grid table with gridlines, alternating rows
│   │   │   ├── ticker-icon.js  # 3D gradient icon with logo
│   │   │   ├── icon-3d.js      # Gradient icon box
│   │   │   └── filter-chips.js # Multi-select chip row
│   │   └── utils/
│   │       ├── format.js       # Currency, %, date formatting
│   │       └── dates.js        # Epoch days ↔ Date conversion
│   └── assets/
│       ├── help.html           # Existing help file (copy directly)
│       └── icons/              # PWA icons (192x192, 512x512)
├── package.json                # { "dependencies": { "express": "^4", "better-sqlite3": "^11" } }
└── backups/                    # Server-side backup files
```

---

## Step-by-Step Implementation

### Phase 1: Server Foundation (Express + SQLite + schema)

#### Step 1.1 — Project Init

```bash
mkdir investhelp-pwa && cd investhelp-pwa
npm init -y
npm install express better-sqlite3
mkdir -p server/routes server/services public/css public/js/screens public/js/components public/js/utils public/assets/icons backups
```

#### Step 1.2 — SQLite Schema (`server/db.js`)

Create all 12 tables matching Room v29 final schema. Since this is a fresh database (not migrating from Android), define the final schema directly — no migrations needed.

```js
const Database = require('better-sqlite3');
const path = require('path');

const db = new Database(path.join(__dirname, 'investhelp.db'));

// Enable WAL mode for better concurrent read performance
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

db.exec(`
  -- ============================================================
  -- Table 1: investment_accounts
  -- ============================================================
  CREATE TABLE IF NOT EXISTS investment_accounts (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL,
    description   TEXT    NOT NULL DEFAULT '',
    initialValue  REAL    NOT NULL DEFAULT 0,
    lastUpdatedOn INTEGER,          -- epoch seconds, nullable
    lastValue     REAL              -- nullable
  );

  -- ============================================================
  -- Table 2: investment_positions  (ticker is sole PK)
  -- ============================================================
  CREATE TABLE IF NOT EXISTS investment_positions (
    ticker       TEXT PRIMARY KEY,
    name         TEXT    NOT NULL DEFAULT '',
    type         TEXT    NOT NULL DEFAULT 'Stock',  -- Stock|ETF|Bond|MutualFund|Crypto|Other
    currentPrice REAL    NOT NULL DEFAULT 0,
    quantity     REAL    NOT NULL DEFAULT 0,
    dayGainLoss  REAL    NOT NULL DEFAULT 0,
    value        REAL    NOT NULL DEFAULT 0,
    dayHigh      REAL    NOT NULL DEFAULT 0,
    dayLow       REAL    NOT NULL DEFAULT 0,
    logo         BLOB                               -- cached company logo, nullable
  );

  -- ============================================================
  -- Table 3: investment_transactions
  -- ============================================================
  CREATE TABLE IF NOT EXISTS investment_transactions (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    date           INTEGER NOT NULL,                 -- epoch days
    time           INTEGER,                          -- seconds of day, nullable
    action         TEXT    NOT NULL DEFAULT 'Buy',   -- Buy|Sell
    ticker         TEXT    NOT NULL,
    numberOfShares REAL    NOT NULL DEFAULT 0,
    pricePerShare  REAL    NOT NULL DEFAULT 0,
    totalAmount    REAL    NOT NULL DEFAULT 0,
    note           TEXT    NOT NULL DEFAULT ''
  );
  CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_unique
    ON investment_transactions (date, action, ticker, totalAmount);
  CREATE INDEX IF NOT EXISTS idx_transactions_ticker
    ON investment_transactions (ticker);

  -- ============================================================
  -- Table 4: account_performance
  -- ============================================================
  CREATE TABLE IF NOT EXISTS account_performance (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    accountId  INTEGER NOT NULL REFERENCES investment_accounts(id) ON DELETE CASCADE,
    totalValue REAL    NOT NULL DEFAULT 0,
    date       INTEGER NOT NULL,                     -- epoch days
    note       TEXT    NOT NULL DEFAULT ''
  );
  CREATE UNIQUE INDEX IF NOT EXISTS idx_perf_account_date
    ON account_performance (accountId, date);
  CREATE INDEX IF NOT EXISTS idx_perf_accountId
    ON account_performance (accountId);

  -- ============================================================
  -- Table 5: watch_lists
  -- ============================================================
  CREATE TABLE IF NOT EXISTS watch_lists (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL
  );

  -- ============================================================
  -- Table 6: watch_list_items
  -- ============================================================
  CREATE TABLE IF NOT EXISTS watch_list_items (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    watchListId     INTEGER NOT NULL REFERENCES watch_lists(id) ON DELETE CASCADE,
    ticker          TEXT    NOT NULL,
    shares          REAL    NOT NULL DEFAULT 0,
    priceWhenAdded  REAL    NOT NULL DEFAULT 0,
    addedDate       INTEGER NOT NULL,                -- epoch days
    reminderDateTime INTEGER,                        -- epoch ms, nullable
    reminderMessage  TEXT    NOT NULL DEFAULT ''
  );
  CREATE INDEX IF NOT EXISTS idx_wli_watchListId
    ON watch_list_items (watchListId);

  -- ============================================================
  -- Table 7: change_history
  -- ============================================================
  CREATE TABLE IF NOT EXISTS change_history (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    date             INTEGER NOT NULL,               -- epoch days
    etfValue         REAL    NOT NULL DEFAULT 0,
    stockValue       REAL    NOT NULL DEFAULT 0,
    totalValue       REAL    NOT NULL DEFAULT 0,
    dailyChangeEtf   REAL    NOT NULL DEFAULT 0,
    dailyChangeStock  REAL    NOT NULL DEFAULT 0,
    dailyChangeTotal  REAL    NOT NULL DEFAULT 0
  );
  CREATE UNIQUE INDEX IF NOT EXISTS idx_change_history_date
    ON change_history (date);

  -- ============================================================
  -- Table 8: definitions
  -- ============================================================
  CREATE TABLE IF NOT EXISTS definitions (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    description TEXT    NOT NULL DEFAULT ''
  );

  -- ============================================================
  -- Table 9: csv_import_mappings
  -- ============================================================
  CREATE TABLE IF NOT EXISTS csv_import_mappings (
    importType    TEXT PRIMARY KEY,                   -- Position|Transaction|Performance
    mappingsJson  TEXT NOT NULL DEFAULT '{}',
    dateFormatJson TEXT NOT NULL DEFAULT '{}'
  );

  -- ============================================================
  -- Table 10: csv_named_mappings
  -- ============================================================
  CREATE TABLE IF NOT EXISTS csv_named_mappings (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL,
    importType    TEXT    NOT NULL,
    mappingsJson  TEXT    NOT NULL DEFAULT '{}',
    dateFormatJson TEXT   NOT NULL DEFAULT '{}'
  );

  -- ============================================================
  -- Table 11: sql_library
  -- ============================================================
  CREATE TABLE IF NOT EXISTS sql_library (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    category    TEXT NOT NULL DEFAULT '',
    sql         TEXT NOT NULL DEFAULT ''
  );

  -- ============================================================
  -- Table 12: ai_library
  -- ============================================================
  CREATE TABLE IF NOT EXISTS ai_library (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    promptText  TEXT NOT NULL DEFAULT ''
  );

  -- ============================================================
  -- Table 13: settings (server-side preferences)
  -- ============================================================
  CREATE TABLE IF NOT EXISTS settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL DEFAULT ''
  );
`);

// Seed definitions if empty
const defCount = db.prepare('SELECT COUNT(*) as n FROM definitions').get().n;
if (defCount === 0) {
  const insert = db.prepare('INSERT INTO definitions (name, description) VALUES (?, ?)');
  const seeds = [
    ['Market Cap', 'Total market value of a company\'s outstanding shares'],
    ['Trailing P/E', 'Price-to-earnings ratio based on the last 12 months of actual earnings'],
    ['Forward P/E', 'Price-to-earnings ratio based on projected future earnings'],
    ['EPS', 'Earnings per share — net income divided by outstanding shares'],
    ['Dividend Yield', 'Annual dividend payment as a percentage of the stock price'],
    ['52-Week Range', 'The lowest and highest prices at which a stock has traded in the past year'],
    ['Profit Margins', 'Percentage of revenue that becomes profit after all expenses'],
    ['Return on Equity', 'Net income as a percentage of shareholder equity — measures profitability'],
  ];
  const tx = db.transaction(() => seeds.forEach(([n, d]) => insert.run(n, d)));
  tx();
}

// Seed AI library if empty
const aiCount = db.prepare('SELECT COUNT(*) as n FROM ai_library').get().n;
if (aiCount === 0) {
  const insert = db.prepare('INSERT INTO ai_library (name, description, promptText) VALUES (?, ?, ?)');
  insert.run('Forensic Ticker Deep-Dive', 'Comprehensive fundamental analysis', 'Analyze {TICKER} ...');
  insert.run('10-K/10-Q Earnings Summarizer', 'Summarize recent earnings filings', 'Summarize the latest ...');
  insert.run('ETF Under the Hood', 'Analyze ETF holdings and strategy', 'Break down the ETF {TICKER} ...');
}

module.exports = db;
```

#### Step 1.3 — Express Server (`server/index.js`)

```js
const express = require('express');
const path = require('path');

const app = express();
app.use(express.json({ limit: '50mb' }));
app.use(express.static(path.join(__dirname, '..', 'public')));

// Mount API routes
app.use('/api/accounts',       require('./routes/accounts'));
app.use('/api/positions',      require('./routes/positions'));
app.use('/api/transactions',   require('./routes/transactions'));
app.use('/api/performance',    require('./routes/performance'));
app.use('/api/watchlists',     require('./routes/watchlist'));
app.use('/api/change-history', require('./routes/change-history'));
app.use('/api/definitions',    require('./routes/definitions'));
app.use('/api/csv-mappings',   require('./routes/csv-mappings'));
app.use('/api/sql-library',    require('./routes/sql-library'));
app.use('/api/ai-library',     require('./routes/ai-library'));
app.use('/api/yahoo',          require('./routes/yahoo'));
app.use('/api/refresh',        require('./routes/refresh'));
app.use('/api/backup',         require('./routes/backup'));
app.use('/api/csv-import',     require('./routes/csv-import'));
app.use('/api/sql',            require('./routes/sql-explorer'));
app.use('/api/settings',       require('./routes/settings'));

// SPA fallback — serve index.html for all non-API, non-static routes
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, '..', 'public', 'index.html'));
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`InvestHelp running on http://localhost:${PORT}`));
```

#### Step 1.4 — REST API Design

Every route file follows the same pattern: import `db`, define CRUD endpoints, export router.

**Full API surface (all endpoints):**

```
ACCOUNTS
  GET    /api/accounts              → all accounts
  GET    /api/accounts/:id          → single account
  POST   /api/accounts              → create account { name, description, initialValue }
  PUT    /api/accounts/:id          → update account
  DELETE /api/accounts/:id          → delete account (CASCADE: performance records)

POSITIONS
  GET    /api/positions             → all positions
  GET    /api/positions/:ticker     → single position
  POST   /api/positions             → upsert position { ticker, name, type, currentPrice, quantity, ... }
  PUT    /api/positions/:ticker     → update position
  DELETE /api/positions/:ticker     → delete position
  GET    /api/positions/:ticker/logo → serve logo as image/webp (from BLOB column)
  GET    /api/positions/summary     → { totalValue, dayGainLoss, dayPercent, etfValue, stockValue }

TRANSACTIONS
  GET    /api/transactions                    → all (with optional ?ticker= filter)
  GET    /api/transactions/:id                → single
  POST   /api/transactions                    → create transaction
  POST   /api/transactions/if-not-exists      → insert with IGNORE (for CSV import)
  PUT    /api/transactions/:id                → update
  DELETE /api/transactions/:id                → delete single
  POST   /api/transactions/bulk-delete        → delete multiple { ids: [1, 2, 3] }
  GET    /api/transactions/stats/:ticker      → avg/max/min prices for ticker (with ?startDate=&endDate=&action=)

ACCOUNT PERFORMANCE
  GET    /api/performance                     → all records (with optional ?accountId= filter)
  POST   /api/performance                     → create record { accountId, totalValue, date, note }
  PUT    /api/performance/:id                 → update record (note editing)
  DELETE /api/performance/:id                 → delete record

WATCH LISTS
  GET    /api/watchlists                      → all watch lists
  POST   /api/watchlists                      → create { name }
  PUT    /api/watchlists/:id                  → rename { name }
  DELETE /api/watchlists/:id                  → delete (CASCADE: items)
  GET    /api/watchlists/:id/items            → items in watch list
  POST   /api/watchlists/:id/items            → add item { ticker, shares, priceWhenAdded, ... }
  PUT    /api/watchlists/items/:itemId        → update item (reminder, shares, etc.)
  DELETE /api/watchlists/items/:itemId        → delete item

CHANGE HISTORY
  GET    /api/change-history                  → all records
  POST   /api/change-history                  → upsert record (INSERT OR REPLACE by date)
  DELETE /api/change-history/:id              → delete record

DEFINITIONS
  GET    /api/definitions                     → all definitions
  POST   /api/definitions                     → create { name, description }
  PUT    /api/definitions/:id                 → update
  DELETE /api/definitions/:id                 → delete

CSV MAPPINGS
  GET    /api/csv-mappings/:importType        → get mapping for type
  PUT    /api/csv-mappings/:importType        → upsert mapping
  GET    /api/csv-mappings/named/:importType  → list named mappings for type
  POST   /api/csv-mappings/named              → save named mapping
  DELETE /api/csv-mappings/named/:id          → delete named mapping

SQL LIBRARY
  GET    /api/sql-library                     → all saved queries
  POST   /api/sql-library                     → save query { name, description, category, sql }
  DELETE /api/sql-library/:id                 → delete

AI LIBRARY
  GET    /api/ai-library                      → all prompts
  POST   /api/ai-library                      → save prompt { name, description, promptText }
  DELETE /api/ai-library/:id                  → delete

YAHOO FINANCE (server-side proxy — no CORS issues)
  GET    /api/yahoo/quote/:ticker             → current quote (price, dayHigh, dayLow, previousClose, shortName, quoteType)
  GET    /api/yahoo/history/:ticker           → price history (?range=1d&interval=1m)
  GET    /api/yahoo/history-period/:ticker    → price history (?period1=&period2=&interval=)
  GET    /api/yahoo/analysis/:ticker          → quoteSummary (analysis info, with crumb auth)
  GET    /api/yahoo/news/:ticker              → news articles (?count=5)
  GET    /api/yahoo/scan/:ticker              → scan data (20-day SMA, volume, support/resistance)
  GET    /api/yahoo/logo/:ticker              → fetch logo from CDN sources, return image
  GET    /api/yahoo/report/:ticker            → full report (combines quote + analysis + news)

REFRESH
  POST   /api/refresh                         → refresh all positions (fetch quotes, update DB, optionally record change history)
  GET    /api/refresh/status                  → current refresh status { isRefreshing, currentTicker, progress }

BACKUP & RESTORE
  GET    /api/backup/export                   → download JSON backup file
  POST   /api/backup/import                   → upload + restore JSON backup (multipart/form-data)
  GET    /api/backup/list                     → list backup files on server
  DELETE /api/backup/:filename                → delete a backup file

CSV IMPORT
  POST   /api/csv-import/preview              → upload CSV, return first 3 rows + headers (multipart/form-data)
  POST   /api/csv-import/execute              → import CSV with mapping { importType, mappingId, file }
  
SQL EXPLORER
  POST   /api/sql/execute                     → execute raw SQL { sql: "SELECT * FROM ..." }
                                                 returns { columns: [...], rows: [...] } or { changes: N }
  GET    /api/sql/tables                      → list all tables with column details

SETTINGS (server-side prefs that affect data behavior)
  GET    /api/settings                        → all settings as { key: value } object
  PUT    /api/settings/:key                   → set a single setting { value: "..." }
```

**Server-side settings** (stored in `settings` table — these affect data, not just UI):
```
auto_update_change_history   "true"|"false"     Record change history on refresh
auto_refresh_enabled         "true"|"false"     Enable server-side cron refresh
auto_refresh_interval        "5m"|"30m"|"1h"|"5h"|"market_close"
auto_backup_on_refresh       "true"|"false"     Auto-backup after each refresh
auto_backup_keep_count       "10"               Max backup files to keep
news_article_count           "5"|"10"|"20"      Articles per ticker
ai_enabled                   "true"|"false"
ai_api_key                   "sk-..."
trailing_stop_pct            "10"
profit_target_pct            "20"
stock_concentration_cap      "10"
etf_concentration_cap        "25"
```

**Client-side localStorage** (UI-only, never sent to server):
```
app_theme                    "Default"|"Ocean"|...   Theme name
pin_card_*                   true|false               Card pin states
dashboard_card_order         "summary,indices,..."    Card display order
market_indices               "^IXIC,^GSPC,..."        Enabled indices
market_indices_order         "^IXIC,^GSPC,..."        Index display order
warn_before_delete           true|false               Confirm dialog toggle
last_refreshed_at            epoch ms                 Last refresh timestamp
```

#### Step 1.5 — Example Route File (`server/routes/positions.js`)

```js
const express = require('express');
const router = express.Router();
const db = require('../db');

// GET /api/positions
router.get('/', (req, res) => {
  const rows = db.prepare('SELECT ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow FROM investment_positions ORDER BY value DESC').all();
  res.json(rows);
});

// GET /api/positions/summary
router.get('/summary', (req, res) => {
  const all = db.prepare('SELECT type, value, dayGainLoss FROM investment_positions').all();
  const totalValue = all.reduce((s, r) => s + r.value, 0);
  const dayGainLoss = all.reduce((s, r) => s + r.dayGainLoss, 0);
  const etfValue = all.filter(r => r.type === 'ETF').reduce((s, r) => s + r.value, 0);
  const stockValue = all.filter(r => r.type === 'Stock').reduce((s, r) => s + r.value, 0);
  res.json({ totalValue, dayGainLoss, etfValue, stockValue });
});

// GET /api/positions/:ticker
router.get('/:ticker', (req, res) => {
  const row = db.prepare('SELECT * FROM investment_positions WHERE ticker = ?').get(req.params.ticker);
  if (!row) return res.status(404).json({ error: 'Not found' });
  res.json(row);
});

// GET /api/positions/:ticker/logo
router.get('/:ticker/logo', (req, res) => {
  const row = db.prepare('SELECT logo FROM investment_positions WHERE ticker = ?').get(req.params.ticker);
  if (!row || !row.logo) return res.status(404).end();
  res.set('Content-Type', 'image/webp');
  res.set('Cache-Control', 'public, max-age=86400');
  res.send(row.logo);
});

// POST /api/positions (upsert)
router.post('/', (req, res) => {
  const { ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow } = req.body;
  db.prepare(`
    INSERT INTO investment_positions (ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(ticker) DO UPDATE SET
      name=excluded.name, type=excluded.type, currentPrice=excluded.currentPrice,
      quantity=excluded.quantity, dayGainLoss=excluded.dayGainLoss, value=excluded.value,
      dayHigh=excluded.dayHigh, dayLow=excluded.dayLow
  `).run(ticker, name || '', type || 'Stock', currentPrice || 0, quantity || 0, dayGainLoss || 0, value || 0, dayHigh || 0, dayLow || 0);
  res.json({ ok: true });
});

// DELETE /api/positions/:ticker
router.delete('/:ticker', (req, res) => {
  db.prepare('DELETE FROM investment_positions WHERE ticker = ?').run(req.params.ticker);
  res.json({ ok: true });
});

module.exports = router;
```

#### Step 1.6 — Yahoo Finance Service (`server/services/yahoo-finance.js`)

All Yahoo Finance calls happen server-side — no CORS proxy needed.

```js
// Crumb/cookie state (module-level singleton)
let crumb = null;
let cookies = '';

async function refreshCrumb() {
  // 1. GET https://fc.yahoo.com/cupcake → extract Set-Cookie
  const r1 = await fetch('https://fc.yahoo.com/cupcake', { redirect: 'manual' });
  cookies = r1.headers.get('set-cookie') || '';
  // 2. GET https://query2.finance.yahoo.com/v1/test/getcrumb with cookie
  const r2 = await fetch('https://query2.finance.yahoo.com/v1/test/getcrumb', {
    headers: { Cookie: cookies }
  });
  crumb = await r2.text();
}

async function fetchQuote(ticker) {
  const url = `https://query1.finance.yahoo.com/v8/finance/chart/${ticker}?range=1d&interval=1m`;
  const resp = await fetch(url);
  const data = await resp.json();
  const meta = data.chart.result[0].meta;
  const quotes = data.chart.result[0].indicators.quote[0];
  return {
    price: meta.regularMarketPrice,
    previousClose: meta.chartPreviousClose || meta.previousClose,
    shortName: meta.shortName || null,
    dayHigh: Math.max(...quotes.high.filter(Boolean)),
    dayLow: Math.min(...quotes.low.filter(v => v > 0)),
    quoteType: meta.instrumentType || null,
  };
}

async function fetchPriceHistory(ticker, range, interval) {
  const url = `https://query1.finance.yahoo.com/v8/finance/chart/${ticker}?range=${range}&interval=${interval}`;
  const resp = await fetch(url);
  const data = await resp.json();
  const result = data.chart.result[0];
  const timestamps = result.timestamp || [];
  const closes = result.indicators.quote[0].close || [];
  return timestamps.map((t, i) => ({ timestamp: t, close: closes[i] })).filter(p => p.close != null);
}

async function fetchPriceHistoryByPeriod(ticker, period1, period2, interval) {
  const url = `https://query1.finance.yahoo.com/v8/finance/chart/${ticker}?period1=${period1}&period2=${period2}&interval=${interval}`;
  const resp = await fetch(url);
  const data = await resp.json();
  const result = data.chart.result[0];
  const timestamps = result.timestamp || [];
  const closes = result.indicators.quote[0].close || [];
  return timestamps.map((t, i) => ({ timestamp: t, close: closes[i] })).filter(p => p.close != null);
}

async function fetchAnalysisInfo(ticker) {
  if (!crumb) await refreshCrumb();
  const modules = 'assetProfile,defaultKeyStatistics,financialData,summaryDetail,calendarEvents,recommendationTrend,fundProfile,topHoldings';
  const url = `https://query2.finance.yahoo.com/v10/finance/quoteSummary/${ticker}?modules=${modules}&crumb=${crumb}`;
  const resp = await fetch(url, { headers: { Cookie: cookies } });
  if (resp.status === 401 || resp.status === 403) {
    await refreshCrumb();
    return fetchAnalysisInfo(ticker); // retry once
  }
  const data = await resp.json();
  const result = data.quoteSummary.result[0];
  // Extract and flatten from assetProfile, financialData, defaultKeyStatistics, summaryDetail
  return { /* ... mapped fields ... */ };
}

async function fetchNews(ticker, count = 5) {
  const url = `https://query2.finance.yahoo.com/v1/finance/search?q=${ticker}&newsCount=${count}`;
  const resp = await fetch(url);
  const data = await resp.json();
  return (data.news || []).map(n => ({
    title: n.title, link: n.link, publisher: n.publisher, publishedAt: n.providerPublishTime
  }));
}

async function fetchScanData(ticker) {
  const history = await fetchPriceHistory(ticker, '1mo', '1d');
  const last20 = history.slice(-20);
  const sma = last20.reduce((s, p) => s + p.close, 0) / last20.length;
  // Compute volume, support, resistance from highs/lows...
  return { twentyDaySma: sma, /* ... */ };
}

async function fetchLogo(ticker) {
  const sources = [
    `https://companiesmarketcap.com/img/company-logos/64/${ticker.toLowerCase()}.webp`,
    `https://assets.parqet.com/logos/symbol/${ticker}`,
    `https://storage.googleapis.com/iexcloud-hl37opg/api/logos/${ticker}.png`
  ];
  for (const url of sources) {
    try {
      const resp = await fetch(url);
      if (resp.ok) return Buffer.from(await resp.arrayBuffer());
    } catch {}
  }
  return null;
}

module.exports = { fetchQuote, fetchPriceHistory, fetchPriceHistoryByPeriod,
  fetchAnalysisInfo, fetchNews, fetchScanData, fetchLogo, refreshCrumb };
```

#### Step 1.7 — Auto-Refresh Scheduler (`server/services/auto-refresh.js`)

```js
const db = require('../db');
const yahoo = require('./yahoo-finance');

let refreshInterval = null;

function startAutoRefresh(intervalMs) {
  stopAutoRefresh();
  refreshInterval = setInterval(() => refreshAll(), intervalMs);
}

function stopAutoRefresh() {
  if (refreshInterval) { clearInterval(refreshInterval); refreshInterval = null; }
}

async function refreshAll() {
  const positions = db.prepare('SELECT ticker FROM investment_positions').all();
  const update = db.prepare(`
    UPDATE investment_positions
    SET currentPrice = ?, dayGainLoss = ?, value = ?, dayHigh = ?, dayLow = ?, name = CASE WHEN ? != '' THEN ? ELSE name END
    WHERE ticker = ?
  `);

  for (const { ticker } of positions) {
    try {
      const q = await yahoo.fetchQuote(ticker);
      const row = db.prepare('SELECT quantity FROM investment_positions WHERE ticker = ?').get(ticker);
      const value = q.price * (row?.quantity || 0);
      const dayGL = (q.price - q.previousClose) * (row?.quantity || 0);
      update.run(q.price, dayGL, value, q.dayHigh, q.dayLow, q.shortName || '', q.shortName || '', ticker);

      // Fetch logo if missing
      const logo = db.prepare('SELECT logo FROM investment_positions WHERE ticker = ?').get(ticker);
      if (!logo?.logo) {
        const logoBlob = await yahoo.fetchLogo(ticker);
        if (logoBlob) db.prepare('UPDATE investment_positions SET logo = ? WHERE ticker = ?').run(logoBlob, ticker);
      }
    } catch (err) {
      console.error(`Refresh failed for ${ticker}:`, err.message);
    }
  }

  // Optionally record change history
  const autoHistory = db.prepare("SELECT value FROM settings WHERE key = 'auto_update_change_history'").get();
  if (autoHistory?.value === 'true') {
    // Compute ETF/Stock/Total sums and upsert into change_history for today
  }
}

module.exports = { startAutoRefresh, stopAutoRefresh, refreshAll };
```

---

### Phase 2: Frontend Shell (PWA scaffold, router, API client)

#### Step 2.1 — PWA Shell (`public/index.html` + `manifest.json` + `sw.js`)

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <meta name="theme-color" content="#2E7D32">
  <link rel="manifest" href="manifest.json">
  <link rel="stylesheet" href="css/base.css">
  <link rel="stylesheet" href="css/themes.css">
  <link rel="stylesheet" href="css/components.css">
  <title>InvestHelp</title>
</head>
<body>
  <div id="top-bar"></div>
  <div id="app"></div>
  <div id="bottom-nav"></div>
  <script type="module" src="js/app.js"></script>
</body>
</html>
```

`sw.js` — cache static assets only; API calls always go to network:
```js
const CACHE = 'investhelp-v1';
const STATIC = ['/', '/index.html', '/css/base.css', '/css/themes.css',
  '/css/components.css', '/js/app.js' /* ...all JS modules */ ];

self.addEventListener('install', e => e.waitUntil(
  caches.open(CACHE).then(c => c.addAll(STATIC))
));

self.addEventListener('fetch', e => {
  if (e.request.url.includes('/api/')) {
    e.respondWith(fetch(e.request)); // API = always network
  } else {
    e.respondWith(caches.match(e.request).then(r => r || fetch(e.request)));
  }
});
```

#### Step 2.2 — API Client (`public/js/api.js`)

Single module wrapping all server calls — every screen imports from here:

```js
const BASE = ''; // same-origin, no prefix needed

async function get(path) {
  const resp = await fetch(BASE + path);
  if (!resp.ok) throw new Error(`GET ${path}: ${resp.status}`);
  return resp.json();
}

async function post(path, body) {
  const resp = await fetch(BASE + path, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
  });
  if (!resp.ok) throw new Error(`POST ${path}: ${resp.status}`);
  return resp.json();
}

async function put(path, body) {
  const resp = await fetch(BASE + path, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
  });
  if (!resp.ok) throw new Error(`PUT ${path}: ${resp.status}`);
  return resp.json();
}

async function del(path) {
  const resp = await fetch(BASE + path, { method: 'DELETE' });
  if (!resp.ok) throw new Error(`DELETE ${path}: ${resp.status}`);
  return resp.json();
}

// Convenience wrappers per domain
export const positions = {
  list:    ()        => get('/api/positions'),
  get:     (ticker)  => get(`/api/positions/${ticker}`),
  summary: ()        => get('/api/positions/summary'),
  upsert:  (data)    => post('/api/positions', data),
  delete:  (ticker)  => del(`/api/positions/${ticker}`),
  logoUrl: (ticker)  => `/api/positions/${ticker}/logo`,
};

export const transactions = {
  list:       (ticker) => get(`/api/transactions${ticker ? '?ticker=' + ticker : ''}`),
  get:        (id)     => get(`/api/transactions/${id}`),
  create:     (data)   => post('/api/transactions', data),
  update:     (id, d)  => put(`/api/transactions/${id}`, d),
  delete:     (id)     => del(`/api/transactions/${id}`),
  bulkDelete: (ids)    => post('/api/transactions/bulk-delete', { ids }),
  stats:      (ticker, params) => get(`/api/transactions/stats/${ticker}?${new URLSearchParams(params)}`),
};

export const accounts     = { /* same pattern */ };
export const performance  = { /* same pattern */ };
export const watchlists   = { /* same pattern */ };
export const changeHistory = { /* same pattern */ };
export const definitions  = { /* same pattern */ };
export const csvMappings  = { /* same pattern */ };
export const sqlLibrary   = { /* same pattern */ };
export const aiLibrary    = { /* same pattern */ };

export const yahoo = {
  quote:       (ticker) => get(`/api/yahoo/quote/${ticker}`),
  history:     (ticker, range, interval) => get(`/api/yahoo/history/${ticker}?range=${range}&interval=${interval}`),
  historyPeriod: (ticker, p1, p2, interval) => get(`/api/yahoo/history-period/${ticker}?period1=${p1}&period2=${p2}&interval=${interval}`),
  analysis:    (ticker) => get(`/api/yahoo/analysis/${ticker}`),
  news:        (ticker, count) => get(`/api/yahoo/news/${ticker}?count=${count || 5}`),
  scan:        (ticker) => get(`/api/yahoo/scan/${ticker}`),
  report:      (ticker) => get(`/api/yahoo/report/${ticker}`),
};

export const refresh = {
  all:    () => post('/api/refresh', {}),
  status: () => get('/api/refresh/status'),
};

export const backup = {
  exportUrl: () => '/api/backup/export',
  import:    (file) => { /* multipart upload */ },
  list:      () => get('/api/backup/list'),
};

export const sql = {
  execute: (query) => post('/api/sql/execute', { sql: query }),
  tables:  () => get('/api/sql/tables'),
};

export const settings = {
  getAll:   () => get('/api/settings'),
  set:      (key, value) => put(`/api/settings/${key}`, { value }),
};
```

#### Step 2.3 — Hash Router (`public/js/router.js`)

Same as before — minimal hash-based SPA router:

```
Routes:
  #/                          → Dashboard
  #/positions                 → Positions (STOCK/ETF/Analysis tabs)
  #/positions/:ticker         → Position Detail
  #/item/:ticker              → Item Detail (3 tabs: Details, Price History, Transactions)
  #/item-form                 → New item
  #/item-form/:ticker         → Edit item
  #/transactions              → Transaction list
  #/transaction-form          → New transaction
  #/transaction-form/:id      → Edit transaction
  #/analyze-price/:ticker     → Price analysis
  #/simulation/:ticker/:shares/:customDays? → Simulation
  #/accounts                  → Account list
  #/account/:id               → Account detail
  #/account-form              → New account
  #/account-form/:id          → Edit account
  #/performance               → Account performance
  #/watchlist                 → Watch lists
  #/settings                  → Settings
  #/sql-explorer              → SQL Explorer
  #/sql-result/:encodedSql    → SQL result
  #/ai-ticker/:ticker         → AI analysis
  #/next-day-actions          → Next day actions
  #/help                      → Help
```

#### Step 2.4 — Preferences (`public/js/preferences.js`)

Only for UI-local state. Data-affecting settings go through `/api/settings`.

```js
const DEFAULTS = {
  app_theme: 'Default',
  warn_before_delete: true,
  market_indices: '^IXIC,^GSPC,^DJI,GC=F',
  market_indices_order: '^IXIC,^GSPC,^DJI,GC=F,^RUT,SI=F,CL=F,BTC-USD',
  dashboard_card_order: 'portfolio_summary,market_indices,daily_glance,positions,position_details,watch_list',
  pin_card_portfolio_summary: true,
  pin_card_market_indices: false,
  pin_card_daily_glance: false,
  pin_card_position_details: false,
  pin_card_watch_list: false,
  last_refreshed_at: -1,
};

export function getPref(key) {
  const val = localStorage.getItem(key);
  return val !== null ? JSON.parse(val) : DEFAULTS[key];
}

export function setPref(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
  window.dispatchEvent(new CustomEvent('pref-changed', { detail: { key, value } }));
}
```

---

### Phase 3: Core UI Components

Identical to the client-only plan — these are pure frontend components:

#### Step 3.1 — Theme System (`css/themes.css`)

Port all 22+ themes as CSS custom property sets (same as before — see Appendix A).

#### Step 3.2 — Layout Components

- **Top Bar** — portfolio value button, spinner, watch list icon, hamburger menu
- **Bottom Nav** — 3 tabs (Dashboard, Positions, Transaction) with 3D gradient icons
- **Collapsible Card** — title + pin button, expand/collapse, pin state in localStorage
- **Data Table** — header, gridlines, alternating rows, click-to-detail, horizontal scroll
- **Confirm Dialog** — respects `warn_before_delete` localStorage pref
- **Filter Chips** — horizontal wrapping, multi-select toggle

#### Step 3.3 — Canvas Charts

Port the 5 chart types from Compose Canvas to HTML5 Canvas:

- **Line Chart** — zoom/pan/tap-to-select, multi-series, smooth curve toggle, tooltips
- **Pie Chart** — arc segments, exploding slice, legend table, click-to-navigate
- **Mini Chart** — sparkline, no interaction, fixed height

#### Step 3.4 — Ticker Icon

```
- Deterministic color from ticker hash
- CSS gradient + box-shadow rounded box
- White letter fallback
- Logo: <img src="/api/positions/{ticker}/logo"> with onerror fallback to letter
```

---

### Phase 4: Screens

Same screen list, but every screen now fetches from the API instead of a local DB.

**Data flow pattern** (every screen follows this):
```js
// Example: dashboard.js
import { positions, changeHistory, yahoo } from '../api.js';

export async function render(container) {
  container.innerHTML = '<div class="loading">Loading...</div>';

  const [posData, summary, history] = await Promise.all([
    positions.list(),
    positions.summary(),
    changeHistory.list(),
  ]);

  // Render with the fetched data...
  container.innerHTML = buildDashboardHTML(posData, summary, history);
  attachEventListeners(container);
}
```

Screens (implement in this order):

1. **Dashboard** — 5 collapsible sections (Portfolio Summary, Market Indices, Daily Glance, Positions Pie, Position Details)
2. **Positions** — 3 tabs (STOCK, ETF, Analysis) with pie charts and item rows
3. **Item Detail** — 3 tabs (Details, Price History, Transactions) with charts and analysis
4. **Transaction Form + List** — CRUD with multi-select bulk delete
5. **Simulation** — time range chips, historical fetch, P/L chart
6. **Account Performance** — multi-account overlay chart, records grid
7. **Watch List** — collapsible panels per list, add/delete/remind
8. **Settings** — themes, toggles, CSV import, backup/restore
9. **Remaining** — accounts, analyze price, SQL explorer, AI ticker, next-day actions, help

---

### Phase 5: Background Features

#### Step 5.1 — Auto-Refresh (Server-Side Cron)

Runs on the server even when the browser is closed:

```js
// In server/index.js startup
const autoRefresh = require('./services/auto-refresh');
const db = require('./db');

const setting = db.prepare("SELECT value FROM settings WHERE key = 'auto_refresh_enabled'").get();
if (setting?.value === 'true') {
  const interval = db.prepare("SELECT value FROM settings WHERE key = 'auto_refresh_interval'").get();
  const ms = { '5m': 300000, '30m': 1800000, '1h': 3600000, '5h': 18000000, 'market_close': 86400000 };
  autoRefresh.startAutoRefresh(ms[interval?.value] || 1800000);
}
```

The frontend polls `/api/refresh/status` to show progress, or uses Server-Sent Events (SSE) for real-time updates:

```js
// Optional: SSE endpoint for live refresh progress
// GET /api/refresh/events → EventSource stream
```

#### Step 5.2 — Notifications (Watch List Reminders)

Client-side `setTimeout` + Web Notifications API for tab-open reminders. For background, the server can expose a `/api/reminders/due` endpoint that the Service Worker polls.

#### Step 5.3 — Backup/Restore

**Export**: `GET /api/backup/export` → server generates JSON, browser downloads it.
**Import**: `POST /api/backup/import` → browser uploads JSON file, server restores it.
**Auto-backup**: server-side after each refresh (configurable via settings).

```js
// Server-side backup logic
function exportBackup() {
  const accounts = db.prepare('SELECT * FROM investment_accounts').all();
  const positions = db.prepare('SELECT ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow FROM investment_positions').all();
  const transactions = db.prepare('SELECT * FROM investment_transactions').all();
  return { version: 4, accounts, positions: items, transactions };
}
```

#### Step 5.4 — CSV Import

File upload goes to server → server parses CSV, applies mapping, inserts into DB:

```
POST /api/csv-import/preview   → { headers: [...], rows: [[...], [...], [...]] }
POST /api/csv-import/execute   → { imported: 15, skipped: 2, errors: [...] }
```

#### Step 5.5 — SQL Explorer

Direct SQLite access on the server — full SQL support, no workarounds:

```
POST /api/sql/execute { sql: "SELECT * FROM investment_positions WHERE type = 'ETF'" }
→ { columns: ["ticker", "name", ...], rows: [["QQQ", "Invesco QQQ", ...], ...] }

GET /api/sql/tables
→ [{ name: "investment_positions", columns: [{ name: "ticker", type: "TEXT", pk: 1, notnull: 0 }, ...] }, ...]
```

CSV export: client fetches query results, builds CSV in browser, triggers download.

---

## Date/Time Handling

Same conventions as Android Room — dates stored as integers in SQLite:

- `LocalDate` → epoch days (integer): `Math.floor(Date.now() / 86400000)`
- `LocalTime` → seconds of day (integer)
- `LocalDateTime` → epoch milliseconds (long)

```js
// Client-side format helpers (public/js/utils/format.js)
export function formatCurrency(n) { return '$' + n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
export function formatPercent(n) { return (n >= 0 ? '+' : '') + n.toFixed(2) + '%'; }
export function formatDate(epochDays) { return new Date(epochDays * 86400000).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }); }
export function toEpochDays(date) { return Math.floor(date.getTime() / 86400000); }
export function fromEpochDays(days) { return new Date(days * 86400000); }
```

---

## Data Migration (Android → PWA)

1. Export JSON backup from Android app (Settings > Data Management > Export)
2. Upload to PWA via Settings > Data Management > Import (`POST /api/backup/import`)
3. Server parses v1/v2/v3/v4 format and inserts into SQLite
4. Same JSON format — no conversion needed

---

## What's NOT Needed in PWA

| Android Feature | PWA Status | Reason |
|----------------|------------|--------|
| Hilt DI | Skip | ES module imports on client; simple `require()` on server |
| Room migrations (v1-v29) | Skip | Fresh schema on server — define final v29 directly |
| AndroidManifest | Skip | `manifest.json` replaces it |
| Gradle build system | Skip | `npm start` runs the server |
| SplashScreen API | Skip | PWA splash auto-generated from manifest |
| FileProvider | Skip | Server serves files; browser downloads |
| Coil image library | Skip | `<img src="/api/positions/:ticker/logo">` |
| WorkManager | Replaced | Server-side `setInterval` / `node-cron` |
| AlarmManager | Partial | `setTimeout` on client; server polling for robust reminders |
| BroadcastReceiver | Skip | Not applicable in web |
| MediaStore | Skip | Blob download for chart PNG export |
| IndexedDB | Skip | All data lives in server SQLite |
| CORS proxy | Skip | Yahoo Finance calls are server-side |

---

## Running the App

```bash
# Install (once)
npm install

# Start server (serves both API and frontend)
node server/index.js
# → InvestHelp running on http://localhost:3000

# Or with auto-restart during development
npx nodemon server/index.js
```

No build step. Edit a `.js` or `.css` file, refresh the browser.

---

## Implementation Order Summary

```
Phase 1 — Server Foundation (est. 3-4 days)
  1.1  Project init (npm, folders)
  1.2  SQLite schema (12 tables + settings + seed data)
  1.3  Express server with static file serving
  1.4  REST API routes (all 16 route files, ~60 endpoints)
  1.5  Yahoo Finance service (7 methods, crumb auth)
  1.6  Auto-refresh scheduler (server-side cron)

Phase 2 — Frontend Shell (est. 1-2 days)
  2.1  PWA shell (index.html, manifest.json, sw.js)
  2.2  API client module (fetch wrapper for all endpoints)
  2.3  Hash router (20 routes)
  2.4  Preferences (localStorage for UI-only state)

Phase 3 — UI Components (est. 2-3 days)
  3.1  Theme system (22 themes as CSS variables)
  3.2  Layout components (top bar, bottom nav, collapsible card, data table, dialogs, chips)
  3.3  Canvas charts (line chart, pie chart, mini chart)
  3.4  Ticker icon component

Phase 4 — Screens (est. 5-7 days)
  4.1  Dashboard (5 collapsible sections)
  4.2  Positions screen (3 tabs)
  4.3  Item Detail (3 tabs, charts, analysis, news)
  4.4  Transaction form + list
  4.5  Simulation
  4.6  Account Performance
  4.7  Watch List
  4.8  Settings
  4.9  Remaining screens (accounts, analyze price, SQL explorer, AI, help)

Phase 5 — Background Features (est. 1-2 days)
  5.1  Auto-refresh (server-side, already scaffolded in Phase 1)
  5.2  Notifications (Web Notifications API)
  5.3  Backup/Restore (server-side JSON export/import)
  5.4  CSV Import (server-side parsing)
  5.5  CSV/Image Export (client-side Blob download)

Total estimated: ~12-18 days for full feature parity
```

---

## Key Decisions to Make Before Starting

1. **Hosting**: Run on a home server / VPS / Raspberry Pi? Or deploy to a cloud PaaS (Render, Railway, Fly.io)?
2. **Multi-user**: Single user (no auth) or add basic auth later? Plan assumes single user for now.
3. **Database file location**: Default `server/investhelp.db` or configurable via env var?
4. **Auto-refresh strategy**: `setInterval` (simple) vs `node-cron` (cron expressions, market-hours awareness)?
5. **Logo caching**: Store in SQLite BLOB (matches Android) or save as files in a `logos/` directory?

---

## Appendix A: Theme CSS Variables

Each theme defines Material 3-style tokens as CSS custom properties:

```css
[data-theme="default"] {
  --primary: #2E7D32; --on-primary: #FFFFFF; --primary-container: #A5D6A7;
  --secondary: #66BB6A; --surface: #FFFBFE; --surface-variant: #E7E0EC;
  --on-surface: #1C1B1F; --outline: #79747E; --error: #B3261E; --tertiary: #7D5260;
}
[data-theme="default"][data-dark="true"] {
  --primary: #A5D6A7; --on-primary: #003300; --surface: #1C1B1F; --on-surface: #E6E1E5;
}
/* Repeat for: ocean, sunset, midnight, forest, ruby, arctic, gold, sakura,
   charcoal, lavender, copper, emerald, slate, mocha, navy, tropical,
   wine, desert, nordic, bms, chase, fidelity */
```

Theme switching (client-side only):
```js
function setTheme(name) {
  document.documentElement.dataset.theme = name;
  document.documentElement.dataset.dark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  localStorage.setItem('app_theme', name);
}
```
