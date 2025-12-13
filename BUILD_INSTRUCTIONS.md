# WinGallery - Build Instructions

## Prerequisites

1. **Java Development Kit (JDK) 21 or higher**
   - Download from: https://adoptium.net/ or https://www.oracle.com/java/technologies/downloads/
   - Make sure JAVA_HOME environment variable is set

2. **Maven** (included via Maven Wrapper - mvnw.cmd)
   - No separate installation needed

3. **For Windows EXE (Optional)**
   - WiX Toolset 3.x for creating MSI installer
   - Download from: https://wixtoolset.org/releases/

## Build Options

### Option 1: Quick JAR Build (Recommended for Testing)

Simply double-click `build-jar.bat` or run:
```cmd
build-jar.bat
```

This creates a JAR file at: `target\WinGallery-1.0-SNAPSHOT.jar`

To run:
```cmd
java -jar target\WinGallery-1.0-SNAPSHOT.jar
```

### Option 2: Windows Executable Installer (Full Distribution)

Double-click `build-exe.bat` or run:
```cmd
build-exe.bat
```

This creates a Windows installer at: `target\dist\WinGallery-1.0.0.exe`

**Note:** This requires JDK with jpackage tool (included in JDK 14+)

### Option 3: Manual Build Commands

**Build JAR:**
```cmd
mvnw.cmd clean package -DskipTests
```

**Build Runtime Image:**
```cmd
mvnw.cmd javafx:jlink
```

**Build Windows Installer:**
```cmd
mvnw.cmd jpackage:jpackage
```

## Running the Application

### From Source:
```cmd
mvnw.cmd javafx:run
```

### From JAR:
```cmd
java -jar target\WinGallery-1.0-SNAPSHOT.jar
```

### From Installer:
After running the installer, launch from Start Menu or Desktop shortcut.

## Troubleshooting

### "No compiler is provided in this environment" or "Perhaps you are running on a JRE rather than a JDK?"

**Problem:** You have JRE installed instead of JDK, or JAVA_HOME points to JRE.

**Solution:**

1. **Check your Java installation:**
   ```cmd
   check-java.bat
   ```

2. **Install JDK (if not installed):**
   - Download JDK 21 from: https://adoptium.net/temurin/releases/
   - Choose "JDK" (not JRE)
   - Install it (e.g., to `C:\Program Files\Eclipse Adoptium\jdk-21.0.x`)

3. **Set JAVA_HOME permanently:**
   - Open System Properties → Advanced → Environment Variables
   - Add new System Variable:
     - Name: `JAVA_HOME`
     - Value: `C:\Program Files\Eclipse Adoptium\jdk-21.0.x` (your JDK path)
   - Edit `Path` variable and add: `%JAVA_HOME%\bin`
   - Restart Command Prompt

4. **Or set temporarily for current session:**
   ```cmd
   set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.x
   set PATH=%JAVA_HOME%\bin;%PATH%
   ```

### "JAVA_HOME not found"
Set JAVA_HOME environment variable:
```cmd
set JAVA_HOME=C:\Path\To\Your\JDK
```

### "jpackage not found"
Make sure you're using JDK 14 or higher with jpackage included.

### Build fails
Try cleaning first:
```cmd
mvnw.cmd clean
```

### How to verify JDK is properly installed:
```cmd
javac -version
```
This should show the Java compiler version. If it says "command not found", you don't have JDK installed or configured.

## Distribution

The Windows installer (`WinGallery-1.0.0.exe`) can be distributed to users who don't have Java installed. It includes a bundled JRE.

## Features

- Photo and video gallery viewer
- Masonry layout with responsive design
- Filter by media type (Photos/Videos)
- Sort by name or date modified
- Fullscreen viewer with controls
- Video playback with loop functionality
- Folder management with recursive scanning
- Dark theme UI
