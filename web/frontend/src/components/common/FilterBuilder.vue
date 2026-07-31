<script setup lang="ts">
import AppCombobox from './AppCombobox.vue'
import { STATE_SUGGESTIONS } from './stateFilters'

interface FilterEntry {
  target?: string
  state?: string
  tool?: string
}

const props = defineProps<{
  modelValue: FilterEntry[]
  tagSuggestions: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: FilterEntry[]]
}>()

function addFilter() {
  emit('update:modelValue', [...props.modelValue, {}])
}

function removeFilter(index: number) {
  const copy = [...props.modelValue]
  copy.splice(index, 1)
  emit('update:modelValue', copy)
}

function updateFilter(index: number, key: keyof FilterEntry, value: string) {
  const copy = [...props.modelValue]
  copy[index] = { ...copy[index], [key]: value || undefined }
  emit('update:modelValue', copy)
}
</script>

<template>
  <div class="filter-builder">
    <div
      v-for="(entry, idx) in modelValue"
      :key="idx"
      class="filter-entry"
    >
      <div class="filter-fields">
        <AppCombobox
          :model-value="entry.target ?? ''"
          :suggestions="tagSuggestions"
          label="Target"
          placeholder="e.g. #minecraft:logs"
          :name="'target-' + idx"
          @update:model-value="updateFilter(idx, 'target', $event)"
        />

        <AppCombobox
          :model-value="entry.state ?? ''"
          :suggestions="STATE_SUGGESTIONS"
          label="State"
          placeholder="e.g. is_sneaking"
          :name="'state-' + idx"
          @update:model-value="updateFilter(idx, 'state', $event)"
        />

        <AppCombobox
          :model-value="entry.tool ?? ''"
          :suggestions="tagSuggestions"
          label="Tool"
          placeholder="e.g. #c:pickaxes"
          :name="'tool-' + idx"
          @update:model-value="updateFilter(idx, 'tool', $event)"
        />
      </div>

      <button
        class="btn btn-ghost btn-sm"
        style="color: var(--p-red-500, #ef4444)"
        @click="removeFilter(idx)"
      >
        &times;
      </button>
    </div>

    <button
      class="btn btn-primary btn-sm"
      @click="addFilter"
    >
      + Add Filter
    </button>
  </div>
</template>

<style scoped>
.filter-builder {
  margin-bottom: 1rem;
}

.filter-entry {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.6rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-content-background);
  margin-bottom: 0.5rem;
}

.filter-fields {
  flex: 1;
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}


</style>
