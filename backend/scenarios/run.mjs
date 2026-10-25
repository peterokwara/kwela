/**
 * Runs each scenario against the deployed Kwela API as a black box, checks the replies, and writes a readable transcript.
 *   node scenarios/run.mjs [name-fragment]
 * Needs an allow-listed tester address whose test code the API returns (never printed).
 */
import { mkdirSync, writeFileSync } from "node:fs";
import { scenarios } from "./scenarios.mjs";

const API = process.env.KWELA_API_URL ?? "https://kwela-api.ojakaapeter711.workers.dev";
const EMAIL = process.env.KWELA_TEST_EMAIL ?? "ojakaapeter+kwela-scenarios@gmail.com";
const only = process.argv[2]?.toLowerCase();
const MIN_GAP_MS = 17_000; // the API allows 4 requests a minute per account

const words = (text) => text.trim().split(/\s+/).filter(Boolean).length;
const emoji = (text) => (text.match(/\p{Extended_Pictographic}/gu) ?? []).length;
const banned = /\b(AI|artificial intelligence|LLM|algorithm|agentic|JSON|schema|ATS)\b/;
let lastCall = 0;

async function post(path, body, token) {
    const wait = lastCall + MIN_GAP_MS - Date.now();
    if (path === "/api/chat" && wait > 0) await new Promise((resolve) => setTimeout(resolve, wait));
    const response = await fetch(`${API}${path}`, {
        method: "POST",
        headers: { "content-type": "application/json", ...(token ? { authorization: `Bearer ${token}` } : {}) },
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(170_000)
    });
    if (path === "/api/chat") lastCall = Date.now();
    return { status: response.status, body: await response.json().catch(() => ({})) };
}

async function signIn() {
    const asked = await post("/auth/request-code", { email: EMAIL });
    if (!asked.body.testCode) throw new Error(`No test code for ${EMAIL} (status ${asked.status}). Is it on the allow-list?`);
    const verified = await post("/auth/verify", { email: EMAIL, code: asked.body.testCode });
    if (!verified.body.token) throw new Error(`Sign-in failed (status ${verified.status}).`);
    return verified.body.token;
}

/** Checks one reply against the voice rules we agreed. */
function check(reply, context) {
    const problems = [];
    const bubbles = reply.message.split("\n\n").filter(Boolean);
    if (bubbles.length > 4) problems.push(["FAIL", `${bubbles.length} bubbles (max 4)`]);
    for (const bubble of bubbles) if (words(bubble) > 25) problems.push(["WARN", `long bubble (${words(bubble)} words): "${bubble.slice(0, 50)}…"`]);
    if (banned.test(reply.message) && !context.askedWhatItIs) problems.push(["FAIL", `banned word: ${reply.message.match(banned)[0]}`]);
    if (/saved to files/i.test(reply.message)) problems.push(["FAIL", 'says "Saved to Files"']);
    if (emoji(reply.message) > 1) problems.push(["WARN", `${emoji(reply.message)} emoji`]);
    if (!/next|tap|tell me|send|let me know|ready|share|reply|check|have a look|read/i.test(bubbles.at(-1) ?? "")) problems.push(["WARN", "last bubble has no clear next step"]);
    if (reply.type === "application_pack" && context.firstHandover && !/digital helper/i.test(reply.message)) problems.push(["WARN", 'first handover lacks the "digital helper" line']);
    if (reply.type === "application_pack" && !/hope it feels like you/i.test(reply.message)) problems.push(["WARN", 'handover lacks "Hope it feels like you"']);
    return problems;
}

