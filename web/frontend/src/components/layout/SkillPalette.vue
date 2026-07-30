<template>
  <div class="skill-palette">
    <div class="palette-search">
      <svg class="search-icon" viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.5">
        <circle cx="6.5" cy="6.5" r="4.5" />
        <line x1="10" y1="10" x2="14" y2="14" />
      </svg>
      <input
        class="search-input"
        type="text"
        placeholder="Search skills..."
        v-model="query"
      />
    </div>
    <div class="palette-list" ref="listRef">
      <div
        v-for="skill in filteredSkills"
        :key="skill.id"
        class="palette-item"
        :class="{ 'palette-item-dragging': draggingId === skill.id }"
        draggable="true"
        @dragstart="onDragStart(skill, $event)"
        @dragend="onDragEnd"
        @mouseenter="showTooltip(skill, $event)"
        @mouseleave="hideTooltip"
      >
        <MinecraftIcon :material="skill.icon || 'minecraft:barrier'" :color="skill.color" :size="28" />
        <span class="palette-item-name">{{ skill.displayName || skill.id }}</span>
      </div>
      <div v-if="filteredSkills.length === 0" class="palette-empty">
        No skills match "{{ query }}"
      </div>
    </div>
    <SkillTooltip
      :skill="tooltipSkill"
      :visible="tooltipVisible"
      :x="tooltipX"
      :y="tooltipY"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import MinecraftIcon from '../common/MinecraftIcon.vue'
import SkillTooltip from './SkillTooltip.vue'

export interface PaletteSkill {
  id: string
  displayName: string
  icon: string
  color: string
  abilities?: { name: string; unlockLevel: number }[]
}

const props = defineProps<{
  skills: PaletteSkill[]
}>()

const query = ref('')
const draggingId = ref<string | null>(null)
const listRef = ref<HTMLElement | null>(null)

const filteredSkills = computed(() => {
  const q = query.value.toLowerCase().trim()
  if (!q) return props.skills
  return props.skills.filter(s =>
    s.id.toLowerCase().includes(q) ||
    (s.displayName || '').toLowerCase().includes(q) ||
    (s.abilities || []).some(a => a.name.toLowerCase().includes(q))
  )
})

function onDragStart(skill: PaletteSkill, e: DragEvent) {
  draggingId.value = skill.id
  e.dataTransfer?.setData('text/plain', skill.id)
  e.dataTransfer!.effectAllowed = 'move'
  if (e.target) {
    (e.target as HTMLElement).style.opacity = '0.5'
  }
}

function onDragEnd(e: DragEvent) {
  draggingId.value = null
  if (e.target) {
    (e.target as HTMLElement).style.opacity = ''
  }
}

// Tooltip
const tooltipVisible = ref(false)
const tooltipSkill = ref<PaletteSkill | null>(null)
const tooltipX = ref(0)
const tooltipY = ref(0)

function showTooltip(skill: PaletteSkill, e: MouseEvent) {
  tooltipSkill.value = skill
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  tooltipX.value = rect.left + rect.width / 2
  tooltipY.value = rect.top
  tooltipVisible.value = true
}

function hideTooltip() {
  tooltipVisible.value = false
  tooltipSkill.value = null
}
</script>

<style scoped>
.skill-palette {
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: var(--p-content-background, #1a1a2e);
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 8px;
  padding: 12px;
  width: 100%;
  max-width: 280px;
  box-sizing: border-box;
}

.palette-search {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--p-form-field-background, #111);
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 6px;
  padding: 6px 8px;
}

.search-icon {
  flex-shrink: 0;
  color: var(--p-form-field-placeholder-color, #666);
}

.search-input {
  flex: 1;
  background: none;
  border: none;
  outline: none;
  color: var(--p-text-color, #fff);
  font-size: 0.8rem;
}

.search-input::placeholder {
  color: var(--p-form-field-placeholder-color, #666);
}

.palette-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 400px;
  overflow-y: auto;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: grab;
  transition: background 0.1s;
  user-select: none;
}

.palette-item:hover {
  background: var(--p-content-hover-background, #2a2a4e);
}

.palette-item-dragging {
  opacity: 0.5;
}

.palette-item-name {
  font-size: 0.8rem;
  color: var(--p-text-color, #ccc);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.palette-empty {
  padding: 16px 8px;
  text-align: center;
  font-size: 0.8rem;
  color: var(--p-form-field-placeholder-color, #666);
}
</style>
