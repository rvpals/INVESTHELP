import { settings, backup, sql, yahoo } from '../api.js';
import { getPref, setPref, applyTheme } from '../preferences.js';
import { showToast, confirmAction } from '../components/confirm-dialog.js';

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

    <h3 class="mt-16 mb-8">Yahoo Finance Proxy</h3>
    <p class="text-xs text-muted mb-8">If Yahoo Finance is blocked or unreachable, set a proxy URL. Leave blank for direct access.</p>
    <div class="form-group">
      <label>Proxy URL</label>
      <input type="text" class="input" id="proxy-url" value="${serverSettings.proxy_url || ''}" placeholder="e.g. https://corsproxy.io/? or http://localhost:3001">
    </div>
    <p class="text-xs text-muted mb-4">Supported formats:</p>
    <ul class="text-xs text-muted" style="padding-left:20px;margin-bottom:8px">
      <li><code>https://corsproxy.io/?</code> — prepends to full URL</li>
      <li><code>http://localhost:3001</code> — replaces Yahoo base URL</li>
      <li><code>https://my-proxy.workers.dev</code> — Cloudflare Worker proxy</li>
    </ul>
    <button class="btn btn-sm btn-secondary" id="save-proxy">Save Proxy</button>
    <button class="btn btn-sm btn-outline" id="test-proxy" style="margin-left:8px">Test Connection</button>
    <div id="proxy-test-result" class="text-sm mt-8"></div>
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

  document.getElementById('save-proxy')?.addEventListener('click', async () => {
    const url = document.getElementById('proxy-url').value.trim();
    await settings.set('proxy_url', url);
    showToast(url ? 'Proxy saved' : 'Proxy cleared — using direct connection');
  });

  document.getElementById('test-proxy')?.addEventListener('click', async () => {
    const resultEl = document.getElementById('proxy-test-result');
    resultEl.innerHTML = '<div class="flex items-center gap-8"><div class="spinner"></div> Testing connection...</div>';
    // Save current proxy value first
    const url = document.getElementById('proxy-url').value.trim();
    await settings.set('proxy_url', url);
    try {
      const quote = await yahoo.quote('AAPL');
      resultEl.innerHTML = `<span class="text-green">Connected! AAPL = $${quote.price?.toFixed(2) || '?'}</span>`;
    } catch (err) {
      resultEl.innerHTML = `<span style="color:var(--error)">Failed: ${err.message}</span>`;
    }
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
