import { env } from "cloudflare:workers";
import { fail } from "@sveltejs/kit";
import { readJoinForm } from "#lib/joinForm.ts";
import type { Actions } from "./$types";

const unreachable = "We couldn’t reach Kwela just now. Please try again in a moment.";

export const actions: Actions = {
    /** Sends one tester sign-up to the Kwela API Worker through its service binding. */
    join: async ({ request, getClientAddress }) => {
        const data = await request.formData();
        const form = readJoinForm(data);
        const typed = { email: String(data.get("email") ?? "").slice(0, 254), phone: String(data.get("phone") ?? "").slice(0, 32) };
        if (!form.ok) return fail(400, { ...typed, message: form.message });
        const secret = env.SIGNUP_SECRET?.trim();
        if (!secret) return fail(503, { ...typed, message: "Tester sign-ups are not open yet. Please check back soon." });

        let clientIp = "";
        try {
            clientIp = getClientAddress();
        } catch {
            clientIp = "";
        }
        try {
            const response = await env.API.fetch("https://kwela-api.internal/signup", {
                method: "POST",
                headers: {
                    "content-type": "application/json",
                    "x-signup-token": secret,
                    ...(clientIp ? { "x-client-ip": clientIp } : {})
                },
                body: JSON.stringify({ ...form.value, consent: true })
            });
            if (response.ok) return { done: true };
            const body = (await response.json().catch(() => ({}))) as { message?: unknown };
            const known = response.status === 400 || response.status === 429;
            // A 403 here means the site and API secrets differ. Log the status only (never the details) so `wrangler tail` shows it.
            if (!known) console.error(`Tester sign-up failed: the API answered ${response.status}`);
            const message = known && typeof body.message === "string" ? body.message : "Something went wrong. Please try again.";
            return fail(known ? response.status : 502, { ...typed, message });
        } catch {
            console.error("Tester sign-up failed: the API could not be reached");
            return fail(503, { ...typed, message: unreachable });
        }
    }
};
