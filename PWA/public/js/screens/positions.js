import { positions, refresh } from '../api.js';
import { navigate } from '../router.js';
import { tickerIcon } from '../components/ticker-icon.js';
import { renderPieChart } from '../components/pie-chart.js';
import { formatCurrency, formatSignedCurrency, formatPercent, formatShares, gainLossClass, gainLossBadgeClass } from '../utils/format.js';
import { showToast } from '../components/confirm-dialog.js';

let currentTab = 'Stock';
let sortBy = 'value';
let divSortBy = 'annual';
let divSortAsc = false;

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
      <button class="tab${currentTab === 'Dividend' ? ' active' : ''}" data-tab="Dividend">Dividend</button>
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

  if (currentTab === 'Dividend') {
    renderDividendTab(content, data);
  } else if (currentTab === 'Analysis') {
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

function renderDividendTab(content, data) {
  const stocks = data.filter(p => p.type === 'Stock' && (p.dividendRate || 0) > 0 && (p.quantity || 0) > 0);
  const etfs = data.filter(p => p.type === 'ETF' && (p.dividendRate || 0) > 0 && (p.quantity || 0) > 0);
  const totalAnnual = [...stocks, ...etfs].reduce((s, p) => s + p.dividendRate * p.quantity, 0);

  if (!stocks.length && !etfs.length) {
    content.innerHTML = '<div class="text-center text-muted p-16">No dividend-paying positions<br><small>Dividend data is fetched during price refresh</small></div>';
    return;
  }

  content.innerHTML = `
    <div class="card mb-12" style="text-align:center;padding:16px;background:var(--primary-bg)">
      <div class="text-sm text-muted">Total Annual Dividend Income</div>
      <div style="font-size:1.5rem;font-weight:bold;color:#1565C0">${formatCurrency(totalAnnual)}</div>
    </div>
    ${stocks.length ? dividendCardHTML('Stock', stocks) : ''}
    ${etfs.length ? dividendCardHTML('ETF', etfs) : ''}
  `;

  if (stocks.length) {
    renderExplodingPie(document.getElementById('div-pie-Stock'), stocks);
    bindDividendTable(content, 'Stock', stocks);
  }
  if (etfs.length) {
    renderExplodingPie(document.getElementById('div-pie-ETF'), etfs);
    bindDividendTable(content, 'ETF', etfs);
  }
}

const PIE_COLORS = [
  '#2E7D32', '#0277BD', '#E65100', '#6A1B9A', '#33691E', '#B71C1C',
  '#37474F', '#F9A825', '#C2185B', '#424242', '#5C6BC0', '#BF360C',
  '#00796B', '#455A64', '#4E342E', '#1A237E', '#00897B', '#880E4F',
  '#F57F17', '#01579B',
];

function dividendCardHTML(type, items) {
  const total = items.reduce((s, p) => s + p.dividendRate * p.quantity, 0);
  return `
    <div class="card mb-12" style="padding:16px">
      <div class="flex justify-between items-center mb-8">
        <h3 style="margin:0">${type}</h3>
        <span style="color:#1565C0;font-weight:500;font-size:0.85rem">Annual: ${formatCurrency(total)}</span>
      </div>
      <div class="chart-container mb-8"><canvas id="div-pie-${type}"></canvas></div>
      <div class="flex gap-8 mb-8" style="flex-wrap:wrap">
        ${items.map((p, i) => {
          const pct = total > 0 ? (p.dividendRate * p.quantity / total * 100).toFixed(1) : '0.0';
          const maxIdx = items.reduce((mi, pp, ii) => pp.dividendRate * pp.quantity > items[mi].dividendRate * items[mi].quantity ? ii : mi, 0);
          return `<span style="display:inline-flex;align-items:center;gap:4px;font-size:0.75rem${i === maxIdx ? ';font-weight:bold' : ''}"><span style="width:10px;height:10px;border-radius:50%;background:${PIE_COLORS[i % PIE_COLORS.length]};display:inline-block"></span>${p.ticker} ${pct}%</span>`;
        }).join('')}
      </div>
      <hr style="border-color:var(--border-color);margin:8px 0">
      <div class="flex justify-end gap-4 mb-4" id="div-sort-${type}">
        <button class="btn btn-sm ${divSortBy==='annual'?'btn-primary':'btn-outline'}" data-sort="annual">Annual</button>
        <button class="btn btn-sm ${divSortBy==='rate'?'btn-primary':'btn-outline'}" data-sort="rate">Div/Share</button>
        <button class="btn btn-sm ${divSortBy==='ticker'?'btn-primary':'btn-outline'}" data-sort="ticker">Ticker</button>
        <button class="btn btn-sm ${divSortBy==='shares'?'btn-primary':'btn-outline'}" data-sort="shares">Shares</button>
      </div>
      <table class="data-table" style="width:100%">
        <thead><tr><th>Ticker</th><th>Shares</th><th>Div/Share</th><th style="color:#1565C0">Annual</th><th>%</th></tr></thead>
        <tbody id="div-tbody-${type}"></tbody>
      </table>
    </div>
  `;
}

function sortDividendItems(items) {
  const sorted = [...items];
  switch (divSortBy) {
    case 'ticker': sorted.sort((a, b) => a.ticker.localeCompare(b.ticker)); break;
    case 'rate': sorted.sort((a, b) => b.dividendRate - a.dividendRate); break;
    case 'shares': sorted.sort((a, b) => b.quantity - a.quantity); break;
    default: sorted.sort((a, b) => (b.dividendRate * b.quantity) - (a.dividendRate * a.quantity)); break;
  }
  if (divSortAsc) sorted.reverse();
  return sorted;
}

function renderDividendRows(tbody, items) {
  const total = items.reduce((s, p) => s + p.dividendRate * p.quantity, 0);
  const sorted = sortDividendItems(items);
  tbody.innerHTML = sorted.map((p) => {
    const annual = p.dividendRate * p.quantity;
    const pct = total > 0 ? (annual / total * 100).toFixed(1) : '0.0';
    return `<tr class="clickable" data-ticker="${p.ticker}">
      <td style="font-weight:bold">${p.ticker}</td>
      <td>${formatShares(p.quantity)}</td>
      <td>${formatCurrency(p.dividendRate)}</td>
      <td style="font-weight:bold;color:#1565C0">${formatCurrency(annual)}</td>
      <td>${pct}%</td>
    </tr>`;
  }).join('');

  tbody.querySelectorAll('.clickable').forEach(row => {
    row.addEventListener('click', () => navigate(`#/item/${row.dataset.ticker}`));
  });
}

function bindDividendTable(content, type, items) {
  const tbody = document.getElementById(`div-tbody-${type}`);
  renderDividendRows(tbody, items);

  content.querySelector(`#div-sort-${type}`).addEventListener('click', (e) => {
    const btn = e.target.closest('[data-sort]');
    if (!btn) return;
    const newSort = btn.dataset.sort;
    if (divSortBy === newSort) divSortAsc = !divSortAsc;
    else { divSortBy = newSort; divSortAsc = false; }
    content.querySelector(`#div-sort-${type}`).querySelectorAll('.btn').forEach(b => {
      b.classList.toggle('btn-primary', b.dataset.sort === divSortBy);
      b.classList.toggle('btn-outline', b.dataset.sort !== divSortBy);
    });
    renderDividendRows(tbody, items);
  });
}

function renderExplodingPie(canvas, items) {
  if (!canvas || !items.length) return;
  const ctx = canvas.getContext('2d');
  const dpr = window.devicePixelRatio || 1;
  const w = canvas.parentElement.clientWidth;
  const h = 200;
  canvas.width = w * dpr;
  canvas.height = h * dpr;
  canvas.style.width = w + 'px';
  canvas.style.height = h + 'px';
  ctx.scale(dpr, dpr);

  const data = items.map(p => p.dividendRate * p.quantity);
  const total = data.reduce((s, v) => s + v, 0);
  if (total === 0) return;

  const maxIdx = data.reduce((mi, v, i) => v > data[mi] ? i : mi, 0);
  const cx = w / 2;
  const cy = h / 2;
  const explodeOffset = 10;
  const radius = Math.min(cx, cy) - explodeOffset - 10;
  let startAngle = -Math.PI / 2;

  data.forEach((val, i) => {
    const sweep = (val / total) * 2 * Math.PI;
    const midAngle = startAngle + sweep / 2;
    let ox = 0, oy = 0;
    if (i === maxIdx) {
      ox = Math.cos(midAngle) * explodeOffset;
      oy = Math.sin(midAngle) * explodeOffset;
    }

    ctx.beginPath();
    ctx.moveTo(cx + ox, cy + oy);
    ctx.arc(cx + ox, cy + oy, radius, startAngle, startAngle + sweep);
    ctx.closePath();
    ctx.fillStyle = PIE_COLORS[i % PIE_COLORS.length];
    ctx.fill();

    if (sweep > 0.25) {
      const lx = cx + ox + Math.cos(midAngle) * radius * 0.65;
      const ly = cy + oy + Math.sin(midAngle) * radius * 0.65;
      ctx.fillStyle = '#fff';
      ctx.font = '11px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(items[i].ticker, lx, ly);
    }
    startAngle += sweep;
  });
}

function positionRow(p) {
  const dayChange = p.currentPrice - (p.value / (p.quantity || 1) - p.dayGainLoss / (p.quantity || 1));
  const dayPct = p.currentPrice > 0 && p.quantity > 0 ? (p.dayGainLoss / (p.value - p.dayGainLoss)) * 100 : 0;
  const annualDiv = (p.dividendRate || 0) * (p.quantity || 0);
  return `
    <div class="position-row" data-ticker="${p.ticker}">
      ${tickerIcon(p.ticker, p.name)}
      <div class="position-info">
        <div class="position-ticker">${p.ticker}</div>
        <div class="position-name">${p.name || ''}</div>
        <div class="position-price">${formatCurrency(p.currentPrice)} <span class="${gainLossClass(p.dayGainLoss)}">${formatSignedCurrency(p.dayGainLoss / (p.quantity || 1))} ${formatPercent(dayPct)}</span></div>
        ${annualDiv > 0 ? `<div class="text-xs text-muted">Div: ${formatCurrency(annualDiv)}/yr</div>` : ''}
      </div>
      <div class="position-right">
        <div class="position-value">${formatCurrency(p.value)}</div>
        <span class="badge ${gainLossBadgeClass(p.dayGainLoss)}">${formatSignedCurrency(p.dayGainLoss)}</span>
      </div>
      <button class="btn btn-sm btn-outline edit-pos-btn" data-edit-ticker="${p.ticker}" title="Edit" style="margin-left:4px;padding:4px 8px">&#9998;</button>
    </div>
  `;
}
