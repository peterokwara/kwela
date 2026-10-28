import { env } from "cloudflare:workers";
import { fail } from "@sveltejs/kit";
import { readContactForm } from "#lib/contactForm.ts";
import type { Actions } from "./$types";

const unreachable = "We couldn’t reach Kwela just now. Please try again in a moment.";

export const actions: Actions = {
    /** Sends one Support message to the Kwela API Worker through its service binding. */
    send: async ({ request, getClientAddress }) => {
        const data = await request.formData();
        const form = readContactForm(data);
        const typed = { email: String(data.get("email") ?? "").slice(0, 254), message: String(data.get("message") ?? "").slice(0, 2000) };
        if (!form.ok) return fail(400, { ...typed, error: form.message });
        const secret = env.SIGNUP_SECRET?.trim();
        if (!secret) return fail(503, { ...typed, error: "The contact form is not open yet. Please check back soon." });

        let clientIp = "";
        try {
            clientIp = getClientAddress();
        } catch {
            clientIp = "";
        }
        try {
            const response = await env.API.fetch("https://kwela-api.internal/contact", {
                method: "POST",
                headers: { "content-type": "application/json", "x-signup-token": secret, ...(clientIp ? { "x-client-ip": clientIp } : {}) },
                body: JSON.stringify(form.value)
            });
            if (response.ok) return { sent: true };
            const body = (await response.json().catch(() => ({}))) as { message?: unknown };
            const known = response.status === 400 || response.status === 429;
            // A 403 here means the site and API secrets differ. Log the status only (never the details) so `wrangler tail` shows it.
            if (!known) console.error(`Support message failed: the API answered ${response.status}`);
            return fail(known ? response.status : 502, { ...typed, error: known && typeof body.message === "string" ? body.message : "Something went wrong. Please try again." });
        } catch {
            console.error("Support message failed: the API could not be reached");
            return fail(503, { ...typed, error: unreachable });
        }
    }
};
