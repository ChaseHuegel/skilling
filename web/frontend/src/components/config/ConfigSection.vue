<template>
    <fieldset class="config-section">
        <legend>{{ title }}</legend>
        <p v-if="description" class="section-desc">{{ description }}</p>
        <div class="section-fields">
            <slot />
        </div>
    </fieldset>
</template>

<script setup lang="ts">
defineProps<{
    title: string;
    description?: string;
}>();
</script>

<style scoped>
.config-section {
    border: 1px solid var(--p-content-border-color, #ddd);
    border-radius: 6px;
    padding: 1rem;
    margin-bottom: 1rem;
    background: var(--p-content-background);
}
.config-section legend {
    font-weight: 600;
    font-size: 1.1rem;
    padding: 0 0.5rem;
}
.section-desc {
    color: var(--p-text-muted-color, #888);
    font-size: 0.85rem;
    margin: 0 0 1rem;
}
.section-fields {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
}
</style>
