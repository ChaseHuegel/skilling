<template>
    <div class="app-input" :class="[type, { inline: type === 'checkbox' }]">
        <label v-if="label" :for="inputId" class="app-input-label">{{ label }}</label>
        <div v-if="type === 'checkbox'" class="checkbox-wrapper">
            <button
                :id="inputId"
                role="switch"
                :aria-checked="!!modelValue"
                :class="['checkbox-toggle', { checked: !!modelValue }]"
                @click="toggleCheckbox"
            >
                <span class="checkbox-thumb" />
            </button>
        </div>
        <input
            v-else
            :id="inputId"
            :type="type"
            :value="modelValue"
            :placeholder="placeholder"
            :min="min"
            :max="max"
            :step="step"
            class="app-input-field"
            @input="onInput"
        />
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
    modelValue?: string | number | boolean;
    type?: 'text' | 'number' | 'checkbox' | 'password';
    label?: string;
    placeholder?: string;
    min?: number;
    max?: number;
    step?: number;
}>(), {
    modelValue: '',
    type: 'text',
    label: '',
    placeholder: '',
});

const emit = defineEmits<{
    'update:modelValue': [value: string | number | boolean];
}>();

let idCounter = 0;
const inputId = computed(() => 'app-input-' + (++idCounter));

function onInput(e: Event) {
    const el = e.target as HTMLInputElement;
    const val = props.type === 'number' ? parseFloat(el.value) || 0 : el.value;
    emit('update:modelValue', val);
}

function toggleCheckbox() {
    emit('update:modelValue', !props.modelValue);
}
</script>

<style scoped>
.app-input {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}

.app-input.inline {
    flex-direction: row;
    align-items: center;
    gap: 0.5rem;
}

.app-input-label {
    font-size: 0.8rem;
    font-weight: 600;
    color: var(--p-text-color);
}

/* ---- Text / Number / Password ---- */
.app-input-field {
    padding: 0.45rem 0.6rem;
    border: 1px solid var(--p-surface-border);
    border-radius: 5px;
    background: var(--p-surface-input);
    color: var(--p-text-color);
    font-size: 0.875rem;
    transition: border-color 0.15s, box-shadow 0.15s;
    outline: none;
}

.app-input-field::placeholder {
    color: var(--p-text-muted-color);
    opacity: 0.6;
}

.app-input-field:focus-visible {
    border-color: var(--p-primary-color);
    box-shadow: 0 0 0 2px color-mix(in srgb, var(--p-primary-color) 25%, transparent);
}

/* ---- Custom Checkbox Toggle ---- */
.checkbox-wrapper {
    display: flex;
    align-items: center;
}

.checkbox-toggle {
    position: relative;
    width: 36px;
    height: 20px;
    border-radius: 10px;
    border: 1px solid var(--p-surface-border);
    background: var(--p-surface-input);
    cursor: pointer;
    padding: 0;
    transition: background 0.2s, border-color 0.2s;
    outline: none;
}

.checkbox-toggle.checked {
    background: var(--p-primary-color);
    border-color: var(--p-primary-color);
}

.checkbox-thumb {
    position: absolute;
    top: 2px;
    left: 2px;
    width: 14px;
    height: 14px;
    border-radius: 50%;
    background: white;
    transition: transform 0.2s;
}

.checkbox-toggle.checked .checkbox-thumb {
    transform: translateX(16px);
}

.checkbox-toggle:focus-visible {
    box-shadow: 0 0 0 2px color-mix(in srgb, var(--p-primary-color) 25%, transparent);
}
</style>
