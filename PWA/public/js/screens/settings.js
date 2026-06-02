import { settings, backup, sql } from '../api.js';
import { getPref, setPref, applyTheme } from '../preferences.js';
import { showToast, confirmAction } from '../components/confirm-dialog.js';
import { collapsibleCard, initCollapsibleCards } from '../components/collapsible-card.js';

const DASHBOARD_CARDS = [
  { id: 'portfolio_summary', label: 'Portfolio Summary' },
  { id: 'market_indices', label: 'Market Indices' },
  { id: 'daily_glance', label: 'Daily Glance' },
  { id: 'positions', label: 'Positions' },
  { id: 'position_details', label: 'Position Details' },
];

const ALL_INDICES = [
  { ticker: '^IXIC', label: 'NASDAQ' },
  { ticker: '^GSPC', label: 'S&P 500' },
  { ticker: '^DJI', label: 'Dow' },
  { ticker: 'GC=F', label: 'Gold' },
  { ticker: '^RUT', label: 'Russell 2K' },
  { ticker: 'SI=F', label: 'Silver' },
  { ticker: 'CL=F', label: 'Oil' },
  { ticker: 'BTC-USD', label: 'Bitcoin' },
];

const THEMES = ['default','ocean','sunset','midnight','forest','ruby','arctic','gold','chase','fidelity','charcoal',
  'lavender','copper','emerald','slate','mocha','navy','tropical','wine','desert','nordic'];

export async function render(container) {
  const serverSettings = await settings.getAll();

  container.innerHTML = `<div class="screen">
    <h2 class="mb-16">Settings</h2>
    <div class="tab-bar mb-16">
      <button class="tab active" data-tab="prefs">Preferences</button>
      <button class="tab" data-tab="data">Data Management</button>
    </div>
    <div id="settings-content"></div>
  </div>`;

  let activeTab = 'prefs';
  container.querySelectorAll('.tab').forEach(t => {
    t.addEventListener('click', () => {
      container.querySelectorAll('.tab').forEach(x => x.classList.remove('active'));
      t.classList.add('active');
      activeTab = t.dataset.tab;
      renderTab();
    });
  });

  function renderTab() {
    const el = document.getElementById('settings-content');
    if (activeTab === 'prefs') renderPrefs(el, serverSettings);
    else renderData(el);
  }
  renderTab();
}

