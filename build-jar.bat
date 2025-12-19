@echo off
echo ========================================
echo Building WinGallery JAR
echo ========================================
echo.

echo Cleaning and building...
call mvnw.cmd clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo BUILD SUCCESSFUL!
echo ========================================
echo.
echo Your JAR file is located at:
echo target\WinGallery-1.1.0.jar
echo.
echo To run the application:
echo java -jar target\WinGallery-1.1.0.jar
echo.
pause
