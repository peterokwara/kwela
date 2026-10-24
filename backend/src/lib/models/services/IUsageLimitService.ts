import type { UsageDecision } from "../../server/services/usagePolicy.ts";

/**
 * Port that limits how much each visitor may use Kwela.
 */
export interface IUsageLimitService {
    /**
     * Checks the limits and, when allowed, holds a minimum charge for the request.
     * @param ownerId Anonymous visitor identifier.
     */
    reserve(ownerId: string): Promise<UsageDecision>;
    /**
     * Records the rest of a request's real cost after it finishes.
     * @param ownerId Anonymous visitor identifier.
     * @param costMicros Total cost of the request in millionths of a US dollar.
     */
    settle(ownerId: string, costMicros: number): Promise<void>;
}
