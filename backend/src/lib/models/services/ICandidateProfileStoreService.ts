import type { ICareergoResume } from "../careergo/ICareergoResume.ts";

/**
 * Contract for a browser owner's reusable CV profile.
 */
export interface ICandidateProfileStoreService {
    /**
     * Loads the saved profile for one owner.
     * @param ownerId Owner whose profile is requested.
     * @returns The saved profile, or null when none exists.
     */
    get(ownerId: string): Promise<ICareergoResume | null>;
    /**
     * Saves the latest profile extracted from a CV.
     * @param ownerId Owner of the profile.
     * @param profile Careergo-shaped CV data.
     * @returns A promise that resolves when the profile is saved.
     */
    save(ownerId: string, profile: ICareergoResume): Promise<void>;
    /**
     * Removes the saved profile for one owner.
     * @param ownerId Owner whose profile is removed.
     * @returns A promise that resolves when the profile is removed.
     */
    delete(ownerId: string): Promise<void>;
}
