import { watchlists, yahoo } from '../api.js';
import { navigate } from '../router.js';
import { collapsibleCard, initCollapsibleCards } from '../components/collapsible-card.js';
import { confirmAction, showToast } from '../components/confirm-dialog.js';
import { formatCurrency, formatSignedCurrency, formatPercent, gainLossClass } from '../utils/format.js';
import { formatDate, todayEpochDays } from '../utils/dates.js';

export async function render(container) {
  const lists = await watchlists.list();
  const listsWithItems = await Promise.all(lists.map(async l => ({
    ...l, items: await watchlists.items(l.id).catch(() => [])
  })));

  container.innerHTML = `<div class="screen">
    <div class="flex justify-between items-center mb-16">
      <h2>Watch Lists</h2>
      <button class="btn btn-primary btn-sm" id="add-wl">+ New List</button>
    </div>
    ${listsWithItems.map(l => collapsibleCard('wl_' + l.id, l.name + ` (${l.items.length})`, `
      <div class="data-table-wrapper">
        <table class="data-table">
          <thead><tr><th>Ticker</th><th>Shares</th><th>Added @</th><th>Change</th><th></th></tr></thead>
          <tbody>
            ${l.items.map(it => {
              const change = (it.priceWhenAdded || 0) * (it.shares || 0);
              return `<tr>
                <td class="clickable text-primary text-bold" data-ticker="${it.ticker}">${it.ticker}</td>
                <td>${it.shares}</td>
                <td>${formatCurrency(it.priceWhenAdded)}</td>
                <td>${formatDate(it.addedDate)}</td>
                <td><button class="btn-icon wl-del-item" data-item="${it.id}">&times;</button></td>
              </tr>`;
            }).join('')}
            ${l.items.length === 0 ? '<tr><td colspan="5" class="text-center text-muted">Empty</td></tr>' : ''}
          </tbody>
        </table>
      </div>
      <div class="flex gap-8 mt-8">
        <button class="btn btn-sm btn-secondary add-ticker-btn" data-wl="${l.id}">+ Add Ticker</button>
        <button class="btn btn-sm btn-outline rename-wl" data-wl="${l.id}">Rename</button>
        <button class="btn btn-sm btn-error delete-wl" data-wl="${l.id}">Delete</button>
      </div>
    `, { defaultExpanded: true })).join('')}
    ${lists.length === 0 ? '<div class="text-center text-muted p-16">No watch lists. Create one to get started.</div>' : ''}
  </div>`;

  initCollapsibleCards(container);

  document.getElementById('add-wl').addEventListener('click', async () => {
    const name = prompt('Watch list name:');
    if (name) { await watchlists.create({ name }); render(container); }
  });

  container.querySelectorAll('.add-ticker-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const ticker = prompt('Ticker:');
      if (!ticker) return;
      const shares = parseFloat(prompt('Shares:', '0')) || 0;
      let price = 0;
      try { const q = await yahoo.quote(ticker.toUpperCase()); price = q.price; } catch {}
      await watchlists.addItem(btn.dataset.wl, { ticker: ticker.toUpperCase(), shares, priceWhenAdded: price, addedDate: todayEpochDays() });
      showToast('Added ' + ticker);
      render(container);
    });
  });

  container.querySelectorAll('.rename-wl').forEach(btn => {
    btn.addEventListener('click', async () => {
      const name = prompt('New name:');
      if (name) { await watchlists.update(btn.dataset.wl, { name }); render(container); }
    });
  });

  container.querySelectorAll('.delete-wl').forEach(btn => {
    btn.addEventListener('click', async () => {
      if (await confirmAction('Delete', 'Delete this watch list?')) {
        await watchlists.delete(btn.dataset.wl);
        render(container);
      }
    });
  });

  container.querySelectorAll('.wl-del-item').forEach(btn => {
    btn.addEventListener('click', async () => {
      if (await confirmAction('Delete', 'Remove this item?')) {
        await watchlists.deleteItem(btn.dataset.item);
        render(container);
      }
    });
  });

  container.querySelectorAll('[data-ticker]').forEach(el => {
    el.addEventListener('click', () => navigate(`#/item/${el.dataset.ticker}`));
  });
}
