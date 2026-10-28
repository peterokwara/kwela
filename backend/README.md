# Kwela API

The Cloudflare Worker behind the Kwela Android app. It is its own project with its own D1 database, secrets and deployment. It shares nothing with any other Worker.

The agent code (career partner, CV extraction, job research, application drafting), spending limits and Careergo document shapes were copied from Chaji and renamed. Kwela chats and their compacted conversation memory live on the phone. The Worker uses that memory for a reply but does not store it after the task ends. A durable run keeps its request in R2 while it works, deletes the request on success, and deletes a failed request within 24 hours. The reply stays in D1 until the app confirms it saved the documents or the account is deleted.

## Routes

People sign in with their email address and a 6-digit code (no passwords). `POST /auth/verify` returns a session token. Every `/api/*` route needs `Authorization: Bearer <session token>`. The Worker stores only hashes of codes and tokens. Usage records use a keyed opaque identifier derived from the normalized email, so deleting and recreating an account cannot reset its spending limit.

| Method | Path | What it does |
| --- | --- | --- |
| GET | `/health` | Returns `{ "ok": true }`. No token needed. |
| POST | `/signup` | Tester sign-ups from the website's Try Kwela early page. Takes `{ email, phone, consent, website }`. Needs the `x-signup-token` header (the shared `SIGNUP_SECRET`), so only the Kwela site can use it. See [Tester sign-ups](#tester-sign-ups). |
| POST | `/auth/request-code` | Takes `{ email }` and emails a 6-digit code (valid 10 minutes, 3 requests per 15 minutes per address and 10 requests per 15 minutes per source IP). |
| POST | `/auth/verify` | Takes `{ email, code }` (5 tries per code) and returns `{ token, email }`. |
| POST | `/auth/sign-out` | Revokes the session token. |
| POST | `/api/runs` | Takes `{ messages, conversationContext? }` and an `Idempotency-Key` UUID. Returns a durable run ID so the app can poll without resending the request. Duplicate submissions reconnect to the same run. Messages queue in order in Android. |
| GET | `/api/runs/{id}` | Reads a run's `QUEUED`, `RUNNING`, `COMPLETE`, or `FAILED` state. On completion, includes the JSON reply until Android acknowledges it. |
| POST | `/api/runs/{id}/acknowledge` | Clears the staged reply after Android saves its documents and conversation summary locally. |
| POST | `/api/chat` | Synchronous legacy route. The app uses `/api/runs` so agent work survives a closed app and a lost connection. The assistant returns updated `conversationContext`; Android stores it locally and sends it with later turns. The saved CV profile is passed separately from the compaction window. |
| GET, PUT, DELETE | `/api/candidate-profile` | Reads, saves or removes the owner's saved CV profile. |
| POST | `/api/documents/pdf` | Renders a CV or cover letter draft (`{ type: "resume" \| "cover_letter", document }`) to PDF with Browser Run. |
| DELETE | `/api/account` | Removes the account, its sessions, pending codes, saved CV profile, and active or retained run data. Usage records stay under the keyed opaque identifier, so deleting and re-creating an account cannot reset the spending cap. |

Cloudflare Workflows owns each run after `/api/runs` accepts it. The Workflow retries a failed agent step once, then records an explicit failure. A failed request stays in R2 for 24 hours so an explicit retry can create a new execution attempt under the same idempotent run ID. Workflows keep execution metadata for 26 hours; response content remains only in D1 staging until Android acknowledges it or the account is deleted.

## Run and check

```sh
pnpm install
pnpm test        # node:test, no network
pnpm run check   # TypeScript
```

For local runs, put `OPENAI_API_KEY=...` in `.dev.vars` (git-ignored), apply the schema, then start the Worker:

```sh
pnpm run db:migrate:local
pnpm run dev
```

## Private closed-test sign-in

The closed test uses a reusable, non-expiring six-digit code for addresses in `AUTH_TEST_EMAILS`. Testers request the code in the app, and the app displays it after the Worker accepts an allow-listed address. This sends no email and needs no custom domain. Set `AUTH_TEST_CODE` and `AUTH_SECRET` as Worker secrets, then deploy. Add each tester's address to `AUTH_TEST_EMAILS` before inviting that tester. Remove `AUTH_TEST_CODE` before public launch.

