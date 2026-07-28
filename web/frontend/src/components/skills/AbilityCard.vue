<template>
    <div class="ability-card" @click="openSkill">
        <div class="ability-header">
            <span class="ability-name">{{ ability.displayName || ability.id }}</span>
            <span class="ability-type-badge" :class="isActive ? 'badge-active' : 'badge-passive'">
                {{ isActive ? 'Active' : 'Passive' }}
            </span>
        </div>
        <div class="ability-body">
            <div class="ability-detail">
                <span class="detail-label">Skill</span>
                <span class="detail-value">{{ skillDisplayName }}</span>
            </div>
            <div class="ability-detail">
                <span class="detail-label">Unlock</span>
                <span class="detail-value">Level {{ ability.unlockLevel }}</span>
            </div>
            <div class="ability-detail">
                <span class="detail-label">ID</span>
                <span class="detail-value" style="font-family: monospace; font-size: 0.75rem;">{{ ability.id }}</span>
            </div>
            <div v-if="ability.requirements?.cooldown" class="ability-detail">
                <span class="detail-label">Cooldown</span>
                <span class="detail-value">{{ ability.requirements.cooldown }}s</span>
            </div>
            <div v-if="mechanicList.length" class="ability-detail">
                <span class="detail-label">Mechanics</span>
                <span class="detail-value mechanic-list" :title="mechanicList.join('\n')">{{ mechanicList.join(', ') }}</span>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';

const props = defineProps<{
    ability: {
        id: string;
        displayName?: string;
        unlockLevel: number;
        requirements?: { cooldown?: number; state?: string[]; items?: any[] };
        mechanics?: { type: string }[];
    };
    skillId: string;
    skillDisplayName: string;
}>();

const isActive = computed(() => {
    const r = props.ability.requirements;
    return (r?.cooldown ?? 0) > 0 || (r?.state?.length ?? 0) > 0 || (r?.items?.length ?? 0) > 0;
});

const mechanicList = computed(() => (props.ability.mechanics || []).map(m => m.type));

const router = useRouter();

function openSkill() {
    router.push(`/skills/${props.skillId}#ability-${props.ability.id}`);
}
</script>

<style scoped>
.ability-card {
    display: flex;
    flex-direction: column;
    border: 1px solid var(--p-content-border-color, #ddd);
    border-radius: 8px;
    background: var(--p-content-background, #fff);
    cursor: pointer;
    transition: all 0.2s ease;
    overflow: hidden;
}
.ability-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}
.ability-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.75rem 1rem;
    background: var(--p-form-field-background, #f8f8f8);
    border-bottom: 1px solid var(--p-content-border-color, #ddd);
}
.ability-name {
    font-weight: 600;
    font-size: 0.95rem;
}
.ability-type-badge {
    font-size: 0.7rem;
    font-weight: 600;
    padding: 0.15rem 0.5rem;
    border-radius: 4px;
    white-space: nowrap;
}
.badge-active {
    background: color-mix(in srgb, var(--p-primary-color, #3b82f6) 20%, transparent);
    color: var(--p-primary-color, #3b82f6);
    border: 1px solid color-mix(in srgb, var(--p-primary-color, #3b82f6) 40%, transparent);
}
.badge-passive {
    background: var(--p-content-border-color);
    color: var(--p-form-field-placeholder-color);
    border: 1px solid var(--p-content-border-color);
}
.ability-body {
    padding: 0.75rem 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
}
.ability-detail {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.8rem;
}
.detail-label {
    color: var(--p-form-field-placeholder-color, #888);
    min-width: 4rem;
}
.detail-value {
    color: var(--p-text-color, #000);
    font-weight: 500;
}
.mechanic-list {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    cursor: help;
}
</style>
