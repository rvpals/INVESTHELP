export async function render(container) {
  container.innerHTML = `<div class="screen">
    <h2 class="mb-16">Help</h2>
    <div class="card p-16">
      <h3 class="mb-8">Getting Started</h3>
      <p class="text-sm mb-16">InvestHelp helps you track your investments. Add positions, record transactions, and monitor your portfolio.</p>

      <h3 class="mb-8">Navigation</h3>
      <ul class="text-sm" style="padding-left:20px;margin-bottom:16px">
        <li><strong>Dashboard</strong> — Portfolio overview, market indices, daily gainers/losers</li>
        <li><strong>Positions</strong> — View all your holdings (STOCK/ETF/Analysis/Dividend tabs), add/edit positions</li>
        <li><strong>Transactions</strong> — Record buy/sell transactions</li>
        <li><strong>Simulation</strong> — Run "what if" scenarios on historical data</li>
        <li><strong>Performance</strong> — Track account values over time with charts</li>
        <li><strong>Watch Lists</strong> — Create watch lists to monitor tickers you're interested in</li>
        <li><strong>SQL Explorer</strong> — Run raw SQL queries against the database</li>
      </ul>

      <h3 class="mb-8">Dividend Tab (Positions)</h3>
      <ul class="text-sm" style="padding-left:20px;margin-bottom:16px">
        <li><strong>Total Annual Dividend Income</strong> — summary card showing combined Stock + ETF income</li>
        <li>Separate Stock and ETF sections with exploding pie charts (largest slice offset)</li>
        <li>Sortable tables: Annual Dividend, Div/Share, Ticker, Shares</li>
        <li>Only dividend-paying tickers shown. Click a row to view item detail</li>
        <li>Dividend rates fetched during Refresh All</li>
      </ul>

      <h3 class="mb-8">Key Features</h3>
      <ul class="text-sm" style="padding-left:20px;margin-bottom:16px">
        <li><strong>Portfolio Button</strong> — Tap the value in the top bar to refresh all prices</li>
        <li><strong>Item Detail</strong> — Tap any ticker to see details, price history, analysis info, and news</li>
        <li><strong>Charts</strong> — Click on chart points to see values. Charts support interactive selection.</li>
        <li><strong>Backup/Restore</strong> — Export your data as JSON from Settings > Data Management</li>
        <li><strong>CSV Import</strong> — Import positions/transactions from brokerage CSV exports</li>
        <li><strong>Auto Refresh</strong> — Enable in Settings to automatically refresh prices on a schedule</li>
      </ul>

      <h3 class="mb-8">Themes</h3>
      <p class="text-sm mb-16">Change your color theme in Settings > Preferences. Supports light and dark mode.</p>

      <h3 class="mb-8">Data</h3>
      <p class="text-sm">All data is stored on the server. You can export backups and restore them at any time. The app uses Yahoo Finance for live prices and market data.</p>
    </div>
  </div>`;
}
