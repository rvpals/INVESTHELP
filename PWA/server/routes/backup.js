const express = require('express');
const router = express.Router();
const db = require('../db');
const fs = require('fs');
const path = require('path');
const multer = require('multer');
const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 50 * 1024 * 1024 } });

const BACKUP_DIR = path.join(__dirname, '..', '..', 'backups');

const EXCLUDED_TABLES = new Set(['sqlite_sequence', 'sqlite_stat1']);

function discoverTables() {
  return db.prepare("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").all()
    .map(r => r.name)
    .filter(n => !EXCLUDED_TABLES.has(n) && !n.startsWith('sqlite_'));
}

function getTableColumns(tableName) {
  return db.prepare(`PRAGMA table_info(${tableName})`).all()
    .map(c => ({ name: c.name, type: (c.type || 'TEXT').toUpperCase() }));
}

function getForeignKeyParents(tableName) {
  return [...new Set(db.prepare(`PRAGMA foreign_key_list(${tableName})`).all().map(fk => fk.table))];
}

function topologicalSort(tables) {
  const deps = {};
  for (const t of tables) deps[t] = getForeignKeyParents(t).filter(p => tables.includes(p));
  const sorted = [], visited = new Set(), visiting = new Set();
  function visit(t) {
    if (visited.has(t)) return;
    if (visiting.has(t)) { sorted.push(t); visited.add(t); return; }
    visiting.add(t);
    for (const p of (deps[t] || [])) visit(p);
    visiting.delete(t);
    visited.add(t);
    sorted.push(t);
  }
  for (const t of tables) visit(t);
  return sorted;
}

function exportAllTablesGeneric() {
  const tables = discoverTables();
  const tablesData = {};
  for (const table of tables) {
    const cols = getTableColumns(table);
    const rows = db.prepare(`SELECT * FROM ${table}`).all();
    tablesData[table] = rows.map(row => {
      const obj = {};
      for (const col of cols) {
        const val = row[col.name];
        if (val === null || val === undefined) {
          obj[col.name] = null;
        } else if (col.type.includes('BLOB') && Buffer.isBuffer(val)) {
          obj[col.name] = val.toString('base64');
        } else {
          obj[col.name] = val;
        }
      }
      return obj;
    });
  }
  return { version: 6, tables: tablesData };
}

router.get('/export', (req, res) => {
  const data = exportAllTablesGeneric();
  const filename = `invest_help_backup_${new Date().toISOString().slice(0, 10)}.json`;
  res.set('Content-Disposition', `attachment; filename="${filename}"`);
  res.set('Content-Type', 'application/json');
  res.send(JSON.stringify(data, null, 2));
});

function restoreGeneric(data) {
  const tablesData = data.tables;
  if (!tablesData) throw new Error("Invalid v6 backup: missing 'tables' key");

  const existingTables = discoverTables();
  const sortedForInsert = topologicalSort(existingTables);
  const sortedForDelete = [...sortedForInsert].reverse();

  const tx = db.transaction(() => {
    for (const table of sortedForDelete) {
      db.prepare(`DELETE FROM ${table}`).run();
    }

    for (const table of sortedForInsert) {
      const rows = tablesData[table];
      if (!rows || !rows.length) continue;
      const cols = getTableColumns(table);
      const colNames = cols.map(c => c.name);
      const colTypes = {};
      for (const c of cols) colTypes[c.name] = c.type;
      const placeholders = colNames.map(() => '?').join(',');
      const stmt = db.prepare(`INSERT OR REPLACE INTO ${table} (${colNames.join(',')}) VALUES (${placeholders})`);

      for (const row of rows) {
        const values = colNames.map(cn => {
          const val = row[cn];
          if (val === null || val === undefined) return null;
          if (colTypes[cn].includes('BLOB') && typeof val === 'string') {
            return Buffer.from(val, 'base64');
          }
          return val;
        });
        stmt.run(...values);
      }
    }
  });
  tx();

  const counts = {};
  for (const [table, rows] of Object.entries(tablesData)) {
    counts[table] = (rows || []).length;
  }
  return counts;
}

