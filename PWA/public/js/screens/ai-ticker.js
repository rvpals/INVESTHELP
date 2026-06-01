import { aiLibrary, yahoo } from '../api.js';
import { showToast } from '../components/confirm-dialog.js';

export async function render(container, { ticker }) {
  const prompts = await aiLibrary.list();

  container.innerHTML = `<div class="screen">
    <h2 class="mb-16">AI Analysis: ${ticker}</h2>
    <div class="form-group">
      <label>Select Prompt Template</label>
      <select class="select" id="ai-prompt-select">
        ${prompts.map(p => `<option value="${p.id}">${p.name}</option>`).join('')}
        <option value="custom">Custom Prompt</option>
      </select>
    </div>
    <div class="form-group">
      <label>Prompt</label>
      <textarea class="textarea" id="ai-prompt" rows="4">${prompts[0]?.promptText?.replace('{TICKER}', ticker) || ''}</textarea>
    </div>
    <button class="btn btn-primary w-full mb-16" id="ai-run">Run Analysis</button>
    <div id="ai-result" class="text-sm"></div>
    <button class="btn btn-outline w-full mt-16" onclick="history.back()">Back</button>
  </div>`;

  document.getElementById('ai-prompt-select').addEventListener('change', (e) => {
    const id = e.target.value;
    if (id === 'custom') { document.getElementById('ai-prompt').value = ''; return; }
    const p = prompts.find(p => p.id == id);
    if (p) document.getElementById('ai-prompt').value = p.promptText.replace('{TICKER}', ticker);
  });

  document.getElementById('ai-run').addEventListener('click', () => {
    showToast('AI analysis requires API key configuration in Settings');
    document.getElementById('ai-result').innerHTML = '<div class="text-muted">Configure your AI API key in Settings to use this feature.</div>';
  });
}
