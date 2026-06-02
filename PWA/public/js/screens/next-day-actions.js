import { positions, transactions, yahoo, settings } from '../api.js';
import { collapsibleCard, initCollapsibleCards } from '../components/collapsible-card.js';
import { formatCurrency, formatPercent, formatShares, gainLossClass } from '../utils/format.js';
import { navigate } from '../router.js';

const ACTIONS = {
  STOP_LOSS:      { label: 'STOP LOSS',    color: '#C62828', order: 0 },
  TRIM_PROFIT:    { label: 'TRIM PROFITS', color: '#FF8F00', order: 1 },
  REBALANCE_TRIM: { label: 'REBALANCE',    color: '#1565C0', order: 2 },
  STRONG_BUY:     { label: 'STRONG BUY',   color: '#2E7D32', order: 3 },
  HOLD:           { label: 'HOLD',          color: '#616161', order: 4 },
};

export async function render(container) {
  container.innerHTML = `<div class="screen">
    <h2 class="mb-16">Next Day Actions</h2>
    <button class="btn btn-primary w-full mb-16" id="run-scan">Run Scan</button>
    <div id="scan-progress" class="text-center text-sm text-muted mb-8"></div>
    <div id="scan-results"></div>
  </div>`;

  document.getElementById('run-scan').addEventListener('click', () => runScan(container));
}

async function runScan(container) {
  const progressEl = document.getElementById('scan-progress');
  const resultsEl = document.getElementById('scan-results');
  const btn = document.getElementById('run-scan');
  btn.disabled = true;
  btn.textContent = 'Scanning...';
  resultsEl.innerHTML = '';

  try {
    const [posList, serverSettings] = await Promise.all([
      positions.list(),
      settings.getAll(),
    ]);

    const items = posList.filter(p => p.quantity > 0);
    if (items.length === 0) {
      progressEl.textContent = '';
      resultsEl.innerHTML = '<div class="text-center text-muted p-16">No positions with shares to scan.</div>';
      return;
    }

    const trailingStopPct = parseInt(serverSettings.trailing_stop_pct) || 10;
    const profitTargetPct = parseInt(serverSettings.profit_target_pct) || 20;
    const stockCap = parseInt(serverSettings.stock_concentration_cap) || 10;
    const etfCap = parseInt(serverSettings.etf_concentration_cap) || 25;

    const totalPortfolioValue = items.reduce((s, p) => s + p.value, 0);
    if (totalPortfolioValue <= 0) {
      resultsEl.innerHTML = '<div class="text-center text-muted p-16">Portfolio value is zero.</div>';
      return;
    }

    const signals = [];

    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      progressEl.textContent = `Scanning ${item.ticker} (${i + 1}/${items.length})`;

      const costBasis = await calculateCostBasis(item.ticker, item.currentPrice);
      let scanData = null;
      try { scanData = await yahoo.scan(item.ticker); } catch {}

      const allocationPct = (item.value / totalPortfolioValue) * 100;
      const totalReturnPct = costBasis > 0 ? ((item.currentPrice - costBasis) / costBasis) * 100 : 0;

      const signal = evaluatePosition(item, scanData, allocationPct, costBasis, totalReturnPct,
        trailingStopPct, profitTargetPct, stockCap, etfCap);
      signals.push(signal);
    }

    signals.sort((a, b) => ACTIONS[a.action].order - ACTIONS[b.action].order || b.allocationPct - a.allocationPct);

    progressEl.textContent = '';
    renderResults(resultsEl, signals, { trailingStopPct, profitTargetPct, stockCap, etfCap });
  } catch (err) {
    progressEl.textContent = '';
    resultsEl.innerHTML = `<div class="text-muted">Scan failed: ${err.message}</div>`;
  } finally {
    btn.disabled = false;
    btn.textContent = 'Run Scan';
  }
}

async function calculateCostBasis(ticker, fallbackPrice) {
  try {
    const txList = await transactions.list(ticker);
    const buys = txList.filter(t => t.action === 'Buy');
    if (buys.length > 0) {
      const totalCost = buys.reduce((s, t) => s + t.pricePerShare * t.numberOfShares, 0);
      const totalShares = buys.reduce((s, t) => s + t.numberOfShares, 0);
      return totalShares > 0 ? totalCost / totalShares : fallbackPrice;
    }
  } catch {}
  return fallbackPrice;
}

