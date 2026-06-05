const entries = [];
const MAX_ENTRIES = 500;

const originalLog = console.log;
const originalError = console.error;
const originalWarn = console.warn;

function timestamp() {
  return new Date().toISOString().replace('T', ' ').slice(0, 19);
}

function addEntry(level, args) {
  const msg = args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');
  entries.push({ time: timestamp(), level, msg });
  if (entries.length > MAX_ENTRIES) entries.splice(0, entries.length - MAX_ENTRIES);
}

console.log = (...args) => {
  addEntry('INFO', args);
  originalLog.apply(console, args);
};

console.error = (...args) => {
  addEntry('ERROR', args);
  originalError.apply(console, args);
};

console.warn = (...args) => {
  addEntry('WARN', args);
  originalWarn.apply(console, args);
};

function getEntries() {
  return [...entries].reverse();
}

function clear() {
  entries.length = 0;
}

module.exports = { getEntries, clear };
