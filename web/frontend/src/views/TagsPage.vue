<template>
    <div class="tags-page">
        <div class="page-header">
            <div>
                <div class="header-left">
                    <h1>Tags</h1>
                    <span v-if="!loading && Object.keys(tags).length > 0" class="count-badge">{{ Object.keys(tags).length }} tag{{ Object.keys(tags).length !== 1 ? 's' : '' }}</span>
                </div>
                <p class="page-subtitle">Manage custom item and block tags for skill definitions</p>
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
            <div v-if="entityTagKeys.length > 0" class="entity-tags-note">
                <strong>Entity tags (read-only)</strong>
                <span>Preserved on save and used by the <code>target_type</code> state filter:</span>
                <div class="entity-tags-list">
                    <span v-for="key in entityTagKeys" :key="key" class="entity-tag-key">{{ key }}</span>
                </div>
            </div>
        </div>

        <StickyActionBanner :visible="isDirty" :saving="saving" @save="saveTags" @cancel="confirmCancel" />

        <!-- Cancel confirm dialog -->
        <div v-if="showCancelDialog" class="modal-overlay" @click.self="showCancelDialog = false">
            <div class="modal">
                <h3>Discard changes?</h3>
                <p>Any unsaved changes to your custom tags will be lost.</p>
                <div class="modal-actions">
                    <button class="btn btn-secondary" @click="showCancelDialog = false">Keep Editing</button>
                    <button class="btn btn-danger" @click="discardTags">Discard</button>
                </div>
            </div>
        </div>

        <!-- Leave confirm dialog -->
        <div v-if="showLeaveDialog" class="modal-overlay" @click.self="showLeaveDialog = false">
            <div class="modal">
                <h3>Unsaved changes</h3>
                <p>Would you like to save your changes before leaving?</p>
                <div class="modal-actions">
                    <button class="btn btn-secondary" @click="showLeaveDialog = false">Cancel</button>
                    <button class="btn btn-danger" @click="leaveDiscard">Discard</button>
                    <button class="btn btn-primary" @click="leaveSave">Save & Leave</button>
                </div>
            </div>
        </div>

    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { api } from '../api/client';
import TagListEditor from '../components/tags/TagListEditor.vue';
import StickyActionBanner from '../components/common/StickyActionBanner.vue';

const loading = ref(true);
const saving = ref(false);
const error = ref<string | null>(null);
const tags = reactive<Record<string, string[]>>({});
const entityTagKeys = ref<string[]>([]);
const cleanTags = ref('');
const searchQuery = ref('');
const showCancelDialog = ref(false);
const showLeaveDialog = ref(false);
let pendingNavigation: (() => void) | null = null;

const isDirty = computed(() => JSON.stringify(tags) !== cleanTags.value);

onBeforeRouteLeave((_to, _from, next) => {
    if (!isDirty.value) {
        next();
        return;
    }
    showLeaveDialog.value = true;
    pendingNavigation = () => next();
});

const filteredTags = computed({
    get: () => {
        if (!searchQuery.value.trim()) return tags;
        const q = searchQuery.value.toLowerCase();
        const result: Record<string, string[]> = {};
        for (const [key, values] of Object.entries(tags)) {
            if (key.toLowerCase().includes(q) || values.some(v => v.toLowerCase().includes(q))) {
                result[key] = values;
            }
        }
        return result;
    },
    set: (val) => {
        // The editor operates on the (possibly filtered) subset. Replace only
        // the keys that were shown, merging the edited entries back into the
        // full set so tags that did not match the search survive a save.
        const shown = Object.keys(filteredTags.value);
        for (const k of shown) delete tags[k];
        for (const [k, v] of Object.entries(val)) tags[k] = v;
    },
});

const suggestions = [
    '#c:ores', '#c:stone', '#c:logs', '#c:gems',
    '#minecraft:logs', '#minecraft:planks', '#minecraft:stone_tool_materials',
    '#minecraft:pickaxes', '#minecraft:axes', '#minecraft:shovels', '#minecraft:hoes',
    '#minecraft:coals', '#minecraft:copper_ores', '#minecraft:iron_ores',
    '#minecraft:gold_ores', '#minecraft:diamond_ores', '#minecraft:emerald_ores',
];

onMounted(fetchTags);

function confirmCancel() {
    showCancelDialog.value = true;
}

function discardTags() {
    showCancelDialog.value = false;
    cleanTags.value = JSON.stringify(tags);
    fetchTags();
}



async function leaveSave() {
    showLeaveDialog.value = false;
    saving.value = true;
    error.value = null;
    try {
        await api.tags.update({ ...tags });
        saving.value = false;
        pendingNavigation?.();
        pendingNavigation = null;
    } catch (e: any) {
        // A failed save must never silently navigate away: surface the error
        // and re-open the leave dialog so the admin can retry or discard.
        saving.value = false;
        error.value = e.message || 'Failed to save tags';
        showLeaveDialog.value = true;
    }
}

function leaveDiscard() {
    showLeaveDialog.value = false;
    pendingNavigation?.();
    pendingNavigation = null;
}

async function fetchTags() {
    loading.value = true;
    error.value = null;
    try {
        const data = await api.tags.get();
        Object.assign(tags, data.tags || {});
        entityTagKeys.value = Object.keys(data.entityTags || {});
        cleanTags.value = JSON.stringify(tags);
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
        window.location.reload();
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
    padding-bottom: 4rem;
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
.header-left {
    display: flex;
    align-items: center;
    gap: 0.75rem;
}
.count-badge {
    font-size: 0.75rem;
    font-weight: 600;
    color: var(--p-form-field-placeholder-color, #888);
    background: var(--p-content-border-color, #ddd);
    padding: 0.15rem 0.6rem;
    border-radius: 10px;
    line-height: 1.4;
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
.modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
}
.modal {
    background: var(--p-content-background);
    border: 1px solid var(--p-content-border-color);
    border-radius: 8px;
    padding: 1.5rem;
    max-width: 400px;
    width: 90%;
    box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}
.modal h3 {
    margin: 0 0 0.5rem;
    font-size: 1.05rem;
    color: var(--p-text-color);
}
.modal p {
    margin: 0 0 1.25rem;
    color: var(--p-text-muted-color, #888);
    font-size: 0.875rem;
    line-height: 1.4;
}
.modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
}
.loading {
    text-align: center;
    padding: 2rem;
    color: var(--p-text-muted-color, #888);
}

.page-subtitle {
    margin: 0.15rem 0 0;
    font-size: 0.8rem;
    color: var(--p-text-muted-color, #888);
}
.entity-tags-note {
    margin-top: 1.25rem;
    padding: 0.75rem;
    border: 1px dashed var(--p-content-border-color, #ccc);
    border-radius: 6px;
    font-size: 0.8rem;
    color: var(--p-text-muted-color, #888);
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}
.entity-tags-note strong {
    color: var(--p-text-color, #000);
}
.entity-tags-note code {
    font-family: monospace;
    background: var(--p-content-background, #f6f6f6);
    padding: 0 0.25rem;
    border-radius: 3px;
}
.entity-tags-list {
    display: flex;
    flex-wrap: wrap;
    gap: 0.4rem;
    margin-top: 0.25rem;
}
.entity-tag-key {
    font-family: monospace;
    font-size: 0.75rem;
    background: var(--p-content-background, #f0f0f0);
    border: 1px solid var(--p-content-border-color, #ddd);
    border-radius: 4px;
    padding: 0.15rem 0.5rem;
}
</style>
