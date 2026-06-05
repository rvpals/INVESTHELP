# PWA App Build Guide

Lessons and patterns from building a production PWA (Node.js + Express + vanilla JS). Use this as a reference when starting new PWA projects.

---

## Architecture

### Stack
- **Server:** Node.js + Express
- **Database:** better-sqlite3 (embedded, zero-config, synchronous API)
- **Frontend:** Vanilla JS modules (ES6 import/export), no framework, no build step
- **Styling:** Plain CSS with CSS custom properties for theming

### Folder Structure
```
project/
├── server/
│   ├── index.js          # Express entry point
│   ├── db.js             # SQLite setup, schema, migrations
│   ├── routes/           # One file per resource (REST endpoints)
│   └── services/         # Business logic (external APIs, cron, etc.)
├── public/
│   ├── index.html        # SPA shell (single page)
│   ├── css/
│   │   └── styles.css    # All styles (or split base.css + styles.css)
│   ├── js/
│   │   ├── app.js        # Init, route registration, SW registration
│   │   ├── router.js     # Hash-based SPA router
│   │   ├── api.js        # Fetch wrappers for all server routes
│   │   ├── preferences.js # localStorage wrapper with defaults
│   │   ├── screens/      # One module per screen/page
│   │   ├── components/   # Reusable UI components
│   │   └── utils/        # Formatting, helpers
│   └── sw.js             # Service worker (at root of public/)
├── package.json
└── START_APP.bat          # Windows launcher
```

### Why No Framework
- Zero build step = instant reload during dev, simple deployment
- Smaller payload, no framework overhead for small-to-mid apps
- Full control over rendering and DOM updates
- Trade-off: manual DOM management, no reactivity system — fine for CRUD apps, painful for highly interactive UIs

---

## SPA Router (Hash-Based)

Use hash routing (`#/path`) — no server config needed, works on any static host.

```js
// router.js
const routes = [];

export function route(pattern, handler) {
  // Convert '/item/:ticker' to regex with named groups
  const regex = new RegExp('^' + pattern.replace(/:(\w+)/g, '(?<$1>[^/]+)') + '$');
  routes.push({ regex, handler });
}

export async function handleRoute() {
  const hash = location.hash.slice(1) || '/';
  const app = document.getElementById('app');
  for (const r of routes) {
    const match = hash.match(r.regex);
    if (match) { await r.handler(app, match.groups || {}); return; }
  }
  app.innerHTML = '<p>Page not found</p>';
}

window.addEventListener('hashchange', handleRoute);
```

```js
// app.js — register routes with lazy imports
route('/', async (app) => { const m = await import('./screens/dashboard.js'); await m.render(app); });
route('/item/:ticker', async (app, p) => { const m = await import('./screens/item-detail.js'); await m.render(app, p); });
```

### Gotchas
- Always use `navigate('#/path')` helper instead of raw `location.hash = ...` for consistency
- URL-encode parameters that might contain special characters
- `hashchange` doesn't fire on initial load — call `handleRoute()` once in `init()`
- Lazy `import()` per screen keeps initial load fast

---

## Service Worker

### Strategy: Network-First for Code, Cache-First for Assets

```js
// sw.js
const CACHE_NAME = 'myapp-v1';  // Bump version on deploy

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(['/', '/index.html', '/css/styles.css', '/js/app.js']))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  // Delete old caches
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // NEVER cache API calls
  if (url.pathname.startsWith('/api/')) return;

  // Network-first for JS/CSS/HTML
  if (url.pathname.endsWith('.js') || url.pathname.endsWith('.css') || url.pathname.endsWith('.html')) {
    event.respondWith(
      fetch(event.request)
        .then(resp => {
          if (resp.ok) {
            const clone = resp.clone();
            caches.open(CACHE_NAME).then(c => c.put(event.request, clone));
          }
          return resp;
        })
        .catch(() => caches.match(event.request))
    );
    return;
  }

  // Cache-first for images/fonts
  event.respondWith(
    caches.match(event.request).then(cached => cached || fetch(event.request))
  );
});
```

### Critical Gotchas

1. **SW caches aggressively by default** — if you use cache-first for JS/CSS, users will NEVER see updates until the SW itself changes. Use network-first for code files.

