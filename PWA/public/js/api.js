async function get(path) {
  const r = await fetch(path);
  if (!r.ok) throw new Error(`GET ${path}: ${r.status}`);
  return r.json();
}

async function post(path, body) {
  const r = await fetch(path, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
  if (!r.ok) { const e = await r.json().catch(() => ({})); throw new Error(e.error || `POST ${path}: ${r.status}`); }
  return r.json();
}

async function put(path, body) {
  const r = await fetch(path, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
  if (!r.ok) throw new Error(`PUT ${path}: ${r.status}`);
  return r.json();
}

async function del(path) {
  const r = await fetch(path, { method: 'DELETE' });
  if (!r.ok) throw new Error(`DELETE ${path}: ${r.status}`);
  return r.json();
}

export const positions = {
  list:    ()        => get('/api/positions'),
  get:     (ticker)  => get(`/api/positions/${encodeURIComponent(ticker)}`),
  summary: ()        => get('/api/positions/summary'),
  upsert:  (data)    => post('/api/positions', data),
  update:  (ticker, d) => put(`/api/positions/${encodeURIComponent(ticker)}`, d),
  delete:  (ticker)  => del(`/api/positions/${encodeURIComponent(ticker)}`),
  logoUrl: (ticker)  => `/api/positions/${encodeURIComponent(ticker)}/logo`,
};

export const transactions = {
  list:       (ticker) => get(`/api/transactions${ticker ? '?ticker=' + encodeURIComponent(ticker) : ''}`),
  get:        (id)     => get(`/api/transactions/${id}`),
  create:     (data)   => post('/api/transactions', data),
  update:     (id, d)  => put(`/api/transactions/${id}`, d),
  delete:     (id)     => del(`/api/transactions/${id}`),
  bulkDelete: (ids)    => post('/api/transactions/bulk-delete', { ids }),
  stats:      (ticker, params) => get(`/api/transactions/stats/${encodeURIComponent(ticker)}?${new URLSearchParams(params)}`),
};

export const accounts = {
  list:   ()       => get('/api/accounts'),
  get:    (id)     => get(`/api/accounts/${id}`),
  create: (data)   => post('/api/accounts', data),
  update: (id, d)  => put(`/api/accounts/${id}`, d),
  delete: (id)     => del(`/api/accounts/${id}`),
};

export const performance = {
  list:   (accountId) => get(`/api/performance${accountId ? '?accountId=' + accountId : ''}`),
  create: (data)      => post('/api/performance', data),
  update: (id, d)     => put(`/api/performance/${id}`, d),
  delete: (id)        => del(`/api/performance/${id}`),
};

export const watchlists = {
  list:       ()         => get('/api/watchlists'),
  create:     (data)     => post('/api/watchlists', data),
  update:     (id, d)    => put(`/api/watchlists/${id}`, d),
  delete:     (id)       => del(`/api/watchlists/${id}`),
  items:      (id)       => get(`/api/watchlists/${id}/items`),
  addItem:    (id, data) => post(`/api/watchlists/${id}/items`, data),
  updateItem: (itemId, d) => put(`/api/watchlists/items/${itemId}`, d),
  deleteItem: (itemId)   => del(`/api/watchlists/items/${itemId}`),
};

export const changeHistory = {
  list:   () => get('/api/change-history'),
  upsert: (data) => post('/api/change-history', data),
  delete: (id) => del(`/api/change-history/${id}`),
};

export const definitions = {
  list:   () => get('/api/definitions'),
  create: (data) => post('/api/definitions', data),
  update: (id, d) => put(`/api/definitions/${id}`, d),
  delete: (id) => del(`/api/definitions/${id}`),
};

export const csvMappings = {
  get:          (type)   => get(`/api/csv-mappings/${type}`),
  save:         (type, d) => put(`/api/csv-mappings/${type}`, d),
  listNamed:    (type)   => get(`/api/csv-mappings/named/${type}`),
  saveNamed:    (data)   => post('/api/csv-mappings/named', data),
  deleteNamed:  (id)     => del(`/api/csv-mappings/named/${id}`),
};

export const sqlLibrary = {
  list:   () => get('/api/sql-library'),
  create: (data) => post('/api/sql-library', data),
  delete: (id) => del(`/api/sql-library/${id}`),
};

export const aiLibrary = {
  list:   () => get('/api/ai-library'),
  create: (data) => post('/api/ai-library', data),
  delete: (id) => del(`/api/ai-library/${id}`),
};

export const yahoo = {
  quote:         (ticker) => get(`/api/yahoo/quote/${encodeURIComponent(ticker)}`),
  history:       (ticker, range, interval) => get(`/api/yahoo/history/${encodeURIComponent(ticker)}?range=${range}&interval=${interval}`),
  historyPeriod: (ticker, p1, p2, interval) => get(`/api/yahoo/history-period/${encodeURIComponent(ticker)}?period1=${p1}&period2=${p2}&interval=${interval}`),
  analysis:      (ticker) => get(`/api/yahoo/analysis/${encodeURIComponent(ticker)}`),
  news:          (ticker, count) => get(`/api/yahoo/news/${encodeURIComponent(ticker)}?count=${count || 5}`),
  scan:          (ticker) => get(`/api/yahoo/scan/${encodeURIComponent(ticker)}`),
  report:        (ticker) => get(`/api/yahoo/report/${encodeURIComponent(ticker)}`),
  events:        (ticker) => get(`/api/yahoo/events/${encodeURIComponent(ticker)}`),
};

export const refresh = {
  all:    () => post('/api/refresh', {}),
  status: () => get('/api/refresh/status'),
};

export const backup = {
  exportUrl: '/api/backup/export',
  import:    async (file) => {
    const form = new FormData();
    form.append('file', file);
    const r = await fetch('/api/backup/import', { method: 'POST', body: form });
    let data;
    try { data = await r.json(); } catch {
      throw new Error(`Server returned HTTP ${r.status} — body was not JSON (possible reverse-proxy size limit)`);
    }
    if (!r.ok) throw new Error(data.error || `Import failed (HTTP ${r.status})`);
    return data;
  },
  list: () => get('/api/backup/list'),
};

export const sql = {
  execute: (query) => post('/api/sql/execute', { sql: query }),
  tables:  () => get('/api/sql/tables'),
};

export const settings = {
  getAll: () => get('/api/settings'),
  set:    (key, value) => put(`/api/settings/${key}`, { value }),
};

export const volatility = {
  get:        (ticker, force = false) =>
    get(`/api/volatility/${encodeURIComponent(ticker)}${force ? '?force=true' : ''}`),
  getCache:   () => get('/api/volatility/cache/all'),
  saveCache:  (items) => post('/api/volatility/cache/bulk', { items }),
  clearCache: () => del('/api/volatility/cache/all'),
};

export const correlation = {
  getCache:   () => get('/api/correlation/cache'),
  compute:    () => post('/api/correlation/compute', {}),
  clearCache: () => del('/api/correlation/cache'),
};

export const sharpe = {
  compute:       (riskFreeRate, lookbackDays) =>
    post('/api/sharpe/compute', { riskFreeRate, lookbackDays }),
  getCached:     () => get('/api/sharpe/cached'),
  computeRolling: (riskFreeRate) =>
    post('/api/sharpe/rolling', { riskFreeRate }),
};

// Auth helpers — these calls must NOT throw on 401 so callers can handle it
export const auth = {
  me: async () => {
    const r = await fetch('/api/auth/me');
    if (r.status === 401) return null;
    if (!r.ok) throw new Error(`GET /api/auth/me: ${r.status}`);
    return r.json();
  },
  login: (username, password) => post('/api/auth/login', { username, password }),
  logout: async () => {
    const r = await fetch('/api/auth/logout', { method: 'POST' });
    if (!r.ok) throw new Error(`POST /api/auth/logout: ${r.status}`);
    return r.json();
  },
  setupNeeded: async () => {
    const r = await fetch('/api/auth/setup-needed');
    if (!r.ok) return false;
    const j = await r.json();
    return j.setupNeeded === true;
  },
};

export const users = {
  list:           ()           => get('/api/auth/users'),
  create:         (username, password) => post('/api/auth/users', { username, password }),
  remove:         (id)         => del(`/api/auth/users/${id}`),
  changePassword: (id, password) => put(`/api/auth/users/${id}/password`, { password }),
};
