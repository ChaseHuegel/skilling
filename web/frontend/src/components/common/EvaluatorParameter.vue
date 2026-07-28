<script setup lang="ts">
import { computed } from 'vue'

interface EvaluatorValue {
  type: string
  params: Record<string, any>
}

const props = defineProps<{
  modelValue: EvaluatorValue
  label: string
  name: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: EvaluatorValue]
}>()

const EVALUATOR_TYPES = ['constant', 'linear', 'milestones', 'polynomial'] as const

const milestones = computed({
  get: () => {
    const entries = props.modelValue.params?.milestones
    if (Array.isArray(entries)) return entries as { level: number; value: number }[]
    return []
  },
  set: (val) => {
    emit('update:modelValue', {
      ...props.modelValue,
      params: {
        ...props.modelValue.params,
        milestones: val,
      },
    })
  },
})

function setType(type: string) {
  emit('update:modelValue', {
    type,
    params: { ...props.modelValue.params },
  })
}

function setParam(key: string, value: any) {
  emit('update:modelValue', {
    ...props.modelValue,
    params: {
      ...props.modelValue.params,
      [key]: value,
    },
  })
}

function addMilestone() {
  milestones.value = [...milestones.value, { level: 0, value: 0 }]
}

function removeMilestone(index: number) {
  const copy = [...milestones.value]
  copy.splice(index, 1)
  milestones.value = copy
}

function updateMilestone(index: number, key: 'level' | 'value', val: string) {
  const copy = [...milestones.value]
  copy[index] = { ...copy[index], [key]: Number(val) }
  milestones.value = copy
}
</script>

<template>
  <div class="evaluator-parameter">
    <label class="evaluator-label">{{ name }}</label>

    <select
      class="evaluator-type-select"
      :value="modelValue.type"
      @change="setType(($event.target as HTMLSelectElement).value)"
    >
      <option
        v-for="t in EVALUATOR_TYPES"
        :key="t"
        :value="t"
      >
        {{ t }}
      </option>
    </select>

    <div class="evaluator-fields">
      <template v-if="modelValue.type === 'constant'">
        <div class="field-row">
          <label class="field-label">Value</label>
          <input
            class="field-input"
            type="number"
            step="any"
            :value="modelValue.params?.value ?? ''"
            @input="setParam('value', Number(($event.target as HTMLInputElement).value))"
          />
        </div>
      </template>

      <template v-else-if="modelValue.type === 'linear'">
        <div class="field-row">
          <label class="field-label">Base</label>
          <input
            class="field-input"
            type="number"
            step="any"
            :value="modelValue.params?.base ?? ''"
            @input="setParam('base', Number(($event.target as HTMLInputElement).value))"
          />
        </div>
        <div class="field-row">
          <label class="field-label">Step</label>
          <input
            class="field-input"
            type="number"
            step="any"
            :value="modelValue.params?.step ?? ''"
            @input="setParam('step', Number(($event.target as HTMLInputElement).value))"
          />
        </div>
        <div class="field-row">
          <label class="field-label">Min</label>
          <input
            class="field-input"
            type="number"
            step="any"
            :value="modelValue.params?.min ?? ''"
            @input="setParam('min', Number(($event.target as HTMLInputElement).value))"
          />
        </div>
        <div class="field-row">
          <label class="field-label">Max</label>
          <input
            class="field-input"
            type="number"
            step="any"
            :value="modelValue.params?.max ?? ''"
            @input="setParam('max', Number(($event.target as HTMLInputElement).value))"
          />
        </div>
      </template>

      <template v-else-if="modelValue.type === 'milestones'">
        <div class="milestones-list">
          <div
            v-for="(entry, idx) in milestones"
            :key="idx"
            class="milestone-row"
          >
            <input
              class="field-input milestone-input"
              type="number"
              step="any"
              placeholder="Level"
              :value="entry.level"
              @input="updateMilestone(idx, 'level', ($event.target as HTMLInputElement).value)"
            />
            <input
              class="field-input milestone-input"
              type="number"
              step="any"
              placeholder="Value"
              :value="entry.value"
              @input="updateMilestone(idx, 'value', ($event.target as HTMLInputElement).value)"
            />
            <button
              class="btn-remove"
              @click="removeMilestone(idx)"
            >
              &times;
            </button>
          </div>
        </div>
        <button
          class="btn-add"
          @click="addMilestone"
        >
          + Add Milestone
        </button>
      </template>

      <template v-else-if="modelValue.type === 'polynomial'">
        <div class="field-row">
          <label class="field-label">Base XP</label>
          <input
            class="field-input"
            type="number"
            step="any"
            :value="modelValue.params?.base_xp ?? ''"
            @input="setParam('base_xp', Number(($event.target as HTMLInputElement).value))"
          />
        </div>
        <div class="field-row">
          <label class="field-label">Exponent</label>
          <input
            class="field-input"
            type="number"
            step="any"
            :value="modelValue.params?.exponent ?? ''"
            @input="setParam('exponent', Number(($event.target as HTMLInputElement).value))"
          />
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.evaluator-parameter {
  margin-bottom: 1rem;
}

.evaluator-label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.25rem;
  font-size: 0.875rem;
  color: var(--p-text-color);
}

.evaluator-type-select {
  width: 100%;
  padding: 0.4rem 0.5rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-form-field-background);
  color: var(--p-text-color);
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}

.evaluator-fields {
  padding-left: 0.5rem;
}

.field-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.35rem;
}

.field-label {
  min-width: 5rem;
  font-size: 0.8rem;
  color: var(--p-form-field-placeholder-color);
}

.field-input {
  flex: 1;
  padding: 0.35rem 0.5rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-form-field-background);
  color: var(--p-text-color);
  font-size: 0.85rem;
}

.milestones-list {
  margin-bottom: 0.5rem;
}

.milestone-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.3rem;
}

.milestone-input {
  flex: 1;
}

.btn-remove {
  background: none;
  border: none;
  color: var(--p-red-500, #f87171);
  font-size: 1.2rem;
  cursor: pointer;
  padding: 0 0.25rem;
  line-height: 1;
}

.btn-remove:hover {
  color: var(--p-red-600, #ef4444);
}

.btn-add {
  background: var(--p-content-background);
  color: var(--p-text-color);
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  padding: 0.35rem 0.75rem;
  font-size: 0.8rem;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-add:hover {
  background: var(--p-content-hover-background);
}
</style>
