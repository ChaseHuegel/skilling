<script setup lang="ts">
interface ProgressionConfig {
  curve: string
  baseXp?: number
  exponent?: number
  base?: number
  step?: number
  min?: number
  max?: number
  value?: number
}

const props = defineProps<{
  modelValue: ProgressionConfig
}>()

const emit = defineEmits<{
  'update:modelValue': [value: ProgressionConfig]
}>()

const CURVE_OPTIONS = ['polynomial', 'linear', 'constant', 'milestone'] as const

function setCurve(curve: string) {
  emit('update:modelValue', { ...props.modelValue, curve })
}

function setParam(key: string, val: number) {
  emit('update:modelValue', { ...props.modelValue, [key]: val })
}
</script>

<template>
  <div class="progression-section">
    <div class="field-row">
      <label class="field-label">Curve</label>
      <select
        class="field-select"
        :value="modelValue.curve"
        @change="setCurve(($event.target as HTMLSelectElement).value)"
      >
        <option
          v-for="c in CURVE_OPTIONS"
          :key="c"
          :value="c"
        >
          {{ c }}
        </option>
      </select>
    </div>

    <template v-if="modelValue.curve === 'polynomial'">
      <div class="field-row">
        <label class="field-label">Base XP</label>
        <input
          class="field-input"
          type="number"
          step="any"
          :value="modelValue.baseXp ?? ''"
          @input="setParam('baseXp', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
      <div class="field-row">
        <label class="field-label">Exponent</label>
        <input
          class="field-input"
          type="number"
          step="any"
          :value="modelValue.exponent ?? ''"
          @input="setParam('exponent', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
    </template>

    <template v-else-if="modelValue.curve === 'linear'">
      <div class="field-row">
        <label class="field-label">Base</label>
        <input
          class="field-input"
          type="number"
          step="any"
          :value="modelValue.base ?? ''"
          @input="setParam('base', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
      <div class="field-row">
        <label class="field-label">Step</label>
        <input
          class="field-input"
          type="number"
          step="any"
          :value="modelValue.step ?? ''"
          @input="setParam('step', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
      <div class="field-row">
        <label class="field-label">Min</label>
        <input
          class="field-input"
          type="number"
          step="any"
          :value="modelValue.min ?? ''"
          @input="setParam('min', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
      <div class="field-row">
        <label class="field-label">Max</label>
        <input
          class="field-input"
          type="number"
          step="any"
          :value="modelValue.max ?? ''"
          @input="setParam('max', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
    </template>

    <template v-else-if="modelValue.curve === 'constant'">
      <div class="field-row">
        <label class="field-label">Value</label>
        <input
          class="field-input"
          type="number"
          step="any"
          :value="modelValue.value ?? ''"
          @input="setParam('value', Number(($event.target as HTMLInputElement).value))"
        />
      </div>
    </template>

    <template v-else-if="modelValue.curve === 'milestone'">
      <div class="unsupported-notice">
        Milestone progression is not supported; use polynomial or linear
      </div>
    </template>
  </div>
</template>

<style scoped>
.progression-section {
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
  min-width: 6rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: #e0e0e0;
}

.field-input {
  flex: 1;
  padding: 0.45rem 0.6rem;
  border: 1px solid #444;
  border-radius: 4px;
  background: #1e1e1e;
  color: #e0e0e0;
  font-size: 0.875rem;
}

.field-select {
  flex: 1;
  padding: 0.45rem 0.6rem;
  border: 1px solid #444;
  border-radius: 4px;
  background: #2a2a2a;
  color: #e0e0e0;
  font-size: 0.875rem;
}

.unsupported-notice {
  padding: 0.75rem 1rem;
  border: 1px solid #7a5a2a;
  border-radius: 4px;
  background: #3f2e1e;
  color: #e0b060;
  font-size: 0.85rem;
}
</style>
