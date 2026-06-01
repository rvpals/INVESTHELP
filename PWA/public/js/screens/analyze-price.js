import { yahoo, transactions } from '../api.js';
import { dataTable } from '../components/data-table.js';
import { formatCurrency } from '../utils/format.js';

export async function render(container, { ticker }) {
  container.innerHTML = `<div class="screen"><h2>Analyze Price: ${ticker}</h2><div class="spinner mt-16"></div></div>`;

  try {
    const [quote, stats, scan] = await Promise.all([
      yahoo.quote(ticker),
      transactions.stats(ticker, {}),
      yahoo.scan(ticker).catch(() => null),
    ]);

    const historicRows = [];
    for (const [label, range, interval] of [['1 Week','5d','1d'],['1 Month','1mo','1d'],['1 Year','1y','1wk'],['Max','max','1mo']]) {
      try {
        const h = await yahoo.history(ticker, range, interval);
        if (h.length) {
          const prices = h.map(p => p.close);
          historicRows.push([label, formatCurrency(Math.max(...prices)), formatCurrency(Math.min(...prices))]);
        }
      } catch {}
    }

    container.innerHTML = `<div class="screen">
      <h2 class="mb-16">Analyze Price: ${ticker}</h2>
      <div class="card p-12 mb-16">
        <div class="text-center"><div class="text-xs text-muted">Current Price</div><div class="text-2xl text-bold">${formatCurrency(quote.price)}</div></div>
      </div>
      <h3 class="mb-8">Transaction Statistics</h3>
      ${stats.count > 0 ? `
        <div class="flex gap-8 mb-16">
          <div class="card p-8" style="flex:1;text-align:center"><div class="text-xs text-muted">Avg</div><div class="text-bold">${formatCurrency(stats.avg)}</div></div>
          <div class="card p-8" style="flex:1;text-align:center"><div class="text-xs text-muted">Max</div><div class="text-bold">${formatCurrency(stats.max)}</div></div>
          <div class="card p-8" style="flex:1;text-align:center"><div class="text-xs text-muted">Min</div><div class="text-bold">${formatCurrency(stats.min)}</div></div>
        </div>
      ` : '<div class="text-muted mb-16">No transactions for this ticker</div>'}
      <h3 class="mb-8">Historic Prices</h3>
      ${dataTable(['Period', 'High', 'Low'], historicRows)}
      ${scan ? `
        <h3 class="mt-16 mb-8">Scan Data</h3>
        <div class="card p-12">
          <div>20-Day SMA: ${formatCurrency(scan.twentyDaySma)}</div>
        </div>
      ` : ''}
      <button class="btn btn-outline w-full mt-16" onclick="history.back()">Back</button>
    </div>`;
  } catch (err) {
    container.innerHTML = `<div class="screen"><h2>Analyze Price: ${ticker}</h2><div class="text-muted mt-16">Error: ${err.message}</div></div>`;
  }
}
