import { accounts } from '../api.js';
import { navigate } from '../router.js';
import { showToast } from '../components/confirm-dialog.js';

export async function render(container, { id } = {}) {
  const isEdit = id && id !== '-1';
  let acct = { name: '', description: '', initialValue: 0 };
  if (isEdit) acct = await accounts.get(id);

  container.innerHTML = `<div class="screen">
    <h2 class="mb-16">${isEdit ? 'Edit' : 'New'} Account</h2>
    <div class="form-group"><label>Name</label><input type="text" class="input" id="acct-name" value="${acct.name}"></div>
    <div class="form-group"><label>Description</label><textarea class="textarea" id="acct-desc" rows="2">${acct.description || ''}</textarea></div>
    <div class="form-group"><label>Initial Value</label><input type="number" class="input" id="acct-val" value="${acct.initialValue}" step="0.01"></div>
    <div class="flex gap-8 mt-16">
      <button class="btn btn-primary" id="save-btn" style="flex:1">${isEdit ? 'Update' : 'Create'}</button>
      <button class="btn btn-outline" onclick="history.back()">Cancel</button>
    </div>
  </div>`;

  document.getElementById('save-btn').addEventListener('click', async () => {
    const data = { name: document.getElementById('acct-name').value.trim(), description: document.getElementById('acct-desc').value.trim(), initialValue: parseFloat(document.getElementById('acct-val').value) || 0 };
    if (!data.name) { showToast('Name required'); return; }
    try {
      if (isEdit) await accounts.update(id, data);
      else await accounts.create(data);
      showToast('Saved');
      navigate('#/accounts');
    } catch (err) { showToast('Error: ' + err.message); }
  });
}
