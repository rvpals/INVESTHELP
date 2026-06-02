const Database = require('better-sqlite3');
const path = require('path');

const dbPath = process.env.INVESTHELP_DB || path.join(__dirname, 'investhelp.db');
const db = new Database(dbPath);

db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

db.exec(`
  CREATE TABLE IF NOT EXISTS investment_accounts (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL,
    description   TEXT    NOT NULL DEFAULT '',
    initialValue  REAL    NOT NULL DEFAULT 0,
    lastUpdatedOn INTEGER,
    lastValue     REAL
  );

  CREATE TABLE IF NOT EXISTS investment_positions (
    ticker       TEXT PRIMARY KEY,
    name         TEXT    NOT NULL DEFAULT '',
    type         TEXT    NOT NULL DEFAULT 'Stock',
    currentPrice REAL    NOT NULL DEFAULT 0,
    quantity     REAL    NOT NULL DEFAULT 0,
    dayGainLoss  REAL    NOT NULL DEFAULT 0,
    value        REAL    NOT NULL DEFAULT 0,
    dayHigh      REAL    NOT NULL DEFAULT 0,
    dayLow       REAL    NOT NULL DEFAULT 0,
    logo         BLOB
  );

  CREATE TABLE IF NOT EXISTS investment_transactions (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    date           INTEGER NOT NULL,
    time           INTEGER,
    action         TEXT    NOT NULL DEFAULT 'Buy',
    ticker         TEXT    NOT NULL,
    numberOfShares REAL    NOT NULL DEFAULT 0,
    pricePerShare  REAL    NOT NULL DEFAULT 0,
    totalAmount    REAL    NOT NULL DEFAULT 0,
    note           TEXT    NOT NULL DEFAULT ''
  );
  CREATE UNIQUE INDEX IF NOT EXISTS idx_tx_unique
    ON investment_transactions (date, action, ticker, totalAmount);
  CREATE INDEX IF NOT EXISTS idx_tx_ticker
    ON investment_transactions (ticker);

  CREATE TABLE IF NOT EXISTS account_performance (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    accountId  INTEGER NOT NULL REFERENCES investment_accounts(id) ON DELETE CASCADE,
    totalValue REAL    NOT NULL DEFAULT 0,
    date       INTEGER NOT NULL,
    note       TEXT    NOT NULL DEFAULT ''
  );
  CREATE UNIQUE INDEX IF NOT EXISTS idx_perf_acct_date
    ON account_performance (accountId, date);
  CREATE INDEX IF NOT EXISTS idx_perf_acctId
    ON account_performance (accountId);

  CREATE TABLE IF NOT EXISTS watch_lists (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL
  );

  CREATE TABLE IF NOT EXISTS watch_list_items (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    watchListId      INTEGER NOT NULL REFERENCES watch_lists(id) ON DELETE CASCADE,
    ticker           TEXT    NOT NULL,
    shares           REAL    NOT NULL DEFAULT 0,
    priceWhenAdded   REAL    NOT NULL DEFAULT 0,
    addedDate        INTEGER NOT NULL,
    reminderDateTime INTEGER,
    reminderMessage  TEXT    NOT NULL DEFAULT ''
  );
  CREATE INDEX IF NOT EXISTS idx_wli_wlid
    ON watch_list_items (watchListId);

  CREATE TABLE IF NOT EXISTS change_history (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    date              INTEGER NOT NULL,
    etfValue          REAL    NOT NULL DEFAULT 0,
    stockValue        REAL    NOT NULL DEFAULT 0,
    totalValue        REAL    NOT NULL DEFAULT 0,
    dailyChangeEtf    REAL    NOT NULL DEFAULT 0,
    dailyChangeStock  REAL    NOT NULL DEFAULT 0,
    dailyChangeTotal  REAL    NOT NULL DEFAULT 0
  );
  CREATE UNIQUE INDEX IF NOT EXISTS idx_ch_date
    ON change_history (date);

  CREATE TABLE IF NOT EXISTS definitions (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL,
    description TEXT    NOT NULL DEFAULT ''
  );

  CREATE TABLE IF NOT EXISTS csv_import_mappings (
    importType     TEXT PRIMARY KEY,
    mappingsJson   TEXT NOT NULL DEFAULT '{}',
    dateFormatJson TEXT NOT NULL DEFAULT '{}'
  );

  CREATE TABLE IF NOT EXISTS csv_named_mappings (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    name           TEXT    NOT NULL,
    importType     TEXT    NOT NULL,
    mappingsJson   TEXT    NOT NULL DEFAULT '{}',
    dateFormatJson TEXT    NOT NULL DEFAULT '{}'
  );

  CREATE TABLE IF NOT EXISTS sql_library (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    category    TEXT NOT NULL DEFAULT '',
    sql         TEXT NOT NULL DEFAULT ''
  );

  CREATE TABLE IF NOT EXISTS ai_library (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    promptText  TEXT NOT NULL DEFAULT ''
  );

  CREATE TABLE IF NOT EXISTS settings (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL DEFAULT ''
  );
`);

