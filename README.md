# Kwela Android experiment

Kwela is an independent Android experiment for helping students and early-career applicants prepare job applications. Milestone one is an offline-first Compose shell. It does not connect to an AI or backend yet.

## Try the app

Build and install the debug app with Android Studio or:

```sh
./gradlew installDebug
```

The app uses package `app.kwela` and supports Android 8.0 (API 26) and later. It uses the provided Material Theme Builder palette, system light/dark mode and wallpaper-based Material You colors on Android 12+, with Noto Sans for app text. The font is subset for Latin and extended Latin characters, including English and Swahili, under SIL Open Font License 1.1. The installable, minified APK is at `app/build/outputs/apk/release/app-release.apk` after `./gradlew assembleRelease`. It uses the local Android debug signing key for experiment sideloading. Use a private upload key before distributing beyond this experiment. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk` after `./gradlew assembleDebug`.

Sign in with any valid-looking email address and the demo OTP `123456`. No email is sent. Chat messages, task state, and files remain in local Room storage. Sign out preserves them; **Delete local demo data** removes them.

Chat supports Android Sharesheet text and links, CV PDF/DOCX selection, the four Chaji starting suggestions, and a sample task that finishes in about 20 seconds. The sample task creates clearly labeled example documents. Open Files to preview, share a PDF, or save a PDF through Android’s document picker. If you close Kwela during a running sample task, its status stays saved and the app resumes or completes it when you reopen Kwela. This milestone does not run the sample task in the background while the app stays closed.

The notification permission appears the first time you start a sample task. Settings can turn task notifications on or off. Notifications contain no CV or job details.

## Verify

```sh
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

The connected tests require an emulator or Android device with USB debugging enabled.
