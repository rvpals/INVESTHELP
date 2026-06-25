const appLog = require('./services/app-log'); // must be first to capture all logs
const express = require('express');
const session = require('express-session');
const path = require('path');
const requireAuth = require('./middleware/require-auth');

const SESSION_SECRET = process.env.SESSION_SECRET;
if (!SESSION_SECRET) {
  console.warn(
    '[WARN] SESSION_SECRET env var not set — using insecure default. ' +
    'Set SESSION_SECRET in your environment before exposing this server.'
  );
}

const app = express();
app.use(express.json({ limit: '50mb' }));

// Session middleware — must come before any route that reads req.session
app.use(session({
  secret: SESSION_SECRET || 'investhelp-default-secret-change-me',
  name: 'investhelp_sid',
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    sameSite: 'lax',
    maxAge: 7 * 24 * 60 * 60 * 1000, // 7 days
  },
}));

// Static files served without auth (needed so login page CSS/JS loads)
app.use(express.static(path.join(__dirname, '..', 'public')));

// Auth routes — no guard, these ARE the auth endpoints
app.use('/api/auth', require('./routes/auth'));

// All remaining API routes require a valid session
app.use('/api', requireAuth);

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

// Server log (protected)
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
