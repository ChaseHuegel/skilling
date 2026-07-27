<template>
    <div class="editor-page">
        <div v-if="loading" class="loading">Loading skill...</div>
        <div v-else>
            <div class="editor-header">
                <h1>{{ isNew ? 'Create Skill' : `Edit: ${form.displayName || form.id}` }}</h1>
                <div class="header-actions">
                    <button class="btn btn-secondary" @click="confirmCancel">Cancel</button>
                    <button class="btn btn-primary" :disabled="saving" @click="save">
                        {{ saving ? 'Saving...' : 'Save Changes' }}
                    </button>
                </div>
            </div>

            <div v-if="error" class="error-banner">{{ error }}</div>

            <div class="editor-sections">
                <fieldset class="section">
                    <legend>Identity</legend>
                    <SkillIdentitySection v-model="identityForm" :readonly="!isNew" />
                </fieldset>

                <fieldset class="section">
                    <legend>Display</legend>
                    <DisplaySection v-model="displayForm" />
                </fieldset>

                <fieldset class="section">
                    <legend>Progression</legend>
                    <ProgressionSection v-model="progressionForm" />
                </fieldset>

                <fieldset class="section">
                    <legend>XP Sources</legend>
                    <XpSourcesSection v-model="form.xpSources" />
                </fieldset>

                <fieldset class="section">
                    <legend>Abilities</legend>
                    <AbilitiesSection v-model="form.abilities" />
                </fieldset>
            </div>
        </div>

        <!-- Cancel confirm dialog -->
        <div v-if="showCancelDialog" class="modal-overlay" @click.self="showCancelDialog = false">
            <div class="modal">
                <h3>Discard changes?</h3>
                <p>Any unsaved changes will be lost.</p>
                <div class="modal-actions">
                    <button class="btn btn-secondary" @click="showCancelDialog = false">Keep Editing</button>
                    <button class="btn btn-danger" @click="discard">Discard</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api/client';
import { useSkillsStore } from '../stores/skills';
import SkillIdentitySection from '../components/skills/SkillIdentitySection.vue';
import DisplaySection from '../components/skills/DisplaySection.vue';
import ProgressionSection from '../components/skills/ProgressionSection.vue';
import XpSourcesSection from '../components/skills/XpSourcesSection.vue';
import AbilitiesSection from '../components/skills/AbilitiesSection.vue';

const route = useRoute();
const router = useRouter();
const skillsStore = useSkillsStore();

const skillId = route.params.id as string;
const isNew = route.name === 'SkillNew';
const loading = ref(false);
const saving = ref(false);
const error = ref<string | null>(null);
const showCancelDialog = ref(false);

const form = reactive<Record<string, any>>({
    id: '',
    displayName: '',
    maxLevel: 100,
    icon: 'minecraft:barrier',
    customModelData: 0,
    color: 'GREEN',
    style: 'SOLID',
    progression: { curve: 'polynomial', baseXp: 50, exponent: 2.5 },
    xpSources: [] as any[],
    abilities: [] as any[],
});

const identityForm = computed({
    get: () => ({ id: form.id, displayName: form.displayName, maxLevel: form.maxLevel }),
    set: (val: any) => { form.id = val.id; form.displayName = val.displayName; form.maxLevel = val.maxLevel; },
});
const displayForm = computed({
    get: () => ({ icon: form.icon, customModelData: form.customModelData, color: form.color, style: form.style }),
    set: (val: any) => { form.icon = val.icon; form.customModelData = val.customModelData; form.color = val.color; form.style = val.style; },
});
const progressionForm = computed({
    get: () => form.progression,
    set: (val: any) => { form.progression = val; },
});

onMounted(async () => {
    if (!isNew && skillId) {
        loading.value = true;
        try {
            const data = await api.skills.get(skillId);
            Object.assign(form, data);
        } catch (e: any) {
            error.value = e.message || 'Failed to load skill';
        } finally {
            loading.value = false;
        }
    }
});

async function save() {
    saving.value = true;
    error.value = null;
    try {
        const payload: Record<string, any> = {
            id: form.id,
            displayName: form.displayName,
            maxLevel: form.maxLevel,
            icon: form.icon,
            customModelData: form.customModelData,
            color: form.color,
            style: form.style,
            progression: form.progression,
            xpSources: form.xpSources,
            abilities: form.abilities,
        };
        if (isNew) {
            await api.skills.create(payload);
        } else {
            await api.skills.update(skillId || form.id, payload);
        }
        router.push('/');
    } catch (e: any) {
        error.value = e.message || 'Failed to save skill';
    } finally {
        saving.value = false;
    }
}

function confirmCancel() {
    showCancelDialog.value = true;
}

function discard() {
    router.push('/');
}
</script>

<style scoped>
.editor-page {
    max-width: 900px;
    margin: 0 auto;
    padding: 1.5rem;
}
.editor-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 1.5rem;
}
.editor-header h1 {
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
.btn-danger {
    background: var(--p-red-600, #dc2626);
    color: white;
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
    padding: 3rem;
    color: var(--p-text-muted-color, #888);
}
.editor-sections {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}
.section {
    border: 1px solid var(--p-surface-border, #ddd);
    border-radius: 6px;
    padding: 1rem;
}
.section legend {
    font-weight: 600;
    font-size: 1.1rem;
    padding: 0 0.5rem;
}
.modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0,0,0,0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
}
.modal {
    background: var(--p-surface-section, #fff);
    border-radius: 8px;
    padding: 1.5rem;
    min-width: 300px;
}
.modal h3 {
    margin: 0 0 0.5rem;
}
.modal p {
    color: var(--p-text-muted-color, #666);
    margin: 0 0 1rem;
}
.modal-actions {
    display: flex;
    gap: 0.5rem;
    justify-content: flex-end;
}
</style>
