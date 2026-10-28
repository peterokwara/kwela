import type { FormResult } from "./deleteAccountForm.ts";

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/** Reads the Support page form.
 * The server checks it again, so this only catches mistakes early.
 * @param form Submitted form data.
 * @returns The email, the message and the hidden field, or a message.
 */
export function readContactForm(form: FormData): FormResult<{ email: string; message: string; website: string }> {
    const email = String(form.get("email") ?? "").trim().toLowerCase();
    if (email.length > 254 || !emailPattern.test(email)) return { ok: false, message: "Enter a valid email address so we can reply." };
    const message = String(form.get("message") ?? "").trim();
    if (message.length < 10 || message.length > 2000) return { ok: false, message: "Write a message of 10 to 2000 characters." };
    // Real people never see the hidden field. It is cut short so a bot cannot send a huge value.
    return { ok: true, value: { email, message, website: String(form.get("website") ?? "").slice(0, 200) } };
}
