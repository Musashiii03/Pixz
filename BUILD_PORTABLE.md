# Building Pixz Portable ZIP

This guide explains how to build a portable ZIP distribution of Pixz.

## Prerequisites

- **Java JDK 21 or higher** installed and configured
- **Maven** (included via Maven Wrapper - `mvnw.cmd`)
- **jpackage** tool (included with JDK 14+)

## Build Process

### Step 1: Update Version (if needed)

Update the version number in `pom.xml`:

```xml
<groupId>com.example</groupId>
<artifactId>Pixz</artifactId>
<version>1.3.0</version>  <!-- Update this -->
<name>Pixz</name>
```

Also update the version in `build-portable-simple.bat`:

```bat
jpackage --type app-image --name Pixz --app-version 1.3.0 ...
```

### Step 2: Clean Previous Builds (if needed)

If you encounter locked files, manually delete the `target` directory:

```powershell
Remove-Item -Path "target" -Recurse -Force
```

### Step 3: Run the Build Script

Execute the simplified build script:

```cmd
build-portable-simple.bat
```

This script will:
1. Check Java version
2. Build the shaded JAR with all dependencies (`mvnw clean package`)
3. Create a portable application package using `jpackage`

### Step 4: Create the ZIP File

After successful build, create the ZIP file:

```powershell
Compress-Archive -Path "target\portable\Pixz" -DestinationPath "Pixz-Portable-v1.3.0.zip" -Force
```

## Build Output

- **Portable Application**: `target\portable\Pixz\`
- **Executable**: `target\portable\Pixz\Pixz.exe`
- **ZIP Distribution**: `Pixz-Portable-v1.3.0.zip` (in root directory)

## Troubleshooting

### Issue: "Failed to delete target/portable/Pixz/Pixz.exe"

**Cause**: The application is still running or the file is locked.

**Solution**:
1. Close any running Pixz instances
2. Kill the process: `taskkill /F /IM Pixz.exe`
3. Manually delete the `target` directory
4. Run the build script again

### Issue: "jlink failed - automatic module cannot be used"

**Cause**: The full `build-portable.bat` script uses jlink which doesn't support automatic modules.

**Solution**: Use `build-portable-simple.bat` instead, which skips jlink and uses jpackage directly.

### Issue: "jpackage failed - main jar does not exist"

**Cause**: Version mismatch between `pom.xml` and build script.

**Solution**: Ensure the version in `pom.xml` matches the version in `build-portable-simple.bat`.

## Distribution

The generated ZIP file contains:
- `Pixz.exe` - Main executable
- `app/` - Application JAR and dependencies
- `runtime/` - Bundled Java runtime (JRE)

Users can extract and run `Pixz.exe` directly without installing Java.

## Build Scripts

- **`build-portable-simple.bat`** - Recommended for creating portable distributions
- **`build-portable.bat`** - Advanced build with jlink (may fail with automatic modules)
- **`build-portable-direct.bat`** - Alternative build method

## Notes

- The portable build includes a bundled Java runtime (~60-65 MB)
- No installation required - fully portable
- Works on Windows 10/11 (64-bit)
