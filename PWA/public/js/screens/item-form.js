import { positions, yahoo } from '../api.js';
import { navigate } from '../router.js';
import { showToast } from '../components/confirm-dialog.js';

export async function render(container, { ticker } = {}) {
  const isEdit = !!ticker;
  let item = { ticker: '', name: '', type: 'Stock', currentPrice: 0, quantity: 0 };
  if (isEdit) {
    const existing = await positions.get(ticker).catch(() => null);
    if (existing) item = existing;
    else item.ticker = ticker;
  }

  container.innerHTML = `<div class="screen">
    <h2 class="mb-16">${isEdit ? 'Edit' : 'Add'} Position</h2>
    <div class="form-group">
      <label>Ticker</label>
      <input type="text" class="input" id="item-ticker" value="${item.ticker}" ${isEdit ? 'readonly' : ''} style="text-transform:uppercase">
    </div>
    <div class="form-group">
      <label>Name</label>
      <input type="text" class="input" id="item-name" value="${item.name}">
    </div>
    <div class="form-row">
      <div class="form-group">
        <label>Type</label>
        <select class="select" id="item-type">
          ${['Stock','ETF','Bond','MutualFund','Crypto','Other'].map(t => `<option${t===item.type?' selected':''}>${t}</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label>Quantity</label>
        <input type="number" class="input" id="item-qty" value="${item.quantity}" step="any">
      </div>
    </div>
    <div class="form-group">
      <label>Current Price</label>
      <div class="flex gap-8">
        <input type="number" class="input" id="item-price" value="${item.currentPrice}" step="0.01" style="flex:1">
        <button class="btn btn-secondary" id="fetch-btn">Fetch</button>
      </div>
    </div>
    <div class="flex gap-8 mt-16">
      <button class="btn btn-primary" id="save-btn" style="flex:1">Save</button>
      <button class="btn btn-outline" onclick="history.back()">Cancel</button>
    </div>
  </div>`;

  document.getElementById('fetch-btn').addEventListener('click', async () => {
    const t = document.getElementById('item-ticker').value.toUpperCase().trim();
    if (!t) return;
    try {
      const q = await yahoo.quote(t);
      document.getElementById('item-price').value = q.price;
      if (q.shortName) document.getElementById('item-name').value = q.shortName;
      showToast(`Fetched: ${q.price}`);
    } catch (err) { showToast('Fetch failed: ' + err.message); }
  });

  document.getElementById('save-btn').addEventListener('click', async () => {
    const data = {
      ticker: document.getElementById('item-ticker').value.toUpperCase().trim(),
      name: document.getElementById('item-name').value.trim(),
      type: document.getElementById('item-type').value,
      quantity: parseFloat(document.getElementById('item-qty').value) || 0,
      currentPrice: parseFloat(document.getElementById('item-price').value) || 0,
    };
    data.value = data.currentPrice * data.quantity;
    if (!data.ticker) { showToast('Ticker required'); return; }
    try {
      await positions.upsert(data);
      showToast('Saved');
      navigate(`#/item/${data.ticker}`);
    } catch (err) { showToast('Error: ' + err.message); }
  });
}
