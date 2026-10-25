import assert from "node:assert/strict";
import test from "node:test";
import { ZeptoMailOtpMailer } from "../src/lib/server/services/otpMailers.ts";

test("ZeptoMail receives the OTP through an authenticated transactional email request", async () => {
    let request;
    const mailer = new ZeptoMailOtpMailer("test-agent-key", "login@example.test", "https://region.example/v1.1/email", async (url, init) => {
        request = { url, init };
        return new Response("{}", { status: 200 });
    });

    await mailer.send("applicant@example.test", "123456");

    assert.equal(request.url, "https://region.example/v1.1/email");
    assert.equal(request.init.method, "POST");
    assert.equal(request.init.headers.Authorization, "Zoho-enczapikey test-agent-key");
    const payload = JSON.parse(request.init.body);
    assert.deepEqual(payload.from, { address: "login@example.test", name: "Kwela" });
    assert.deepEqual(payload.to, [{ email_address: { address: "applicant@example.test" } }]);
    assert.match(payload.subject, /123456/);
    assert.match(payload.textbody, /10 minutes/);
    assert.equal(payload.track_clicks, false);
    assert.equal(payload.track_opens, false);
});

test("ZeptoMail API errors fail delivery instead of silently accepting a code", async () => {
    const mailer = new ZeptoMailOtpMailer("test-agent-key", "login@example.test", undefined, async () =>
        new Response("sender domain is not verified", { status: 400 })
    );

    await assert.rejects(() => mailer.send("applicant@example.test", "123456"), /ZeptoMail rejected.*400/);
});
