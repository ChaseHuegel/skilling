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
                        <div v-if="form.lore && form.lore.length > 0" class="banner-lore">
                            <div
                                v-for="(line, i) in form.lore"
                                :key="i"
                                class="banner-lore-line"
                            >{{ line }}</div>
                        </div>
                    </div>
                </div>
                <div class="banner-actions">
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
                  <legend>Level-up Commands</legend>
                  <LevelUpCommandsSection v-model="form.levelUpCommands" />
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

        <StickyActionBanner :visible="isDirty" :saving="saving" @save="save" @cancel="confirmCancel" />

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
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router';
import { api } from '../api/client';
import { useSkillsStore } from '../stores/skills';
import MinecraftIcon from '../components/common/MinecraftIcon.vue';
import SkillIdentitySection from '../components/skills/SkillIdentitySection.vue';
import DisplaySection from '../components/skills/DisplaySection.vue';
import ProgressionSection from '../components/skills/ProgressionSection.vue';
import XpSourcesSection from '../components/skills/XpSourcesSection.vue';
import AbilitiesSection from '../components/skills/AbilitiesSection.vue';
import LevelUpCommandsSection from '../components/skills/LevelUpCommandsSection.vue';
import StickyActionBanner from '../components/common/StickyActionBanner.vue';

const route = useRoute();
const router = useRouter();
const skillsStore = useSkillsStore();

const skillId = route.params.id as string;
const isNew = route.name === 'SkillNew';
const loading = ref(false);
const saving = ref(false);
const error = ref<string | null>(null);
const showCancelDialog = ref(false);
const showLeaveDialog = ref(false);
let pendingNavigation: (() => void) | null = null;

const fieldErrors = reactive<Record<string, string>>({});
const cleanForm = ref('');

const isDirty = computed(() => JSON.stringify(form) !== cleanForm.value);

onBeforeRouteLeave((to, from, next) => {
    if (!isDirty.value) {
        next();
        return;
    }
    showLeaveDialog.value = true;
    pendingNavigation = () => next();
});

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
            if (!ab.trigger || !ab.trigger.trim()) {
                errors[`ability-${i}-trigger`] = 'Ability trigger is required';
            }
            seen.add(ab.id);
        }
    }

    Object.assign(fieldErrors, errors);
    return Object.keys(errors).length === 0;
}

const TAG_SUGGESTIONS_BASE = [
    '#c:ores', '#c:stone', '#c:logs', '#c:gems',
    '#minecraft:logs', '#minecraft:planks', '#minecraft:stone_tool_materials',
    '#minecraft:pickaxes', '#minecraft:axes', '#minecraft:shovels', '#minecraft:hoes',
    '#minecraft:coals', '#minecraft:copper_ores', '#minecraft:iron_ores',
    '#minecraft:gold_ores', '#minecraft:diamond_ores', '#minecraft:emerald_ores',
];