function renderPrefs(el, serverSettings) {
  const currentTheme = getPref('app_theme') || 'default';
  el.innerHTML = `
    <h3 class="mb-8">Theme</h3>
    <div class="chip-row mb-16">
      ${THEMES.map(t => `<button class="chip${t===currentTheme?' selected':''}" data-theme="${t}">${t}</button>`).join('')}
    </div>
    <h3 class="mb-8">General</h3>
    <div class="toggle-row"><span class="toggle-label">Warn before delete</span><button class="toggle${getPref('warn_before_delete')?' on':''}" id="toggle-warn"></button></div>
    <div class="toggle-row"><span class="toggle-label">Auto-update change history on refresh</span><button class="toggle${serverSettings.auto_update_change_history==='true'?' on':''}" id="toggle-history"></button></div>
    <div class="toggle-row"><span class="toggle-label">Auto-refresh prices</span><button class="toggle${serverSettings.auto_refresh_enabled==='true'?' on':''}" id="toggle-refresh"></button></div>
    ${serverSettings.auto_refresh_enabled === 'true' ? `
      <div class="form-group mt-8">
        <label>Refresh Interval</label>
        <select class="select" id="refresh-interval">
          ${['5m','30m','1h','5h'].map(v => `<option value="${v}"${serverSettings.auto_refresh_interval===v?' selected':''}>${v}</option>`).join('')}
        </select>
      </div>
    ` : ''}
    <div class="form-group mt-16">
      <label>Max News Articles</label>
      <select class="select" id="news-count">
        ${['5','10','20'].map(v => `<option value="${v}"${serverSettings.news_article_count===v?' selected':''}>${v}</option>`).join('')}
      </select>
    </div>

    <h3 class="mt-16 mb-8">Next Day Actions Thresholds</h3>
    <div class="flex gap-8 mb-8">
      <div class="form-group" style="flex:1"><label>Profit Target %</label><input type="number" class="input" id="profit-target" value="${serverSettings.profit_target_pct || 20}" min="1" max="100"></div>
      <div class="form-group" style="flex:1"><label>Stock Cap %</label><input type="number" class="input" id="stock-cap" value="${serverSettings.stock_concentration_cap || 10}" min="1" max="100"></div>
    </div>
    <div class="flex gap-8 mb-8">
      <div class="form-group" style="flex:1"><label>ETF Cap %</label><input type="number" class="input" id="etf-cap" value="${serverSettings.etf_concentration_cap || 25}" min="1" max="100"></div>
      <div class="form-group" style="flex:1"><label>Trailing Stop %</label><input type="number" class="input" id="trailing-stop" value="${serverSettings.trailing_stop_pct || 10}" min="1" max="100"></div>
    </div>

    ${collapsibleCard('settings_dashboard_cards', 'Dashboard Cards', renderDashboardCardsSection())}
    ${collapsibleCard('settings_market_indices', 'Market Indices', renderMarketIndicesSection())}
  `;

  el.querySelectorAll('[data-theme]').forEach(btn => {
    btn.addEventListener('click', () => {
      el.querySelectorAll('[data-theme]').forEach(c => c.classList.remove('selected'));
      btn.classList.add('selected');
      setPref('app_theme', btn.dataset.theme);
      applyTheme();
    });
  });

  document.getElementById('toggle-warn')?.addEventListener('click', function() {
    const on = !this.classList.contains('on');
    this.classList.toggle('on', on);
    setPref('warn_before_delete', on);
  });

  document.getElementById('toggle-history')?.addEventListener('click', async function() {
    const on = !this.classList.contains('on');
    this.classList.toggle('on', on);
    await settings.set('auto_update_change_history', on ? 'true' : 'false');
  });

  document.getElementById('toggle-refresh')?.addEventListener('click', async function() {
    const on = !this.classList.contains('on');
    this.classList.toggle('on', on);
    await settings.set('auto_refresh_enabled', on ? 'true' : 'false');
    showToast(on ? 'Auto-refresh enabled' : 'Auto-refresh disabled');
  });

  document.getElementById('refresh-interval')?.addEventListener('change', async function() {
    await settings.set('auto_refresh_interval', this.value);
    showToast('Interval updated');
  });

  document.getElementById('news-count')?.addEventListener('change', async function() {
    await settings.set('news_article_count', this.value);
  });

  for (const [id, key] of [['profit-target','profit_target_pct'],['stock-cap','stock_concentration_cap'],['etf-cap','etf_concentration_cap'],['trailing-stop','trailing_stop_pct']]) {
    document.getElementById(id)?.addEventListener('change', async function() {
      await settings.set(key, this.value);
      showToast('Threshold updated');
    });
  }

  initCollapsibleCards(el);
  attachDashboardCardHandlers(el, serverSettings);
  attachMarketIndexHandlers(el, serverSettings);
}

function renderDashboardCardsSection() {
  const order = (getPref('dashboard_card_order') || DASHBOARD_CARDS.map(c => c.id).join(',')).split(',');
  const cards = order.map(id => DASHBOARD_CARDS.find(c => c.id === id)).filter(Boolean);
  return `
    <p class="text-xs text-muted mb-8">Toggle visibility and reorder dashboard cards.</p>
    <div id="dashboard-cards-list">
      ${cards.map((c, i) => {
        const visible = getPref('dashboard_card_visible_' + c.id) !== false;
        return `
        <div class="flex items-center gap-8 py-6" style="border-bottom:1px solid color-mix(in srgb, var(--outline) 20%, transparent)">
          <button class="toggle${visible ? ' on' : ''}" data-card-toggle="${c.id}" style="flex-shrink:0"></button>
          <span class="text-sm" style="flex:1">${c.label}</span>
          <button class="btn-icon" data-card-up="${c.id}" ${i === 0 ? 'disabled style="opacity:0.3"' : ''} title="Move up">&#9650;</button>
          <button class="btn-icon" data-card-down="${c.id}" ${i === cards.length - 1 ? 'disabled style="opacity:0.3"' : ''} title="Move down">&#9660;</button>
        </div>`;
      }).join('')}
    </div>
  `;
}

