<template>
    <div class="tags-page">
        <div class="page-header">
            <h1>Custom Tags</h1>
            <div class="header-actions">
                <button class="btn btn-secondary" @click="fetchTags">Reset</button>
                <button class="btn btn-primary" :disabled="saving" @click="saveTags">
                    {{ saving ? 'Saving...' : 'Save Changes' }}
                </button>
            </div>
        </div>

        <div v-if="error" class="error-banner">{{ error }}</div>
        <div v-if="loading" class="loading">Loading tags...</div>

        <div v-else class="tags-content">
            <TagListEditor v-model="tags" :suggestions="suggestions" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { api } from '../api/client';
import TagListEditor from '../components/tags/TagListEditor.vue';

const loading = ref(true);
const saving = ref(false);
const error = ref<string | null>(null);
const tags = reactive<Record<string, string[]>>({});

const suggestions = [
    '#c:ores', '#c:stone', '#c:logs', '#c:gems',
    '#minecraft:logs', '#minecraft:planks', '#minecraft:stone_tool_materials',
    '#minecraft:pickaxes', '#minecraft:axes', '#minecraft:shovels', '#minecraft:hoes',
    '#minecraft:coals', '#minecraft:copper_ores', '#minecraft:iron_ores',
    '#minecraft:gold_ores', '#minecraft:diamond_ores', '#minecraft:emerald_ores',
];

onMounted(fetchTags);

async function fetchTags() {
    loading.value = true;
    error.value = null;
    try {
        const data = await api.tags.get();
        Object.assign(tags, data.tags || {});
    } catch (e: any) {
        error.value = e.message || 'Failed to load tags';
    } finally {
        loading.value = false;
    }
}

async function saveTags() {
    saving.value = true;
    error.value = null;
    try {
        await api.tags.update({ ...tags });
    } catch (e: any) {
        error.value = e.message || 'Failed to save tags';
    } finally {
        saving.value = false;
    }
}
</script>

<style scoped>
.tags-page {
    max-width: 700px;
    margin: 0 auto;
    padding: 1.5rem;
}
.page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 1.5rem;
}
.page-header h1 {
    margin: 0;
    font-size: 1.5rem;
}
.header-actions {
    display: flex;
    gap: 0.5rem;
}
.btn {
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 0.875rem;
}
.btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}
.btn-primary {
    background: var(--p-primary-color, #3b82f6);
    color: white;
}
.btn-secondary {
    background: var(--p-surface-border, #e5e7eb);
    color: var(--p-text-color, #333);
}
.error-banner {
    background: var(--p-red-100, #fee2e2);
    color: var(--p-red-800, #991b1b);
    padding: 0.75rem;
    border-radius: 4px;
    margin-bottom: 1rem;
    font-size: 0.875rem;
}
.loading {
    text-align: center;
    padding: 2rem;
    color: var(--p-text-muted-color, #888);
}
</style>