function evaluatePosition(item, scanData, allocationPct, costBasis, totalReturnPct,
  trailingStopPct, profitTargetPct, stockCap, etfCap) {
  let action, reasoning;
  const lines = [];

  lines.push(`=== ${item.ticker} (${item.type}) ===`);
  lines.push(`Current Price: ${formatCurrency(item.currentPrice)}`);
  lines.push(`Shares: ${formatShares(item.quantity)}`);
  lines.push(`Position Value: ${formatCurrency(item.value)}`);
  lines.push(`Cost Basis (avg buy): ${formatCurrency(costBasis)}`);
  lines.push(`Total Return: (${formatCurrency(item.currentPrice)} - ${formatCurrency(costBasis)}) / ${formatCurrency(costBasis)} x 100 = ${totalReturnPct >= 0 ? '+' : ''}${totalReturnPct.toFixed(2)}%`);
  lines.push(`Allocation: ${allocationPct.toFixed(2)}%`);
  lines.push('');

  if (scanData && scanData.twentyDaySma > 0) {
    const volumeRatio = scanData.avgVolume20Day > 0 ? scanData.closingVolume / scanData.avgVolume20Day : 0;
    lines.push('--- Yahoo Finance Scan Data ---');
    lines.push(`20-Day SMA: ${formatCurrency(scanData.twentyDaySma)}`);
    lines.push(`20-Day Avg Volume: ${scanData.avgVolume20Day.toLocaleString()}`);
    lines.push(`Today's Closing Volume: ${scanData.closingVolume.toLocaleString()}`);
    lines.push(`Volume Ratio: ${volumeRatio.toFixed(2)}x`);
    lines.push('');

    lines.push('--- Tier A: Risk Check ---');
    const belowSma = item.currentPrice < scanData.twentyDaySma;
    lines.push(`Price vs 20-Day SMA: ${formatCurrency(item.currentPrice)} ${belowSma ? '<' : '>='} ${formatCurrency(scanData.twentyDaySma)} → ${belowSma ? 'STOP LOSS TRIGGERED' : 'OK'}`);
    const aboveProfit = totalReturnPct >= profitTargetPct;
    lines.push(`Return ${totalReturnPct >= 0 ? '+' : ''}${totalReturnPct.toFixed(1)}% vs Target +${profitTargetPct}% → ${aboveProfit ? 'TRIM PROFIT TRIGGERED' : 'OK'}`);
    lines.push('');

    const cap = item.type === 'ETF' ? etfCap : stockCap;
    lines.push('--- Tier B: Concentration Check ---');
    lines.push(`Allocation ${allocationPct.toFixed(1)}% vs ${item.type} Cap ${cap}% → ${allocationPct > cap ? 'REBALANCE TRIGGERED' : 'OK'}`);
    lines.push('');

    lines.push('--- Tier C: Momentum Check ---');
    lines.push(`Volume Ratio ${volumeRatio.toFixed(2)}x vs Threshold 1.50x → ${volumeRatio >= 1.5 ? 'STRONG BUY TRIGGERED' : 'OK'}`);

    if (belowSma) {
      action = 'STOP_LOSS';
      reasoning = `Price (${formatCurrency(item.currentPrice)}) closed below 20-day SMA (${formatCurrency(scanData.twentyDaySma)}).`;
    } else if (aboveProfit) {
      action = 'TRIM_PROFIT';
      reasoning = `Return +${totalReturnPct.toFixed(1)}% exceeds target of +${profitTargetPct}%.`;
    } else if (allocationPct > cap) {
      action = 'REBALANCE_TRIM';
      reasoning = `${item.type} allocation ${allocationPct.toFixed(1)}% exceeds cap of ${cap}%.`;
    } else if (volumeRatio >= 1.5) {
      action = 'STRONG_BUY';
      reasoning = `Volume spike! Closing volume was ${volumeRatio.toFixed(1)}x its 20-day average.`;
    } else {
      action = 'HOLD';
      reasoning = 'Position is healthy. Within all thresholds.';
    }
  } else {
    lines.push('(Scan data unavailable — Yahoo Finance fetch failed)');
    const cap = item.type === 'ETF' ? etfCap : stockCap;
    if (totalReturnPct >= profitTargetPct) {
      action = 'TRIM_PROFIT';
      reasoning = `Return +${totalReturnPct.toFixed(1)}% exceeds target of +${profitTargetPct}%.`;
    } else if (allocationPct > cap) {
      action = 'REBALANCE_TRIM';
      reasoning = `${item.type} allocation ${allocationPct.toFixed(1)}% exceeds cap of ${cap}%.`;
    } else {
      action = 'HOLD';
      reasoning = 'Position is healthy. Within all thresholds.';
    }
  }

  lines.push('');
  lines.push(`>>> RESULT: ${ACTIONS[action].label}`);

  return {
    ticker: item.ticker, type: item.type, shares: item.quantity,
    currentPrice: item.currentPrice, totalValue: item.value,
    allocationPct, costBasis, totalReturnPct, action, reasoning,
    detailLog: lines.join('\n'),
  };
}

