<script lang="ts">
    import Button from "./Button.svelte";
    import Container from "./Container.svelte";
    import Logo from "./Logo.svelte";

    const links = [
        { href: "/#features", label: "Features" },
        { href: "/#benefits", label: "Benefits" },
        { href: "/#faqs", label: "FAQs" }
    ];
    let open = $state(false);
</script>

<header>
    <nav>
        <Container class="relative z-50 flex items-center justify-between py-6 sm:py-8">
            <div class="relative z-10 flex items-center gap-16">
                <a href="/" aria-label="Kwela home"><Logo /></a>
                <div class="hidden lg:flex lg:gap-10">
                    {#each links as link}
                        <a href={link.href} class="text-base text-gray-700 transition-colors hover:text-gray-900">{link.label}</a>
                    {/each}
                </div>
            </div>
            <div class="flex items-center gap-6">
                <div class="flex items-center gap-6 max-lg:hidden"><Button href="/support" variant="outline">Support</Button><Button href="/join">Try Kwela early</Button></div>
                <button
                    class="relative z-10 -m-2 inline-flex items-center rounded-lg stroke-gray-900 p-2 hover:bg-gray-200/50 lg:hidden"
                    aria-label="Toggle site navigation"
                    aria-expanded={open}
                    onclick={() => (open = !open)}
                >
                    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" class="h-6 w-6">
                        {#if open}<path d="M17 14l-5-5-5 5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />{:else}<path d="M5 6h14M5 18h14M5 12h14" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />{/if}
                    </svg>
                </button>
            </div>
            {#if open}
                <div class="absolute inset-x-0 top-0 z-0 origin-top rounded-b-2xl bg-gray-50 px-6 pt-28 pb-6 shadow-2xl shadow-gray-900/20 lg:hidden">
                    <div class="space-y-4">
                        {#each links as link}
                            <a href={link.href} onclick={() => (open = false)} class="block text-base/7 tracking-tight text-gray-700">{link.label}</a>
                        {/each}
                    </div>
                    <div class="mt-8 flex flex-col gap-4"><Button href="/support" variant="outline">Support</Button><Button href="/join">Try Kwela early</Button></div>
                </div>
            {/if}
        </Container>
    </nav>
</header>
