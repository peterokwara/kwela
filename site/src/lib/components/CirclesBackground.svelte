<script lang="ts">
    let { class: extra = "" }: { class?: string } = $props();
</script>

<!-- The attributes give the circles their size; without them the svg collapses to a stub. -->
<svg viewBox="0 0 1090 1090" width="1090" height="1090" aria-hidden="true" fill="none" preserveAspectRatio="none" class={extra}>
    <circle cx="545" cy="545" r="544.5" />
    <circle cx="545" cy="545" r="480.5" />
    <circle cx="545" cy="545" r="416.5" />
    <circle cx="545" cy="545" r="352.5" />
</svg>
