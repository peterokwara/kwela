/**
 * One draft response to a question from a job application.
 */
export interface IApplicationAnswer {
    /**
     * Question shown by the employer's application form.
     */
    question: string;
    /**
     * Suggested response for the candidate to review.
     */
    answer: string;
}
