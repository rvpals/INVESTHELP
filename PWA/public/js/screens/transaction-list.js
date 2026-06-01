import { transactions, positions } from '../api.js';
import { navigate } from '../router.js';
import { confirmAction, showToast } from '../components/confirm-dialog.js';
import { formatCurrency, formatSignedCurrency, gainLossClass, formatShares } from '../utils/format.js';
import { formatDate } from '../utils/dates.js';

export async function render(container) {
  const [txList, posList] = await Promise.all([transactions.list(), positions.list()]);
  const priceMap = {};
  posList.forEach(p => priceMap[p.ticker] = p.currentPrice);

  container.innerHTML = `<div class="screen">
    <div class="flex justify-between items-center mb-16">
      <h2>Transactions</h2>
      <button class="btn btn-primary btn-sm" id="add-tx">+ New</button>
    </div>
    <div id="tx-list">
      ${txList.map(tx => txRow(tx, priceMap[tx.ticker] || 0)).join('')}
      ${txList.length === 0 ? '<div class="text-center text-muted p-16">No transactions yet</div>' : ''}
    </div>
  </div>`;

  document.getElementById('add-tx').addEventListener('click', () => navigate('#/transaction-form'));

  container.querySelectorAll('.tx-row').forEach(el => {
    el.addEventListener('click', () => navigate(`#/transaction-form/${el.dataset.id}`));
  });

  container.querySelectorAll('.tx-del').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      if (await confirmAction('Delete', 'Delete this transaction?')) {
        await transactions.delete(btn.dataset.id);
        showToast('Deleted');
        render(container);
      }
    });
  });
}

function txRow(tx, currentPrice) {
  const gl = (currentPrice - tx.pricePerShare) * tx.numberOfShares;
  return `
    <div class="card p-12 mb-8 tx-row" data-id="${tx.id}" style="cursor:pointer">
      <div class="flex justify-between items-center">
        <div>
          <span class="badge ${tx.action === 'Buy' ? 'badge-green' : 'badge-red'}">${tx.action}</span>
          <span class="text-bold" style="margin-left:6px">${tx.ticker}</span>
        </div>
        <div class="flex items-center gap-8">
          <span class="${gainLossClass(gl)} text-bold">${formatSignedCurrency(gl)}</span>
          <button class="btn-icon tx-del" data-id="${tx.id}">&times;</button>
        </div>
      </div>
      <div class="text-xs text-muted mt-4">
        ${formatShares(tx.numberOfShares)} shares @ ${formatCurrency(tx.pricePerShare)} &middot; ${formatDate(tx.date)}
        ${tx.note ? ' &middot; ' + tx.note : ''}
      </div>
    </div>
  `;
}
