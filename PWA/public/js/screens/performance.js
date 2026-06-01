import { accounts, performance } from '../api.js';
import { collapsibleCard, initCollapsibleCards } from '../components/collapsible-card.js';
import { renderLineChart } from '../components/line-chart.js';
import { dataTable } from '../components/data-table.js';
import { showToast } from '../components/confirm-dialog.js';
import { formatCurrency } from '../utils/format.js';
import { formatDate, todayEpochDays } from '../utils/dates.js';

export async function render(container) {
  const [acctList, records] = await Promise.all([accounts.list(), performance.list()]);

  container.innerHTML = `<div class="screen">
    <h2 class="mb-16">Account Performance</h2>
    ${collapsibleCard('perf_add', 'Add Performance Record', `
      <div class="form-group">
        <label>Account</label>
        <select class="select" id="perf-acct">${acctList.map(a => `<option value="${a.id}">${a.name}</option>`).join('')}</select>
      </div>
      <div class="form-row">
        <div class="form-group"><label>Total Value</label><input type="number" class="input" id="perf-val" step="0.01"></div>
        <div class="form-group"><label>Note</label><input type="text" class="input" id="perf-note"></div>
      </div>
      <button class="btn btn-primary w-full" id="add-record-btn">Add Record</button>
    `)}
    ${collapsibleCard('perf_chart', 'Performance Chart', `
      <div class="chart-container"><canvas id="perf-chart-canvas"></canvas></div>
    `, { defaultPinned: true })}
    ${collapsibleCard('perf_records', 'Records (' + records.length + ')', `
      ${dataTable(['Account', 'Date', 'Value', 'Note'], records.map(r => {
        const acct = acctList.find(a => a.id === r.accountId);
        return [acct?.name || '?', formatDate(r.date), formatCurrency(r.totalValue), r.note || ''];
      }))}
    `, { defaultPinned: true })}
  </div>`;

  initCollapsibleCards(container);

  document.getElementById('add-record-btn').addEventListener('click', async () => {
    const accountId = parseInt(document.getElementById('perf-acct').value);
    const totalValue = parseFloat(document.getElementById('perf-val').value) || 0;
    const note = document.getElementById('perf-note').value.trim();
    try {
      await performance.create({ accountId, totalValue, date: todayEpochDays(), note });
      showToast('Record added');
      render(container);
    } catch (err) { showToast('Error: ' + err.message); }
  });

  if (records.length >= 2) {
    const sorted = [...records].sort((a, b) => a.date - b.date);
    const canvas = document.getElementById('perf-chart-canvas');
    renderLineChart(canvas, sorted.map(r => ({ x: r.date, y: r.totalValue })), { height: 220, xIsEpochDays: true });
  }
}
