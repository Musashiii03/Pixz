# Creating a GitHub Release

## Option 1: Automated Release (Recommended)

The project includes a GitHub Actions workflow that automatically builds and creates releases when you push a tag.

### Steps:

1. **Commit and push your changes:**
   ```bash
   git add .
   git commit -m "Prepare for release v1.0.0"
   git push
   ```

2. **Create and push a version tag:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. **GitHub Actions will automatically:**
   - Build the application
   - Create the installer
   - Create a GitHub release
   - Upload the installer as a release asset

4. **Check your releases:**
   - Go to your GitHub repository
   - Click on "Releases" in the right sidebar
   - Your new release should appear with the installer attached

## Option 2: Manual Release

If you prefer to build and upload manually:

### Build the Installer:

1. **Run the build script:**
   ```cmd
   build-installer.bat
   ```

2. **Find your installer at:**
   ```
   target\installer\WinGallery-Setup-1.0.0.exe
   ```

### Create GitHub Release:

1. **Go to your GitHub repository**

2. **Click "Releases" → "Create a new release"**

3. **Fill in the details:**
   - Tag: `v1.0.0`
   - Title: `WinGallery v1.0.0`
   - Description: Add release notes (features, bug fixes, etc.)

4. **Upload the installer:**
   - Drag and drop `WinGallery-Setup-1.0.0.exe`

5. **Click "Publish release"**

## Before Your First Release

1. **Update the GitHub URL in setup.iss:**
   ```iss
   #define MyAppURL "https://github.com/YOUR-USERNAME/WinGallery"
   ```

2. **Update version numbers if needed:**
   - `pom.xml` - `<version>1.0.0</version>`
   - `setup.iss` - `#define MyAppVersion "1.0.0"`

3. **Test the installer locally:**
   - Run `build-installer.bat`
   - Install the generated exe on a test machine
   - Verify everything works

## Release Checklist

- [ ] All features tested and working
- [ ] Version numbers updated in pom.xml and setup.iss
- [ ] GitHub URL updated in setup.iss
- [ ] LICENSE.txt exists
- [ ] README.md is up to date
- [ ] Build script runs successfully
- [ ] Installer tested on clean Windows machine
- [ ] Release notes prepared

## Versioning

Follow semantic versioning (MAJOR.MINOR.PATCH):
- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes

Examples:
- `v1.0.0` - Initial release
- `v1.1.0` - Added new features
- `v1.1.1` - Bug fixes
- `v2.0.0` - Major changes

## Troubleshooting

### GitHub Actions fails
- Check the Actions tab for error logs
- Ensure secrets are configured if needed
- Verify the workflow file syntax

### Installer doesn't work
- Test on a clean Windows machine without Java
- Check that jpackage created the app-image correctly
- Verify all files are included in the installer

### Can't push tags
```bash
git push --tags
```

## What Users Will Download

Users will download `WinGallery-Setup-1.0.0.exe` which:
- Is a single executable installer
- Includes bundled Java runtime (no Java installation needed)
- Creates Start Menu shortcuts
- Optionally creates Desktop shortcut
- Can be uninstalled via Windows Settings

## File Size

The installer will be approximately 60-80 MB because it includes:
- Your application
- JavaFX runtime
- Bundled JRE
