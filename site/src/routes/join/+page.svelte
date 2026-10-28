<script lang="ts">
    import { enhance } from "$app/forms";
    import { site } from "#lib/config.ts";

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
    <title>Join the Kwela private test</title>
    <meta name="description" content="Ask to try Kwela early. Leave your email and phone number and we will get in touch." />
</svelte:head>

<h1>Try Kwela early</h1>
<p class="muted" style="font-size:1.2rem">Kwela is your CV writer, on your phone. Tell us where to reach you.</p>

{#if form?.done}
    <div class="card">
        <h2 style="margin-top:0">Asante, we have your details</h2>
        <p>We will get in touch about trying Kwela. If you change your mind, email <a href="mailto:{site.supportEmail}">{site.supportEmail}</a> and we will delete them.</p>
    </div>
{:else}
    <form method="POST" action="?/join" use:enhance={submitting} class="card">
        <label for="email">Email address</label>
        <input id="email" name="email" type="email" autocomplete="email" value={form?.email ?? ""} maxlength="254" required />

        <label for="phone">Phone number</label>
        <input id="phone" name="phone" type="tel" autocomplete="tel" inputmode="tel" placeholder="0712 345 678" value={form?.phone ?? ""} maxlength="32" required />

        <!-- Real people never see this. Bots fill in every field, and anything sent here is thrown away. -->
        <div class="trap" aria-hidden="true">
            <label for="website">Leave this empty</label>
            <input id="website" name="website" type="text" tabindex="-1" autocomplete="off" />
        </div>

        <label class="check">
            <input type="checkbox" name="consent" value="yes" required />
            <span>
                Yes, Kwela may contact me by email, phone call or WhatsApp about testing the app, and may ask me about how I look for jobs and apply for them.
                I can ask to be removed at any time.
            </span>
        </label>
        {#if form?.message}<p class="error" role="alert">{form.message}</p>{/if}
        <button disabled={busy}>{busy ? "Sending…" : "Count me in"}</button>
    </form>
    <p class="muted">We use your details only to run the test. See our <a href="/privacy#tester-sign-up">Privacy Policy</a>. We never sell them.</p>
{/if}
