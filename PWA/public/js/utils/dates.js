export function toEpochDays(date) {
  return Math.floor(date.getTime() / 86400000);
}

export function fromEpochDays(days) {
  return new Date(days * 86400000);
}

export function todayEpochDays() {
  return toEpochDays(new Date());
}

export function formatDate(epochDays) {
  return fromEpochDays(epochDays).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

export function formatDateShort(epochDays) {
  return fromEpochDays(epochDays).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

export function formatDateTime(epochMs) {
  return new Date(epochMs).toLocaleString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' });
}

export function formatTimestamp(epochSec) {
  return new Date(epochSec * 1000).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' });
}

export function formatTimeAgo(epochSec) {
  const diff = Math.floor(Date.now() / 1000) - epochSec;
  if (diff < 60) return 'just now';
  if (diff < 3600) return Math.floor(diff / 60) + 'm ago';
  if (diff < 86400) return Math.floor(diff / 3600) + 'h ago';
  if (diff < 604800) return Math.floor(diff / 86400) + 'd ago';
  return formatTimestamp(epochSec);
}

export function dateInputValue(epochDays) {
  const d = fromEpochDays(epochDays);
  return d.toISOString().slice(0, 10);
}

export function dateInputToEpochDays(str) {
  const d = new Date(str + 'T00:00:00');
  return toEpochDays(d);
}

export function daysSince(epochDays) {
  return todayEpochDays() - epochDays;
}
