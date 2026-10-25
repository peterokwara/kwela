/**
 * One thing a careful writer would check with the person before handing a CV over.
 */
export interface IReviewFinding {
    /**
     * Which rule found it.
     */
    kind: "duty_phrase" | "no_numbers";
    /**
     * The person's own line, quoted exactly.
     */
    line: string;
    /**
     * A short question that can be answered with one tap.
     */
    question: string;
    /**
     * Quick answers to tap.
     */
    options: string[];
}

/**
 * The most findings shown at once, so the review never feels like a test.
 */
const MAX_FINDINGS = 3;

/**
 * Lines shorter than this are not worth asking about.
 */
const MIN_LINE_LENGTH = 25;

const dutyOpening = /^(responsible for|duties (included|include)|tasked with|worked on|in charge of)\b/i;
const hasDigit = /\d/;
const saysHowMuch = /\b(dozens?|hundreds?|thousands?|several|a few|many|daily|weekly|monthly|yearly|every|each|nightly|hourly)\b/i;

/** Reviews the achievement lines of a CV with fixed rules, with no AI involved.
 * @param resume The CV, where the most recent job comes first.
 * @returns Up to three findings: duties with no result first, then lines with no numbers.
 */
export function reviewResume(resume: { employments?: { achievements?: string[] }[] }): IReviewFinding[] {
    const lines = (resume.employments ?? []).flatMap((job) => (job.achievements ?? []).map((line) => line.trim()).filter(Boolean));
    const duties = lines.filter((line) => dutyOpening.test(line) && !hasDigit.test(line));
    const quiet = lines.filter((line) => !dutyOpening.test(line) && !hasDigit.test(line) && !saysHowMuch.test(line) && line.length >= MIN_LINE_LENGTH);
    return [
        ...duties.map(
            (line): IReviewFinding => ({
                kind: "duty_phrase",
                line,
                question: "What changed because of your work on this?",
                options: ["It got faster", "It saved money", "Fewer mistakes", "More customers", "Not sure"]
            })
        ),
        ...quiet.map(
            (line): IReviewFinding => ({
                kind: "no_numbers",
                line,
                question: "Roughly how many, or how often?",
                options: ["A few", "Dozens", "Hundreds", "I don’t count"]
            })
        )
    ].slice(0, MAX_FINDINGS);
}
