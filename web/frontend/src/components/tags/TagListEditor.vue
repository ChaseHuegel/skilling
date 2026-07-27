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
  emit('update:modelValue', { ...props.modelValue, [name]: [] })
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
        <span class="tag-name">#c:{{ tag }}</span>
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
  border: 1px solid #333;
  border-radius: 4px;
  background: #1e1e1e;
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
  color: #60a5fa;
  font-family: monospace;
}

.btn-remove-tag {
  background: none;
  border: 1px solid #7a2a2a;
  color: #f87171;
  border-radius: 4px;
  padding: 0.25rem 0.5rem;
  font-size: 0.8rem;
  cursor: pointer;
}

.btn-remove-tag:hover {
  background: #3f1e1e;
}

.btn-add {
  background: #1e3a5f;
  color: #e0e0e0;
  border: 1px solid #2a4a7f;
  border-radius: 4px;
  padding: 0.4rem 0.75rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-add:hover {
  background: #2a4a7f;
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
  border: 1px solid #444;
  border-radius: 4px;
  background: #1e1e1e;
  color: #e0e0e0;
  font-size: 0.85rem;
  font-family: monospace;
}

.btn-confirm {
  background: #1e3f2a;
  color: #e0e0e0;
  border: 1px solid #2a7a3f;
  border-radius: 4px;
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-confirm:hover {
  background: #2a5f3f;
}

.btn-cancel {
  background: #3f1e1e;
  color: #e0e0e0;
  border: 1px solid #7a2a2a;
  border-radius: 4px;
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #5f2a2a;
}

.tag-name-hint {
  font-size: 0.75rem;
  color: #777;
}

.tag-name-hint code {
  background: #2a2a2a;
  padding: 0.1rem 0.3rem;
  border-radius: 2px;
  color: #e0e0e0;
}
</style>
