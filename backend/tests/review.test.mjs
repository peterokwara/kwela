import assert from "node:assert/strict";
import test from "node:test";
import { reviewResume } from "../src/lib/models/careergo/review.ts";

/** A minimal CV with just the parts the review reads. */
function cv(...jobs) {
    return { employments: jobs.map((achievements, index) => ({ jobTitle: `Role ${index}`, employer: `Employer ${index}`, isPresent: index === 0, achievements })) };
}

test("a duty with no result is flagged and quoted exactly", () => {
    const findings = reviewResume(cv(["Responsible for cleaning weekly sales reports"]));
    assert.equal(findings.length, 1);
    assert.equal(findings[0].kind, "duty_phrase");
    assert.equal(findings[0].line, "Responsible for cleaning weekly sales reports");
    assert.match(findings[0].question, /what changed/i);
    assert.ok(findings[0].options.length >= 3);
});

test("lines that already have a number are left alone", () => {
    assert.deepEqual(reviewResume(cv(["Responsible for cleaning 40 sales reports a week", "Led a team of 3"])), []);
});

test("a line without numbers is never asked how many, because that question felt out of place", () => {
    assert.deepEqual(reviewResume(cv(["Cleaned sales reports for the regional team", "Built AI workflows integrating Gemini and OpenAI through OpenRouter"])), []);
});

test("at most three findings, and the most recent job comes first", () => {
    const findings = reviewResume(
        cv(
            ["Responsible for opening the office each morning", "Responsible for closing the office each evening"],
            ["Responsible for ordering supplies each month", "Responsible for weekly stock counts"]
        )
    );
    assert.equal(findings.length, 3);
    assert.equal(findings[0].line, "Responsible for opening the office each morning");
});

test("a CV with nothing to ask about gives no findings", () => {
    assert.deepEqual(reviewResume({ employments: [] }), []);
    assert.deepEqual(reviewResume({}), []);
});