2. **Browser caches the SW file itself** — browsers check for SW updates every 24h max. `registration.update()` forces an immediate check. Bump `CACHE_NAME` version on every deploy.

3. **Stale code is the #1 PWA complaint** — always provide a manual "Refresh App" button:
   ```js
   async function forceRefresh() {
     const keys = await caches.keys();
     await Promise.all(keys.map(k => caches.delete(k)));
     const reg = await navigator.serviceWorker?.getRegistration();
     if (reg) {
       reg.waiting?.postMessage('force-refresh');
       reg.active?.postMessage('force-refresh');
       await reg.update();
     }
     window.location.reload(true);
   }
   ```

4. **Never cache API responses** — return early (don't call `respondWith`) for `/api/` paths. Otherwise you get stale data that's impossible to debug.

5. **SW must be at root of public folder** — `/sw.js`, not `/js/sw.js`. Its scope is its directory and below.

6. **`skipWaiting()` + `clients.claim()`** — without these, the new SW waits until all tabs are closed. With them, it activates immediately.

7. **If you don't need offline support, consider skipping SW entirely** — the caching headaches may not be worth it. A simple "Clear Cache" button that calls `caches.delete()` + `location.reload(true)` works without any SW.

---

## Database (better-sqlite3)

### Setup
```js
const Database = require('better-sqlite3');
const path = require('path');
const db = new Database(path.join(__dirname, '..', 'data', 'app.db'));

db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

// Create tables
db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT UNIQUE
  )
`);

module.exports = db;
```

### Migrations (Column Additions)

SQLite can't alter column types or add constraints — only `ALTER TABLE ADD COLUMN`. Use `PRAGMA table_info` to check if a column exists before adding:

```js
function migrateDb() {
  const cols = db.prepare("PRAGMA table_info('users')").all().map(c => c.name);

  if (!cols.includes('avatar')) {
    db.exec("ALTER TABLE users ADD COLUMN avatar BLOB");
    console.log('Migration: added avatar column');
  }

  if (!cols.includes('created_at')) {
    db.exec("ALTER TABLE users ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0");
    console.log('Migration: added created_at column');
  }
}

migrateDb();
```

### Gotchas
- **Migrations run on every server start** — `PRAGMA table_info` check makes them idempotent. No version tracking needed for simple column additions.
- **For table recreation** (rename column, change PK, add constraints): create new table, copy data, drop old, rename new — all inside a transaction.
- **better-sqlite3 is synchronous** — no `await` needed. This is a feature, not a bug. Simplifies server code enormously.
- **BLOB columns** for binary data (images, files). Pass `Buffer` objects directly.
- **Dates:** store as epoch days (`Math.floor(Date.now() / 86400000)`) or epoch seconds for simple SQL range queries. Avoid date strings.
- **`INSERT OR REPLACE`** for upserts with unique constraints.
- **`INSERT ... ON CONFLICT DO UPDATE`** for partial upserts (update specific columns on conflict).
- **Unique constraints** prevent duplicate imports: `UNIQUE(date, action, ticker, amount)`.
- **CASCADE deletes:** `FOREIGN KEY (parentId) REFERENCES parent(id) ON DELETE CASCADE` — requires `PRAGMA foreign_keys = ON`.

---

## API Layer

### Server Routes Pattern
```js
// routes/items.js
const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/', (req, res) => {
  const items = db.prepare('SELECT * FROM items ORDER BY name').all();
  res.json(items);
});

router.post('/', (req, res) => {
  const { name, value } = req.body;
  const result = db.prepare('INSERT INTO items (name, value) VALUES (?, ?)').run(name, value);
  res.json({ id: result.lastInsertRowid });
});

module.exports = router;
```

```js
// index.js
app.use(express.json());
app.use('/api/items', require('./routes/items'));
```

### Frontend API Client
```js
// api.js — thin wrappers around fetch
const BASE = '';  // Same origin

async function get(path) {
  const res = await fetch(BASE + path);
  if (!res.ok) throw new Error(`GET ${path}: ${res.status}`);
  return res.json();
}

async function post(path, body) {
  const res = await fetch(BASE + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`POST ${path}: ${res.status}`);
  return res.json();
}

