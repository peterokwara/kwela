import { env } from "cloudflare:workers";
import { fail } from "@sveltejs/kit";
import { readConfirmForm, readRequestForm } from "#lib/deleteAccountForm.ts";
import type { Actions } from "./$types";

/** Calls the Kwela API Worker through its service binding and reads its JSON reply.
 * @param path API path.
 * @param init Request options.
 * @returns The status and parsed body, or null when the API could not be reached.
 */
async function callApi(path: string, init: RequestInit): Promise<{ status: number; body: Record<string, unknown> } | null> {
    try {
        const response = await env.API.fetch(`https://kwela-api.internal${path}`, init);
        const body = (await response.json().catch(() => ({}))) as Record<string, unknown>;
        return { status: response.status, body };
    } catch {
        return null;
    }
}

const unreachable = "We couldn’t reach Kwela just now. Please try again in a moment.";

/** Reads the message the API sent, or falls back to a generic one. */
function apiMessage(body: Record<string, unknown>): string {
    return typeof body.message === "string" ? body.message : "Something went wrong. Please try again.";
}

export const actions: Actions = {
    /** Step one: email a sign-in code to the address. */
    request: async ({ request }) => {
        const form = readRequestForm(await request.formData());
        if (!form.ok) return fail(400, { step: "email", message: form.message });
        const reply = await callApi("/auth/request-code", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ email: form.value.email })
        });
        if (!reply) return fail(503, { step: "email", email: form.value.email, message: unreachable });
        if (reply.status !== 200) return fail(reply.status, { step: "email", email: form.value.email, message: apiMessage(reply.body) });
        return { step: "code", email: form.value.email };
    },

    /** Step two: check the code, then delete the account. */
    confirm: async ({ request }) => {
        const data = await request.formData();
        const form = readConfirmForm(data);
        if (!form.ok) return fail(400, { step: "code", email: String(data.get("email") ?? ""), message: form.message });
        const { email, code } = form.value;

        const verified = await callApi("/auth/verify", {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ email, code })
        });
        if (!verified) return fail(503, { step: "code", email, message: unreachable });
        if (verified.status !== 200 || typeof verified.body.token !== "string") {
            return fail(verified.status === 200 ? 502 : verified.status, { step: "code", email, message: apiMessage(verified.body) });
        }

        const deleted = await callApi("/api/account", { method: "DELETE", headers: { authorization: `Bearer ${verified.body.token}` } });
        if (!deleted) return fail(503, { step: "code", email, message: unreachable });
        if (deleted.status !== 200) return fail(deleted.status, { step: "code", email, message: apiMessage(deleted.body) });
        return { step: "done", email };
    }
};
