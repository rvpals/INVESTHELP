import { accounts } from '../api.js';
import { navigate } from '../router.js';
import { confirmAction, showToast } from '../components/confirm-dialog.js';
import { formatCurrency } from '../utils/format.js';

export async function render(container) {
  const list = await accounts.list();

  container.innerHTML = `<div class="screen">
    <div class="flex justify-between items-center mb-16">
      <h2>Accounts</h2>
      <button class="btn btn-primary btn-sm" id="add-acct">+ New</button>
    </div>
    ${list.map(a => `
      <div class="card p-12 mb-8 clickable" data-id="${a.id}" style="cursor:pointer">
        <div class="flex justify-between items-center">
          <div><div class="text-bold">${a.name}</div><div class="text-xs text-muted">${a.description || ''}</div></div>
          <div class="text-right">
            ${a.lastValue ? `<div class="text-bold">${formatCurrency(a.lastValue)}</div>` : ''}
            <div class="text-xs text-muted">Initial: ${formatCurrency(a.initialValue)}</div>
          </div>
        </div>
      </div>
    `).join('')}
    ${list.length === 0 ? '<div class="text-center text-muted p-16">No accounts</div>' : ''}
  </div>`;

  document.getElementById('add-acct').addEventListener('click', () => navigate('#/account-form'));
  container.querySelectorAll('[data-id]').forEach(el => {
    el.addEventListener('click', () => navigate(`#/account/${el.dataset.id}`));
  });
}
