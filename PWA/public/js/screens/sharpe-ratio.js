import { sharpe as sharpeApi } from '../api.js';
import { formatPercent } from '../utils/format.js';
import { getPref } from '../preferences.js';

// ── Interpretation ────────────────────────────────────────────────────────────

const INTERPRETATIONS = [
  { threshold: 1.0, label: 'Subpar',      color: '#C62828' },
  { threshold: 2.0, label: 'Good',        color: '#2E7D32' },
  { threshold: 3.0, label: 'Very Good',   color: '#0D47A1' },
  { threshold: Infinity, label: 'Exceptional', color: '#6A1B9A' },
];

function interpretSharpe(value) {
  return INTERPRETATIONS.find(i => value < i.threshold) || INTERPRETATIONS[INTERPRETATIONS.length - 1];
}

const LOOKBACK_OPTIONS = [
  { label: '6M',  days: 180  },
  { label: '1Y',  days: 365  },
  { label: '2Y',  days: 730  },
  { label: '5Y',  days: 1825 },
  { label: '10Y', days: 3650 },
];

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmtPct(decimal, dp = 1) {
  if (decimal == null || !isFinite(decimal)) return '—';
  return (decimal * 100).toFixed(dp) + '%';
}

function fmtPct4(decimal) {
  if (decimal == null || !isFinite(decimal)) return '—';
  return (decimal * 100).toFixed(4) + '%';
}

