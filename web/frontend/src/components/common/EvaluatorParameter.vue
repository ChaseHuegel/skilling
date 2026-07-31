<script setup lang="ts">
import { computed } from 'vue'
import DecimalInput from './DecimalInput.vue'

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

function updateMilestone(index: number, key: 'level' | 'value', val: number) {
  const copy = [...milestones.value]
  copy[index] = { ...copy[index], [key]: val }
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
          <DecimalInput
            :model-value="modelValue.params?.value"
            @update:model-value="setParam('value', $event)"
          />
        </div>
      </template>

      <template v-else-if="modelValue.type === 'linear'">
        <div class="field-row">
          <label class="field-label">Base</label>
          <DecimalInput
            :model-value="modelValue.params?.base"
            @update:model-value="setParam('base', $event)"
          />
        </div>
        <div class="field-row">
          <label class="field-label">Step</label>
          <DecimalInput
            :model-value="modelValue.params?.step"
            @update:model-value="setParam('step', $event)"
          />
        </div>
        <div class="field-row">
          <label class="field-label">Min</label>
          <DecimalInput
            :model-value="modelValue.params?.min"
            @update:model-value="setParam('min', $event)"
          />
        </div>
        <div class="field-row">
          <label class="field-label">Max</label>
          <DecimalInput
            :model-value="modelValue.params?.max"
            @update:model-value="setParam('max', $event)"
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
            <DecimalInput
              :model-value="entry.level"
              placeholder="Level"
              @update:model-value="updateMilestone(idx, 'level', $event)"
            />
            <DecimalInput
              :model-value="entry.value"
              placeholder="Value"
              @update:model-value="updateMilestone(idx, 'value', $event)"
            />
            <button
              class="btn btn-ghost btn-sm"
              style="color: var(--p-red-500, #ef4444)"
              @click="removeMilestone(idx)"
            >
              &times;
            </button>
          </div>
        </div>
        <button
          class="btn btn-primary btn-sm"
          @click="addMilestone"
        >
          + Add Milestone
        </button>
      </template>

      <template v-else-if="modelValue.type === 'polynomial'">
        <div class="field-row">
          <label class="field-label">Base XP</label>
          <DecimalInput
            :model-value="modelValue.params?.base_xp"
            @update:model-value="setParam('base_xp', $event)"
          />
        </div>
        <div class="field-row">
          <label class="field-label">Exponent</label>
          <DecimalInput
            :model-value="modelValue.params?.exponent"
            @update:model-value="setParam('exponent', $event)"
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

.milestones-list {
  margin-bottom: 0.5rem;
}

.milestone-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.3rem;
}
</style>
