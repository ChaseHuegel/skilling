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

        <div v-if="loading" class="loading">Loading skills...</div>
        <div v-else-if="error" class="error">{{ error }}</div>
        <div v-else class="skill-grid">
            <SkillCard v-for="s in skills" :key="s.id" :skill="s" />
            <div v-if="skills.length === 0" class="empty">No skills loaded. Create one to get started.</div>
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

onMounted(async () => {
    try {
        skills.value = await api.skills.list();
        staging.fetchStatus();
    } catch (e: any) {
        error.value = e.message;
    } finally {
        loading.value = false;
    }
});

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
.loading, .error, .empty {
    text-align: center;
    padding: 2rem;
    color: var(--p-text-muted-color);
}
.error {
    color: var(--p-red-600);
}
</style>