// Seed definitions
const defCount = db.prepare('SELECT COUNT(*) as n FROM definitions').get().n;
if (defCount === 0) {
  const ins = db.prepare('INSERT INTO definitions (name, description) VALUES (?, ?)');
  const tx = db.transaction(() => {
    ins.run('Market Cap', "Total market value of a company's outstanding shares");
    ins.run('Trailing P/E', 'Price-to-earnings ratio based on the last 12 months of actual earnings');
    ins.run('Forward P/E', 'Price-to-earnings ratio based on projected future earnings');
    ins.run('EPS', 'Earnings per share — net income divided by outstanding shares');
    ins.run('Dividend Yield', 'Annual dividend payment as a percentage of the stock price');
    ins.run('52-Week Range', 'The lowest and highest prices at which a stock has traded in the past year');
    ins.run('Profit Margins', 'Percentage of revenue that becomes profit after all expenses');
    ins.run('Return on Equity', 'Net income as a percentage of shareholder equity — measures profitability');
  });
  tx();
}

// Seed AI library
const aiCount = db.prepare('SELECT COUNT(*) as n FROM ai_library').get().n;
if (aiCount === 0) {
  const ins = db.prepare('INSERT INTO ai_library (name, description, promptText) VALUES (?, ?, ?)');
  const tx = db.transaction(() => {
    ins.run('Forensic Ticker Deep-Dive', 'Comprehensive fundamental analysis', 'Analyze {TICKER} with a forensic deep-dive covering financials, valuation, competitive position, risks, and catalysts.');
    ins.run('10-K/10-Q Earnings Summarizer', 'Summarize recent earnings filings', 'Summarize the latest 10-K or 10-Q filing for {TICKER}, highlighting revenue trends, margin changes, guidance, and risk factors.');
    ins.run('ETF Under the Hood', 'Analyze ETF holdings and strategy', 'Break down the ETF {TICKER}: top holdings, sector allocation, expense ratio, tracking error, and how it compares to alternatives.');
  });
  tx();
}

// Seed default settings
const settingsCount = db.prepare('SELECT COUNT(*) as n FROM settings').get().n;
if (settingsCount === 0) {
  const ins = db.prepare('INSERT OR IGNORE INTO settings (key, value) VALUES (?, ?)');
  const tx = db.transaction(() => {
    ins.run('auto_update_change_history', 'false');
    ins.run('auto_refresh_enabled', 'false');
    ins.run('auto_refresh_interval', '30m');
    ins.run('auto_backup_on_refresh', 'false');
    ins.run('auto_backup_keep_count', '10');
    ins.run('news_article_count', '5');
    ins.run('ai_enabled', 'false');
    ins.run('ai_api_key', '');
    ins.run('trailing_stop_pct', '10');
    ins.run('profit_target_pct', '20');
    ins.run('stock_concentration_cap', '10');
    ins.run('etf_concentration_cap', '25');
  });
  tx();
}

module.exports = db;