function renderMarketIndicesSection() {
  const orderStr = getPref('market_indices_order') || ALL_INDICES.map(x => x.ticker).join(',');
  const order = orderStr.split(',');
  const enabledStr = getPref('market_indices') || '^IXIC,^GSPC,^DJI,GC=F';
  const enabledSet = new Set(enabledStr.split(','));
  const indices = order.map(t => ALL_INDICES.find(x => x.ticker === t)).filter(Boolean);
  return `
    <p class="text-xs text-muted mb-8">Toggle which market indices appear on the dashboard and reorder them.</p>
    <div id="market-indices-list">
      ${indices.map((idx, i) => {
        const on = enabledSet.has(idx.ticker);
        return `
        <div class="flex items-center gap-8 py-6" style="border-bottom:1px solid color-mix(in srgb, var(--outline) 20%, transparent)">
          <button class="toggle${on ? ' on' : ''}" data-idx-toggle="${idx.ticker}" style="flex-shrink:0"></button>
          <span class="text-sm" style="flex:1">${idx.label} <span class="text-xs text-muted">(${idx.ticker})</span></span>
          <button class="btn-icon" data-idx-up="${idx.ticker}" ${i === 0 ? 'disabled style="opacity:0.3"' : ''} title="Move up">&#9650;</button>
          <button class="btn-icon" data-idx-down="${idx.ticker}" ${i === indices.length - 1 ? 'disabled style="opacity:0.3"' : ''} title="Move down">&#9660;</button>
        </div>`;
      }).join('')}
    </div>
  `;
}

function attachDashboardCardHandlers(el, serverSettings) {
  el.querySelectorAll('[data-card-toggle]').forEach(btn => {
    btn.addEventListener('click', function() {
      const id = this.dataset.cardToggle;
      const on = !this.classList.contains('on');
      this.classList.toggle('on', on);
      setPref('dashboard_card_visible_' + id, on);
    });
  });

  el.querySelectorAll('[data-card-up], [data-card-down]').forEach(btn => {
    btn.addEventListener('click', function() {
      const id = this.dataset.cardUp || this.dataset.cardDown;
      const dir = this.dataset.cardUp ? -1 : 1;
      const order = (getPref('dashboard_card_order') || DASHBOARD_CARDS.map(c => c.id).join(',')).split(',');
      const idx = order.indexOf(id);
      if (idx < 0) return;
      const newIdx = idx + dir;
      if (newIdx < 0 || newIdx >= order.length) return;
      [order[idx], order[newIdx]] = [order[newIdx], order[idx]];
      setPref('dashboard_card_order', order.join(','));
      const content = document.getElementById('content-settings_dashboard_cards');
      if (content) {
        content.querySelector('.collapsible-body').innerHTML = renderDashboardCardsSection();
        attachDashboardCardHandlers(el, serverSettings);
      }
    });
  });
}

function attachMarketIndexHandlers(el, serverSettings) {
  el.querySelectorAll('[data-idx-toggle]').forEach(btn => {
    btn.addEventListener('click', function() {
      const ticker = this.dataset.idxToggle;
      const on = !this.classList.contains('on');
      this.classList.toggle('on', on);
      const orderStr = getPref('market_indices_order') || ALL_INDICES.map(x => x.ticker).join(',');
      const allTickers = orderStr.split(',');
      const enabled = allTickers.filter(t => {
        if (t === ticker) return on;
        return (getPref('market_indices') || '^IXIC,^GSPC,^DJI,GC=F').split(',').includes(t);
      });
      setPref('market_indices', enabled.join(','));
    });
  });

  el.querySelectorAll('[data-idx-up], [data-idx-down]').forEach(btn => {
    btn.addEventListener('click', function() {
      const ticker = this.dataset.idxUp || this.dataset.idxDown;
      const dir = this.dataset.idxUp ? -1 : 1;
      const order = (getPref('market_indices_order') || ALL_INDICES.map(x => x.ticker).join(',')).split(',');
      const idx = order.indexOf(ticker);
      if (idx < 0) return;
      const newIdx = idx + dir;
      if (newIdx < 0 || newIdx >= order.length) return;
      [order[idx], order[newIdx]] = [order[newIdx], order[idx]];
      setPref('market_indices_order', order.join(','));
      const content = document.getElementById('content-settings_market_indices');
      if (content) {
        content.querySelector('.collapsible-body').innerHTML = renderMarketIndicesSection();
        attachMarketIndexHandlers(el, serverSettings);
      }
    });
  });
}

