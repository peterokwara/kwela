# Kwela API

The Cloudflare Worker behind the Kwela Android app. It is its own project with its own D1 database, secrets and deployment. It shares nothing with any other Worker.

The agent code (career partner, CV extraction, job research, application drafting), spending limits and Careergo document shapes were copied from Chaji and renamed. Kwela chats and their compacted conversation memory live on the phone; the Worker processes the memory for a reply but does not store it.

## Routes

People sign in with their email address and a 6-digit code (no passwords). `POST /auth/verify` returns a session token. Every `/api/*` route needs `Authorization: Bearer <session token>`. The Worker stores only hashes of codes and tokens. Usage records use a keyed opaque identifier derived from the normalized email, so deleting and recreating an account cannot reset its spending limit.

| Method | Path | What it does |
| --- | --- | --- |
| GET | `/health` | Returns `{ "ok": true }`. No token needed. |
| POST | `/auth/request-code` | Takes `{ email }` and emails a 6-digit code (valid 10 minutes, 3 requests per 15 minutes per address and 10 requests per 15 minutes per source IP). |
| POST | `/auth/verify` | Takes `{ email, code }` (5 tries per code) and returns `{ token, email }`. |
| POST | `/auth/sign-out` | Revokes the session token. |
| POST | `/api/chat` | Takes `{ messages, conversationContext? }` and returns the agent's JSON reply, including updated `conversationContext`. The app saves that compact summary on the phone and sends it with later turns. `message` is up to four short bubbles separated by blank lines. `review` holds up to three things the writer wants to check, found by fixed rules (a duty with no result, a line with no numbers). A reply that fails after retries is a real error, never a pretend message. Enforces the spending limits. |
| GET, PUT, DELETE | `/api/candidate-profile` | Reads, saves or removes the owner's saved CV profile. |
| POST | `/api/documents/pdf` | Renders a CV or cover letter draft (`{ type: "resume" \| "cover_letter", document }`) to PDF with Browser Run. |
| DELETE | `/api/account` | Removes the account, its sessions, pending codes and saved CV profile. Usage records stay under the keyed opaque identifier, so deleting and re-creating an account cannot reset the spending cap. |

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

## Deploy

```sh
pnpm exec wrangler d1 create kwela        # once; copy the id into wrangler.jsonc
pnpm run db:migrate:remote
pnpm exec wrangler secret put OPENAI_API_KEY
openssl rand -base64 48 | tr -d '\n' | pnpm exec wrangler secret put AUTH_SECRET
pnpm run deploy
```

Spending limits and model prices are the `vars` in `wrangler.jsonc`. Never put the OpenAI key there.

The Android app reads this Worker's address from `kwela.apiUrl` in the root `gradle.properties`.

## Voice checks

`scenarios/` holds real-life conversations (a cook on a shift, a doctor between patients, a bartender in a club, Swahili, Sheng, a correction, a noisy job ad, and the quick-question flow). `node scenarios/run.mjs [name]` runs them against the deployed API with an allow-listed tester address, checks the rules (at most four bubbles, short bubbles, no banned words, a next step each time, the honest one-liner at the first handover), and writes a transcript to `scenarios/out/` (git-ignored) for a person to read.
