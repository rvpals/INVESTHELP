@echo off
echo ============================================
echo  Building Invest Help - Release APK
echo ============================================
echo.

call gradlew.bat assembleRelease

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

echo.
echo ============================================
echo  Build successful!
echo  APK location: app\build\outputs\apk\release\
echo ============================================
echo.

explorer "app\build\outputs\apk\release"
pause
