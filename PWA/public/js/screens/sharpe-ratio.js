import { sharpe as sharpeApi } from '../api.js';
import { formatPercent } from '../utils/format.js';

// ── Interpretation ────────────────────────────────────────────────────────────

const INTERPRETATIONS = [
  { threshold: 1.0, label: 'Subpar',     color: '#C62828' },
  { threshold: 2.0, label: 'Good',       color: '#2E7D32' },
  { threshold: 3.0, label: 'Very Good',  color: '#0D47A1' },
  { threshold: Infinity, label: 'Exceptional', color: '#6A1B9A' },
];

function interpretSharpe(value) {
  return INTERPRETATIONS.find(i => value < i.threshold) || INTERPRETATIONS[INTERPRETATIONS.length - 1];
}

const LOOKBACK_OPTIONS = [
  { label: '6 months', days: 180 },
  { label: '1 year',   days: 365 },
  { label: '2 years',  days: 730 },
];

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmtPct(decimal) {
  if (decimal == null || !isFinite(decimal)) return '—';
  return (decimal * 100).toFixed(1) + '%';
}

function fmtDate(epochSeconds) {
  const d = new Date(epochSeconds * 1000);
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

// ── Canvas chart ──────────────────────────────────────────────────────────────

function drawReturnChart(canvas, returnSeries) {
  const dpr = window.devicePixelRatio || 1;
  const cssW = canvas.offsetWidth;
  const cssH = canvas.offsetHeight;
  canvas.width  = cssW * dpr;
  canvas.height = cssH * dpr;
  const ctx = canvas.getContext('2d');
  ctx.scale(dpr, dpr);

  if (!returnSeries || returnSeries.length < 2) return;

  const PAD_LEFT = 48, PAD_RIGHT = 8, PAD_TOP = 10, PAD_BOTTOM = 28;
  const chartW = cssW - PAD_LEFT - PAD_RIGHT;
  const chartH = cssH - PAD_TOP  - PAD_BOTTOM;

  const returnsPercent = returnSeries.map(p => p.return * 100);
  const peak = Math.max(...returnsPercent.map(Math.abs));
  const yRange = Math.max(peak, 0.5);

  function yOf(pct) {
    return PAD_TOP + chartH * (1 - (pct + yRange) / (2 * yRange));
  }
  function xOf(i) {
    return PAD_LEFT + (i / (returnSeries.length - 1)) * chartW;
  }

  const zeroY = yOf(0);
  const isDark = document.documentElement.classList.contains('dark') ||
    window.matchMedia('(prefers-color-scheme: dark)').matches;
  const gridColor     = isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)';
  const zeroLineColor = isDark ? 'rgba(255,255,255,0.35)' : 'rgba(0,0,0,0.30)';
  const labelColor    = isDark ? '#9e9e9e' : '#757575';
  const lineColor     = isDark ? '#90CAF9' : '#1565C0';

  // Grid lines
  const gridLevels = [-yRange, -yRange / 2, 0, yRange / 2, yRange];
  gridLevels.forEach(level => {
    const y = yOf(level);
    ctx.beginPath();
    ctx.moveTo(PAD_LEFT, y);
    ctx.lineTo(PAD_LEFT + chartW, y);
    ctx.strokeStyle = level === 0 ? zeroLineColor : gridColor;
    ctx.lineWidth   = level === 0 ? 1.5 : 1;
    ctx.stroke();
  });

  // Build full fill path (from zeroY → data → back to zeroY)
  ctx.beginPath();
  ctx.moveTo(xOf(0), zeroY);
  returnSeries.forEach((p, i) => ctx.lineTo(xOf(i), yOf(p.return * 100)));
  ctx.lineTo(xOf(returnSeries.length - 1), zeroY);
  ctx.closePath();
  const fillPath = new Path2D(ctx.getPath ? undefined : undefined);

  // We re-draw the path twice with different clips
  function drawFill(clipTop, clipBottom, fillStyle) {
    ctx.save();
    ctx.beginPath();
    ctx.rect(PAD_LEFT, clipTop, chartW, clipBottom - clipTop);
    ctx.clip();
    ctx.beginPath();
    ctx.moveTo(xOf(0), zeroY);
    returnSeries.forEach((p, i) => ctx.lineTo(xOf(i), yOf(p.return * 100)));
    ctx.lineTo(xOf(returnSeries.length - 1), zeroY);
    ctx.closePath();
    ctx.fillStyle = fillStyle;
    ctx.fill();
    ctx.restore();
  }

  drawFill(PAD_TOP,  zeroY,              'rgba(46,125,50,0.25)');   // green above zero
  drawFill(zeroY,    PAD_TOP + chartH,   'rgba(198,40,40,0.25)');   // red below zero

  // Return line
  ctx.beginPath();
  returnSeries.forEach((p, i) => {
    const x = xOf(i), y = yOf(p.return * 100);
    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
  });
  ctx.strokeStyle = lineColor;
  ctx.lineWidth = 1.5;
  ctx.lineJoin = 'round';
  ctx.stroke();

  // Y-axis labels
  ctx.font = '11px system-ui, sans-serif';
  ctx.fillStyle = labelColor;
  ctx.textAlign = 'right';
  gridLevels.forEach(level => {
    ctx.fillText(`${level.toFixed(1)}%`, PAD_LEFT - 4, yOf(level) + 4);
  });

  // X-axis labels (5 evenly spaced)
  ctx.textAlign = 'center';
  const labelIndices = [
    0,
    Math.floor(returnSeries.length / 4),
    Math.floor(returnSeries.length / 2),
    Math.floor(returnSeries.length * 3 / 4),
    returnSeries.length - 1,
  ].filter((v, i, a) => a.indexOf(v) === i);

  labelIndices.forEach(i => {
    ctx.fillText(
      fmtDate(returnSeries[i].timestamp),
      xOf(i),
      PAD_TOP + chartH + 18
    );
  });

  return { xOf, yOf, chartW, PAD_LEFT, PAD_TOP, chartH };
}

