@echo off
echo ========================================
echo Building WinGallery Portable Version
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
echo Step 4: Creating portable launcher...
if not exist "target\portable" mkdir "target\portable"
xcopy /E /I /Y "target\app" "target\portable\WinGallery"

echo @echo off > "target\portable\WinGallery.bat"
echo cd /d "%%~dp0WinGallery\bin" >> "target\portable\WinGallery.bat"
echo start "" "java.exe" -m com.example.wingallery/com.example.wingallery.Launcher >> "target\portable\WinGallery.bat"

echo.
echo ========================================
echo BUILD SUCCESSFUL!
echo ========================================
echo.
echo Your portable application is located at:
echo target\portable\WinGallery\
echo.
echo To run: Double-click target\portable\WinGallery.bat
echo.
echo You can copy the entire "portable" folder to any Windows PC
echo and run it without installing Java!
echo.
pause
