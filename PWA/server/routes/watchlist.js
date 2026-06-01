const express = require('express');
const router = express.Router();
const db = require('../db');

router.get('/', (req, res) => {
  const lists = db.prepare('SELECT * FROM watch_lists ORDER BY name').all();
  res.json(lists);
});

router.post('/', (req, res) => {
  const { name } = req.body;
  const r = db.prepare('INSERT INTO watch_lists (name) VALUES (?)').run(name);
  res.json({ id: r.lastInsertRowid });
});

router.put('/:id', (req, res) => {
  const { name } = req.body;
  db.prepare('UPDATE watch_lists SET name = ? WHERE id = ?').run(name, req.params.id);
  res.json({ ok: true });
});

router.delete('/:id', (req, res) => {
  db.prepare('DELETE FROM watch_lists WHERE id = ?').run(req.params.id);
  res.json({ ok: true });
});

router.get('/:id/items', (req, res) => {
  res.json(db.prepare('SELECT * FROM watch_list_items WHERE watchListId = ? ORDER BY addedDate DESC').all(req.params.id));
});

router.post('/:id/items', (req, res) => {
  const { ticker, shares, priceWhenAdded, addedDate, reminderDateTime, reminderMessage } = req.body;
  const r = db.prepare('INSERT INTO watch_list_items (watchListId, ticker, shares, priceWhenAdded, addedDate, reminderDateTime, reminderMessage) VALUES (?, ?, ?, ?, ?, ?, ?)')
    .run(req.params.id, ticker, shares || 0, priceWhenAdded || 0, addedDate || Math.floor(Date.now() / 86400000), reminderDateTime ?? null, reminderMessage || '');
  res.json({ id: r.lastInsertRowid });
});

router.put('/items/:itemId', (req, res) => {
  const { shares, priceWhenAdded, reminderDateTime, reminderMessage } = req.body;
  db.prepare('UPDATE watch_list_items SET shares=?, priceWhenAdded=?, reminderDateTime=?, reminderMessage=? WHERE id=?')
    .run(shares || 0, priceWhenAdded || 0, reminderDateTime ?? null, reminderMessage || '', req.params.itemId);
  res.json({ ok: true });
});

router.delete('/items/:itemId', (req, res) => {
  db.prepare('DELETE FROM watch_list_items WHERE id = ?').run(req.params.itemId);
  res.json({ ok: true });
});

module.exports = router;
