import {
    buildClassicCoverLetterHtml,
    buildClassicResumeHtml
} from "./lib/features/careergo/classicDocumentHtmlService.ts";
import { careergoCoverLetterSchema } from "./lib/models/careergo/ICareergoCoverLetter.ts";
import { careergoResumeSchema } from "./lib/models/careergo/ICareergoResume.ts";
import { MAX_PDF_BODY_CHARS, type UsageDecision } from "./lib/server/services/usagePolicy.ts";
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
    /** Durable chat runs that can be resumed after the initiating request ends. */
    chatRuns?: {
        start(request: Request, ownerId: string, idempotencyKey: string): Promise<Response>;
        get(runId: string, ownerId: string): Promise<Response>;
        acknowledge(runId: string, ownerId: string): Promise<Response>;
        deleteOwner(ownerId: string): Promise<void>;
    };
    /**
     * Saved CV profiles, one per owner.
     */
    profiles: ICandidateProfileStoreService;
    /**
     * Turns finished HTML into a PDF response.
     */
    renderPdf(html: string): Promise<Response>;
    /**
     * Checks the account's PDF allowance and records the charge before a render starts.
     */
    reserveRender(ownerId: string): Promise<UsageDecision>;
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
 * @param ownerId Signed-in account, charged for the render.
 * @returns A PDF response, or a JSON error.
 */
async function exportPdf(request: Request, deps: IRouterDependencies, ownerId: string): Promise<Response> {
    const text = await request.text();
    if (text.length > MAX_PDF_BODY_CHARS) return json({ message: "That document is too large to turn into a PDF." }, 413);
    let body: unknown = null;
    try {
        body = JSON.parse(text);
    } catch {
        body = null;
    }
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
    // Checked only once the draft is valid, so a bad request never costs anything.
    const allowance = await deps.reserveRender(ownerId);
    if (!allowance.allowed) return json({ message: allowance.message }, allowance.status);
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
            case "/api/runs":
                if (!deps.chatRuns) return json({ message: "Background replies are not available yet." }, 503);
                if (request.method !== "POST") return methodNotAllowed(["POST"]);
                return deps.chatRuns.start(request, ownerId, request.headers.get("idempotency-key") ?? "");
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
                return request.method === "POST" ? exportPdf(request, deps, ownerId) : methodNotAllowed(["POST"]);
            case "/api/account":
                if (request.method !== "DELETE") return methodNotAllowed(["DELETE"]);
                await deps.chatRuns?.deleteOwner(ownerId);
                await deps.profiles.delete(ownerId);
                await deps.auth.deleteAccount(ownerId, session.email);
                return json({ ok: true });
            default:
                if (pathname.startsWith("/api/runs/")) {
                    if (!deps.chatRuns) return json({ message: "Background replies are not available yet." }, 503);
                    if (pathname.endsWith("/acknowledge")) {
                        if (request.method !== "POST") return methodNotAllowed(["POST"]);
                        const parentId = pathname.slice("/api/runs/".length, -"/acknowledge".length);
                        if (!/^[0-9a-f-]{36}$/i.test(parentId)) return json({ message: "That task could not be found." }, 404);
                        return deps.chatRuns.acknowledge(parentId, ownerId);
                    }
                    if (request.method !== "GET") return methodNotAllowed(["GET"]);
                    const runId = pathname.slice("/api/runs/".length);
                    if (!/^[0-9a-f-]{36}$/i.test(runId)) return json({ message: "That task could not be found." }, 404);
                    return deps.chatRuns.get(runId, ownerId);
                }
                return json({ message: "Not found." }, 404);
        }
    };
}
