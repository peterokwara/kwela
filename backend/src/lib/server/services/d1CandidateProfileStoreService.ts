import type { ICareergoResume } from "../../models/careergo/ICareergoResume.ts";
import type { ICandidateProfileStoreService } from "../../models/services/ICandidateProfileStoreService.ts";

/**
 * D1 methods used by the candidate-profile adapter.
 */
export interface CandidateProfileDatabase {
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
 * Saves one reusable CV profile per browser owner in D1.
 */
export class D1CandidateProfileStoreService implements ICandidateProfileStoreService {
    /**
     * Creates the D1 adapter for candidate profiles.
     * @param database Cloudflare D1 binding.
     */
    constructor(private readonly database: CandidateProfileDatabase) {}

    /**
     * Loads the saved profile for one owner.
     * @param ownerId Owner whose profile is requested.
     * @returns The saved profile, or null when none exists.
     */
    async get(ownerId: string): Promise<ICareergoResume | null> {
        const row = await this.database
            .prepare("SELECT data FROM candidate_profiles WHERE owner_id = ?")
            .bind(ownerId)
            .first<{ data: string }>();
        return row ? (JSON.parse(row.data) as ICareergoResume) : null;
    }

    /**
     * Saves the latest profile extracted from a CV.
     * @param ownerId Owner of the profile.
     * @param profile Careergo-shaped CV data.
     * @returns A promise that resolves when the profile is saved.
     */
    async save(ownerId: string, profile: ICareergoResume): Promise<void> {
        await this.database
            .prepare(
                "INSERT INTO candidate_profiles (owner_id, data, updated_at) VALUES (?, ?, ?) ON CONFLICT(owner_id) DO UPDATE SET data = excluded.data, updated_at = excluded.updated_at"
            )
            .bind(ownerId, JSON.stringify(profile), Date.now())
            .run();
    }

    /**
     * Removes the saved profile for one owner.
     * @param ownerId Owner whose profile is removed.
     * @returns A promise that resolves when the profile is removed.
     */
    async delete(ownerId: string): Promise<void> {
        await this.database.prepare("DELETE FROM candidate_profiles WHERE owner_id = ?").bind(ownerId).run();
    }
}
