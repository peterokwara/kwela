/**
 * Most bubbles a single reply may contain.
 */
const MAX_BUBBLES = 4;

/**
 * Words a bubble should stay under. One long sentence is kept whole rather than cut mid-thought.
 */
const MAX_WORDS = 25;

/** Counts the words in a piece of text.
 * @param text Text to count.
 * @returns Number of whitespace-separated words.
 */
function wordCount(text: string): number {
    return text.split(/\s+/).filter(Boolean).length;
}

/** Splits one over-long paragraph at sentence ends into bubbles of about MAX_WORDS or fewer.
 * @param paragraph Paragraph that may be too long for a text message.
 * @returns One or more bubbles. Paragraphs with line breaks (short lists) are left alone.
 */
function splitLong(paragraph: string): string[] {
    if (wordCount(paragraph) <= MAX_WORDS || paragraph.includes("\n")) return [paragraph];
    const bubbles: string[] = [];
    let current = "";
    for (const sentence of paragraph.split(/(?<=[.!?…])\s+/)) {
        const joined = current ? `${current} ${sentence}` : sentence;
        if (current && wordCount(joined) > MAX_WORDS) {
            bubbles.push(current);
            current = sentence;
        } else {
            current = joined;
        }
    }
    if (current) bubbles.push(current);
    return bubbles;
}

/** Shapes a reply into short texting bubbles separated by blank lines.
 * @param message Reply text as the model wrote it.
 * @returns At most four bubbles, each a short paragraph, joined by blank lines.
 */
export function shapeBubbles(message: string): string {
    const bubbles = message
        .split(/\n\s*\n/)
        .map((paragraph) => paragraph.trim())
        .filter(Boolean)
        .flatMap(splitLong);
    const capped = bubbles.length > MAX_BUBBLES ? [...bubbles.slice(0, MAX_BUBBLES - 1), bubbles.slice(MAX_BUBBLES - 1).join(" ")] : bubbles;
    return capped.join("\n\n");
}
