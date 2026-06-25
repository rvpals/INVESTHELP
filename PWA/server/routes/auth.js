const express = require('express');
const bcrypt = require('bcryptjs');
const db = require('../db');
const requireAuth = require('../middleware/require-auth');

const router = express.Router();
const BCRYPT_ROUNDS = 10;

// ---------------------------------------------------------------------------
// Session routes (no auth guard — these are the auth endpoints themselves)
// ---------------------------------------------------------------------------

/** Check if the current session is valid. Returns the logged-in user. */
router.get('/me', (req, res) => {
  if (!req.session || !req.session.userId) {
    return res.status(401).json({ error: 'Not authenticated' });
  }
  const user = db.prepare('SELECT id, username, created_at FROM users WHERE id = ?')
    .get(req.session.userId);
  if (!user) {
    req.session.destroy(() => {});
    return res.status(401).json({ error: 'Not authenticated' });
  }
  res.json({ id: user.id, username: user.username, createdAt: user.created_at });
});

/** Log in with username + password. Sets a session cookie on success. */
router.post('/login', (req, res) => {
  const { username, password } = req.body || {};
  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password are required' });
  }

  const user = db.prepare('SELECT * FROM users WHERE username = ?').get(username.trim());
  if (!user) {
    return res.status(401).json({ error: 'Invalid username or password' });
  }

  const match = bcrypt.compareSync(password, user.password_hash);
  if (!match) {
    return res.status(401).json({ error: 'Invalid username or password' });
  }

  req.session.userId = user.id;
  req.session.username = user.username;
  res.json({ id: user.id, username: user.username });
});

/** Log out — destroy the server-side session. */
router.post('/logout', (req, res) => {
  req.session.destroy(() => {
    res.clearCookie('investhelp_sid');
    res.json({ ok: true });
  });
});

// ---------------------------------------------------------------------------
// Setup check — public endpoint so the login page can detect first-run state
// ---------------------------------------------------------------------------

/** Returns { setupNeeded: true } when no users exist yet. */
router.get('/setup-needed', (req, res) => {
  const count = db.prepare('SELECT COUNT(*) AS n FROM users').get().n;
  res.json({ setupNeeded: count === 0 });
});

// ---------------------------------------------------------------------------
// User management routes (require auth + at least one user must always remain)
// ---------------------------------------------------------------------------

/** List all users (id, username, created_at — no password hash). */
router.get('/users', requireAuth, (req, res) => {
  const users = db.prepare(
    'SELECT id, username, created_at FROM users ORDER BY created_at ASC'
  ).all();
  res.json(users);
});

/** Create a new user. Unauthenticated only allowed when no users exist (bootstrap). */
router.post('/users', (req, res, next) => {
  const count = db.prepare('SELECT COUNT(*) AS n FROM users').get().n;
  if (count === 0) return next(); // first user — no auth required
  requireAuth(req, res, next);
}, (req, res) => {
  const { username, password } = req.body || {};
  if (!username || !password) {
    return res.status(400).json({ error: 'Username and password are required' });
  }
  if (username.trim().length < 2) {
    return res.status(400).json({ error: 'Username must be at least 2 characters' });
  }
  if (password.length < 4) {
    return res.status(400).json({ error: 'Password must be at least 4 characters' });
  }

  const existing = db.prepare('SELECT id FROM users WHERE username = ?')
    .get(username.trim());
  if (existing) {
    return res.status(409).json({ error: 'Username already exists' });
  }

  const hash = bcrypt.hashSync(password, BCRYPT_ROUNDS);
  const now = Math.floor(Date.now() / 1000);
  const result = db.prepare(
    'INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)'
  ).run(username.trim(), hash, now);

  res.status(201).json({ id: result.lastInsertRowid, username: username.trim(), createdAt: now });
});

/** Delete a user. A user cannot delete their own account. */
router.delete('/users/:id', requireAuth, (req, res) => {
  const targetId = parseInt(req.params.id, 10);
  if (targetId === req.session.userId) {
    return res.status(400).json({ error: 'You cannot delete your own account' });
  }

  const count = db.prepare('SELECT COUNT(*) AS n FROM users').get().n;
  if (count <= 1) {
    return res.status(400).json({ error: 'Cannot delete the last user account' });
  }

  db.prepare('DELETE FROM users WHERE id = ?').run(targetId);
  res.json({ ok: true });
});

/** Change password for a user (self or any user if you are logged in). */
router.put('/users/:id/password', requireAuth, (req, res) => {
  const targetId = parseInt(req.params.id, 10);
  const { password } = req.body || {};
  if (!password || password.length < 4) {
    return res.status(400).json({ error: 'Password must be at least 4 characters' });
  }

  const user = db.prepare('SELECT id FROM users WHERE id = ?').get(targetId);
  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }

  const hash = bcrypt.hashSync(password, BCRYPT_ROUNDS);
  db.prepare('UPDATE users SET password_hash = ? WHERE id = ?').run(hash, targetId);
  res.json({ ok: true });
});

module.exports = router;
