function parseCsvLine(line) {
  const fields = [];
  let current = '';
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    if (inQuotes) {
      if (ch === '"' && line[i + 1] === '"') {
        current += '"';
        i++;
      } else if (ch === '"') {
        inQuotes = false;
      } else {
        current += ch;
      }
    } else {
      if (ch === '"') {
        inQuotes = true;
      } else if (ch === ',') {
        fields.push(current.trim());
        current = '';
      } else {
        current += ch;
      }
    }
  }
  fields.push(current.trim());
  return fields;
}

function parseNumeric(str) {
  if (str == null || str === '') return 0;
  return parseFloat(String(str).replace(/[$,]/g, '')) || 0;
}

function parseCsv(text) {
  const lines = text.split(/\r?\n/).filter(l => l.trim().length > 0);
  if (lines.length === 0) return { headers: [], rows: [] };
  const headers = parseCsvLine(lines[0]);
  const rows = [];
  for (let i = 1; i < lines.length; i++) {
    const fields = parseCsvLine(lines[i]);
    if (fields.length >= headers.length / 2) {
      rows.push(fields);
    }
  }
  return { headers, rows };
}

const ALIASES = {
  'Symbol': 'ticker', 'Ticker': 'ticker', 'SYMBOL': 'ticker',
  'Description': 'name', 'Name': 'name', 'Company': 'name', 'Security': 'name',
  'Price': 'currentPrice', 'Last Price': 'currentPrice', 'Current Price': 'currentPrice', 'Last': 'currentPrice',
  'Shares': 'quantity', 'Quantity': 'quantity', 'Qty': 'quantity', 'Units': 'quantity',
  'Type': 'type', 'Asset Type': 'type', 'Security Type': 'type',
  'Date': 'date', 'Trade Date': 'date', 'Transaction Date': 'date',
  'Action': 'action', 'Transaction Type': 'action', 'Trans Type': 'action',
  'Amount': 'totalAmount', 'Total': 'totalAmount', 'Total Amount': 'totalAmount',
  'Note': 'note', 'Notes': 'note', 'Memo': 'note',
  'Account': 'accountName', 'Account Name': 'accountName', 'account': 'accountName', 'account name': 'accountName',
  'Total Value': 'totalValue', 'Value': 'totalValue', 'Market Value': 'totalValue',
};

function autoMapHeaders(headers) {
  const mapping = {};
  headers.forEach((h, i) => {
    const normalized = h.trim();
    if (ALIASES[normalized]) {
      mapping[i] = ALIASES[normalized];
    }
  });
  return mapping;
}

function escapeCsv(val) {
  const s = String(val ?? '');
  if (s.includes(',') || s.includes('"') || s.includes('\n')) {
    return '"' + s.replace(/"/g, '""') + '"';
  }
  return s;
}

module.exports = { parseCsvLine, parseNumeric, parseCsv, autoMapHeaders, escapeCsv };
