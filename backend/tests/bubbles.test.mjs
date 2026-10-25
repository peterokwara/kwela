import assert from "node:assert/strict";
import test from "node:test";
import { shapeBubbles } from "../src/lib/models/careergo/bubbles.ts";

const words = (text) => text.trim().split(/\s+/).length;

test("short paragraphs stay as they are, one bubble each", () => {
    assert.equal(shapeBubbles("Okay, done 🙂\n\nYour CV is ready.\n\nTap a line to change it."), "Okay, done 🙂\n\nYour CV is ready.\n\nTap a line to change it.");
});

test("stray blank lines and spaces are tidied", () => {
    assert.equal(shapeBubbles("  Hello  \n\n\n\n  How are you?  \n"), "Hello\n\nHow are you?");
    assert.equal(shapeBubbles("   \n\n  "), "");
});

test("a long paragraph is split at sentence ends into bubbles of about 25 words or fewer", () => {
    const long =
        "I have gone through your CV and the job carefully and I think your experience fits well. " +
        "The data clerk role matches what they are asking for and the skills line is strong. " +
        "I only need one small detail from you before I hand it over.";
    const bubbles = shapeBubbles(long).split("\n\n");
    assert.ok(bubbles.length >= 2);
    for (const bubble of bubbles) assert.ok(words(bubble) <= 25, bubble);
    assert.equal(bubbles.join(" "), long);
});

test("a single sentence that is too long is kept whole rather than cut in the middle", () => {
    const sentence = Array.from({ length: 40 }, (_, i) => `word${i}`).join(" ") + ".";
    assert.equal(shapeBubbles(sentence), sentence);
});

test("no more than four bubbles: the extra ones are folded into the last", () => {
    const shaped = shapeBubbles("One.\n\nTwo.\n\nThree.\n\nFour.\n\nFive.\n\nSix.");
    assert.deepEqual(shaped.split("\n\n"), ["One.", "Two.", "Three.", "Four. Five. Six."]);
});

test("single line breaks inside a bubble are kept, so short lists survive", () => {
    assert.equal(shapeBubbles("Quick check:\n- phone number\n- end date"), "Quick check:\n- phone number\n- end date");
});

test("emoji and non-English text are untouched", () => {
    assert.equal(shapeBubbles("Sawa kabisa 👍\n\nNimemaliza CV yako."), "Sawa kabisa 👍\n\nNimemaliza CV yako.");
});