const MATERIAL_SUGGESTIONS = [
    'minecraft:stone', 'minecraft:andesite', 'minecraft:diorite', 'minecraft:granite',
    'minecraft:dirt', 'minecraft:grass_block', 'minecraft:sand', 'minecraft:gravel',
    'minecraft:oak_log', 'minecraft:spruce_log', 'minecraft:birch_log', 'minecraft:jungle_log',
    'minecraft:dark_oak_log', 'minecraft:acacia_log', 'minecraft:mangrove_log', 'minecraft:cherry_log',
    'minecraft:oak_planks', 'minecraft:spruce_planks', 'minecraft:birch_planks',
    'minecraft:cobblestone', 'minecraft:deepslate', 'minecraft:tuff', 'minecraft:calcite',
    'minecraft:iron_ore', 'minecraft:copper_ore', 'minecraft:gold_ore', 'minecraft:diamond_ore',
    'minecraft:emerald_ore', 'minecraft:lapis_ore', 'minecraft:redstone_ore', 'minecraft:coal_ore',
    'minecraft:netherrack', 'minecraft:nether_gold_ore', 'minecraft:nether_quartz_ore',
    'minecraft:ancient_debris', 'minecraft:end_stone', 'minecraft:obsidian',
    'minecraft:diamond_pickaxe', 'minecraft:iron_pickaxe', 'minecraft:stone_pickaxe',
    'minecraft:netherite_pickaxe', 'minecraft:diamond_axe', 'minecraft:iron_axe',
    'minecraft:stone_axe', 'minecraft:netherite_axe', 'minecraft:diamond_shovel',
    'minecraft:iron_shovel', 'minecraft:netherite_shovel',
    'minecraft:diamond_hoe', 'minecraft:netherite_hoe',
    'minecraft:cobblestone', 'minecraft:iron_ingot', 'minecraft:gold_ingot',
    'minecraft:diamond', 'minecraft:emerald', 'minecraft:netherite_scrap',
    'minecraft:redstone', 'minecraft:coal', 'minecraft:lapis_lazuli',
    'minecraft:copper_ingot', 'minecraft:raw_iron', 'minecraft:raw_gold', 'minecraft:raw_copper',
    'minecraft:wheat', 'minecraft:carrot', 'minecraft:potato', 'minecraft:beetroot',
    'minecraft:apple', 'minecraft:golden_apple', 'minecraft:enchanted_golden_apple',
    'minecraft:rotten_flesh', 'minecraft:bone', 'minecraft:string', 'minecraft:feather',
    'minecraft:gunpowder', 'minecraft:blaze_rod', 'minecraft:blaze_powder',
    'minecraft:ender_pearl', 'minecraft:eye_of_ender', 'minecraft:ghast_tear',
    'minecraft:magma_cream', 'minecraft:slime_ball', 'minecraft:spider_eye',
    'minecraft:fermented_spider_eye', 'minecraft:golden_carrot', 'minecraft:glistering_melon_slice',
    'minecraft:potion', 'minecraft:experience_bottle', 'minecraft:book', 'minecraft:enchanted_book',
    'minecraft:paper', 'minecraft:map', 'minecraft:compass', 'minecraft:clock',
    'minecraft:leather', 'minecraft:rabbit_hide', 'minecraft:scute', 'minecraft:nautilus_shell',
    'minecraft:heart_of_the_sea', 'minecraft:prismarine_shard', 'minecraft:prismarine_crystals',
];

const tagSuggestions = [...TAG_SUGGESTIONS_BASE, ...MATERIAL_SUGGESTIONS];

const form = reactive<Record<string, any>>({
    id: '',
    displayName: '',
    maxLevel: 100,
    icon: 'minecraft:barrier',
    customModelData: 0,
    color: 'GREEN',
    style: 'SOLID',
    lore: [] as string[],
    progression: { curve: 'polynomial', baseXp: 50, exponent: 2.5 },
    xpSources: [] as any[],
    abilities: [] as any[],
    levelUpCommands: [] as string[],
});

const identityForm = computed({
    get: () => ({ id: form.id, displayName: form.displayName, maxLevel: form.maxLevel }),
    set: (val: any) => { form.id = val.id; form.displayName = val.displayName; form.maxLevel = val.maxLevel; },
});
const displayForm = computed({
    get: () => ({ icon: form.icon, customModelData: form.customModelData, color: form.color, style: form.style, lore: form.lore || [] }),
    set: (val: any) => { form.icon = val.icon; form.customModelData = val.customModelData; form.color = val.color; form.style = val.style; form.lore = val.lore || []; },
});
const progressionForm = computed({
    get: () => form.progression,
    set: (val: any) => { form.progression = val; },
});

function convertParticleOffsets(particles: any[]): any[] {
    return (particles || []).map((p: any) => {
        if (p.offset && Array.isArray(p.offset)) {
            return { ...p, offsetX: p.offset[0] || 0, offsetY: p.offset[1] || 0, offsetZ: p.offset[2] || 0, offset: undefined };
        }
        return p;
    });
}

