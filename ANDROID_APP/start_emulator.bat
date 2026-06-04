@echo off
echo ============================================
echo  Starting Emulator with Invest Help
echo ============================================
echo.

REM Find the latest available emulator AVD
for /f "tokens=*" %%a in ('emulator -list-avds 2^>nul') do set AVD_NAME=%%a

if "%AVD_NAME%"=="" (
    echo [ERROR] No AVDs found. Create one in Android Studio first.
    pause
    exit /b 1
)

echo Using AVD: %AVD_NAME%
echo.

REM Start emulator in background with physical keyboard enabled
echo Starting emulator...
start "" emulator -avd %AVD_NAME% -qemu -k en-us

REM Wait for device to boot
echo Waiting for device to boot...
adb wait-for-device
:wait_boot
adb shell getprop sys.boot_completed 2>nul | find "1" >nul
if %ERRORLEVEL% NEQ 0 (
    timeout /t 2 /nobreak >nul
    goto wait_boot
)

echo Device booted.
echo.

REM Enable physical keyboard on emulator
adb shell settings put secure show_ime_with_hard_keyboard 0

REM Build and install the app
echo Building and installing app...
call gradlew.bat installDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build/install failed!
    pause
    exit /b 1
)

REM Launch the app
echo Launching Invest Help...
adb shell am start -n com.investhelp.app/.MainActivity

echo.
echo ============================================
echo  Invest Help is running on %AVD_NAME%
echo  Physical keyboard: enabled
echo ============================================
pause
