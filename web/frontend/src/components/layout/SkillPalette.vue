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
    <div class="palette-hint" v-if="selectedSkillId">
      Click an empty slot to place <strong>{{ selectedSkillName }}</strong>
      <button class="hint-cancel" @click="clearSelection">&times;</button>
    </div>
    <div class="palette-grid" ref="listRef">
      <div
        v-for="skill in filteredSkills"
        :key="skill.id"
        class="palette-item"
        :class="{
          'palette-item-dragging': draggingId === skill.id,
          'palette-item-selected': selectedSkillId === skill.id,
        }"
        draggable="true"
        @dragstart="onDragStart(skill, $event)"
        @dragend="onDragEnd"
        @click="toggleSelect(skill)"
      >
        <MinecraftIcon :material="skill.icon || 'minecraft:barrier'" :color="skill.color" :size="28" />
        <span class="palette-item-name">{{ skill.displayName || skill.id }}</span>
      </div>
      <div v-if="filteredSkills.length === 0" class="palette-empty">
        No skills match "{{ query }}"
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject } from 'vue'
import MinecraftIcon from '../common/MinecraftIcon.vue'
import { byColorThenName } from '../../utils/skillSort'

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
const selectedSkillId = inject('selectedSkillId') as ReturnType<typeof ref<string | null>>

const filteredSkills = computed(() => {
  const sorted = [...props.skills].sort(byColorThenName)
  const q = query.value.toLowerCase().trim()
  if (!q) return sorted
  return sorted.filter(s =>
    s.id.toLowerCase().includes(q) ||
    (s.displayName || '').toLowerCase().includes(q) ||
    (s.abilities || []).some(a => a.name.toLowerCase().includes(q))
  )
})

const selectedSkillName = computed(() => {
  if (!selectedSkillId?.value) return ''
  const skill = props.skills.find(s => s.id === selectedSkillId.value)
  return skill?.displayName || skill?.id || ''
})

function toggleSelect(skill: PaletteSkill) {
  if (selectedSkillId?.value === skill.id) {
    selectedSkillId.value = null
  } else {
    selectedSkillId.value = skill.id
  }
}

function clearSelection() {
  if (selectedSkillId) selectedSkillId.value = null
}

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
  max-width: 700px;
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
  font-size: 0.85rem;
}

.search-input::placeholder {
  color: var(--p-form-field-placeholder-color, #666);
}

.palette-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: color-mix(in srgb, var(--p-primary-color, #3b82f6) 12%, transparent);
  border: 1px solid color-mix(in srgb, var(--p-primary-color, #3b82f6) 30%, transparent);
  border-radius: 6px;
  font-size: 0.75rem;
  color: var(--p-text-color, #ccc);
}

.palette-hint strong {
  color: var(--p-primary-color, #3b82f6);
}

.hint-cancel {
  margin-left: auto;
  background: none;
  border: none;
  color: var(--p-text-muted-color, #888);
  cursor: pointer;
  font-size: 1rem;
  line-height: 1;
  padding: 0 2px;
}

.hint-cancel:hover {
  color: var(--p-text-color, #fff);
}

.palette-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 4px;
  max-height: 300px;
  overflow-y: auto;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 6px;
  cursor: grab;
  transition: background 0.1s;
  user-select: none;
  border: 1px solid transparent;
}

.palette-item:hover {
  background: var(--p-content-hover-background, #2a2a4e);
}

.palette-item-dragging {
  opacity: 0.5;
}

.palette-item-selected {
  background: transparent;
  border-color: var(--p-primary-color, #3b82f6);
}

.palette-item-name {
  font-size: 0.8rem;
  color: var(--p-text-color, #ccc);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.palette-empty {
  grid-column: 1 / -1;
  padding: 16px 8px;
  text-align: center;
  font-size: 0.8rem;
  color: var(--p-form-field-placeholder-color, #666);
}
</style>
