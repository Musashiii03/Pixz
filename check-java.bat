@echo off
echo ========================================
echo Java Environment Check
echo ========================================
echo.

echo Checking Java version...
java -version
echo.

echo Checking if javac (Java compiler) is available...
javac -version
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: javac not found!
    echo You are running JRE, not JDK.
    echo.
    echo Please install JDK from one of these sources:
    echo 1. Eclipse Temurin: https://adoptium.net/
    echo 2. Oracle JDK: https://www.oracle.com/java/technologies/downloads/
    echo.
    echo After installation, set JAVA_HOME to JDK path:
    echo Example: set JAVA_HOME=C:\Program Files\Java\jdk-21
    echo.
    pause
    exit /b 1
)

echo.
echo Checking JAVA_HOME...
if "%JAVA_HOME%"=="" (
    echo WARNING: JAVA_HOME is not set!
    echo Please set it to your JDK installation path.
    echo Example: set JAVA_HOME=C:\Program Files\Java\jdk-21
) else (
    echo JAVA_HOME = %JAVA_HOME%
    echo.
    echo Checking if JAVA_HOME points to JDK...
    if exist "%JAVA_HOME%\bin\javac.exe" (
        echo SUCCESS: JAVA_HOME points to a valid JDK!
    ) else (
        echo ERROR: JAVA_HOME does not point to a JDK!
        echo Please update JAVA_HOME to point to JDK installation.
    )
)

echo.
echo ========================================
echo Check Complete
echo ========================================
pause
