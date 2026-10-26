# Analytics

Product analytics run on PostHog, in its own project **Kwela** (US cloud, project id 689764), separate from Careergo. Events go straight to `https://us.i.posthog.com`. There is no reverse proxy. The project key is a public client key and sits in `gradle.properties` (`kwela.posthogApiKey`) and `backend/wrangler.jsonc` (`POSTHOG_API_KEY`). Leave either empty to turn analytics off.

## Rules we follow (PostHog's best practices)

- **Growth events first.** Sign-up, sign-in and the main action (an application written) are explicit events, not autocapture.
- **Backend first.** Anything that matters for the numbers is sent by the server, where an ad blocker or a closed app cannot lose it. The phone sends only taps.
- **One naming convention.** `category:object_action`, lowercase snake case, present-tense verbs: click, view, create, update, delete, send, resend, fail, generate, start, end, open, download, share, render, sign_in. Names are fixed strings. Variables go in properties. Tests check this on both sides (`backend/tests/analytics.test.mjs`, `AnalyticsEventsTest`).
- **Careful distinct ids.** The id is the server's opaque account id (a keyed hash of the email for newer accounts, a random id for older ones). It is the same on the phone and the server, and it is never an email. The server returns it at sign-in as `analyticsId`.
- **Internal users are tagged.** Every event carries `is_internal_user`. It is true for the team and testers (`AUTH_TEST_EMAILS`) and for debug builds. The project setting "Filter out internal and test users" excludes `is_internal_user = true` and is on by default.
- **No content.** Events never carry a CV, message, file name, name or email.
- **The person can opt out.** Settings has a "Usage statistics" switch. It is on by default.

## Events

| Event | Sent by | When | Properties |
| --- | --- | --- | --- |
| `account:user_create` | server | First sign-in of a new account | `sign_in_method` |
| `account:user_sign_in` | server | Any sign-in | `sign_in_method` (`google` or `email_code`) |
| `account:user_delete` | server | Account deleted | none |
| `chat:application_generate` | server | A CV, letter or answers were written | `has_resume`, `has_cover_letter`, `answer_count`, `duration_ms`, `attempt_count` |
| `chat:run_fail` | server | A request ended in failure | `failure_kind`, `duration_ms`, `attempt_count` |
| `document:pdf_render` | server | A full-design PDF was made | `document_type` |
| `app:session_start` | phone | The app was opened | none |
| `login_flow:google_button_click` | phone | Continue with Google tapped | none |
| `login_flow:email_button_click` | phone | Use email instead tapped | none |
| `chat:message_send` | phone | A message was sent | `has_attachment` |
| `chat:message_resend` | phone | Resend tapped | none |
| `chat:message_delete` | phone | A message was deleted | none |
| `document:file_open` | phone | A file was opened | `document_type` |
| `document:pdf_download` | phone | Download tapped | `document_type` |
| `document:pdf_share` | phone | Share tapped | `document_type` |

Add a new event in `backend/src/lib/server/services/analyticsEvents.ts` or `app/.../analytics/AnalyticsEvents.kt`, follow the convention, and add it to the table above and to the privacy page if it collects anything new.
