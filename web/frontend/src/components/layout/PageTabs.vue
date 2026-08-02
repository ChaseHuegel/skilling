<template>
  <div class="page-tabs">
    <div
      v-for="(page, idx) in pages"
      :key="keyFor(page)"
      class="page-tab"
      :class="{ 'page-tab-active': idx === activeIndex, 'page-tab-drag-over': dragOverIndex === idx && dragOverIndex !== dragSourceIndex }"
      draggable="true"
      @click="$emit('select', idx)"
      @dragstart="onDragStart(idx, $event)"
      @dragenter.prevent="onDragEnter(idx)"
      @dragover.prevent
      @dragleave="onDragLeave(idx)"
      @drop.prevent="onDrop(idx)"
      @dragend="onDragEnd"
    >
      <MinecraftIcon :material="page.icon || 'minecraft:book'" :size="22" />

      <span
        class="page-tab-label"
        v-if="renamingIndex !== idx"
      >{{ plainLabels[idx] }}</span>
      <input
        v-else
        ref="renameInput"
        class="page-tab-rename-input"
        :value="page.label"
        @blur="finishRename(idx, ($event.target as HTMLInputElement).value)"
        @keydown.enter="finishRename(idx, ($event.target as HTMLInputElement).value)"
        @keydown.escape="cancelRename"
        v-focus
      />

      <div class="tab-inline-actions">
        <button class="tab-action-btn" title="Rename" @click.stop="startRename(idx)">
          <svg viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="currentColor" stroke-width="1.4">
            <path d="M11.5 2.5a1.41 1.41 0 112 2l-8 8L2 12l.5-3.5 8-8z" stroke-linejoin="round"/>
          </svg>
        </button>
        <button class="tab-action-btn" title="Duplicate" @click.stop="emit('duplicate', idx)">
          <svg viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="currentColor" stroke-width="1.4">
            <rect x="3.5" y="5.5" width="8" height="9" rx="1"/>
            <path d="M5.5 5.5V3a1 1 0 011-1h5a1 1 0 011 1v6a1 1 0 01-1 1h-.5"/>
          </svg>
        </button>
        <button
          class="tab-action-btn tab-action-danger"
          title="Remove page"
          @click.stop="confirmRemove(idx)"
          v-if="pages.length > 1"
        >
          <svg viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="currentColor" stroke-width="1.4">
            <path d="M3 4h10M6 4V2.5a.5.5 0 01.5-.5h3a.5.5 0 01.5.5V4M5 4v8a1 1 0 001 1h4a1 1 0 001-1V4" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    </div>
    <button class="tab-add-btn" title="Add page" @click="showAddModal = true">
      + Add Page
    </button>

    <!-- Add page modal -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="showAddModal = false">
      <div class="modal-dialog">
        <h3 class="modal-title">New page</h3>
        <label class="modal-field">
          <span class="modal-field-label">Label</span>
          <input
            class="modal-input"
            type="text"
            v-model="newPageLabel"
            placeholder="Page label"
            @keydown.enter="executeAdd"
            @keydown.escape="showAddModal = false"
            v-focus
          />
        </label>
        <label class="modal-field">
          <span class="modal-field-label">Icon material</span>
          <MaterialPicker v-model="newPageIcon" />
        </label>
        <label class="modal-field">
          <span class="modal-field-label">Custom model data</span>
          <input
            class="modal-input"
            type="number"
            v-model.number="newPageCmd"
            placeholder="0"
            min="0"
          />
        </label>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="showAddModal = false">Cancel</button>
          <button class="btn btn-primary btn-sm" :disabled="!newPageLabel.trim()" @click="executeAdd">Add</button>
        </div>
      </div>
    </div>

    <!-- Remove confirmation modal -->
    <div v-if="confirmRemoveIndex !== null" class="modal-overlay" @click.self="confirmRemoveIndex = null">
      <div class="modal-dialog">
        <h3 class="modal-title">Remove page?</h3>
        <p>Remove page "{{ pages[confirmRemoveIndex]?.label }}"? This cannot be undone.</p>
        <div class="modal-actions">
          <button class="btn btn-secondary btn-sm" @click="confirmRemoveIndex = null">Cancel</button>
          <button class="btn btn-danger btn-sm" @click="executeRemove">Remove</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { stripAmpersandCodes } from '../../utils/minecraftColors'
import MinecraftIcon from '../common/MinecraftIcon.vue'
import MaterialPicker from '../common/MaterialPicker.vue'

export interface PageTabData {
  label: string
  icon?: string
  customModelData?: number
}

const props = defineProps<{
  pages: PageTabData[]
  activeIndex: number
}>()

const emit = defineEmits<{
  select: [index: number]
  add: [label: string, icon: string, customModelData: number]
  remove: [index: number]
  rename: [index: number, label: string]
  duplicate: [index: number]
  clearSlots: [index: number]
  moveLeft: [index: number]
  moveRight: [index: number]
}>()

// Stable DOM key per page: keyed by object identity so a drag reorder (which
// keeps the same page objects) moves the tab nodes instead of reusing them by
// index, preserving rename-input focus across reorders.
const pageKeys = new WeakMap<PageTabData, string>()
let pageKeySeed = 0
function keyFor(page: PageTabData): string {
  let key = pageKeys.get(page)
  if (!key) {
    key = `page-${(pageKeySeed += 1)}`
    pageKeys.set(page, key)
  }
  return key
}

const renamingIndex = ref<number | null>(null)
const renameInput = ref<HTMLInputElement | null>(null)
const confirmRemoveIndex = ref<number | null>(null)
const showAddModal = ref(false)
const newPageLabel = ref('New Page')
const newPageIcon = ref('minecraft:book')
const newPageCmd = ref(0)
const dragSourceIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)