async function renderData(el) {
  const serverSettings = await settings.getAll();

  el.innerHTML = `
    <h3 class="mb-8">Backup & Restore</h3>
    <div class="flex gap-8 mb-8">
      <a href="${backup.exportUrl}" class="btn btn-primary" download>Export Data</a>
      <label class="btn btn-secondary" style="cursor:pointer">
        Restore Data
        <input type="file" accept=".json" id="import-file" style="display:none">
      </label>
    </div>

    <div class="toggle-row">
      <span class="toggle-label">Automatic backup on refresh</span>
      <button class="toggle${serverSettings.auto_backup_on_refresh === 'true' ? ' on' : ''}" id="toggle-auto-backup"></button>
    </div>
    ${serverSettings.auto_backup_on_refresh === 'true' ? `
      <div class="form-group mt-8">
        <label>Number of automatic backups to keep</label>
        <input type="number" class="input" id="backup-keep-count" value="${serverSettings.auto_backup_keep_count || 10}" min="1" max="100" style="width:80px">
      </div>
    ` : ''}

    <div id="backup-list-section" class="mt-8 mb-16">
      <button class="btn btn-sm btn-outline" id="show-backups-btn">Show Backup Files</button>
      <div id="backup-list" class="mt-8 hidden"></div>
    </div>

    <hr style="border:none;border-top:1px solid var(--outline);opacity:0.3;margin:16px 0">

    <h3 class="mb-8">Import Data</h3>

    <div class="card p-12 mb-8">
      <div class="flex justify-between items-center mb-8">
        <h4 class="text-sm text-bold">Transaction Records</h4>
        <button class="btn btn-sm btn-error" id="clear-tx">Clear All</button>
      </div>
      <div class="flex gap-8">
        <button class="btn btn-sm btn-outline" id="define-map-tx">Define Mapping</button>
        <label class="btn btn-sm btn-secondary" style="cursor:pointer">
          Start Import
          <input type="file" accept=".csv" id="csv-tx" style="display:none">
        </label>
      </div>
      <div id="csv-tx-result" class="text-sm mt-4"></div>
    </div>

    <div class="card p-12 mb-8">
      <div class="flex justify-between items-center mb-8">
        <h4 class="text-sm text-bold">Position Details</h4>
        <button class="btn btn-sm btn-error" id="clear-pos">Clear All</button>
      </div>
      <div class="flex gap-8">
        <button class="btn btn-sm btn-outline" id="define-map-pos">Define Mapping</button>
        <label class="btn btn-sm btn-secondary" style="cursor:pointer">
          Start Import
          <input type="file" accept=".csv" id="csv-pos" style="display:none">
        </label>
      </div>
      <div id="csv-pos-result" class="text-sm mt-4"></div>
    </div>

    <div class="card p-12 mb-8">
      <div class="flex justify-between items-center mb-8">
        <h4 class="text-sm text-bold">Performance Records</h4>
        <button class="btn btn-sm btn-error" id="clear-perf">Clear All</button>
      </div>
      <div class="flex gap-8">
        <button class="btn btn-sm btn-outline" id="define-map-perf">Define Mapping</button>
        <label class="btn btn-sm btn-secondary" style="cursor:pointer">
          Start Import
          <input type="file" accept=".csv" id="csv-perf" style="display:none">
        </label>
      </div>
      <div id="csv-perf-result" class="text-sm mt-4"></div>
    </div>
  `;

  // Backup import
  document.getElementById('import-file')?.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    try {
      const result = await backup.import(file);
      showToast(`Imported: ${result.accounts || 0} accounts, ${result.positions || 0} positions, ${result.transactions || 0} transactions`);
    } catch (err) { showToast('Import failed: ' + err.message); }
  });

  // Auto-backup toggle
  document.getElementById('toggle-auto-backup')?.addEventListener('click', async function () {
    const on = !this.classList.contains('on');
    this.classList.toggle('on', on);
    await settings.set('auto_backup_on_refresh', on ? 'true' : 'false');
    renderData(el);
  });

  // Backup keep count
  document.getElementById('backup-keep-count')?.addEventListener('change', async function () {
    await settings.set('auto_backup_keep_count', this.value);
    showToast('Updated');
  });

  // Show backup list
  document.getElementById('show-backups-btn')?.addEventListener('click', async () => {
    const listEl = document.getElementById('backup-list');
    listEl.classList.toggle('hidden');
    if (!listEl.classList.contains('hidden')) {
      try {
        const files = await backup.list();
        if (files.length === 0) {
          listEl.innerHTML = '<div class="text-sm text-muted">No backup files</div>';
        } else {
          listEl.innerHTML = files.map(f => `
            <div class="flex justify-between items-center py-4" style="border-bottom:1px solid color-mix(in srgb, var(--outline) 20%, transparent)">
              <div>
                <div class="text-sm">${f.name}</div>
                <div class="text-xs text-muted">${new Date(f.modified).toLocaleString()} &middot; ${(f.size / 1024).toFixed(1)} KB</div>
              </div>
              <button class="btn btn-sm btn-error backup-del" data-name="${f.name}">&times;</button>
            </div>
          `).join('');
          listEl.querySelectorAll('.backup-del').forEach(btn => {
            btn.addEventListener('click', async () => {
              if (await confirmAction('Delete Backup', `Delete ${btn.dataset.name}?`)) {
                await fetch(`/api/backup/${encodeURIComponent(btn.dataset.name)}`, { method: 'DELETE' });
                showToast('Deleted');
                document.getElementById('show-backups-btn').click();
                document.getElementById('show-backups-btn').click();
              }
            });
          });
        }
      } catch { listEl.innerHTML = '<div class="text-sm text-muted">Error loading backups</div>'; }
    }
  });

  // Clear buttons
  document.getElementById('clear-tx')?.addEventListener('click', async () => {
    if (await confirmAction('Clear Transactions', 'Delete ALL transaction records?')) {
      await sql.execute('DELETE FROM investment_transactions');
      showToast('All transactions cleared');
    }
  });
  document.getElementById('clear-pos')?.addEventListener('click', async () => {
    if (await confirmAction('Clear Positions', 'Delete ALL position records?')) {
      await sql.execute('DELETE FROM investment_positions');
      showToast('All positions cleared');
    }
  });
  document.getElementById('clear-perf')?.addEventListener('click', async () => {
    if (await confirmAction('Clear Performance', 'Delete ALL performance records?')) {
      await sql.execute('DELETE FROM account_performance');
      showToast('All performance records cleared');
    }
  });

  // Define mapping buttons (open mapping dialog)
  for (const [btnId, importType] of [['define-map-tx', 'Transaction'], ['define-map-pos', 'Position'], ['define-map-perf', 'Performance']]) {
    document.getElementById(btnId)?.addEventListener('click', () => {
      showMappingDialog(importType);
    });
  }

  // CSV import buttons
  for (const [inputId, importType, resultId] of [['csv-tx', 'Transaction', 'csv-tx-result'], ['csv-pos', 'Position', 'csv-pos-result'], ['csv-perf', 'Performance', 'csv-perf-result']]) {
    document.getElementById(inputId)?.addEventListener('change', async (e) => {
      const file = e.target.files[0];
      if (!file) return;

      if (importType === 'Position') {
        if (!await confirmAction('Confirm Import', 'Position details will be refreshed with imported CSV file. Are you sure?')) return;
      }

      if (importType === 'Performance') {
        await handlePerformanceImport(file, resultId);
        e.target.value = '';
        return;
      }

      const form = new FormData();
      form.append('file', file);
      form.append('importType', importType);
      form.append('mapping', '{}');
      const resultEl = document.getElementById(resultId);
      try {
        const r = await fetch('/api/csv-import/execute', { method: 'POST', body: form });
        const result = await r.json();
        if (result.error) {
          resultEl.innerHTML = `<span style="color:var(--error)">${result.error}</span>`;
        } else {
          resultEl.innerHTML = `${result.imported || 0} records imported`;
          showToast(`${importType} import complete: ${result.imported} records`);
        }
      } catch (err) {
        resultEl.innerHTML = `<span style="color:var(--error)">Import failed</span>`;
        showToast('CSV import failed');
      }
      e.target.value = '';
    });
  }
}

