import { navigate, getHash } from '../router.js';

const NAV_ITEMS = [
  { label: 'Dashboard', icon: '&#128200;', route: '#/' },
  { label: 'Positions', icon: '&#128176;', route: '#/positions' },
  { label: 'Transaction', icon: '&#128221;', route: '#/transactions' },
];

export function renderBottomNav() {
  const el = document.getElementById('bottom-nav');
  el.innerHTML = `<div class="bottom-nav">${NAV_ITEMS.map(item => `
    <button class="nav-item${isActive(item.route) ? ' active' : ''}" data-route="${item.route}">
      <span class="nav-icon">${item.icon}</span>
      <span>${item.label}</span>
    </button>
  `).join('')}</div>`;

  el.querySelectorAll('.nav-item').forEach(btn => {
    btn.addEventListener('click', () => navigate(btn.dataset.route));
  });
}

function isActive(route) {
  const hash = getHash();
  if (route === '#/') return hash === '/';
  return hash.startsWith(route.slice(1));
}

export function updateBottomNav() {
  const nav = document.querySelector('.bottom-nav');
  if (!nav) return;
  nav.querySelectorAll('.nav-item').forEach(btn => {
    btn.classList.toggle('active', isActive(btn.dataset.route));
  });
}
