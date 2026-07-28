<template>
    <div class="app-combobox">
        <label v-if="label" class="combobox-label" :for="inputId">{{ label }}</label>
        <input
            :id="inputId"
            class="combobox-input"
            type="text"
            :placeholder="placeholder"
            :list="listId"
            :value="modelValue"
            @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        />
        <datalist :id="listId">
            <option v-for="s in suggestions" :key="s" :value="s" />
        </datalist>
    </div>
</template>

<script lang="ts">
let comboboxCounter = 0
</script>

<script setup lang="ts">
const uid = ++comboboxCounter

const props = defineProps<{
    modelValue: string
    suggestions?: string[]
    label?: string
    placeholder?: string
    name?: string
}>()

defineEmits<{
    'update:modelValue': [value: string]
}>()

const listId = props.name ? `cl-${props.name}` : `cl-${uid}`
const inputId = props.name ? `ci-${props.name}` : `ci-${uid}`
</script>

<style scoped>
.app-combobox {
    flex: 1;
    min-width: 140px;
}
.combobox-label {
    display: block;
    font-size: 0.75rem;
    font-weight: 600;
    color: var(--p-form-field-placeholder-color, #888);
    margin-bottom: 0.2rem;
    text-transform: uppercase;
    letter-spacing: 0.03em;
}
.combobox-input {
    width: 100%;
    padding: 0.35rem 0.5rem;
    border: 1px solid var(--p-content-border-color, #ddd);
    border-radius: 4px;
    background: var(--p-form-field-background, #fff);
    color: var(--p-text-color, #000);
    font-size: 0.85rem;
    box-sizing: border-box;
    outline: none;
}
.combobox-input:focus {
    border-color: var(--p-primary-color, #3b82f6);
    box-shadow: 0 0 0 2px color-mix(in srgb, var(--p-primary-color, #3b82f6) 20%, transparent);
}
</style>
