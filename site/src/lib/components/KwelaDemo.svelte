<script lang="ts">
    // A small copy of the Kwela chat, in the app's own dark colours, for the phone on the home page.
    const rise = (n: number) => `animation-delay:${n}ms`;
</script>

<div class="flex min-h-0 flex-col bg-[#0d0e12] text-[#e3e5ee]" aria-label="A Kwela chat">
    <div class="flex flex-col items-center gap-0.5 pt-3 pb-2">
        <img src="/kwela-avatar.svg" alt="" class="h-9 w-9" width="36" height="36" />
        <span class="text-[10px] font-medium">Kwela</span>
    </div>
    <div class="flex flex-1 flex-col gap-2 overflow-hidden px-2.5 pb-3 text-[11.5px] leading-[1.45]">
        <div class="animate-rise self-end rounded-[18px] bg-[#455270] px-3.5 py-2 text-[#dae3fc]" style={rise(300)}>
            Please review my file.
            <div class="mt-1 text-[9.5px] text-[#aab4cf]">📎 job-poster.jpg</div>
        </div>
        <div class="animate-rise max-w-[85%] self-start rounded-[18px] bg-[#181a1f] px-3.5 py-2" style={rise(900)}>Got it, thank you. Give me about a minute to put this together.</div>
        <div class="animate-rise max-w-[85%] self-start rounded-[18px] bg-[#181a1f] px-3.5 py-2" style={rise(1500)}>Quick one: do you have pharmacy experience? Roughly how many years?</div>
        <div class="animate-rise flex gap-1.5 pl-5" style={rise(2000)}>
            {#each ["Under a year", "1-2 years", "3+ years"] as chip}
                <span class="rounded-2xl bg-[#455270] px-2.5 py-1.5 text-[10px] font-medium text-[#dae3fc]">{chip}</span>
            {/each}
        </div>
        <div class="animate-rise w-[92%] rounded-[18px] bg-[#1e2025] px-3.5 py-2.5" style={rise(2700)}>
            <div class="text-[11px] font-bold">CV - Pharmaceutical Technologist</div>
            <div class="text-[9.5px] text-[#a9abb4]">Tailored to this job</div>
            <div class="mt-2 flex items-center gap-1 text-[10.5px] font-medium">
                <span class="rounded-full bg-[#353c4c] px-3 py-1.5 text-[#b9bfd3]">Open</span>
                <span class="px-2 text-[#b9c6ea]">Download</span>
                <span class="px-2 text-[#b9c6ea]">Share</span>
            </div>
        </div>
    </div>
</div>
