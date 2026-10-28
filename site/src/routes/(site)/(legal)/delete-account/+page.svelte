<script lang="ts">
    import { enhance } from "$app/forms";
    import Button from "#lib/components/Button.svelte";
    import Field from "#lib/components/Field.svelte";

    let { form } = $props();
    let busy = $state(false);

    const step = $derived(form?.step ?? "email");
    const email = $derived(form?.email ?? "");

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
        <Field label="6-digit code" name="code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" required class="mt-6" />
        <label class="mt-6 flex items-start gap-3 text-sm text-gray-700">
            <input type="checkbox" name="confirm" value="yes" class="mt-1 h-4 w-4 rounded border-gray-300 text-red-700 focus:ring-red-600" />
            <span>Yes, permanently delete my Kwela account and saved CV profile.</span>
        </label>
        {#if form?.message}<p class="mt-6 text-sm font-semibold text-red-700" role="alert">{form.message}</p>{/if}
        <Button type="submit" color="red" class="mt-8 w-full" disabled={busy}>{busy ? "Deleting…" : "Delete my account"}</Button>
    </form>
    <p class="muted"><a href="/delete-account">Start again with a different email</a></p>
{:else}
    <form method="POST" action="?/request" use:enhance={submitting} class="card">
        <h2 style="margin-top:0">Step 1: confirm it is you</h2>
        <p>Enter the email address on your Kwela account. We will send you a code.</p>
        <Field label="Email address" name="email" type="email" autocomplete="email" value={email} required class="mt-6" />
        {#if form?.message}<p class="mt-6 text-sm font-semibold text-red-700" role="alert">{form.message}</p>{/if}
        <Button type="submit" color="gray" class="mt-8 w-full" disabled={busy}>{busy ? "Sending…" : "Send me a code"}</Button>
    </form>
{/if}

<p class="muted">Cannot use this page? Send a message on the <a href="/support">Support page</a>, using the address on your account, and we will delete it for you.</p>
