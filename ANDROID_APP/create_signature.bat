@echo off
call env.bat
echo ============================================
echo  Create Android Signing Keystore
echo ============================================
echo.
set KEYTOOL=%JAVA_HOME%\bin\keytool.exe

if not exist "%KEYTOOL%" (
    echo [ERROR] keytool not found at: %KEYTOOL%
    echo Make sure JAVA_HOME is set correctly.
    pause
    exit /b 1
)

echo This will generate a keystore and keystore.properties file
echo for signing your release APK.
echo.

set /p KEYSTORE_FILE="Keystore filename (default: release-keystore.jks): "
if "%KEYSTORE_FILE%"=="" set KEYSTORE_FILE=release-keystore.jks

if exist "%KEYSTORE_FILE%" (
    echo.
    echo [WARNING] %KEYSTORE_FILE% already exists!
    set /p OVERWRITE="Overwrite? (y/N): "
    if /i not "%OVERWRITE%"=="y" (
        echo Aborted.
        pause
        exit /b 0
    )
    del "%KEYSTORE_FILE%"
)

set /p KEY_ALIAS="Key alias (default: investhelp): "
if "%KEY_ALIAS%"=="" set KEY_ALIAS=investhelp

set /p STORE_PASS="Keystore password (min 6 chars): "
if "%STORE_PASS%"=="" (
    echo [ERROR] Password cannot be empty.
    pause
    exit /b 1
)

set /p KEY_PASS="Key password (press Enter to use same as keystore): "
if "%KEY_PASS%"=="" set KEY_PASS=%STORE_PASS%

set /p VALIDITY="Validity in days (default: 10000): "
if "%VALIDITY%"=="" set VALIDITY=10000

set /p CN="Your name (CN): "
if "%CN%"=="" set CN=Developer

echo.
echo Generating keystore...
echo.

"%KEYTOOL%" -genkeypair -v ^
    -keystore "%KEYSTORE_FILE%" ^
    -alias %KEY_ALIAS% ^
    -keyalg RSA ^
    -keysize 2048 ^
    -validity %VALIDITY% ^
    -storepass %STORE_PASS% ^
    -keypass %KEY_PASS% ^
    -dname "CN=%CN%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Keystore generation failed!
    pause
    exit /b 1
)

echo.
echo Keystore created: %KEYSTORE_FILE%
echo.

echo Writing keystore.properties...
(
    echo storeFile=%KEYSTORE_FILE%
    echo storePassword=%STORE_PASS%
    echo keyAlias=%KEY_ALIAS%
    echo keyPassword=%KEY_PASS%
)> keystore.properties

echo.
echo ============================================
echo  Done!
echo  Created:
echo    - %KEYSTORE_FILE%  (keystore)
echo    - keystore.properties  (build config)
echo.
echo  DO NOT commit these files to git!
echo  Make sure they are in .gitignore.
echo ============================================
echo.
pause