function restoreLegacy(data) {
  const existingTables = discoverTables();
  const sortedForDelete = [...topologicalSort(existingTables)].reverse();

  const tx = db.transaction(() => {
    for (const table of sortedForDelete) {
      db.prepare(`DELETE FROM ${table}`).run();
    }

    const insAcct = db.prepare('INSERT INTO investment_accounts (id, name, description, initialValue) VALUES (?, ?, ?, ?)');
    for (const a of (data.accounts || [])) {
      insAcct.run(a.id, a.name, a.description || '', a.initialValue || 0);
    }

    const insPos = db.prepare('INSERT INTO investment_positions (ticker, name, type, currentPrice, quantity, dayGainLoss, value, dayHigh, dayLow, dividendRate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)');
    for (const p of (data.positions || data.items || [])) {
      const ticker = (p.ticker || '').trim() || 'UNKNOWN';
      insPos.run(ticker, p.name || '', p.type || 'Stock', p.currentPrice || 0,
        p.quantity || p.numShares || 0, p.dayGainLoss || 0, p.value || 0, p.dayHigh || 0, p.dayLow || 0, p.dividendRate || 0);
    }

    const insTx = db.prepare('INSERT OR IGNORE INTO investment_transactions (date, time, action, ticker, numberOfShares, pricePerShare, totalAmount, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
    for (const t of (data.transactions || [])) {
      const date = t.dateEpochDay ?? t.date;
      const time = t.timeSecondOfDay ?? t.time ?? null;
      insTx.run(date, time, t.action || 'Buy', t.ticker, t.numberOfShares || 0, t.pricePerShare || 0, t.totalAmount || 0, t.note || '');
    }

    if (data.performanceRecords?.length) {
      const insPerf = db.prepare('INSERT OR IGNORE INTO account_performance (id, accountId, totalValue, date, note) VALUES (?, ?, ?, ?, ?)');
      for (const p of data.performanceRecords) {
        insPerf.run(p.id, p.accountId, p.totalValue || 0, p.dateEpochDay ?? p.date, p.note || '');
      }
    }
    if (data.watchLists?.length) {
      const insWL = db.prepare('INSERT OR REPLACE INTO watch_lists (id, name) VALUES (?, ?)');
      for (const w of data.watchLists) insWL.run(w.id, w.name);
    }
    if (data.watchListItems?.length) {
      const insWLI = db.prepare('INSERT OR REPLACE INTO watch_list_items (id, watchListId, ticker, shares, priceWhenAdded, addedDate, reminderDateTime, reminderMessage) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
      for (const i of data.watchListItems) {
        insWLI.run(i.id, i.watchListId, i.ticker, i.shares || 0, i.priceWhenAdded || 0,
          i.addedDateEpochDay ?? i.addedDate, i.reminderDateTimeEpochMs ?? i.reminderDateTime ?? null, i.reminderMessage || '');
      }
    }
    if (data.changeHistory?.length) {
      const insCH = db.prepare('INSERT OR REPLACE INTO change_history (id, date, etfValue, stockValue, totalValue, dailyChangeEtf, dailyChangeStock, dailyChangeTotal) VALUES (?, ?, ?, ?, ?, ?, ?, ?)');
      for (const c of data.changeHistory) {
        insCH.run(c.id, c.dateEpochDay ?? c.date, c.etfValue || 0, c.stockValue || 0, c.totalValue || 0,
          c.dailyChangeEtf || 0, c.dailyChangeStock || 0, c.dailyChangeTotal || 0);
      }
    }
    if (data.definitions?.length) {
      const insDef = db.prepare('INSERT OR REPLACE INTO definitions (id, name, description) VALUES (?, ?, ?)');
      for (const d of data.definitions) insDef.run(d.id, d.name, d.description || '');
    }
    if (data.sqlLibrary?.length) {
      const insSQL = db.prepare('INSERT OR REPLACE INTO sql_library (id, name, description, category, sql) VALUES (?, ?, ?, ?, ?)');
      for (const s of data.sqlLibrary) insSQL.run(s.id, s.name, s.description || '', s.category || '', s.sql || '');
    }
    if (data.aiLibrary?.length) {
      const insAI = db.prepare('INSERT OR REPLACE INTO ai_library (id, name, description, promptText) VALUES (?, ?, ?, ?)');
      for (const a of data.aiLibrary) insAI.run(a.id, a.name, a.description || '', a.promptText || '');
    }
  });
  tx();

  return {
    accounts: (data.accounts || []).length,
    positions: (data.positions || data.items || []).length,
    transactions: (data.transactions || []).length,
    performanceRecords: (data.performanceRecords || []).length,
    watchLists: (data.watchLists || []).length,
    watchListItems: (data.watchListItems || []).length,
    changeHistory: (data.changeHistory || []).length,
    definitions: (data.definitions || []).length,
    sqlLibrary: (data.sqlLibrary || []).length,
    aiLibrary: (data.aiLibrary || []).length,
  };
}

router.post('/import', upload.single('file'), (req, res) => {
  try {
    const text = req.file ? req.file.buffer.toString('utf8') : req.body.data;
    if (!text) return res.status(400).json({ error: 'No file provided' });
    const data = JSON.parse(text);
    const version = data.version || 1;
    console.log('Backup import: version', version, 'keys:', Object.keys(data).join(', '));

    let counts;
    if (version >= 6) {
      counts = restoreGeneric(data);
    } else {
      counts = restoreLegacy(data);
    }

    res.json({ ok: true, ...counts });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/list', (req, res) => {
  if (!fs.existsSync(BACKUP_DIR)) return res.json([]);
  const files = fs.readdirSync(BACKUP_DIR)
    .filter(f => f.endsWith('.json'))
    .map(f => ({ name: f, size: fs.statSync(path.join(BACKUP_DIR, f)).size, modified: fs.statSync(path.join(BACKUP_DIR, f)).mtime }))
    .sort((a, b) => new Date(b.modified) - new Date(a.modified));
  res.json(files);
});

router.delete('/:filename', (req, res) => {
  const filepath = path.join(BACKUP_DIR, req.params.filename);
  if (fs.existsSync(filepath)) fs.unlinkSync(filepath);
  res.json({ ok: true });
});

module.exports = router;
module.exports.exportAllTablesGeneric = exportAllTablesGeneric;
