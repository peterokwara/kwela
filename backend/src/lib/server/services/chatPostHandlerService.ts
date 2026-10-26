import type { ICareerAssistant } from "../../models/services/ICareerAssistant.ts";
import type { IUsageLimitService } from "../../models/services/IUsageLimitService.ts";
import { AssistantFailedError } from "./assistantFailedError.ts";
import { readChatMessages } from "./chatRequestValidationService.ts";
import { estimateCostMicros, MINIMUM_CHARGE_MICROS, type IModelPrices } from "./usagePolicy.ts";

type ChatRequestEvent = { request: Request; ownerId?: string };

/**
 * Optional spending controls for the chat endpoint.
 */
export interface IChatUsageControls {
    /**
     * Service that enforces and records usage.
     */
    limiter: IUsageLimitService;
    /**
     * Model prices used to estimate each request's cost.
     */
    prices: IModelPrices;
}

/** Creates the HTTP handler that validates chat input and streams an assistant reply.
 * @param assistant Career assistant used to produce the reply.
 * @param usage Optional limits that cap how much each visitor can use.
 * @returns A POST handler for the chat endpoint.
 */
export function createChatPostHandler(assistant: ICareerAssistant, usage?: IChatUsageControls) {
    return async ({ request, ownerId }: ChatRequestEvent): Promise<Response> => {
        const result = await readChatMessages(request);
        if ("response" in result) return result.response;

        if (usage) {
            try {
                if (!ownerId) throw new Error("Missing visitor id.");
                const decision = await usage.limiter.reserve(ownerId);
                if (!decision.allowed) return Response.json({ message: decision.message }, { status: decision.status });
            } catch (cause) {
                console.error("Kwela usage check failed:", cause);
                return Response.json(
                    { message: "Kwela cannot start a reply right now. Please try again in a moment." },
                    { status: 503 }
                );
            }
        }

        let costMicros = 0;
        try {
            const stream = await assistant.respond(result.messages, (used) => {
                costMicros += used && usage ? estimateCostMicros(used, usage.prices) : MINIMUM_CHARGE_MICROS;
            }, result.conversationContext);
            return new Response(stream, {
                headers: {
                    "content-type": "text/plain; charset=utf-8",
                    "cache-control": "no-cache, no-transform",
                    "x-content-type-options": "nosniff"
                }
            });
        } catch (cause) {
            console.error("Kwela assistant request failed:", cause);
            // A timeout is answered with a 4xx so the durable run stops at once instead of waiting through a second identical attempt.
            if (cause instanceof AssistantFailedError && cause.timedOut) {
                return Response.json({ message: "That one took too long. Tap Try again, or send a shorter job text." }, { status: 408 });
            }
            return Response.json(
                { message: "Couldn’t finish that one. Tap Try again." },
                { status: 500 }
            );
        } finally {
            if (usage && ownerId) await usage.limiter.settle(ownerId, costMicros).catch(() => undefined);
        }
    };
}
