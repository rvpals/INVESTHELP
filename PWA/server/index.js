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

// Version info
const { execSync } = require('child_process');
app.get('/api/version', (req, res) => {
  try {
    const rootDir = path.join(__dirname, '..', '..');
    const commitDate = execSync('git log -1 --format=%ci', { cwd: rootDir }).toString().trim();
    const commitHash = execSync('git log -1 --format=%h', { cwd: rootDir }).toString().trim();
    const commitMsg = execSync('git log -1 --format=%s', { cwd: rootDir }).toString().trim();
    res.json({ commitDate, commitHash, commitMsg });
  } catch {
    res.json({ commitDate: 'unknown', commitHash: '', commitMsg: '' });
  }
});

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
