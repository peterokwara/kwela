<script lang="ts">
    import type { Snippet } from "svelte";
    import CirclesBackground from "./CirclesBackground.svelte";
    import Logo from "./Logo.svelte";

    // The template's sign-up layout: the logo, a title over concentric circles, and a white card.
    let { title, subtitle, children }: { title: string; subtitle?: string; children: Snippet } = $props();
</script>

<main class="flex min-h-full w-full flex-1 overflow-hidden pt-16 sm:py-28">
    <div class="mx-auto flex w-full max-w-2xl flex-col px-4 sm:px-6">
        <a href="/" aria-label="Kwela home" class="mx-auto"><Logo /></a>
        <div class="relative mt-12 sm:mt-16">
            <CirclesBackground class="absolute -top-7 left-1/2 -z-10 h-[788px] -translate-x-1/2 mask-[linear-gradient(to_bottom,white_20%,transparent_75%)] stroke-gray-300/30 sm:-top-9 sm:h-auto" />
            <h1 class="text-center text-2xl font-medium tracking-tight text-gray-900">{title}</h1>
            {#if subtitle}<p class="mt-3 text-center text-lg text-balance text-gray-600">{subtitle}</p>{/if}
        </div>
        <div class="-mx-4 mt-10 flex-auto bg-white px-4 py-10 shadow-2xl shadow-gray-900/10 sm:mx-0 sm:flex-none sm:rounded-5xl sm:p-24">
            {@render children()}
        </div>
    </div>
</main>
