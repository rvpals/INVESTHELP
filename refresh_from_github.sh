#!/bin/bash

# Navigate to your project directory (Replace with your actual NAS path)
cd "$(dirname "$0")"

echo "Checking for updates from GitHub..."

# 1. Fetch latest changes from remote without merging yet
git fetch origin

# Check if we are behind the remote branch
LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse @{u})

if [ "$LOCAL" = "$REMOTE" ]; then
    echo "App is already up to date."
else
    echo "New updates found! Pulling latest changes..."
    
    # 2. Temporarily stash any local files/logs to prevent git conflicts
    git stash
    
    # 3. Pull the code
    git pull origin main  # Change 'main' to 'master' if your branch uses the old naming
    
    # 4. Re-apply any local changes if necessary
    git stash pop
    
    # 5. Check if package.json changed, and update dependencies if it did
    if git diff --name-only HEAD@{1} HEAD | grep -q "package.json"; then
        echo "package.json changed. Updating dependencies..."
        npm install
    fi
    
    echo "Code updated successfully!"
    
    # 6. OPTIONAL: If using PM2 to manage your app, trigger a zero-downtime reload
    # pm2 reload server/index.js
fi