import { route, handleRoute } from './router.js';
import { applyTheme } from './preferences.js';
import { renderTopBar } from './components/top-bar.js';
import { renderBottomNav, updateBottomNav } from './components/bottom-nav.js';
import { auth } from './api.js';
import { renderLogin } from './screens/login.js';

// Register service worker
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js');
}

// Apply theme
applyTheme();

// Register all routes
route('/', async (app) => { const m = await import('./screens/dashboard.js'); await m.render(app); });
route('/positions', async (app) => { const m = await import('./screens/positions.js'); await m.render(app); });
route('/item/:ticker', async (app, p) => { const m = await import('./screens/item-detail.js'); await m.render(app, p); });
route('/item-form', async (app) => { const m = await import('./screens/item-form.js'); await m.render(app); });
route('/item-form/:ticker', async (app, p) => { const m = await import('./screens/item-form.js'); await m.render(app, p); });
route('/transactions', async (app) => { const m = await import('./screens/transaction-list.js'); await m.render(app); });
route('/transaction-form', async (app) => { const m = await import('./screens/transaction-form.js'); await m.render(app); });
route('/transaction-form/:id', async (app, p) => { const m = await import('./screens/transaction-form.js'); await m.render(app, p); });
route('/analyze-price/:ticker', async (app, p) => { const m = await import('./screens/analyze-price.js'); await m.render(app, p); });
route('/simulation/:ticker/:shares', async (app, p) => { const m = await import('./screens/simulation.js'); await m.render(app, p); });
route('/accounts', async (app) => { const m = await import('./screens/account-list.js'); await m.render(app); });
route('/account/:id', async (app, p) => { const m = await import('./screens/account-detail.js'); await m.render(app, p); });
route('/account-form', async (app) => { const m = await import('./screens/account-form.js'); await m.render(app); });
route('/account-form/:id', async (app, p) => { const m = await import('./screens/account-form.js'); await m.render(app, p); });
route('/performance', async (app) => { const m = await import('./screens/performance.js'); await m.render(app); });
route('/watchlist', async (app) => { const m = await import('./screens/watchlist.js'); await m.render(app); });
route('/settings', async (app) => { const m = await import('./screens/settings.js'); await m.render(app); });
route('/sql-explorer', async (app) => { const m = await import('./screens/sql-explorer.js'); await m.render(app); });
route('/ai-ticker/:ticker', async (app, p) => { const m = await import('./screens/ai-ticker.js'); await m.render(app, p); });
route('/next-day-actions', async (app) => { const m = await import('./screens/next-day-actions.js'); await m.render(app); });
route('/help', async (app) => { const m = await import('./screens/help.js'); await m.render(app); });
route('/volatility/:ticker/:shares', async (app, p) => { const m = await import('./screens/volatility.js'); await m.render(app, p); });
route('/volatility-analysis', async (app) => { const m = await import('./screens/volatility-analysis.js'); await m.render(app); });
route('/correlation', async (app) => { const m = await import('./screens/correlation.js'); await m.render(app); });
route('/sharpe-ratio', async (app) => { const m = await import('./screens/sharpe-ratio.js'); await m.render(app); });

// Update bottom nav on route change
window.addEventListener('hashchange', updateBottomNav);

// App container used when showing the login screen
const appContainer = document.getElementById('app');

async function initApp() {
  await renderTopBar();
  renderBottomNav();
  await handleRoute();
}

async function boot() {
  const user = await auth.me();
  if (!user) {
    // Not authenticated — show login screen only, no top/bottom bar
    document.getElementById('top-bar').style.display = 'none';
    document.getElementById('bottom-nav').style.display = 'none';
    await renderLogin(appContainer, async () => {
      // Login succeeded — restore chrome and launch app
      document.getElementById('top-bar').style.display = '';
      document.getElementById('bottom-nav').style.display = '';
      appContainer.innerHTML = '';
      await initApp();
    });
    return;
  }
  await initApp();
}

boot();
