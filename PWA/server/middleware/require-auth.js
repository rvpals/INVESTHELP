/**
 * Middleware: reject requests that don't carry a valid session.
 * Mount AFTER express-session so req.session is already populated.
 */
function requireAuth(req, res, next) {
  if (req.session && req.session.userId) {
    return next();
  }
  return res.status(401).json({ error: 'Not authenticated' });
}

module.exports = requireAuth;
