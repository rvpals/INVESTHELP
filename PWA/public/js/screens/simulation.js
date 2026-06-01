import { yahoo } from '../api.js';
import { renderLineChart } from '../components/line-chart.js';
import { formatCurrency, formatSignedCurrency, formatPercent, gainLossClass } from '../utils/format.js';
import { showToast } from '../components/confirm-dialog.js';

const RANGES = [
  { label: '1W', range: '5d', interval: '1d' }, { label: '2W', range: '14d', interval: '1d' },
  { label: '1M', range: '1mo', interval: '1d' }, { label: '3M', range: '3mo', interval: '1d' },
  { label: '6M', range: '6mo', interval: '1d' }, { label: '1Y', range: '1y', interval: '1wk' },
  { label: '2Y', range: '2y', interval: '1wk' }, { label: '5Y', range: '5y', interval: '1wk' },
  { label: '10Y', range: '10y', interval: '1mo' }, { label: 'MAX', range: 'max', interval: '1mo' },
];

export async function render(container, { ticker, shares } = {}) {
  container.innerHTML = `<div class="screen">
    <h2 class="mb-16">Simulation</h2>
    <div class="form-row mb-8">
      <div class="form-group"><label>Ticker</label><input type="text" class="input" id="sim-ticker" value="${ticker || ''}" style="text-transform:uppercase"></div>
      <div class="form-group"><label>Shares</label><input type="number" class="input" id="sim-shares" value="${shares || 1}" step="any"></div>
    </div>
    <div class="chip-row mb-8">${RANGES.map((r, i) => `<button class="chip${i===0?' selected':''}" data-idx="${i}">${r.label}</button>`).join('')}</div>
    <button class="btn btn-primary w-full mb-16" id="run-sim">Run Simulation</button>
    <div id="sim-result"></div>
  </div>`;

  let selectedIdx = 0;
  container.querySelectorAll('.chip').forEach(c => {
    c.addEventListener('click', () => {
      container.querySelectorAll('.chip').forEach(x => x.classList.remove('selected'));
      c.classList.add('selected');
      selectedIdx = parseInt(c.dataset.idx);
    });
  });

  document.getElementById('run-sim').addEventListener('click', () => runSim(selectedIdx));
  if (ticker) runSim(0);
}

async function runSim(idx) {
  const ticker = document.getElementById('sim-ticker').value.toUpperCase().trim();
  const numShares = parseFloat(document.getElementById('sim-shares').value) || 1;
  const resultEl = document.getElementById('sim-result');
  if (!ticker) { showToast('Enter a ticker'); return; }

  resultEl.innerHTML = '<div class="spinner"></div>';
  try {
    const r = RANGES[idx];
    const data = await yahoo.history(ticker, r.range, r.interval);
    if (!data.length) { resultEl.innerHTML = '<div class="text-muted">No data</div>'; return; }

    const startPrice = data[0].close;
    const endPrice = data[data.length - 1].close;
    const pl = (endPrice - startPrice) * numShares;
    const plPct = startPrice > 0 ? ((endPrice - startPrice) / startPrice) * 100 : 0;

    resultEl.innerHTML = `
      <div class="card p-12 mb-8">
        <div class="flex justify-between"><span class="text-muted">Start Price</span><span>${formatCurrency(startPrice)}</span></div>
        <div class="flex justify-between"><span class="text-muted">Current Price</span><span>${formatCurrency(endPrice)}</span></div>
        <div class="flex justify-between mt-8"><span class="text-bold">P/L (${numShares} shares)</span><span class="text-bold ${gainLossClass(pl)}">${formatSignedCurrency(pl)} (${formatPercent(plPct)})</span></div>
      </div>
      <div class="chart-container"><canvas id="sim-chart"></canvas></div>
    `;
    const canvas = document.getElementById('sim-chart');
    renderLineChart(canvas, data.map(d => ({ x: d.timestamp, y: d.close })), { height: 220 });
  } catch (err) {
    resultEl.innerHTML = `<div class="text-muted">Error: ${err.message}</div>`;
  }
}
