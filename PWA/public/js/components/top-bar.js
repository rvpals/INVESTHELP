import { positions, refresh } from '../api.js';
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

function toggleMenu() {
  if (menuOpen) { closeMenu(); return; }
  menuOpen = true;
  const backdrop = document.createElement('div');
  backdrop.className = 'menu-backdrop';
  backdrop.addEventListener('click', closeMenu);
  document.body.appendChild(backdrop);

  const menu = document.createElement('div');
  menu.className = 'hamburger-menu open';
  menu.id = 'hamburger-menu';
  menu.innerHTML = `
    <div class="menu-header">InvestHelp</div>
    <button class="menu-item" data-route="#/accounts">&#128179; Accounts</button>
    <button class="menu-item" data-route="#/performance">&#128200; Performance</button>
    <button class="menu-item" data-route="#/simulation/SPY/1">&#128202; Simulation</button>
    <button class="menu-item" data-route="#/next-day-actions">&#128197; Next Day Actions</button>
    <div class="menu-divider"></div>
    <button class="menu-item" data-route="#/settings">&#9881; Settings</button>
    <button class="menu-item" data-route="#/sql-explorer">&#128451; SQL Explorer</button>
    <button class="menu-item" data-route="#/help">&#10067; Help</button>
    <div class="menu-divider"></div>
    <button class="menu-item" id="about-btn">&#9432; About</button>
  `;
  document.body.appendChild(menu);

  menu.querySelectorAll('[data-route]').forEach(btn => {
    btn.addEventListener('click', () => { closeMenu(); navigate(btn.dataset.route); });
  });
  menu.querySelector('#about-btn').addEventListener('click', () => { closeMenu(); showAbout(); });
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
      <p>InvestHelp PWA v1.0</p>
      <p class="text-sm text-muted mt-8">Investment tracking progressive web app.</p>
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
