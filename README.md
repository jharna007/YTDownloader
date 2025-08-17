# YTDownloader

A simple Android app to download YouTube videos in MP4 or MP3 format using yt-dlp.

## Features

- Download YouTube videos in MP4 format
- Extract audio and save as MP3
- Material Design UI
- Support for Android 6+ (API 23+)
- Progress tracking and status updates

## Build Instructions

### Local Build

1. Clone this repository
2. Open in Android Studio or use command line
3. Replace stub binaries (see Binary Setup below)
4. Build using Gradle:

```bash
./gradlew assembleDebug
```

### CI Build

The project includes a GitHub Actions workflow that automatically builds the APK on push/PR to main branch.

## Binary Setup

**IMPORTANT**: This project includes stub executables for yt-dlp and ffmpeg that will allow the app to compile and run, but won't actually download videos. To make the app functional, you need to replace these with real ARM64 Android binaries.

### Replacing Stub Binaries

1. **Install Termux** on your Android device or use a Linux environment with ARM64 Android binaries
2. **Get yt-dlp binary**:
   - Termux: `pkg install yt-dlp`, then `which yt-dlp`
   - Or download ARM64 Android binary from: https://github.com/yt-dlp/yt-dlp/releases
3. **Get ffmpeg binary**:
   - Termux: `pkg install ffmpeg`, then `which ffmpeg`
   - Or use a pre-built ARM64 Android ffmpeg binary
4. **Replace the stubs**:
   - Copy the real yt-dlp binary to `app/src/main/assets/yt-dlp`
   - Copy the real ffmpeg binary to `app/src/main/assets/ffmpeg`
   - Ensure both files have executable permissions

### Why Stubs?

The real yt-dlp and ffmpeg binaries are quite large (20-50MB each) and would make this repository very heavy. The stubs allow the project to:
- Build successfully in CI
- Initialize without crashing
- Show clear error messages when download is attempted

## Download Locations

Downloaded files are saved to:

- **Android 10+**: `/Android/data/com.example.ytdownloader/files/Downloads/YTDownloader/`
- **Android 9 and below**: `/storage/emulated/0/Downloads/YTDownloader/`

## Permissions

The app requests the following permissions:

- `INTERNET` - Download videos from YouTube
- `ACCESS_NETWORK_STATE` - Check network connectivity
- `WRITE_EXTERNAL_STORAGE` (Android 9 and below) - Save files to Downloads
- `READ_EXTERNAL_STORAGE` - Read downloaded files
- `READ_MEDIA_VIDEO` (Android 13+) - Access video files
- `READ_MEDIA_AUDIO` (Android 13+) - Access audio files

## Usage

1. Launch the app
2. Paste a YouTube URL (supports youtube.com/watch, youtu.be, youtube.com/shorts)
3. Choose Download MP4 or Download MP3
4. Monitor progress in the status area
5. Find downloaded files in the Downloads/YTDownloader folder

## Technical Details

- **Language**: Kotlin
- **UI**: ViewBinding with Material Design
- **Min SDK**: 23 (Android 6.0)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle with Groovy DSL
- **Architecture**: Single Activity with coroutines for background tasks

## Troubleshooting

- **"Download failed"**: Make sure you've replaced the stub binaries with real ones
- **"Permission denied"**: Grant storage permissions when prompted
- **"Invalid URL"**: Only YouTube URLs are supported
- **App crashes on download**: Check that binaries are ARM64 compatible and executable

## Legal Notice

This app is for educational purposes. Users are responsible for complying with YouTube's Terms of Service and applicable copyright laws when downloading content.