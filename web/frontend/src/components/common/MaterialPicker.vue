<template>
    <div class="material-picker" ref="pickerRef">
        <label v-if="label" class="picker-label">{{ label }}</label>
        <div class="picker-trigger" @click="toggleOpen">
            <span class="picker-selected">
                <MinecraftIcon v-if="modelValue" :material="modelValue" :size="28" />
                <span class="picker-text">{{ modelValue || 'Select an icon...' }}</span>
            </span>
            <svg class="picker-chevron" :class="{ open: isOpen }" viewBox="0 0 16 16" width="12" height="12" fill="none">
                <path d="M4 6l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
        </div>
        <div v-if="isOpen" class="picker-dropdown">
            <div class="picker-search">
                <svg class="picker-search-icon" viewBox="0 0 16 16" width="12" height="12" fill="none">
                    <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.5" />
                    <path d="M11 11l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                </svg>
                <input
                    v-model="searchQuery"
                    class="picker-search-input"
                    type="text"
                    placeholder="Search materials..."
                    @click.stop
                />
            </div>
            <div class="picker-list">
                <div
                    v-for="m in filteredMaterials"
                    :key="m"
                    class="picker-option"
                    :class="{ selected: m === modelValue }"
                    @click="select(m)"
                >
                    <MinecraftIcon :material="m" :size="28" />
                    <span class="picker-option-text">{{ m }}</span>
                </div>
                <div v-if="filteredMaterials.length === 0" class="picker-no-results">
                    No materials match your search
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import MinecraftIcon from './MinecraftIcon.vue'
import { useRegistriesStore } from '../../stores/registries'

defineProps<{
    modelValue: string
    label?: string
}>()

const emit = defineEmits<{
    'update:modelValue': [value: string]
}>()

const registriesStore = useRegistriesStore()

const isOpen = ref(false)
const searchQuery = ref('')
const pickerRef = ref<HTMLElement | null>(null)

const filteredMaterials = computed(() => {
    if (!searchQuery.value.trim()) return registriesStore.materials
    const q = searchQuery.value.toLowerCase()
    return registriesStore.materials.filter(m => m.toLowerCase().includes(q))
})

function toggleOpen() {
    isOpen.value = !isOpen.value
    if (isOpen.value) searchQuery.value = ''
}

function select(material: string) {
    emit('update:modelValue', material)
    isOpen.value = false
}

function onClickOutside(e: MouseEvent) {
    if (pickerRef.value && !pickerRef.value.contains(e.target as Node)) {
        isOpen.value = false
    }
}

onMounted(() => document.addEventListener('click', onClickOutside))
onUnmounted(() => document.removeEventListener('click', onClickOutside))
</script>

<style scoped>
.material-picker {
    position: relative;
    flex: 1;
}
.picker-label {
    display: block;
    font-size: 0.85rem;
    font-weight: 600;
    color: var(--p-text-color);
    margin-bottom: 0.25rem;
}
.picker-trigger {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.4rem 0.5rem;
    border: 1px solid var(--p-content-border-color, #ddd);
    border-radius: 4px;
    background: var(--p-form-field-background, #fff);
    cursor: pointer;
    gap: 0.5rem;
    min-height: 34px;
}
.picker-trigger:hover {
    border-color: var(--p-primary-color, #3b82f6);
}
.picker-selected {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    overflow: hidden;
}
.picker-text {
    font-size: 0.85rem;
    color: var(--p-text-color, #000);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.picker-text:empty {
    color: var(--p-form-field-placeholder-color, #888);
}
.picker-chevron {
    flex-shrink: 0;
    color: var(--p-form-field-placeholder-color, #888);
    transition: transform 0.15s;
}
.picker-chevron.open {
    transform: rotate(180deg);
}
.picker-dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 100;
    margin-top: 2px;
    border: 1px solid var(--p-content-border-color, #ddd);
    border-radius: 6px;
    background: var(--p-content-background, #fff);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    overflow: hidden;
}
.picker-search {
    position: relative;
    padding: 0.5rem;
    border-bottom: 1px solid var(--p-content-border-color, #ddd);
}
.picker-search-icon {
    position: absolute;
    left: 0.85rem;
    top: 50%;
    transform: translateY(-50%);
    color: var(--p-form-field-placeholder-color, #888);
    pointer-events: none;
}
.picker-search-input {
    width: 100%;
    padding: 0.35rem 0.5rem 0.35rem 1.75rem;
    border: 1px solid var(--p-content-border-color, #ddd);
    border-radius: 4px;
    background: var(--p-form-field-background, #fff);
    color: var(--p-text-color, #000);
    font-size: 0.8rem;
    outline: none;
    box-sizing: border-box;
}
.picker-search-input:focus {
    border-color: var(--p-primary-color, #3b82f6);
}
.picker-list {
    max-height: 260px;
    overflow-y: auto;
}
.picker-option {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.4rem 0.75rem;
    cursor: pointer;
    font-size: 0.8rem;
    color: var(--p-text-color, #000);
    transition: background 0.1s;
}
.picker-option:hover {
    background: var(--p-content-hover-background, #f0f0f0);
}
.picker-option.selected {
    background: color-mix(in srgb, var(--p-primary-color, #3b82f6) 15%, transparent);
    font-weight: 600;
}
.picker-option-text {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.picker-no-results {
    padding: 1rem;
    text-align: center;
    color: var(--p-form-field-placeholder-color, #888);
    font-size: 0.8rem;
}
</style>
