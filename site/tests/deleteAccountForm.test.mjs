import assert from "node:assert/strict";
import test from "node:test";
import { readConfirmForm, readRequestForm } from "../src/lib/deleteAccountForm.ts";

function form(values) {
    const data = new FormData();
    for (const [key, value] of Object.entries(values)) data.set(key, value);
    return data;
}

test("asking for a code needs a valid email, which is tidied up", () => {
    assert.deepEqual(readRequestForm(form({ email: "  Amina@Example.test " })), { ok: true, value: { email: "amina@example.test" } });
    for (const email of ["", "nope", "a@b", "a b@c.com"]) {
        const result = readRequestForm(form({ email }));
        assert.equal(result.ok, false);
        assert.equal(result.message, "Enter a valid email address.");
    }
    assert.equal(readRequestForm(new FormData()).ok, false);
});

test("deleting needs the email, a six-digit code and the confirmation box", () => {
    const good = readConfirmForm(form({ email: "a@example.test", code: " 123456 ", confirm: "yes" }));
    assert.deepEqual(good, { ok: true, value: { email: "a@example.test", code: "123456" } });
});

test("a code that is not six digits is refused", () => {
    for (const code of ["", "12345", "1234567", "12345a"]) {
        const result = readConfirmForm(form({ email: "a@example.test", code, confirm: "yes" }));
        assert.equal(result.ok, false);
        assert.equal(result.message, "Enter the 6-digit code from your email.");
    }
});

test("the confirmation box must be ticked before anything is deleted", () => {
    const result = readConfirmForm(form({ email: "a@example.test", code: "123456" }));
    assert.equal(result.ok, false);
    assert.equal(result.message, "Tick the box to confirm you want to delete your account.");
});
