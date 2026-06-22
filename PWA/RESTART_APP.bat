@echo off
cd /d "%~dp0"

echo Killing anything on port 3000...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":3000 "') do (
    taskkill /F /PID %%a 2>nul
)
timeout /t 1 /nobreak >nul

echo Starting PWA server...
start /b "" node server\index.js > server.log 2>&1

timeout /t 2 /nobreak >nul
echo Server started at http://localhost:3000
