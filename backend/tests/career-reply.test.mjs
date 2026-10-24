import assert from "node:assert/strict";
import test from "node:test";
import { careerReplySchema, normalizeCareerReply } from "../src/lib/models/careergo/ICareerReply.ts";

test("a reply that leaves out empty fields is accepted and filled with null", () => {
    const parsed = careerReplySchema.parse({ type: "message", message: "Send your CV." });
    assert.deepEqual(normalizeCareerReply(parsed), {
        type: "message",
        message: "Send your CV.",
        applicationPack: null,
        candidateProfile: null
    });
});

test("explicit nulls pass through unchanged", () => {
    const reply = normalizeCareerReply(
        careerReplySchema.parse({ type: "message", message: "Hi", applicationPack: null, candidateProfile: null })
    );
    assert.equal(reply.applicationPack, null);
    assert.equal(reply.candidateProfile, null);
});

test("a reply with an unknown type is still rejected", () => {
    assert.equal(careerReplySchema.safeParse({ type: "nonsense", message: "x" }).success, false);
});
