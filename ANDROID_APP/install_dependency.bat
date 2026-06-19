@echo off
setlocal

echo ============================================
echo  Invest Help - Dependency Installer
echo ============================================
echo.

:: ------------------------------------------------
:: 1. Check/Install JDK 17
:: ------------------------------------------------
echo [1/3] Checking JDK 17...

set "JAVA_HOME_PATH=C:\Program Files\Java\jdk1.8.0_211"

if exist "%JAVA_HOME_PATH%\bin\java.exe" (
    echo  Found JDK 17 at %JAVA_HOME_PATH%
) else (
    echo  JDK 17 not found at %JAVA_HOME_PATH%
    echo  Downloading JDK 17 (Eclipse Temurin)...
    curl -L -o "%TEMP%\jdk17.zip" "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
    if errorlevel 1 (
        echo  ERROR: Failed to download JDK 17.
        echo  Please install JDK 17 manually from: https://adoptium.net/temurin/releases/?version=17
        pause
        exit /b 1
    )
    echo  Extracting JDK 17 to %JAVA_HOME_PATH%...
    powershell -Command "Expand-Archive -Path '%TEMP%\jdk17.zip' -DestinationPath '%TEMP%\jdk17_extract' -Force"
    for /d %%D in ("%TEMP%\jdk17_extract\*") do (
        xcopy "%%D\*" "%JAVA_HOME_PATH%\" /E /I /Q /Y >nul
    )
    del "%TEMP%\jdk17.zip" 2>nul
    rmdir /s /q "%TEMP%\jdk17_extract" 2>nul
    echo  JDK 17 installed to %JAVA_HOME_PATH%
)

set "JAVA_HOME=%JAVA_HOME_PATH%"
echo  JAVA_HOME set to %JAVA_HOME%
echo.

:: ------------------------------------------------
:: 2. Check/Install Android SDK Command-Line Tools
:: ------------------------------------------------
echo [2/3] Checking Android SDK...

if defined ANDROID_HOME (
    set "SDK_PATH=%ANDROID_HOME%"
) else if defined ANDROID_SDK_ROOT (
    set "SDK_PATH=%ANDROID_SDK_ROOT%"
) else (
    set "SDK_PATH=%LOCALAPPDATA%\Android\Sdk"
)

echo  SDK path: %SDK_PATH%

if not exist "%SDK_PATH%\cmdline-tools\latest\bin\sdkmanager.bat" (
    echo  Android SDK command-line tools not found.
    echo  Downloading Android SDK command-line tools...
    if not exist "%SDK_PATH%\cmdline-tools" mkdir "%SDK_PATH%\cmdline-tools"
    curl -L -o "%TEMP%\cmdline-tools.zip" "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
    if errorlevel 1 (
        echo  ERROR: Failed to download Android SDK command-line tools.
        echo  Please install Android Studio from: https://developer.android.com/studio
        pause
        exit /b 1
    )
    powershell -Command "Expand-Archive -Path '%TEMP%\cmdline-tools.zip' -DestinationPath '%TEMP%\cmdline-tools-extract' -Force"
    xcopy "%TEMP%\cmdline-tools-extract\cmdline-tools\*" "%SDK_PATH%\cmdline-tools\latest\" /E /I /Q /Y >nul
    del "%TEMP%\cmdline-tools.zip" 2>nul
    rmdir /s /q "%TEMP%\cmdline-tools-extract" 2>nul
    echo  Command-line tools installed.
) else (
    echo  Found Android SDK command-line tools.
)

set "ANDROID_HOME=%SDK_PATH%"
echo.

:: ------------------------------------------------
:: 3. Install required SDK packages
:: ------------------------------------------------
echo [3/3] Installing required SDK packages...
echo  (Accept licenses if prompted)
echo.

call "%SDK_PATH%\cmdline-tools\latest\bin\sdkmanager.bat" --licenses < nul 2>nul
echo y | call "%SDK_PATH%\cmdline-tools\latest\bin\sdkmanager.bat" --licenses 2>nul

echo  Installing platform-tools...
call "%SDK_PATH%\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools"

echo  Installing Android API 35 (compileSdk)...
call "%SDK_PATH%\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-35"

echo  Installing Build Tools 35.0.0...
call "%SDK_PATH%\cmdline-tools\latest\bin\sdkmanager.bat" "build-tools;35.0.0"

echo  Installing Android API 29 (minSdk)...
call "%SDK_PATH%\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-29"

echo.

:: ------------------------------------------------
:: 4. Create local.properties
:: ------------------------------------------------
echo Creating local.properties...
echo sdk.dir=%SDK_PATH:\=/% > "%~dp0local.properties"
echo  local.properties created with sdk.dir=%SDK_PATH:\=/%
echo.

:: ------------------------------------------------
:: Done
:: ------------------------------------------------
echo ============================================
echo  Installation complete!
echo ============================================
echo.
echo  JAVA_HOME: %JAVA_HOME%
echo  ANDROID_HOME: %ANDROID_HOME%
echo.
echo  To build the app, run:
echo    set JAVA_HOME=%JAVA_HOME%
echo    gradlew assembleRelease
echo.
pause
