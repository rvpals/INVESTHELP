import { positions, refresh, auth } from '../api.js';
import { navigate } from '../router.js';
import { formatCurrency, formatPercent, formatSignedCurrency, gainLossClass } from '../utils/format.js';
import { getPref, setPref } from '../preferences.js';

let menuOpen = false;

export async function renderTopBar() {
  const el = document.getElementById('top-bar');
  el.innerHTML = `
    <div class="top-bar">
      <button class="portfolio-btn" id="portfolio-btn">
        <div class="portfolio-value" id="portfolio-value">$0.00</div>
        <div class="portfolio-change" id="portfolio-change"></div>
      </button>
      <span id="refresh-spinner" class="hidden"></span>
      <button class="icon-btn" id="search-btn" title="Search Ticker">&#128269;</button>
      <button class="icon-btn" id="watchlist-btn" title="Watch Lists">&#9733;</button>
      <button class="icon-btn" id="menu-btn" title="Menu">&#9776;</button>
    </div>
  `;

  let isRefreshing = false;
  document.getElementById('portfolio-btn').addEventListener('click', async () => {
    if (isRefreshing) return;
    isRefreshing = true;
    showRefreshSpinner();
    const statusEl = document.getElementById('refresh-status');
    try {
      statusEl.textContent = 'Refreshing prices...';
      statusEl.classList.remove('hidden');
      const result = await refresh.all();
      setPref('last_refreshed_at', Date.now());
      const okCount = result.results?.filter(r => r.ok).length || 0;
      const failCount = result.results?.filter(r => !r.ok).length || 0;
      statusEl.textContent = `Refreshed ${okCount} tickers${failCount ? `, ${failCount} failed` : ''}`;
      setTimeout(() => statusEl.classList.add('hidden'), 3000);
    } catch (err) {
      statusEl.textContent = 'Refresh failed: ' + (err.message || 'unknown error');
      setTimeout(() => statusEl.classList.add('hidden'), 4000);
    } finally {
      hideRefreshSpinner();
      isRefreshing = false;
      await updatePortfolioValue();
    }
    navigate('#/');
  });

  document.getElementById('search-btn').addEventListener('click', () => showTickerSearch());
  document.getElementById('watchlist-btn').addEventListener('click', () => navigate('#/watchlist'));
  document.getElementById('menu-btn').addEventListener('click', () => toggleMenu());

  await updatePortfolioValue();
}

export async function updatePortfolioValue() {
  try {
    const s = await positions.summary();
    document.getElementById('portfolio-value').textContent = formatCurrency(s.totalValue);
    const changeEl = document.getElementById('portfolio-change');
    if (s.dayGainLoss !== 0) {
      changeEl.innerHTML = `<span class="${s.dayGainLoss >= 0 ? '' : 'text-red'}">${formatSignedCurrency(s.dayGainLoss)} (${formatPercent(s.dayPercent)})</span>`;
    } else {
      changeEl.textContent = '';
    }
  } catch {}
}

export function showRefreshSpinner() {
  const el = document.getElementById('refresh-spinner');
  el.className = 'spinner-sm';
}

export function hideRefreshSpinner() {
  const el = document.getElementById('refresh-spinner');
  el.className = 'hidden';
}

function showTickerSearch() {
  const overlay = document.getElementById('dialog-overlay');
  overlay.className = 'dialog-overlay';
  overlay.innerHTML = `
    <div class="dialog">
      <div class="dialog-title">Search Ticker</div>
      <div class="form-group">
        <input type="text" class="input" id="search-ticker-input" placeholder="Enter ticker (e.g. AAPL)" style="text-transform:uppercase" autofocus>
      </div>
      <div class="dialog-actions">
        <button class="btn btn-outline" id="search-cancel">Cancel</button>
        <button class="btn btn-primary" id="search-go">Go</button>
      </div>
    </div>
  `;

  const input = document.getElementById('search-ticker-input');
  const go = () => {
    const ticker = input.value.toUpperCase().trim();
    if (!ticker) return;
    overlay.className = 'dialog-overlay hidden';
    navigate(`#/item/${ticker}`);
  };

  document.getElementById('search-go').addEventListener('click', go);
  document.getElementById('search-cancel').addEventListener('click', () => { overlay.className = 'dialog-overlay hidden'; });
  input.addEventListener('keydown', (e) => { if (e.key === 'Enter') go(); });
  overlay.addEventListener('click', (e) => { if (e.target === overlay) overlay.className = 'dialog-overlay hidden'; });
  setTimeout(() => input.focus(), 100);
}

