#!/bin/bash

cd "$(dirname "$0")"

PORT=${PORT:-3000}

echo "Killing existing InvestHelp server..."
sudo pkill -f "node server/index.js" 2>/dev/null
sleep 1

echo "Starting PWA server on port $PORT..."
PORT=$PORT nohup node server/index.js > server.log 2>&1 &
sleep 2

if curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT | grep -q "200"; then
    echo "Server started successfully at http://localhost:$PORT"
else
    echo "Server may have failed to start. Check server.log"
fi
