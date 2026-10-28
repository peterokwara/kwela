<script lang="ts">
    import Footer from "#lib/components/Footer.svelte";
    import Header from "#lib/components/Header.svelte";

    let { children } = $props();
</script>

<div class="flex min-h-screen flex-col font-sans text-gray-700">
    <Header />
    <div class="flex-auto">{@render children()}</div>
    <Footer />
</div>