function showMappingDialog(importType) {
  const overlay = document.getElementById('dialog-overlay');
  overlay.className = 'dialog-overlay';
  overlay.innerHTML = `
    <div class="dialog" style="max-width:500px">
      <div class="dialog-title">Define Mapping: ${importType}</div>
      <p class="text-sm text-muted mb-8">Upload a CSV file to preview columns and define the mapping.</p>
      <label class="btn btn-secondary w-full" style="cursor:pointer">
        Select CSV to Preview
        <input type="file" accept=".csv" id="mapping-csv-file" style="display:none">
      </label>
      <div id="mapping-preview" class="mt-8"></div>
      <div class="dialog-actions">
        <button class="btn btn-outline" id="mapping-close">Close</button>
      </div>
    </div>
  `;

  document.getElementById('mapping-close').addEventListener('click', () => {
    overlay.className = 'dialog-overlay hidden';
  });

  document.getElementById('mapping-csv-file')?.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const form = new FormData();
    form.append('file', file);
    try {
      const r = await fetch('/api/csv-import/preview', { method: 'POST', body: form });
      const result = await r.json();
      const previewEl = document.getElementById('mapping-preview');
      if (result.error) {
        previewEl.innerHTML = `<span style="color:var(--error)">${result.error}</span>`;
        return;
      }
      previewEl.innerHTML = `
        <div class="text-sm text-bold mb-4">${result.totalRows} data rows found</div>
        <div class="text-xs text-muted mb-8">Headers: ${result.headers.join(', ')}</div>
        <div class="text-xs text-muted mb-4">Auto-mapped fields:</div>
        <div class="text-sm mb-8">${Object.entries(result.autoMapping || {}).map(([col, field]) =>
          `<div>${result.headers[col] || col} &rarr; <strong>${field}</strong></div>`
        ).join('') || '<em>No auto-mappings found</em>'}</div>
        <div class="text-xs text-muted">Preview (first 3 rows):</div>
        <div class="data-table-wrapper mt-4" style="max-height:200px;overflow:auto">
          <table class="data-table">
            <thead><tr>${result.headers.map(h => `<th>${h}</th>`).join('')}</tr></thead>
            <tbody>${result.preview.map(row => `<tr>${row.map(c => `<td>${c}</td>`).join('')}</tr>`).join('')}</tbody>
          </table>
        </div>
      `;
    } catch (err) {
      document.getElementById('mapping-preview').innerHTML = `<span style="color:var(--error)">Error: ${err.message}</span>`;
    }
  });
}