async function toggleMenu() {
  if (menuOpen) { closeMenu(); return; }
  menuOpen = true;
  const backdrop = document.createElement('div');
  backdrop.className = 'menu-backdrop';
  backdrop.addEventListener('click', closeMenu);
  document.body.appendChild(backdrop);

  const menu = document.createElement('div');
  menu.className = 'hamburger-menu open';
  menu.id = 'hamburger-menu';
  // Fetch current user to display in menu header
  const currentUser = await auth.me().catch(() => null);
  const userLine = currentUser
    ? `<div style="font-size:11px;color:var(--text-muted);padding:0 16px 8px;margin-top:-4px">&#128100; ${currentUser.username}</div>`
    : '';

  menu.innerHTML = `
    <div class="menu-header">InvestHelp</div>
    ${userLine}
    <button class="menu-item" data-route="#/accounts">&#128179; Accounts</button>
    <button class="menu-item" data-route="#/performance">&#128200; Performance</button>
    <button class="menu-item" data-route="#/simulation/SPY/1">&#128202; Simulation</button>
    <button class="menu-item" data-route="#/next-day-actions">&#128197; Next Day Actions</button>
    <button class="menu-item" data-route="#/volatility-analysis">&#128200; Volatility Analysis</button>
    <button class="menu-item" data-route="#/correlation">&#9781; Correlation Matrix</button>
    <button class="menu-item" data-route="#/sharpe-ratio">&#128202; Sharpe Ratio</button>
    <div class="menu-divider"></div>
    <button class="menu-item" data-route="#/settings">&#9881; Settings</button>
    <button class="menu-item" data-route="#/sql-explorer">&#128451; SQL Explorer</button>
    <button class="menu-item" data-route="#/help">&#10067; Help</button>
    <div class="menu-divider"></div>
    <button class="menu-item" id="about-btn">&#9432; About</button>
    <div class="menu-divider"></div>
    <button class="menu-item" id="logout-btn" style="color:var(--error,#b3261e)">&#128275; Sign Out</button>
  `;
  document.body.appendChild(menu);

  menu.querySelectorAll('[data-route]').forEach(btn => {
    btn.addEventListener('click', () => { closeMenu(); navigate(btn.dataset.route); });
  });
  menu.querySelector('#about-btn').addEventListener('click', () => { closeMenu(); showAbout(); });
  menu.querySelector('#logout-btn').addEventListener('click', async () => {
    closeMenu();
    try { await auth.logout(); } catch (_) {}
    // Reload page — app.js boot() will detect 401 and show the login screen
    window.location.reload();
  });
}

function closeMenu() {
  menuOpen = false;
  document.querySelector('.menu-backdrop')?.remove();
  document.getElementById('hamburger-menu')?.remove();
}

function showAbout() {
  const overlay = document.getElementById('dialog-overlay');
  overlay.className = 'dialog-overlay';
  overlay.innerHTML = `
    <div class="dialog">
      <div class="dialog-title">About InvestHelp</div>
      <p>InvestHelp PWA <strong>v1.69</strong></p>
      <p class="text-sm text-muted mt-8">Investment tracking progressive web app.</p>
      <hr style="border-color:var(--border-color);margin:12px 0 8px">
      <p class="text-sm" style="font-weight:600">What's New</p>
      <ul class="text-sm text-muted" style="padding-left:20px;margin-top:4px">
        <li><strong>v1.69</strong> — Sharpe Ratio: SQLite cache (instant load on open, cached-at banner); 5Y/10Y lookback options; "About Sharpe Ratio" card (formula, components, interpretation); "Calculation Detail" card (inputs, per-ticker breakdown, step-by-step)</li>
        <li><strong>v1.67</strong> — Sharpe Ratio analytics screen (Android + PWA): portfolio risk-adjusted return metric with configurable risk-free rate (default 5%), lookback period (6m/1y/2y), Canvas daily returns chart with green/red fills</li>
        <li><strong>v1.66</strong> — Item Detail: new inline Correlation card (peer correlations + SPY sensitivity) and Volatility card (52w range bar, annualized vol %, scale legend); removed standalone Volatility toolbar button</li>
        <li><strong>v1.64</strong> — Correlation Matrix: sticky column header, row separator bands, column ticker hint on each cell; backup import now surfaces real errors instead of silent 0-count</li>
        <li><strong>v1.62</strong> — Correlation Matrix: pairwise Pearson correlation of 1-year daily returns, filter toggle (≥0.75 highlight), market sensitivity vs SPY, portfolio insights, PNG download</li>
        <li><strong>v1.61</strong> — Volatility Analysis caching: results persist to DB, "Last calculated on" banner, Refresh clears cache</li>
        <li><strong>v1.60</strong> — Volatility Analysis screen: all Stock/ETF positions grouped by Low/Moderate/High/Very High volatility with live progress bar</li>
        <li><strong>v1.51</strong> — 52-Week Volatility per ticker, Dashboard Watch List live price columns (Chg%, Chg$, Added$)</li>
        <li>Next Day Actions scanner: STOP LOSS / TRIM PROFITS / REBALANCE / STRONG BUY / HOLD signals</li>
        <li>Positions tabs: flat layout with STOCK/ETF/Analysis/Dividend (icon + equal width)</li>
        <li>Dividend tab: total annual income, Stock/ETF exploding pie charts, sortable tables</li>
        <li>Generic backup/restore (v6) — auto-discovers all tables via sqlite_master</li>
      </ul>
      <div class="mt-16">
        <button class="btn btn-outline w-full" id="refresh-app-btn">&#128260; Refresh App</button>
        <p class="text-xs text-muted mt-4 text-center">Clears cached files and reloads with latest code</p>
      </div>
      <div class="dialog-actions">
        <button class="btn btn-primary" id="close-about">Close</button>
      </div>
    </div>
  `;
  document.getElementById('close-about').addEventListener('click', () => { overlay.className = 'dialog-overlay hidden'; });
  document.getElementById('refresh-app-btn').addEventListener('click', async () => {
    const btn = document.getElementById('refresh-app-btn');
    btn.disabled = true;
    btn.textContent = 'Refreshing...';
    try {
      // Clear all caches
      const keys = await caches.keys();
      await Promise.all(keys.map(k => caches.delete(k)));
      // Tell service worker to skip waiting
      const reg = await navigator.serviceWorker?.getRegistration();
      if (reg) {
        reg.waiting?.postMessage('force-refresh');
        reg.active?.postMessage('force-refresh');
        await reg.update();
      }
    } catch {}
    // Hard reload
    window.location.reload(true);
  });
}
