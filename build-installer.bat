@echo off
echo ========================================
echo Building WinGallery Installer for GitHub Release
echo ========================================
echo.

REM Check if Inno Setup is installed
where iscc >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Inno Setup not found!
    echo.
    echo Please install Inno Setup from: https://jrsoftware.org/isdl.php
    echo After installation, add it to your PATH or run this script from Inno Setup directory.
    echo.
    pause
    exit /b 1
)

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
echo Step 4: Creating app image with jpackage...
call mvnw.cmd jpackage:jpackage
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jpackage failed!
    pause
    exit /b 1
)

echo.
echo Step 5: Creating installer with Inno Setup...
iscc setup.iss
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Inno Setup compilation failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo BUILD SUCCESSFUL!
echo ========================================
echo.
echo Your installer is located at:
echo target\installer\WinGallery-Setup-1.0.0.exe
echo.
echo This file is ready to upload to GitHub Releases!
echo.
pause
