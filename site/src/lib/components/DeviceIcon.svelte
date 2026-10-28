<script lang="ts">
    // The small phone icons from the Pocket template, each with its own glyph on the screen.
    let {
        kind,
        tone = "light",
        class: extra = "h-8 w-8"
    }: { kind: "share" | "photo" | "pdf" | "pen" | "lock" | "match"; tone?: "light" | "dark"; class?: string } = $props();
    // The template draws the same icons in lighter greys on its charcoal section.
    const outline = $derived(tone === "dark" ? "#A3A3A3" : "#737373");
    const glyph = $derived(tone === "dark" ? "#737373" : "#171717");
    const soft = $derived(outline);
    const light = $derived(tone === "dark" ? "#D4D4D4" : "#A3A3A3");
</script>

<svg viewBox="0 0 32 32" fill="none" aria-hidden="true" class={extra}>
    <circle cx="16" cy="16" r="16" fill="#A3A3A3" fill-opacity="0.2" />
    <path
        fill-rule="evenodd"
        clip-rule="evenodd"
        d="M9 0a4 4 0 00-4 4v24a4 4 0 004 4h14a4 4 0 004-4V4a4 4 0 00-4-4H9zm0 2a2 2 0 00-2 2v24a2 2 0 002 2h14a2 2 0 002-2V4a2 2 0 00-2-2h-1.382a1 1 0 00-.894.553l-.448.894a1 1 0 01-.894.553h-6.764a1 1 0 01-.894-.553l-.448-.894A1 1 0 0010.382 2H9z"
        fill={outline}
    />
    {#if kind === "share"}
        <path d="M12 25l8-8m0 0h-6m6 0v6" stroke={glyph} stroke-width="2" stroke-linecap="round" />
    {:else if kind === "match"}
        <path d="M11 17l3.5 3.5L21.5 13" stroke={glyph} stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        <path d="M11 9h10" stroke={soft} stroke-width="2" stroke-linecap="square" />
    {:else if kind === "pen"}
        <path d="M11 22l1-4 7-7 3 3-7 7-4 1z" fill={glyph} />
        <path d="M11 9h10" stroke={soft} stroke-width="2" stroke-linecap="square" />
    {:else if kind === "photo"}
        <rect x="10" y="12" width="12" height="10" rx="1.5" fill={glyph} />
        <circle cx="16" cy="17" r="2.6" fill={light} />
        <path d="M10 9h12" stroke={soft} stroke-width="2" stroke-linecap="square" />
    {:else if kind === "pdf"}
        <path d="M11 11h7l3 3v9H11z" fill={glyph} />
        <path d="M18 11v3h3" stroke={light} stroke-width="1.4" />
        <path d="M13 19h6M13 21.5h4" stroke={light} stroke-width="1.4" stroke-linecap="round" />
    {:else}
        <path fill-rule="evenodd" clip-rule="evenodd" d="M12 17v-2a4 4 0 118 0v2a2 2 0 012 2v4a2 2 0 01-2 2h-8a2 2 0 01-2-2v-4a2 2 0 012-2zm2 0h4v-2a2 2 0 10-4 0v2z" fill={glyph} />
    {/if}
</svg>
