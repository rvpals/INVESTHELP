import { sql } from '../api.js';
import { dataTable } from '../components/data-table.js';
import { showToast } from '../components/confirm-dialog.js';

export async function render(container) {
  const tables = await sql.tables().catch(() => []);

  container.innerHTML = `<div class="screen">
    <h2 class="mb-16">SQL Explorer</h2>
    <div class="form-group">
      <label>SQL Query</label>
      <textarea class="textarea" id="sql-input" rows="4" style="font-family:var(--font-mono);font-size:13px" placeholder="SELECT * FROM investment_positions"></textarea>
    </div>
    <div class="flex gap-8 mb-16">
      <button class="btn btn-primary" id="run-sql">Execute</button>
      <button class="btn btn-secondary" id="export-csv">Export CSV</button>
    </div>
    <div id="sql-result"></div>
    <h3 class="mt-16 mb-8">Tables</h3>
    <div id="table-browser">
      ${tables.map(t => `
        <div class="card p-12 mb-8">
          <div class="flex justify-between items-center">
            <div class="text-bold">${t.name}</div>
            <button class="btn btn-sm btn-secondary open-table" data-table="${t.name}">Open</button>
          </div>
          <div class="text-xs text-muted mt-4">${t.columns.map(c => `${c.name} (${c.type}${c.pk?' PK':''})`).join(', ')}</div>
        </div>
      `).join('')}
    </div>
  </div>`;

  let lastResult = null;

  document.getElementById('run-sql').addEventListener('click', async () => {
    const query = document.getElementById('sql-input').value.trim();
    if (!query) return;
    await executeQuery(query);
  });

  document.getElementById('export-csv').addEventListener('click', () => {
    if (!lastResult || lastResult.type !== 'query') { showToast('No query results to export'); return; }
    const csv = [lastResult.columns.join(','), ...lastResult.rows.map(r => r.map(v => escapeCsv(v)).join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'sql_export.csv';
    a.click();
  });

  container.querySelectorAll('.open-table').forEach(btn => {
    btn.addEventListener('click', () => {
      document.getElementById('sql-input').value = `SELECT * FROM ${btn.dataset.table}`;
      executeQuery(`SELECT * FROM ${btn.dataset.table}`);
    });
  });

  async function executeQuery(query) {
    const el = document.getElementById('sql-result');
    try {
      const result = await sql.execute(query);
      lastResult = result;
      if (result.type === 'query') {
        el.innerHTML = `<div class="text-xs text-muted mb-4">${result.rows.length} rows</div>` +
          dataTable(result.columns, result.rows);
      } else {
        el.innerHTML = `<div class="card p-12">${result.changes} rows affected</div>`;
      }
    } catch (err) {
      el.innerHTML = `<div class="card p-12" style="color:var(--error)">${err.message}</div>`;
    }
  }
}

function escapeCsv(val) {
  const s = String(val ?? '');
  if (s.includes(',') || s.includes('"') || s.includes('\n')) return '"' + s.replace(/"/g, '""') + '"';
  return s;
}
