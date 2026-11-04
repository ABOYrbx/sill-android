# Sill for Android

Android client for Sill. The Mac side (`Sources/`) and the shared protocol
(`Sources/StreamProtocol/`) are unchanged; this module ports the device side.

## Build

Requirements: JDK 17+, Android SDK (API 26+), and Gradle (wrapper included).

```
cd android
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Run

Open the `android/` folder in Android Studio, or install the debug APK on a
device/emulator on the same network as a Mac running Sill.

## Modules

- `app` — application entry point and navigation.
- `protocol` — Kotlin port of `Sources/StreamProtocol/`.
- `network` — discovery, sockets, TLS.
- `decode` — HEVC decoding.
- `session` — connection/session state.
- `ui` — Jetpack Compose screens.