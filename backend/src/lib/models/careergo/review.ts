/**
 * One thing a careful writer would check with the person before handing a CV over.
 */
export interface IReviewFinding {
    /**
     * Which rule found it.
     */
    kind: "duty_phrase";
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

const dutyOpening = /^(responsible for|duties (included|include)|tasked with|worked on|in charge of)\b/i;
const hasDigit = /\d/;

/** Reviews the achievement lines of a CV with fixed rules, with no AI involved.
 * Only weak duty phrases with no result are asked about. A "how many?" question on any other line sounded out of place, so it was removed.
 * @param resume The CV, where the most recent job comes first.
 * @returns Up to three findings.
 */
export function reviewResume(resume: { employments?: { achievements?: string[] }[] }): IReviewFinding[] {
    const lines = (resume.employments ?? []).flatMap((job) => (job.achievements ?? []).map((line) => line.trim()).filter(Boolean));
    return lines
        .filter((line) => dutyOpening.test(line) && !hasDigit.test(line))
        .map(
            (line): IReviewFinding => ({
                kind: "duty_phrase",
                line,
                question: "What changed because of your work on this?",
                options: ["It got faster", "It saved money", "Fewer mistakes", "More customers", "Not sure"]
            })
        )
        .slice(0, MAX_FINDINGS);
}
