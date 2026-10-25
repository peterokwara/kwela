import assert from 'node:assert/strict';
import test from 'node:test';
import { compactConversationContext } from '../src/lib/server/services/conversationContextService.ts';
import { createOpenAICareerAssistantService } from '../src/lib/server/services/openAICareerAssistantService.ts';
const messages = count => Array.from({ length: count }, (_, i) => ({ id: `m${i + 1}`, role: i % 2 ? 'assistant' : 'user', content: `turn-${i + 1}` }));
const usage = { inputTokens: 20, outputTokens: 10 };

test('compaction waits for six old turns and preserves twelve full turns', async () => {
    let calls = 0;
    const summarize = async input => { calls++; assert.equal(input.length, 7); return { summary: 'Confirmed job and pending question', usage }; };
    assert.equal(await compactConversationContext(messages(17), null, null, summarize), null);
    assert.equal(calls, 0);
    const summary = await compactConversationContext(messages(18), null, null, summarize);
    assert.equal(calls, 1);
    assert.equal(summary.throughMessageId, 'm6');
});

test('rolling compaction carries forward the existing summary and only folds new old turns', async () => {
    const current = { summary: 'Earlier confirmed facts', throughMessageId: 'm6' };
    const result = await compactConversationContext(messages(24), current, null, async input => {
        assert.match(JSON.stringify(input[0]), /Earlier confirmed facts/);
        assert.equal(input.length, 7);
        assert.match(JSON.stringify(input[1]), /turn-7/);
        return { summary: 'Updated facts', usage };
    });
    assert.equal(result.throughMessageId, 'm12');
});

test('a summary failure keeps the previous memory intact', async () => {
    const current = { summary: 'Confirmed facts', throughMessageId: 'm6' };
    assert.equal(await compactConversationContext(messages(24), current, null, async () => { throw new Error('offline'); }), current);
});

test('model receives twelve full turns after successful compaction and the profile bypasses the summary', async () => {
    let input;
    const history = [{ id: 'saved-cv-profile', role: 'assistant', content: 'profile' }, ...messages(30)];
    const assistant = createOpenAICareerAssistantService('test', async items => { input = items; return { output: { type: 'message', message: 'Ready.' }, usage }; }, async items => {
        assert.equal(items.length, 30);
        assert.ok(items.every(item => item.id !== 'saved-cv-profile'));
        return { summary: 'Earlier facts', throughMessageId: 'm18' };
    });
    await assistant.respond(history);
    assert.equal(input.length, 14); // memory, profile, twelve actual turns
    assert.match(JSON.stringify(input[2]), /turn-19/);
});

test('model retains all unsummarized turns when compaction fails instead of truncating to twenty', async () => {
    let input;
    const current = { summary: 'Facts through turn four', throughMessageId: 'm4' };
    const assistant = createOpenAICareerAssistantService('test', async items => { input = items; return { output: { type: 'message', message: 'Ready.' }, usage }; }, async () => current);
    await assistant.respond(messages(35), null, current);
    assert.equal(input.length, 32); // existing memory and 31 unsummarized turns
    assert.match(JSON.stringify(input[1]), /turn-5/);
});
