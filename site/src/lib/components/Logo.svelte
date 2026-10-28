<script lang="ts">
    let { class: extra = "", mark = false }: { class?: string; mark?: boolean } = $props();
</script>

<span class="inline-flex items-center gap-3 {extra}">
    <img src="/kwela-avatar.svg" alt="" class="h-10 w-10 flex-none" width="40" height="40" />
    {#if !mark}<span class="text-xl font-semibold tracking-tight text-gray-900">Kwela</span>{/if}
</span>
