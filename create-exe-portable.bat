@echo off
echo ========================================
echo Creating Pixz Portable EXE Release
echo ========================================
echo.

REM Step 1: Build the JAR with Maven
echo Building JAR with Maven...
call mvnw.cmd package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)

REM Step 2: Create portable directory structure
echo Creating directory structure...
if exist "target\Pixz-Portable-EXE" rmdir /s /q "target\Pixz-Portable-EXE"
mkdir "target\Pixz-Portable-EXE"

REM Step 3: Use jpackage to create portable app image
echo Creating portable app with jpackage...
jpackage ^
  --type app-image ^
  --name Pixz ^
  --app-version 1.1.0 ^
  --input target ^
  --main-jar Pixz-1.1.0.jar ^
  --main-class com.example.pixz.Launcher ^
  --dest target\Pixz-Portable-EXE ^
  --icon src\main\resources\com\example\pixz\icon.ico ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --vendor "Pixz" ^
  --description "Photo and Video Gallery"

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jpackage failed!
    echo Make sure you have JDK 21 or higher installed.
    pause
    exit /b 1
)

REM Step 4: Create README in the Pixz folder
echo Creating README...
(
echo Pixz - Photo and Video Gallery v1.1.0
echo =====================================
echo.
echo This is a portable version - no installation required!
echo.
echo To Run:
echo - Double-click Pixz.exe
echo.
echo Features:
echo - Browse photos and videos in a beautiful gallery
echo - Video playback with loop controls
echo - Keyboard shortcuts: Space, M, R, arrows, F, ESC
echo - Auto-rotation for images and videos
echo - Thumbnail caching for fast loading
echo.
echo Keyboard Shortcuts:
echo - Space: Play/Pause
echo - M: Mute/Unmute
echo - R: Rotate 90 degrees
echo - Left/Right arrows: Navigate between media
echo - F or F11: Toggle fullscreen
echo - ESC: Close fullscreen viewer
echo.
echo Note: Some video codecs ^(H.265, VP9, MKV^) may not be supported.
echo Use "Open in System Player" button for unsupported videos.
echo.
echo This portable version includes its own Java runtime.
echo No need to install Java separately!
) > "target\Pixz-Portable-EXE\Pixz\README.txt"

REM Step 5: Create ZIP
echo Creating ZIP archive...
cd target\Pixz-Portable-EXE
powershell -Command "Compress-Archive -Path Pixz -DestinationPath ..\Pixz-1.1.0-Portable.zip -Force"
cd ..\..

echo.
echo ========================================
echo BUILD SUCCESSFUL!
echo ========================================
echo.
echo Portable EXE release created at:
echo   target\Pixz-Portable-EXE\Pixz\
echo.
echo ZIP archive created at:
echo   target\Pixz-1.1.0-Portable.zip
echo.
echo Contents:
echo - Pixz.exe (main executable)
echo - Java runtime (bundled, no installation needed)
echo - All dependencies included
echo.
echo To distribute:
echo 1. Share target\Pixz-1.1.0-Portable.zip
echo 2. Users extract and run Pixz.exe
echo 3. No Java installation required!
echo.
pause
