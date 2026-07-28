<template>
    <div class="abilities-page">
        <div class="page-header">
            <h1>Abilities</h1>
            <span v-if="!loading && abilities.length > 0" class="count-badge">{{ abilities.length }} abilit{{ abilities.length !== 1 ? 'ies' : 'y' }}</span>
        </div>
        <p class="page-subtitle">All abilities across all skills</p>

        <div class="search-bar">
            <svg class="search-icon" viewBox="0 0 16 16" width="14" height="14" fill="none">
                <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.5" />
                <path d="M11 11l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
            <input
                v-model="searchQuery"
                class="search-input"
                type="text"
                placeholder="Search abilities..."
            />
        </div>

        <div v-if="loading" class="loading-grid">
            <div v-for="i in 4" :key="i" class="skeleton-card">
                <div class="skeleton-header" />
                <div class="skeleton-body">
                    <div class="skeleton-line w-50" />
                    <div class="skeleton-line w-30" />
                </div>
            </div>
        </div>

        <div v-else-if="error" class="state-card error-state">
            <h2 class="state-title">Failed to load abilities</h2>
            <p class="state-desc">{{ error }}</p>
            <button class="btn btn-secondary btn-sm" @click="fetchAll">Retry</button>
        </div>

        <div v-else-if="searchQuery.trim() && filteredAbilities.length === 0" class="state-card empty-state">
            <h2 class="state-title">No abilities match your search</h2>
            <p class="state-desc">Try adjusting your search query.</p>
        </div>

        <div v-else-if="abilities.length === 0" class="state-card empty-state">
            <h2 class="state-title">No abilities found</h2>
            <p class="state-desc">Create skills with abilities to see them here.</p>
        </div>

        <div v-else class="abilities-grid">
            <AbilityCard
                v-for="a in filteredAbilities"
                :key="a.skillId + '-' + a.ability.id"
                :ability="a.ability"
                :skill-id="a.skillId"
                :skill-display-name="a.skillDisplayName"
                :skill-color="a.skillColor"
            />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { api } from '../api/client';
import AbilityCard from '../components/skills/AbilityCard.vue';

interface AbilityEntry {
    skillId: string;
    skillDisplayName: string;
    skillColor: string;
    ability: {
        id: string;
        displayName?: string;
        unlockLevel: number;
        requirements?: {
            cooldown?: number;
            state?: string[];
            items?: any[];
        };
        mechanics?: {
            type: string;
            filters?: any[];
            parameters?: any;
        }[];
        feedback?: {
            actionBar?: boolean;
            chat?: boolean;
            message?: string;
            particles?: any[];
            sounds?: any[];
        };
    };
}

const abilities = ref<AbilityEntry[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const searchQuery = ref('');

const filteredAbilities = computed(() => {
    if (!searchQuery.value.trim()) return abilities.value;
    const q = searchQuery.value.toLowerCase();
    return abilities.value.filter((a) => {
        const id = (a.ability.id || '').toLowerCase();
        const displayName = (a.ability.displayName || '').toLowerCase();
        const skillId = (a.skillId || '').toLowerCase();
        const skillDisplay = (a.skillDisplayName || '').toLowerCase();
        const unlockLevel = String(a.ability.unlockLevel);
        const states = (a.ability.requirements?.state || []).join(' ').toLowerCase();
        const feedbackMsg = (a.ability.feedback?.message || '').toLowerCase();
        const mechanics = (a.ability.mechanics || []).map((m: any) => m.type).join(' ').toLowerCase();
        const fields = [id, displayName, skillId, skillDisplay, unlockLevel, states, feedbackMsg, mechanics];
        return fields.some(f => f.includes(q));
    });
});

onMounted(fetchAll);

async function fetchAll() {
    loading.value = true;
    error.value = null;
    try {
        const skills = await api.skills.list();
        const results: AbilityEntry[] = [];
        for (const s of skills) {
            const detail = await api.skills.get(s.id);
            if (detail.abilities) {
                for (const ab of detail.abilities) {
                    results.push({
                        skillId: s.id,
                        skillDisplayName: s.displayName || s.id,
                        skillColor: s.color || 'WHITE',
                        ability: ab,
                    });
                }
            }
        }
        results.sort((a, b) => {
            const skillCmp = a.skillDisplayName.localeCompare(b.skillDisplayName);
            if (skillCmp !== 0) return skillCmp;
            const levelCmp = a.ability.unlockLevel - b.ability.unlockLevel;
            if (levelCmp !== 0) return levelCmp;
            return (a.ability.displayName || a.ability.id).localeCompare(b.ability.displayName || b.ability.id);
        });
        abilities.value = results;
    } catch (e: any) {
        error.value = e.message || 'Failed to load abilities';
    } finally {
        loading.value = false;
    }
}
</script>

<style scoped>
.abilities-page {
    max-width: 960px;
    margin: 0 auto;
    padding: 1.5rem;
}
.page-header {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 0.25rem;
}
.page-header h1 {
    margin: 0;
    font-size: 1.5rem;
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
.page-subtitle {
    margin: 0 0 1rem;
    color: var(--p-form-field-placeholder-color, #888);
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
.abilities-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 1rem;
}
@media (max-width: 640px) {
    .abilities-grid {
        grid-template-columns: 1fr;
    }
}
.loading-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 1rem;
}
.skeleton-card {
    border: 1px solid var(--p-content-border-color, #ddd);
    border-radius: 8px;
    overflow: hidden;
}
.skeleton-header {
    height: 44px;
    background: linear-gradient(90deg, var(--p-content-border-color, #ddd) 25%, var(--p-content-hover-background, #eee) 50%, var(--p-content-border-color, #ddd) 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
}
.skeleton-body {
    padding: 0.75rem 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}
.skeleton-line {
    height: 14px;
    border-radius: 4px;
    background: linear-gradient(90deg, var(--p-content-border-color, #ddd) 25%, var(--p-content-hover-background, #eee) 50%, var(--p-content-border-color, #ddd) 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
}
.skeleton-line.w-50 { width: 50%; }
.skeleton-line.w-30 { width: 30%; }
@keyframes shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}
.state-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 3rem 1.5rem;
    text-align: center;
}
.state-title {
    margin: 0 0 0.35rem;
    font-size: 1.1rem;
    font-weight: 600;
}
.state-desc {
    margin: 0 0 1.25rem;
    color: var(--p-form-field-placeholder-color, #888);
    font-size: 0.875rem;
    max-width: 320px;
}
</style>
