#!/bin/bash

cd "$(dirname "$0")"

echo "Stopping existing Node server..."
pkill -f "node server/index.js" 2>/dev/null
sleep 1

echo "Pulling latest code from GitHub..."
cd ..
git fetch origin
git stash
git pull origin master
git stash pop 2>/dev/null
cd PWA

if git diff --name-only HEAD@{1} HEAD 2>/dev/null | grep -q "package.json"; then
    echo "package.json changed. Installing dependencies..."
    npm install
fi

echo "Starting PWA server..."
nohup node server/index.js > server.log 2>&1 &
sleep 2

if curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 | grep -q "200"; then
    echo "Server started successfully at http://localhost:3000"
else
    echo "Server may have failed to start. Check server.log"
fi
