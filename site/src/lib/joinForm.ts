import type { FormResult } from "./deleteAccountForm.ts";

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/** Reads the tester sign-up form.
 * The server checks and tidies the phone number, because it owns the rules, so this only catches mistakes early.
 * @param form Submitted form data.
 * @returns The email, the phone number as typed, and the hidden field, or a message.
 */
export function readJoinForm(form: FormData): FormResult<{ email: string; phone: string; website: string }> {
    const email = String(form.get("email") ?? "").trim().toLowerCase();
    if (email.length > 254 || !emailPattern.test(email)) return { ok: false, message: "Enter a valid email address." };
    const phone = String(form.get("phone") ?? "").trim();
    if (phone.length === 0 || phone.length > 32) return { ok: false, message: "Enter a phone number we can reach you on, like 0712 345 678. Start with + for other countries." };
    // Real people never see the hidden field. It is cut short so a bot cannot send a huge value.
    return { ok: true, value: { email, phone, website: String(form.get("website") ?? "").slice(0, 200) } };
}
