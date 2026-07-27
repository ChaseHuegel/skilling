<template>
    <div class="dashboard">
        <PendingChangesBanner />
        <div class="dashboard-header">
            <h1>Skills</h1>
            <button class="create-btn" @click="createSkill">+ New Skill</button>
        </div>

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
    margin-bottom: 1.5rem;
}
.dashboard-header h1 {
    margin: 0;
    font-size: 1.5rem;
}
.create-btn {
    background: var(--p-primary-color);
    color: white;
    border: none;
    padding: 0.5rem 1rem;
    border-radius: 4px;
    cursor: pointer;
    font-size: 0.875rem;
}
    .skill-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
        gap: 1rem;
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
