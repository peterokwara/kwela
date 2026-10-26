/**
 * A person Google has vouched for.
 */
export interface IGoogleIdentity {
    /**
     * Verified email address, in lowercase.
     */
    email: string;
    /**
     * Display name Google holds for the person, or an empty string.
     */
    name: string;
}

/**
 * Google's published signing keys, in JSON Web Key Set form.
 */
export interface IGoogleKeySet {
    /**
     * The keys.
     */
    keys: JsonWebKey[];
}

/**
 * Dependencies of the verifier.
 */
export interface IGoogleVerifierOptions {
    /**
     * The OAuth web client id the app asked Google to issue tokens for. A token for any other app is refused.
     */
    clientId: string;
    /**
     * Fetches Google's current signing keys. Swappable so tests need no network.
     */
    fetchKeys?: () => Promise<IGoogleKeySet>;
    /**
     * Clock in milliseconds since the epoch.
     */
    now?: () => number;
}

const GOOGLE_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";
const KEY_CACHE_MS = 60 * 60 * 1000;
const MIN_REFETCH_MS = 60 * 1000;
const FETCH_TIMEOUT_MS = 5000;
const CLOCK_SKEW_SECONDS = 60;
const ISSUERS = new Set(["https://accounts.google.com", "accounts.google.com"]);

type KeyWithId = JsonWebKey & { kid?: string };

/** Decodes a base64url segment to bytes.
 * @param segment Base64url text.
 * @returns The bytes.
 */
function fromBase64Url(segment: string): Uint8Array<ArrayBuffer> {
    const padded = segment.replaceAll("-", "+").replaceAll("_", "/").padEnd(Math.ceil(segment.length / 4) * 4, "=");
    const binary = atob(padded);
    const bytes = new Uint8Array(new ArrayBuffer(binary.length));
    for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index);
    return bytes;
}

/** Reads a JSON segment of a token.
 * @param segment Base64url text.
 * @returns The parsed object, or null when it is not a JSON object.
 */
function readSegment(segment: string): Record<string, unknown> | null {
    try {
        const value: unknown = JSON.parse(new TextDecoder().decode(fromBase64Url(segment)));
        return value && typeof value === "object" && !Array.isArray(value) ? (value as Record<string, unknown>) : null;
    } catch {
        return null;
    }
}

/** Fetches Google's signing keys over the network.
 * @returns The key set.
 */
async function fetchGoogleKeys(): Promise<IGoogleKeySet> {
    const response = await fetch(GOOGLE_KEYS_URL, { signal: AbortSignal.timeout(FETCH_TIMEOUT_MS) });
    if (!response.ok) throw new Error(`Google keys request failed with ${response.status}.`);
    return (await response.json()) as IGoogleKeySet;
}

/** Creates a function that checks a Google ID token and says who it belongs to.
 * It checks the signature against Google's keys, the issuer, that the token was made for this app, that it has not expired, and that Google verified the email.
 * @param options Client id, key source and clock.
 * @returns A verifier that returns the person, or null for anything it cannot fully trust. It never throws on a bad token.
 */
export function createGoogleTokenVerifier(options: IGoogleVerifierOptions): (idToken: string) => Promise<IGoogleIdentity | null> {
    const { clientId } = options;
    const now = options.now ?? Date.now;
    const fetchKeys = options.fetchKeys ?? fetchGoogleKeys;
    let cached: { keys: KeyWithId[]; fetchedAt: number } | null = null;

    async function keyFor(kid: string): Promise<KeyWithId | null> {
        const fresh = cached && now() - cached.fetchedAt < KEY_CACHE_MS;
        if (fresh) {
            const known = cached!.keys.find((key) => key.kid === kid);
            if (known) return known;
        }
        // An unknown key id may mean Google rotated its keys, so look again. But the id comes from an unchecked token, so never more than once a minute.
        if (cached && now() - cached.fetchedAt < MIN_REFETCH_MS) return null;
        const keySet = await fetchKeys();
        cached = { keys: keySet.keys as KeyWithId[], fetchedAt: now() };
        return cached.keys.find((key) => key.kid === kid) ?? null;
    }

    return async (idToken) => {
        try {
            const parts = idToken.split(".");
            if (parts.length !== 3 || parts.some((part) => part.length === 0)) return null;
            const header = readSegment(parts[0]);
            const claims = readSegment(parts[1]);
            if (!header || !claims || header.alg !== "RS256" || typeof header.kid !== "string") return null;

            const jwk = await keyFor(header.kid);
            if (!jwk) return null;
            const key = await crypto.subtle.importKey("jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
            const signed = new TextEncoder().encode(`${parts[0]}.${parts[1]}`);
            const signatureOk = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", key, fromBase64Url(parts[2]), signed);
            if (!signatureOk) return null;

            const seconds = now() / 1000;
            const audience = claims.aud;
            const forThisApp = audience === clientId || (Array.isArray(audience) && audience.includes(clientId));
            if (typeof claims.iss !== "string" || !ISSUERS.has(claims.iss) || !forThisApp) return null;
            if (typeof claims.exp !== "number" || claims.exp + CLOCK_SKEW_SECONDS < seconds) return null;
            if (typeof claims.iat === "number" && claims.iat - CLOCK_SKEW_SECONDS > seconds) return null;
            if (claims.email_verified !== true || typeof claims.email !== "string" || claims.email.length === 0) return null;
            return { email: claims.email.trim().toLowerCase(), name: typeof claims.name === "string" ? claims.name.trim() : "" };
        } catch (cause) {
            console.error("Kwela could not check a Google token:", cause);
            return null;
        }
    };
}
