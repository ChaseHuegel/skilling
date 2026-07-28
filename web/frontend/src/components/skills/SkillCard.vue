<template>
    <div
        class="skill-card"
        :style="{ '--skill-color': skill.color?.toLowerCase() || '#fff' }"
        @click="open"
    >
        <div class="card-header">
            <MinecraftIcon :material="skill.icon" :color="skill.color" :size="48" />
            <span v-if="skill.abilityCount > 0" class="ability-badge">
                {{ skill.abilityCount }}
            </span>
        </div>
        <div class="card-body">
            <div class="skill-name">{{ skill.displayName || skill.id }}</div>
            <div class="skill-meta">Level 1 – {{ skill.maxLevel }}</div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import MinecraftIcon from '../common/MinecraftIcon.vue';

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
    flex-direction: column;
    border: 1px solid var(--p-surface-border, #ddd);
    border-top: 3px solid var(--skill-color, #fff);
    border-radius: 8px;
    cursor: pointer;
    background: var(--p-surface-section, #fff);
    transition: all 0.2s ease;
    overflow: hidden;
}

.skill-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 1rem 1rem 0.5rem;
    position: relative;
}

.ability-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 20px;
    height: 20px;
    padding: 0 6px;
    border-radius: 10px;
    background: color-mix(in srgb, var(--skill-color, #fff) 20%, transparent);
    color: var(--skill-color, #fff);
    font-size: 0.7rem;
    font-weight: 600;
    border: 1px solid color-mix(in srgb, var(--skill-color, #fff) 40%, transparent);
}

.card-body {
    padding: 0.5rem 1rem 1rem;
}

.skill-name {
    font-weight: 600;
    font-size: 1rem;
    margin-bottom: 0.15rem;
}

.skill-meta {
    font-size: 0.8rem;
    color: var(--p-text-muted-color, #888);
}
</style>