async function handlePerformanceImport(file, resultId) {
  const resultEl = document.getElementById(resultId);
  try {
    // Preview CSV to get auto-mapping (column index → field name)
    const previewForm = new FormData();
    previewForm.append('file', file);
    const previewResp = await fetch('/api/csv-import/preview', { method: 'POST', body: previewForm });
    const preview = await previewResp.json();
    if (preview.error) { resultEl.innerHTML = `<span style="color:var(--error)">${preview.error}</span>`; return; }
    const colMapping = preview.autoMapping || {};

    // Scan for unique account names using the auto-mapping
    const scanForm = new FormData();
    scanForm.append('file', file);
    scanForm.append('mapping', JSON.stringify(colMapping));
    const scanResp = await fetch('/api/csv-import/preview-accounts', { method: 'POST', body: scanForm });
    const scanData = await scanResp.json();

    let accountNameMapping = {};
    if (scanData.csvAccountNames && scanData.csvAccountNames.length > 0 && scanData.accounts && scanData.accounts.length > 0) {
      const mapping = await showAccountMappingDialog(scanData.csvAccountNames, scanData.accounts);
      if (!mapping) return;
      accountNameMapping = mapping;
    } else if (scanData.csvAccountNames && scanData.csvAccountNames.length > 0 && (!scanData.accounts || scanData.accounts.length === 0)) {
      resultEl.innerHTML = `<span style="color:var(--error)">No accounts found. Create accounts first before importing performance data.</span>`;
      return;
    }

    const form = new FormData();
    form.append('file', file);
    form.append('importType', 'Performance');
    form.append('mapping', JSON.stringify(colMapping));
    form.append('accountNameMapping', JSON.stringify(accountNameMapping));
    const r = await fetch('/api/csv-import/execute', { method: 'POST', body: form });
    const result = await r.json();
    if (result.error) {
      resultEl.innerHTML = `<span style="color:var(--error)">${result.error}</span>`;
    } else {
      resultEl.innerHTML = `${result.imported || 0} records imported`;
      showToast(`Performance import complete: ${result.imported} records`);
    }
  } catch (err) {
    resultEl.innerHTML = `<span style="color:var(--error)">Import failed</span>`;
    showToast('CSV import failed');
  }
}

