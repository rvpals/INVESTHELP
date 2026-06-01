import { transactions, positions } from '../api.js';
import { navigate } from '../router.js';
import { showToast } from '../components/confirm-dialog.js';
import { formatCurrency } from '../utils/format.js';
import { todayEpochDays, dateInputValue, dateInputToEpochDays } from '../utils/dates.js';

export async function render(container, { id } = {}) {
  const isEdit = id && id !== '-1';
  let tx = { date: todayEpochDays(), time: null, action: 'Buy', ticker: '', numberOfShares: 0, pricePerShare: 0, totalAmount: 0, note: '' };
  if (isEdit) tx = await transactions.get(id);

  const posList = await positions.list();
  const tickers = posList.map(p => p.ticker);

  container.innerHTML = `<div class="screen">
    <h2 class="mb-16">${isEdit ? 'Edit' : 'New'} Transaction</h2>
    <div class="form-group">
      <label>Date</label>
      <input type="date" class="input" id="tx-date" value="${dateInputValue(tx.date)}">
    </div>
    <div class="form-row">
      <div class="form-group">
        <label>Action</label>
        <select class="select" id="tx-action">
          <option value="Buy"${tx.action==='Buy'?' selected':''}>Buy</option>
          <option value="Sell"${tx.action==='Sell'?' selected':''}>Sell</option>
        </select>
      </div>
      <div class="form-group">
        <label>Ticker</label>
        <input type="text" class="input" id="tx-ticker" value="${tx.ticker}" list="ticker-list" style="text-transform:uppercase">
        <datalist id="ticker-list">${tickers.map(t => `<option value="${t}">`).join('')}</datalist>
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label>Shares</label>
        <input type="number" class="input" id="tx-shares" value="${tx.numberOfShares}" step="any">
      </div>
      <div class="form-group">
        <label>Price per Share</label>
        <input type="number" class="input" id="tx-price" value="${tx.pricePerShare}" step="0.01">
      </div>
    </div>
    <div class="form-group">
      <label>Total Amount</label>
      <input type="text" class="input" id="tx-total" value="${formatCurrency(tx.totalAmount)}" readonly>
    </div>
    <div class="form-group">
      <label>Note</label>
      <textarea class="textarea" id="tx-note" rows="2">${tx.note || ''}</textarea>
    </div>
    <div class="flex gap-8 mt-16">
      <button class="btn btn-primary" id="save-btn" style="flex:1">${isEdit ? 'Update' : 'Create'}</button>
      <button class="btn btn-outline" onclick="history.back()">Cancel</button>
    </div>
  </div>`;

  const sharesEl = document.getElementById('tx-shares');
  const priceEl = document.getElementById('tx-price');
  const totalEl = document.getElementById('tx-total');
  const updateTotal = () => {
    const total = (parseFloat(sharesEl.value) || 0) * (parseFloat(priceEl.value) || 0);
    totalEl.value = formatCurrency(total);
  };
  sharesEl.addEventListener('input', updateTotal);
  priceEl.addEventListener('input', updateTotal);

  document.getElementById('save-btn').addEventListener('click', async () => {
    const data = {
      date: dateInputToEpochDays(document.getElementById('tx-date').value),
      action: document.getElementById('tx-action').value,
      ticker: document.getElementById('tx-ticker').value.toUpperCase().trim(),
      numberOfShares: parseFloat(sharesEl.value) || 0,
      pricePerShare: parseFloat(priceEl.value) || 0,
      totalAmount: (parseFloat(sharesEl.value) || 0) * (parseFloat(priceEl.value) || 0),
      note: document.getElementById('tx-note').value.trim(),
    };
    if (!data.ticker) { showToast('Ticker is required'); return; }
    try {
      if (isEdit) await transactions.update(id, data);
      else await transactions.create(data);
      showToast(isEdit ? 'Updated' : 'Created');
      navigate('#/transactions');
    } catch (err) {
      showToast('Error: ' + err.message);
    }
  });
}
