import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { DatabaseSync } from "node:sqlite";
import test from "node:test";
import { D1TesterSignupStoreService } from "../src/lib/server/services/d1TesterSignupStoreService.ts";

/** Runs the real migrations on real SQLite behind the small slice of the D1 API the store uses. */
function database() {
    const sqlite = new DatabaseSync(":memory:");
    for (const file of ["0004_auth_rate_limits.sql", "0007_tester_signups.sql"]) sqlite.exec(readFileSync(new URL(`../migrations/${file}`, import.meta.url), "utf8"));
    const d1 = {
        prepare: (query) => ({
            bind: (...values) => ({
                first: async () => sqlite.prepare(query).get(...values) ?? null,
                run: async () => sqlite.prepare(query).run(...values)
            })
        })
    };
    return { sqlite, d1 };
}

test("a sign-up is stored as given", async () => {
    const { sqlite, d1 } = database();
    await new D1TesterSignupStoreService(d1).save({ email: "a@example.test", phone: "+254712345678", consentAt: 5, createdAt: 5 });
    assert.deepEqual({ ...sqlite.prepare("SELECT * FROM tester_signups").get() }, { email: "a@example.test", phone: "+254712345678", consent_at: 5, created_at: 5 });
});

test("signing up again with the same details changes nothing", async () => {
    const { sqlite, d1 } = database();
    const store = new D1TesterSignupStoreService(d1);
    await store.save({ email: "a@example.test", phone: "+254712345678", consentAt: 5, createdAt: 5 });
    await store.save({ email: "a@example.test", phone: "+254712345678", consentAt: 9, createdAt: 9 });
    assert.deepEqual(sqlite.prepare("SELECT * FROM tester_signups").all().map((row) => ({ ...row })), [{ email: "a@example.test", phone: "+254712345678", consent_at: 5, created_at: 5 }]);
});

test("another phone for the same email is kept beside the first, never over it", async () => {
    const { sqlite, d1 } = database();
    const store = new D1TesterSignupStoreService(d1);
    await store.save({ email: "a@example.test", phone: "+254712345678", consentAt: 5, createdAt: 5 });
    await store.save({ email: "a@example.test", phone: "+254722000111", consentAt: 9, createdAt: 9 });
    assert.deepEqual(sqlite.prepare("SELECT phone FROM tester_signups ORDER BY created_at").all().map((row) => row.phone), ["+254712345678", "+254722000111"]);
});

test("values are bound, never pasted into the query", async () => {
    const { sqlite, d1 } = database();
    const hostile = "x'); DROP TABLE tester_signups; --@example.test";
    await new D1TesterSignupStoreService(d1).save({ email: hostile, phone: "+254712345678", consentAt: 1, createdAt: 1 });
    assert.equal(sqlite.prepare("SELECT email FROM tester_signups").get().email, hostile);
});

test("the rate limit allows the limit, then refuses, then opens again in a new window", async () => {
    const { d1 } = database();
    const store = new D1TesterSignupStoreService(d1);
    const hit = (now) => store.hitRateLimit("key", now, 1000, 2);
    assert.deepEqual([await hit(0), await hit(1), await hit(2)], [true, true, false]);
    assert.equal(await hit(1500), true);
});