function showAccountMappingDialog(csvNames, appAccounts) {
  return new Promise((resolve) => {
    const overlay = document.getElementById('dialog-overlay');
    overlay.className = 'dialog-overlay';

    // Pre-select best match: case-insensitive partial match
    const preselect = {};
    for (const csvName of csvNames) {
      const lower = csvName.toLowerCase();
      const exact = appAccounts.find(a => a.name.toLowerCase() === lower);
      if (exact) { preselect[csvName] = exact.id; continue; }
      const partial = appAccounts.find(a => a.name.toLowerCase().includes(lower) || lower.includes(a.name.toLowerCase()));
      if (partial) { preselect[csvName] = partial.id; continue; }
      preselect[csvName] = appAccounts.length > 0 ? appAccounts[0].id : 0;
    }

    const rows = csvNames.map(csvName => {
      const options = appAccounts.map(a =>
        `<option value="${a.id}"${preselect[csvName] === a.id ? ' selected' : ''}>${a.name}</option>`
      ).join('');
      return `
        <div class="flex items-center gap-8 py-6" style="border-bottom:1px solid color-mix(in srgb, var(--outline) 20%, transparent)">
          <span class="text-sm" style="flex:1;font-weight:500">${csvName}</span>
          <span class="text-muted text-sm">&rarr;</span>
          <select class="select" data-csv-name="${csvName}" style="flex:1;min-width:0">${options}</select>
        </div>`;
    }).join('');

    overlay.innerHTML = `
      <div class="dialog" style="max-width:500px">
        <div class="dialog-title">Map Account Names</div>
        <p class="text-sm text-muted mb-8">Map CSV account names to your existing accounts.</p>
        <div class="text-xs text-muted mb-4" style="display:flex;gap:8px">
          <span style="flex:1;font-weight:600">CSV Account Name</span>
          <span style="width:24px"></span>
          <span style="flex:1;font-weight:600">App Account</span>
        </div>
        <div style="max-height:300px;overflow-y:auto">${rows}</div>
        <div class="dialog-actions mt-16">
          <button class="btn btn-outline" id="acct-map-cancel">Cancel</button>
          <button class="btn btn-primary" id="acct-map-confirm">Import</button>
        </div>
      </div>
    `;

    document.getElementById('acct-map-cancel').addEventListener('click', () => {
      overlay.className = 'dialog-overlay hidden';
      resolve(null);
    });

    document.getElementById('acct-map-confirm').addEventListener('click', () => {
      const mapping = {};
      overlay.querySelectorAll('[data-csv-name]').forEach(sel => {
        mapping[sel.dataset.csvName] = parseInt(sel.value);
      });
      overlay.className = 'dialog-overlay hidden';
      resolve(mapping);
    });
  });
}
