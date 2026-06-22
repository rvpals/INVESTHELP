import { correlation as correlationApi } from '../api.js';
import { collapsibleCard, initCollapsibleCards } from '../components/collapsible-card.js';
import { navigate } from '../router.js';

// ── Colour helpers ─────────────────────────────────────────────────────────────

function correlationColor(v) {
  if (v === null || v === undefined || (typeof v === 'number' && isNaN(v))) return '#9E9E9E';
  if (v >= 0.75) return '#E53935';
  if (v >= 0.50) return '#FB8C00';
  if (v >= 0.25) return '#FDD835';
  if (v >= 0.00) return '#43A047';
  return '#1E88E5';
}

function cellTextColor(v) {
  if (v === null || (typeof v === 'number' && isNaN(v))) return '#fff';
  if (v >= 0.25 && v < 0.75) return '#000';
  return '#fff';
}

function correlationLabel(v) {
  if (v === null || (typeof v === 'number' && isNaN(v))) return 'unavailable';
  if (v >= 0.75) return 'highly correlated';
  if (v >= 0.50) return 'moderately correlated';
  if (v >= 0.25) return 'low correlation';
  if (v >= 0.00) return 'largely uncorrelated';
  return 'inversely correlated';
}

function fmt(v) {
  if (v === null || (typeof v === 'number' && isNaN(v))) return '—';
  return Number(v).toFixed(2);
}

// ── Matrix math ────────────────────────────────────────────────────────────────

function computeAvgCorr(matrix, n) {
  let sum = 0, count = 0;
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      const v = matrix[i][j];
      if (v !== null && !isNaN(v)) { sum += v; count++; }
    }
  }
  return count > 0 ? sum / count : NaN;
}

function computeMostCorrelated(tickers, matrix) {
  let maxV = -Infinity, bestI = -1, bestJ = -1;
  const n = tickers.length;
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      const v = matrix[i][j];
      if (v !== null && !isNaN(v) && v > maxV) { maxV = v; bestI = i; bestJ = j; }
    }
  }
  if (bestI === -1) return null;
  return { a: tickers[bestI], b: tickers[bestJ], v: maxV };
}

function computeMostDiversifying(tickers, matrix) {
  const n = tickers.length;
  if (n < 2) return null;
  let minAvg = Infinity, bestTicker = null;
  for (let i = 0; i < n; i++) {
    let sum = 0, count = 0;
    for (let j = 0; j < n; j++) {
      if (i === j) continue;
      const v = matrix[i][j];
      if (v !== null && !isNaN(v)) { sum += v; count++; }
    }
    if (count === 0) continue;
    const avg = sum / count;
    if (avg < minAvg) { minAvg = avg; bestTicker = tickers[i]; }
  }
  return bestTicker ? { ticker: bestTicker, avg: minAvg } : null;
}

// ── HTML builders ──────────────────────────────────────────────────────────────

function explainerCardHtml() {
  const legendItems = [
    ['#E53935', '0.75 – 1.00', 'Highly correlated — moves nearly in lockstep'],
    ['#FB8C00', '0.50 – 0.74', 'Moderately correlated — significant overlap'],
    ['#FDD835', '0.25 – 0.49', 'Low correlation — decent diversification'],
    ['#43A047', '0.00 – 0.24', 'Uncorrelated — strong diversification'],
    ['#1E88E5', 'Negative',    'Inverse relationship — one rises as other falls'],
  ];

  const legendHtml = legendItems.map(([color, range, desc]) => `
    <div class="flex items-center gap-8 py-4" style="border-bottom:1px solid color-mix(in srgb,var(--outline,#ccc) 20%,transparent)">
      <div style="width:12px;height:12px;background:${color};border-radius:2px;flex-shrink:0"></div>
      <span class="text-xs"><strong>${range}</strong> — ${desc}</span>
    </div>`).join('');

  const content = `
    <div class="text-sm mb-10"><strong>What it measures</strong><br>
    Correlation measures how much two stocks move together on a daily basis. A high correlation means
    they tend to rise and fall on the same days — so owning both gives you less protection than you
    might think.</div>
    <div class="mb-10"><strong class="text-sm">The scale</strong>${legendHtml}</div>
    <div class="text-sm mb-10"><strong>What to look for</strong><br>
    If most of your holdings show red or orange against each other, your portfolio may not be as
    diversified as it looks. Aim for a mix of greens and yellows across sectors.</div>
    <div class="text-sm"><strong>The diagonal</strong><br>
    The diagonal is always 1.0 — every stock is perfectly correlated with itself.</div>`;

  return collapsibleCard('corr_explainer', 'What is a Correlation Matrix?', content);
}