function fmtDate(epochSeconds) {
  const d = new Date(epochSeconds * 1000);
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

function fmtDateTime(epochSeconds) {
  const d = new Date(epochSeconds * 1000);
  return d.toLocaleString(undefined, { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' });
}

// ── Collapsible card builder ──────────────────────────────────────────────────

function buildCollapsibleCard(title, contentHtml, defaultExpanded = false) {
  const card = document.createElement('div');
  card.className = 'card mb-12';
  const id = 'coll-' + Math.random().toString(36).slice(2);
  card.innerHTML = `
    <div class="collapsible-header" data-target="${id}"
         style="display:flex;align-items:center;justify-content:space-between;
                padding:12px 16px;cursor:pointer;user-select:none">
      <span style="font-size:14px;font-weight:600">${title}</span>
      <span class="coll-arrow" style="font-size:18px;transition:transform 0.2s">
        ${defaultExpanded ? '&#8963;' : '&#8964;'}
      </span>
    </div>
    <div id="${id}" style="display:${defaultExpanded ? 'block' : 'none'}">
      <hr style="border:none;border-top:1px solid var(--border-color);margin:0">
      <div style="padding:16px">${contentHtml}</div>
    </div>`;

  card.querySelector('.collapsible-header').addEventListener('click', () => {
    const body = document.getElementById(id);
    const arrow = card.querySelector('.coll-arrow');
    const isVisible = body.style.display !== 'none';
    body.style.display = isVisible ? 'none' : 'block';
    arrow.innerHTML = isVisible ? '&#8964;' : '&#8963;';
  });

  return card;
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

  drawFill(PAD_TOP,  zeroY,            'rgba(46,125,50,0.25)');
  drawFill(zeroY,    PAD_TOP + chartH, 'rgba(198,40,40,0.25)');

  ctx.beginPath();
  returnSeries.forEach((p, i) => {
    const x = xOf(i), y = yOf(p.return * 100);
    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
  });
  ctx.strokeStyle = lineColor;
  ctx.lineWidth = 1.5;
  ctx.lineJoin = 'round';
  ctx.stroke();

  ctx.font = '11px system-ui, sans-serif';
  ctx.fillStyle = labelColor;
  ctx.textAlign = 'right';
  gridLevels.forEach(level => {
    ctx.fillText(`${level.toFixed(1)}%`, PAD_LEFT - 4, yOf(level) + 4);
  });

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

  const p = returnSeries[hoveredIndex];
  const sx = xOf(hoveredIndex);
  const sy = yOf(p.return * 100);
  const retPct = p.return * 100;

  ctx.save();
  ctx.beginPath();
  ctx.moveTo(sx, PAD_TOP);
  ctx.lineTo(sx, PAD_TOP + chartH);
  ctx.strokeStyle = 'rgba(128,128,128,0.5)';
  ctx.lineWidth = 1;
  ctx.stroke();

  ctx.beginPath();
  ctx.arc(sx, sy, 5, 0, 2 * Math.PI);
  ctx.fillStyle = retPct >= 0 ? '#2E7D32' : '#C62828';
  ctx.fill();

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
        <div class="flex flex-wrap gap-8" id="lookback-chips">
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
    // Update button to "Recalculate" after first compute
    document.getElementById('btn-calculate').textContent = 'Recalculate';
    runCompute(rf / 100, lookbackDays);
  };

  document.getElementById('btn-calculate').addEventListener('click', compute);
  document.getElementById('btn-recalc').addEventListener('click', compute);

  // Try to load cached result on init — only compute if nothing is cached
  tryLoadCached();

  // ── Cache load ──

  async function tryLoadCached() {
    try {
      const cached = await sharpeApi.getCached();
      if (cached) {
        // Restore UI controls to match the cached computation
        rfInput.value = (cached.riskFreeRate * 100).toFixed(1);
        riskFreeRatePercent = rfInput.value;
        lookbackDays = cached.lookbackDays;
        document.querySelectorAll('#lookback-chips .chip').forEach(c => {
          c.classList.toggle('chip-selected', parseInt(c.dataset.days, 10) === lookbackDays);
        });
        document.getElementById('btn-calculate').textContent = 'Recalculate';
        currentResult = cached;
        showResult(cached);
      }
      // No cache → stay on Idle; user presses Calculate when ready
    } catch (e) {
      // Server error — stay on Idle
    }
  }

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

    // Cache banner
    if (result.fromCache && result.cachedAt) {
      body.appendChild(buildCacheBanner(result.cachedAt));
    }

    body.appendChild(buildResultCard(result));
    body.appendChild(buildMetricsCard(result));

    if (result.portfolioReturnSeries && result.portfolioReturnSeries.length >= 2) {
      body.appendChild(buildChartCard(result.portfolioReturnSeries));
    }

    // Educational & detail collapsible cards (only when Show Explanation is on)
    if (getPref('show_explanation')) {
      body.appendChild(buildAboutCard());
      if (result.tickerDetails && result.tickerDetails.length > 0) {
        body.appendChild(buildDetailCard(result));
      }
    }

    if (result.skippedTickers && result.skippedTickers.length > 0) {
      body.appendChild(buildSkippedCard(result.skippedTickers, result.skipReasons));
    }
  }

  // ── Card builders ──

  function buildCacheBanner(cachedAt) {
    const div = document.createElement('div');
    div.style.cssText = `
      display:flex;justify-content:space-between;align-items:center;
      padding:10px 16px;margin-bottom:12px;border-radius:8px;
      background:var(--secondary-container,#e8eaf6);
      color:var(--on-secondary-container,#1a237e);font-size:13px`;
    div.innerHTML = `
      <span>Cached result from ${fmtDateTime(cachedAt)}</span>
      <span style="font-size:12px;opacity:0.7">Press ↻ to refresh</span>`;
    return div;
  }

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
              padding:4px 14px;border-radius:16px;font-size:13px;font-weight:600;
              background:${interp.color}26;color:${interp.color}">${interp.label}</span>
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
        const xStep = coords.chartW / (returnSeries.length - 1);
        const idx = Math.round((e.clientX - rect.left - coords.PAD_LEFT) / xStep);
        hoveredIndex = Math.max(0, Math.min(idx, returnSeries.length - 1));
        redraw();
      });

      canvas.addEventListener('mousemove', e => {
        if (!coords) return;
        const rect = canvas.getBoundingClientRect();
        const xStep = coords.chartW / (returnSeries.length - 1);
        const idx = Math.round((e.clientX - rect.left - coords.PAD_LEFT) / xStep);
        const newIdx = Math.max(0, Math.min(idx, returnSeries.length - 1));
        if (newIdx !== hoveredIndex) { hoveredIndex = newIdx; redraw(); }
      });

      canvas.addEventListener('mouseleave', () => { hoveredIndex = -1; redraw(); });
      window.addEventListener('resize', redraw);
    }, 0);

    return div;
  }

  function buildAboutCard() {
    const interpRows = [
      ['< 1.0',     'Subpar',      '#C62828', 'Risk may outweigh the return'],
      ['1.0 – 2.0', 'Good',        '#2E7D32', 'Acceptable risk-adjusted performance'],
      ['2.0 – 3.0', 'Very Good',   '#0D47A1', 'Strong risk-adjusted performance'],
      ['≥ 3.0',     'Exceptional', '#6A1B9A', 'Outstanding risk-adjusted return'],
    ];

    const html = `
      <div style="margin-bottom:12px">
        <div style="font-size:12px;font-weight:600;color:var(--primary,#1565C0);margin-bottom:6px">Formula</div>
        <div style="background:var(--surface-variant,#f5f5f5);border-radius:6px;padding:10px 16px;
                    font-size:16px;font-weight:700;text-align:center;letter-spacing:0.5px">
          SR = (Rp − Rf) / σp
        </div>
      </div>

      <div style="margin-bottom:12px">
        <div style="font-size:12px;font-weight:600;color:var(--primary,#1565C0);margin-bottom:6px">Components</div>
        ${[
          ['Rp', 'Portfolio annualized return  (mean daily return × 252)'],
          ['Rf', 'Annual risk-free rate  (e.g., US 10-yr Treasury yield)'],
          ['σp', 'Annualized std dev of excess returns  (sample, × √252)'],
        ].map(([ sym, desc ], i) => `
          ${i > 0 ? '<hr style="border:none;border-top:1px solid var(--border-color);margin:2px 0">' : ''}
          <div class="flex gap-12" style="padding:4px 0">
            <span style="font-size:13px;font-weight:700;min-width:24px">${sym}</span>
            <span style="font-size:12px;color:var(--text-muted)">${desc}</span>
          </div>`).join('')}
      </div>

      <hr style="border:none;border-top:1px solid var(--border-color);margin:8px 0">

      <div>
        <div style="font-size:12px;font-weight:600;color:var(--primary,#1565C0);margin-bottom:6px">Interpretation</div>
        <table style="width:100%;border-collapse:collapse;font-size:12px">
          <thead>
            <tr style="background:var(--surface-variant,#f5f5f5)">
              <th style="padding:4px 8px;text-align:left;font-weight:600">Range</th>
              <th style="padding:4px 8px;text-align:left;font-weight:600">Rating</th>
              <th style="padding:4px 8px;text-align:left;font-weight:600">Meaning</th>
            </tr>
          </thead>
          <tbody>
            ${interpRows.map(([range, rating, color, meaning], i) => `
              <tr style="${i % 2 === 1 ? 'background:var(--surface-variant,#f5f5f5);opacity:0.6' : ''}">
                <td style="padding:5px 8px;border-top:1px solid var(--border-color)">${range}</td>
                <td style="padding:5px 8px;border-top:1px solid var(--border-color);
                           font-weight:600;color:${color}">${rating}</td>
                <td style="padding:5px 8px;border-top:1px solid var(--border-color);
                           color:var(--text-muted)">${meaning}</td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;

    return buildCollapsibleCard('About Sharpe Ratio', html, false);
  }

  function buildDetailCard(result) {
    const rfPct = (result.riskFreeRate * 100).toFixed(1);
    const annRetPct = (result.annualizedReturn * 100).toFixed(2);
    const annVolPct = (result.annualizedVolatility * 100).toFixed(2);
    const rfRate = result.riskFreeRate;

    // Section 1: Inputs
    const inputsHtml = `
      <div style="font-size:12px;font-weight:600;color:var(--primary,#1565C0);margin-bottom:6px">Inputs Used</div>
      ${[
        ['Risk-free rate',       `${rfPct}%  (${fmtPct4(result.dailyRfRate)} / day)`],
        ['Lookback period',      `${result.lookbackDays} calendar days`],
        ['Aligned trading days', `${result.alignedTradingDays}`],
        ['Mean daily return',    fmtPct4(result.meanDailyReturn)],
      ].map(([label, val], i) => `
        ${i > 0 ? '<hr style="border:none;border-top:1px solid var(--border-color);margin:2px 0">' : ''}
        <div class="flex justify-between" style="padding:4px 0;font-size:12px">
          <span style="color:var(--text-muted)">${label}</span>
          <span style="font-weight:500">${val}</span>
        </div>`).join('')}`;

    // Section 2: Per-ticker table
    const tickerRows = result.tickerDetails.map((d, i) => {
      const retColor = d.annualizedReturn >= 0 ? '#2E7D32' : '#C62828';
      const rowBg = i % 2 === 1 ? 'background:var(--surface-variant,#f5f5f5);opacity:0.8' : '';
      return `<tr style="${rowBg}">
        <td style="padding:5px 8px;border-top:1px solid var(--border-color);font-weight:600">${d.ticker}</td>
        <td style="padding:5px 8px;border-top:1px solid var(--border-color);text-align:right">
          ${(d.weight * 100).toFixed(1)}%</td>
        <td style="padding:5px 8px;border-top:1px solid var(--border-color);text-align:right;color:${retColor}">
          ${fmtPct(d.annualizedReturn)}</td>
        <td style="padding:5px 8px;border-top:1px solid var(--border-color);text-align:right">
          ${fmtPct(d.annualizedVolatility)}</td>
      </tr>`;
    }).join('');

    const tickerHtml = `
      <div style="font-size:12px;font-weight:600;color:var(--primary,#1565C0);margin:12px 0 6px">Per-Ticker Breakdown</div>
      <table style="width:100%;border-collapse:collapse;font-size:12px">
        <thead>
          <tr style="background:var(--surface-variant,#f5f5f5)">
            <th style="padding:4px 8px;text-align:left;font-weight:600">Ticker</th>
            <th style="padding:4px 8px;text-align:right;font-weight:600">Weight</th>
            <th style="padding:4px 8px;text-align:right;font-weight:600">Ann.Return</th>
            <th style="padding:4px 8px;text-align:right;font-weight:600">Ann.Vol</th>
          </tr>
        </thead>
        <tbody>${tickerRows}</tbody>
      </table>`;

    // Section 3: Step-by-step
    const steps = [
      ['1. Mean daily portfolio return',  fmtPct4(result.meanDailyReturn)],
      ['2. Annualized return  (× 252)',   `${annRetPct}%`],
      [`3. Daily risk-free rate  (${rfPct}% ÷ 252)`, fmtPct4(result.dailyRfRate)],
      ['4. Excess returns computed', `${result.alignedTradingDays} daily obs − Rf`],
      ['5. Annualized volatility  (σ × √252)', `${annVolPct}%`],
      [`6. Sharpe = (${annRetPct}% − ${rfPct}%) / ${annVolPct}%`,
        result.sharpeRatio != null ? String(result.sharpeRatio) : 'N/A'],
    ];

    const stepsHtml = `
      <div style="font-size:12px;font-weight:600;color:var(--primary,#1565C0);margin:12px 0 6px">Step-by-Step</div>
      ${steps.map(([label, val], i) => {
        const isLast = i === steps.length - 1;
        return `
          ${i > 0 ? '<hr style="border:none;border-top:1px solid var(--border-color);margin:2px 0">' : ''}
          <div class="flex justify-between items-center" style="padding:4px 0;font-size:12px">
            <span style="color:var(--text-muted);flex:1">${label}</span>
            <span style="font-weight:${isLast ? '700' : '500'};font-size:${isLast ? '14px' : '12px'};
                         margin-left:8px">${val}</span>
          </div>`;
      }).join('')}`;

    return buildCollapsibleCard('Calculation Detail', inputsHtml + tickerHtml + stepsHtml, false);
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
