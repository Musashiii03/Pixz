@echo off
echo ========================================
echo Building WinGallery Installer for GitHub Release
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
echo Step 4: Creating Windows installer with jpackage...
call mvnw.cmd jpackage:jpackage
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jpackage failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo BUILD SUCCESSFUL!
echo ========================================
echo.
echo Your installer is located at:
dir /b target\installer\*.exe
echo.
echo Full path: %CD%\target\installer\
echo.
echo This file is ready to upload to GitHub Releases!
echo.
echo To upload:
echo 1. Go to https://github.com/Musashiiii03/WinGallery/releases/tag/v1.0.0
echo 2. Click "Edit release"
echo 3. Drag and drop the .exe file from target\installer\
echo 4. Click "Update release"
echo.
pause
