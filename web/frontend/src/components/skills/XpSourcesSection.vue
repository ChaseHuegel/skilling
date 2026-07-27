<script setup lang="ts">
import SectionToolbar from '../common/SectionToolbar.vue'
import FilterBuilder from '../common/FilterBuilder.vue'
import EvaluatorParameter from '../common/EvaluatorParameter.vue'

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
    >
      <div class="source-header">
        <span class="source-title">Source #{{ idx + 1 }}</span>
        <button
          class="btn-remove"
          @click="removeSource(idx)"
        >
          &times;
        </button>
      </div>

      <div class="source-body">
        <div class="field-row">
          <label class="field-label">Trigger</label>
          <select
            class="field-select"
            :value="source.trigger"
            @change="updateSource(idx, { trigger: ($event.target as HTMLSelectElement).value })"
          >
            <option
              v-for="t in TRIGGER_OPTIONS"
              :key="t"
              :value="t"
            >
              {{ t }}
            </option>
          </select>
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
</template>

<style scoped>
.xp-sources-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.xp-source-card {
  border: 1px solid #333;
  border-radius: 6px;
  background: #1e1e1e;
  overflow: hidden;
}

.source-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0.75rem;
  background: #252525;
  border-bottom: 1px solid #333;
}

.source-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: #ccc;
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
  color: #e0e0e0;
}

.field-select {
  flex: 1;
  padding: 0.4rem 0.5rem;
  border: 1px solid #444;
  border-radius: 4px;
  background: #2a2a2a;
  color: #e0e0e0;
  font-size: 0.85rem;
}

.sub-section {
  margin-top: 0.25rem;
}

.sub-label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: #bbb;
  margin-bottom: 0.35rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.btn-remove {
  background: none;
  border: none;
  color: #f87171;
  font-size: 1.3rem;
  cursor: pointer;
  padding: 0 0.25rem;
  line-height: 1;
}

.btn-remove:hover {
  color: #ef4444;
}
</style>
