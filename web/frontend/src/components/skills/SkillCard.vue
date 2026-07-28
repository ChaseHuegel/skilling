<template>
    <div
        class="skill-card"
        :style="{ '--skill-color': skill.color?.toLowerCase() || '#fff' }"
        @click="open"
    >
        <div class="card-header">
            <MinecraftIcon :material="skill.icon" :color="skill.color" :size="48" />
            <div class="card-badges">
                <span v-if="skill.xpSourceCount && skill.xpSourceCount > 0" class="badge badge-xp">
                    {{ skill.xpSourceCount }} XP source{{ skill.xpSourceCount !== 1 ? 's' : '' }}
                </span>
                <span v-if="skill.abilityCount > 0" class="badge badge-ability">
                    {{ skill.abilityCount }} abilit{{ skill.abilityCount !== 1 ? 'ies' : 'y' }}
                </span>
            </div>
            <div class="card-actions">
                <button class="btn-icon-sm" title="Duplicate" @click.stop="emit('duplicate', skill.id)">
                    <svg viewBox="0 0 16 16" width="14" height="14" fill="none">
                        <rect x="3" y="5" width="9" height="10" rx="1" stroke="currentColor" stroke-width="1.2" />
                        <path d="M5 5V3a1 1 0 011-1h6a1 1 0 011 1v7a1 1 0 01-1 1h-1" stroke="currentColor" stroke-width="1.2" />
                    </svg>
                </button>
                <button class="btn-icon-sm btn-icon-danger" title="Delete" @click.stop="emit('delete', skill.id)">
                    <svg viewBox="0 0 16 16" width="14" height="14" fill="none">
                        <path d="M3 4h10M6 4V3a1 1 0 011-1h2a1 1 0 011 1v1M5 4v9a1 1 0 001 1h4a1 1 0 001-1V4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                </button>
            </div>
        </div>
        <div class="card-body">
            <div class="skill-name">{{ skill.displayName || skill.id }}</div>
            <div class="skill-meta">Level 1 – {{ skill.maxLevel }}</div>
            <div class="skill-id">{{ skill.id }}</div>
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

const emit = defineEmits<{
    duplicate: [skillId: string]
    delete: [skillId: string]
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
    padding: 0 6px;
    border-radius: 4px;
    font-size: 0.7rem;
    font-weight: 600;
    line-height: 1.6;
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

.card-actions {
    display: flex;
    gap: 0.15rem;
    position: absolute;
    top: 0.35rem;
    right: 0.5rem;
}
.btn-icon-sm {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border: none;
    border-radius: 4px;
    background: transparent;
    color: var(--p-form-field-placeholder-color, #888);
    cursor: pointer;
    transition: background 0.15s, color 0.15s;
}
.btn-icon-sm:hover {
    background: var(--p-content-hover-background, #eee);
    color: var(--p-text-color, #000);
}
.btn-icon-danger:hover {
    color: var(--p-red-500, #ef4444);
}
.card-body {
    padding: 0.5rem 1rem 1rem;
}
.skill-id {
    margin-top: 0.35rem;
    font-size: 0.7rem;
    color: var(--p-form-field-placeholder-color, #888);
    font-family: monospace;
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
