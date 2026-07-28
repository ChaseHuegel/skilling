<script setup lang="ts">
import { ref } from 'vue'
import MaterialMultiSelect from './MaterialMultiSelect.vue'

const props = defineProps<{
  modelValue: Record<string, string[]>
  suggestions: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, string[]>]
}>()

const addingTag = ref(false)
const newTagName = ref('')

const entries = () => Object.entries(props.modelValue)

function updateTagMaterials(tag: string, materials: string[]) {
  emit('update:modelValue', { ...props.modelValue, [tag]: materials })
}

function removeTag(tag: string) {
  const copy = { ...props.modelValue }
  delete copy[tag]
  emit('update:modelValue', copy)
}

function startAddTag() {
  addingTag.value = true
  newTagName.value = ''
}

function confirmAddTag() {
  const name = newTagName.value.trim()
  if (!name) {
    addingTag.value = false
    return
  }
  if (!/^[a-z_]+$/.test(name)) return
  if (name in props.modelValue) {
    addingTag.value = false
    return
  }
  const key = name.startsWith('#c:') ? name : '#c:' + name
  emit('update:modelValue', { ...props.modelValue, [key]: [] })
  addingTag.value = false
}

function cancelAddTag() {
  addingTag.value = false
}
</script>

<template>
  <div class="tag-list-editor">
    <div
      v-for="[tag, materials] in entries()"
      :key="tag"
      class="tag-entry"
    >
      <div class="tag-header">
        <span class="tag-name">{{ tag }}</span>
        <button
          v-if="entries().length > 1"
          class="btn-remove-tag"
          @click="removeTag(tag)"
        >
          Remove Tag
        </button>
      </div>
      <MaterialMultiSelect
        :model-value="materials"
        :suggestions="suggestions"
        @update:model-value="updateTagMaterials(tag, $event)"
      />
    </div>

    <div v-if="!addingTag">
      <button
        class="btn-add"
        @click="startAddTag"
      >
        + Add Tag
      </button>
    </div>

    <div
      v-else
      class="add-tag-row"
    >
      <input
        class="tag-name-input"
        type="text"
        v-model="newTagName"
        placeholder="tag_name"
        @keydown.enter="confirmAddTag"
        @keydown.escape="cancelAddTag"
      />
      <button
        class="btn-confirm"
        @click="confirmAddTag"
      >
        OK
      </button>
      <button
        class="btn-cancel"
        @click="cancelAddTag"
      >
        Cancel
      </button>
      <span class="tag-name-hint">Name must match <code>[a-z_]+</code></span>
    </div>
  </div>
</template>

<style scoped>
.tag-list-editor {
  margin-bottom: 1rem;
}

.tag-entry {
  padding: 0.6rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-content-background);
  margin-bottom: 0.5rem;
}

.tag-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.tag-name {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--p-primary-color);
  font-family: monospace;
}

.btn-remove-tag {
  background: none;
  border: 1px solid color-mix(in srgb, var(--p-red-500, #f87171), black 60%);
  color: var(--p-red-500, #f87171);
  border-radius: 4px;
  padding: 0.25rem 0.5rem;
  font-size: 0.8rem;
  cursor: pointer;
}

.btn-remove-tag:hover {
  background: color-mix(in srgb, var(--p-red-500, #f87171), black 80%);
}

.btn-add {
  background: color-mix(in srgb, var(--p-primary-color), black 70%);
  color: var(--p-text-color);
  border: 1px solid color-mix(in srgb, var(--p-primary-color), black 55%);
  border-radius: 4px;
  padding: 0.4rem 0.75rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-add:hover {
  background: color-mix(in srgb, var(--p-primary-color), black 55%);
}

.add-tag-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  flex-wrap: wrap;
}

.tag-name-input {
  width: 160px;
  padding: 0.35rem 0.5rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-content-background);
  color: var(--p-text-color);
  font-size: 0.85rem;
  font-family: monospace;
}

.btn-confirm {
  background: color-mix(in srgb, var(--p-green-600, #16a34a), black 75%);
  color: var(--p-text-color);
  border: 1px solid color-mix(in srgb, var(--p-green-600, #16a34a), black 50%);
  border-radius: 4px;
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-confirm:hover {
  background: color-mix(in srgb, var(--p-green-600, #16a34a), black 60%);
}

.btn-cancel {
  background: color-mix(in srgb, var(--p-red-600, #ef4444), black 80%);
  color: var(--p-text-color);
  border: 1px solid color-mix(in srgb, var(--p-red-500, #f87171), black 60%);
  border-radius: 4px;
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-cancel:hover {
  background: color-mix(in srgb, var(--p-red-600, #ef4444), black 65%);
}

.tag-name-hint {
  font-size: 0.75rem;
  color: var(--p-form-field-placeholder-color);
}

.tag-name-hint code {
  background: var(--p-form-field-background);
  padding: 0.1rem 0.3rem;
  border-radius: 2px;
  color: var(--p-text-color);
}
</style>
