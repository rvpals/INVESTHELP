import { getPref } from '../preferences.js';

export function confirmAction(title, message) {
  if (!getPref('warn_before_delete')) return Promise.resolve(true);

  return new Promise(resolve => {
    const overlay = document.getElementById('dialog-overlay');
    overlay.className = 'dialog-overlay';
    overlay.innerHTML = `
      <div class="dialog">
        <div class="dialog-title">${title}</div>
        <p>${message}</p>
        <div class="dialog-actions">
          <button class="btn btn-outline" id="dialog-cancel">Cancel</button>
          <button class="btn btn-error" id="dialog-confirm">Delete</button>
        </div>
      </div>
    `;
    document.getElementById('dialog-cancel').addEventListener('click', () => {
      overlay.className = 'dialog-overlay hidden';
      resolve(false);
    });
    document.getElementById('dialog-confirm').addEventListener('click', () => {
      overlay.className = 'dialog-overlay hidden';
      resolve(true);
    });
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) { overlay.className = 'dialog-overlay hidden'; resolve(false); }
    });
  });
}

export function showToast(message) {
  const existing = document.querySelector('.toast');
  if (existing) existing.remove();
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.textContent = message;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}
