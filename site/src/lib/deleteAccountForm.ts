/**
 * Result of reading a form: the cleaned values, or a message to show the person.
 */
export type FormResult<T> = { ok: true; value: T } | { ok: false; message: string };

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

/** Reads the first form, where a person asks for a code.
 * @param form Submitted form data.
 * @returns The tidied email address, or a message.
 */
export function readRequestForm(form: FormData): FormResult<{ email: string }> {
    const email = String(form.get("email") ?? "").trim().toLowerCase();
    if (email.length > 254 || !emailPattern.test(email)) return { ok: false, message: "Enter a valid email address." };
    return { ok: true, value: { email } };
}

/** Reads the second form, where a person enters the code and confirms.
 * @param form Submitted form data.
 * @returns The email and code, or a message.
 */
export function readConfirmForm(form: FormData): FormResult<{ email: string; code: string }> {
    const request = readRequestForm(form);
    if (!request.ok) return request;
    const code = String(form.get("code") ?? "").trim();
    if (!/^\d{6}$/.test(code)) return { ok: false, message: "Enter the 6-digit code from your email." };
    if (form.get("confirm") !== "yes") return { ok: false, message: "Tick the box to confirm you want to delete your account." };
    return { ok: true, value: { email: request.value.email, code } };
}
