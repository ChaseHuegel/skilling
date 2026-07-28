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
                <button class="apply-btn" :disabled="staging.applying" @click="apply">
                    {{ staging.applying ? 'Applying...' : 'Apply & Reload' }}
                </button>
                <button class="discard-btn" :disabled="staging.applying" @click="staging.discard()">Discard</button>
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
    background: var(--p-yellow-100);
    border-bottom: 1px solid var(--p-yellow-300);
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
    color: var(--p-yellow-800);
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

.apply-btn {
    background: var(--p-primary-color);
    color: white;
    border: none;
    padding: 0.3rem 0.7rem;
    border-radius: 4px;
    cursor: pointer;
    font-size: 0.8rem;
    font-weight: 500;
    transition: filter 0.15s;
}

.apply-btn:hover {
    filter: brightness(1.1);
}

.apply-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.discard-btn {
    background: none;
    border: none;
    cursor: pointer;
    font-size: 0.8rem;
    color: var(--p-yellow-800);
    opacity: 0.6;
    padding: 0.2rem 0.3rem;
    transition: opacity 0.15s;
}

.discard-btn:hover {
    opacity: 1;
    text-decoration: underline;
}

.discard-btn:disabled {
    opacity: 0.3;
    cursor: not-allowed;
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
