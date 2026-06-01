import { accounts, performance } from '../api.js';
import { navigate } from '../router.js';
import { formatCurrency } from '../utils/format.js';
import { formatDate } from '../utils/dates.js';
import { renderLineChart } from '../components/line-chart.js';
import { dataTable } from '../components/data-table.js';

export async function render(container, { id }) {
  const [acct, records] = await Promise.all([accounts.get(id), performance.list(id)]);

  container.innerHTML = `<div class="screen">
    <h2 class="mb-8">${acct.name}</h2>
    <div class="text-sm text-muted mb-16">${acct.description || ''}</div>
    <div class="flex gap-8 mb-16">
      <div class="card p-12" style="flex:1;text-align:center"><div class="text-xs text-muted">Initial Value</div><div class="text-bold">${formatCurrency(acct.initialValue)}</div></div>
      <div class="card p-12" style="flex:1;text-align:center"><div class="text-xs text-muted">Last Value</div><div class="text-bold">${formatCurrency(acct.lastValue || 0)}</div></div>
    </div>
    ${records.length >= 2 ? '<div class="chart-container mb-16"><canvas id="acct-chart"></canvas></div>' : ''}
    ${records.length > 0 ? dataTable(['Date', 'Value', 'Note'], records.map(r => [formatDate(r.date), formatCurrency(r.totalValue), r.note || ''])) : '<div class="text-muted text-center">No performance records</div>'}
    <div class="flex gap-8 mt-16">
      <button class="btn btn-secondary" onclick="location.hash='#/account-form/${id}'">Edit</button>
      <button class="btn btn-outline" onclick="history.back()">Back</button>
    </div>
  </div>`;

  if (records.length >= 2) {
    const sorted = [...records].sort((a, b) => a.date - b.date);
    const canvas = document.getElementById('acct-chart');
    renderLineChart(canvas, sorted.map(r => ({ x: r.date, y: r.totalValue })), { height: 180, xIsEpochDays: true });
  }
}
