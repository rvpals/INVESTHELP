import { positions, refresh } from '../api.js';
import { navigate } from '../router.js';
import { tickerIcon } from '../components/ticker-icon.js';
import { renderPieChart } from '../components/pie-chart.js';
import { formatCurrency, formatSignedCurrency, formatPercent, gainLossClass, gainLossBadgeClass } from '../utils/format.js';
import { showToast } from '../components/confirm-dialog.js';

let currentTab = 'Stock';
let sortBy = 'value';

export async function render(container) {
  const data = await positions.list();

  container.innerHTML = `<div class="screen">
    <div class="flex justify-between items-center mb-8">
      <h2>Positions</h2>
      <div class="flex gap-8">
        <button class="btn btn-sm btn-secondary" id="add-pos-btn">+ Add</button>
        <button class="btn btn-sm btn-primary" id="refresh-all-btn">Refresh All</button>
      </div>
    </div>
    <div class="tab-bar mb-8">
      <button class="tab${currentTab === 'Stock' ? ' active' : ''}" data-tab="Stock">STOCK</button>
      <button class="tab${currentTab === 'ETF' ? ' active' : ''}" data-tab="ETF">ETF</button>
      <button class="tab${currentTab === 'Analysis' ? ' active' : ''}" data-tab="Analysis">Analysis</button>
    </div>
    <div id="tab-content"></div>
  </div>`;

  container.querySelectorAll('.tab').forEach(t => {
    t.addEventListener('click', () => { currentTab = t.dataset.tab; render(container); });
  });

  document.getElementById('add-pos-btn').addEventListener('click', () => navigate('#/item-form'));

  document.getElementById('refresh-all-btn').addEventListener('click', async () => {
    showToast('Refreshing prices...');
    await refresh.all();
    showToast('Refresh complete');
    render(container);
  });

  const content = document.getElementById('tab-content');

  if (currentTab === 'Analysis') {
    const stocks = data.filter(p => p.type === 'Stock');
    const etfs = data.filter(p => p.type === 'ETF');
    content.innerHTML = `
      <h3 class="mb-8">Stock Allocation</h3>
      <div class="chart-container mb-16"><canvas id="stock-pie"></canvas></div>
      <h3 class="mb-8">ETF Allocation</h3>
      <div class="chart-container"><canvas id="etf-pie"></canvas></div>
    `;
    const sp = document.getElementById('stock-pie');
    if (sp && stocks.length) renderPieChart(sp, stocks.map(p => ({ label: p.ticker, value: p.value })));
    const ep = document.getElementById('etf-pie');
    if (ep && etfs.length) renderPieChart(ep, etfs.map(p => ({ label: p.ticker, value: p.value })));
  } else {
    const filtered = data.filter(p => p.type === currentTab);
    const sorted = sortItems(filtered);
    content.innerHTML = `
      <div class="chart-container mb-8"><canvas id="tab-pie"></canvas></div>
      <div class="flex justify-between items-center mb-8">
        <span class="text-sm text-muted">${sorted.length} items</span>
        <select class="select" id="sort-select" style="width:auto;padding:4px 8px;font-size:12px">
          <option value="value"${sortBy==='value'?' selected':''}>Total Value</option>
          <option value="ticker"${sortBy==='ticker'?' selected':''}>Ticker</option>
          <option value="price"${sortBy==='price'?' selected':''}>Price</option>
        </select>
      </div>
      ${sorted.map(p => positionRow(p)).join('')}
      ${sorted.length === 0 ? '<div class="text-center text-muted p-16">No positions</div>' : ''}
    `;

    const pie = document.getElementById('tab-pie');
    if (pie && sorted.length) renderPieChart(pie, sorted.map(p => ({ label: p.ticker, value: p.value })), { height: 160 });

    document.getElementById('sort-select').addEventListener('change', (e) => { sortBy = e.target.value; render(container); });

    content.querySelectorAll('.position-row').forEach(el => {
      el.addEventListener('click', (e) => {
        if (e.target.closest('.edit-pos-btn')) return;
        navigate(`#/item/${el.dataset.ticker}`);
      });
    });

    content.querySelectorAll('.edit-pos-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        navigate(`#/item-form/${btn.dataset.editTicker}`);
      });
    });
  }
}

function sortItems(items) {
  switch (sortBy) {
    case 'ticker': return [...items].sort((a, b) => a.ticker.localeCompare(b.ticker));
    case 'price': return [...items].sort((a, b) => b.currentPrice - a.currentPrice);
    default: return [...items].sort((a, b) => b.value - a.value);
  }
}

function positionRow(p) {
  const dayChange = p.currentPrice - (p.value / (p.quantity || 1) - p.dayGainLoss / (p.quantity || 1));
  const dayPct = p.currentPrice > 0 && p.quantity > 0 ? (p.dayGainLoss / (p.value - p.dayGainLoss)) * 100 : 0;
  return `
    <div class="position-row" data-ticker="${p.ticker}">
      ${tickerIcon(p.ticker, p.name)}
      <div class="position-info">
        <div class="position-ticker">${p.ticker}</div>
        <div class="position-name">${p.name || ''}</div>
        <div class="position-price">${formatCurrency(p.currentPrice)} <span class="${gainLossClass(p.dayGainLoss)}">${formatSignedCurrency(p.dayGainLoss / (p.quantity || 1))} ${formatPercent(dayPct)}</span></div>
      </div>
      <div class="position-right">
        <div class="position-value">${formatCurrency(p.value)}</div>
        <span class="badge ${gainLossBadgeClass(p.dayGainLoss)}">${formatSignedCurrency(p.dayGainLoss)}</span>
      </div>
      <button class="btn btn-sm btn-outline edit-pos-btn" data-edit-ticker="${p.ticker}" title="Edit" style="margin-left:4px;padding:4px 8px">&#9998;</button>
    </div>
  `;
}
