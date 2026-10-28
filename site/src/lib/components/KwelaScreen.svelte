<script lang="ts">
    import ScaledScreen from "./ScaledScreen.svelte";

    // The Kwela chat, in the app's own dark colours and sizes (the app's dp x 2.625), drawn at the phone's native width.
    let { scene, animate = false }: { scene: "share" | "ask" | "docs"; animate?: boolean } = $props();

    const cards = [
        { title: "CV - Pharmaceutical Technologist", preview: "Diploma in Pharmaceutical Technology. Enrolled with the Pharmacy and Poisons Board. Two years in a busy retail pharmacy." },
        { title: "Cover letter - Pharmaceutical Technologist", preview: "Dear Hiring Team, I am applying for the Pharmaceutical Technologist role in Nairobi. I have two years in a busy pharmacy." },
        { title: "Application answers - Pharmaceutical Technologist", preview: "Why do you want this role? I want to grow in a pharmacy team that serves customers every day." }
    ];
    const delay = (n: number) => (animate ? `animation-delay:${n}ms` : "");
    const rise = $derived(animate ? "animate-rise" : "");
</script>

{#snippet bubble(text: string, mine: boolean, ms: number, attachment?: string)}
    <div class="flex {mine ? 'justify-end' : 'justify-start'} {rise}" style={delay(ms)}>
        <div class="max-w-[830px] rounded-[63px] px-[42px] py-[26px] text-[42px] leading-[63px] {mine ? 'bg-[#455270] text-[#dae3fc]' : 'bg-[#181a1f] text-[#e3e5ee]'}">
            {text}
            {#if attachment}<div class="mt-[21px] text-[31px] leading-[40px] text-[#aab4cf]">📎 {attachment}</div>{/if}
        </div>
    </div>
{/snippet}

<ScaledScreen>
    <div class="absolute inset-0 bg-[#0d0e12] font-sans text-[#e3e5ee]">
        <div class="absolute top-[40px] right-0 left-0 flex flex-col items-center gap-[6px]">
            <img src="/kwela-avatar.svg" alt="" class="h-[147px] w-[147px]" />
            <div class="text-[31px] font-medium">Kwela</div>
        </div>

        <div class="absolute top-[290px] right-0 left-0 flex flex-col gap-[21px] overflow-hidden px-[21px]" style="bottom:380px">
            {#if scene === "share"}
                <div class="pt-[21px] text-center text-[31px] text-[#a9abb4]">Today 10:24 AM</div>
                {@render bubble("Please review my file.", true, 200, "job-poster.jpg")}
                {@render bubble("Got it, thank you. Give me about a minute to put this together.", false, 700)}
                <div class="{rise}" style={delay(1200)}>
                    <div class="inline-flex gap-[16px] rounded-[63px] bg-[#181a1f] px-[47px] py-[42px]">
                        <span class="h-[21px] w-[21px] rounded-full bg-[#a9abb4]"></span><span class="h-[21px] w-[21px] rounded-full bg-[#a9abb4] opacity-40"></span><span class="h-[21px] w-[21px] rounded-full bg-[#a9abb4] opacity-40"></span>
                    </div>
                </div>
            {:else if scene === "ask"}
                {@render bubble("Quick one: do you have pharmacy experience? Roughly how many years?", false, 200)}
                <div class="flex flex-wrap gap-[21px] pl-[63px] {rise}" style={delay(700)}>
                    {#each ["Under a year", "1-2 years", "3+ years"] as chip, i}
                        <span class="rounded-[52px] px-[42px] py-[31px] text-[37px] font-medium {i === 1 ? 'bg-[#5a6a92] text-white' : 'bg-[#455270] text-[#dae3fc]'}">{chip}</span>
                    {/each}
                </div>
                {@render bubble("1-2 years", true, 1200)}
                {@render bubble("Thanks. I am writing your CV now.", false, 1700)}
            {:else}
                {@render bubble("Your Pharmaceutical Technologist application is ready.", false, 100)}
                {#each cards as card, i}
                    <div class="w-[955px] rounded-[52px] bg-[#1e2025] pt-[37px] pr-[21px] pb-[16px] pl-[42px] {rise}" style={delay(300 + i * 350)}>
                        <div class="flex items-center gap-[26px]">
                            <svg viewBox="0 0 24 24" class="h-[58px] w-[58px] flex-none" fill="none" stroke="#b9c6ea" stroke-width="1.8" stroke-linejoin="round"><path d="M6 2h8l5 5v15H6z" /><path d="M14 2v5h5" /></svg>
                            <div>
                                <div class="text-[37px] leading-[50px] font-bold">{card.title}</div>
                                <div class="text-[31px] leading-[42px] text-[#a9abb4]">Pharmaceutical Technologist</div>
                            </div>
                        </div>
                        <div class="mt-[21px] line-clamp-2 pr-[21px] text-[37px] leading-[52px] text-[#a9abb4]">{card.preview}</div>
                        <div class="mt-[21px] flex items-center gap-[10px] text-[37px] font-medium">
                            <span class="flex h-[105px] items-center rounded-full bg-[#353c4c] px-[63px] text-[#b9bfd3]">Open</span>
                            <span class="px-[42px] text-[#b9c6ea]">Download</span>
                            <span class="px-[42px] text-[#b9c6ea]">Share</span>
                        </div>
                    </div>
                {/each}
            {/if}
        </div>

        <div class="absolute right-[21px] left-[21px] flex h-[147px] items-center gap-[6px] rounded-[74px] bg-[#181a1f]" style="bottom:216px">
            <svg viewBox="0 0 24 24" class="ml-[10px] h-[63px] w-[126px]"><g transform="translate(-1,0)"><circle cx="12" cy="12" r="9" fill="none" stroke="#a9abb4" stroke-width="1.6" /><path d="M12 7.5v9M7.5 12h9" stroke="#a9abb4" stroke-width="1.6" /></g></svg>
            <div class="text-[42px] text-[#a9abb4]">Message Kwela</div>
        </div>
        <div class="absolute right-[42px] left-[42px] h-[168px] rounded-[84px] bg-[#181a1f]" style="bottom:36px">
            <div class="absolute top-[26px] left-[176px] flex h-[116px] w-[167px] items-center justify-center rounded-[58px] bg-[#23262c]">
                <svg viewBox="0 0 24 24" class="h-[64px] w-[64px]" fill="none" stroke="#e3e5ee" stroke-width="1.7" stroke-linejoin="round"><path d="M4 4h16v12H8l-4 4z" /></svg>
            </div>
            <svg viewBox="0 0 24 24" class="absolute top-[52px] left-[700px] h-[64px] w-[64px]" fill="none" stroke="#e3e5ee" stroke-width="1.7" stroke-linejoin="round"><path d="M3 6h7l2 2h9v11H3z" /></svg>
        </div>
    </div>
</ScaledScreen>