For real email sign-in, configure a sender domain with an email provider. Both ZeptoMail and Cloudflare Email Service require a verified sender domain. ZeptoMail also requires an Agent and its Send API key. See [ZeptoMail's send-email API](https://www.zoho.com/cpaas/help/api/email-sending.html) for the current endpoint and regional setup.

Set the API key as a Worker secret, set the verified sender address as a variable, and use the regional email endpoint provided by the Agent setup:

```sh
pnpm exec wrangler secret put ZEPTOMAIL_API_KEY
pnpm exec wrangler secret put EMAIL_FROM
pnpm exec wrangler secret put ZEPTOMAIL_API_URL
pnpm run deploy
```

`EMAIL_FROM` and `ZEPTOMAIL_API_URL` are configuration values, not credentials. In email mode, only `AUTH_TEST_EMAILS` can request codes until the allow-list check is removed deliberately. In private-test mode, codes are returned only to allowed app clients; do not use this mode for public production.

## Tester sign-ups

The website's `/join` page collects an email address and phone number from people who want to test Kwela, with a ticked consent box. The site Worker sends them to `POST /signup` through its service binding, with the visitor's address in `x-client-ip` and a shared secret in `x-signup-token`.

How it is protected:

- **Closed unless configured.** The route answers 503 until `SIGNUP_SECRET` is set, and 403 for any request without the matching token (compared in constant time). Setting the token on the API Worker only is not enough: the site needs the same value.
- **Rate limited.** 5 sign-ups per hour per visitor address, kept as a keyed hash in `login_rate_limits`, never the address itself. A hidden form field catches simple bots: they are told it worked and nothing is saved.
- **Checked on the server.** The email is validated and lower-cased. The phone number is turned into international format (`+2547…`) and anything that is not a reachable number is refused. Consent must be exactly `true`; the time it was given is stored.
- **Nothing is readable over HTTP.** No route returns sign-ups, and a repeat sign-up gets the same answer as a new one, so the form cannot be used to check who has signed up. Replies are never cached.
- **Kept small, never overwritten.** Only email, phone, consent time and sign-up time are stored, in the `tester_signups` D1 table, one row per email and phone pair. Nothing is verified, so a stranger who types someone else's email adds a row beside theirs and cannot change it. Treat every row as a claim: your first message should confirm the person is who they say. A shared cap of 200 sign-ups an hour backs up the per-address limit. Rows are not linked to Kwela accounts, and deleting an account does not remove them: delete a person by hand when they ask.

Set it up (use the same random value, 32 or more characters, in both Workers):

```sh
pnpm run db:migrate:remote
openssl rand -base64 32          # copy this value
pnpm exec wrangler secret put SIGNUP_SECRET            # in backend/
pnpm exec wrangler secret put SIGNUP_SECRET            # in site/
```

Read the sign-ups yourself (newest first), export them, or delete one person:

```sh
pnpm exec wrangler d1 execute kwela --remote --command "SELECT email, phone, datetime(created_at/1000,'unixepoch') AS signed_up FROM tester_signups ORDER BY created_at DESC"
pnpm exec wrangler d1 execute kwela --remote --command "DELETE FROM tester_signups WHERE email = 'person@example.com'"
```

## Deploy

```sh
pnpm exec wrangler d1 create kwela        # once; copy the id into wrangler.jsonc
pnpm exec wrangler r2 bucket create kwela-run-inputs # once
pnpm run db:migrate:remote
pnpm exec wrangler secret put OPENAI_API_KEY
openssl rand -base64 48 | tr -d '\n' | pnpm exec wrangler secret put AUTH_SECRET
pnpm run deploy
```

Spending limits and model prices are the `vars` in `wrangler.jsonc`. Never put the OpenAI key there.

The Android app reads this Worker's address from `kwela.apiUrl` in the root `gradle.properties`.

## Voice checks

`scenarios/` holds real-life conversations (a cook on a shift, a doctor between patients, a bartender in a club, Swahili, Sheng, a correction, a noisy job ad, and the quick-question flow). `node scenarios/run.mjs [name]` runs them against the deployed API with an allow-listed tester address, checks the rules (at most four bubbles, short bubbles, no banned words, a next step each time, the honest one-liner at the first handover), and writes a transcript to `scenarios/out/` (git-ignored) for a person to read.
