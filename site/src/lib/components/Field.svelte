<script lang="ts">
    // The template's text field: a bold label over a rounded input. Pass `multiline` for a message box.
    let {
        label,
        name,
        type = "text",
        multiline = false,
        value = "",
        class: extra = "",
        ...rest
    }: { label: string; name: string; type?: string; multiline?: boolean; value?: string; class?: string; [key: string]: unknown } = $props();
    const id = $props.id();
    const classes =
        "block w-full appearance-none rounded-lg border border-gray-200 bg-white py-[calc(--spacing(2)-1px)] px-[calc(--spacing(3)-1px)] text-gray-900 placeholder:text-gray-400 focus:border-teal-500 focus:outline-hidden focus:ring-teal-500 sm:text-sm";
</script>

<div class={extra}>
    <label for={id} class="mb-2 block text-sm font-semibold text-gray-900">{label}</label>
    {#if multiline}
        <textarea {...rest} {id} {name} class={classes} rows="5">{value}</textarea>
    {:else}
        <input {...rest} {id} {name} {type} {value} class={classes} />
    {/if}
</div>
