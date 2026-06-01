const DEFAULTS = {
  app_theme: 'default',
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
  const val = localStorage.getItem('ih_' + key);
  if (val === null) return DEFAULTS[key];
  try { return JSON.parse(val); } catch { return val; }
}

export function setPref(key, value) {
  localStorage.setItem('ih_' + key, JSON.stringify(value));
  window.dispatchEvent(new CustomEvent('pref-changed', { detail: { key, value } }));
}

export function applyTheme() {
  const theme = getPref('app_theme') || 'default';
  document.documentElement.dataset.theme = theme;
  document.documentElement.dataset.dark = window.matchMedia('(prefers-color-scheme: dark)').matches;
}

window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => applyTheme());
