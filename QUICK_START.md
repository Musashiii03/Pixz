# Quick Start - Build Your Executable

## ✅ Easiest Option: Standalone Package

**Run this command:**
```cmd
build-standalone.bat
```

**What you get:**
- Location: `target\standalone\`
- Files: `WinGallery.jar` + `WinGallery.bat`
- Size: ~5-10 MB
- To run: Double-click `WinGallery.bat`

**Requirements:**
- Java must be installed on the PC where you run it

**Distribution:**
1. Zip the `target\standalone\` folder
2. Send to anyone
3. They unzip and double-click `WinGallery.bat`

---

## 🚀 Best Option: Portable with Bundled Java

**Run this command:**
```cmd
build-portable.bat
```

**What you get:**
- Location: `target\portable\WinGallery\`
- Size: ~100 MB (includes Java runtime)
- To run: Double-click `WinGallery.bat`

**Requirements:**
- None! Java is included

**Distribution:**
1. Zip the `target\portable\` folder
2. Send to anyone
3. They unzip and double-click `WinGallery.bat`
4. Works on any Windows PC without Java installed

---

## 🎯 Professional Option: Windows Installer

**Requirements:**
- Install WiX Toolset first (see INSTALL_WIX.md)

**Run this command:**
```cmd
build-exe.bat
```

**What you get:**
- Location: `target\dist\WinGallery-1.0.0.exe`
- Professional installer with Start Menu shortcuts
- Uninstaller included

---

## Which Should You Choose?

| Option | Best For | Java Required? | Size |
|--------|----------|----------------|------|
| **Standalone** | Quick testing, developers | Yes | 5-10 MB |
| **Portable** | Distribution to users | No | ~100 MB |
| **Installer** | Professional distribution | No | ~100 MB |

## Recommendation

1. **For yourself/testing:** Use `build-standalone.bat`
2. **For sharing with others:** Use `build-portable.bat`
3. **For professional release:** Use `build-exe.bat` (after installing WiX)

---

## Current Issue Fix

The error you saw ("Windows cannot find 'app.exe'") is because the portable script had a bug. 

**Solution:** Use `build-standalone.bat` instead - it's simpler and works immediately!

```cmd
build-standalone.bat
```

Then run:
```cmd
target\standalone\WinGallery.bat
```
