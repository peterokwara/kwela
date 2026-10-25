# Kwela 1.0.2 verification

Release package: `app.kwela`, version name `1.0.2`, version code `6`.

## Automated checks

- Backend: 90 tests passed, including real SQLite tests for concurrent duplicate submissions, owner isolation, one explicit retry per retry key, failed-input expiry, output recovery after days offline, acknowledgment, and deletion.
- Android: 112 unit tests passed.
- Emulator: six instrumentation tests passed on the Pixel 7 AVD, Android 15 / API 35. Coverage includes login validation, navigation, sharing before/after sign-in, sending while work is pending, clearing a consumed shared draft, Room persistence/migrations, and account isolation.
- Release compilation, R8 optimization, lint vital, APK signing, and AAB signing passed.
- Site: four tests passed; Svelte check and autofixer reported no issues.

## Live verification

- The deployed API accepted a 19-message synthetic application conversation and compacted its first seven turns, retaining twelve full turns.
- Concurrent duplicate submissions returned the same run ID and used one Workflow execution attempt.
- The API generated a CV and cover letter for an Administrative Assistant internship in 45 seconds, including a 53,270-byte PDF with a valid PDF header.
- A signed release on the emulator accepted an Android share intent and queued messages.
- The app was force-stopped during work. Cloudflare completed the run and retained its unacknowledged output with the input deleted.
- The app reopened with Wi-Fi and mobile data disabled. WorkManager retried while offline, then delivered the result after connectivity returned and proceeded to the queued follow-up. Both runs completed with one execution attempt and cleared the temporary server output after delivery.
- The recovered CV appeared in Files under Resumes, with Download and Share actions.

## Deployed resources

- API: https://kwela-api.ojakaapeter711.workers.dev
- API deployment version: `34921628-3e56-4a8f-a707-7171c4148f65`
- Website/privacy deployment version: `dbe84c37-44d2-4faf-8845-67a2b1b55611`
- R2 bucket: `kwela-run-inputs`
- D1 migration: `0005_durable_chat_runs.sql`, applied remotely.
- Workflow: `kwela-chat-runs`; input cleanup runs every 15 minutes. Failed input expires after 24 hours. Completed output stays available until acknowledged by the phone or account deletion.

## Google Play submission

Submitted to the existing Alpha closed-test track on October 25, 2026, at 100% rollout for selected testers. Publishing overview confirms **Changes in review**. The quick-check panel cleared, and a fresh page load still confirms **Changes in review**. Managed publishing is off, so approval permits automatic publication. Version 1.0.1 remains available until then. Only version code 6 is included; superseded draft bundles are excluded. Play estimates a 2.11 MB download and reports no blocking errors.

## Artifacts

Immutable copies are in `build/releases/1.0.2/`; checksums are in that directory's `manifest.json`. Local verification screenshots and the synthetic PDF are in `build/reports/release-1.0.2/`.

The APK is 2,359,293 bytes. The AAB is 4,950,853 bytes.

## Limits

The physical Pixel 7 was unavailable. Verification used the emulator, automated tests, and the live API. Closed-test sign-in remains the approved allow-listed private-code mode. Google Play controls approval and availability after submission. Play may display a non-blocking warning about native debug symbols from packaged native libraries; the AAB includes the ReTrace mapping file.
