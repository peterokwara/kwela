import type { IUsageLimitService } from "../../models/services/IUsageLimitService.ts";
import {
    evaluateUsage,
    MAX_RENDERS_PER_MINUTE,
    MINIMUM_CHARGE_MICROS,
    RENDER_CHARGE_MICROS,
    type IUsageLimits,
    type IUsageSnapshot,
    type UsageDecision
} from "./usagePolicy.ts";

/**
 * D1 methods used by the usage-limit adapter.
 */
export interface UsageDatabase {
    /**
     * Prepares a parameterized D1 statement.
     */
    prepare(query: string): {
        /**
         * Binds values and runs or reads the statement.
         */
        bind(...values: unknown[]): {
            first<T>(): Promise<T | null>;
            run(): Promise<unknown>;
        };
    };
}

/**
 * Tracks each visitor's requests and spending in D1.
 */
export class D1UsageLimitService implements IUsageLimitService {
    private readonly database: UsageDatabase;
    private readonly limits: IUsageLimits;
    private readonly now: () => number;

    /**
     * Creates the D1 adapter for usage limits.
     * @param database Cloudflare D1 binding.
     * @param limits Limits to apply.
     * @param now Clock used for the one-minute window.
     */
    constructor(database: UsageDatabase, limits: IUsageLimits, now: () => number = Date.now) {
        this.database = database;
        this.limits = limits;
        this.now = now;
    }

    /**
     * Checks the limits and, when allowed, records the minimum charge for the request.
     * @param ownerId Anonymous visitor identifier.
     * @returns Whether the request may start.
     */
    async reserve(ownerId: string): Promise<UsageDecision> {
        // Record first so simultaneous requests can see each other, then roll back if refused.
        const id = await this.record(ownerId, MINIMUM_CHARGE_MICROS, "request");
        if (id === null) throw new Error("Could not record the usage reservation.");
        const snapshot = await this.snapshot(ownerId);
        const decision = evaluateUsage(
            {
                requestsLastMinute: snapshot.requestsLastMinute - 1,
                spentMicros: snapshot.spentMicros - MINIMUM_CHARGE_MICROS,
                globalSpentMicros: snapshot.globalSpentMicros - MINIMUM_CHARGE_MICROS
            },
            this.limits
        );
        if (!decision.allowed) await this.database.prepare("DELETE FROM usage_events WHERE id = ?").bind(id).run();
        return decision;
    }

    /**
     * Checks the limits for one PDF render and, when allowed, records its charge. Renders have their own per-minute allowance, so they never use up the chat's.
     * @param ownerId Account identifier.
     * @returns Whether the render may start.
     */
    async reserveRender(ownerId: string): Promise<UsageDecision> {
        const id = await this.record(ownerId, RENDER_CHARGE_MICROS, "render");
        if (id === null) throw new Error("Could not record the render reservation.");
        const snapshot = await this.snapshot(ownerId);
        const decision: UsageDecision =
            (snapshot.rendersLastMinute ?? 0) - 1 >= MAX_RENDERS_PER_MINUTE
                ? { allowed: false, status: 429, message: "Slow down a little. Try again in a minute." }
                : evaluateUsage(
                      {
                          requestsLastMinute: 0,
                          spentMicros: snapshot.spentMicros - RENDER_CHARGE_MICROS,
                          globalSpentMicros: snapshot.globalSpentMicros - RENDER_CHARGE_MICROS
                      },
                      this.limits
                  );
        if (!decision.allowed) await this.database.prepare("DELETE FROM usage_events WHERE id = ?").bind(id).run();
        return decision;
    }

    /**
     * Records the part of a request's cost above the minimum already held.
     * @param ownerId Anonymous visitor identifier.
     * @param costMicros Total cost of the request in millionths of a US dollar.
     */
    async settle(ownerId: string, costMicros: number): Promise<void> {
        const extra = Math.ceil(costMicros) - MINIMUM_CHARGE_MICROS;
        if (extra > 0) await this.record(ownerId, extra, "adjustment");
    }

    /**
     * Reads this visitor's recent use and the overall total.
     */
    private async snapshot(ownerId: string): Promise<IUsageSnapshot> {
        const owner = await this.database
            .prepare(
                "SELECT COALESCE(SUM(cost_micros), 0) AS spent, COALESCE(SUM(CASE WHEN kind = 'request' AND created_at > ? THEN 1 ELSE 0 END), 0) AS recent, COALESCE(SUM(CASE WHEN kind = 'render' AND created_at > ? THEN 1 ELSE 0 END), 0) AS renders FROM usage_events WHERE owner_id = ?"
            )
            .bind(this.now() - 60_000, this.now() - 60_000, ownerId)
            .first<{ spent: number; recent: number; renders: number }>();
        const global = await this.database
            .prepare("SELECT COALESCE(SUM(cost_micros), 0) AS spent FROM usage_events")
            .bind()
            .first<{ spent: number }>();
        return {
            requestsLastMinute: owner?.recent ?? 0,
            rendersLastMinute: owner?.renders ?? 0,
            spentMicros: owner?.spent ?? 0,
            globalSpentMicros: global?.spent ?? 0
        };
    }

    /**
     * Stores one usage row.
     */
    private async record(ownerId: string, costMicros: number, kind: "request" | "adjustment" | "render"): Promise<number | null> {
        const row = await this.database
            .prepare(
                "INSERT INTO usage_events (owner_id, created_at, cost_micros, kind) VALUES (?, ?, ?, ?) RETURNING id"
            )
            .bind(ownerId, this.now(), costMicros, kind)
            .first<{ id: number }>();
        return row?.id ?? null;
    }
}
