<template>
    <div class="dashboard">
        <PendingChangesBanner />
        <div class="dashboard-header">
            <div class="header-left">
                <h1>Skills</h1>
                <span v-if="!loading && skills.length > 0" class="skill-count-badge">{{ skills.length }} skill{{ skills.length !== 1 ? 's' : '' }}</span>
            </div>
            <button class="create-btn" @click="createSkill">
                <svg class="plus-icon" viewBox="0 0 16 16" width="14" height="14" fill="none">
                    <path d="M8 2v12M2 8h12" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
                </svg>
                New Skill
            </button>
        </div>
        <p class="dashboard-subtitle">Manage your skill definitions and abilities</p>

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
            <button class="retry-btn" @click="fetchSkills">Retry</button>
        </div>

        <!-- Skill grid or empty state -->
        <div v-else class="skill-grid">
            <SkillCard v-for="s in skills" :key="s.id" :skill="s" />
            <div v-if="skills.length === 0" class="state-card empty-state">
                <div class="state-icon">📦</div>
                <h2 class="state-title">No skills yet</h2>
                <p class="state-desc">Create your first skill definition to get started.</p>
                <button class="create-btn" @click="createSkill">
                    <svg class="plus-icon" viewBox="0 0 16 16" width="14" height="14" fill="none">
                        <path d="M8 2v12M2 8h12" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" />
                    </svg>
                    Create Skill
                </button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { api } from '../api/client';
import { useStagingStore } from '../stores/staging';
import SkillCard from '../components/skills/SkillCard.vue';
import PendingChangesBanner from '../components/layout/PendingChangesBanner.vue';

const router = useRouter();
const staging = useStagingStore();
const skills = ref<any[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

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
    color: var(--p-text-muted-color);
    background: var(--p-surface-border);
    padding: 0.15rem 0.6rem;
    border-radius: 10px;
    line-height: 1.4;
}
.dashboard-subtitle {
    margin: 0 0 1.5rem;
    color: var(--p-text-muted-color);
    font-size: 0.875rem;
}
.create-btn {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    background: var(--p-primary-color);
    color: white;
    border: none;
    padding: 0.5rem 1rem;
    border-radius: 6px;
    cursor: pointer;
    font-size: 0.875rem;
    font-weight: 500;
    transition: background 0.15s, transform 0.15s;
}
.create-btn:hover {
    filter: brightness(1.1);
    transform: scale(1.02);
}
.create-btn:active {
    transform: scale(0.98);
}
.plus-icon {
    flex-shrink: 0;
}

/* ---- Skill Grid ---- */
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
    border: 1px solid var(--p-surface-border);
    border-radius: 8px;
    background: var(--p-surface-section);
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
        var(--p-surface-border) 25%,
        var(--p-surface-hover) 50%,
        var(--p-surface-border) 75%
    );
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
}
.skeleton-badge {
    width: 24px;
    height: 20px;
    border-radius: 10px;
    background: var(--p-surface-border);
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
        var(--p-surface-border) 25%,
        var(--p-surface-hover) 50%,
        var(--p-surface-border) 75%
    );
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
}
.skeleton-line.w-70 { width: 70%; }
.skeleton-line.w-40 { width: 40%; }

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
    color: var(--p-text-muted-color);
    font-size: 0.875rem;
    max-width: 320px;
}
.error-state .state-icon {
    font-size: 2rem;
}
.retry-btn {
    background: none;
    border: 1px solid var(--p-surface-border);
    padding: 0.5rem 1rem;
    border-radius: 6px;
    cursor: pointer;
    font-size: 0.875rem;
    color: var(--p-text-color);
    transition: background 0.15s;
}
.retry-btn:hover {
    background: var(--p-surface-hover);
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
