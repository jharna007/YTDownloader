# YTDownloader Project Summary

## Core Components

### Application
- **App Name**: YTDownloader
- **Package**: com.example.ytdownloader
- **Language**: Kotlin
- **UI Framework**: ViewBinding + Material Design Components

### Build Configuration
- **Build System**: Gradle with Groovy DSL
- **Android Gradle Plugin**: 8.1.2
- **Kotlin Version**: 1.9.10
- **Gradle Version**: 8.6
- **Min SDK**: 23 (Android 6.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

### Dependencies
- androidx.core:core-ktx:1.10.1
- androidx.appcompat:appcompat:1.6.1
- com.google.android.material:material:1.9.0
- androidx.constraintlayout:constraintlayout:2.1.4
- androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0
- androidx.lifecycle:lifecycle-livedata-ktx:2.7.0

### Key Features
1. **Video Download**: MP4 format using yt-dlp
2. **Audio Extraction**: MP3 format using yt-dlp with --extract-audio
3. **Progress Tracking**: Real-time status updates and progress bar
4. **Permission Management**: Dynamic permissions for different Android versions
5. **Storage Handling**: Scoped storage for Android 10+ and legacy for older versions

### File Structure
```
YTDownloader/
├── app/
│   ├── build.gradle (Groovy DSL)
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/ytdownloader/MainActivity.kt
│   │   ├── res/ (layouts, values, xml configs)
│   │   └── assets/ (yt-dlp, ffmpeg stubs)
│   └── proguard-rules.pro
├── gradle/wrapper/ (Gradle 8.6 wrapper)
├── .github/workflows/main.yml (CI/CD)
├── build.gradle (root, Groovy DSL)
├── settings.gradle (repository configuration)
└── README.md
```

### Binary Dependencies
- **yt-dlp**: YouTube video/audio downloader (stub included, real binary required)
- **ffmpeg**: Media processing tool (stub included, real binary required)

### Permissions
- INTERNET, ACCESS_NETWORK_STATE
- WRITE_EXTERNAL_STORAGE (maxSdkVersion=28)
- READ_EXTERNAL_STORAGE
- READ_MEDIA_VIDEO, READ_MEDIA_AUDIO (Android 13+)

### CI/CD
- GitHub Actions workflow for automated building
- Artifact upload of debug APK
- Java 17 + Android SDK setup
- Gradle wrapper execution with caching