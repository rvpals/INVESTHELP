import { positions, transactions, yahoo } from '../api.js';
import { navigate } from '../router.js';
import { tickerIcon } from '../components/ticker-icon.js';
import { collapsibleCard, initCollapsibleCards } from '../components/collapsible-card.js';
import { renderLineChart } from '../components/line-chart.js';
import { confirmAction, showToast } from '../components/confirm-dialog.js';
import { formatCurrency, formatSignedCurrency, formatPercent, gainLossClass, formatShares } from '../utils/format.js';
import { formatDate, daysSince, formatTimeAgo } from '../utils/dates.js';

let currentTab = 'details';

export async function render(container, { ticker }) {
  const [pos, txList] = await Promise.all([
    positions.get(ticker).catch(() => null),
    transactions.list(ticker).catch(() => []),
  ]);

  const p = pos || { ticker, name: '', type: 'Stock', currentPrice: 0, quantity: 0, dayGainLoss: 0, value: 0, dayHigh: 0, dayLow: 0 };
  const dayGLPerShare = p.quantity > 0 ? p.dayGainLoss / p.quantity : 0;

  container.innerHTML = `<div class="screen">
    <div class="flex items-center gap-8 mb-16">
      ${tickerIcon(ticker, p.name)}
      <div style="flex:1">
        <div class="text-lg text-bold">${ticker}</div>
        <div class="text-sm text-muted">${p.name || ''} <span class="badge" style="font-size:10px">${p.type}</span></div>
      </div>
      <div class="text-right">
        <div class="text-lg text-bold">${formatCurrency(p.currentPrice)}</div>
      </div>
    </div>
    <div class="tab-bar mb-8">
      <button class="tab${currentTab==='details'?' active':''}" data-tab="details">Details</button>
      <button class="tab${currentTab==='history'?' active':''}" data-tab="history">Price History</button>
      <button class="tab${currentTab==='transactions'?' active':''}" data-tab="transactions">Transactions</button>
    </div>
    <div id="tab-content"></div>
  </div>`;

  container.querySelectorAll('.tab').forEach(t => {
    t.addEventListener('click', () => { currentTab = t.dataset.tab; render(container, { ticker }); });
  });

  const content = document.getElementById('tab-content');

  if (currentTab === 'details') {
    content.innerHTML = `
      ${p.quantity === 0 ? '<div class="badge bg-red mb-8" style="display:block;text-align:center;padding:8px">You don\'t own any shares of this ticker</div>' : ''}
      <div class="flex gap-8 mb-8">
        <div class="card p-12" style="flex:1;text-align:center"><div class="text-xs text-muted">Total Shares</div><div class="text-lg text-bold">${formatShares(p.quantity)}</div></div>
        <div class="card p-12" style="flex:1;text-align:center"><div class="text-xs text-muted">Total Value</div><div class="text-lg text-bold">${formatCurrency(p.value)}</div></div>
      </div>
      <div class="flex gap-8 mb-16">
        <div class="card p-12" style="flex:1;text-align:center"><div class="text-xs text-muted">Daily G/L</div><div class="text-bold ${gainLossClass(p.dayGainLoss)}">${formatSignedCurrency(p.dayGainLoss)}</div></div>
        <div class="card p-12" style="flex:1;text-align:center"><div class="text-xs text-muted">G/L / Share</div><div class="text-bold ${gainLossClass(dayGLPerShare)}">${formatSignedCurrency(dayGLPerShare)}</div></div>
        <div class="card p-12" style="flex:1;text-align:center"><div class="text-xs text-muted">Day High</div><div>${formatCurrency(p.dayHigh)}</div></div>
        <div class="card p-12" style="flex:1;text-align:center"><div class="text-xs text-muted">Day Low</div><div>${formatCurrency(p.dayLow)}</div></div>
      </div>
      ${collapsibleCard('analysis_' + ticker, 'Analysis Info', '<div id="analysis-content"><div class="spinner"></div> Loading...</div>')}
      ${collapsibleCard('news_' + ticker, 'News on ' + ticker, '<div id="news-content"><div class="spinner"></div> Loading...</div>')}
      <button class="btn btn-primary w-full mt-8" onclick="window.open('https://finance.yahoo.com/quote/${encodeURIComponent(ticker)}','_blank')">Open on Yahoo Finance</button>
    `;
    initCollapsibleCards(content);
    loadAnalysis(ticker);
    loadNews(ticker);
  } else if (currentTab === 'history') {
    const TIMEFRAMES = [
      { label: 'Hourly', range: '1d', interval: '1m', hint: "Today's market hours (1m interval)" },
      { label: 'Daily', range: '3mo', interval: '1d', hint: 'Last 3 months (1d interval)' },
      { label: 'Monthly', range: '1y', interval: '1mo', hint: 'Last 1 year (1mo interval)' },
      { label: 'Yearly', range: '10y', interval: '1mo', hint: 'Last 10 years (1mo interval)' },
    ];
    let selectedTimeframe = 0;

    content.innerHTML = `
      <div class="chip-row mb-4">
        ${TIMEFRAMES.map((tf, i) => `<button class="chip${i === 0 ? ' selected' : ''}" data-idx="${i}">${tf.label}</button>`).join('')}
      </div>
      <div class="text-xs text-muted mb-8" id="history-hint">${TIMEFRAMES[0].hint}</div>
      ${selectedTimeframe === 0 ? `
        <div class="chip-row mb-8" id="interval-row">
          <button class="chip selected" data-int="1h">Every Hour</button>
          <button class="chip" data-int="30m">30 Min</button>
          <button class="chip" data-int="15m">15 Min</button>
          <button class="chip" data-int="5m">5 Min</button>
          <button class="chip" data-int="1m">1 Min</button>
        </div>
      ` : ''}
      <div class="chart-container mb-8"><canvas id="history-chart"></canvas></div>
      <div id="history-summary"></div>
      <div id="history-table"></div>
    `;

    let currentInterval = '1h';

    content.querySelectorAll('.chip[data-idx]').forEach(c => {
      c.addEventListener('click', () => {
        content.querySelectorAll('.chip[data-idx]').forEach(x => x.classList.remove('selected'));
        c.classList.add('selected');
        selectedTimeframe = parseInt(c.dataset.idx);
        const tf = TIMEFRAMES[selectedTimeframe];
        document.getElementById('history-hint').textContent = tf.hint;
        const intervalRow = document.getElementById('interval-row');
        if (selectedTimeframe === 0) {
          if (!intervalRow) {
            const hint = document.getElementById('history-hint');
            const row = document.createElement('div');
            row.className = 'chip-row mb-8';
            row.id = 'interval-row';
            row.innerHTML = `
              <button class="chip selected" data-int="1h">Every Hour</button>
              <button class="chip" data-int="30m">30 Min</button>
              <button class="chip" data-int="15m">15 Min</button>
              <button class="chip" data-int="5m">5 Min</button>
              <button class="chip" data-int="1m">1 Min</button>
            `;
            hint.after(row);
            attachIntervalListeners(row, ticker);
          }
          loadHistory(ticker, tf.range, currentInterval);
        } else {
          if (intervalRow) intervalRow.remove();
          loadHistory(ticker, tf.range, tf.interval);
        }
      });
    });

    const intervalRow = document.getElementById('interval-row');
    if (intervalRow) attachIntervalListeners(intervalRow, ticker);

    loadHistory(ticker, '1d', '1h');

    function attachIntervalListeners(row, ticker) {
      row.querySelectorAll('.chip[data-int]').forEach(c => {
        c.addEventListener('click', () => {
          row.querySelectorAll('.chip').forEach(x => x.classList.remove('selected'));
          c.classList.add('selected');
          currentInterval = c.dataset.int;
          loadHistory(ticker, '1d', currentInterval);
        });
      });
    }
  } else {
    content.innerHTML = `
      ${collapsibleCard('txstats_' + ticker, 'Transactions & Stats', `
        <div id="tx-list">
          ${txList.map(tx => txCard(tx, p.currentPrice)).join('')}
          ${txList.length === 0 ? '<div class="text-center text-muted p-16">No transactions</div>' : ''}
        </div>
      `, { defaultExpanded: true })}
      ${txList.length > 0 ? collapsibleCard('investperf_' + ticker, 'Investing Performance for ' + ticker, `
        <div class="chart-container mb-8"><canvas id="perf-chart"></canvas></div>
        <div id="perf-legend" class="flex gap-12 mb-8" style="justify-content:center">
          <span class="flex items-center gap-4 text-xs"><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:#D32F2F"></span> Transaction</span>
          <span class="flex items-center gap-4 text-xs"><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:var(--outline,#79747E);opacity:0.5"></span> Market</span>
          <span class="flex items-center gap-4 text-xs"><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:var(--tertiary,#7C5800)"></span> Current Price</span>
        </div>
        <div id="perf-table"><div class="flex items-center gap-8"><div class="spinner"></div> <span class="text-sm text-muted">Loading performance data...</span></div></div>
      `, { defaultExpanded: true }) : ''}
      <button class="btn btn-primary w-full mt-8" onclick="location.hash='#/transaction-form'">Add Transaction</button>
    `;
    initCollapsibleCards(content);
    content.querySelectorAll('.tx-delete').forEach(btn => {
      btn.addEventListener('click', async (e) => {
        e.stopPropagation();
        if (await confirmAction('Delete Transaction', 'Are you sure?')) {
          await transactions.delete(btn.dataset.id);
          showToast('Deleted');
          render(container, { ticker });
        }
      });
    });
    if (txList.length > 0) {
      loadInvestingPerformance(ticker, txList, p.currentPrice);
    }
  }
}

function txCard(tx, currentPrice) {
  const gl = (currentPrice - tx.pricePerShare) * tx.numberOfShares;
  const days = daysSince(tx.date);
  return `
    <div class="card p-12 mb-8">
      <div class="flex justify-between items-center">
        <div>
          <span class="text-bold">${tx.action}</span> ${formatShares(tx.numberOfShares)} @ ${formatCurrency(tx.pricePerShare)}
          <span class="text-xs text-muted">${days}d ago</span>
        </div>
        <div class="flex items-center gap-8">
          <span class="badge ${gl >= 0 ? 'badge-green' : 'badge-red'}">${formatSignedCurrency(gl)}</span>
          <button class="btn-icon tx-delete" data-id="${tx.id}" title="Delete">&times;</button>
        </div>
      </div>
      <div class="text-xs text-muted mt-4">${formatDate(tx.date)}${tx.note ? ' — ' + tx.note : ''}</div>
    </div>
  `;
}

async function loadAnalysis(ticker) {
  const el = document.getElementById('analysis-content');
  if (!el) return;
  try {
    const a = await yahoo.analysis(ticker);
    el.innerHTML = `
      <div class="mb-8"><strong>Sector:</strong> ${a.sector || 'N/A'} &middot; <strong>Industry:</strong> ${a.industry || 'N/A'}</div>
      <div class="data-table-wrapper">
        <table class="data-table">
          <tr><td>Market Cap</td><td>${formatCurrency(a.marketCap)}</td></tr>
          <tr><td>Trailing P/E</td><td>${a.trailingPE?.toFixed(2) || 'N/A'}</td></tr>
          <tr><td>Forward P/E</td><td>${a.forwardPE?.toFixed(2) || 'N/A'}</td></tr>
          <tr><td>EPS</td><td>${formatCurrency(a.eps)}</td></tr>
          <tr><td>Dividend Yield</td><td>${a.dividendYield ? (a.dividendYield * 100).toFixed(2) + '%' : 'N/A'}</td></tr>
          <tr><td>52-Week High</td><td>${formatCurrency(a.fiftyTwoWeekHigh)}</td></tr>
          <tr><td>52-Week Low</td><td>${formatCurrency(a.fiftyTwoWeekLow)}</td></tr>
          <tr><td>50-Day Avg</td><td>${formatCurrency(a.fiftyDayAverage)}</td></tr>
          <tr><td>Analyst Target</td><td>${formatCurrency(a.targetMeanPrice)}</td></tr>
          <tr><td>Profit Margins</td><td>${a.profitMargins ? (a.profitMargins * 100).toFixed(2) + '%' : 'N/A'}</td></tr>
        </table>
      </div>
      ${a.longBusinessSummary ? `<p class="text-sm mt-8">${a.longBusinessSummary}</p>` : ''}
    `;
  } catch (err) {
    el.innerHTML = `<div class="text-sm text-muted">Unable to load analysis: ${err.message}</div>`;
  }
}

async function loadNews(ticker) {
  const el = document.getElementById('news-content');
  if (!el) return;
  try {
    const news = await yahoo.news(ticker, 5);
    if (!news.length) { el.innerHTML = '<div class="text-sm text-muted">No news found</div>'; return; }
    el.innerHTML = news.map((n, i) => `
      <div style="padding:8px 0;${i > 0 ? 'border-top:1px solid color-mix(in srgb, var(--outline) 20%, transparent)' : ''}">
        <a href="${n.link}" target="_blank" style="text-decoration:none;color:var(--on-surface)">
          <div class="text-bold text-sm">${n.title}</div>
          <div class="text-xs"><span class="text-primary">${n.publisher}</span> &middot; ${formatTimeAgo(n.publishedAt)}</div>
        </a>
      </div>
    `).join('');
  } catch {
    el.innerHTML = '<div class="text-sm text-muted">Unable to load news</div>';
  }
}

function getCSS(prop) {
  return getComputedStyle(document.documentElement).getPropertyValue(prop).trim();
}

async function loadInvestingPerformance(ticker, txList, currentPrice) {
  const tableEl = document.getElementById('perf-table');
  const canvas = document.getElementById('perf-chart');
  if (!canvas || !tableEl) return;

  try {
    const txDates = txList.map(tx => tx.date);
    const earliestDate = Math.min(...txDates);
    const period1 = (earliestDate - 1) * 86400;
    const period2 = Math.floor(Date.now() / 1000);
    const history = await yahoo.historyPeriod(ticker, period1, period2, '1d');

    const marketPoints = (history || []).map(h => ({
      x: h.timestamp, y: h.close, type: 'market'
    }));

    const txPoints = txList.map(tx => ({
      x: tx.date * 86400 + 43200,
      y: tx.pricePerShare,
      type: 'transaction',
      action: tx.action,
      shares: tx.numberOfShares
    }));

    const currentPoint = {
      x: Math.floor(Date.now() / 1000),
      y: currentPrice,
      type: 'current'
    };

    const allPoints = [...marketPoints, ...txPoints, currentPoint].sort((a, b) => a.x - b.x);

    if (allPoints.length < 2) {
      tableEl.innerHTML = '<div class="text-muted">Not enough data for chart</div>';
      return;
    }

    renderPerfChart(canvas, allPoints);
    renderPerfTable(tableEl, allPoints);
  } catch (err) {
    tableEl.innerHTML = `<div class="text-muted">Error loading performance data: ${err.message}</div>`;
  }
}

function renderPerfChart(canvas, points) {
  if (!points || points.length < 2) return;

  const ctx = canvas.getContext('2d');
  const dpr = window.devicePixelRatio || 1;
  const w = canvas.parentElement.clientWidth;
  const h = 260;
  canvas.width = w * dpr;
  canvas.height = h * dpr;
  canvas.style.width = w + 'px';
  canvas.style.height = h + 'px';
  ctx.scale(dpr, dpr);

  const pad = { top: 24, right: 10, bottom: 30, left: 60 };
  const cw = w - pad.left - pad.right;
  const ch = h - pad.top - pad.bottom;

  const xs = points.map(p => p.x);
  const ys = points.map(p => p.y);
  const xMin = Math.min(...xs), xMax = Math.max(...xs);
  const yMin = Math.min(...ys), yMax = Math.max(...ys);
  const yRange = yMax - yMin || 1;

  const toX = x => pad.left + ((x - xMin) / (xMax - xMin || 1)) * cw;
  const toY = y => pad.top + ch - ((y - yMin) / yRange) * ch;

  const outlineColor = getCSS('--outline') || '#79747E';
  const onSurface = getCSS('--on-surface') || '#1C1B1F';
  const txColor = '#D32F2F';
  const currentColor = getCSS('--tertiary') || '#7C5800';

  ctx.strokeStyle = outlineColor;
  ctx.globalAlpha = 0.2;
  ctx.lineWidth = 1;
  for (let i = 0; i <= 3; i++) {
    const y = pad.top + (ch / 3) * i;
    ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(w - pad.right, y); ctx.stroke();
  }
  ctx.globalAlpha = 1;

  ctx.fillStyle = onSurface;
  ctx.font = '10px sans-serif';
  ctx.textAlign = 'right';
  ctx.textBaseline = 'middle';
  for (let i = 0; i <= 3; i++) {
    const val = yMax - (yRange / 3) * i;
    ctx.fillText(formatCurrency(val), pad.left - 6, pad.top + (ch / 3) * i);
  }

  ctx.strokeStyle = outlineColor;
  ctx.globalAlpha = 0.5;
  ctx.lineWidth = 1.5;
  ctx.lineJoin = 'round';
  ctx.beginPath();
  points.forEach((p, i) => {
    const x = toX(p.x), y = toY(p.y);
    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
  });
  ctx.stroke();
  ctx.globalAlpha = 1;

  points.forEach(p => {
    const cx = toX(p.x), cy = toY(p.y);
    if (p.type === 'transaction') {
      ctx.beginPath(); ctx.arc(cx, cy, 7, 0, Math.PI * 2);
      ctx.fillStyle = '#fff'; ctx.fill();
      ctx.beginPath(); ctx.arc(cx, cy, 5, 0, Math.PI * 2);
      ctx.fillStyle = txColor; ctx.fill();
      ctx.fillStyle = txColor;
      ctx.font = 'bold 10px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(formatCurrency(p.y), cx, cy - 12);
    } else if (p.type === 'current') {
      ctx.beginPath(); ctx.arc(cx, cy, 7, 0, Math.PI * 2);
      ctx.fillStyle = '#fff'; ctx.fill();
      ctx.beginPath(); ctx.arc(cx, cy, 5, 0, Math.PI * 2);
      ctx.fillStyle = currentColor; ctx.fill();
      ctx.fillStyle = currentColor;
      ctx.font = 'bold 10px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(formatCurrency(p.y), cx, cy - 12);
    } else {
      ctx.beginPath(); ctx.arc(cx, cy, 3, 0, Math.PI * 2);
      ctx.fillStyle = outlineColor;
      ctx.globalAlpha = 0.4;
      ctx.fill();
      ctx.globalAlpha = 1;
    }
  });

  ctx.fillStyle = onSurface;
  ctx.font = '10px sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'top';
  const step = Math.max(1, Math.floor(points.length / 5));
  for (let i = 0; i < points.length; i += step) {
    const p = points[i];
    ctx.fillText(new Date(p.x * 1000).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }), toX(p.x), h - pad.bottom + 8);
  }

  canvas.onclick = (e) => {
    const rect = canvas.getBoundingClientRect();
    const mx = e.clientX - rect.left;
    let closest = null, minDist = Infinity;
    points.forEach(p => {
      const d = Math.abs(toX(p.x) - mx);
      if (d < minDist) { minDist = d; closest = p; }
    });
    if (closest && minDist < 30) {
      renderPerfChart(canvas, points);
      const cx = toX(closest.x), cy = toY(closest.y);
      const tipColor = closest.type === 'transaction' ? txColor : closest.type === 'current' ? currentColor : outlineColor;
      ctx.strokeStyle = tipColor;
      ctx.setLineDash([4, 4]);
      ctx.beginPath(); ctx.moveTo(cx, pad.top); ctx.lineTo(cx, pad.top + ch); ctx.stroke();
      ctx.setLineDash([]);
      ctx.fillStyle = onSurface;
      ctx.font = 'bold 12px sans-serif';
      ctx.textAlign = cx > w / 2 ? 'right' : 'left';
      const tx = cx + (cx > w / 2 ? -8 : 8);
      let label = formatCurrency(closest.y);
      if (closest.type === 'transaction') label += ` (${closest.action} ${formatShares(closest.shares)})`;
      if (closest.type === 'current') label += ' (Current)';
      ctx.fillText(label, tx, cy - 18);
      ctx.font = '10px sans-serif';
      ctx.fillText(new Date(closest.x * 1000).toLocaleDateString(), tx, cy - 6);
      ctx.beginPath(); ctx.arc(cx, cy, 6, 0, Math.PI * 2);
      ctx.fillStyle = tipColor; ctx.fill();
      ctx.strokeStyle = '#fff'; ctx.lineWidth = 2; ctx.stroke();
    }
  };
}