async function runScenario(scenario, token) {
    const lines = [`## ${scenario.name}`, ""];
    const history = [];
    let profile = null;
    let handedOver = false;
    const results = [];
    let lastReply = null;
    for (const [index, scripted] of scenario.turns.entries()) {
        // "@review:Dozens" taps an answer to the writer's quick question. "@change:make it shorter" circles the first CV line.
        let text = scripted;
        if (scripted.startsWith("@review:")) {
            const finding = lastReply?.review?.[0];
            if (!finding) { lines.push(`_(skipped "${scripted}": the writer asked no quick question)_`, ""); results.push(["WARN", "no quick question to answer"]); continue; }
            text = `About “${finding.line}”: ${scripted.slice(8)}`;
        } else if (scripted.startsWith("@change:")) {
            const line = lastReply?.applicationPack?.resume?.employments?.[0]?.achievements?.[0];
            if (!line) { lines.push(`_(skipped "${scripted}": there is no CV line yet)_`, ""); results.push(["WARN", "no CV line to change"]); continue; }
            text = `Change this line in my CV: “${line}” — ${scripted.slice(8)}`;
        }
        history.push({ id: `u${index}`, role: "user", content: text });
        const messages = [...(profile ? [{ id: "saved-cv-profile", role: "assistant", content: "Saved CV profile for this person. Reuse it when preparing job materials.", candidateProfile: profile }] : []), ...history.slice(-19)];
        const started = Date.now();
        const { status, body } = await post("/api/chat", { messages }, token);
        lines.push(`**You:** ${text}`, "");
        if (status !== 200) {
            lines.push(`> ERROR ${status}: ${body.message ?? "no message"}`, "");
            results.push(["FAIL", `request failed with ${status}`]);
            break;
        }
        const seconds = ((Date.now() - started) / 1000).toFixed(0);
        const problems = check(body, { firstHandover: !handedOver, askedWhatItIs: /real person|are you (an? )?(ai|bot|human)/i.test(text) });
        if (body.type === "application_pack") handedOver = true;
        for (const bubble of body.message.split("\n\n")) lines.push(`> **Kwela:** ${bubble.replaceAll("\n", "\n> ")}`, ">");
        if (body.applicationPack) lines.push(`> _[hands over: ${body.applicationPack.job?.title ?? "job"}, CV + cover letter + ${body.applicationPack.applicationAnswers?.length ?? 0} answers]_`, ">");
        lines.push(`_${seconds}s · ${body.type}${problems.length ? " · " + problems.map(([level, note]) => `${level}: ${note}`).join("; ") : " · all checks passed"}_`, "");
        results.push(...problems);
        history.push({ id: `a${index}`, role: "assistant", content: body.message, ...(body.applicationPack ? { application: body.applicationPack } : {}) });
        if (body.candidateProfile) profile = body.candidateProfile;
        lastReply = body;
        if (body.review?.length) lines.push(`> _[quick question: “${body.review[0].line}” ${body.review[0].question} ${body.review[0].options.join(" / ")}]_`, "");
    }
    return { markdown: lines.join("\n"), results };
}

const token = await signIn();
const chosen = scenarios.filter((scenario) => !only || scenario.name.toLowerCase().includes(only));
const output = [`# Kwela scenario run`, `API: ${API}`, `Run: ${new Date().toISOString()}`, ""];
const summary = [];
for (const scenario of chosen) {
    console.log(`Running: ${scenario.name}`);
    try {
        const { markdown, results } = await runScenario(scenario, token);
        output.push(markdown);
        summary.push([scenario.name, results.filter(([level]) => level === "FAIL").length, results.filter(([level]) => level === "WARN").length]);
    } catch (error) {
        output.push(`## ${scenario.name}\n\n> RUN ERROR: ${error.message}\n`);
        summary.push([scenario.name, 1, 0]);
    }
}
output.push("## Summary", "", "| Scenario | Fails | Warnings |", "| --- | --- | --- |", ...summary.map(([name, fails, warns]) => `| ${name} | ${fails} | ${warns} |`));
mkdirSync(new URL("./out/", import.meta.url), { recursive: true });
const file = new URL(`./out/run-${Date.now()}.md`, import.meta.url);
writeFileSync(file, output.join("\n"));
console.log(summary.map(([name, fails, warns]) => `${fails ? "FAIL" : "ok  "} ${name} (fails ${fails}, warnings ${warns})`).join("\n"));
console.log(`Transcript: ${file.pathname}`);