function drawTooltip(canvas, returnSeries, hoveredIndex, coords) {
  if (hoveredIndex < 0 || !coords) return;

  const dpr = window.devicePixelRatio || 1;
  const cssW = canvas.offsetWidth;
  const { xOf, yOf, PAD_LEFT, PAD_TOP, chartH } = coords;
  const ctx = canvas.getContext('2d');
  ctx.scale ? undefined : ctx.scale(dpr, dpr);   // scale already applied

  const p = returnSeries[hoveredIndex];
  const sx = xOf(hoveredIndex);
  const sy = yOf(p.return * 100);
  const retPct = p.return * 100;

  // Vertical crosshair
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(sx, PAD_TOP);
  ctx.lineTo(sx, PAD_TOP + chartH);
  ctx.strokeStyle = 'rgba(128,128,128,0.5)';
  ctx.lineWidth = 1;
  ctx.stroke();

  // Dot
  ctx.beginPath();
  ctx.arc(sx, sy, 5, 0, 2 * Math.PI);
  ctx.fillStyle = retPct >= 0 ? '#2E7D32' : '#C62828';
  ctx.fill();

  // Tooltip text
  const label = `${fmtDate(p.timestamp)}: ${retPct >= 0 ? '+' : ''}${retPct.toFixed(3)}%`;
  ctx.font = 'bold 11px system-ui, sans-serif';
  ctx.fillStyle = document.documentElement.classList.contains('dark') ? '#e0e0e0' : '#212121';
  const tx = Math.max(PAD_LEFT + 4, Math.min(sx, cssW - 90));
  ctx.fillText(label, tx, PAD_TOP + 14);
  ctx.restore();
}

// ── Main render ───────────────────────────────────────────────────────────────

