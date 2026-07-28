<template>
    <div class="dashboard">
        <div class="dashboard-header">
            <div class="header-left">
                <h1>Skills</h1>
                <span v-if="!loading && skills.length > 0" class="skill-count-badge">{{ skills.length }} skill{{ skills.length !== 1 ? 's' : '' }}</span>
            </div>
            <div class="header-actions">
                <button v-if="staging.hasPending" class="btn btn-danger" @click="showResetDialog = true">Reset</button>
                <button class="btn btn-primary" @click="createSkill">
                    <svg class="plus-icon" viewBox="0 0 16 16" width="14" height="14" fill="none">
                        <path d="M8 2v12M2 8h12" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
                    </svg>
                    New Skill
                </button>
            </div>
        </div>
        <p class="dashboard-subtitle">Manage your skill definitions and abilities</p>

        <div class="search-bar">
            <svg class="search-icon" viewBox="0 0 16 16" width="14" height="14" fill="none">
                <circle cx="7" cy="7" r="5" stroke="currentColor" stroke-width="1.5" />
                <path d="M11 11l3 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
            <input
                v-model="searchQuery"
                class="search-input"
                type="text"
                placeholder="Search skills..."
            />
        </div>

        <!-- Loading state: skeleton cards -->
        <div v-if="loading" class="skill-grid">
            <div v-for="i in 3" :key="i" class="skeleton-card">
                <div class="skeleton-header">
                    <div class="skeleton-icon" />
                    <div class="skeleton-badge" />
                </div>
                <div class="skeleton-body">
                    <div class="skeleton-line w-70" />
                    <div class="skeleton-line w-40" />
                </div>
            </div>
        </div>

        <!-- Error state -->
        <div v-else-if="error" class="state-card error-state">
            <div class="state-icon">⚠️</div>
            <h2 class="state-title">Failed to load skills</h2>
            <p class="state-desc">{{ error }}</p>
                <button class="btn btn-secondary btn-sm" @click="fetchSkills">Retry</button>
        </div>

        <!-- Skill grid or empty state -->
        <div v-else class="skill-grid">
            <SkillCard
                v-for="s in filteredSkills"
                :key="s.id"
                :skill="s"
                @duplicate="duplicateSkill"
                @delete="confirmDeleteSkill"
            />
            <div v-if="searchQuery.trim() && filteredSkills.length === 0" class="state-card empty-state">
                <h2 class="state-title">No skills match your search</h2>
                <p class="state-desc">Try adjusting your search query.</p>
            </div>
            <div v-else-if="skills.length === 0" class="state-card empty-state">
                <div class="state-icon">📦</div>
                <h2 class="state-title">No skills yet</h2>
                <p class="state-desc">Create your first skill definition to get started.</p>
            <button class="btn btn-primary" @click="createSkill">
                    <svg class="plus-icon" viewBox="0 0 16 16" width="14" height="14" fill="none">
                        <path d="M8 2v12M2 8h12" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
                    </svg>
                    Create Skill
                </button>
            </div>
        </div>

        <!-- Delete skill confirm dialog -->
        <div v-if="showDeleteDialog" class="modal-overlay" @click.self="showDeleteDialog = false">
            <div class="modal">
                <h3>Delete skill?</h3>
                <p>This will permanently remove <strong>{{ deleteTargetName }}</strong> and all its data.</p>
                <div class="modal-actions">
                    <button class="btn btn-secondary" @click="showDeleteDialog = false">Keep</button>
                    <button class="btn btn-danger" @click="executeDeleteSkill">Delete</button>
                </div>
            </div>
        </div>

        <!-- Reset confirm dialog -->
        <div v-if="showResetDialog" class="modal-overlay" @click.self="showResetDialog = false">
            <div class="modal">
                <h3>Discard all pending changes?</h3>
                <p>This will remove all staged edits to skills, tags, and configuration. The pending changes banner will disappear.</p>
                <div class="modal-actions">
                    <button class="btn btn-secondary" @click="showResetDialog = false">Keep Editing</button>
                    <button class="btn btn-danger" @click="confirmReset">Discard</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api/client';
import { useStagingStore } from '../stores/staging';
import SkillCard from '../components/skills/SkillCard.vue';

