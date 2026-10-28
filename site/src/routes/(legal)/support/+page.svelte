<script lang="ts">
    import { enhance } from "$app/forms";

    let { form } = $props();
    let busy = $state(false);

    /** Keeps the button disabled while a request is in flight. */
    function submitting() {
        busy = true;
        return async ({ update }: { update: () => Promise<void> }) => {
            try {
                await update();
            } finally {
                busy = false;
            }
        };
    }
</script>

<svelte:head>
    <title>Support · Kwela</title>
    <meta name="description" content="Get help with Kwela." />
</svelte:head>

<h1>Support</h1>
<p>Send us a message and we will reply by email. Please say which phone you use and what happened.</p>

{#if form?.sent}
    <div class="card">
        <h2 style="margin-top:0">Message sent</h2>
        <p>Thank you. We will reply to the address you gave. Replies can take a day or two.</p>
    </div>
{:else}
    <form method="POST" action="?/send" use:enhance={submitting} class="card">
        <label for="email">Your email address</label>
        <input id="email" name="email" type="email" autocomplete="email" value={form?.email ?? ""} maxlength="254" required />

        <label for="message">Your message</label>
        <textarea id="message" name="message" rows="6" minlength="10" maxlength="2000" required>{form?.message ?? ""}</textarea>

        <!-- Real people never see this. Bots fill in every field, and anything sent here is thrown away. -->
        <div class="trap" aria-hidden="true">
            <label for="website">Leave this empty</label>
            <input id="website" name="website" type="text" tabindex="-1" autocomplete="off" />
        </div>

        {#if form?.error}<p class="error" role="alert">{form.error}</p>{/if}
        <button disabled={busy}>{busy ? "Sending…" : "Send message"}</button>
        <p class="muted" style="font-size:0.9rem">We use your address and message only to answer you. See our <a href="/privacy">Privacy Policy</a>.</p>
    </form>
{/if}

<h2>Common questions</h2>

<h3>My sign-in code did not arrive</h3>
<p>Check your spam folder, make sure the address is spelled correctly, then ask the app for a new code. A code works for 10 minutes. You can ask for up to three codes in 15 minutes.</p>

<h3>Kwela says it cannot read my file</h3>
<p>Kwela reads PDF, Word, text and photo files up to 5 MB. A clear photo of each page works too.</p>

<h3>Kwela made a mistake</h3>
<p>Tell it in the chat what to change, for example "move my skills higher" or "make it shorter". Always check every detail before you send anything.</p>

<h3>I want to delete my account</h3>
<p>Use the app (profile, Settings, Delete account and data) or our <a href="/delete-account">deletion page</a>.</p>

<h3>I found a security problem</h3>
<p>Please send us a message on this page before sharing it publicly, so we can fix it.</p>
