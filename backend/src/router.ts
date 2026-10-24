import {
    buildClassicCoverLetterHtml,
    buildClassicResumeHtml
} from "./lib/features/careergo/classicDocumentHtmlService.ts";
import { careergoCoverLetterSchema } from "./lib/models/careergo/ICareergoCoverLetter.ts";
import { careergoResumeSchema } from "./lib/models/careergo/ICareergoResume.ts";
import type { ICandidateProfileStoreService } from "./lib/models/services/ICandidateProfileStoreService.ts";

/**
 * Everything the router needs from the outside world, so it can be tested without Cloudflare.
 */
export interface IRouterDependencies {
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

/** Derives the stable owner id for a request from its install token.
 * @param request Incoming request.
 * @returns A SHA-256 hex id, or null when the token is missing or malformed.
 */
async function ownerIdFrom(request: Request): Promise<string | null> {
    const match = tokenPattern.exec(request.headers.get("authorization") ?? "");
    if (!match) return null;
    const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(match[1]));
    return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
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
    return async (request) => {
        const { pathname } = new URL(request.url);
        if (pathname === "/health") return json({ ok: true });
        if (!pathname.startsWith("/api/")) return json({ message: "Not found." }, 404);

        const ownerId = await ownerIdFrom(request);
        if (!ownerId) return json({ message: "Sign in again to continue." }, 401);

        switch (pathname) {
            case "/api/chat":
                return request.method === "POST" ? deps.chat(request, ownerId) : methodNotAllowed(["POST"]);
            case "/api/candidate-profile":
                if (request.method === "GET") return json({ profile: await deps.profiles.get(ownerId) });
                if (request.method === "PUT") {
                    const body = (await request.json().catch(() => null)) as { profile?: unknown } | null;
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
                return json({ ok: true });
            default:
                return json({ message: "Not found." }, 404);
        }
    };
}
