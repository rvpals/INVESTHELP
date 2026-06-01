const express = require('express');
const router = express.Router();
const db = require('../db');
const multer = require('multer');
const { parseCsv, parseNumeric, autoMapHeaders } = require('../services/csv-parser');

const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 10 * 1024 * 1024 } });

router.post('/preview', upload.single('file'), (req, res) => {
  try {
    const text = req.file.buffer.toString('utf8');
    const { headers, rows } = parseCsv(text);
    const autoMapping = autoMapHeaders(headers);
    res.json({ headers, preview: rows.slice(0, 3), totalRows: rows.length, autoMapping });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.post('/execute', upload.single('file'), (req, res) => {
  try {
    const text = req.file.buffer.toString('utf8');
    const { importType, mapping } = req.body;
    const columnMapping = JSON.parse(mapping || '{}');
    const { headers, rows } = parseCsv(text);
    const results = [];

    if (importType === 'Position') {
      const upsert = db.prepare(`
        INSERT INTO investment_positions (ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(ticker) DO UPDATE SET
          name=CASE WHEN excluded.name != '' THEN excluded.name ELSE investment_positions.name END,
          type=CASE WHEN excluded.type != 'Stock' THEN excluded.type ELSE investment_positions.type END,
          currentPrice=CASE WHEN excluded.currentPrice > 0 THEN excluded.currentPrice ELSE investment_positions.currentPrice END,
          quantity=CASE WHEN excluded.quantity > 0 THEN excluded.quantity ELSE investment_positions.quantity END
      `);
      for (const row of rows) {
        const rec = mapRow(row, columnMapping);
        if (!rec.ticker) continue;
        const ticker = rec.ticker.split(' - ')[0].trim().toUpperCase();
        upsert.run(ticker, rec.name || '', rec.type || 'Stock', parseNumeric(rec.currentPrice), parseNumeric(rec.quantity), 0, 0, 0, 0);
        results.push({ ticker, status: 'OK' });
      }
    } else if (importType === 'Transaction') {
      const ins = db.prepare('INSERT OR IGNORE INTO investment_transactions (date, time, action, ticker, numberOfShares, pricePerShare, totalAmount, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
      for (const row of rows) {
        const rec = mapRow(row, columnMapping);
        if (!rec.ticker) continue;
        const ticker = rec.ticker.split(' - ')[0].trim().toUpperCase();
        const date = rec.date ? parseDateToEpochDays(rec.date) : Math.floor(Date.now() / 86400000);
        ins.run(date, null, rec.action || 'Buy', ticker, parseNumeric(rec.numberOfShares), parseNumeric(rec.pricePerShare), parseNumeric(rec.totalAmount), rec.note || '');
        results.push({ ticker, status: 'OK' });
      }
    } else if (importType === 'Performance') {
      const ins = db.prepare('INSERT OR IGNORE INTO account_performance (accountId, totalValue, date, note) VALUES (?, ?, ?, ?)');
      for (const row of rows) {
        const rec = mapRow(row, columnMapping);
        const date = rec.date ? parseDateToEpochDays(rec.date) : Math.floor(Date.now() / 86400000);
        ins.run(parseNumeric(rec.accountId) || 1, parseNumeric(rec.totalValue), date, rec.note || '');
        results.push({ status: 'OK' });
      }
    }

    res.json({ ok: true, imported: results.length, results });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

function mapRow(row, mapping) {
  const rec = {};
  for (const [colIdx, fieldName] of Object.entries(mapping)) {
    const idx = parseInt(colIdx);
    if (idx >= 0 && idx < row.length) {
      rec[fieldName] = row[idx];
    }
  }
  return rec;
}

function parseDateToEpochDays(dateStr) {
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return Math.floor(Date.now() / 86400000);
  return Math.floor(d.getTime() / 86400000);
}

module.exports = router;
