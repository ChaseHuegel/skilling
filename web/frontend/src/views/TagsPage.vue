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

        <div class="search-bar">
            <svg class="search-icon" viewBox="0 0 16 16" width="14" height="14" fill="none">
                <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.5" />
                <path d="M11 11l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
            <input
                v-model="searchQuery"
                class="search-input"
                type="text"
                placeholder="Search tags..."
            />
        </div>

        <div v-if="error" class="error-banner">{{ error }}</div>
        <div v-if="loading" class="loading">Loading tags...</div>

        <div v-else-if="searchQuery.trim() && Object.keys(filteredTags).length === 0" class="no-results">
            No tags match your search
        </div>

        <div v-else class="tags-content">
            <TagListEditor v-model="filteredTags" :suggestions="suggestions" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { api } from '../api/client';
import TagListEditor from '../components/tags/TagListEditor.vue';

const loading = ref(true);
const saving = ref(false);
const error = ref<string | null>(null);
const tags = reactive<Record<string, string[]>>({});
const searchQuery = ref('');

const filteredTags = computed(() => {
    if (!searchQuery.value.trim()) return tags;
    const q = searchQuery.value.toLowerCase();
    const result: Record<string, string[]> = {};
    for (const [key, values] of Object.entries(tags)) {
        if (key.toLowerCase().includes(q) || values.some(v => v.toLowerCase().includes(q))) {
            result[key] = values;
        }
    }
    return result;
});

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
.error-banner {
    background: var(--p-red-100, #fee2e2);
    color: var(--p-red-800, #991b1b);
    padding: 0.75rem;
    border-radius: 4px;
    margin-bottom: 1rem;
    font-size: 0.875rem;
}
.search-bar {
    position: relative;
    margin-bottom: 1.25rem;
}
.search-icon {
    position: absolute;
    left: 0.75rem;
    top: 50%;
    transform: translateY(-50%);
    color: var(--p-form-field-placeholder-color, #888);
    pointer-events: none;
}
.search-input {
    width: 100%;
    padding: 0.5rem 0.75rem 0.5rem 2.25rem;
    border: 1px solid var(--p-content-border-color, #ddd);
    border-radius: 6px;
    background: var(--p-form-field-background, #fff);
    color: var(--p-form-field-color, #000);
    font-size: 0.875rem;
    outline: none;
    box-sizing: border-box;
}
.search-input:focus {
    border-color: var(--p-primary-color, #3b82f6);
    box-shadow: 0 0 0 2px color-mix(in srgb, var(--p-primary-color, #3b82f6) 20%, transparent);
}
.search-input::placeholder {
    color: var(--p-form-field-placeholder-color, #888);
}
.no-results {
    text-align: center;
    padding: 2rem;
    color: var(--p-text-muted-color, #888);
    font-size: 0.875rem;
}
.loading {
    text-align: center;
    padding: 2rem;
    color: var(--p-text-muted-color, #888);
}
</style>