export async function render(container) {
  let riskFreeRatePercent = '5.0';
  let lookbackDays = 365;
  let currentResult = null;
  let computing = false;

  container.innerHTML = `<div class="screen">
    <div class="flex items-center gap-8 mb-16">
      <button class="btn btn-sm btn-outline" onclick="history.back()">&#8592; Back</button>
      <h2 style="flex:1">Sharpe Ratio</h2>
      <button class="btn btn-sm btn-outline" id="btn-recalc" title="Recalculate">&#8635; Recalculate</button>
    </div>

    <!-- Parameters card -->
    <div class="card mb-12" id="params-card">
      <div class="card-title mb-12">Parameters</div>
      <div class="flex items-center gap-12 mb-12">
        <label style="font-size:13px;color:var(--text-muted)">Risk-Free Rate</label>
        <div class="flex items-center gap-4">
          <input type="number" class="input" id="rf-rate-input" value="${riskFreeRatePercent}"
            step="0.1" min="0" max="20" style="width:80px;text-align:right">
          <span style="font-size:13px">%</span>
        </div>
      </div>
      <div class="mb-12">
        <div style="font-size:12px;color:var(--text-muted);margin-bottom:6px">Lookback Period</div>
        <div class="flex gap-8" id="lookback-chips">
          ${LOOKBACK_OPTIONS.map(o => `
            <button class="chip ${o.days === lookbackDays ? 'chip-selected' : ''}"
              data-days="${o.days}">${o.label}</button>
          `).join('')}
        </div>
      </div>
      <div class="flex justify-end">
        <button class="btn btn-primary" id="btn-calculate">Calculate</button>
      </div>
    </div>

    <div id="sharpe-body"></div>
  </div>`;

  // Wire up parameters
  const rfInput = document.getElementById('rf-rate-input');
  rfInput.addEventListener('change', () => { riskFreeRatePercent = rfInput.value; });

  document.getElementById('lookback-chips').addEventListener('click', e => {
    const btn = e.target.closest('[data-days]');
    if (!btn) return;
    lookbackDays = parseInt(btn.dataset.days, 10);
    document.querySelectorAll('#lookback-chips .chip').forEach(c => {
      c.classList.toggle('chip-selected', parseInt(c.dataset.days, 10) === lookbackDays);
    });
  });

  const compute = () => {
    if (computing) return;
    const rf = parseFloat(rfInput.value);
    if (isNaN(rf) || rf < 0 || rf > 100) {
      showError('Risk-Free Rate must be a number between 0 and 100.');
      return;
    }
    runCompute(rf / 100, lookbackDays);
  };

  document.getElementById('btn-calculate').addEventListener('click', compute);
  document.getElementById('btn-recalc').addEventListener('click', compute);

  // Auto-run on load
  runCompute(parseFloat(riskFreeRatePercent) / 100, lookbackDays);

  // ── Compute ──

  async function runCompute(rf, days) {
    computing = true;
    showLoading('Loading positions…');
    try {
      const result = await sharpeApi.compute(rf, days);
      currentResult = result;
      showResult(result);
    } catch (err) {
      showError(err.message || 'Computation failed');
    } finally {
      computing = false;
    }
  }

  // ── State renderers ──

  function showLoading(msg) {
    document.getElementById('sharpe-body').innerHTML = `
      <div class="card text-center p-32">
        <div class="spinner mb-12" style="margin:0 auto"></div>
        <div class="text-muted text-sm">${msg}</div>
      </div>`;
  }

  function showError(msg) {
    document.getElementById('sharpe-body').innerHTML = `
      <div class="card p-20 text-center">
        <div style="font-size:24px;margin-bottom:8px">&#9888;</div>
        <div class="text-sm" style="color:var(--error,#b3261e);margin-bottom:16px">${msg}</div>
        <button class="btn btn-outline" id="btn-retry">Retry</button>
      </div>`;
    document.getElementById('btn-retry')?.addEventListener('click', () => {
      const rf = parseFloat(rfInput.value) / 100;
      runCompute(rf, lookbackDays);
    });
  }

  function showResult(result) {
    const body = document.getElementById('sharpe-body');
    body.innerHTML = '';

    // Result card
    body.appendChild(buildResultCard(result));

    // Metrics card
    body.appendChild(buildMetricsCard(result));

    // Chart card
    if (result.portfolioReturnSeries && result.portfolioReturnSeries.length >= 2) {
      body.appendChild(buildChartCard(result.portfolioReturnSeries));
    }

    // Skipped tickers
    if (result.skippedTickers && result.skippedTickers.length > 0) {
      body.appendChild(buildSkippedCard(result.skippedTickers, result.skipReasons));
    }
  }

  // ── Card builders ──

  function buildResultCard(result) {
    const div = document.createElement('div');
    div.className = 'card mb-12';

    if (result.sharpeRatio != null) {
      const interp = interpretSharpe(result.sharpeRatio);
      div.innerHTML = `
        <div class="text-center p-8">
          <div class="flex items-center justify-center gap-14 mb-6">
            <span style="font-size:48px;font-weight:700;line-height:1">${result.sharpeRatio}</span>
            <span style="
              padding:4px 14px;
              border-radius:16px;
              font-size:13px;
              font-weight:600;
              background:${interp.color}26;
              color:${interp.color}
            ">${interp.label}</span>
          </div>
          <div style="font-size:12px;color:var(--text-muted)">Sharpe Ratio</div>
        </div>`;
    } else {
      div.innerHTML = `
        <div class="text-center p-20">
          <div style="font-size:28px;margin-bottom:8px">&#128202;</div>
          <div style="font-size:16px;font-weight:600;margin-bottom:8px">Unable to compute</div>
          ${result.insufficientDataReason
            ? `<div class="text-sm text-muted">${result.insufficientDataReason}</div>`
            : ''}
        </div>`;
    }
    return div;
  }

  function buildMetricsCard(result) {
    const rows = [
      ['Annualized Return',    fmtPct(result.annualizedReturn)],
      ['Annualized Volatility',fmtPct(result.annualizedVolatility)],
      ['Risk-Free Rate Used',  fmtPct(result.riskFreeRate)],
      ['Period',               `Last ${result.lookbackDays} calendar days`],
      ['As of Date',           result.calculationDate || '—'],
    ];

    const div = document.createElement('div');
    div.className = 'card mb-12';
    div.innerHTML = rows.map((row, i) => `
      ${i > 0 ? '<hr style="border:none;border-top:1px solid var(--border-color);margin:0">' : ''}
      <div class="flex justify-between items-center" style="padding:10px 16px">
        <span class="text-sm text-muted">${row[0]}</span>
        <span class="text-sm" style="font-weight:500">${row[1]}</span>
      </div>`).join('');
    return div;
  }

  function buildChartCard(returnSeries) {
    const div = document.createElement('div');
    div.className = 'card mb-12';
    div.innerHTML = `
      <div style="padding:16px 16px 0">
        <div style="font-size:14px;font-weight:600;margin-bottom:2px">Daily Portfolio Returns</div>
        <div class="text-xs text-muted mb-8">Each point is one trading day's weighted return</div>
      </div>
      <div style="position:relative;padding:0 4px 4px">
        <canvas id="sharpe-chart" style="width:100%;height:200px;display:block"></canvas>
      </div>`;

    // Draw after insertion into DOM so offsetWidth is accurate
    setTimeout(() => {
      const canvas = document.getElementById('sharpe-chart');
      if (!canvas) return;
      let coords = drawReturnChart(canvas, returnSeries);

      let hoveredIndex = -1;

      function redraw() {
        coords = drawReturnChart(canvas, returnSeries);
        if (hoveredIndex >= 0) drawTooltip(canvas, returnSeries, hoveredIndex, coords);
      }

      canvas.addEventListener('click', e => {
        if (!coords) return;
        const rect = canvas.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const xStep = coords.chartW / (returnSeries.length - 1);
        const idx = Math.round((mouseX - coords.PAD_LEFT) / xStep);
        hoveredIndex = Math.max(0, Math.min(idx, returnSeries.length - 1));
        redraw();
      });

      canvas.addEventListener('mousemove', e => {
        if (!coords) return;
        const rect = canvas.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const xStep = coords.chartW / (returnSeries.length - 1);
        const idx = Math.round((mouseX - coords.PAD_LEFT) / xStep);
        const newIdx = Math.max(0, Math.min(idx, returnSeries.length - 1));
        if (newIdx !== hoveredIndex) {
          hoveredIndex = newIdx;
          redraw();
        }
      });

      canvas.addEventListener('mouseleave', () => {
        hoveredIndex = -1;
        redraw();
      });

      window.addEventListener('resize', redraw);
    }, 0);

    return div;
  }

  function buildSkippedCard(tickers, reasons) {
    const div = document.createElement('div');
    div.className = 'card mb-12';
    div.innerHTML = `
      <div style="padding:12px 16px 0">
        <div style="font-size:13px;font-weight:600;color:var(--text-muted)">
          Excluded from calculation (${tickers.length})
        </div>
      </div>
      ${tickers.map((ticker, i) => `
        ${i > 0 ? '<hr style="border:none;border-top:1px solid var(--border-color);margin:0">' : ''}
        <div class="flex justify-between items-center" style="padding:8px 16px">
          <span style="font-size:13px;font-weight:500">${ticker}</span>
          <span class="text-xs text-muted" style="text-align:right;max-width:60%">
            ${reasons?.[ticker] || 'Unknown reason'}
          </span>
        </div>`).join('')}`;
    return div;
  }
}
