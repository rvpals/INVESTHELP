@echo off

cd /d "%~dp0"

echo Checking for updates from GitHub...

REM Fetch latest changes from remote without merging yet
git fetch origin

REM Check if we are behind the remote branch
for /f %%i in ('git rev-parse @') do set LOCAL=%%i
for /f %%i in ('git rev-parse @{u}') do set REMOTE=%%i

if "%LOCAL%"=="%REMOTE%" (
    echo App is already up to date.
) else (
    echo New updates found! Pulling latest changes...

    REM Temporarily stash any local files/logs to prevent git conflicts
    git stash

    REM Pull the code
    git pull origin master

    REM Re-apply any local changes if necessary
    git stash pop

    REM Check if package.json changed, and update dependencies if it did
    git diff --name-only HEAD@{1} HEAD | findstr /C:"package.json" >nul
    if not errorlevel 1 (
        echo package.json changed. Updating dependencies...
        cd PWA && npm install && cd ..
    )

    echo Code updated successfully!
)

pause
