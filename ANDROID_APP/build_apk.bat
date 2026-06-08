@echo off
call env.bat
echo ============================================
echo  Building Invest Help - Release APK
echo ============================================
echo.
echo Using JAVA_HOME: %JAVA_HOME%
echo.

echo Running clean...
call gradlew.bat clean
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Clean failed!
    pause
    exit /b 1
)

echo.
echo Running assembleRelease...
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
