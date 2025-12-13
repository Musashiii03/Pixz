@echo off
echo ========================================
echo Building WinGallery Standalone Package
echo ========================================
echo.

echo Step 1: Cleaning previous builds...
call mvnw.cmd clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Building fat JAR with all dependencies...
call mvnw.cmd package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Package failed!
    pause
    exit /b 1
)

echo.
echo Step 3: Creating standalone package...
if not exist "target\standalone" mkdir "target\standalone"
copy "target\WinGallery-1.0-SNAPSHOT.jar" "target\standalone\WinGallery.jar"

echo @echo off > "target\standalone\WinGallery.bat"
echo echo Starting WinGallery... >> "target\standalone\WinGallery.bat"
echo java -jar "%%~dp0WinGallery.jar" >> "target\standalone\WinGallery.bat"
echo if %%ERRORLEVEL%% NEQ 0 ( >> "target\standalone\WinGallery.bat"
echo     echo. >> "target\standalone\WinGallery.bat"
echo     echo ERROR: Java not found! >> "target\standalone\WinGallery.bat"
echo     echo Please install Java from: https://adoptium.net/ >> "target\standalone\WinGallery.bat"
echo     pause >> "target\standalone\WinGallery.bat"
echo ) >> "target\standalone\WinGallery.bat"

echo.
echo ========================================
echo BUILD SUCCESSFUL!
echo ========================================
echo.
echo Your standalone application is located at:
echo target\standalone\
echo.
echo Files:
echo - WinGallery.jar (the application)
echo - WinGallery.bat (launcher)
echo.
echo To run: Double-click target\standalone\WinGallery.bat
echo.
echo Note: Java must be installed on the target PC.
echo For a version without Java requirement, use build-portable.bat
echo.
pause
