# Kwela Android experiment

Kwela is an independent Android app for helping students and early-career applicants prepare job applications. Chat with Kwela, share a CV and a job, and get a tailored CV, cover letter and answers saved in Files. The assistant runs on its own Cloudflare Worker in [`backend/`](backend/README.md).

## Try the app

Build and install the debug app with Android Studio or:

```sh
./gradlew installDebug
```

The app uses package `app.kwela` and supports Android 8.0 (API 26) and later. It uses the provided Material Theme Builder palette, system light/dark mode and wallpaper-based Material You colors on Android 12+, with Noto Sans for app text. The font is subset for Latin and extended Latin characters, including English and Swahili, under SIL Open Font License 1.1. The installable, minified APK is at `app/build/outputs/apk/release/app-release.apk` after `./gradlew assembleRelease`. It uses the local Android debug signing key for experiment sideloading. Use a private upload key before distributing beyond this experiment. The debug APK is at `app/build/outputs/apk/debug/app-debug.apk` after `./gradlew assembleDebug`.

Sign in with an email address and the 6-digit code `123456`. No email is sent yet. Chats and files are kept in local Room storage on the phone. Sign out keeps them; **Delete account and data** in Settings removes them and the saved CV profile on the server.

Chat supports Android Sharesheet text and links and one attached file per message (PDF, Word, text or a photo, up to 5 MB). Kwela replies from the API; when it prepares an application, the CV, cover letter and answers are saved in Files, where you can preview, share a PDF or save a PDF. Debug builds add **Run test task** in Settings for trying the review flow.

Set `kwela.apiUrl` in `gradle.properties` to the deployed Worker address. Without it the assistant is unavailable and the app says so.

## Verify

```sh
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

The connected tests require an emulator or Android device with USB debugging enabled.
