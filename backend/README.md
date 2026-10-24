# Kwela API

The Cloudflare Worker behind the Kwela Android app. It is its own project with its own D1 database, secrets and deployment. It shares nothing with any other Worker.

The agent code (career partner, CV extraction, job research, application drafting), spending limits and Careergo document shapes were copied from Chaji and renamed. Kwela chats live on the phone, so there is no conversation storage here.

## Routes

Every `/api/*` route needs `Authorization: Bearer <install token>`. The app creates a random token on first launch. The Worker stores only its SHA-256 hash as the owner id.

| Method | Path | What it does |
| --- | --- | --- |
| GET | `/health` | Returns `{ "ok": true }`. No token needed. |
| POST | `/api/chat` | Takes `{ messages }` and returns the agent's JSON reply: `{ type, message, applicationPack, candidateProfile }`. Enforces the spending limits. |
| GET, PUT, DELETE | `/api/candidate-profile` | Reads, saves or removes the owner's saved CV profile. |
| POST | `/api/documents/pdf` | Renders a CV or cover letter draft (`{ type: "resume" \| "cover_letter", document }`) to PDF with Browser Run. |
| DELETE | `/api/account` | Removes the owner's saved CV profile. Usage records stay, keyed only by the hashed token, so deleting cannot reset the spending cap. |

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

## Deploy

```sh
pnpm exec wrangler d1 create kwela        # once; copy the id into wrangler.jsonc
pnpm run db:migrate:remote
pnpm exec wrangler secret put OPENAI_API_KEY
pnpm run deploy
```

Spending limits and model prices are the `vars` in `wrangler.jsonc`. Never put the OpenAI key there.

The Android app reads this Worker's address from `kwela.apiUrl` in the root `gradle.properties`.
