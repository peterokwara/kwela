import assert from 'node:assert/strict';
import test from 'node:test';
import { DatabaseSync } from 'node:sqlite';
import { readFileSync } from 'node:fs';
import { D1ChatRunService } from '../src/lib/server/services/d1ChatRunService.ts';

const id = 'a1111111-1111-4111-8111-111111111111';
const retryKey = 'b2222222-2222-4222-8222-222222222222';
function fixture() {
    const sql = new DatabaseSync(':memory:');
    sql.exec(readFileSync(new URL('../migrations/0005_durable_chat_runs.sql', import.meta.url), 'utf8'));
    const db = { prepare(query) { return { bind(...values) { return {
        async first() { return sql.prepare(query).get(...values) ?? null; },
        async all() { return { results: sql.prepare(query).all(...values) }; },
        async run() { return sql.prepare(query).run(...values); }
    }; } }; } };
    const objects = new Map();
    const bucket = {
        async put(key, data) { objects.set(key, data); },
        async get(key) { const value = objects.get(key); return value ? { async text() { return value; } } : null; },
        async delete(key) { objects.delete(key); }
    };
    const instances = new Map();
    const workflow = {
        async create(options) { if (instances.has(options.id)) throw new Error('Already exists'); instances.set(options.id, options); },
        async get(key) { if (!instances.has(key)) throw new Error('Not found'); return {
            async status() { return { status: 'running' }; },
            async terminate() {},
            async delete() { instances.delete(key); }
        }; }
    };
    let now = 1_000_000;
    const service = new D1ChatRunService(db, bucket, workflow, () => now);
    const request = (text = 'Help with my CV', retry = null) => new Request('https://test/api/runs', {
        method: 'POST', headers: { 'content-type': 'application/json', ...(retry ? { 'retry-key': retry } : {}) },
        body: JSON.stringify({ messages: [{ id, role: 'user', content: text }] })
    });
    const row = () => sql.prepare('SELECT * FROM chat_runs WHERE id = ?').get(id);
    return { service, sql, objects, instances, request, row, setNow(value) { now = value; }, now: () => now };
}

test('concurrent duplicate submissions keep exactly one immutable input and Workflow', async () => {
    const f = fixture();
    const responses = await Promise.all([f.service.start(f.request('First'), 'owner', id), f.service.start(f.request('Second'), 'owner', id)]);
    assert.ok(responses.every(response => response.status === 202));
    assert.equal(f.instances.size, 1);
    assert.equal(f.objects.size, 1);
    const input = f.objects.get(f.row().input_key);
    assert.ok(input.includes('First') || input.includes('Second'));
    await f.service.start(f.request('Overwrite attempt'), 'owner', id);
    assert.equal(f.objects.get(f.row().input_key), input);
});

test('run lookup and acknowledgement are scoped to the signed in owner', async () => {
    const f = fixture();
    await f.service.start(f.request(), 'owner', id);
    assert.equal((await f.service.get(id, 'stranger')).status, 404);
    f.sql.prepare("UPDATE chat_runs SET status = 'COMPLETE', reply_json = ? WHERE id = ?").run('{"message":"private"}', id);
    await f.service.acknowledge(id, 'stranger');
    assert.ok(f.row().reply_json);
    assert.equal((await f.service.start(f.request(), 'stranger', id)).status, 409);
    assert.equal(f.objects.size, 1);
});

test('a network replay never restarts a failed run; one explicit retry key starts one new attempt', async () => {
    const f = fixture();
    await f.service.start(f.request(), 'owner', id);
    f.sql.prepare("UPDATE chat_runs SET status = 'FAILED', input_expires_at = ? WHERE id = ?").run(f.now() + 86400000, id);
    await f.service.start(f.request(), 'owner', id);
    assert.equal(f.instances.size, 1);
    await f.service.start(f.request('ignored', retryKey), 'owner', id);
    assert.equal(f.instances.size, 2);
    assert.equal(f.row().attempt_count, 2);
    f.sql.prepare("UPDATE chat_runs SET status = 'FAILED', input_expires_at = ? WHERE id = ?").run(f.now() + 86400000, id);
    await f.service.start(f.request('replayed retry', retryKey), 'owner', id);
    assert.equal(f.instances.size, 2);
    assert.equal(f.row().attempt_count, 2);
});

test('failed input expires after 24 hours and cannot be retried afterward', async () => {
    const f = fixture();
    await f.service.start(f.request(), 'owner', id);
    const expires = f.now() + 86400000;
    f.sql.prepare("UPDATE chat_runs SET status = 'FAILED', input_expires_at = ? WHERE id = ?").run(expires, id);
    f.setNow(expires - 1);
    await f.service.cleanupExpiredInputs();
    assert.equal(f.objects.size, 1);
    f.setNow(expires);
    await f.service.cleanupExpiredInputs();
    assert.equal(f.objects.size, 0);
    await f.service.start(f.request('retry', retryKey), 'owner', id);
    assert.equal(f.instances.size, 1);
});

test('completed output remains recoverable after days offline, then acknowledgement clears it', async () => {
    const f = fixture();
    await f.service.start(f.request(), 'owner', id);
    await f.objects.delete(f.row().input_key);
    f.sql.prepare("UPDATE chat_runs SET status = 'COMPLETE', input_key = '', reply_json = ? WHERE id = ?").run('{"message":"Here is your CV"}', id);
    f.setNow(f.now() + 7 * 86400000);
    await f.service.cleanupExpiredInputs();
    let reply = await (await f.service.get(id, 'owner')).json();
    assert.equal(reply.reply.message, 'Here is your CV');
    await f.service.start(f.request('do not recreate input'), 'owner', id);
    assert.equal(f.objects.size, 0);
    await f.service.acknowledge(id, 'owner');
    reply = await (await f.service.get(id, 'owner')).json();
    assert.equal(reply.acknowledged, true);
    assert.equal(reply.reply, undefined);
});

test('missing unacknowledged output reports failure instead of pretending delivery succeeded', async () => {
    const f = fixture();
    await f.service.start(f.request(), 'owner', id);
    f.sql.prepare("UPDATE chat_runs SET status = 'COMPLETE', reply_json = NULL WHERE id = ?").run(id);
    const body = await (await f.service.get(id, 'owner')).json();
    assert.equal(body.status, 'FAILED');
    assert.match(body.error, /recover/);
});

test('account deletion removes task inputs, result records and all Workflow attempts', async () => {
    const f = fixture();
    await f.service.start(f.request(), 'owner', id);
    f.sql.prepare("UPDATE chat_runs SET status = 'FAILED', input_expires_at = ? WHERE id = ?").run(f.now() + 86400000, id);
    await f.service.start(f.request('retry', retryKey), 'owner', id);
    await f.service.deleteOwner('owner');
    assert.equal(f.objects.size, 0);
    assert.equal(f.instances.size, 0);
    assert.equal(f.row(), undefined);
});