function renderPerfTable(el, allPoints) {
  const rows = allPoints.map((p, i) => {
    const dateStr = new Date(p.x * 1000).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    const isTx = p.type === 'transaction';
    const isCurrent = p.type === 'current';
    const rowClass = isTx ? 'style="background:rgba(211,47,47,0.08)"' : isCurrent ? 'style="background:rgba(124,88,0,0.08)"' : (i % 2 ? 'class="row-alt"' : '');
    const typeLabel = isTx ? `<span class="text-bold" style="color:#D32F2F">${p.action} ${formatShares(p.shares)}</span>`
                    : isCurrent ? `<span class="text-bold" style="color:var(--tertiary,#7C5800)">Current</span>`
                    : '<span class="text-muted">Market</span>';
    const priceStyle = isTx ? ' style="color:#D32F2F;font-weight:bold"' : isCurrent ? ' style="color:var(--tertiary,#7C5800);font-weight:bold"' : '';
    return `<tr ${rowClass}><td>${dateStr}</td><td${priceStyle}>${formatCurrency(p.y)}</td><td>${typeLabel}</td></tr>`;
  });

  el.innerHTML = `
    <div class="data-table-wrapper" style="max-height:300px;overflow-y:auto">
      <table class="data-table">
        <thead><tr><th>Date</th><th>Price</th><th>Type</th></tr></thead>
        <tbody>${rows.join('')}</tbody>
      </table>
    </div>
  `;
}

