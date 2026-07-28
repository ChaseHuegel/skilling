<template>
    <div class="editor-page">
        <div v-if="loading" class="loading">Loading skill...</div>
        <div v-else>
            <div
                class="editor-banner"
                :style="{ '--banner-color': form.color?.toLowerCase() || '#fff' }"
            >
                <div class="banner-left">
                    <MinecraftIcon :material="form.icon || 'minecraft:barrier'" :color="form.color" :size="48" />
                    <div class="banner-info">
                        <div class="banner-name">{{ form.displayName || form.id || 'New Skill' }}</div>
                        <div class="banner-meta">Level 1 – {{ form.maxLevel }}</div>
                    </div>
                </div>
                <div class="banner-actions">
                    <button class="btn btn-secondary" @click="confirmCancel">Cancel</button>
                    <button class="btn btn-primary" :disabled="saving" @click="save">
                        {{ saving ? 'Saving...' : 'Save Changes' }}
                    </button>
                </div>
            </div>

            <div v-if="error" class="error-banner">{{ error }}</div>
            <div v-if="Object.keys(fieldErrors).length > 0" class="error-banner">
                <div v-for="(msg, field) in fieldErrors" :key="field" class="field-error-line">
                    <strong>{{ field }}</strong>: {{ msg }}
                </div>
            </div>

            <div class="editor-sections">
                <fieldset class="section">
                    <legend>Identity</legend>
                    <SkillIdentitySection v-model="identityForm" :readonly="false" />
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
                    <XpSourcesSection v-model="form.xpSources" :tagSuggestions="tagSuggestions" />
                </fieldset>

                <fieldset class="section">
                    <legend>Abilities</legend>
                    <AbilitiesSection v-model="form.abilities" :tagSuggestions="tagSuggestions" />
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
import { ref, reactive, onMounted, computed, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { api } from '../api/client';
import { useSkillsStore } from '../stores/skills';
import MinecraftIcon from '../components/common/MinecraftIcon.vue';
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

const fieldErrors = reactive<Record<string, string>>({});

function clearFieldError(field: string) {
    delete fieldErrors[field];
}

function validate(): boolean {
    const errors: Record<string, string> = {};
    const trimmedId = form.id.trim();

    if (!trimmedId) {
        errors['id'] = 'Skill ID is required';
    } else if (isNew && skillsStore.skills.some((s: any) => s.id === trimmedId)) {
        errors['id'] = 'Skill ID already exists';
    }

    if (form.abilities && form.abilities.length > 0) {
        const seen = new Set<string>();
        for (let i = 0; i < form.abilities.length; i++) {
            const ab = form.abilities[i];
            if (!ab.id || !ab.id.trim()) {
                errors[`ability-${i}-id`] = 'Ability ID is required';
            } else if (seen.has(ab.id)) {
                errors[`ability-${i}-id`] = 'Duplicate ability ID';
            }
            seen.add(ab.id);
        }
    }

    Object.assign(fieldErrors, errors);
    return Object.keys(errors).length === 0;
}

const tagSuggestions = [
    '#c:ores', '#c:stone', '#c:logs', '#c:gems',
    '#minecraft:logs', '#minecraft:planks', '#minecraft:stone_tool_materials',
    '#minecraft:pickaxes', '#minecraft:axes', '#minecraft:shovels', '#minecraft:hoes',
    '#minecraft:coals', '#minecraft:copper_ores', '#minecraft:iron_ores',
    '#minecraft:gold_ores', '#minecraft:diamond_ores', '#minecraft:emerald_ores',
];

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
            await nextTick();
            const hash = route.hash;
            if (hash && hash.startsWith('#ability-')) {
                const abilityId = hash.replace('#ability-', '');
                const el = document.getElementById('ability-' + abilityId);
                if (el) {
                    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            }
        } catch (e: any) {
            error.value = e.message || 'Failed to load skill';
        } finally {
            loading.value = false;
        }
    }
});

async function save() {
    if (!validate()) return;
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
.editor-banner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    padding: 1rem;
    border: 1px solid var(--p-content-border-color);
    border-top: 3px solid var(--banner-color);
    border-radius: 8px;
    background: var(--p-content-background);
    margin-bottom: 1.5rem;
}
.banner-left {
    display: flex;
    align-items: center;
    gap: 0.75rem;
}
.banner-info {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
}
.banner-name {
    font-weight: 600;
    font-size: 1.1rem;
}
.banner-meta {
    font-size: 0.8rem;
    color: var(--p-text-muted-color, #888);
}
.banner-actions {
    display: flex;
    gap: 0.5rem;
    flex-shrink: 0;
}
.btn-danger {
    background: transparent;
    color: var(--p-red-500, #ef4444);
    border-color: var(--p-red-500, #ef4444);
}
.btn-danger:hover {
    background: color-mix(in srgb, var(--p-red-500, #ef4444) 10%, transparent);
}
.error-banner {
    background: var(--p-red-100, #fee2e2);
    color: var(--p-red-800, #991b1b);
    padding: 0.75rem;
    border-radius: 4px;
    margin-bottom: 1rem;
    font-size: 0.875rem;
}
.field-error-line {
    margin-bottom: 0.25rem;
}
.field-error-line:last-child {
    margin-bottom: 0;
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
    border: 1px solid var(--p-content-border-color);
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
    background: var(--p-content-background);
    border: 1px solid var(--p-content-border-color);
    border-radius: 8px;
    padding: 1.5rem;
    min-width: 300px;
}
.modal h3 {
    margin: 0 0 0.5rem;
    color: var(--p-text-color);
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
