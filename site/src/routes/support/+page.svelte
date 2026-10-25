<script lang="ts">
    import { site } from "#lib/config.ts";
</script>

<svelte:head>
    <title>Support · Kwela</title>
    <meta name="description" content="Get help with Kwela." />
</svelte:head>

<h1>Support</h1>
<p>Write to <a href="mailto:{site.supportEmail}">{site.supportEmail}</a> and we will reply. Please say which phone you use and what happened.</p>

<h2>Common questions</h2>

<h3>My sign-in code did not arrive</h3>
<p>Check your spam folder, make sure the address is spelled correctly, then ask the app for a new code. A code works for 10 minutes. You can ask for up to three codes in 15 minutes.</p>

<h3>Kwela says it cannot read my file</h3>
<p>Kwela reads PDF, Word, text and photo files up to 5 MB. A clear photo of each page works too.</p>

<h3>Kwela made a mistake</h3>
<p>Tell it in the chat what to change, for example "move my skills higher" or "make it shorter". Always check every detail before you send anything.</p>

<h3>I want to delete my account</h3>
<p>Use the app (profile, Settings, Delete account and data) or our <a href="/delete-account">deletion page</a>.</p>

<h3>I found a security problem</h3>
<p>Please email us before sharing it publicly so we can fix it.</p>
