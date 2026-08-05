<template>
    <div class="template-list-input">
        <label v-if="label" class="tli-label">{{ label }}</label>
        <textarea
            class="tli-textarea"
            :value="text"
            :rows="rows"
            spellcheck="false"
            placeholder="One template line per row"
            @input="onInput"
        />
        <span class="tli-note">One template line per row; blank rows are kept (they render as blank lore lines)</span>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
    modelValue: string[];
    label?: string;
    rows?: number;
}>(), {
    modelValue: () => [],
    label: '',
    rows: 4,
});

const emit = defineEmits<{
    'update:modelValue': [value: string[]];
}>();

const text = computed(() => props.modelValue.join('\n'));

function onInput(e: Event) {
    const value = (e.target as HTMLTextAreaElement).value;
    let lines = value.split('\n');
    // A trailing newline from typing adds an empty final element; drop it so
    // saving does not append an accidental blank line. Interior blanks are kept.
    if (lines.length > 0 && lines[lines.length - 1] === '') {
        lines = lines.slice(0, -1);
    }
    emit('update:modelValue', lines);
}
</script>

<style scoped>
.template-list-input {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}
.tli-label {
    font-size: 0.8rem;
    font-weight: 600;
    color: var(--p-text-color);
}
.tli-textarea {
    padding: 0.45rem 0.6rem;
    border: 1px solid var(--p-content-border-color);
    border-radius: 5px;
    background: var(--p-form-field-background);
    color: var(--p-text-color);
    font-size: 0.875rem;
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    line-height: 1.4;
    resize: vertical;
    outline: none;
}
.tli-textarea:focus-visible {
    border-color: var(--p-primary-color);
    box-shadow: 0 0 0 2px color-mix(in srgb, var(--p-primary-color) 25%, transparent);
}
.tli-note {
    font-size: 0.75rem;
    color: var(--p-text-muted-color, #888);
    font-style: italic;
}
</style>
