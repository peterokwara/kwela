# Kwela closed test runbook

## Before inviting testers

1. The closed-test track is active, and **Kwela closed test 1.0** is available to selected testers.
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

- Play Console submission: approved and published; closed-testing track **Alpha** is active.
- Active test release: **Kwela closed test 1.0**, available to selected testers.
- Android opt-in link: <https://play.google.com/store/apps/details?id=app.kwela>
- Selected Play email list: **Kwela early access**, currently containing the owner account and `cliff.gor.dev@gmail.com`.
- Tester opt-ins: 0 currently.
- Backend test-login allowlist: `ojakaapeter@gmail.com` and `cliff.gor.dev@gmail.com`.
- Authentication mode: private fixed code; no custom domain, email service, or email delivery.
- Verified on the Android emulator: requesting a code for the allow-listed account, signing in, and restoring the session after force-stop and reopen.
- Production price: KES 100.
