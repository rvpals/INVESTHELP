import { positions as positionsApi, volatility as volatilityApi } from '../api.js';
import { navigate } from '../router.js';
import { tickerIcon } from '../components/ticker-icon.js';
import { formatCurrency } from '../utils/format.js';

const LABEL_ORDER = ['Low', 'Moderate', 'High', 'Very High'];

const LABEL_META = {
  'Low':       { color: '#388E3C', range: '< 15%'  },
  'Moderate':  { color: '#F57C00', range: '15–30%' },
  'High':      { color: '#E64A19', range: '30–60%' },
  'Very High': { color: '#B71C1C', range: '> 60%'  },
};

function hexToRgba(hex, a) {
  const r = parseInt(hex.slice(1,3),16), g = parseInt(hex.slice(3,5),16), b = parseInt(hex.slice(5,7),16);
  return `rgba(${r},${g},${b},${a})`;
}

export async function render(container) {
  container.innerHTML = `<div class="screen">
    <div class="flex items-center gap-8 mb-16">
      <button class="btn btn-sm btn-outline" onclick="history.back()">&#8592; Back</button>
      <h2 style="flex:1">Volatility Analysis</h2>
      <button class="btn btn-sm btn-outline" id="btn-refresh">&#8635; Refresh</button>
    </div>
    <div id="va-body">
      <div class="text-center p-32">
        <div class="spinner"></div>
        <div class="text-muted mt-12">Loading positions…</div>
      </div>
    </div>
  </div>`;

  let activeTab = 0; // 0 = Stocks, 1 = ETFs
  let results = [];  // { ticker, name, type, shares, value, vol, error, loading }
  let running = false;

  async function runAnalysis(force = false) {
    if (running) return;
    running = true;
    document.getElementById('btn-refresh')?.setAttribute('disabled', 'true');

    let allPositions;
    try {
      allPositions = await positionsApi.list();
    } catch (err) {
      document.getElementById('va-body').innerHTML =
        `<div class="card p-16 text-center"><div class="text-muted">Failed to load positions: ${err.message}</div></div>`;
      running = false;
      return;
    }

    const eligible = allPositions.filter(p => p.type === 'Stock' || p.type === 'ETF');
    if (eligible.length === 0) {
      document.getElementById('va-body').innerHTML =
        `<div class="card p-16 text-center"><div class="text-muted">No Stock or ETF positions found.</div></div>`;
      running = false;
      return;
    }

    // Prime results with loading state
    results = eligible.map(p => ({
      ticker: p.ticker, name: p.name, type: p.type,
      shares: p.quantity, value: p.value,
      vol: null, error: null, loading: true,
    }));
    renderAll();

    // Fetch one-by-one, updating incrementally
    for (let i = 0; i < eligible.length; i++) {
      const p = eligible[i];
      try {
        const vol = await volatilityApi.get(p.ticker, force);
        results[i] = { ...results[i], vol, loading: false };
      } catch (err) {
        results[i] = { ...results[i], error: err.message || 'Failed', loading: false };
      }
      renderAll();
    }

    running = false;
    document.getElementById('btn-refresh')?.removeAttribute('disabled');
  }

  function renderAll() {
    const body = document.getElementById('va-body');
    if (!body) return;

    const stocks = results.filter(r => r.type === 'Stock');
    const etfs   = results.filter(r => r.type === 'ETF');
    const currentList = activeTab === 0 ? stocks : etfs;

    const total  = currentList.length;
    const loaded = currentList.filter(r => !r.loading).length;
    const showProgress = loaded < total;

    // Group loaded items
    const grouped = {};
    for (const label of LABEL_ORDER) {
      grouped[label] = currentList
        .filter(r => r.vol && r.vol.volatilityLabel === label)
        .sort((a, b) => (a.vol?.annualizedVolPct ?? 0) - (b.vol?.annualizedVolPct ?? 0));
    }
    const loadingItems = currentList.filter(r => r.loading);
    const errorItems   = currentList.filter(r => !r.loading && !r.vol);

    let html = `
      <div class="tab-bar mb-16" style="display:flex;gap:0;border-bottom:2px solid var(--outline,#ccc)">
        ${[`Stocks (${stocks.length})`, `ETFs (${etfs.length})`].map((label, i) => `
          <button class="tab-btn${activeTab === i ? ' active' : ''}"
            data-tab="${i}"
            style="flex:1;padding:10px 0;font-weight:${activeTab === i ? '700' : '400'};
              background:none;border:none;cursor:pointer;font-size:14px;
              color:${activeTab === i ? 'var(--primary,#6750A4)' : 'var(--on-surface-variant,#49454F)'};
              border-bottom:${activeTab === i ? '2px solid var(--primary,#6750A4)' : '2px solid transparent'};
              margin-bottom:-2px">
            ${label}
          </button>`).join('')}
      </div>

      ${showProgress ? `
        <div class="mb-8">
          <div style="height:4px;border-radius:2px;background:var(--surface-variant,#E7E0EC);overflow:hidden">
            <div style="height:100%;width:${total > 0 ? (loaded/total*100).toFixed(0) : 0}%;background:var(--primary,#6750A4);transition:width .3s"></div>
          </div>
          <div class="text-xs text-muted mt-4">Fetching ${loaded} / ${total}…</div>
        </div>` : ''}
    `;

    // Groups
    for (const label of LABEL_ORDER) {
      const group = grouped[label];
      if (!group.length) continue;
      const { color, range } = LABEL_META[label];
      html += groupHeader(label, range, group.length, color);
      for (const item of group) html += itemRow(item, color);
    }

    if (loadingItems.length) {
      html += groupHeader('Loading', '', loadingItems.length, 'var(--on-surface-variant,#49454F)');
      for (const item of loadingItems) {
        html += `<div class="flex items-center gap-12 py-10 px-16" style="border-bottom:1px solid color-mix(in srgb,var(--outline,#ccc) 30%,transparent)">
          <div class="spinner" style="width:16px;height:16px;border-width:2px"></div>
          <span class="text-bold" style="font-size:14px">${item.ticker}</span>
          <span class="text-xs text-muted" style="margin-left:auto">Fetching…</span>
        </div>`;
      }
    }

    if (errorItems.length) {
      html += groupHeader('Failed', '', errorItems.length, 'var(--error,#B3261E)');
      for (const item of errorItems) {
        html += `<div class="flex items-center gap-12 py-10 px-16" style="border-bottom:1px solid color-mix(in srgb,var(--outline,#ccc) 30%,transparent)">
          <span class="text-bold" style="font-size:14px">${item.ticker}</span>
          <span class="text-xs" style="color:var(--error,#B3261E);margin-left:auto">Failed</span>
        </div>`;
      }
    }

    if (!showProgress && !LABEL_ORDER.some(l => grouped[l].length) && !loadingItems.length && !errorItems.length) {
      html += `<div class="text-center text-muted p-24">No results yet.</div>`;
    }

    body.innerHTML = html;

    // Tab listeners
    body.querySelectorAll('[data-tab]').forEach(btn => {
      btn.addEventListener('click', () => {
        activeTab = parseInt(btn.dataset.tab);
        renderAll();
      });
    });

    // Row click listeners
    body.querySelectorAll('[data-ticker]').forEach(el => {
      el.addEventListener('click', () => navigate(`#/item/${el.dataset.ticker}`));
    });
  }

  function groupHeader(label, range, count, color) {
    const bg = color.startsWith('#') ? hexToRgba(color, 0.08) : 'rgba(0,0,0,0.04)';
    const textColor = color;
    return `<div class="flex items-center gap-8 px-16 py-8"
      style="background:${bg};border-bottom:1px solid color-mix(in srgb,var(--outline,#ccc) 30%,transparent)">
      <div style="width:8px;height:8px;border-radius:50%;background:${color};flex-shrink:0"></div>
      <span style="font-size:11px;font-weight:700;letter-spacing:.5px;color:${textColor};text-transform:uppercase">${label}</span>
      ${range ? `<span style="font-size:11px;color:${color};opacity:.7">${range}</span>` : ''}
      <span style="margin-left:auto;font-size:11px;font-weight:700;color:${color};
        background:${color.startsWith('#') ? hexToRgba(color, 0.15) : 'rgba(0,0,0,.1)'};
        border-radius:10px;padding:1px 8px">${count}</span>
    </div>`;
  }

  function itemRow(item, labelColor) {
    const vol = item.vol;
    const posValue = vol ? vol.currentPrice * item.shares : item.value;
    const volPct = vol ? vol.annualizedVolPct.toFixed(1) + '%' : '—';
    const bg = labelColor.startsWith('#') ? hexToRgba(labelColor, 0.10) : 'transparent';
    const badgeBg = labelColor.startsWith('#') ? hexToRgba(labelColor, 0.12) : 'rgba(0,0,0,.08)';

    return `<div class="flex items-center gap-12 px-16 py-10 clickable" data-ticker="${item.ticker}"
      style="border-bottom:1px solid color-mix(in srgb,var(--outline,#ccc) 30%,transparent);cursor:pointer"
      onmouseenter="this.style.background='var(--surface-variant,#E7E0EC)'"
      onmouseleave="this.style.background=''">
      ${tickerIcon(item.ticker, item.name, { small: true })}
      <div style="flex:1;min-width:0">
        <div class="text-bold" style="font-size:14px">${item.ticker}</div>
        ${item.name && item.name !== item.ticker
          ? `<div class="text-xs text-muted" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:200px">${item.name}</div>`
          : ''}
      </div>
      <div style="text-align:right;flex-shrink:0">
        <div style="font-size:13px;font-weight:700;color:${labelColor};
          background:${badgeBg};border-radius:6px;padding:2px 8px;display:inline-block">
          ${volPct}
        </div>
        <div class="text-xs text-muted mt-2">${formatCurrency(posValue)}</div>
      </div>
    </div>`;
  }

  document.getElementById('btn-refresh').addEventListener('click', () => {
    results = [];
    runAnalysis(true);
  });

  runAnalysis(false);
}
