# Creating Your First GitHub Release

## Quick Steps:

### 1. Commit Your Current Code

```bash
git add .
git commit -m "Prepare v1.0.0 release"
git push
```

### 2. Create a Tag

```bash
git tag v1.0.0
git push origin v1.0.0
```

### 3. Create the Release on GitHub

1. Go to your repository: https://github.com/Musashiiii03/WinGallery

2. Click on **"Releases"** (right sidebar)

3. Click **"Create a new release"**

4. Fill in the form:
   - **Choose a tag:** Select `v1.0.0` (the tag you just pushed)
   - **Release title:** `WinGallery v1.0.0`
   - **Description:** Copy and paste from `GITHUB_RELEASE_NOTES.md`

5. **Attach the file:**
   - Drag and drop: `target\WinGallery-1.0.0-Windows-Portable.zip`
   - Or click "Attach binaries by dropping them here or selecting them"

6. Click **"Publish release"**

## Done! 🎉

Your release is now live at:
https://github.com/Musashiiii03/WinGallery/releases/tag/v1.0.0

Users can download the ZIP file and run your app!

---

## Alternative: Create Release Without Git Tag

If you don't want to use git tags:

1. Go to: https://github.com/Musashiiii03/WinGallery/releases

2. Click **"Create a new release"**

3. In "Choose a tag" field, type: `v1.0.0` (it will create the tag for you)

4. Fill in title and description

5. Upload the ZIP file

6. Click **"Publish release"**

---

## What You're Uploading:

- **File:** `target\WinGallery-1.0.0-Windows-Portable.zip`
- **Size:** 32 MB
- **Contents:** Complete app with bundled Java runtime
- **User experience:** Download → Extract → Run WinGallery.exe

No installation wizard needed - it's a portable app!
