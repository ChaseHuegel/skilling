<script setup lang="ts">
import DecimalInput from '../common/DecimalInput.vue'

interface ProgressionConfig {
  curve: string
  baseXp?: number
  exponent?: number
}

const props = defineProps<{
  modelValue: ProgressionConfig
}>()

const emit = defineEmits<{
  'update:modelValue': [value: ProgressionConfig]
}>()

const CURVE_OPTIONS = ['polynomial', 'linear', 'constant'] as const

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

    <!-- Every curve is derived from base_xp; only polynomial adds exponent. -->
    <div class="field-row">
      <label class="field-label">Base XP</label>
      <DecimalInput
        :model-value="modelValue.baseXp"
        @update:model-value="setParam('baseXp', $event)"
      />
    </div>

    <template v-if="modelValue.curve === 'polynomial'">
      <div class="field-row">
        <label class="field-label">Exponent</label>
        <DecimalInput
          :model-value="modelValue.exponent"
          @update:model-value="setParam('exponent', $event)"
        />
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
  color: var(--p-text-color);
}

.field-select {
  flex: 1;
  padding: 0.45rem 0.6rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-form-field-background);
  color: var(--p-text-color);
  font-size: 0.875rem;
}

.unsupported-notice {
  padding: 0.75rem 1rem;
  border: 1px solid color-mix(in srgb, var(--p-warning-color, #eab308), black 50%);
  border-radius: 4px;
  background: color-mix(in srgb, var(--p-warning-color, #eab308), black 80%);
  color: var(--p-warning-color, #eab308);
  font-size: 0.85rem;
}
</style>