function matrixGridHtml(tickers, matrix, filterHighCorr) {
  const cellW = '68px';
  const labelW = '80px';

  const headerCells = tickers.map(t => `
    <th style="min-width:${cellW};max-width:${cellW};padding:0;vertical-align:bottom;scroll-snap-align:start;height:84px;border:none;background:transparent">
      <div style="width:${cellW};height:84px;position:relative;overflow:hidden">
        <div style="position:absolute;bottom:8px;left:50%;transform-origin:left bottom;
          transform:translateX(-50%) rotate(-45deg);white-space:nowrap;font-size:11px;
          font-weight:700;color:var(--on-surface,#1C1B1F);padding-right:4px">
          ${t}
        </div>
      </div>
    </th>`).join('');

  const dataRows = tickers.map((rowTicker, i) => {
    const cells = tickers.map((_, j) => {
      const v = matrix[i][j];
      const isDiag = i === j;
      const bg = isDiag ? '#424242' : correlationColor(v);
      const textColor = isDiag ? '#fff' : cellTextColor(v);
      const isFiltered = filterHighCorr && !isDiag && (v === null || v < 0.75);
      const opacity = isFiltered ? '0.2' : '1';
      const cursor = isDiag ? 'default' : 'pointer';
      return `<td style="min-width:${cellW};max-width:${cellW};height:${cellW};padding:1px;scroll-snap-align:start;border:none"
          ${!isDiag ? `data-cell="${i},${j}"` : ''}>
          <div style="width:calc(${cellW} - 2px);height:calc(${cellW} - 2px);background:${bg};border-radius:4px;
            display:flex;align-items:center;justify-content:center;
            color:${textColor};font-size:11px;font-weight:${isDiag ? '700' : '400'};
            cursor:${cursor};opacity:${opacity};transition:opacity .15s;user-select:none">
            ${isDiag ? '1.00' : fmt(v)}
          </div>
        </td>`;
    }).join('');

    return `<tr>
      <td style="min-width:${labelW};max-width:${labelW};padding:1px 6px 1px 0;font-size:11px;font-weight:700;
        color:var(--on-surface,#1C1B1F);white-space:nowrap;position:sticky;left:0;
        background:var(--surface,#FFFBFE);z-index:2;border:none">
        ${rowTicker}
      </td>
      ${cells}
    </tr>`;
  }).join('');

  return `<div class="card mb-12">
    <div class="p-12">
      <div class="text-sm text-bold mb-8">Correlation Matrix</div>
      <div style="overflow-x:auto;scroll-snap-type:x mandatory;-webkit-overflow-scrolling:touch;scrollbar-width:thin">
        <table style="border-collapse:collapse;min-width:max-content">
          <thead><tr>
            <th style="min-width:${labelW};max-width:${labelW};border:none;background:transparent"></th>
            ${headerCells}
          </tr></thead>
          <tbody>${dataRows}</tbody>
        </table>
      </div>
      <div class="text-xs text-muted mt-8 text-center">Tap any non-diagonal cell for details</div>
    </div>
  </div>`;
}

function marketSensitivityHtml(tickers, marketCorrelation) {
  const chips = tickers.map(t => {
    const raw = marketCorrelation[t];
    const v = raw === null ? NaN : Number(raw);
    const bg = correlationColor(v);
    const text = cellTextColor(v);
    return `<span style="background:${bg};color:${text};border-radius:16px;padding:4px 10px;
      font-size:11px;font-weight:700;display:inline-block;margin:2px 2px">${t} ${fmt(v)}</span>`;
  }).join('');

  return `<div class="card mb-12">
    <div class="p-12">
      <div class="text-sm text-bold mb-8">Market Sensitivity (vs S&amp;P 500 / SPY)</div>
      <div style="display:flex;flex-wrap:wrap;gap:0">${chips}</div>
      <div class="text-xs text-muted mt-8">How closely each holding tracks the overall market.
        Values near 1.0 mean the stock largely follows the S&amp;P 500.</div>
    </div>
  </div>`;
}

