<script setup lang="ts">
import { computed, ref, watch, type Ref } from 'vue'
import SectionToolbar from '../common/SectionToolbar.vue'
import FilterBuilder from '../common/FilterBuilder.vue'
import EvaluatorParameter from '../common/EvaluatorParameter.vue'
import AppCombobox from '../common/AppCombobox.vue'
import { useDragReorder } from '../../composables/useDragReorder'

interface FilterEntry {
  target?: string
  state?: string
  tool?: string
}

interface XpSource {
  trigger: string
  filters: FilterEntry[]
  reward: { type: string; params: Record<string, any> }
}

const props = defineProps<{
  modelValue: XpSource[]
  tagSuggestions: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: XpSource[]]
}>()

const expanded = ref<Record<number, boolean>>({})

watch(() => props.modelValue.length, (len) => {
  for (let i = 0; i < len; i++) {
    if (expanded.value[i] === undefined) expanded.value[i] = true
  }
}, { immediate: true })

function toggleExpand(idx: number) {
  expanded.value[idx] = !expanded.value[idx]
}

const sources = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})
const { dragIndex, onDragStart, onDragOver, onDragEnd } = useDragReorder(sources)

const pendingRemoveSource = ref<number | null>(null)

function confirmRemoveSource(index: number) {
  pendingRemoveSource.value = index
}

function executeRemoveSource() {
  if (pendingRemoveSource.value === null) return
  removeSource(pendingRemoveSource.value)
  pendingRemoveSource.value = null
}

const TRIGGER_OPTIONS = [
  'block_break',
  'block_place',
  'entity_damage',
  'entity_damage_taken',
  'entity_kill',
  'craft_item',
  'furnace_extract',
  'brew_potion',
  'player_interact',
  'consume_item',
  'fishing',
  'crop_grow',
  'breed_animals',
] as const

function updateSource(index: number, patch: Partial<XpSource>) {
  const copy = [...props.modelValue]
  copy[index] = { ...copy[index], ...patch }
  emit('update:modelValue', copy)
}

function updateReward(index: number, reward: XpSource['reward']) {
  updateSource(index, { reward })
}

function removeSource(index: number) {
  const copy = [...props.modelValue]
  copy.splice(index, 1)
  emit('update:modelValue', copy)
}

function addSource() {
  const idx = props.modelValue.length
  expanded.value[idx] = true
  emit('update:modelValue', [
    ...props.modelValue,
    {
      trigger: 'block_break',
      filters: [],
      reward: { type: 'constant', params: { value: 0 } },
    },
  ])
}

function duplicateSource() {
  if (props.modelValue.length === 0) {
    addSource()
    return
  }
  const last = props.modelValue[props.modelValue.length - 1]
  emit('update:modelValue', [
    ...props.modelValue,
    { ...last, filters: [...last.filters], reward: { ...last.reward, params: { ...last.reward.params } } },
  ])
}
</script>

<template>
  <div class="xp-sources-section">
    <SectionToolbar
      section-name="XP Source"
      :can-delete="false"
      :can-duplicate="modelValue.length > 0"
      @add="addSource"
      @duplicate="duplicateSource"
    />

    <div
      v-for="(source, idx) in modelValue"
      :key="idx"
      class="xp-source-card"
      :class="{ 'drag-over': dragIndex !== null && dragIndex !== idx }"
      draggable="true"
      @dragstart="onDragStart(idx)"
      @dragover="onDragOver($event, idx)"
      @dragend="onDragEnd"
    >
      <div
        class="source-header"
        @click="toggleExpand(idx)"
      >
        <span class="drag-handle" title="Drag to reorder" @click.stop>&#8801;</span>
        <span class="source-title">Source #{{ idx + 1 }}</span>
        <span class="expand-toggle">{{ expanded[idx] ? '▼' : '▶' }}</span>
        <button
          class="btn btn-ghost btn-sm"
          style="color: var(--p-red-500, #ef4444)"
          @click.stop="confirmRemoveSource(idx)"
        >
          &times;
        </button>
      </div>

      <div
        v-if="expanded[idx]"
        class="source-body"
      >
        <div class="field-row">
          <label class="field-label">Trigger</label>
          <AppCombobox
            :model-value="source.trigger"
            :suggestions="TRIGGER_OPTIONS as unknown as string[]"
            placeholder="Select or type trigger"
            :name="'trigger-' + idx"
            @update:model-value="updateSource(idx, { trigger: $event })"
          />
        </div>

        <div class="sub-section">
          <label class="sub-label">Filters</label>
          <FilterBuilder
            :model-value="source.filters"
            :tag-suggestions="tagSuggestions"
            @update:model-value="updateSource(idx, { filters: $event })"
          />
        </div>

        <div class="sub-section">
          <EvaluatorParameter
            :model-value="source.reward"
            label="Reward"
            name="reward"
            @update:model-value="updateReward(idx, $event)"
          />
        </div>
      </div>
    </div>
  </div>

  <div v-if="pendingRemoveSource !== null" class="modal-overlay" @click.self="pendingRemoveSource = null">
    <div class="modal">
      <h3>Delete XP source?</h3>
      <p>This will permanently remove this XP source.</p>
      <div class="modal-actions">
        <button class="btn btn-secondary" @click="pendingRemoveSource = null">Cancel</button>
        <button class="btn btn-danger" @click="executeRemoveSource">Delete</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.xp-sources-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.xp-source-card {
  border: 1px solid var(--p-content-border-color);
  border-radius: 6px;
  background: var(--p-content-background);
  overflow: hidden;
}

.xp-source-card[draggable="true"] {
  cursor: default;
}
.xp-source-card.drag-over {
  opacity: 0.5;
}
.drag-handle {
  cursor: grab;
  color: var(--p-form-field-placeholder-color);
  font-size: 1.1rem;
  line-height: 1;
  user-select: none;
  margin-right: 0.25rem;
}
.drag-handle:active {
  cursor: grabbing;
}
.source-header {
  display: flex;
  align-items: center;
  padding: 0.5rem 0.75rem;
  background: var(--p-form-field-background);
  border-bottom: 1px solid var(--p-content-border-color);
  cursor: pointer;
  user-select: none;
}

.source-title {
  flex: 1;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--p-form-field-placeholder-color);
}

.source-body {
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.field-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.field-label {
  min-width: 5rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--p-text-color);
}

.field-select {
  flex: 1;
  padding: 0.4rem 0.5rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-form-field-background);
  color: var(--p-text-color);
  font-size: 0.85rem;
}

.sub-section {
  margin-top: 0.25rem;
}

.sub-label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--p-form-field-placeholder-color);
  margin-bottom: 0.35rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 8px;
  padding: 1.5rem;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}
.modal h3 {
  margin: 0 0 0.5rem;
  font-size: 1.05rem;
  color: var(--p-text-color);
}
.modal p {
  margin: 0 0 1.25rem;
  color: var(--p-text-muted-color, #888);
  font-size: 0.875rem;
  line-height: 1.4;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
