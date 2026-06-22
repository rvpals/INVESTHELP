const appLog = require('./services/app-log'); // must be first to capture all logs
const express = require('express');
const path = require('path');

const app = express();
app.use(express.json({ limit: '50mb' }));
app.use(express.static(path.join(__dirname, '..', 'public')));

// API routes
app.use('/api/accounts', require('./routes/accounts'));
app.use('/api/positions', require('./routes/positions'));
app.use('/api/transactions', require('./routes/transactions'));
app.use('/api/performance', require('./routes/performance'));
app.use('/api/watchlists', require('./routes/watchlist'));
app.use('/api/change-history', require('./routes/change-history'));
app.use('/api/definitions', require('./routes/definitions'));
app.use('/api/csv-mappings', require('./routes/csv-mappings'));
app.use('/api/sql-library', require('./routes/sql-library'));
app.use('/api/ai-library', require('./routes/ai-library'));
app.use('/api/yahoo', require('./routes/yahoo'));
app.use('/api/refresh', require('./routes/refresh'));
app.use('/api/backup', require('./routes/backup'));
app.use('/api/csv-import', require('./routes/csv-import'));
app.use('/api/sql', require('./routes/sql-explorer'));
app.use('/api/settings', require('./routes/settings'));
app.use('/api/volatility', require('./routes/volatility'));
app.use('/api/correlation', require('./routes/correlation'));

// Version info
// Server log
app.get('/api/server-log', (req, res) => { res.json(appLog.getEntries()); });
app.delete('/api/server-log', (req, res) => { appLog.clear(); res.json({ ok: true }); });

// SPA fallback
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, '..', 'public', 'index.html'));
});

// Start auto-refresh if enabled
const autoRefresh = require('./services/auto-refresh');
autoRefresh.initFromSettings();

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`InvestHelp PWA running on http://localhost:${PORT}`);
});
