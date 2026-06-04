#!/bin/bash

cd "$(dirname "$0")"

echo "=== InvestHelp PWA Full Reset ==="

# 1. Stop existing server
echo "Stopping server..."
pkill -f "node server/index.js" 2>/dev/null
sleep 1

# 2. Backup the SQLite database
DB_FILE="server/investhelp.db"
if [ -f "$DB_FILE" ]; then
    BACKUP_NAME="server/investhelp_backup_$(date +%Y%m%d_%H%M%S).db"
    cp "$DB_FILE" "$BACKUP_NAME"
    echo "Database backed up to: $BACKUP_NAME"
else
    echo "No database file found, skipping backup."
fi

# 3. Hard reset to match remote
echo "Fetching latest from GitHub..."
cd ..
git fetch origin
echo "Resetting to origin/master..."
git reset --hard origin/master
echo "Cleaning untracked files..."
git clean -fd
cd PWA

# 4. Restore the database backup
if [ -n "$BACKUP_NAME" ] && [ -f "$BACKUP_NAME" ]; then
    cp "$BACKUP_NAME" "$DB_FILE"
    echo "Database restored from backup."
fi

# 5. Install dependencies
echo "Installing dependencies..."
npm install

# 6. Start server
echo "Starting PWA server..."
nohup node server/index.js > server.log 2>&1 &
sleep 2

if curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 | grep -q "200"; then
    echo "Server started successfully at http://localhost:3000"
else
    echo "Server may have failed to start. Check server.log"
fi

echo "=== Reset complete ==="
