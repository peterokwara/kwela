import {
    buildClassicCoverLetterHtml,
    buildClassicResumeHtml
} from "./lib/features/careergo/classicDocumentHtmlService.ts";
import { careergoCoverLetterSchema } from "./lib/models/careergo/ICareergoCoverLetter.ts";
import { careergoResumeSchema } from "./lib/models/careergo/ICareergoResume.ts";
import type { IResolvedSession } from "./lib/models/services/IAuthStore.ts";
import type { ICandidateProfileStoreService } from "./lib/models/services/ICandidateProfileStoreService.ts";

/**
 * Everything the router needs from the outside world, so it can be tested without Cloudflare.
 */
export interface IRouterDependencies {
    /**
     * Email sign-in and sessions.
     */
    auth: {
        requestCode(email: string, sourceIp?: string): Promise<{ ok: true; testCode?: string } | { ok: false; status: number; message: string }>;
        verify(
            email: string,
            code: string
        ): Promise<{ ok: true; token: string; email: string } | { ok: false; status: number; message: string }>;
        authenticate(token: string): Promise<IResolvedSession | null>;
        signOut(token: string): Promise<void>;
        deleteAccount(accountId: string, email: string): Promise<void>;
    };
    /**
     * Runs one chat turn for the signed-in owner and returns the streamed reply.
     */
    chat(request: Request, ownerId: string): Promise<Response>;
    /**
     * Saved CV profiles, one per owner.
     */
    profiles: ICandidateProfileStoreService;
    /**
     * Turns finished HTML into a PDF response.
     */
    renderPdf(html: string): Promise<Response>;
}

const tokenPattern = /^Bearer ([A-Za-z0-9_-]{32,128})$/;

/** Builds a JSON response in the shape every Kwela error uses.
 * @param body JSON body.
 * @param status HTTP status code.
 * @returns A JSON response.
 */
function json(body: unknown, status = 200): Response {
    return Response.json(body, { status });
}

/** Reads the bearer token from a request.
 * @param request Incoming request.
 * @returns The token, or null when it is missing or malformed.
 */
function bearerToken(request: Request): string | null {
    return tokenPattern.exec(request.headers.get("authorization") ?? "")?.[1] ?? null;
}

/** Reads a JSON object body.
 * @param request Incoming request.
 * @returns The object, or null when the body is not a JSON object.
 */
async function readObject(request: Request): Promise<Record<string, unknown> | null> {
    const body: unknown = await request.json().catch(() => null);
    return body && typeof body === "object" && !Array.isArray(body) ? (body as Record<string, unknown>) : null;
}

/** Answers a request that used a method the route does not accept.
 * @param allowed Methods the route accepts.
 * @returns A 405 response.
 */
function methodNotAllowed(allowed: string[]): Response {
    return Response.json({ message: "Method not allowed." }, { status: 405, headers: { allow: allowed.join(", ") } });
}

/** Renders a CV or cover letter to PDF.
 * @param request Request whose body holds the document type and draft.
 * @param deps Router dependencies.
 * @returns A PDF response, or a JSON error.
 */
async function exportPdf(request: Request, deps: IRouterDependencies): Promise<Response> {
    const body: unknown = await request.json().catch(() => null);
    if (!body || typeof body !== "object" || !("type" in body) || !("document" in body)) {
        return json({ message: "Choose a CV or cover letter to export." }, 400);
    }
    let html: string;
    let filename: string;
    if (body.type === "resume") {
        const parsed = careergoResumeSchema.safeParse(body.document);
        if (!parsed.success) return json({ message: "The CV draft is incomplete." }, 400);
        html = buildClassicResumeHtml(parsed.data);
        filename = "kwela-cv.pdf";
    } else if (body.type === "cover_letter") {
        const parsed = careergoCoverLetterSchema.safeParse(body.document);
        if (!parsed.success) return json({ message: "The cover letter draft is incomplete." }, 400);
        html = buildClassicCoverLetterHtml(parsed.data);
        filename = "kwela-cover-letter.pdf";
    } else {
        return json({ message: "Choose a CV or cover letter to export." }, 400);
    }
    try {
        const pdf = await deps.renderPdf(html);
        if (!pdf.ok) return json({ message: "The PDF renderer could not prepare this file." }, 503);
        const headers = new Headers(pdf.headers);
        headers.set("content-type", "application/pdf");
        headers.set("content-disposition", `attachment; filename="${filename}"`);
        headers.set("cache-control", "no-store");
        return new Response(pdf.body, { status: pdf.status, headers });
    } catch {
        return json({ message: "Kwela could not render this PDF. Please try again." }, 503);
    }
}