function summaryInsightsHtml(tickers, matrix, avgCorr, mostCorr, mostDiv) {
  const bullets = [];

  if (!isNaN(avgCorr)) {
    bullets.push(
      `Your holdings have an average correlation of <strong>${avgCorr.toFixed(2)}</strong>` +
      (avgCorr > 0.70
        ? ' — moderately high. Consider adding assets from uncorrelated sectors.'
        : ' — reasonable diversification across your holdings.')
    );
  }

  if (mostCorr) {
    bullets.push(
      `<strong>${mostCorr.a}</strong> and <strong>${mostCorr.b}</strong> are your most correlated
      holdings (${mostCorr.v.toFixed(2)}). They may behave as a single position in volatile markets.`
    );
  }

  if (mostDiv) {
    bullets.push(
      `<strong>${mostDiv.ticker}</strong> has the lowest average correlation to your other holdings
      (${mostDiv.avg.toFixed(2)}), making it your most diversifying position.`
    );
  }

  const warning = !isNaN(avgCorr) && avgCorr > 0.70
    ? `<div class="mt-8 p-10" style="background:var(--error-container,#FFDAD6);
        color:var(--on-error-container,#410002);border-radius:8px">
        ⚠️ High average correlation detected. Your portfolio may be less diversified than it appears.
        Consider holdings in different sectors or asset classes.
      </div>`
    : '';

  const bulletHtml = bullets.map(b =>
    `<div class="flex gap-6 mt-6"><span>•</span><span class="text-sm">${b}</span></div>`
  ).join('');

  return `<div class="card mb-12">
    <div class="p-12">
      <div class="text-sm text-bold mb-8">Portfolio Insights</div>
      ${bulletHtml || '<div class="text-sm text-muted">Not enough data for insights.</div>'}
      ${warning}
    </div>
  </div>`;
}

function cellExplainer(a, b, v) {
  if (v === null || isNaN(v))
    return `Insufficient overlapping data to compute correlation between ${a} and ${b}.`;
  if (v >= 0.75)
    return `${a} and ${b} are highly correlated. When one rises or falls sharply, the other tends to follow. Owning both may offer less diversification than expected.`;
  if (v >= 0.50)
    return `${a} and ${b} are moderately correlated with significant overlap in price movement. They provide some diversification, but tend to react similarly to market events.`;
  if (v >= 0.25)
    return `${a} and ${b} show low correlation with decent independent movement. This pair contributes meaningfully to portfolio diversification.`;
  if (v >= 0.00)
    return `${a} and ${b} are largely uncorrelated — they tend to move independently. This is a strong diversification pair.`;
  return `${a} and ${b} are inversely correlated — one tends to rise when the other falls. This is the strongest form of diversification.`;
}

function showCellDialog(a, b, v) {
  const overlay = document.getElementById('dialog-overlay');
  overlay.className = 'dialog-overlay';
  overlay.innerHTML = `
    <div class="dialog">
      <div class="dialog-title">${a} vs ${b}</div>
      <div class="flex items-center gap-8 mb-10">
        <div style="width:14px;height:14px;border-radius:3px;background:${correlationColor(v)};flex-shrink:0"></div>
        <span class="text-sm text-bold">Correlation: ${fmt(v)} (${correlationLabel(v)})</span>
      </div>
      <p class="text-sm">${cellExplainer(a, b, v)}</p>
      <div class="dialog-actions">
        <button class="btn btn-primary" id="close-cell-dialog">Close</button>
      </div>
    </div>`;
  document.getElementById('close-cell-dialog').addEventListener('click', () => {
    overlay.className = 'dialog-overlay hidden';
  });
  overlay.addEventListener('click', e => {
    if (e.target === overlay) overlay.className = 'dialog-overlay hidden';
  });
}

// ── Canvas download ────────────────────────────────────────────────────────────

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

