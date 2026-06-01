import { positions } from '../api.js';

const COLORS = [
  '#2E7D32', '#0277BD', '#E65100', '#6A1B9A', '#33691E', '#B71C1C',
  '#37474F', '#F9A825', '#C2185B', '#424242', '#5C6BC0', '#BF360C',
  '#00796B', '#455A64', '#4E342E', '#1A237E', '#00897B', '#880E4F',
];

function hashColor(ticker) {
  let hash = 0;
  for (let i = 0; i < ticker.length; i++) hash = ((hash << 5) - hash + ticker.charCodeAt(i)) | 0;
  return COLORS[(hash & 0x7FFFFFFF) % COLORS.length];
}

export function tickerIcon(ticker, name, opts = {}) {
  const color = hashColor(ticker);
  const size = opts.small ? 'ticker-icon-sm' : '';
  const letter = (name || ticker || '?')[0].toUpperCase();
  const logoUrl = positions.logoUrl(ticker);
  return `
    <div class="ticker-icon ${size}" style="background:linear-gradient(135deg, ${color}dd, ${color})">
      <span>${letter}</span>
      <img src="${logoUrl}" alt="" loading="lazy" onerror="this.style.display='none'">
    </div>
  `;
}
