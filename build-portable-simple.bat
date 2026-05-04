@echo off
REM Simplified Portable Build for Pixz
REM Uses jpackage directly with shaded JAR (skips jlink)

echo ========================================
echo   Pixz Portable Build (Simplified)
echo ========================================
echo.

echo [1/3] Checking Java version...
java -version
if errorlevel 1 (
    echo ERROR: Java not found!
    exit /b 1
)
echo.

echo [2/3] Building shaded JAR...
call mvnw.cmd clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Build failed!
    exit /b 1
)
echo.

echo [3/3] Creating portable package with jpackage...
jpackage --type app-image --name Pixz --app-version 1.3.0 --vendor Pixz --dest target\portable --input target --main-jar Pixz-1.3.0.jar --main-class com.example.pixz.Launcher --icon src\main\resources\com\example\pixz\pixz_128x128.ico --java-options "-Dfile.encoding=UTF-8"
if errorlevel 1 (
    echo ERROR: jpackage failed!
    exit /b 1
)
echo.

echo ========================================
echo   BUILD SUCCESSFUL!
echo ========================================
echo.
echo Portable application location:
echo   target\portable\Pixz\
echo.
echo To distribute:
echo   1. Compress the 'target\portable\Pixz' folder to a ZIP file
echo   2. Share the ZIP file with users
echo   3. Users can extract and run Pixz.exe directly
echo.

pause
