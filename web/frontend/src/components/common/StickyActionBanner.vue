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
    background: var(--p-content-background);
    border-top: 1px solid var(--p-content-border-color);
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.08);
    padding: 0.75rem 1.5rem;
}

.banner-inner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    max-width: 700px;
    margin: 0 auto;
}

.banner-label {
    font-size: 0.85rem;
    font-weight: 600;
    color: var(--p-primary-color);
}

.banner-actions {
    display: flex;
    gap: 0.5rem;
}
</style>