// Export named groups per resource
export const items = {
  list: () => get('/api/items'),
  get: (id) => get(`/api/items/${id}`),
  create: (data) => post('/api/items', data),
  update: (id, data) => post(`/api/items/${id}`, { ...data, _method: 'PUT' }),
  delete: (id) => post(`/api/items/${id}/delete`),
};
```

### Gotchas
- **Always `express.json()` middleware** before routes — without it, `req.body` is undefined.
- **Static files:** `app.use(express.static('public'))` — serves `public/` at root.
- **Error responses:** include the status code and path in error messages for debugging: `throw new Error(\`GET ${path}: ${res.status}\`)`.
- **CORS:** not needed when frontend and API are same origin (same Express server). Only needed for external API calls — proxy those through your server.

---

## Theming (Dark/Light Mode)

### CSS Custom Properties
```css
:root {
  --bg: #f5f5f5;
  --surface: #ffffff;
  --surface-variant: #f0f0f0;
  --text: #1C1B1F;
  --text-muted: #666;
  --primary: #6750A4;
  --outline: #ccc;
}

[data-theme="dark"] {
  --bg: #1C1B1F;
  --surface: #2B2930;
  --surface-variant: #363240;
  --text: #E6E1E5;
  --text-muted: #aaa;
  --primary: #D0BCFF;
  --outline: #555;
}

body { background: var(--bg); color: var(--text); }
.card { background: var(--surface); }
```

### Theme Toggle
```js
// preferences.js
export function applyTheme() {
  const theme = localStorage.getItem('theme') || 'system';
  if (theme === 'system') {
    const dark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
  } else {
    document.documentElement.setAttribute('data-theme', theme);
  }
}
```

### Gotchas
- Use `data-theme` attribute on `<html>`, not a class — avoids specificity conflicts.
- Default to `system` preference, not hardcoded light/dark.
- Canvas charts don't inherit CSS variables — read them with `getComputedStyle()`:
  ```js
  const style = getComputedStyle(document.documentElement);
  const textColor = style.getPropertyValue('--text').trim();
  ctx.fillStyle = textColor;
  ```

---

## UI Components (Vanilla JS)

### Screen Pattern
Each screen exports a `render(container, params)` function:

```js
// screens/items.js
import { items } from '../api.js';

export async function render(container) {
  container.innerHTML = `<div class="screen">
    <h2>Items</h2>
    <div id="items-list"></div>
  </div>`;

  const list = await items.list();
  const el = document.getElementById('items-list');
  el.innerHTML = list.map(item => `
    <div class="card clickable" data-id="${item.id}">
      <div class="text-bold">${escapeHtml(item.name)}</div>
      <div class="text-muted">${item.value}</div>
    </div>
  `).join('');

  el.querySelectorAll('[data-id]').forEach(card => {
    card.addEventListener('click', () => navigate(`#/item/${card.dataset.id}`));
  });
}
```

### Dialog Pattern
Use a shared overlay element in `index.html`:

```html
<div id="dialog-overlay" class="dialog-overlay hidden"></div>
```

```js
function showDialog(title, bodyHtml, actions) {
  const overlay = document.getElementById('dialog-overlay');
  overlay.className = 'dialog-overlay';
  overlay.innerHTML = `
    <div class="dialog">
      <div class="dialog-title">${title}</div>
      <div class="dialog-body">${bodyHtml}</div>
      <div class="dialog-actions">${actions}</div>
    </div>
  `;
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) overlay.className = 'dialog-overlay hidden';
  });
}
```

### Collapsible Card with Pin Persistence
```js
export function collapsibleCard(id, title, contentHtml, defaultExpanded = false) {
  const pinned = localStorage.getItem(`pin_${id}`) === 'true';
  const expanded = pinned || defaultExpanded;
  return `
    <div class="collapsible-card" data-card-id="${id}">
      <div class="card-header">
        <span class="card-title">${title}</span>
        <button class="pin-btn ${pinned ? 'pinned' : ''}" data-pin="${id}" title="Pin">&#128204;</button>
        <button class="collapse-btn" data-collapse="${id}">${expanded ? '&#9650;' : '&#9660;'}</button>
      </div>
      <div class="card-content ${expanded ? '' : 'hidden'}" data-content="${id}">${contentHtml}</div>
    </div>
  `;
}
```

### Gotchas
- **XSS:** Always escape user-generated content in templates. Create an `escapeHtml()` utility:
  ```js
  function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }
  ```
- **Event listeners on dynamic content:** attach after setting `innerHTML`, not before. Or use event delegation on a parent element.
- **`innerHTML` destroys existing event listeners** — if you re-render a section, re-attach listeners.
- **Canvas charts need explicit width/height attributes**, not just CSS. Set `canvas.width = canvas.offsetWidth` before drawing, or they'll be blurry.
- **`requestAnimationFrame` before drawing charts** — ensures the DOM has laid out and `offsetWidth`/`offsetHeight` return correct values:
  ```js
  requestAnimationFrame(() => {
    const canvas = document.getElementById('chart');
    canvas.width = canvas.offsetWidth || 600;
    canvas.height = canvas.offsetHeight || 300;
    drawChart(canvas);
  });
  ```

---

## localStorage Preferences

```js
const DEFAULTS = {
  theme: 'system',
  items_per_page: 20,
  market_indices: ['NASDAQ', 'SP500', 'DOW', 'GOLD'],
};

