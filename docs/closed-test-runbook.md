# Kwela closed test runbook

## Before inviting testers

1. The Alpha closed-test track is active. **Kwela closed test 1.0.2** (version code 6) has been submitted for review. Version 1.0.1 is currently available to selected testers until the update is approved.
2. Add each tester's Google account email to **Kwela early access** in Play Console and to `AUTH_TEST_EMAILS` in `backend/wrangler.jsonc`. Deploy `kwela-api` and confirm the address can request a code before inviting the tester.
3. Share the Android opt-in link: <https://play.google.com/store/apps/details?id=app.kwela>. Only accounts in the selected tester list can access the test.
4. For production access, Play Console requires at least 12 testers to opt in and stay opted in for 14 days. You can start gathering feedback with the first testers before collecting all 12.
5. Keep the Play Console production price at KES 100. The closed-test release is restricted to the selected test group.

No custom domain or email provider is configured for this experiment. The current login is a private test mode: an allow-listed tester requests a code in Kwela and sees the six-digit code in the app. No email is sent. Remove `AUTH_TEST_CODE` and replace this mode with a verified email sender before public launch.

## Tester instructions

After the tester's email is on both allowlists:

1. Open the opt-in link while signed into the Google account added to **Kwela early access**.
2. Join the test, then follow Google Play's link to install Kwela.
3. In Kwela, enter the same email address and tap **Continue**.
4. Enter the six-digit private test code shown in the app.
5. Try a real job advert, review the generated documents, and send feedback to `ojakaapeter@gmail.com`.
6. Keep the account opted in for the full 14-day test period.

## Current state

- Play Console submission: **Kwela closed test 1.0.2**, version code **6**, submitted on October 25, 2026 with a 100% rollout to the existing **Alpha** closed test. Publishing overview shows **Changes in review**; Google quick checks are running before the review proceeds. Managed publishing is off.
- Currently available test release: **Kwela closed test 1.0.1**. Version 1.0.2 becomes available after Google approves it.
- Android opt-in link: <https://play.google.com/store/apps/details?id=app.kwela>
- Selected Play email list: **Kwela early access**, currently containing the owner account and `cliff.gor.dev@gmail.com`.
- Tester opt-ins: 0 currently.
- Backend test-login allowlist: `ojakaapeter@gmail.com` and `cliff.gor.dev@gmail.com`.
- Authentication mode: private fixed code; no custom domain, email service, or email delivery.
- Verified on the Android emulator: private-code sign-in, sharing, sending while busy, force-stop and offline recovery, queued follow-ups, and generated CV retrieval from Files. See [1.0.2 verification](release-1.0.2.md).
- Production price: KES 100.
