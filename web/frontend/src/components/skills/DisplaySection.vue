<script setup lang="ts">
interface DisplayConfig {
  icon: string
  customModelData: number
  color: string
  style: string
}

const props = defineProps<{
  modelValue: DisplayConfig
}>()

const emit = defineEmits<{
  'update:modelValue': [value: DisplayConfig]
}>()

const COLOR_OPTIONS = ['WHITE', 'RED', 'GREEN', 'BLUE', 'YELLOW', 'PINK', 'PURPLE'] as const

const STYLE_OPTIONS = ['SOLID', 'SEGMENTED_6', 'SEGMENTED_10', 'SEGMENTED_12', 'SEGMENTED_20'] as const

function setField<K extends keyof DisplayConfig>(key: K, val: DisplayConfig[K]) {
  emit('update:modelValue', { ...props.modelValue, [key]: val })
}
</script>

<template>
  <div class="display-section">
    <div class="field-row">
      <label class="field-label">Icon</label>
      <input
        class="field-input"
        type="text"
        placeholder="minecraft:iron_pickaxe"
        :value="modelValue.icon"
        @input="setField('icon', ($event.target as HTMLInputElement).value)"
      />
    </div>

    <div class="field-row">
      <label class="field-label">Custom Model Data</label>
      <input
        class="field-input"
        type="number"
        step="1"
        :value="modelValue.customModelData"
        @input="setField('customModelData', Number(($event.target as HTMLInputElement).value))"
      />
    </div>

    <div class="field-row">
      <label class="field-label">Color</label>
      <select
        class="field-select"
        :value="modelValue.color"
        @change="setField('color', ($event.target as HTMLSelectElement).value)"
      >
        <option
          v-for="c in COLOR_OPTIONS"
          :key="c"
          :value="c"
        >
          {{ c }}
        </option>
      </select>
    </div>

    <div class="field-row">
      <label class="field-label">Style</label>
      <select
        class="field-select"
        :value="modelValue.style"
        @change="setField('style', ($event.target as HTMLSelectElement).value)"
      >
        <option
          v-for="s in STYLE_OPTIONS"
          :key="s"
          :value="s"
        >
          {{ s }}
        </option>
      </select>
    </div>
  </div>
</template>

<style scoped>
.display-section {
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
  min-width: 8rem;
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
</style>
