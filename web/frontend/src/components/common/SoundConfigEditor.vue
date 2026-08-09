<script setup lang="ts">
import AppCombobox from './AppCombobox.vue'
import { useRegistriesStore } from '../../stores/registries'

export interface SoundConfig {
  type: string
  volume: number
  pitch: number
  target: string
}

const props = defineProps<{
  modelValue: SoundConfig[]
  namePrefix: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: SoundConfig[]]
}>()

const registriesStore = useRegistriesStore()

function addSound() {
  emit('update:modelValue', [
    ...props.modelValue,
    { type: '', volume: 1, pitch: 1, target: 'self' },
  ])
}

function removeSound(index: number) {
  const copy = [...props.modelValue]
  copy.splice(index, 1)
  emit('update:modelValue', copy)
}

function updateSound(index: number, patch: Partial<SoundConfig>) {
  const copy = [...props.modelValue]
  copy[index] = { ...copy[index], ...patch }
  emit('update:modelValue', copy)
}
</script>

<template>
  <div class="sound-config-editor">
    <div
      v-for="(sound, sIdx) in modelValue"
      :key="sIdx"
      class="sound-card"
    >
      <div class="sound-type-row">
        <AppCombobox
          :model-value="sound.type"
          :suggestions="registriesStore.sounds"
          :placeholder="placeholder || 'minecraft:entity_experience_orb_pickup'"
          :name="namePrefix + '-' + sIdx"
          @update:model-value="updateSound(sIdx, { type: $event })"
        />
        <button
          class="btn btn-ghost btn-sm"
          style="color: var(--p-red-500, #ef4444); flex-shrink: 0"
          @click="removeSound(sIdx)"
        >
          &times;
        </button>
      </div>
      <div class="sound-fields">
        <div class="sound-field">
          <label class="field-label-sm">Volume</label>
          <input
            class="field-input-sm"
            type="number"
            step="any"
            :value="sound.volume"
            @input="updateSound(sIdx, { volume: Number(($event.target as HTMLInputElement).value) })"
          />
        </div>
        <div class="sound-field">
          <label class="field-label-sm">Pitch</label>
          <input
            class="field-input-sm"
            type="number"
            step="any"
            :value="sound.pitch"
            @input="updateSound(sIdx, { pitch: Number(($event.target as HTMLInputElement).value) })"
          />
        </div>
        <div class="sound-field">
          <label class="field-label-sm">Target</label>
          <select
            class="field-input-sm"
            :value="sound.target"
            @change="updateSound(sIdx, { target: ($event.target as HTMLSelectElement).value })"
          >
            <option value="self">self</option>
            <option value="target">target</option>
          </select>
        </div>
      </div>
    </div>
    <button
      class="btn btn-primary btn-sm"
      @click="addSound"
    >
      + Add Sound
    </button>
  </div>
</template>

<style scoped>
.sound-config-editor {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sound-card {
  border: 1px solid var(--p-content-border-color, #333);
  border-left: 3px solid var(--p-cyan-400, #22d3ee);
  border-radius: 6px;
  padding: 0.6rem;
  background: var(--p-content-background, #1a1a2e);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sound-type-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.sound-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.sound-field {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.field-label-sm {
  font-size: 0.75rem;
  color: var(--p-form-field-placeholder-color, #888);
}

.field-input-sm {
  width: 5.5rem;
  padding: 0.3rem 0.45rem;
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 4px;
  background: var(--p-form-field-background, #111);
  color: var(--p-text-color, #fff);
  font-size: 0.8rem;
}
</style>
