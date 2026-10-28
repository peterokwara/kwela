import assert from "node:assert/strict";
import test from "node:test";
import { readContactForm } from "../src/lib/contactForm.ts";

function form(values) {
    const data = new FormData();
    for (const [key, value] of Object.entries(values)) data.set(key, value);
    return data;
}

const good = { email: "  Amina@Example.test ", message: "  My sign-in code did not arrive.  ", website: "" };

test("a complete form is tidied up and passed on", () => {
    assert.deepEqual(readContactForm(form(good)), { ok: true, value: { email: "amina@example.test", message: "My sign-in code did not arrive.", website: "" } });
});

test("a bad email is refused with a message", () => {
    for (const email of ["", "nope", "a@b", `${"a".repeat(250)}@x.co`]) {
        const result = readContactForm(form({ ...good, email }));
        assert.equal(result.ok, false, email);
        assert.equal(result.message, "Enter a valid email address so we can reply.");
    }
});

test("a message that is too short or too long is refused", () => {
    for (const message of ["", "   ", "hi", "x".repeat(2001)]) {
        const result = readContactForm(form({ ...good, message }));
        assert.equal(result.ok, false);
        assert.equal(result.message, "Write a message of 10 to 2000 characters.");
    }
});

test("the hidden field is passed on so the server can spot bots, and long values are cut off", () => {
    assert.equal(readContactForm(form({ ...good, website: "https://spam.test" })).value.website, "https://spam.test");
    assert.equal(readContactForm(form({ ...good, website: "x".repeat(5000) })).value.website.length, 200);
});

test("a missing form does not crash", () => {
    assert.equal(readContactForm(new FormData()).ok, false);
});
