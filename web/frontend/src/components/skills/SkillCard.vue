<template>
    <div class="skill-card" :style="{ borderLeftColor: skill.color?.toLowerCase() || '#fff' }" @click="open">
        <div class="skill-icon">{{ skill.icon?.replace('minecraft:', '') || '?' }}</div>
        <div class="skill-info">
            <div class="skill-name">{{ skill.displayName || skill.id }}</div>
            <div class="skill-meta">Max Level: {{ skill.maxLevel }} · {{ skill.abilityCount }} abilities</div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const props = defineProps<{
    skill: {
        id: string;
        displayName: string;
        icon: string;
        color: string;
        maxLevel: number;
        abilityCount: number;
    };
}>();

const router = useRouter();
function open() {
    router.push(`/skills/${props.skill.id}`);
}
</script>

<style scoped>
.skill-card {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1rem;
    border: 1px solid var(--p-surface-border);
    border-left: 4px solid;
    border-radius: 6px;
    cursor: pointer;
    background: var(--p-surface-section);
    transition: box-shadow 0.15s;
}
.skill-card:hover {
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.skill-icon {
    font-size: 1.5rem;
    width: 2.5rem;
    height: 2.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--p-surface-ground);
    border-radius: 4px;
}
.skill-name {
    font-weight: 600;
    font-size: 1rem;
}
.skill-meta {
    font-size: 0.8rem;
    color: var(--p-text-muted-color);
}
</style>
