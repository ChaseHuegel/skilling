<script setup lang="ts">
import { ref, computed } from 'vue'
import MaterialMultiSelect from './MaterialMultiSelect.vue'
import { useDragReorder } from '../../composables/useDragReorder'

const props = defineProps<{
  modelValue: Record<string, string[]>
  suggestions: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, string[]>]
}>()

const addingTag = ref(false)
const newTagName = ref('')
const pendingRemoveTag = ref<string | null>(null)

const entriesList = computed({
  get: () => Object.entries(props.modelValue).map(([key, val]) => ({ key, val })),
  set: (newEntries) => {
    const obj: Record<string, string[]> = {}
    for (const e of newEntries) {
      obj[e.key] = e.val
    }
    emit('update:modelValue', obj)
  },
})
const { dragIndex, onDragStart, onDragOver, onDragEnd } = useDragReorder(entriesList)

function updateTagMaterials(tag: string, materials: string[]) {
  emit('update:modelValue', { ...props.modelValue, [tag]: materials })
}

function confirmRemoveTag(tag: string) {
  pendingRemoveTag.value = tag
}

function executeRemoveTag() {
  const tag = pendingRemoveTag.value
  if (!tag) return
  pendingRemoveTag.value = null
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
      v-for="(entry, idx) in entriesList"
      :key="entry.key"
      class="tag-entry"
      :class="{ 'drag-over': dragIndex !== null && dragIndex !== idx }"
      draggable="true"
      @dragstart="onDragStart(idx)"
      @dragover="onDragOver($event, idx)"
      @dragend="onDragEnd"
    >
      <div class="tag-header">
        <span class="drag-handle" title="Drag to reorder">&#8801;</span>
        <span class="tag-name">{{ entry.key }}</span>
        <button
          v-if="entriesList.length > 1"
          class="btn-remove-tag"
          title="Remove Tag"
          @click="confirmRemoveTag(entry.key)"
        >
          &times;
        </button>
      </div>
      <MaterialMultiSelect
        :model-value="entry.val"
        :suggestions="suggestions"
        @update:model-value="updateTagMaterials(entry.key, $event)"
      />
    </div>

    <div v-if="!addingTag">
      <button
        class="btn btn-primary btn-sm"
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
        class="btn btn-primary btn-sm"
        @click="confirmAddTag"
      >
        OK
      </button>
      <button
        class="btn btn-secondary btn-sm"
        @click="cancelAddTag"
      >
        Cancel
      </button>
      <span class="tag-name-hint">Name must match <code>[a-z_]+</code></span>
    </div>
    </div>

    <div v-if="pendingRemoveTag" class="modal-overlay" @click.self="pendingRemoveTag = null">
      <div class="modal">
        <h3>Remove tag?</h3>
        <p>This will permanently remove <strong>{{ pendingRemoveTag }}</strong> and its materials.</p>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="pendingRemoveTag = null">Cancel</button>
          <button class="btn btn-danger" @click="executeRemoveTag">Remove</button>
        </div>
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
.tag-entry[draggable="true"] {
  cursor: default;
}
.tag-entry.drag-over {
  opacity: 0.5;
}
.drag-handle {
  cursor: grab;
  color: var(--p-form-field-placeholder-color);
  font-size: 1.1rem;
  line-height: 1;
  user-select: none;
  margin-right: 0.35rem;
}
.drag-handle:active {
  cursor: grabbing;
}
.tag-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}
.btn-remove-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--p-red-500, #ef4444);
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-remove-tag:hover {
  background: color-mix(in srgb, var(--p-red-500, #ef4444) 10%, transparent);
}

.tag-name {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--p-primary-color);
  font-family: monospace;
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
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 8px;
  padding: 1.5rem;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}
.modal h3 {
  margin: 0 0 0.5rem;
  font-size: 1.05rem;
  color: var(--p-text-color);
}
.modal p {
  margin: 0 0 1.25rem;
  color: var(--p-text-muted-color, #888);
  font-size: 0.875rem;
  line-height: 1.4;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
