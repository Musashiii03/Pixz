# Quick JDK Setup Guide for Windows

## The Problem
You're getting this error:
```
No compiler is provided in this environment. Perhaps you are running on a JRE rather than a JDK?
```

This means you need to install the **JDK (Java Development Kit)**, not just the JRE (Java Runtime Environment).

## Quick Fix (5 minutes)

### Step 1: Download JDK
1. Go to: https://adoptium.net/temurin/releases/
2. Select:
   - **Operating System:** Windows
   - **Architecture:** x64
   - **Package Type:** JDK (not JRE!)
   - **Version:** 21 (LTS)
3. Download the `.msi` installer
4. Run the installer and follow the prompts

### Step 2: Set JAVA_HOME
After installation:

**Option A: Using GUI (Permanent)**
1. Press `Win + X` and select "System"
2. Click "Advanced system settings"
3. Click "Environment Variables"
4. Under "System variables", click "New"
5. Set:
   - Variable name: `JAVA_HOME`
   - Variable value: `C:\Program Files\Eclipse Adoptium\jdk-21.0.x` (check actual path)
6. Find "Path" in System variables, click "Edit"
7. Click "New" and add: `%JAVA_HOME%\bin`
8. Click OK on all dialogs
9. **Restart your Command Prompt**

**Option B: Using Command (Temporary)**
Open Command Prompt and run:
```cmd
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.x
set PATH=%JAVA_HOME%\bin;%PATH%
```
(Replace the path with your actual JDK installation path)

### Step 3: Verify Installation
Run this script to check:
```cmd
check-java.bat
```

Or manually verify:
```cmd
javac -version
```

You should see something like: `javac 21.0.x`

### Step 4: Build Your App
Now you can run:
```cmd
build-jar.bat
```

## Alternative JDK Sources

If you prefer other JDK distributions:
- **Oracle JDK:** https://www.oracle.com/java/technologies/downloads/
- **Microsoft OpenJDK:** https://learn.microsoft.com/en-us/java/openjdk/download
- **Amazon Corretto:** https://aws.amazon.com/corretto/

## Still Having Issues?

Run the diagnostic script:
```cmd
check-java.bat
```

This will tell you exactly what's wrong with your Java setup.
