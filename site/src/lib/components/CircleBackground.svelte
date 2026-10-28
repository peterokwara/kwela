<script lang="ts">
    let { color, class: extra = "", id }: { color: string; class?: string; id: string } = $props();
</script>

<svg viewBox="0 0 558 558" width="558" height="558" fill="none" aria-hidden="true" class={extra}>
    <defs>
        <linearGradient {id} x1="79" y1="16" x2="105" y2="237" gradientUnits="userSpaceOnUse">
            <stop stop-color={color} />
            <stop offset="1" stop-color={color} stop-opacity="0" />
        </linearGradient>
    </defs>
    <path opacity=".2" d="M1 279C1 125.465 125.465 1 279 1s278 124.465 278 278-124.465 278-278 278S1 432.535 1 279Z" stroke={color} />
    <path d="M1 279C1 125.465 125.465 1 279 1" stroke="url(#{id})" stroke-linecap="round" />
</svg>