async function loadHistory(ticker, range, interval) {
  const chart = document.getElementById('history-chart');
  const summary = document.getElementById('history-summary');
  const table = document.getElementById('history-table');
  if (!chart) return;
  summary.innerHTML = '<div class="flex items-center gap-8"><div class="spinner"></div> <span class="text-sm text-muted">Loading price history...</span></div>';
  table.innerHTML = '';
  try {
    const data = await yahoo.history(ticker, range, interval);
    if (!data.length) { summary.innerHTML = '<div class="text-muted">No data available for this timeframe</div>'; return; }
    const points = data.map(d => ({ x: d.timestamp, y: d.close }));
    renderLineChart(chart, points, { height: 200 });
    const prices = data.map(d => d.close);
    const avg = prices.reduce((a, b) => a + b, 0) / prices.length;
    summary.innerHTML = `
      <div class="flex gap-8 mb-8">
        <div class="card p-8" style="flex:1;text-align:center"><div class="text-xs text-muted">Avg</div><div class="text-bold">${formatCurrency(avg)}</div></div>
        <div class="card p-8" style="flex:1;text-align:center"><div class="text-xs text-muted">Max</div><div class="text-bold">${formatCurrency(Math.max(...prices))}</div></div>
        <div class="card p-8" style="flex:1;text-align:center"><div class="text-xs text-muted">Min</div><div class="text-bold">${formatCurrency(Math.min(...prices))}</div></div>
      </div>
    `;
    table.innerHTML = `<div class="data-table-wrapper" style="max-height:300px;overflow-y:auto">
      <table class="data-table"><thead><tr><th>#</th><th>Date</th><th>Close</th></tr></thead><tbody>
        ${data.map((d, i) => `<tr><td>${i + 1}</td><td>${new Date(d.timestamp * 1000).toLocaleString()}</td><td>${formatCurrency(d.close)}</td></tr>`).join('')}
      </tbody></table></div>`;
  } catch (err) {
    summary.innerHTML = `<div class="text-muted">Error: ${err.message}</div>`;
  }
}
