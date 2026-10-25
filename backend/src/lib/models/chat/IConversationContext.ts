import type { IApplicationPack } from "../careergo/IApplicationPack.ts";

/** Compact memory that lets Kwela keep useful context beyond the recent chat window. */
export interface IConversationContext {
    /** Short summary of older conversation turns. */
    summary: string;
    /** Last conversation message included in the summary. */
    throughMessageId: string;
    /** Latest older draft, kept structured so later revisions can use it. */
    currentApplication?: IApplicationPack;
}
