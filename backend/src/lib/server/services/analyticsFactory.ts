import { accountIdForEmail } from "./authService.ts";
import { createAnalytics, type IAnalytics } from "./analytics.ts";

/**
 * The settings that turn analytics on and tell internal accounts apart.
 */
export interface IAnalyticsEnv {
    /**
     * PostHog project key. Analytics is off while this is empty.
     */
    POSTHOG_API_KEY?: string;
    /**
     * PostHog host. Events go straight there, with no proxy.
     */
    POSTHOG_HOST?: string;
    /**
     * Server secret, used to work out the opaque ids of the testers.
     */
    AUTH_SECRET?: string;
    /**
     * Comma-separated tester emails. Their events are tagged internal.
     */
    AUTH_TEST_EMAILS?: string;
    /**
     * Database, used to find the stored ids of the testers. Older accounts have random ids rather than keyed ones.
     */
    DB?: IAnalyticsDatabase;
}

/**
 * The small part of the database that analytics needs: the opt-out table and the stored ids of the testers.
 */
export interface IAnalyticsDatabase {
    /**
     * Prepares a parameterized statement.
     */
    prepare(query: string): { bind(...values: unknown[]): { first<T>(): Promise<T | null>; run(): Promise<unknown>; all<T>(): Promise<{ results: T[] }> } };
}

const OPT_OUT_CACHE_MS = 60 * 1000;

/** Builds a lookup of who opted out of usage statistics, remembered for a minute per person.
 * @param database Database holding the opt-out table.
 * @param now Clock in milliseconds.
 * @returns A function saying whether an account opted out.
 */
export function optOutChecker(database: IAnalyticsDatabase, now: () => number = Date.now): (accountId: string) => Promise<boolean> {
    const remembered = new Map<string, { at: number; optedOut: boolean }>();
    return async (accountId) => {
        const known = remembered.get(accountId);
        if (known && now() - known.at < OPT_OUT_CACHE_MS) return known.optedOut;
        const row = await database.prepare("SELECT account_id FROM analytics_opt_outs WHERE account_id = ?").bind(accountId).first<{ account_id: string }>();
        if (remembered.size > 5000) remembered.clear();
        remembered.set(accountId, { at: now(), optedOut: row !== null });
        return row !== null;
    };
}

/** Saves a person's choice about usage statistics.
 * @param database Database holding the opt-out table.
 * @param accountId The account choosing.
 * @param enabled False to stop all usage statistics for the account, true to allow them again.
 * @param time Time of the choice in milliseconds.
 */
export async function setAnalyticsOptOut(database: IAnalyticsDatabase, accountId: string, enabled: boolean, time: number = Date.now()): Promise<void> {
    if (enabled) await database.prepare("DELETE FROM analytics_opt_outs WHERE account_id = ?").bind(accountId).run();
    else await database.prepare("INSERT OR REPLACE INTO analytics_opt_outs (account_id, created_at) VALUES (?, ?)").bind(accountId, time).run();
}

let internalCache: { key: string; at: number; ids: Promise<ReadonlySet<string>> } | null = null;
const INTERNAL_CACHE_MS = 5 * 60 * 1000;

/** The opaque account ids of the team and testers, found once every few minutes.
 * An id is the keyed hash of the email for newer accounts, but a random id for older ones, so the stored ids are looked up too.
 * @param env Analytics settings.
 * @returns The ids.
 */
export function internalAccountIds(env: IAnalyticsEnv): Promise<ReadonlySet<string>> {
    const secret = env.AUTH_SECRET ?? "";
    const testers = (env.AUTH_TEST_EMAILS ?? "").split(",").map((address) => address.trim().toLowerCase()).filter(Boolean);
    const key = `${secret}:${testers.join(",")}`;
    const fresh = internalCache && internalCache.key === key && Date.now() - internalCache.at < INTERNAL_CACHE_MS;
    if (!fresh) {
        const ids = (async () => {
            const found = new Set<string>();
            if (secret) for (const address of testers) found.add(await accountIdForEmail(secret, address));
            if (env.DB && testers.length > 0) {
                try {
                    const marks = testers.map(() => "?").join(", ");
                    const rows = await env.DB.prepare(`SELECT id FROM accounts WHERE email IN (${marks})`).bind(...testers).all<{ id: string }>();
                    for (const row of rows.results) found.add(row.id);
                } catch (cause) {
                    console.warn("Kwela could not look up the tester accounts for analytics:", cause);
                }
            }
            return found as ReadonlySet<string>;
        })();
        internalCache = { key, at: Date.now(), ids };
        // A failed lookup must not be remembered for minutes.
        ids.catch(() => { if (internalCache?.ids === ids) internalCache = null; });
    }
    return internalCache!.ids;
}

/** Builds the PostHog sender from the Worker's settings.
 * @param env Analytics settings.
 * @returns The sender, a quiet no-op when no key is set.
 */
export function analyticsFromEnv(env: IAnalyticsEnv): IAnalytics {
    return createAnalytics({
        apiKey: env.POSTHOG_API_KEY,
        host: env.POSTHOG_HOST?.trim() || "https://us.i.posthog.com",
        internalIds: () => internalAccountIds(env),
        isOptedOut: env.DB ? optOutChecker(env.DB) : undefined
    });
}
