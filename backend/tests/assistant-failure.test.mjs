import assert from "node:assert/strict";
import test from "node:test";
import { createOpenAICareerAssistantService } from "../src/lib/server/services/openAICareerAssistantService.ts";

const userMessage = [{ id: "1", role: "user", content: "Hi" }];
const goodOutput = { type: "message", message: "Okay, done 🙂\n\nYour CV is ready." };

async function readReply(stream) {
    return JSON.parse(await new Response(stream).text());
}

test("a reply that works first time is returned and counted", async () => {
    const usage = [];
    const assistant = createOpenAICareerAssistantService("key", async () => ({ output: goodOutput, usage: { inputTokens: 10, outputTokens: 5 } }));
    const reply = await readReply(await assistant.respond(userMessage, (used) => usage.push(used)));
    assert.equal(reply.message, "Okay, done 🙂\n\nYour CV is ready.");
    assert.deepEqual(usage, [{ inputTokens: 10, outputTokens: 5 }]);
});

test("one failed attempt is retried quietly", async () => {
    let calls = 0;
    const assistant = createOpenAICareerAssistantService("key", async () => {
        calls += 1;
        if (calls === 1) throw new Error("model returned junk");
        return { output: goodOutput, usage: null };
    });
    const reply = await readReply(await assistant.respond(userMessage));
    assert.equal(calls, 2);
    assert.equal(reply.type, "message");
});

test("two failed attempts end in an error, never a pretend reply", async () => {
    let calls = 0;
    const usage = [];
    const assistant = createOpenAICareerAssistantService("key", async () => {
        calls += 1;
        throw new Error("model returned junk");
    });
    await assert.rejects(assistant.respond(userMessage, (used) => usage.push(used)), /could not finish/i);
    assert.equal(calls, 2);
    assert.deepEqual(usage, [null, null]);
});

test("an empty answer twice is also an error", async () => {
    const assistant = createOpenAICareerAssistantService("key", async () => ({ output: null, usage: null }));
    await assert.rejects(assistant.respond(userMessage), /could not finish/i);
});

test("a timeout is not retried, because a second attempt would only make the wait longer", async () => {
    let calls = 0;
    const assistant = createOpenAICareerAssistantService("key", async () => {
        calls += 1;
        const error = new Error("took too long");
        error.name = "TimeoutError";
        throw error;
    });
    await assert.rejects(assistant.respond(userMessage), (error) => error.timedOut === true);
    assert.equal(calls, 1);
});
