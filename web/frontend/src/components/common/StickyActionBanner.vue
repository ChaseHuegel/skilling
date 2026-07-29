<template>
    <div v-if="visible" class="sticky-banner">
        <div class="banner-inner">
            <span class="banner-label">Unsaved changes</span>
            <div class="banner-actions">
                <button class="btn btn-secondary" :disabled="saving" @click="$emit('cancel')">Cancel</button>
                <button class="btn btn-primary" :disabled="saving" @click="$emit('save')">
                    {{ saving ? 'Saving...' : 'Save Changes' }}
                </button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
defineProps<{
    visible: boolean
    saving: boolean
}>();

defineEmits<{
    save: []
    cancel: []
}>();
</script>

<style scoped>
.sticky-banner {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 900;
    background: color-mix(in srgb, var(--p-primary-color) 8%, var(--p-content-background));
    border-top: 1px solid var(--p-content-border-color);
    padding: 0.4rem 1.5rem;
}

.banner-inner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    max-width: 700px;
    margin: 0 auto;
    font-size: 0.825rem;
}

.banner-label {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    color: var(--p-text-muted-color);
    font-weight: 500;
}

.banner-actions {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}
</style>
