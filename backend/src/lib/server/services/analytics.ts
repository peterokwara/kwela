/**
 * Sends product events to PostHog from the server, where they cannot be blocked or lost with a closed app.
 */
export interface IAnalytics {
    /**
     * Records one event for a person. Never throws and never waits long, so analytics can never slow or break a request.
     */
    capture(distinctId: string, event: string, properties?: Record<string, unknown>): Promise<void>;
}

/**
 * Settings of the PostHog sender.
 */
export interface IAnalyticsOptions {
    /**
     * PostHog project key. Without one, events are dropped.
     */
    apiKey?: string;
    /**
     * PostHog host, such as https://us.i.posthog.com. Events go straight there, with no proxy.
     */
    host: string;
    /**
     * Account ids of the team and testers, so their events can be filtered out of the numbers.
     */
    internalIds?: () => Promise<ReadonlySet<string>>;
    /**
     * Says whether a person has switched usage statistics off. Their events are never sent. If this cannot answer, nothing is sent.
     */
    isOptedOut?: (distinctId: string) => Promise<boolean>;
    /**
     * HTTP client. Swappable so tests need no network.
     */
    fetcher?: typeof fetch;
    /**
     * Clock in milliseconds since the epoch.
     */
    now?: () => number;
}

const CAPTURE_TIMEOUT_MS = 2000;

/** Creates the PostHog sender.
 * @param options Key, host and helpers.
 * @returns A sender that is a quiet no-op when no key is set.
 */
export function createAnalytics(options: IAnalyticsOptions): IAnalytics {
    const apiKey = options.apiKey?.trim();
    const fetcher = options.fetcher ?? fetch;
    const now = options.now ?? Date.now;
    const host = options.host.replace(/\/+$/, "");
    return {
        async capture(distinctId, event, properties = {}) {
            if (!apiKey) return;
            try {
                if (options.isOptedOut && (await options.isOptedOut(distinctId))) return;
                const internal = options.internalIds ? (await options.internalIds()).has(distinctId) : false;
                const response = await fetcher(`${host}/i/v0/e/`, {
                    method: "POST",
                    headers: { "content-type": "application/json" },
                    body: JSON.stringify({
                        api_key: apiKey,
                        event,
                        distinct_id: distinctId,
                        properties: { ...properties, $lib: "kwela-api", is_internal_user: internal },
                        timestamp: new Date(now()).toISOString()
                    }),
                    signal: AbortSignal.timeout(CAPTURE_TIMEOUT_MS)
                });
                if (!response.ok) console.warn(`PostHog refused an event with status ${response.status}.`);
            } catch (cause) {
                console.warn("PostHog could not be reached:", cause);
            }
        }
    };
}

/** Summarises a finished reply for analytics by what it holds, never by what it says.
 * @param replyJson The reply as stored, with its message and any application pack.
 * @returns Which documents came back, or null when the reply held no application.
 */
export function describeReplyForAnalytics(replyJson: string): { has_resume: boolean; has_cover_letter: boolean; answer_count: number } | null {
    try {
        const reply = JSON.parse(replyJson) as { applicationPack?: { resume?: unknown; coverLetter?: unknown; applicationAnswers?: unknown } | null };
        const pack = reply.applicationPack;
        if (!pack || typeof pack !== "object") return null;
        return {
            has_resume: Boolean(pack.resume),
            has_cover_letter: Boolean(pack.coverLetter),
            answer_count: Array.isArray(pack.applicationAnswers) ? pack.applicationAnswers.length : 0
        };
    } catch {
        return null;
    }
}