/** Creates the request router for the Kwela API.
 * @param deps Chat, storage and PDF dependencies.
 * @returns A function that answers one request.
 */
export function createRouter(deps: IRouterDependencies): (request: Request) => Promise<Response> {
    /** Handles the unauthenticated sign-in routes. */
    async function authRoute(request: Request, pathname: string): Promise<Response> {
        if (pathname === "/auth/sign-out") {
            if (request.method !== "POST") return methodNotAllowed(["POST"]);
            const token = bearerToken(request);
            if (!token) return json({ message: "Sign in again to continue." }, 401);
            await deps.auth.signOut(token);
            return json({ ok: true });
        }
        if (pathname !== "/auth/request-code" && pathname !== "/auth/verify") return json({ message: "Not found." }, 404);
        if (request.method !== "POST") return methodNotAllowed(["POST"]);
        const body = await readObject(request);
        if (pathname === "/auth/request-code") {
            if (typeof body?.email !== "string") return json({ message: "Enter a valid email address." }, 400);
            const result = await deps.auth.requestCode(body.email, request.headers.get("cf-connecting-ip") ?? undefined);
            return result.ok
                ? json({ ok: true, ...(result.testCode ? { testCode: result.testCode } : {}) })
                : json({ message: result.message }, result.status);
        }
        if (typeof body?.email !== "string" || typeof body.code !== "string") {
            return json({ message: "Enter the 6-digit code from your email." }, 400);
        }
        const result = await deps.auth.verify(body.email, body.code);
        return result.ok ? json({ token: result.token, email: result.email }) : json({ message: result.message }, result.status);
    }

    return async (request) => {
        const { pathname } = new URL(request.url);
        if (pathname === "/health") return json({ ok: true });
        if (pathname.startsWith("/auth/")) return authRoute(request, pathname);
        if (!pathname.startsWith("/api/")) return json({ message: "Not found." }, 404);

        const token = bearerToken(request);
        const session = token ? await deps.auth.authenticate(token) : null;
        if (!session) return json({ message: "Sign in again to continue." }, 401);
        const ownerId = session.accountId;

        switch (pathname) {
            case "/api/chat":
                return request.method === "POST" ? deps.chat(request, ownerId) : methodNotAllowed(["POST"]);
            case "/api/candidate-profile":
                if (request.method === "GET") return json({ profile: await deps.profiles.get(ownerId) });
                if (request.method === "PUT") {
                    const body = await readObject(request);
                    const profile = careergoResumeSchema.safeParse(body?.profile);
                    if (!profile.success) return json({ message: "The CV profile could not be saved." }, 400);
                    await deps.profiles.save(ownerId, profile.data);
                    return json({ ok: true });
                }
                if (request.method === "DELETE") {
                    await deps.profiles.delete(ownerId);
                    return json({ ok: true });
                }
                return methodNotAllowed(["GET", "PUT", "DELETE"]);
            case "/api/documents/pdf":
                return request.method === "POST" ? exportPdf(request, deps) : methodNotAllowed(["POST"]);
            case "/api/account":
                if (request.method !== "DELETE") return methodNotAllowed(["DELETE"]);
                await deps.profiles.delete(ownerId);
                await deps.auth.deleteAccount(ownerId, session.email);
                return json({ ok: true });
            default:
                return json({ message: "Not found." }, 404);
        }
    };
}
