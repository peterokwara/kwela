# Play Console: Data safety answers

Current draft answers for the closed-test build. Recheck this file and the Play Console form whenever data handling or providers change.

## Overview

| Question | Answer |
| --- | --- |
| Does the app collect or share required user data types? | Yes |
| Is all user data encrypted in transit? | Yes. The app uses HTTPS for API requests. |
| Account creation method | Email address and one-time code |
| Account deletion URL | `https://kwela-site.ojakaapeter711.workers.dev/delete-account` |
| Privacy policy URL | `https://kwela-site.ojakaapeter711.workers.dev/privacy` |
| Can users request deletion of individual data types without deleting their account? | No |

## Data types selected in Play Console

For this form, transfers to Cloudflare and OpenAI are treated as service-provider processing on Kwela's behalf. Google Play's definition excludes those transfers from "shared" data, while data sent off-device is still "collected". See [Google Play's Data safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en) and [OpenAI's API data controls](https://developers.openai.com/api/docs/guides/your-data).

| Play data type | Collected | Shared | Ephemeral | Required? | Purpose |
| --- | --- | --- | --- | --- | --- |
| Name | Yes | No | No | Optional | App functionality |
| Email address | Yes | No | No | Required | App functionality, account management |
| User IDs | Yes | No | No | Required | App functionality, account management |
| Address | Yes | No | No | Optional | App functionality |
| Phone number | Yes | No | No | Optional | App functionality |
| Other info (CV details) | Yes | No | No | Optional | App functionality |
| Other in-app messages | Yes | No | No | Optional | App functionality |
| Photos | Yes | No | No | Optional | App functionality |
| Files and docs | Yes | No | No | Optional | App functionality |
| App interactions | Yes | No | No | Required | App functionality, fraud prevention/security |
| Device or other IDs | Yes | No | No | Required | Fraud prevention/security |

## Handling notes

- The app stores chats and attachments in its local database. Kwela temporarily stores task requests, including chat context and attachments, in R2 until success or until failed inputs expire after 24 hours (cleanup every 15 minutes). Completed replies and generated documents are staged in D1 until the phone saves and acknowledges them, or account deletion. Minimal task records remain until account deletion. Workflow metadata is retained for 26 hours after an attempt finishes. OpenAI's API may retain abuse-monitoring logs containing prompts and responses for up to 30 days by default, with exceptions for legal or security needs.
- The structured CV profile is stored in D1 until account deletion. Account deletion removes the account, sessions, pending code, and CV profile. Usage records remain under a stable keyed opaque ID derived from the normalized email to enforce the experiment's spending limit, even if someone deletes and recreates an account.
- The account email and a keyed code hash are stored in D1. Codes expire after 10 minutes and are invalidated on use. Unused expired code rows may remain until replaced or account deletion.
- Sign-in rate-limit rows contain keyed hashes of email/IP values and counts, not raw values. The current implementation has no automatic cleanup job for those rows.
- Closed-test sign-in uses a reusable, non-expiring fixed code only for addresses in `AUTH_TEST_EMAILS`. A tester requests the code in the app, which displays it only after the Worker accepts the allow-listed address. No email is sent in this test mode. Remove `AUTH_TEST_CODE` and configure a verified email sender before public production.
- Not collected: precise or approximate location, contacts, calendar, health, financial information, audio, video, advertising ID, or analytics/crash diagnostics from an SDK.

## Other Play Console declarations still to complete

- Sign-in details for reviewers. Provide the allow-listed reviewer address and private-test code.
- Target audience and content. The intended audience is 18 and over.
- Save and submit the Data safety declaration for review after the remaining app-content declaration is complete.
- Closed-test release and reviewer access are staged separately; tester addresses must be added to the Worker allow-list before those people can sign in.
