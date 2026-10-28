<script lang="ts">
    import { enhance } from "$app/forms";
    import AuthLayout from "#lib/components/AuthLayout.svelte";
    import Button from "#lib/components/Button.svelte";
    import Field from "#lib/components/Field.svelte";
    import Honeypot from "#lib/components/Honeypot.svelte";

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
    <title>Be first to try Kwela</title>
    <meta name="description" content="Leave your email and phone number and we will invite you to the Kwela private test." />
</svelte:head>

<AuthLayout title="Be first to apply the easy way" subtitle="Leave your email and phone number and we will invite you to the private test.">
    {#if form?.done}
        <div class="text-center">
            <span class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-teal-100">
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" class="h-8 w-8"><path d="M6 12.5l4 4 8-9" class="stroke-teal-700" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
            </span>
            <h2 class="mt-6 text-2xl font-medium tracking-tight text-gray-900">Asante, you are on the list</h2>
            <p class="mt-3 text-base text-gray-600">We will message you with how to get Kwela on your phone. Changed your mind? Tell us on the <a href="/support" class="text-teal-700 underline underline-offset-4">Support page</a> and we will delete your details.</p>
            <div class="mt-8"><Button href="/" variant="outline" class="w-full">Back to Kwela</Button></div>
        </div>
    {:else}
        <form method="POST" action="?/join" use:enhance={submitting}>
            <div class="grid grid-cols-1 gap-6">
                <Field label="Email address" name="email" type="email" autocomplete="email" value={form?.email ?? ""} maxlength="254" required />
                <Field label="Phone number" name="phone" type="tel" autocomplete="tel" inputmode="tel" placeholder="0712 345 678" value={form?.phone ?? ""} maxlength="32" required />

                <Honeypot />
            </div>
            {#if form?.message}<p class="mt-6 text-sm font-semibold text-red-700" role="alert">{form.message}</p>{/if}
            <Button type="submit" color="teal" class="mt-8 w-full" disabled={busy}>{busy ? "Sending…" : "Count me in"}</Button>
            <p class="mt-6 text-center text-sm text-gray-500">By tapping Count me in, you agree we may message you about testing Kwela and ask for your feedback. We never sell your details. <a href="/privacy#tester-sign-up" class="text-teal-700 underline underline-offset-4">Privacy Policy</a></p>
        </form>
    {/if}
</AuthLayout>
