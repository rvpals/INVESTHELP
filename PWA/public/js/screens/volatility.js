import { volatility as volatilityApi } from '../api.js';
import { formatCurrency, formatShares } from '../utils/format.js';

const SCALE_LEVELS = [
  ['Low',       '< 15%',  '#388E3C'],
  ['Moderate',  '15–30%', '#F57C00'],
  ['High',      '30–60%', '#E64A19'],
  ['Very High', '> 60%',  '#B71C1C'],
];

const LABEL_COLORS = {
  'Low':       '#388E3C',
  'Moderate':  '#F57C00',
  'High':      '#E64A19',
  'Very High': '#B71C1C',
};

function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r},${g},${b},${alpha})`;
}

function drawRangeBar(positionPct) {
  const canvas = document.getElementById('range-bar');
  if (!canvas) return;
  const dpr = window.devicePixelRatio || 1;
  const w = canvas.offsetWidth;
  const h = 28;
  canvas.width = w * dpr;
  canvas.height = h * dpr;
  const ctx = canvas.getContext('2d');
  ctx.scale(dpr, dpr);

  const style = getComputedStyle(document.documentElement);
  const primaryColor = style.getPropertyValue('--primary').trim() || '#6750A4';
  const trackColor = style.getPropertyValue('--surface-variant').trim() || '#E7E0EC';

  const trackH = 8;
  const trackY = h / 2;
  const dotR = 10;
  const dotX = Math.max(dotR, Math.min(w - dotR, w * positionPct / 100));

  // Background track
  ctx.beginPath();
  ctx.roundRect(0, trackY - trackH / 2, w, trackH, trackH / 2);
  ctx.fillStyle = trackColor;
  ctx.fill();

  // Filled portion (low → current position)
  if (dotX > 0) {
    ctx.beginPath();
    ctx.roundRect(0, trackY - trackH / 2, dotX, trackH, trackH / 2);
    ctx.fillStyle = primaryColor;
    ctx.fill();
  }

  // White fill dot
  ctx.beginPath();
  ctx.arc(dotX, trackY, dotR, 0, Math.PI * 2);
  ctx.fillStyle = '#ffffff';
  ctx.fill();

  // Primary border dot
  ctx.beginPath();
  ctx.arc(dotX, trackY, dotR, 0, Math.PI * 2);
  ctx.strokeStyle = primaryColor;
  ctx.lineWidth = 2.5;
  ctx.stroke();
}

export async function render(container, { ticker, shares }) {
  const numShares = parseFloat(shares) || 0;

  container.innerHTML = `<div class="screen">
    <div class="flex items-center gap-8 mb-16">
      <button class="btn btn-sm btn-outline" onclick="history.back()">&#8592; Back</button>
      <h2 style="flex:1">52-Week Volatility</h2>
      <button class="btn btn-sm btn-outline" id="btn-refresh">&#8635; Refresh</button>
    </div>
    <div id="vol-content">
      <div class="text-center p-32">
        <div class="spinner"></div>
        <div class="text-muted mt-12">Fetching market data…</div>
      </div>
    </div>
  </div>`;

  document.getElementById('btn-refresh')?.addEventListener('click', () => loadData(true));

  async function loadData(force = false) {
    const contentEl = document.getElementById('vol-content');
    if (!contentEl) return;
    contentEl.innerHTML = `<div class="text-center p-32">
      <div class="spinner"></div>
      <div class="text-muted mt-12">Fetching market data…</div>
    </div>`;

    try {
      const data = await volatilityApi.get(ticker, force);
      renderContent(contentEl, data, numShares);
    } catch (err) {
      contentEl.innerHTML = `<div class="card p-24" style="text-align:center">
        <div class="text-bold mb-8">Failed to load data</div>
        <div class="text-sm text-muted mb-16">${err.message}</div>
        <button class="btn btn-outline" id="btn-retry">Retry</button>
      </div>`;
      document.getElementById('btn-retry')?.addEventListener('click', () => loadData(true));
    }
  }

  loadData();
}

function renderContent(contentEl, data, numShares) {
  const labelColor = LABEL_COLORS[data.volatilityLabel] || '#616161';
  const labelBg = hexToRgba(labelColor, 0.12);
  const labelBorder = hexToRgba(labelColor, 0.4);

  const scaleCells = SCALE_LEVELS.map(([label, range, color]) => {
    const isActive = label === data.volatilityLabel;
    return `<div style="border-radius:8px;padding:6px 4px;text-align:center;
      background:${isActive ? hexToRgba(color, 0.15) : 'var(--surface-variant)'};
      border:1.5px solid ${isActive ? color : 'transparent'}">
      <div style="font-size:11px;font-weight:${isActive ? '700' : '400'};
        color:${isActive ? color : 'var(--on-surface-variant,#49454F)'};line-height:1.3">${label}</div>
      <div style="font-size:10px;opacity:${isActive ? '0.85' : '0.6'};
        color:${isActive ? color : 'var(--on-surface-variant,#49454F)'}">${range}</div>
    </div>`;
  }).join('');

  contentEl.innerHTML = `
    <!-- Position Value -->
    <div class="card p-16 mb-12"
      style="background:var(--primary-container);color:var(--on-primary-container);text-align:center">
      <div class="text-lg text-bold">${data.ticker}</div>
      ${data.companyName
        ? `<div class="text-sm" style="opacity:0.7;margin-top:2px">${data.companyName}</div>`
        : ''}
      <div class="text-xs mt-8" style="opacity:0.6">Position Value</div>
      <div style="font-size:36px;font-weight:700;margin:4px 0;line-height:1.1">
        ${formatCurrency(data.currentPrice * numShares)}
      </div>
      <div class="text-sm" style="opacity:0.7">
        ${formatCurrency(data.currentPrice)} × ${formatShares(numShares)} shares
      </div>
    </div>

    <!-- 52-Week Range -->
    <div class="card p-16 mb-12">
      <h3 class="mb-16">52-Week Range</h3>
      <canvas id="range-bar" style="width:100%;height:28px;display:block"></canvas>
      <div class="flex justify-between mt-8">
        <div>
          <div class="text-xs text-muted">52W Low</div>
          <div class="text-sm text-bold">${formatCurrency(data.low52w)}</div>
        </div>
        <div style="text-align:right">
          <div class="text-xs text-muted">52W High</div>
          <div class="text-sm text-bold">${formatCurrency(data.high52w)}</div>
        </div>
      </div>
      <div style="height:1px;background:var(--outline);opacity:0.3;margin:12px 0"></div>
      <div class="flex items-center justify-between">
        <div>
          <div class="text-xs text-muted">Current Price</div>
          <div class="text-bold">${formatCurrency(data.currentPrice)}</div>
        </div>
        <span class="badge"
          style="background:var(--secondary-container,#E8DEF8);color:var(--on-secondary-container,#1D192B)">
          ${data.rangePositionPct.toFixed(1)}% of range
        </span>
      </div>
    </div>

    <!-- Annualized Volatility -->
    <div class="card p-16 mb-12">
      <h3 class="mb-16">Annualized Volatility</h3>
      <div class="text-center mb-16">
        <div style="font-size:48px;font-weight:700;line-height:1;color:${labelColor}">
          ${data.annualizedVolPct.toFixed(1)}%
        </div>
        <div class="mt-10">
          <span class="badge"
            style="background:${labelBg};color:${labelColor};border:1.5px solid ${labelBorder};
                   font-size:13px;padding:5px 18px;font-weight:700">
            ${data.volatilityLabel.toUpperCase()}
          </span>
        </div>
      </div>
      <div style="height:1px;background:var(--outline);opacity:0.3;margin:12px 0"></div>
      <div class="flex justify-between text-sm mb-6">
        <span class="text-muted">Daily Std Dev</span>
        <span class="text-bold">${data.dailyStdDevPct.toFixed(2)}%</span>
      </div>
      <div class="flex justify-between text-sm mb-6">
        <span class="text-muted">Annualized (×√252)</span>
        <span class="text-bold">${data.annualizedVolPct.toFixed(1)}%</span>
      </div>
      <div class="flex justify-between text-sm mb-6">
        <span class="text-muted">Trading sessions</span>
        <span class="text-bold">${data.sampleCount}</span>
      </div>
      <div class="flex justify-between text-sm mb-6">
        <span class="text-muted">Method</span>
        <span class="text-bold">Log returns, sample σ</span>
      </div>
      <div style="height:1px;background:var(--outline);opacity:0.3;margin:12px 0"></div>
      <div class="text-xs text-muted mb-8">Volatility Scale</div>
      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:6px">
        ${scaleCells}
      </div>
    </div>
  `;

  requestAnimationFrame(() => drawRangeBar(data.rangePositionPct));
}
