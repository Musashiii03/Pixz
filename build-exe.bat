@echo off
echo ========================================
echo Building WinGallery Executable
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
echo Your executable installer is located at:
echo target\dist\WinGallery-1.0.0.exe
echo.
pause
