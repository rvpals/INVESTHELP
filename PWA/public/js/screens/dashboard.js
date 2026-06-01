import { positions, changeHistory, yahoo } from '../api.js';
import { navigate } from '../router.js';
import { collapsibleCard, initCollapsibleCards } from '../components/collapsible-card.js';
import { tickerIcon } from '../components/ticker-icon.js';
import { renderMiniChart } from '../components/mini-chart.js';
import { renderPieChart } from '../components/pie-chart.js';
import { formatCurrency, formatPercent, formatSignedCurrency, gainLossClass, gainLossBadgeClass } from '../utils/format.js';
import { formatDate, formatDateTime } from '../utils/dates.js';
import { getPref } from '../preferences.js';

export async function render(container) {
  const [posData, summary, history] = await Promise.all([
    positions.list(),
    positions.summary(),
    changeHistory.list().catch(() => []),
  ]);

  const sortedByGL = [...posData].sort((a, b) => b.dayGainLoss - a.dayGainLoss);
  const gainers = sortedByGL.filter(p => p.dayGainLoss > 0).slice(0, 5);
  const losers = sortedByGL.filter(p => p.dayGainLoss < 0).reverse().slice(0, 5);

  const refreshedAt = getPref('last_refreshed_at');
  const refreshLabel = refreshedAt > 0 ? `Refreshed: ${formatDateTime(refreshedAt)}` : '';

  container.innerHTML = `<div class="screen">
    ${collapsibleCard('portfolio_summary', 'Portfolio Summary', `
      <div class="text-center">
        <div class="text-2xl text-bold">${formatCurrency(summary.totalValue)}</div>
        <div class="mt-8 ${gainLossClass(summary.dayGainLoss)}">
          ${formatSignedCurrency(summary.dayGainLoss)} (${formatPercent(summary.dayPercent)}) today
        </div>
        <div class="chart-container mt-8"><canvas id="summary-chart"></canvas></div>
        ${refreshLabel ? `<div class="text-xs text-muted mt-8">${refreshLabel}</div>` : ''}
      </div>
    `, { defaultPinned: true })}

    ${collapsibleCard('market_indices', 'Market Indices', `
      <div class="flex gap-8 overflow-x" id="indices-row" style="padding-bottom:4px">
        <div class="text-sm text-muted">Loading indices...</div>
      </div>
    `)}

    ${collapsibleCard('daily_glance', 'Daily Glance', `
      <div class="mb-8">
        <div class="flex justify-between py-8">
          <div><span class="text-sm text-muted">Stock Daily:</span> <span class="${gainLossClass(summary.stockValue > 0 ? 1 : -1)}">${formatSignedCurrency(posData.filter(p=>p.type==='Stock').reduce((s,p)=>s+p.dayGainLoss,0))}</span></div>
          <div><span class="text-sm text-muted">ETF Daily:</span> <span class="${gainLossClass(summary.etfValue > 0 ? 1 : -1)}">${formatSignedCurrency(posData.filter(p=>p.type==='ETF').reduce((s,p)=>s+p.dayGainLoss,0))}</span></div>
        </div>
      </div>
      ${gainers.length ? `<h3 class="text-sm text-bold mb-8">Top Gainers</h3>` : ''}
      ${gainers.map(p => glanceRow(p)).join('')}
      ${losers.length ? `<h3 class="text-sm text-bold mb-8 mt-8">Top Losers</h3>` : ''}
      ${losers.map(p => glanceRow(p)).join('')}
      ${!gainers.length && !losers.length ? '<div class="text-sm text-muted text-center py-8">No daily changes</div>' : ''}
    `)}

    ${collapsibleCard('positions', 'Positions', `
      <div class="chart-container"><canvas id="positions-pie"></canvas></div>
      <div id="pie-legend" class="mt-8"></div>
    `)}

    ${collapsibleCard('position_details', 'Position Details', `
      <div class="data-table-wrapper">
        <table class="data-table">
          <thead><tr><th>Ticker</th><th>Shares</th><th>Price</th><th>Value</th></tr></thead>
          <tbody>
            ${posData.map(p => `
              <tr class="clickable" data-ticker="${p.ticker}">
                <td>${tickerIcon(p.ticker, p.name, {small:true})} ${p.ticker}</td>
                <td>${p.quantity}</td>
                <td>${formatCurrency(p.currentPrice)}</td>
                <td>${formatCurrency(p.value)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
    `)}
  </div>`;

  initCollapsibleCards(container);

  // Clickable rows
  container.querySelectorAll('[data-ticker]').forEach(el => {
    el.addEventListener('click', () => navigate(`#/item/${el.dataset.ticker}`));
  });

  // Render charts
  const summaryCanvas = document.getElementById('summary-chart');
  if (summaryCanvas && history.length >= 2) {
    const sorted = [...history].sort((a, b) => a.date - b.date);
    renderMiniChart(summaryCanvas, sorted.map(h => ({ y: h.totalValue })));
  }

  const pieCanvas = document.getElementById('positions-pie');
  if (pieCanvas && posData.length > 0) {
    renderPieChart(pieCanvas, posData.map(p => ({ label: p.ticker, value: p.value })));
  }

  // Load market indices
  loadMarketIndices();
}

function glanceRow(p) {
  return `
    <div class="flex items-center justify-between py-8 clickable" data-ticker="${p.ticker}" style="border-bottom:1px solid color-mix(in srgb, var(--outline) 20%, transparent)">
      <div>
        <span class="text-bold">${p.ticker}</span>
        <span class="text-xs text-muted" style="margin-left:6px">${p.name}</span>
      </div>
      <span class="badge ${gainLossBadgeClass(p.dayGainLoss)}">${formatSignedCurrency(p.dayGainLoss)}</span>
    </div>
  `;
}

async function loadMarketIndices() {
  const indicesStr = getPref('market_indices') || '^IXIC,^GSPC,^DJI,GC=F';
  const tickers = indicesStr.split(',');
  const labels = { '^IXIC': 'NASDAQ', '^GSPC': 'S&P 500', '^DJI': 'Dow', 'GC=F': 'Gold', '^RUT': 'Russell 2K', 'SI=F': 'Silver', 'CL=F': 'Oil', 'BTC-USD': 'Bitcoin' };
  const row = document.getElementById('indices-row');
  if (!row) return;

  const cards = [];
  for (const t of tickers) {
    try {
      const q = await yahoo.quote(t);
      const change = q.price - q.previousClose;
      const pct = q.previousClose ? (change / q.previousClose) * 100 : 0;
      cards.push(`
        <div class="index-card" onclick="window.open('https://finance.yahoo.com/quote/${encodeURIComponent(t)}','_blank')">
          <div class="index-label">${labels[t] || t}</div>
          <div class="index-price">${formatCurrency(q.price)}</div>
          <div class="index-change ${gainLossClass(change)}">${formatSignedCurrency(change)} (${formatPercent(pct)})</div>
        </div>
      `);
    } catch {
      cards.push(`<div class="index-card"><div class="index-label">${labels[t] || t}</div><div class="text-xs text-muted">Unavailable</div></div>`);
    }
  }
  row.innerHTML = cards.join('');
}