export function getPref(key) {
  const val = localStorage.getItem(`pref_${key}`);
  if (val === null) return DEFAULTS[key];
  try { return JSON.parse(val); } catch { return val; }
}

export function setPref(key, value) {
  localStorage.setItem(`pref_${key}`, typeof value === 'string' ? value : JSON.stringify(value));
}
```

### Gotchas
- Prefix keys (`pref_`, `pin_`) to avoid collisions with other libraries.
- `localStorage` only stores strings — always `JSON.parse`/`JSON.stringify` for objects/arrays.
- `localStorage` is synchronous and blocks the main thread — fine for small reads, don't store large blobs.

---

## External API Calls (e.g., Yahoo Finance)

### Always Proxy Through Your Server
Frontend `fetch()` to third-party APIs will fail due to CORS. Proxy everything:

```js
// Server: routes/yahoo.js
router.get('/quote/:ticker', async (req, res) => {
  try {
    const data = await yahooService.fetchQuote(req.params.ticker);
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Frontend: api.js
export const yahoo = {
  quote: (ticker) => get(`/api/yahoo/quote/${ticker}`),
};
```

### Gotchas
- **Some APIs need auth tokens (cookies, crumbs)** — fetch these server-side and cache them. Build a multi-tier fallback: auth'd request → unauth'd request → alternative endpoint.
- **Rate limiting:** add delays between batch requests. Yahoo Finance will throttle/block rapid-fire calls.
- **Undefined fields:** external APIs return inconsistent shapes. Always default: `data.volume || 0`, `data.name || ''`.
- **Don't call `.toLocaleString()` or `.toFixed()` on potentially undefined values** — this is the #1 crash source. Always guard with `|| 0` for numbers, `|| ''` for strings.

---

## Backup & Restore

### Portable JSON Format
Use a versioned JSON format that can be shared across platforms (web, mobile):

```js
const backup = {
  version: 5,
  accounts: [...],
  items: [...],
  transactions: [...],
  exportedAt: new Date().toISOString()
};
```

### Gotchas
- **Version the backup format** — include a `version` field. Write backward-compatible import logic for each version.
- **Use `INSERT OR REPLACE`** for restore — handles both fresh import and re-import of existing records.
- **Date fields:** standardize on epoch days or epoch seconds in backups, not date strings. Different platforms parse date strings differently.
- **Blob fields (images):** export as base64 strings in JSON, decode on import.
- **Auto-backup:** implement cooldown guards (e.g., 30-minute minimum between auto-backups) to prevent excessive writes.
- **Prune old backups:** keep the last N backup files and delete older ones.

---

## CSV Import

### Parsing
```js
function parseCsv(text) {
  const lines = text.split('\n');
  const headers = parseRow(lines[0]);
  return lines.slice(1)
    .filter(line => line.trim() && !line.startsWith('FOOTNOTE'))
    .map(line => {
      const values = parseRow(line);
      const obj = {};
      headers.forEach((h, i) => obj[h.trim()] = values[i]?.trim() || '');
      return obj;
    });
}
```

### Gotchas
- **Numeric fields with commas:** brokerages format numbers like `"92,150.62"`. Strip commas before parsing: `parseFloat(val.replace(/,/g, ''))`.
- **Quoted fields with commas inside:** a proper CSV parser must handle `"Smith, John"` as one field. Don't just `.split(',')`.
- **Column name aliases:** different brokerages use different names. Build an alias map: `{ 'Symbol': 'ticker', 'Price': 'currentPrice', 'Description': 'name' }`.
- **Non-data rows:** filter out blank lines, footnotes, totals rows that brokerages append.
- **Duplicate prevention:** use unique constraints on meaningful column combinations (date + action + ticker + amount). Use `INSERT OR IGNORE` to skip duplicates silently.

---

## Deployment Gotchas

### Process Management
- Use `pm2` or a systemd service for production — `node index.js` dies on crash.
- Start script should kill existing process first: `pkill -f 'node.*index.js' || true && node server/index.js`

### Database Location
- Store the `.db` file outside the git repo or in a gitignored `data/` folder. A `git pull` should never overwrite your database.
- Back up the database before destructive operations (hard reset, reinstall).

### Full Reset Script Pattern
```bash
#!/bin/bash
# Backup DB, reset code, restore DB, restart
cp data/app.db data/app.db.bak
git fetch origin
git reset --hard origin/master
git clean -fd
cp data/app.db.bak data/app.db
npm install
# restart server
```

### Port Conflicts (Windows)
```bash
# Kill existing node processes (double-slash in git bash)
taskkill //F //IM node.exe 2>/dev/null
```

### Static Files Not Updating
Even without a service worker, browsers cache aggressively. Solutions:
1. Cache-busting query strings: `<script src="app.js?v=1.2">`
2. Set `Cache-Control` headers: `res.set('Cache-Control', 'no-cache')` for HTML
3. Service worker with network-first strategy (recommended)
4. Always provide a manual "Refresh App" button as escape hatch

---

## Common CSS Patterns

### Data Table with Grid Lines
```css
.data-table {
  width: 100%;
  border-collapse: collapse;
}
.data-table th, .data-table td {
  padding: 8px;
  border: 1px solid var(--outline);
  text-align: left;
}
.data-table tr:nth-child(even) {
  background: color-mix(in srgb, var(--surface-variant) 30%, transparent);
}
```

### Tab Bar
```css
.tab-bar {
  display: flex;
  border-bottom: 2px solid var(--outline);
}
.tab {
  padding: 8px 16px;
  border: 1px solid var(--outline);
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  background: transparent;
  cursor: pointer;
}
.tab:hover { background: color-mix(in srgb, var(--primary) 10%, transparent); }
.tab.active { background: var(--surface); font-weight: bold; border-bottom: 2px solid var(--primary); }
```

### Gain/Loss Colors
```css
.text-green { color: #2E7D32; }
.text-red { color: #C62828; }
/* In dark mode, use lighter variants */
[data-theme="dark"] .text-green { color: #66BB6A; }
[data-theme="dark"] .text-red { color: #EF5350; }
```

---

## Performance Tips

- **Lazy-load screens** via dynamic `import()` — only load code for the current route.
- **Debounce search inputs** — don't fire API calls on every keystroke.
- **Virtual scrolling** for large lists (100+ items) — render only visible rows.
- **Batch DOM updates** — build HTML as a string, set `innerHTML` once. Don't call `appendChild` in a loop.
- **Canvas for charts** — DOM-based charts (SVG, div-based) struggle above 100 data points. Canvas handles thousands.

---

## Checklist for New PWA Project

1. [ ] Set up Express server with `express.json()` and `express.static('public')`
2. [ ] Create `db.js` with better-sqlite3, schema, and migration function
3. [ ] Create `index.html` SPA shell with `#app`, `#top-bar`, `#dialog-overlay`, `#bottom-nav`
4. [ ] Implement hash-based router (`router.js`)
5. [ ] Create API client (`api.js`) with fetch wrappers
6. [ ] Set up CSS custom properties for theming
7. [ ] Build first screen to verify the full stack works
8. [ ] Add service worker with network-first strategy
9. [ ] Add "Refresh App" button in About/Settings
10. [ ] Add backup/restore (versioned JSON)
11. [ ] Add error handling for external API calls (default undefined fields)
12. [ ] Test on target deployment environment (NAS, VPS, etc.)