const router = useRouter();
const staging = useStagingStore();
const skills = ref<any[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const searchQuery = ref('');
const showResetDialog = ref(false);
const showDeleteDialog = ref(false);
const deleteTarget = ref<string | null>(null);
const deleteTargetName = ref('');

const deletedSkillIds = computed(() => {
    const ids: string[] = [];
    for (const f of staging.files) {
        const match = f.match(/^deleted_skills\/(.+)\.yml\.deleted$/);
        if (match) ids.push(match[1]);
    }
    return ids;
});

const filteredSkills = computed(() => {
    const q = searchQuery.value.trim().toLowerCase();
    return skills.value.filter((s) => {
        if (deletedSkillIds.value.includes(s.id)) return false;
        if (!q) return true;
        const id = (s.id || '').toLowerCase();
        const display = (s.displayName || '').toLowerCase();
        const triggers = (s.xpSourceTriggers || []).join(' ').toLowerCase();
        const abilityIds = (s.abilityIds || []).join(' ').toLowerCase();
        const abilityNames = (s.abilityNames || []).join(' ').toLowerCase();
        return [id, display, triggers, abilityIds, abilityNames].some(f => f.includes(q));
    });
});

async function fetchSkills() {
    loading.value = true;
    error.value = null;
    try {
        skills.value = await api.skills.list();
        staging.fetchStatus();
    } catch (e: any) {
        error.value = e.message;
    } finally {
        loading.value = false;
    }
}

onMounted(fetchSkills);

function createSkill() {
    router.push('/skills/new');
}

async function duplicateSkill(id: string) {
    const detail = await api.skills.get(id);
    let copyId = id + '_1';
    let attempts = 0;
    const existingIds = new Set(skills.value.map((s: any) => s.id));
    while (existingIds.has(copyId) && attempts < 100) {
        const num = parseInt(copyId.replace(/.*_(\d+)$/, '$1')) + 1;
        copyId = id + '_' + num;
        attempts++;
    }
    detail.id = copyId;
    detail.displayName = (detail.displayName || id) + ' (copy)';
    await api.skills.create(detail);
    router.push(`/skills/${copyId}`);
}

function confirmDeleteSkill(id: string) {
    const skill = skills.value.find((s: any) => s.id === id);
    deleteTarget.value = id;
    deleteTargetName.value = skill?.displayName || skill?.id || id;
    showDeleteDialog.value = true;
}

async function executeDeleteSkill() {
    const id = deleteTarget.value;
    if (!id) return;
    showDeleteDialog.value = false;
    deleteTarget.value = null;
    try {
        await api.skills.delete(id);
        await staging.fetchStatus();
    } catch { /* ignore */ }
}

async function confirmReset() {
    showResetDialog.value = false;
    await staging.discard();
    await staging.fetchStatus();
}
</script>

<style scoped>
.dashboard {
    max-width: 960px;
    margin: 0 auto;
    padding: 1.5rem;
}

/* ---- Header ---- */
.dashboard-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.5rem;
}
.dashboard-header h1 {
    margin: 0;
    font-size: 1.5rem;
}
.header-left {
    display: flex;
    align-items: center;
    gap: 0.75rem;
}
.skill-count-badge {
    font-size: 0.75rem;
    font-weight: 600;
    color: var(--p-form-field-placeholder-color);
    background: var(--p-content-border-color);
    padding: 0.15rem 0.6rem;
    border-radius: 10px;
    line-height: 1.4;
}
.dashboard-subtitle {
    margin: 0 0 1rem;
    color: var(--p-form-field-placeholder-color);
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
.plus-icon {
    flex-shrink: 0;
}

.skill-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 1rem;
}

@media (max-width: 960px) {
    .skill-grid {
        grid-template-columns: repeat(2, 1fr);
    }
}

@media (max-width: 640px) {
    .skill-grid {
        grid-template-columns: 1fr;
    }
    .dashboard-header {
        flex-direction: column;
        gap: 0.75rem;
        align-items: stretch;
    }
    .dashboard-header h1 {
        text-align: center;
    }
    .header-left {
        justify-content: center;
    }
}

/* ---- Skeleton Cards ---- */
.skeleton-card {
    border: 1px solid var(--p-content-border-color);
    border-radius: 8px;
    background: var(--p-content-background);
    overflow: hidden;
}
.skeleton-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 1rem 1rem 0.5rem;
}
.skeleton-icon {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    background: linear-gradient(
        90deg,
        var(--p-content-border-color) 25%,
        var(--p-content-hover-background) 50%,
        var(--p-content-border-color) 75%
    );
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
}
.skeleton-badge {
    width: 24px;
    height: 20px;
    border-radius: 10px;
    background: var(--p-content-border-color);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
}
.skeleton-body {
    padding: 0.5rem 1rem 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}
.skeleton-line {
    height: 14px;
    border-radius: 4px;
    background: linear-gradient(
        90deg,
        var(--p-content-border-color) 25%,
        var(--p-content-hover-background) 50%,
        var(--p-content-border-color) 75%
    );
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
}
.skeleton-line.w-70 { width: 70%; }
.skeleton-line.w-40 { width: 40%; }

.header-actions {
    display: flex;
    gap: 0.5rem;
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

@keyframes shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
}

/* ---- State Cards (empty/error) ---- */
.state-card {
    grid-column: 1 / -1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3rem 1.5rem;
    text-align: center;
}
.state-icon {
    font-size: 2.5rem;
    margin-bottom: 0.75rem;
}
.state-title {
    margin: 0 0 0.35rem;
    font-size: 1.1rem;
    font-weight: 600;
}
.state-desc {
    margin: 0 0 1.25rem;
    color: var(--p-form-field-placeholder-color);
    font-size: 0.875rem;
    max-width: 320px;
}
.error-state .state-icon {
    font-size: 2rem;
}
/* ---- Card Entrance Animation ---- */
.skill-card {
    animation: cardEnter 0.35s ease both;
}
.skill-card:nth-child(1) { animation-delay: 0ms; }
.skill-card:nth-child(2) { animation-delay: 50ms; }
.skill-card:nth-child(3) { animation-delay: 100ms; }
.skill-card:nth-child(4) { animation-delay: 150ms; }
.skill-card:nth-child(5) { animation-delay: 200ms; }
.skill-card:nth-child(6) { animation-delay: 250ms; }
.skill-card:nth-child(7) { animation-delay: 300ms; }
.skill-card:nth-child(8) { animation-delay: 350ms; }
.skill-card:nth-child(9) { animation-delay: 400ms; }
.skill-card:nth-child(10) { animation-delay: 450ms; }

@keyframes cardEnter {
    from { opacity: 0; transform: translateY(12px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
