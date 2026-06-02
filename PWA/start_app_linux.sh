cat << 'EOF' > start_pwa.sh
#!/bin/bash

# Set terminal window title
echo -ne "\033]0;InvestHelp PWA Server\007"

# Navigate to the directory where this script is located
cd "$(dirname "$0")"

echo "============================================"
echo "   InvestHelp PWA Server"
echo "============================================"
echo ""

# Check if node_modules directory does not exist
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    echo ""
    npm install
    echo ""
fi

echo "Starting server on http://localhost:3000"
echo "Press Ctrl+C to stop."
echo ""

# Start the Node.js server
node server/index.js

# Mimic the Windows 'pause' command
read -p "Press [Enter] to continue..."
EOF