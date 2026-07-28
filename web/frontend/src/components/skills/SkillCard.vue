<template>
    <div
        class="skill-card"
        :style="{ '--skill-color': skill.color?.toLowerCase() || '#fff' }"
        @click="open"
    >
        <div class="card-header">
            <MinecraftIcon :material="skill.icon" :color="skill.color" :size="48" />
            <div class="card-badges">
                <span v-if="skill.xpSourceCount && skill.xpSourceCount > 0" class="badge badge-xp" title="XP Sources">
                    {{ skill.xpSourceCount }} src
                </span>
                <span v-if="skill.abilityCount > 0" class="badge badge-ability" :title="skill.abilityCount + ' abilit' + (skill.abilityCount !== 1 ? 'ies' : 'y')">
                    {{ skill.abilityCount }} abil
                </span>
            </div>
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
        xpSourceCount?: number;
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
    border: 1px solid var(--p-content-border-color);
    border-top: 3px solid var(--skill-color);
    border-radius: 8px;
    cursor: pointer;
    background: var(--p-content-background);
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

.card-badges {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 0.25rem;
}

.badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0 6px;
    border-radius: 4px;
    font-size: 0.65rem;
    font-weight: 600;
    line-height: 1.5;
    white-space: nowrap;
}

.badge-ability {
    background: color-mix(in srgb, var(--skill-color, #fff) 20%, transparent);
    color: var(--skill-color, #fff);
    border: 1px solid color-mix(in srgb, var(--skill-color, #fff) 40%, transparent);
}

.badge-xp {
    background: var(--p-content-border-color);
    color: var(--p-form-field-placeholder-color);
    border: 1px solid var(--p-content-border-color);
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