const plainLabels = computed(() =>
  props.pages.map(p => stripAmpersandCodes(p.label))
)

async function startRename(idx: number) {
  renamingIndex.value = idx
  await nextTick()
  renameInput.value?.focus()
  renameInput.value?.select()
}

function finishRename(idx: number, label: string) {
  if (renamingIndex.value !== idx) return
  renamingIndex.value = null
  const trimmed = label.trim()
  if (trimmed && trimmed !== props.pages[idx].label) {
    emit('rename', idx, trimmed)
  }
}

function cancelRename() {
  renamingIndex.value = null
}

function confirmRemove(idx: number) {
  confirmRemoveIndex.value = idx
}

function executeRemove() {
  if (confirmRemoveIndex.value !== null) {
    emit('remove', confirmRemoveIndex.value)
    confirmRemoveIndex.value = null
  }
}

function executeAdd() {
  const label = newPageLabel.value.trim()
  if (label) {
    emit('add', label, newPageIcon.value || 'minecraft:book', newPageCmd.value)
  }
  showAddModal.value = false
  newPageLabel.value = 'New Page'
  newPageIcon.value = 'minecraft:book'
  newPageCmd.value = 0
}

function onDragStart(idx: number, e: DragEvent) {
  dragSourceIndex.value = idx
  e.dataTransfer?.setData('text/plain', String(idx))
  e.dataTransfer!.effectAllowed = 'move'
}

function onDragEnter(idx: number) {
  if (idx !== dragSourceIndex.value) {
    dragOverIndex.value = idx
  }
}

function onDragLeave(idx: number) {
  if (dragOverIndex.value === idx) {
    dragOverIndex.value = null
  }
}

function onDrop(idx: number) {
  dragOverIndex.value = null
  if (dragSourceIndex.value === null || dragSourceIndex.value === idx) return
  const from = dragSourceIndex.value
  const to = idx
  if (from < to) {
    for (let i = from; i < to; i++) {
      emit('moveLeft', i + 1)
    }
  } else {
    for (let i = from; i > to; i--) {
      emit('moveRight', i - 1)
    }
  }
  dragSourceIndex.value = null
}

function onDragEnd() {
  dragOverIndex.value = null
  dragSourceIndex.value = null
}

const vFocus = {
  mounted(el: HTMLElement) {
    el.focus()
  },
}
</script>

<style scoped>
.page-tabs {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.page-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 6px;
  background: var(--p-content-background, #1a1a2e);
  color: var(--p-text-color, #ccc);
  font-size: 0.85rem;
  cursor: grab;
  transition: all 0.15s;
  user-select: none;
  position: relative;
  flex-shrink: 0;
}

.page-tab:active {
  cursor: grabbing;
}

.page-tab:hover {
  background: var(--p-content-hover-background, #2a2a4e);
}

.page-tab-active {
  background: transparent;
  border-color: var(--p-primary-color, #3b82f6);
  color: var(--p-primary-color, #3b82f6);
}

.page-tab-drag-over {
  border-color: var(--p-primary-color, #3b82f6);
  background: color-mix(in srgb, var(--p-primary-color, #3b82f6) 20%, transparent);
}

.page-tab-label {
  pointer-events: none;
  cursor: inherit;
  white-space: nowrap;
}

.page-tab-rename-input {
  width: 100px;
  padding: 2px 4px;
  font-size: 0.8rem;
  border: 1px solid var(--p-primary-color, #3b82f6);
  border-radius: 4px;
  background: var(--p-form-field-background, #111);
  color: var(--p-text-color, #fff);
  outline: none;
}

.tab-inline-actions {
  display: flex;
  align-items: center;
  gap: 1px;
  margin-left: 2px;
}

.tab-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 3px;
  background: transparent;
  color: var(--p-text-muted-color, #888);
  cursor: pointer;
  opacity: 0.5;
  transition: opacity 0.15s, color 0.15s, background 0.15s;
  padding: 0;
}

.page-tab:hover .tab-action-btn {
  opacity: 0.8;
}

.tab-action-btn:hover {
  opacity: 1;
  background: var(--p-content-hover-background, rgba(255,255,255,0.1));
  color: var(--p-text-color, #ccc);
}

.tab-action-danger:hover {
  color: var(--p-red-500, #ef4444);
}

.tab-add-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: 1px dashed var(--p-content-border-color, #555);
  border-radius: 6px;
  background: transparent;
  color: var(--p-primary-color, #3b82f6);
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
}

.tab-add-btn:hover {
  background: color-mix(in srgb, var(--p-primary-color, #3b82f6) 15%, transparent);
  border-color: var(--p-primary-color, #3b82f6);
}

/* Modal system */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-dialog {
  background: var(--p-content-background, #1a1a2e);
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 8px;
  padding: 16px;
  max-width: 360px;
  width: 90%;
}

.modal-title {
  margin: 0 0 12px;
  color: var(--p-text-color, #fff);
  font-size: 1rem;
  font-weight: 600;
}

.modal-dialog p {
  margin: 0 0 12px;
  color: var(--p-text-muted-color, #aaa);
  font-size: 0.85rem;
}

.modal-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

.modal-field-label {
  font-size: 0.8rem;
  color: var(--p-text-muted-color, #aaa);
  font-weight: 500;
}

.modal-input {
  width: 100%;
  padding: 6px 8px;
  font-size: 0.85rem;
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 4px;
  background: var(--p-form-field-background, #111);
  color: var(--p-text-color, #fff);
  outline: none;
  box-sizing: border-box;
}

.modal-input:focus {
  border-color: var(--p-primary-color, #3b82f6);
}

.modal-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 4px;
}
</style>
