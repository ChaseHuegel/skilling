<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  modelValue: string[]
  suggestions: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const inputValue = ref('')

function addEntry() {
  const value = inputValue.value.trim()
  if (!value) return
  if (props.modelValue.includes(value)) return
  emit('update:modelValue', [...props.modelValue, value])
  inputValue.value = ''
}

function removeEntry(index: number) {
  const copy = [...props.modelValue]
  copy.splice(index, 1)
  emit('update:modelValue', copy)
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    addEntry()
  }
}
</script>

<template>
  <div class="material-multi-select">
    <div class="input-row">
      <input
        class="multi-input"
        type="text"
        placeholder="e.g. minecraft:stone"
        list="multi-suggestions"
        v-model="inputValue"
        @keydown="onKeydown"
      />
      <datalist id="multi-suggestions">
        <option
          v-for="suggestion in suggestions"
          :key="suggestion"
          :value="suggestion"
        />
      </datalist>
      <button
        class="btn btn-primary btn-sm"
        @click="addEntry"
      >
        Add
      </button>
    </div>

    <div
      v-if="modelValue.length > 0"
      class="chips"
    >
      <span
        v-for="(entry, idx) in modelValue"
        :key="entry"
        class="chip"
        :class="{ 'chip-tag': entry.startsWith('#') }"
      >
        <span class="chip-text">{{ entry }}</span>
        <button
          class="chip-remove"
          @click="removeEntry(idx)"
        >
          &times;
        </button>
      </span>
    </div>
  </div>
</template>

<style scoped>
.material-multi-select {
  margin-bottom: 0.5rem;
}

.input-row {
  display: flex;
  gap: 0.5rem;
}

.multi-input {
  flex: 1;
  padding: 0.35rem 0.5rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-content-background);
  color: var(--p-text-color);
  font-size: 0.85rem;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-top: 0.4rem;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.5rem;
  background: var(--p-form-field-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  font-size: 0.8rem;
  color: var(--p-text-color);
}

.chip-tag {
  border-color: var(--p-primary-color);
  background: var(--p-content-background);
}

.chip-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chip-remove {
  background: none;
  border: none;
  color: var(--p-red-500, #f87171);
  font-size: 1rem;
  cursor: pointer;
  padding: 0 0.1rem;
  line-height: 1;
}

.chip-remove:hover {
  color: var(--p-red-600, #ef4444);
}
</style>
