#!/bin/bash

cd "$(dirname "$0")"

echo "Killing anything on port 3000..."
fuser -k 3000/tcp 2>/dev/null
pkill -f "node server/index.js" 2>/dev/null
sleep 1

echo "Starting PWA server..."
nohup node server/index.js > server.log 2>&1 &
sleep 2

if curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 | grep -q "200"; then
    echo "Server started successfully at http://localhost:3000"
else
    echo "Server may have failed to start. Check server.log"
fi
