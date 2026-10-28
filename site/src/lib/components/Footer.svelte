<script lang="ts">
    import { site } from "#lib/config.ts";
    import Container from "./Container.svelte";
    import Logo from "./Logo.svelte";

    const links = [
        { href: "/privacy", label: "Privacy" },
        { href: "/terms", label: "Terms" },
        { href: "/delete-account", label: "Delete account" },
        { href: "/support", label: "Support" }
    ];
</script>

<footer class="border-t border-gray-200">
    <Container>
        <div class="flex flex-col items-start justify-between gap-y-10 pt-16 pb-6 lg:flex-row lg:items-center lg:py-16">
            <div>
                <div class="flex items-center text-gray-900">
                    <Logo mark />
                    <div class="ml-4">
                        <p class="text-base font-semibold">Kwela</p>
                        <p class="mt-1 text-sm">Your CV writer, on your phone.</p>
                    </div>
                </div>
                <nav class="mt-10 flex flex-wrap gap-x-8 gap-y-3">
                    {#each links as link}
                        <a href={link.href} class="text-base text-gray-700 transition-colors hover:text-gray-900">{link.label}</a>
                    {/each}
                </nav>
            </div>
            <div class="text-sm text-gray-700 lg:w-72">
                <p class="text-base font-semibold text-gray-900">Questions?</p>
                <p class="mt-1">Send us a message on the <a href="/support" class="text-teal-700 underline underline-offset-4">Support page</a> and we will reply.</p>
            </div>
        </div>
        <div class="flex flex-col items-center border-t border-gray-200 pt-8 pb-12 md:flex-row-reverse md:justify-between md:pt-6">
            <p class="mt-6 text-sm text-gray-500 md:mt-0">Kwela is an independent app and is not affiliated with WhatsApp, Instagram or any employer.</p>
            <p class="mt-6 text-sm text-gray-500 md:mt-0">&copy; {new Date().getFullYear()} {site.name}. All rights reserved.</p>
        </div>
    </Container>
</footer>
