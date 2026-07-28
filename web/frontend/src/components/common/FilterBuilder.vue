<script setup lang="ts">
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
        <div class="filter-field">
          <label class="filter-label">Target</label>
          <input
            class="filter-input"
            type="text"
            placeholder="e.g. #minecraft:logs"
            list="target-suggestions"
            :value="entry.target ?? ''"
            @input="updateFilter(idx, 'target', ($event.target as HTMLInputElement).value)"
          />
          <datalist id="target-suggestions">
            <option
              v-for="tag in tagSuggestions"
              :key="tag"
              :value="tag"
            />
          </datalist>
        </div>

        <div class="filter-field">
          <label class="filter-label">State</label>
          <input
            class="filter-input"
            type="text"
            placeholder="e.g. is_sneaking"
            :value="entry.state ?? ''"
            @input="updateFilter(idx, 'state', ($event.target as HTMLInputElement).value)"
          />
          <span class="state-hint">Options: is_sneaking, is_sprinting, is_in_water, is_on_ground, player_placed:false</span>
        </div>

        <div class="filter-field">
          <label class="filter-label">Tool</label>
          <input
            class="filter-input"
            type="text"
            placeholder="e.g. #c:pickaxes"
            list="tool-suggestions"
            :value="entry.tool ?? ''"
            @input="updateFilter(idx, 'tool', ($event.target as HTMLInputElement).value)"
          />
          <datalist id="tool-suggestions">
            <option
              v-for="tag in tagSuggestions"
              :key="tag"
              :value="tag"
            />
          </datalist>
        </div>
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

.filter-field {
  flex: 1;
  min-width: 140px;
}

.filter-label {
  display: block;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--p-form-field-placeholder-color);
  margin-bottom: 0.2rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.filter-input {
  width: 100%;
  padding: 0.35rem 0.5rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-form-field-background);
  color: var(--p-text-color);
  font-size: 0.85rem;
  box-sizing: border-box;
}

.state-hint {
  display: block;
  font-size: 0.7rem;
  color: var(--p-form-field-placeholder-color);
  margin-top: 0.2rem;
  line-height: 1.2;
}


</style>
