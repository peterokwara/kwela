import assert from "node:assert/strict";
import test from "node:test";
import { readJoinForm } from "../src/lib/joinForm.ts";

function form(values) {
    const data = new FormData();
    for (const [key, value] of Object.entries(values)) data.set(key, value);
    return data;
}

const good = { email: "  Amina@Example.test ", phone: " 0712 345 678 ", website: "" };

test("a complete form is tidied up and passed on", () => {
    assert.deepEqual(readJoinForm(form(good)), { ok: true, value: { email: "amina@example.test", phone: "0712 345 678", website: "" } });
});

test("the phone number is left for the server to tidy, because only it knows the rules", () => {
    assert.equal(readJoinForm(form({ ...good, phone: "+254 712 345 678" })).value.phone, "+254 712 345 678");
});

test("a bad email is refused with a message", () => {
    for (const email of ["", "nope", "a@b", "a b@c.com", `${"a".repeat(250)}@x.co`]) {
        const result = readJoinForm(form({ ...good, email }));
        assert.equal(result.ok, false, email);
        assert.equal(result.message, "Enter a valid email address.");
    }
});

test("an empty or huge phone number is refused", () => {
    for (const phone of ["", "   ", "1".repeat(33)]) {
        const result = readJoinForm(form({ ...good, phone }));
        assert.equal(result.ok, false);
        assert.equal(result.message, "Enter a phone number we can reach you on, like 0712 345 678. Start with + for other countries.");
    }
});

test("there is no tick box: the notice under the button is the agreement, so the form needs no consent field", () => {
    const values = { ...good };
    delete values.consent;
    assert.equal(readJoinForm(form(values)).ok, true);
});

test("the hidden field is passed on so the server can spot bots, and long values are cut off", () => {
    assert.equal(readJoinForm(form({ ...good, website: "https://spam.test" })).value.website, "https://spam.test");
    assert.equal(readJoinForm(form({ ...good, website: "x".repeat(5000) })).value.website.length, 200);
});

test("a missing form does not crash", () => {
    assert.equal(readJoinForm(new FormData()).ok, false);
});
