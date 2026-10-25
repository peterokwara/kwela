<script lang="ts">
    import { enhance } from "$app/forms";
    import { site } from "#lib/config.ts";

    let { form } = $props();
    let busy = $state(false);

    const step = $derived(form?.step ?? "email");
    const email = $derived(form?.email ?? "");

    /** Keeps the button disabled while a request is in flight. */
    function submitting() {
        busy = true;
        return async ({ update }: { update: () => Promise<void> }) => {
            await update();
            busy = false;
        };
    }
</script>

<svelte:head>
    <title>Delete your Kwela account</title>
    <meta name="description" content="Delete your Kwela account and the data we hold about you." />
</svelte:head>

<h1>Delete your Kwela account</h1>
<p class="muted">Use this page if you want your account removed and no longer have the app. It works the same as Delete account and data inside the app.</p>

<h2>What happens</h2>
<ul>
    <li><strong>Deleted from our servers:</strong> your account and email address, your sign-in sessions and any pending codes, and your saved CV profile.</li>
    <li>
        <strong>Kept:</strong> usage records keyed by a one-way hash of your email, with request times and estimated cost. We keep them only to enforce free-use limits
        and prevent abuse. They cannot be used to read your messages or documents, because we do not store those.
    </li>
    <li><strong>On your phone:</strong> your chats and documents are stored only on your phone. Delete them with Delete account and data in the app, or by uninstalling Kwela.</li>
</ul>
<p>Deleting cannot be undone.</p>

{#if step === "done"}
    <div class="card">
        <h2 style="margin-top:0">Your account has been deleted</h2>
        <p>We removed the account for <strong>{email}</strong> and its saved CV profile. If you still have Kwela on your phone, you can remove your chats there too, in your profile under Settings.</p>
    </div>
{:else if step === "code"}
    <form method="POST" action="?/confirm" use:enhance={submitting} class="card">
        <h2 style="margin-top:0">Enter the code</h2>
        <p>We sent a 6-digit code to <strong>{email}</strong>. It works for 10 minutes.</p>
        <input type="hidden" name="email" value={email} />
        <label for="code">6-digit code</label>
        <input id="code" name="code" type="text" inputmode="numeric" autocomplete="one-time-code" maxlength="6" required />
        <label class="check">
            <input type="checkbox" name="confirm" value="yes" />
            <span>Yes, permanently delete my Kwela account and saved CV profile.</span>
        </label>
        {#if form?.message}<p class="error" role="alert">{form.message}</p>{/if}
        <button class="danger" disabled={busy}>{busy ? "Deleting…" : "Delete my account"}</button>
    </form>
    <p class="muted"><a href="/delete-account">Start again with a different email</a></p>
{:else}
    <form method="POST" action="?/request" use:enhance={submitting} class="card">
        <h2 style="margin-top:0">Step 1: confirm it is you</h2>
        <p>Enter the email address on your Kwela account. We will send you a code.</p>
        <label for="email">Email address</label>
        <input id="email" name="email" type="email" autocomplete="email" value={email} required />
        {#if form?.message}<p class="error" role="alert">{form.message}</p>{/if}
        <button disabled={busy}>{busy ? "Sending…" : "Send me a code"}</button>
    </form>
{/if}

<p class="muted">Cannot use this page? Email <a href="mailto:{site.supportEmail}">{site.supportEmail}</a> from your account address and we will delete it for you.</p>