function renderResults(el, signals, thresholds) {
  const counts = {};
  for (const s of signals) {
    if (s.action !== 'HOLD') counts[s.action] = (counts[s.action] || 0) + 1;
  }

  const summaryCards = Object.entries(counts).map(([action, count]) =>
    `<div class="card p-8" style="flex:1;text-align:center;border-left:3px solid ${ACTIONS[action].color}">
      <div class="text-lg text-bold" style="color:${ACTIONS[action].color}">${count}</div>
      <div class="text-xs">${ACTIONS[action].label}</div>
    </div>`
  ).join('');

  const explanationHtml = `
    <div class="text-sm">
      ${Object.entries(ACTIONS).map(([key, a]) =>
        `<div class="flex gap-8 py-6" style="border-bottom:1px solid color-mix(in srgb, var(--outline) 20%, transparent)">
          <span class="badge" style="background:${a.color};color:#fff;min-width:100px;text-align:center">${a.label}</span>
          <span>${actionDescription(key)}</span>
        </div>`
      ).join('')}
    </div>
    <div class="text-xs text-muted mt-8">
      Thresholds: Profit Target ${thresholds.profitTargetPct}% · Stock Cap ${thresholds.stockCap}% · ETF Cap ${thresholds.etfCap}% · Volume Spike 1.5x
    </div>
  `;

  const detailHtml = signals.map(s => `
    <details class="mb-4">
      <summary class="text-sm text-bold" style="cursor:pointer">${s.ticker} — <span style="color:${ACTIONS[s.action].color}">${ACTIONS[s.action].label}</span></summary>
      <pre class="text-xs mt-4 p-8" style="background:var(--surface-variant);border-radius:8px;white-space:pre-wrap;overflow-x:auto">${s.detailLog}</pre>
    </details>
  `).join('');

  const tableRows = signals.map(s => {
    const a = ACTIONS[s.action];
    return `<tr class="clickable" data-ticker="${s.ticker}">
      <td class="text-bold">${s.ticker}</td>
      <td>${formatShares(s.shares)}</td>
      <td>${formatCurrency(s.currentPrice)}</td>
      <td>${formatCurrency(s.totalValue)}</td>
      <td>${s.allocationPct.toFixed(1)}%</td>
      <td class="${gainLossClass(s.totalReturnPct)}">${s.totalReturnPct >= 0 ? '+' : ''}${s.totalReturnPct.toFixed(1)}%</td>
      <td><span class="badge" style="background:${a.color};color:#fff;font-size:10px">${a.label}</span></td>
      <td class="text-xs">${s.reasoning}</td>
    </tr>`;
  }).join('');

  el.innerHTML = `
    ${summaryCards ? `<div class="flex gap-8 mb-16">${summaryCards}</div>` : ''}

    ${collapsibleCard('nda_explanation', 'Explanation', explanationHtml)}

    ${collapsibleCard('nda_detail', 'Detail on Analysis', detailHtml)}

    <div class="data-table-wrapper mt-8">
      <table class="data-table">
        <thead><tr><th>Ticker</th><th>Shares</th><th>Price</th><th>Value</th><th>Alloc%</th><th>Return%</th><th>Action</th><th>Reasoning</th></tr></thead>
        <tbody>${tableRows}</tbody>
      </table>
    </div>
  `;

  initCollapsibleCards(el);

  el.querySelectorAll('[data-ticker]').forEach(row => {
    row.addEventListener('click', () => navigate(`#/item/${row.dataset.ticker}`));
  });
}

function actionDescription(key) {
  switch (key) {
    case 'STOP_LOSS': return 'Price closed below the 20-day SMA. Technical breakdown — exit or protect position.';
    case 'TRIM_PROFIT': return 'Total return exceeds profit target. Harvest gains by selling a portion.';
    case 'REBALANCE_TRIM': return 'Position allocation exceeds concentration cap. Trim to reduce risk.';
    case 'STRONG_BUY': return 'Volume spike (1.5x+ average) with price above SMA. Consider adding at open.';
    case 'HOLD': return 'Position healthy. Trading above SMA, within all thresholds.';
    default: return '';
  }
}
