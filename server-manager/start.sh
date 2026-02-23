#!/bin/bash

# ============================================================================
# Luna2 Server Manager Docker Setup
# Starts the Web UI manager for Nginx, SSL, and Domain management
# ============================================================================

set -e

echo "🚀 Starting Luna2 Server Manager..."
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed"
    echo "Install with: curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash - && sudo apt install -y nodejs"
    exit 1
fi

echo "✓ Node.js $(node -v) found"

# Check if npm modules are installed
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install --production --silent
    echo "✓ Dependencies installed"
fi

# Start the server
echo ""
echo "🌐 Starting web server on port 8888..."
echo ""

exec node server.js
