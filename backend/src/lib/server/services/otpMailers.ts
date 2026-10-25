import type { IOtpMailer } from "../../models/services/IOtpMailer.ts";

/**
 * Cloudflare Email Service binding, reduced to what the mailer uses.
 */
export interface IEmailSender {
    /**
     * Sends one message through Cloudflare Email Service.
     */
    send(message: { to: string; from: string | { email: string; name: string }; subject: string; text: string; html: string }): Promise<unknown>;
}

/** Builds the sign-in email.
 * @param code Six-digit code.
 * @returns Subject, plain text and HTML bodies.
 */
export function otpMessage(code: string): { subject: string; text: string; html: string } {
    const text = `Your Kwela sign-in code is ${code}.\n\nIt works for 10 minutes. If you did not ask for it, you can ignore this email.`;
    const html = `<p>Your Kwela sign-in code is</p><p style="font-size:28px;letter-spacing:4px;font-weight:700">${code}</p><p>It works for 10 minutes. If you did not ask for it, you can ignore this email.</p>`;
    return { subject: `${code} is your Kwela sign-in code`, text, html };
}

/**
 * Sends sign-in codes through Cloudflare Email Service. Needs a sender address on an onboarded domain.
 */
export class CloudflareOtpMailer implements IOtpMailer {
    private readonly sender: IEmailSender;
    private readonly from: string;
    /**
     * Creates the Cloudflare mailer.
     * @param sender Cloudflare send_email binding.
     * @param from Sender address on an onboarded domain.
     */
    constructor(
        sender: IEmailSender,
        from: string
    ) {
        this.sender = sender;
        this.from = from;
    }

    /**
     * Sends a sign-in code.
     * @param email Recipient address.
     * @param code Six-digit code.
     */
    async send(email: string, code: string): Promise<void> {
        await this.sender.send({ to: email, from: { email: this.from, name: "Kwela" }, ...otpMessage(code) });
    }
}

/** Sends sign-in codes through ZeptoMail's transactional email API. */
export class ZeptoMailOtpMailer implements IOtpMailer {
    private readonly apiKey: string;
    private readonly from: string;
    private readonly endpoint: string;
    private readonly fetcher: typeof fetch;
    /**
     * Creates a ZeptoMail API mailer.
     * @param apiKey Agent-specific ZeptoMail Send API key.
     * @param from Verified sender address.
     * @param endpoint Regional ZeptoMail send endpoint.
     * @param fetcher Fetch implementation, injectable for tests.
     */
    constructor(
        apiKey: string,
        from: string,
        endpoint = "https://cpaas.zoho.com/v1.1/email",
        fetcher: typeof fetch = fetch
    ) {
        this.apiKey = apiKey;
        this.from = from;
        this.endpoint = endpoint;
        this.fetcher = fetcher;
    }

    /**
     * Sends a sign-in code.
     * @param email Recipient address.
     * @param code Six-digit code.
     */
    async send(email: string, code: string): Promise<void> {
        const message = otpMessage(code);
        const response = await this.fetcher(this.endpoint, {
            method: "POST",
            headers: {
                Authorization: `Zoho-enczapikey ${this.apiKey}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                from: { address: this.from, name: "Kwela" },
                to: [{ email_address: { address: email } }],
                subject: message.subject,
                textbody: message.text,
                htmlbody: message.html,
                track_clicks: false,
                track_opens: false
            })
        });

        if (!response.ok) {
            const detail = await response.text().catch(() => "");
            throw new Error(`ZeptoMail rejected the sign-in email (${response.status}): ${detail.slice(0, 300)}`);
        }
    }
}

/**
 * Writes sign-in codes to the Worker's private log. Used only for allow-listed testers until a sender domain exists.
 */
export class LogOtpMailer implements IOtpMailer {
    /**
     * Logs a sign-in code.
     * @param email Recipient address.
     * @param code Six-digit code.
     */
    async send(email: string, code: string): Promise<void> {
        console.log(`KWELA_SIGN_IN_CODE ${email} ${code}`);
    }
}

/** Accepts private test codes only for addresses explicitly allow-listed by the operator. */
export class AllowlistedTestOtpMailer implements IOtpMailer {
    private readonly testers: ReadonlySet<string>;

    constructor(testers: readonly string[]) {
        this.testers = new Set(testers.map((email) => email.trim().toLowerCase()).filter(Boolean));
    }

    async send(email: string, _code: string): Promise<void> {
        if (!this.testers.has(email.trim().toLowerCase())) {
            throw new Error("Email sign-in is not enabled for this address.");
        }
    }
}

/**
 * Chooses how to deliver a code: real email when a sender is configured, the private log for allow-listed testers, otherwise refuse.
 */
export class SelectingOtpMailer implements IOtpMailer {
    private readonly email: IOtpMailer | null;
    private readonly log: IOtpMailer;
    private readonly testers: readonly string[];
    /**
     * Creates the selecting mailer.
     * @param email Real mailer, or null when no sender domain is configured.
     * @param log Log mailer used for testers.
     * @param testers Lowercase addresses allowed to use the log mailer.
     */
    constructor(
        email: IOtpMailer | null,
        log: IOtpMailer,
        testers: readonly string[]
    ) {
        this.email = email;
        this.log = log;
        this.testers = testers;
    }

    /**
     * Delivers a code by the best available route.
     * @param email Recipient address.
     * @param code Six-digit code.
     */
    async send(email: string, code: string): Promise<void> {
        if (this.email) return this.email.send(email, code);
        if (this.testers.includes(email)) return this.log.send(email, code);
        throw new Error("Email sign-in is not set up for this address yet.");
    }
}
