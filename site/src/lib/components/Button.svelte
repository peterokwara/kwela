<script lang="ts">
    import type { Snippet } from "svelte";

    let {
        variant = "solid",
        color = "gray",
        href,
        class: extra = "",
        children,
        ...rest
    }: { variant?: "solid" | "outline"; color?: "teal" | "white" | "gray" | "red"; href?: string; class?: string; children: Snippet; [key: string]: unknown } = $props();

    const solid = {
        teal: "relative overflow-hidden bg-teal-600 text-white before:absolute before:inset-0 hover:before:bg-white/10 active:bg-teal-700 active:text-white/80 before:transition-colors",
        red: "bg-red-700 text-white hover:bg-red-800 active:bg-red-700 active:text-white/80",
        white: "bg-white text-teal-900 hover:bg-white/90 active:bg-white/90 active:text-teal-900/70",
        gray: "bg-gray-800 text-white hover:bg-gray-900 active:bg-gray-800 active:text-white/80"
    };
    const outline = {
        teal: "border-gray-300 text-gray-700 hover:border-gray-400 active:bg-gray-100 active:text-gray-700/80",
        white: "border-gray-300 text-gray-700 hover:border-gray-400 active:bg-gray-100 active:text-gray-700/80",
        red: "border-red-300 text-red-700 hover:border-red-400 active:bg-red-50 active:text-red-700/80",
        gray: "border-gray-300 text-gray-700 hover:border-gray-400 active:bg-gray-100 active:text-gray-700/80"
    };
    const base = {
        solid: "inline-flex justify-center rounded-lg py-2 px-3 text-sm font-semibold transition-colors",
        outline: "inline-flex justify-center rounded-lg border py-[calc(--spacing(2)-1px)] px-[calc(--spacing(3)-1px)] text-sm transition-colors"
    };
    const classes = $derived(`${base[variant]} ${variant === "solid" ? solid[color] : outline[color]} ${extra}`);
</script>

{#if href}
    <a {href} class={classes} {...rest}>{@render children()}</a>
{:else}
    <button class={classes} {...rest}>{@render children()}</button>
{/if}
