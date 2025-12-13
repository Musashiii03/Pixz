# WinGallery

A modern photo and video gallery application for Windows with a beautiful masonry layout.

## Features

- 📸 **Photo & Video Support** - View images (JPG, PNG, GIF, BMP) and videos (MP4, AVI, MOV, MKV)
- 🎨 **Masonry Layout** - Pinterest-style responsive grid
- 🔍 **Search & Filter** - Search by filename, filter by media type
- 📊 **Sort Options** - Sort by name or date modified
- 🎬 **Video Player** - Built-in player with controls, loop, and fullscreen
- 📁 **Folder Management** - Add multiple folders, recursive subfolder scanning
- 🌙 **Dark Theme** - Modern dark UI with custom title bar
- ⌨️ **Keyboard Shortcuts** - Arrow keys for navigation, spacebar for play/pause, F11 for fullscreen

## Quick Start

### First Time Setup

**You need JDK (not JRE) to build this app!**

1. Run the diagnostic:
   ```cmd
   check-java.bat
   ```

2. If you don't have JDK, follow: **[SETUP_JDK.md](SETUP_JDK.md)**

### Building the Application

**Option 1: Portable Version (Recommended - No Java Required)**
```cmd
build-portable.bat
```
Creates: `target\portable\WinGallery\`

Run with: Double-click `target\portable\WinGallery.bat`

✅ No Java needed on target PC
✅ No installation required
✅ Copy folder to any Windows PC and run
✅ Perfect for USB drives

**Option 2: JAR File (Quick & Easy)**
```cmd
build-jar.bat
```
Creates: `target\WinGallery-1.0-SNAPSHOT.jar`

Run with:
```cmd
java -jar target\WinGallery-1.0-SNAPSHOT.jar
```

**Option 3: Windows Installer (Requires WiX Toolset)**
```cmd
build-exe.bat
```
Creates: `target\dist\WinGallery-1.0.0.exe`

⚠️ Requires WiX Toolset installation - See [INSTALL_WIX.md](INSTALL_WIX.md)

### Running from Source
```cmd
mvnw.cmd javafx:run
```

## Requirements

### For Building:
- JDK 21 or higher
- Maven (included via wrapper)

### For Running (JAR):
- Java Runtime 21 or higher

### For Running (EXE):
- No requirements! The installer includes everything.

## Usage

1. **Add Folders** - Click "+ Add Folder" in the sidebar
2. **Browse Media** - Scroll through your photos and videos in masonry layout
3. **Filter** - Use "All", "Photos", or "Videos" buttons
4. **Sort** - Choose "Name" or "Date Modified" from dropdown
5. **Search** - Type in the search bar to find specific files
6. **View** - Click any media to open fullscreen viewer
7. **Navigate** - Use arrow keys or on-screen buttons
8. **Video Controls** - Play/pause, seek, volume, loop, rotate

## Keyboard Shortcuts

- **Arrow Keys** - Navigate between media
- **Spacebar** - Play/pause video
- **F / F11** - Toggle fullscreen
- **ESC** - Close viewer

## Project Structure

```
WinGallery/
├── src/main/java/com/example/wingallery/
│   ├── HelloApplication.java    - Main application
│   ├── HelloController.java     - UI controller
│   ├── Launcher.java            - Entry point
│   ├── MasonryPane.java         - Custom layout
│   ├── MediaItem.java           - Media model
│   ├── MediaPopup.java          - Popup viewer
│   └── ThumbnailGenerator.java  - Thumbnail creation
├── src/main/resources/
│   └── com/example/wingallery/
│       ├── hello-view.fxml      - UI layout
│       └── dark-theme.css       - Styling
├── build-jar.bat                - Build JAR script
├── build-exe.bat                - Build EXE script
├── check-java.bat               - Java diagnostic
├── BUILD_INSTRUCTIONS.md        - Detailed build guide
└── SETUP_JDK.md                 - JDK setup guide
```

## Troubleshooting

See **[BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md)** for detailed troubleshooting.

Common issues:
- **"No compiler provided"** → You need JDK, not JRE. See [SETUP_JDK.md](SETUP_JDK.md)
- **"JAVA_HOME not found"** → Set JAVA_HOME environment variable
- **Video thumbnails not showing** → Install ffmpeg (optional but recommended)

## Optional: FFmpeg for Better Video Thumbnails

For better video thumbnail generation, install ffmpeg:
1. Download from: https://www.gyan.dev/ffmpeg/builds/
2. Extract and add to PATH
3. Restart the application

## License

This project is open source and available for personal and commercial use.

## Support

For issues or questions, check the troubleshooting guides:
- [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md)
- [SETUP_JDK.md](SETUP_JDK.md)
