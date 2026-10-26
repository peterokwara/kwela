import assert from "node:assert/strict";
import test from "node:test";
import { AssistantFailedError } from "../src/lib/server/services/assistantFailedError.ts";
import { createChatPostHandler } from "../src/lib/server/services/chatPostHandlerService.ts";

function streamText(text) {
    return new ReadableStream({
        start(controller) {
            controller.enqueue(new TextEncoder().encode(text));
            controller.close();
        }
    });
}

test("chat handler passes valid messages through the CareerAssistant port and streams its reply", async () => {
    const messages = [{ id: "message-1", role: "user", content: "Help me update my CV." }];
    let receivedMessages;
    const assistant = {
        async respond(input) {
            receivedMessages = input;
            return streamText('{"type":"message","message":"Ready."}');
        }
    };
    const handler = createChatPostHandler(assistant);
    const response = await handler({
        request: new Request("http://kwela.test/api/chat", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ messages })
        })
    });

    assert.equal(response.status, 200);
    assert.equal(response.headers.get("content-type"), "text/plain; charset=utf-8");
    assert.deepEqual(receivedMessages, messages);
    assert.equal(await response.text(), '{"type":"message","message":"Ready."}');
});

test("chat handler rejects malformed messages before calling the CareerAssistant port", async () => {
    let called = false;
    const handler = createChatPostHandler({
        async respond() {
            called = true;
            return streamText("unexpected");
        }
    });
    const response = await handler({
        request: new Request("http://kwela.test/api/chat", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ messages: [{ id: "message-1", role: "system", content: "bad role" }] })
        })
    });

    assert.equal(response.status, 400);
    assert.equal(called, false);
});

test("chat handler rejects JSON null without calling the CareerAssistant port", async () => {
    let called = false;
    const handler = createChatPostHandler({
        async respond() {
            called = true;
            return streamText("unexpected");
        }
    });
    const response = await handler({
        request: new Request("http://kwela.test/api/chat", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: "null"
        })
    });

    assert.equal(response.status, 400);
    assert.equal(called, false);
});

test("chat handler accepts supported document attachments and passes them to the CareerAssistant port", async () => {
    let receivedMessages;
    const attachment = {
        id: "file-1",
        name: "resume.pdf",
        type: "application/pdf",
        data: "data:application/pdf;base64,dGVzdA=="
    };
    const handler = createChatPostHandler({
        async respond(input) {
            receivedMessages = input;
            return streamText("reply");
        }
    });
    const response = await handler({
        request: new Request("http://kwela.test/api/chat", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ messages: [{ id: "message-1", role: "user", content: "", files: [attachment] }] })
        })
    });

    assert.equal(response.status, 200);
    assert.equal(receivedMessages[0].files[0].name, attachment.name);
});

test("chat handler rejects unsupported attachment extensions before calling the CareerAssistant port", async () => {
    let called = false;
    const handler = createChatPostHandler({
        async respond() {
            called = true;
            return streamText("unexpected");
        }
    });
    const response = await handler({
        request: new Request("http://kwela.test/api/chat", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({
                messages: [
                    {
                        id: "message-1",
                        role: "user",
                        content: "",
                        files: [
                            {
                                id: "file-1",
                                name: "resume.exe",
                                type: "application/pdf",
                                data: "data:application/pdf;base64,dGVzdA=="
                            }
                        ]
                    }
                ]
            })
        })
    });

    assert.equal(response.status, 400);
    assert.equal(called, false);
});

test("a reply that ran out of time answers with a 4xx, so the durable run does not repeat the same slow attempt", async () => {
    const assistant = { async respond() { throw new AssistantFailedError(true); } };
    const handler = createChatPostHandler(assistant);
    const response = await handler({
        request: new Request("http://kwela.test/api/chat", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ messages: [{ id: "1", role: "user", content: "Do my CV." }] })
        })
    });
    assert.equal(response.status, 408);
    assert.match((await response.json()).message, /took too long/);
});

test("any other assistant failure stays a retryable 500", async () => {
    const assistant = { async respond() { throw new AssistantFailedError(); } };
    const handler = createChatPostHandler(assistant);
    const response = await handler({
        request: new Request("http://kwela.test/api/chat", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ messages: [{ id: "1", role: "user", content: "Do my CV." }] })
        })
    });
    assert.equal(response.status, 500);
});
