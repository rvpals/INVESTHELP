export function formatCurrency(n) {
  if (n == null || isNaN(n)) return '$0.00';
  const abs = Math.abs(n);
  const formatted = abs.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return (n < 0 ? '-$' : '$') + formatted;
}

export function formatCurrencyShort(n) {
  if (n == null || isNaN(n)) return '$0';
  const abs = Math.abs(n);
  if (abs >= 1e12) return (n < 0 ? '-' : '') + '$' + (abs / 1e12).toFixed(1) + 'T';
  if (abs >= 1e9) return (n < 0 ? '-' : '') + '$' + (abs / 1e9).toFixed(1) + 'B';
  if (abs >= 1e6) return (n < 0 ? '-' : '') + '$' + (abs / 1e6).toFixed(1) + 'M';
  if (abs >= 1e3) return (n < 0 ? '-' : '') + '$' + (abs / 1e3).toFixed(1) + 'K';
  return formatCurrency(n);
}

export function formatPercent(n) {
  if (n == null || isNaN(n)) return '0.00%';
  return (n >= 0 ? '+' : '') + n.toFixed(2) + '%';
}

export function formatShares(n) {
  if (n == null) return '0';
  if (n === Math.floor(n)) return n.toLocaleString();
  return n.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 4 });
}

export function formatNumber(n, decimals = 2) {
  if (n == null || isNaN(n)) return '0';
  return n.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
}

export function gainLossClass(n) {
  if (n > 0) return 'text-green';
  if (n < 0) return 'text-red';
  return '';
}

export function gainLossBadgeClass(n) {
  if (n > 0) return 'badge-green';
  if (n < 0) return 'badge-red';
  return '';
}

export function formatSignedCurrency(n) {
  if (n == null || isNaN(n)) return '$0.00';
  return (n >= 0 ? '+' : '') + formatCurrency(n);
}
