# Installing WiX Toolset (Optional - for .exe installer)

## What is WiX?
WiX Toolset is required to create Windows `.exe` installers. However, you have alternatives:
- **Portable version** (no WiX needed) - Use `build-portable.bat`
- **JAR file** (no WiX needed) - Use `build-jar.bat`

## If You Want the .exe Installer

### Option 1: Install WiX 3.x (Recommended)

1. **Download WiX 3.14:**
   - Go to: https://github.com/wixtoolset/wix3/releases
   - Download: `wix314.exe` (latest 3.x version)

2. **Install:**
   - Run the installer
   - Default installation path: `C:\Program Files (x86)\WiX Toolset v3.14\`

3. **Add to PATH:**
   - Press `Win + X` → System → Advanced system settings
   - Click "Environment Variables"
   - Under "System variables", find "Path" and click "Edit"
   - Click "New" and add: `C:\Program Files (x86)\WiX Toolset v3.14\bin`
   - Click OK on all dialogs
   - **Restart Command Prompt**

4. **Verify:**
   ```cmd
   candle.exe -?
   ```
   Should show WiX help.

5. **Build installer:**
   ```cmd
   build-exe.bat
   ```

### Option 2: Use Chocolatey (Package Manager)

If you have Chocolatey installed:
```cmd
choco install wixtoolset
```

### Option 3: Use winget (Windows Package Manager)

If you have winget (Windows 10/11):
```cmd
winget install WiXToolset.WiX
```

## Easier Alternatives (No WiX Required)

### 1. Portable Version (Recommended)
```cmd
build-portable.bat
```
Creates a folder you can copy to any PC and run without installation.

### 2. JAR File
```cmd
build-jar.bat
```
Creates a JAR file that runs on any PC with Java installed.

## Comparison

| Method | File Size | Requires Java | Installation | Best For |
|--------|-----------|---------------|--------------|----------|
| **EXE Installer** | ~100MB | No | Yes | Distribution to end users |
| **Portable** | ~100MB | No | No | Quick deployment, USB drives |
| **JAR** | ~5MB | Yes | No | Developers, testing |

## Recommendation

For most users, **build-portable.bat** is the best option:
- ✅ No WiX installation needed
- ✅ No Java required on target PC
- ✅ Easy to distribute (just zip the folder)
- ✅ No installation needed
- ✅ Can run from USB drive

Only use the EXE installer if you need:
- Start menu integration
- Desktop shortcuts
- Uninstaller
- Professional distribution