function downloadMatrix(tickers, matrix, marketCorrelation) {
  const n = tickers.length;
  const cellPx  = 80;
  const labelPx = 100;
  const headerPx = 90;
  const titlePx  = 50;
  const legendPx = 80;
  const padH = 20;

  const width  = padH + labelPx + n * cellPx + padH;
  const height = titlePx + headerPx + n * cellPx + legendPx + padH;

  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');

  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, width, height);

  // Title
  ctx.fillStyle = '#000';
  ctx.font = 'bold 22px sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('Correlation Matrix  (1-year daily returns)', width / 2, titlePx - 10);

  // Column headers
  tickers.forEach((ticker, j) => {
    const cx = padH + labelPx + j * cellPx + cellPx / 2;
    const cy = titlePx + headerPx - 4;
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(-Math.PI / 4);
    ctx.fillStyle = '#333';
    ctx.font = 'bold 13px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText(ticker, -cellPx / 2 + 4, 0);
    ctx.restore();
  });

  // Cells
  tickers.forEach((rowTicker, i) => {
    const rowY = titlePx + headerPx + i * cellPx;

    ctx.fillStyle = '#333';
    ctx.font = 'bold 13px sans-serif';
    ctx.textAlign = 'right';
    ctx.fillText(rowTicker, padH + labelPx - 6, rowY + cellPx / 2 + 5);

    tickers.forEach((_, j) => {
      const v = matrix[i][j];
      const isDiag = i === j;
      const left = padH + labelPx + j * cellPx;
      const top  = rowY;

      ctx.fillStyle = isDiag ? '#424242' : correlationColor(v);
      roundRect(ctx, left + 1, top + 1, cellPx - 2, cellPx - 2, 4);
      ctx.fill();

      ctx.fillStyle = isDiag || v >= 0.75 || v < 0.25 ? '#fff' : '#000';
      ctx.font = '13px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(isDiag ? '1.00' : fmt(v), left + cellPx / 2, top + cellPx / 2 + 5);
    });
  });

  // Legend
  const legendY = titlePx + headerPx + n * cellPx + 14;
  const legendItems = [
    ['#E53935', '≥0.75 High'],
    ['#FB8C00', '≥0.50 Mod'],
    ['#FDD835', '≥0.25 Low'],
    ['#43A047', '≥0.00 None'],
    ['#1E88E5', '< 0 Inverse'],
  ];
  const slotW = (width - padH * 2) / legendItems.length;
  legendItems.forEach(([color, label], k) => {
    const lx = padH + k * slotW;
    ctx.fillStyle = color;
    roundRect(ctx, lx, legendY, 20, 20, 3);
    ctx.fill();
    ctx.fillStyle = '#555';
    ctx.font = '11px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText(label, lx + 24, legendY + 15);
  });

  const link = document.createElement('a');
  link.download = `correlation_matrix_${Date.now()}.png`;
  link.href = canvas.toDataURL('image/png');
  link.click();
}

// ── Main render ────────────────────────────────────────────────────────────────

