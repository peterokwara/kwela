/**
 * Spending and request limits applied to each visitor.
 */
export interface IUsageLimits {
    /**
     * Most one visitor may spend, in millionths of a US dollar.
     */
    capMicros: number;
    /**
     * Most all visitors together may spend, in millionths of a US dollar.
     */
    globalCapMicros: number;
    /**
     * Most chat requests one visitor may start in a minute.
     */
    maxRequestsPerMinute: number;
}

/**
 * Model prices in US dollars per million tokens.
 */
export interface IModelPrices {
    /**
     * Price per million input tokens.
     */
    inputPerMillionUsd: number;
    /**
     * Price per million output tokens.
     */
    outputPerMillionUsd: number;
}

/**
 * What the database knows about recent use.
 */
export interface IUsageSnapshot {
    /**
     * Requests this visitor started in the last minute.
     */
    requestsLastMinute: number;
    /**
     * Total this visitor has spent, in millionths of a US dollar.
     */
    spentMicros: number;
    /**
     * Total all visitors have spent, in millionths of a US dollar.
     */
    globalSpentMicros: number;
}

/**
 * Whether a request may start, and what to tell the person if not.
 */
export type UsageDecision = { allowed: true } | { allowed: false; status: 429 | 503; message: string };

/**
 * Charge held for every request before its real cost is known, in millionths of a dollar.
 */
export const MINIMUM_CHARGE_MICROS = 20_000;

/**
 * Limits used when none are configured: $1 per visitor, $25 overall, 4 requests a minute.
 */
export const DEFAULT_USAGE_LIMITS: IUsageLimits = {
    capMicros: 1_000_000,
    globalCapMicros: 25_000_000,
    maxRequestsPerMinute: 4
};

/**
 * Deliberately high prices so the cap errs on the side of spending less than it allows.
 */
export const DEFAULT_MODEL_PRICES: IModelPrices = { inputPerMillionUsd: 5, outputPerMillionUsd: 20 };

/** Estimates what a model call cost from its token counts.
 * @param usage Input and output tokens the call used.
 * @param prices Model prices per million tokens.
 * @returns The cost in millionths of a US dollar, rounded up.
 */
export function estimateCostMicros(usage: { inputTokens: number; outputTokens: number }, prices: IModelPrices): number {
    return Math.ceil(usage.inputTokens * prices.inputPerMillionUsd + usage.outputTokens * prices.outputPerMillionUsd);
}

/** Decides whether a visitor may start another chat request.
 * @param snapshot Recent use recorded in the database.
 * @param limits Limits to apply.
 * @returns Allowed, or the status and plain message to show the person.
 */
export function evaluateUsage(snapshot: IUsageSnapshot, limits: IUsageLimits): UsageDecision {
    if (snapshot.globalSpentMicros >= limits.globalCapMicros) {
        return {
            allowed: false,
            status: 503,
            message: "Kwela is resting for now because the free trial budget is used up. Please come back soon."
        };
    }
    if (snapshot.spentMicros >= limits.capMicros) {
        return {
            allowed: false,
            status: 429,
            message: "You have used all of your free Kwela credit. Thank you for trying it."
        };
    }
    if (snapshot.requestsLastMinute >= limits.maxRequestsPerMinute) {
        return { allowed: false, status: 429, message: "Slow down a little. Try again in a minute." };
    }
    return { allowed: true };
}

/** Reads a positive number from a configuration value.
 * @param value Raw configuration value.
 * @param fallback Value used when the raw value is missing or not a positive number.
 * @returns The parsed number or the fallback.
 */
export function positiveNumber(value: unknown, fallback: number): number {
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}
