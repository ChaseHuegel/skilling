<template>
    <div v-if="staging.hasPending" class="banner">
        <div class="banner-content">
            <div class="banner-message">
                <svg class="banner-icon" viewBox="0 0 24 24" width="16" height="16" fill="none">
                    <path d="M17 3a2.83 2.83 0 114 4L7.5 20.5 2 22l1.5-5.5L17 3z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
                </svg>
                <span>{{ staging.fileCount }} file{{ staging.fileCount !== 1 ? 's' : '' }} changed</span>
            </div>
            <div class="banner-actions">
                <button class="btn btn-primary btn-sm" :disabled="staging.applying" @click="apply">
                    {{ staging.applying ? 'Applying...' : 'Apply & Reload' }}
                </button>
                <button class="btn btn-ghost btn-sm" :disabled="staging.applying" @click="staging.discard()">Discard</button>
                <span v-if="error" class="error-pill">{{ error }}</span>
            </div>
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
    background: color-mix(in srgb, var(--p-primary-color) 8%, var(--p-content-background));
    border-bottom: 1px solid var(--p-content-border-color);
    padding: 0.35rem 1.5rem;
}

.banner-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    font-size: 0.825rem;
    flex-wrap: wrap;
}

.banner-message {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    color: var(--p-text-muted-color);
    font-weight: 500;
}

.banner-icon {
    flex-shrink: 0;
    opacity: 0.7;
}

.banner-actions {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

.error-pill {
    background: var(--p-red-100);
    color: var(--p-red-800);
    padding: 0.15rem 0.5rem;
    border-radius: 10px;
    font-size: 0.75rem;
    white-space: nowrap;
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
}
</style>
