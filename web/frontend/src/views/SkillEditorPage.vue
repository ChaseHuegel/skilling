<template>
    <div class="editor-page">
        <h1>{{ isNew ? 'Create Skill' : `Edit: ${skill?.displayName || skillId}` }}</h1>
        <p class="placeholder">Skill editor will be implemented in Phase 5.</p>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { api } from '../api/client';

const route = useRoute();
const skillId = route.params.id as string;
const isNew = route.name === 'SkillNew';
const skill = ref<any>(null);

onMounted(async () => {
    if (!isNew && skillId) {
        try {
            skill.value = await api.skills.get(skillId);
        } catch { /* ignore */ }
    }
});
</script>

<style scoped>
.editor-page {
    max-width: 800px;
    margin: 0 auto;
    padding: 1.5rem;
}
.placeholder {
    color: var(--p-text-muted-color);
    padding: 2rem;
    text-align: center;
}
</style>
