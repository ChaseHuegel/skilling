<template>
    <div v-if="staging.hasPending" class="banner">
        <div class="banner-content">
            <span>⚠️ {{ staging.fileCount }} file(s) have pending changes.</span>
            <button class="apply-btn" :disabled="staging.applying" @click="apply">
                {{ staging.applying ? 'Applying...' : 'Apply & Reload' }}
            </button>
            <button class="discard-btn" :disabled="staging.applying" @click="staging.discard()">Discard</button>
            <span v-if="error" class="error">{{ error }}</span>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useStagingStore } from '../../stores/staging';

const staging = useStagingStore();
const error = ref<string | null>(null);

async function apply() {
    error.value = null;
    const msg = await staging.applyAndReload();
    if (msg) error.value = msg;
}
</script>

<style scoped>
.banner {
    background: var(--p-yellow-100);
    border-bottom: 1px solid var(--p-yellow-300);
    padding: 0.5rem 1.5rem;
}
.banner-content {
    display: flex;
    align-items: center;
    gap: 1rem;
    font-size: 0.875rem;
}
.apply-btn {
    background: var(--p-primary-color);
    color: white;
    border: none;
    padding: 0.375rem 0.75rem;
    border-radius: 4px;
    cursor: pointer;
}
.discard-btn {
    background: none;
    border: 1px solid var(--p-surface-border);
    padding: 0.375rem 0.75rem;
    border-radius: 4px;
    cursor: pointer;
}
.error {
    color: var(--p-red-600);
}
</style>
