const db = require('../db');
const fs = require('fs');
const path = require('path');

function generateSnapshot() {
  try {
    const positions = db.prepare('SELECT ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow, dividendRate FROM investment_positions ORDER BY value DESC').all();
    const accounts = db.prepare('SELECT * FROM investment_accounts').all();
    const totalValue = positions.reduce((s, p) => s + p.value, 0);
    const totalDayGL = positions.reduce((s, p) => s + p.dayGainLoss, 0);
    const etfValue = positions.filter(p => p.type === 'ETF').reduce((s, p) => s + p.value, 0);
    const stockValue = positions.filter(p => p.type === 'Stock').reduce((s, p) => s + p.value, 0);
    const totalDividend = positions.reduce((s, p) => s + (p.dividendRate || 0) * (p.quantity || 0), 0);
    const dayPct = totalValue > 0 ? (totalDayGL / (totalValue - totalDayGL)) * 100 : 0;
    const now = new Date();
    const timestamp = now.toLocaleString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit', second: '2-digit' });

    const fmt = (v) => '$' + Math.abs(v).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    const fmtSigned = (v) => (v >= 0 ? '+' : '-') + fmt(v);
    const fmtPct = (v) => (v >= 0 ? '+' : '') + v.toFixed(2) + '%';
    const glColor = (v) => v >= 0 ? '#2E7D32' : '#C62828';

    const posRows = positions.map((p, i) => {
      const gl = p.dayGainLoss;
      const glPerShare = p.quantity > 0 ? gl / p.quantity : 0;
      const divIncome = (p.dividendRate || 0) * (p.quantity || 0);
      return `
        <tr style="background:${i % 2 ? '#f8f8f8' : '#fff'}">
          <td style="padding:8px;font-weight:bold">${p.ticker}</td>
          <td style="padding:8px">${p.name || ''}</td>
          <td style="padding:8px">${p.type}</td>
          <td style="padding:8px;text-align:right">${fmt(p.currentPrice)}</td>
          <td style="padding:8px;text-align:right">${p.quantity.toFixed(4)}</td>
          <td style="padding:8px;text-align:right;font-weight:bold">${fmt(p.value)}</td>
          <td style="padding:8px;text-align:right;color:${glColor(gl)}">${fmtSigned(gl)}</td>
          <td style="padding:8px;text-align:right;color:${glColor(glPerShare)}">${fmtSigned(glPerShare)}</td>
          <td style="padding:8px;text-align:right">${fmt(p.dayHigh)}</td>
          <td style="padding:8px;text-align:right">${fmt(p.dayLow)}</td>
          <td style="padding:8px;text-align:right;color:#1565C0">${divIncome > 0 ? fmt(divIncome) + '/yr' : ''}</td>
        </tr>`;
    }).join('');

    const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>InvestHelp Portfolio Snapshot</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; color: #1C1B1F; padding: 16px; }
    .container { max-width: 1200px; margin: 0 auto; }
    h1 { font-size: 24px; margin-bottom: 4px; }
    .timestamp { color: #666; font-size: 13px; margin-bottom: 16px; }
    .summary { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
    .card { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); text-align: center; flex: 1; min-width: 140px; }
    .card-label { font-size: 12px; color: #666; margin-bottom: 4px; }
    .card-value { font-size: 20px; font-weight: bold; }
    .card-sub { font-size: 13px; margin-top: 2px; }
    table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    th { background: #f0f0f0; padding: 10px 8px; text-align: left; font-size: 12px; font-weight: 600; color: #555; border-bottom: 2px solid #ddd; }
    th:nth-child(n+4) { text-align: right; }
    td { border-bottom: 1px solid #eee; font-size: 13px; }
    .footer { text-align: center; margin-top: 16px; font-size: 11px; color: #999; }
    @media (prefers-color-scheme: dark) {
      body { background: #1C1B1F; color: #E6E1E5; }
      .card { background: #2B2930; }
      .card-label { color: #aaa; }
      table { background: #2B2930; }
      th { background: #363240; color: #ccc; border-bottom-color: #444; }
      td { border-bottom-color: #3a3a3a; }
      tr:nth-child(even) { background: #322F37 !important; }
      tr:nth-child(odd) { background: #2B2930 !important; }
      .footer { color: #666; }
      .timestamp { color: #999; }
    }
  </style>
</head>
<body>
  <div class="container">
    <h1>InvestHelp Portfolio Snapshot</h1>
    <div class="timestamp">Generated: ${timestamp}</div>

    <div class="summary">
      <div class="card">
        <div class="card-label">Total Portfolio</div>
        <div class="card-value">${fmt(totalValue)}</div>
        <div class="card-sub" style="color:${glColor(totalDayGL)}">${fmtSigned(totalDayGL)} (${fmtPct(dayPct)})</div>
      </div>
      <div class="card">
        <div class="card-label">Stock Value</div>
        <div class="card-value">${fmt(stockValue)}</div>
      </div>
      <div class="card">
        <div class="card-label">ETF Value</div>
        <div class="card-value">${fmt(etfValue)}</div>
      </div>
      <div class="card">
        <div class="card-label">Daily Change</div>
        <div class="card-value" style="color:${glColor(totalDayGL)}">${fmtSigned(totalDayGL)}</div>
        <div class="card-sub" style="color:${glColor(dayPct)}">${fmtPct(dayPct)}</div>
      </div>
      ${totalDividend > 0 ? `<div class="card">
        <div class="card-label">Annual Dividends</div>
        <div class="card-value" style="color:#1565C0">${fmt(totalDividend)}</div>
      </div>` : ''}
    </div>

    <table>
      <thead>
        <tr>
          <th>Ticker</th><th>Name</th><th>Type</th><th>Price</th><th>Shares</th>
          <th>Value</th><th>Day G/L</th><th>G/L/Share</th><th>High</th><th>Low</th><th>Dividend</th>
        </tr>
      </thead>
      <tbody>${posRows}</tbody>
    </table>

    <div class="footer">
      ${positions.length} positions &middot; ${accounts.length} accounts &middot; Snapshot is read-only &middot; Refresh prices in the app to update
    </div>
  </div>
</body>
</html>`;

    const outPath = path.join(__dirname, '..', '..', 'public', 'snapshot.html');
    fs.writeFileSync(outPath, html);
    console.log(`Snapshot generated: ${timestamp} (${positions.length} positions)`);
  } catch (err) {
    console.error('Snapshot generation failed:', err.message);
  }
}

module.exports = { generateSnapshot };
