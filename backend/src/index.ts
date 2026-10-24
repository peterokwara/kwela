import { createRouter } from "./router.ts";
import { createChatPostHandler } from "./lib/server/services/chatPostHandlerService.ts";
import { D1CandidateProfileStoreService, type CandidateProfileDatabase } from "./lib/server/services/d1CandidateProfileStoreService.ts";
import { D1UsageLimitService, type UsageDatabase } from "./lib/server/services/d1UsageLimitService.ts";
import { createOpenAICareerAssistantService } from "./lib/server/services/openAICareerAssistantService.ts";
import {
    DEFAULT_MODEL_PRICES,
    DEFAULT_USAGE_LIMITS,
    positiveNumber,
    type IModelPrices,
    type IUsageLimits
} from "./lib/server/services/usagePolicy.ts";

/**
 * Bindings and settings configured for the Worker in wrangler.jsonc and secrets.
 */
export interface Env {
    /**
     * D1 database for usage limits and saved CV profiles.
     */
    DB: D1Database;
    /**
     * Cloudflare Browser Run binding used to render PDFs.
     */
    BROWSER: { quickAction(action: "pdf", options: unknown): Promise<Response> };
    /**
     * OpenAI key, set as a Worker secret.
     */
    OPENAI_API_KEY?: string;
    USAGE_CAP_USD?: string;
    GLOBAL_USAGE_CAP_USD?: string;
    MAX_REQUESTS_PER_MINUTE?: string;
    INPUT_PRICE_PER_MILLION_USD?: string;
    OUTPUT_PRICE_PER_MILLION_USD?: string;
}

/** Reads the spending limits and model prices from the Worker's variables.
 * @param env Worker bindings.
 * @returns Limits and prices with safe defaults.
 */
function readUsageConfig(env: Env): { limits: IUsageLimits; prices: IModelPrices } {
    return {
        limits: {
            capMicros: positiveNumber(env.USAGE_CAP_USD, DEFAULT_USAGE_LIMITS.capMicros / 1_000_000) * 1_000_000,
            globalCapMicros:
                positiveNumber(env.GLOBAL_USAGE_CAP_USD, DEFAULT_USAGE_LIMITS.globalCapMicros / 1_000_000) * 1_000_000,
            maxRequestsPerMinute: positiveNumber(env.MAX_REQUESTS_PER_MINUTE, DEFAULT_USAGE_LIMITS.maxRequestsPerMinute)
        },
        prices: {
            inputPerMillionUsd: positiveNumber(env.INPUT_PRICE_PER_MILLION_USD, DEFAULT_MODEL_PRICES.inputPerMillionUsd),
            outputPerMillionUsd: positiveNumber(env.OUTPUT_PRICE_PER_MILLION_USD, DEFAULT_MODEL_PRICES.outputPerMillionUsd)
        }
    };
}

let postChat: ReturnType<typeof createChatPostHandler> | undefined;

export default {
    async fetch(request: Request, env: Env): Promise<Response> {
        const router = createRouter({
            async chat(chatRequest, ownerId) {
                if (!env.OPENAI_API_KEY) return Response.json({ message: "Kwela’s assistant is not set up yet." }, { status: 503 });
                const { limits, prices } = readUsageConfig(env);
                postChat ??= createChatPostHandler(createOpenAICareerAssistantService(env.OPENAI_API_KEY), {
                    limiter: new D1UsageLimitService(env.DB as unknown as UsageDatabase, limits),
                    prices
                });
                return postChat({ request: chatRequest, ownerId });
            },
            profiles: new D1CandidateProfileStoreService(env.DB as unknown as CandidateProfileDatabase),
            renderPdf: (html) =>
                env.BROWSER.quickAction("pdf", {
                    html,
                    pdfOptions: { format: "letter", printBackground: true, preferCSSPageSize: true }
                })
        });
        return router(request);
    }
} satisfies ExportedHandler<Env>;
