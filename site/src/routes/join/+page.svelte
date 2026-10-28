<script lang="ts">
    import { enhance } from "$app/forms";
    import { site } from "#lib/config.ts";
    import Button from "#lib/components/Button.svelte";
    import CirclesBackground from "#lib/components/CirclesBackground.svelte";

    let { form } = $props();
    let busy = $state(false);

    const field =
        "block w-full appearance-none rounded-lg border border-gray-200 bg-white py-[calc(--spacing(2)-1px)] px-[calc(--spacing(3)-1px)] text-gray-900 placeholder:text-gray-400 focus:border-teal-500 focus:outline-hidden focus:ring-teal-500 sm:text-sm";

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
    <title>Try Kwela early</title>
    <meta name="description" content="Ask to try Kwela early. Leave your email and phone number and we will get in touch." />
</svelte:head>

<main class="flex overflow-hidden pt-4 pb-16 sm:py-16">
    <div class="mx-auto flex w-full max-w-2xl flex-col px-4 sm:px-6">
        <div class="relative">
            <CirclesBackground class="absolute -top-7 left-1/2 -z-10 h-[788px] -translate-x-1/2 mask-[linear-gradient(to_bottom,white_20%,transparent_75%)] stroke-gray-300/30 sm:-top-9 sm:h-auto" />
            <h1 class="text-center text-3xl font-medium tracking-tight text-gray-900">Try Kwela early</h1>
            <p class="mt-3 text-center text-lg text-gray-600">Tell us where to reach you and we will invite you to the private test.</p>
        </div>

        <div class="-mx-4 mt-10 flex-auto bg-white px-4 py-10 shadow-2xl shadow-gray-900/10 sm:mx-0 sm:flex-none sm:rounded-5xl sm:p-16">
            {#if form?.done}
                <div class="text-center">
                    <span class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-teal-100">
                        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" class="h-8 w-8"><path d="M6 12.5l4 4 8-9" class="stroke-teal-700" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
                    </span>
                    <h2 class="mt-6 text-2xl font-medium tracking-tight text-gray-900">Asante, we have your details</h2>
                    <p class="mt-3 text-base text-gray-600">We will be in touch about trying Kwela. If you change your mind, email <a href="mailto:{site.supportEmail}" class="text-teal-700 underline underline-offset-4">{site.supportEmail}</a> and we will delete them.</p>
                </div>
            {:else}
                <form method="POST" action="?/join" use:enhance={submitting}>
                    <div class="grid grid-cols-1 gap-6">
                        <div>
                            <label for="email" class="mb-2 block text-sm font-semibold text-gray-900">Email address</label>
                            <input id="email" name="email" type="email" autocomplete="email" value={form?.email ?? ""} maxlength="254" required class={field} />
                        </div>
                        <div>
                            <label for="phone" class="mb-2 block text-sm font-semibold text-gray-900">Phone number</label>
                            <input id="phone" name="phone" type="tel" autocomplete="tel" inputmode="tel" placeholder="0712 345 678" value={form?.phone ?? ""} maxlength="32" required class={field} />
                        </div>

                        <!-- Real people never see this. Bots fill in every field, and anything sent here is thrown away. -->
                        <div class="trap" aria-hidden="true">
                            <label for="website">Leave this empty</label>
                            <input id="website" name="website" type="text" tabindex="-1" autocomplete="off" />
                        </div>

                        <label class="flex items-start gap-3 text-sm text-gray-700">
                            <input type="checkbox" name="consent" value="yes" required class="mt-1 h-4 w-4 rounded border-gray-300 text-teal-700 focus:ring-teal-500" />
                            <span>
                                Yes, Kwela may contact me by email, phone call or WhatsApp about testing the app, and may ask me about how I look for jobs and apply for them. I can ask to be
                                removed at any time.
                            </span>
                        </label>
                    </div>
                    {#if form?.message}<p class="mt-6 text-sm font-semibold text-red-700" role="alert">{form.message}</p>{/if}
                    <Button type="submit" color="teal" class="mt-8 w-full py-3 text-base" disabled={busy}>{busy ? "Sending…" : "Count me in"}</Button>
                    <p class="mt-6 text-center text-sm text-gray-500">We use your details only to run the test and never sell them. See our <a href="/privacy#tester-sign-up" class="text-teal-700 underline underline-offset-4">Privacy Policy</a>.</p>
                </form>
            {/if}
        </div>
    </div>
</main>
