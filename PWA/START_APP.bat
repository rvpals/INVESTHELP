@echo off
title InvestHelp PWA Server
cd /d "%~dp0"

echo ============================================
echo   InvestHelp PWA Server
echo ============================================
echo.

:: Check if node_modules exists
if not exist "node_modules" (
    echo Installing dependencies...
    echo.
    call npm install
    echo.
)

echo Starting server on http://localhost:3000
echo Press Ctrl+C to stop.
echo.

node server/index.js

pause
