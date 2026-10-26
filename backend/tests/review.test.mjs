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

test("a long line with no numbers asks for a rough size with quick taps", () => {
    const findings = reviewResume(cv(["Cleaned sales reports for the regional team"]));
    assert.equal(findings[0].kind, "no_numbers");
    assert.deepEqual(findings[0].options, ["A few", "Dozens", "Hundreds", "I don’t count"]);
    assert.match(findings[0].question, /roughly how many|how often/i);
});

test("lines that already have a number are left alone", () => {
    assert.deepEqual(reviewResume(cv(["Cleaned 40 sales reports a week", "Led a team of 3"])), []);
});

test("short lines are not worth asking about", () => {
    assert.deepEqual(reviewResume(cv(["Filing", "Typing"])), []);
});

test("at most three findings, duties first, then the most recent job", () => {
    const findings = reviewResume(
        cv(
            ["Managed the front desk and welcomed visitors", "Responsible for opening the office each morning", "Answered customer questions in person"],
            ["Prepared monthly stock reports for the manager", "Responsible for ordering supplies each month"]
        )
    );
    assert.equal(findings.length, 3);
    assert.deepEqual(findings.slice(0, 2).map((finding) => finding.kind), ["duty_phrase", "duty_phrase"]);
    assert.equal(findings[0].line, "Responsible for opening the office each morning");
    assert.equal(findings[2].kind, "no_numbers");
    assert.equal(findings[2].line, "Managed the front desk and welcomed visitors");
});

test("a CV with nothing to ask about gives no findings", () => {
    assert.deepEqual(reviewResume({ employments: [] }), []);
    assert.deepEqual(reviewResume({}), []);
});

test("a line that already says how much or how often is not asked about again", () => {
    const answered = [
        "Kept daily stock records for the warehouse team",
        "Cleaned sales reports for dozens of regional teams",
        "Processed several hundred supplier invoices each month",
        "Handled many customer questions in person"
    ];
    assert.deepEqual(reviewResume(cv(answered)), []);
});

test("a long, detailed line is not asked how many, because the size question makes no sense for it", () => {
    const line = "Owned end-to-end SaaS delivery across architecture, implementation, deployment, monitoring, analytics and product operations for the whole platform";
    assert.deepEqual(reviewResume(cv([line])), []);
});

test("strategy and leadership lines are not asked how many either", () => {
    assert.deepEqual(reviewResume(cv(["Set the engineering strategy and roadmap for the product team"])), []);
});
