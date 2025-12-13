# How to Upload to GitHub Release

## You Already Have:

✅ **WinGallery-1.0.0-Windows-Portable.zip** (32 MB)
   - Location: `target\WinGallery-1.0.0-Windows-Portable.zip`
   - Ready to upload!

## Steps to Update Your Release:

### 1. Go to Your Release Page
https://github.com/Musashiiii03/WinGallery/releases/tag/v1.0.0

### 2. Edit the Release
- Click the **"Edit release"** button (pencil icon)

### 3. Update the Description
- Copy the content from `GITHUB_RELEASE_NOTES.md`
- Paste it into the release description

### 4. Upload the File
- Scroll down to the "Assets" section
- Drag and drop: `target\WinGallery-1.0.0-Windows-Portable.zip`
- Or click "Attach binaries" and select the file

### 5. Publish
- Click **"Update release"**

## What Users Will Get:

When users download your release, they will get:
- A 32 MB ZIP file
- No installation needed
- No Java required (bundled)
- Just extract and run WinGallery.exe

## File Structure Inside ZIP:

```
WinGallery/
├── WinGallery.exe          ← Users run this
├── app/                    ← Bundled Java runtime
│   ├── bin/
│   ├── conf/
│   └── lib/
└── runtime/
```

## For Future Releases:

### To build a new version:

1. Update version in `pom.xml`:
   ```xml
   <version>1.0.1</version>
   <appVersion>1.0.1</appVersion>
   ```

2. Run the build script:
   ```cmd
   build-github-release.bat
   ```

3. Create a new tag:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```

4. Upload the new ZIP to the new release

## Quick Build Command:

If you just want to rebuild without the script:

```cmd
mvnw.cmd clean package -DskipTests
mvnw.cmd javafx:jlink
mvnw.cmd jpackage:jpackage@portable
```

Then create ZIP manually from `target\portable\WinGallery\`
