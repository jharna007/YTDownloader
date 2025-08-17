# YTDownloader

An Android app for downloading YouTube videos and audio using yt-dlp and ffmpeg.

## Features

- Download YouTube videos in MP4 format
- Extract audio from YouTube videos in MP3 format  
- Simple and clean UI
- Works offline after initial setup
- Supports Android 6.0+ (API 23+)

## Setup

### Prerequisites

This project requires the actual yt-dlp and ffmpeg binaries for Android. The current binaries in `app/src/main/assets/` are placeholders.

#### Getting Real Binaries

1. **yt-dlp**: Download the Linux ARM64 version from [yt-dlp releases](https://github.com/yt-dlp/yt-dlp/releases)
2. **ffmpeg**: Download from [FFmpeg Kit](https://github.com/arthenica/ffmpeg-kit/releases) or compile from source

#### Replacing Placeholder Files

Replace these files with the actual binaries:
- `app/src/main/assets/yt-dlp` 
- `app/src/main/assets/ffmpeg`

Make sure the binaries are:
- Compiled for Android ARM64/ARM architectures
- Executable on Android systems
- Include all required dependencies

### Building

1. Open the project in Android Studio
2. Sync project with Gradle files
3. Build and run on device or emulator

Or build from command line:
```bash
./gradlew assembleDebug
```

### GitHub Actions

The project includes a GitHub Actions workflow that automatically builds APKs on push/PR.

## Permissions

The app requests these permissions:
- `INTERNET` - For downloading videos
- `WRITE_EXTERNAL_STORAGE` - For saving files (Android 9 and below)
- `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` - For accessing downloads (Android 13+)

## Technical Details

- **Target SDK**: 34 (Android 14)
- **Minimum SDK**: 23 (Android 6.0)
- **Language**: Kotlin
- **Build System**: Gradle
- **Architecture**: Uses native binaries via ProcessBuilder

## Limitations

- Requires actual yt-dlp and ffmpeg binaries (not included due to size)
- May not work on all Android device architectures
- Downloads limited by YouTube's terms of service

## Legal Notice

This app is for educational purposes. Users are responsible for complying with YouTube's Terms of Service and applicable copyright laws.

## License

This project is provided as-is for educational purposes.
