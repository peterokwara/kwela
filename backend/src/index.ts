import { createRouter } from "./router.ts";
import { createAuthService } from "./lib/server/services/authService.ts";
import { analyticsFromEnv, internalAccountIds } from "./lib/server/services/analyticsFactory.ts";
import { createGoogleTokenVerifier } from "./lib/server/services/googleIdToken.ts";
import { D1AuthStoreService, type AuthDatabase } from "./lib/server/services/d1AuthStoreService.ts";
import { AllowlistedTestOtpMailer, CloudflareOtpMailer, LogOtpMailer, SelectingOtpMailer, ZeptoMailOtpMailer, type IEmailSender } from "./lib/server/services/otpMailers.ts";
import { createChatPostHandler } from "./lib/server/services/chatPostHandlerService.ts";
import { D1CandidateProfileStoreService, type CandidateProfileDatabase } from "./lib/server/services/d1CandidateProfileStoreService.ts";
import { D1UsageLimitService, type UsageDatabase } from "./lib/server/services/d1UsageLimitService.ts";
import { createOpenAICareerAssistantService } from "./lib/server/services/openAICareerAssistantService.ts";
import { D1ChatRunService } from "./lib/server/services/d1ChatRunService.ts";
export { ChatRunWorkflow } from "./lib/server/services/chatRunWorkflow.ts";
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
    /** Short-lived chat inputs used by durable Workflow runs. */
    RUN_INPUTS: R2Bucket;
    /** Durable execution for chat and document tasks. */
    CHAT_RUNS: Workflow<{ runId: string }>;
    /**
     * Cloudflare Browser Run binding used to render PDFs.
     */
    BROWSER: { quickAction(action: "pdf", options: unknown): Promise<Response> };
    /**
     * OpenAI key, set as a Worker secret.
     */
    OPENAI_API_KEY?: string;
    /**
     * Secret that keys the sign-in code hashes. Set as a Worker secret.
     */
    AUTH_SECRET?: string;
    /**
     * Cloudflare Email Service binding. Present only once a sender domain is set up.
     */
    EMAIL?: IEmailSender;
    /**
     * Sender address on the onboarded domain, for example noreply@example.com.
     */
    EMAIL_FROM?: string;
    /** ZeptoMail Agent Send API key, set as a Worker secret. */
    ZEPTOMAIL_API_KEY?: string;
    /** Regional ZeptoMail email endpoint from the Agent's API setup. */
    ZEPTOMAIL_API_URL?: string;
    /**
     * Comma-separated addresses that may receive codes in the Worker log until real email is set up.
     */
    AUTH_TEST_EMAILS?: string;
    /** Fixed private-test code. Only accepted for AUTH_TEST_EMAILS addresses. Remove before public production. */
    AUTH_TEST_CODE?: string;
    /**
     * OAuth web client id Google signs sign-in tokens for. Google sign-in is off while this is empty.
     */
    GOOGLE_WEB_CLIENT_ID?: string;
    /**
     * PostHog project key for product analytics. Analytics is off while this is empty.
     */
    POSTHOG_API_KEY?: string;
    /**
     * PostHog host. Events go straight there, with no reverse proxy.
     */
    POSTHOG_HOST?: string;
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

/** Google's signing keys are cached inside the verifier, so one verifier is kept for as long as this instance lives. */
let googleVerifier: { clientId: string; verify: ReturnType<typeof createGoogleTokenVerifier> } | null = null;

/** Gives the verifier for a client id, creating it once.
 * @param clientId OAuth web client id.
 * @returns The verifier.
 */
function googleVerifierFor(clientId: string) {
    if (googleVerifier?.clientId !== clientId) googleVerifier = { clientId, verify: createGoogleTokenVerifier({ clientId }) };
    return googleVerifier.verify;
}

export default {
    async fetch(request: Request, env: Env, context: ExecutionContext): Promise<Response> {
        if (!env.AUTH_SECRET) return Response.json({ message: "Kwela is not set up yet." }, { status: 503 });
        const testers = (env.AUTH_TEST_EMAILS ?? "").split(",").map((address) => address.trim().toLowerCase()).filter(Boolean);
        const testCode = env.AUTH_TEST_CODE?.trim() || undefined;
        if (testCode && !/^\d{6}$/.test(testCode)) {
            return Response.json({ message: "Kwela sign-in is not configured correctly." }, { status: 503 });
        }
        const realMailer = env.ZEPTOMAIL_API_KEY && env.EMAIL_FROM
            ? new ZeptoMailOtpMailer(env.ZEPTOMAIL_API_KEY, env.EMAIL_FROM, env.ZEPTOMAIL_API_URL)
            : env.EMAIL && env.EMAIL_FROM
              ? new CloudflareOtpMailer(env.EMAIL, env.EMAIL_FROM)
              : null;
        const analytics = analyticsFromEnv(env);
        const auth = createAuthService({
            store: new D1AuthStoreService(env.DB as unknown as AuthDatabase),
            mailer: testCode
                ? new AllowlistedTestOtpMailer(testers)
                : new SelectingOtpMailer(realMailer, new LogOtpMailer(), testers),
            secret: env.AUTH_SECRET,
            testCode,
            analytics,
            internalIds: () => internalAccountIds(env),
            defer: (work) => context.waitUntil(work),
            googleVerifier: env.GOOGLE_WEB_CLIENT_ID?.trim() ? googleVerifierFor(env.GOOGLE_WEB_CLIENT_ID.trim()) : undefined
        });
        const router = createRouter({
            auth,
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
            chatRuns: new D1ChatRunService(env.DB, env.RUN_INPUTS, env.CHAT_RUNS),
            track: (ownerId, event, properties) => context.waitUntil(analytics.capture(ownerId, event, properties)),
            reserveRender: (ownerId) =>
                new D1UsageLimitService(env.DB as unknown as UsageDatabase, readUsageConfig(env).limits).reserveRender(ownerId),
            renderPdf: (html) =>
                env.BROWSER.quickAction("pdf", {
                    html,
                    pdfOptions: { format: "letter", printBackground: true, preferCSSPageSize: true }
                })
        });
        return router(request);
    },
    async scheduled(_controller: ScheduledController, env: Env, context: ExecutionContext) {
        context.waitUntil(new D1ChatRunService(env.DB, env.RUN_INPUTS, env.CHAT_RUNS).cleanupExpiredInputs());
    }
} satisfies ExportedHandler<Env>;
