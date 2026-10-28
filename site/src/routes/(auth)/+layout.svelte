<script lang="ts">
    let { children } = $props();
</script>

<!-- Like the template's sign-up pages: no site header or footer, just the logo and the card. -->
<div class="flex min-h-screen font-sans text-gray-700">{@render children()}</div>