export async function render(container) {
  container.innerHTML = `<div class="screen">
    <div class="flex items-center gap-8 mb-8">
      <button class="btn btn-sm btn-outline" onclick="history.back()">&#8592; Back</button>
      <div style="flex:1">
        <h2 style="margin:0">Correlation Matrix</h2>
        <div class="text-xs text-muted">Based on 1 year of daily returns</div>
      </div>
      <button class="btn btn-sm btn-outline" id="btn-download" title="Download as PNG image" style="display:none">&#128247; Save</button>
      <button class="btn btn-sm btn-outline" id="btn-refresh" title="Recompute matrix">&#8635; Refresh</button>
    </div>
    <div id="corr-last-calc" style="display:none;padding:2px 0 8px;font-size:12px;color:var(--on-surface-variant,#49454F);text-align:right"></div>
    <div id="corr-body">
      <div class="text-center p-32"><div class="spinner"></div><div class="text-muted mt-12">Loading…</div></div>
    </div>
  </div>`;

  let currentResult = null;
  let filterHighCorr = false;

  function showLastCalc(epochSeconds) {
    const el = document.getElementById('corr-last-calc');
    if (!el) return;
    if (epochSeconds) {
      const d = new Date(epochSeconds * 1000);
      el.textContent = `Calculated on ${d.toLocaleString(undefined, { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' })}`;
      el.style.display = '';
    } else {
      el.style.display = 'none';
    }
  }

  function renderMatrix(data) {
    currentResult = data;
    const { tickers, matrix, marketCorrelation, failedTickers } = data;

    const dlBtn = document.getElementById('btn-download');
    if (dlBtn) dlBtn.style.display = '';

    const avgCorr  = computeAvgCorr(matrix, tickers.length);
    const mostCorr = computeMostCorrelated(tickers, matrix);
    const mostDiv  = computeMostDiversifying(tickers, matrix);

    let html = '';

    if (failedTickers && failedTickers.length > 0) {
      html += `<div class="card p-12 mb-12" style="background:var(--error-container,#FFDAD6);color:var(--on-error-container,#410002)">
        Could not load data for <strong>${failedTickers.join(', ')}</strong> — excluded from analysis.
      </div>`;
    }

    html += explainerCardHtml();

    // Filter toggle
    html += `<div class="flex items-center gap-8 mt-12 mb-4">
      <span class="text-sm" style="font-weight:600">Filter:</span>
      <button id="filter-toggle" style="border-radius:20px;padding:4px 14px;font-size:12px;cursor:pointer;
        background:${filterHighCorr ? 'var(--primary,#6750A4)' : 'transparent'};
        color:${filterHighCorr ? 'var(--on-primary,#fff)' : 'var(--primary,#6750A4)'};
        border:1.5px solid var(--primary,#6750A4)">
        ${filterHighCorr ? '✓ ' : ''}Highlight ≥ 0.75 only
      </button>
    </div>`;
    if (filterHighCorr) {
      html += `<div class="text-xs text-muted mb-8">Dimmed cells have correlation &lt; 0.75. Only highly correlated pairs are shown at full opacity.</div>`;
    }

    html += matrixGridHtml(tickers, matrix, filterHighCorr);

    if (marketCorrelation && Object.keys(marketCorrelation).length > 0) {
      html += marketSensitivityHtml(tickers, marketCorrelation);
    }

    html += summaryInsightsHtml(tickers, matrix, avgCorr, mostCorr, mostDiv);
    html += `<div style="height:32px"></div>`;

    const body = document.getElementById('corr-body');
    body.innerHTML = html;
    initCollapsibleCards(body);

    // Filter toggle handler
    document.getElementById('filter-toggle')?.addEventListener('click', () => {
      filterHighCorr = !filterHighCorr;
      renderMatrix(currentResult);
    });

    // Cell click → detail dialog
    body.querySelectorAll('[data-cell]').forEach(td => {
      td.addEventListener('click', () => {
        const [i, j] = td.dataset.cell.split(',').map(Number);
        showCellDialog(tickers[i], tickers[j], matrix[i][j]);
      });
    });

    // Download handler (wired fresh each render)
    document.getElementById('btn-download')?.addEventListener('click', () => {
      downloadMatrix(tickers, matrix, marketCorrelation);
    });
  }

  async function loadFromCache() {
    try {
      const data = await correlationApi.getCache();
      if (data.noCache) {
        compute();
      } else {
        showLastCalc(data.calculatedAt);
        renderMatrix(data);
      }
    } catch {
      compute();
    }
  }

  async function compute() {
    const body = document.getElementById('corr-body');
    body.innerHTML = `<div class="text-center p-32">
      <div class="spinner"></div>
      <div class="text-muted mt-12">Fetching 1 year of price history for each position…</div>
      <div class="text-xs text-muted mt-4">This may take 30–60 seconds depending on how many positions you hold.</div>
    </div>`;
    const refreshBtn = document.getElementById('btn-refresh');
    if (refreshBtn) refreshBtn.disabled = true;

    try {
      const data = await correlationApi.compute();
      showLastCalc(data.calculatedAt);
      renderMatrix(data);
    } catch (err) {
      body.innerHTML = `<div class="card p-16 text-center">
        <div class="text-muted">Computation failed: ${err.message}</div>
        <button class="btn btn-primary mt-16" id="btn-retry">Retry</button>
      </div>`;
      document.getElementById('btn-retry')?.addEventListener('click', compute);
    } finally {
      const btn = document.getElementById('btn-refresh');
      if (btn) btn.disabled = false;
    }
  }

  document.getElementById('btn-refresh').addEventListener('click', async () => {
    showLastCalc(null);
    document.getElementById('btn-download').style.display = 'none';
    currentResult = null;
    try { await correlationApi.clearCache(); } catch { /* non-fatal */ }
    compute();
  });

  loadFromCache();
}
