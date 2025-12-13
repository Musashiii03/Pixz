@echo off
echo ========================================
echo Building WinGallery for GitHub Release
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
echo Step 2: Compiling and packaging...
call mvnw.cmd package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Package failed!
    pause
    exit /b 1
)

echo.
echo Step 3: Creating runtime image with jlink...
call mvnw.cmd javafx:jlink
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jlink failed!
    pause
    exit /b 1
)

echo.
echo Step 4: Creating portable app (no installer needed)...
call mvnw.cmd jpackage:jpackage@portable
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Portable build failed!
    pause
    exit /b 1
)

echo.
echo Step 5: Creating ZIP for distribution...
cd target\portable
powershell -Command "Compress-Archive -Path WinGallery -DestinationPath ..\WinGallery-1.0.0-Windows-Portable.zip -Force"
cd ..\..

echo.
echo ========================================
echo BUILD SUCCESSFUL!
echo ========================================
echo.
echo Your portable app is located at:
echo   target\portable\WinGallery\
echo.
echo Your ZIP for GitHub release is at:
echo   target\WinGallery-1.0.0-Windows-Portable.zip
echo.
echo To upload to GitHub:
echo 1. Go to https://github.com/Musashiiii03/WinGallery/releases/tag/v1.0.0
echo 2. Click "Edit release"
echo 3. Drag and drop: target\WinGallery-1.0.0-Windows-Portable.zip
echo 4. Click "Update release"
echo.
echo Users can download, extract, and run WinGallery.exe directly!
echo.
pause