function revertParticleOffsets(particles: any[]): any[] {
    return (particles || []).map((p: any) => {
        if (p.offsetX !== undefined || p.offsetY !== undefined || p.offsetZ !== undefined) {
            return { ...p, offset: [p.offsetX || 0, p.offsetY || 0, p.offsetZ || 0], offsetX: undefined, offsetY: undefined, offsetZ: undefined };
        }
        return p;
    });
}

function apiAbilityToForm(ab: any): any {
    return {
        ...ab,
        lore: ab.display?.lore || [],
        display: undefined,
        feedback: ab.feedback ? {
            ...ab.feedback,
            particles: convertParticleOffsets(ab.feedback.particles),
            sounds: ab.feedback.sounds || [],
        } : ab.feedback,
        requirements: {
            ...ab.requirements,
            cooldown: cooldownToNumber(ab.requirements?.cooldown),
            items: (ab.requirements?.items || []).map((item: any) => ({
                action: item.action || 'possession',
                tag: item.tag || '',
                slot: item.slot || '',
                amount: item.amount || 1,
                itemCooldown: item.itemCooldown || 0,
            })),
        },
        mechanics: (ab.mechanics || []).map((m: any) => ({
            ...m,
            params: Object.entries(m.parameters || {}).map(([name, evaluator]) => ({ name, evaluator })),
            parameters: undefined,
        })),
    };
}

function cooldownToNumber(cooldown: any): number {
    if (typeof cooldown === 'number') return cooldown;
    if (cooldown && typeof cooldown === 'object') {
        const value = cooldown.params?.value;
        if (typeof value === 'number') return value;
    }
    return 0;
}

function formAbilityToApi(ab: any): any {
    const result: any = {
        ...ab,
        lore: undefined,
        display: { lore: ab.lore || [] },
        feedback: ab.feedback ? {
            ...ab.feedback,
            particles: revertParticleOffsets(ab.feedback.particles),
        } : ab.feedback,
        mechanics: (ab.mechanics || []).map((m: any) => {
            const params: Record<string, any> = {};
            for (const p of m.params || []) {
                if (p.name) params[p.name] = p.evaluator;
            }
            return { ...m, params: undefined, parameters: params };
        }),
    };
    return result;
}

onMounted(async () => {
    if (isNew) {
        await nextTick();
        cleanForm.value = JSON.stringify(form);
    }
    if (!isNew && skillId) {
        loading.value = true;
        try {
            const data = await api.skills.get(skillId);
            if (data.abilities) {
                data.abilities = data.abilities.map(apiAbilityToForm);
            }
            Object.assign(form, data);
            cleanForm.value = JSON.stringify(form);
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
            abilities: (form.abilities || []).map(formAbilityToApi),
            levelUpCommands: form.levelUpCommands || [],
        };
        if (isNew) {
            await api.skills.create(payload);
        } else {
            await api.skills.update(skillId || form.id, payload);
        }
        window.location.reload();
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
    showCancelDialog.value = false;
    cleanForm.value = JSON.stringify(form);
    router.push('/');
}

async function leaveSave() {
    showLeaveDialog.value = false;
    if (!validate()) {
        pendingNavigation = null;
        return;
    }
    saving.value = true;
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
            abilities: (form.abilities || []).map(formAbilityToApi),
            levelUpCommands: form.levelUpCommands || [],
        };
        if (isNew) {
            await api.skills.create(payload);
        } else {
            await api.skills.update(skillId || form.id, payload);
        }
    } catch { /* navigate anyway */ }
    saving.value = false;
    pendingNavigation?.();
    pendingNavigation = null;
}

function leaveDiscard() {
    showLeaveDialog.value = false;
    showCancelDialog.value = false;
    pendingNavigation?.();
    pendingNavigation = null;
}
</script>

<style scoped>
.editor-page {
    max-width: 900px;
    margin: 0 auto;
    padding: 1.5rem;
    padding-bottom: 4rem;
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
.banner-lore {
    display: flex;
    flex-direction: column;
    gap: 0.1rem;
    margin-top: 0.25rem;
}
.banner-lore-line {
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
