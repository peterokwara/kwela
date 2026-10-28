<script lang="ts">
    import { onMount } from "svelte";
    import type { Snippet } from "svelte";

    // The Kwela screens are laid out at the phone's real width (1080 px) and scaled to fit, so they match the app exactly at any size.
    // They are decoration for the page, so assistive technology skips them. They fade in once measured, so they never show at the wrong size.
    let { children, design = 1080 }: { children: Snippet; design?: number } = $props();
    let width = $state(0);
    let height = $state(0);
    let ready = $state(false);
    const scale = $derived(width > 0 ? width / design : 1);
    onMount(() => (ready = true));
</script>

<div class="relative h-full min-h-0 w-full overflow-hidden transition-opacity duration-300 {ready && width > 0 ? 'opacity-100' : 'opacity-0'}" aria-hidden="true" bind:clientWidth={width} bind:clientHeight={height}>
    <div class="absolute top-0 left-0 origin-top-left" style="width:{design}px;height:{height / scale}px;transform:scale({scale})">
        {@render children()}
    </div>
</div>
