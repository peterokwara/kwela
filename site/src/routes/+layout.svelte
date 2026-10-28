<script lang="ts">
    import "../app.css";
    import { site } from "#lib/config.ts";

    let { children } = $props();
</script>

<header style="border-bottom:1px solid var(--line)">
    <nav style="max-width:60rem;margin:0 auto;padding:1rem 1.25rem;display:flex;flex-wrap:wrap;gap:0.4rem 1.4rem;align-items:center">
        <a href="/" style="font-weight:800;font-size:1.25rem;text-decoration:none">{site.name}</a>
        <a href="/join">Join the test</a>
        <a href="/privacy">Privacy</a>
        <a href="/terms">Terms</a>
        <a href="/delete-account">Delete account</a>
        <a href="/support">Support</a>
    </nav>
</header>

<main style="max-width:60rem;margin:0 auto;padding:2rem 1.25rem 4rem">
    {@render children()}
</main>

<footer style="border-top:1px solid var(--line)">
    <p class="muted" style="max-width:60rem;margin:0 auto;padding:1.25rem;font-size:0.9rem">
        © {new Date().getFullYear()} {site.name}. Questions? <a href="mailto:{site.supportEmail}">{site.supportEmail}</a>
    </p>
</footer>
