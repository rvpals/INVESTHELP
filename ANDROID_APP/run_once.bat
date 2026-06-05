@echo off
echo ============================================
echo  First-Time Setup for InvestHelp Android
echo ============================================
echo.
echo This script creates local config files that
echo are gitignored and needed for building.
echo.

:: ---- keystore.properties ----
if exist keystore.properties (
    echo [SKIP] keystore.properties already exists.
) else (
    echo Creating keystore.properties...
    set /p STORE_FILE="Keystore filename (default: release-keystore.jks): "
    if "%STORE_FILE%"=="" set STORE_FILE=release-keystore.jks
    set /p KEY_ALIAS="Key alias (default: investhelp): "
    if "%KEY_ALIAS%"=="" set KEY_ALIAS=investhelp
    set /p STORE_PASS="Keystore password: "
    set /p KEY_PASS="Key password (Enter for same as keystore): "
    if "%KEY_PASS%"=="" set KEY_PASS=%STORE_PASS%

    (
        echo storeFile=%STORE_FILE%
        echo storePassword=%STORE_PASS%
        echo keyAlias=%KEY_ALIAS%
        echo keyPassword=%KEY_PASS%
    )> keystore.properties

    echo [OK] keystore.properties created.
)
echo.

:: ---- local.properties ----
if exist local.properties (
    echo [SKIP] local.properties already exists.
) else (
    echo Creating local.properties...
    set /p SDK_PATH="Android SDK path (e.g. C\:\\Users\\you\\AppData\\Local\\Android\\Sdk): "
    (
        echo sdk.dir=%SDK_PATH%
    )> local.properties
    echo [OK] local.properties created.
)
echo.

:: ---- keystore file ----
if exist release-keystore.jks (
    echo [SKIP] release-keystore.jks already exists.
) else (
    echo No keystore file found.
    set /p GEN_KEY="Generate a new keystore now? (Y/n): "
    if /i "%GEN_KEY%"=="n" (
        echo Skipped. Run create_signature.bat later to generate one.
    ) else (
        call create_signature.bat
    )
)
echo.

echo ============================================
echo  Setup complete! You can now run:
echo    build_apk.bat     - Build release APK
echo    start_emulator.bat - Run on emulator
echo ============================================
echo.
pause
