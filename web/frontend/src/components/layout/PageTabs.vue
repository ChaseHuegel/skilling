<template>
  <div class="page-tabs">
    <div
      v-for="(page, idx) in pages"
      :key="idx"
      class="page-tab"
      :class="{ 'page-tab-active': idx === activeIndex }"
      @click="$emit('select', idx)"
    >
      <span
        class="page-tab-label"
        v-html="renderedLabels[idx]"
        @dblclick="startRename(idx)"
        v-if="renamingIndex !== idx"
      ></span>
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
      <button
        class="tab-remove-btn"
        title="Remove page"
        @click.stop="confirmRemove(idx)"
        v-if="pages.length > 1"
      >
        &times;
      </button>
      <div class="tab-actions-dropdown" v-if="pages.length > 1">
        <button class="tab-actions-toggle" title="Page actions" @click.stop="toggleActions(idx)">&#8942;</button>
        <div class="tab-actions-menu" v-if="actionsOpen === idx" @click.stop>
          <button class="action-item" @click.stop="emit('duplicate', idx); actionsOpen = null">
            Duplicate
          </button>
          <button class="action-item" @click.stop="emit('clearSlots', idx); actionsOpen = null">
            Clear slots
          </button>
          <button class="action-item" :disabled="idx === 0" @click.stop="emit('moveLeft', idx); actionsOpen = null">
            Move left
          </button>
          <button class="action-item" :disabled="idx === pages.length - 1" @click.stop="emit('moveRight', idx); actionsOpen = null">
            Move right
          </button>
        </div>
      </div>
    </div>
    <button class="tab-add-btn" title="Add page" @click="showAddModal = true">
      +
    </button>

    <!-- Add page modal -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="showAddModal = false">
      <div class="modal-dialog modal-sm">
        <h3 class="modal-title">New page</h3>
        <input
          ref="addInput"
          class="modal-input"
          type="text"
          v-model="newPageLabel"
          placeholder="&6Page label"
          @keydown.enter="executeAdd"
          @keydown.escape="showAddModal = false"
          v-focus
        />
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
import { renderFormattedText, parseAmpersandCodes } from '../../utils/minecraftColors'

export interface PageTabData {
  label: string
}

const props = defineProps<{
  pages: PageTabData[]
  activeIndex: number
}>()

const emit = defineEmits<{
  select: [index: number]
  add: [label: string]
  remove: [index: number]
  rename: [index: number, label: string]
  duplicate: [index: number]
  clearSlots: [index: number]
  moveLeft: [index: number]
  moveRight: [index: number]
}>()

const renamingIndex = ref<number | null>(null)
const renameInput = ref<HTMLInputElement | null>(null)
const confirmRemoveIndex = ref<number | null>(null)
const pendingLabel = ref('')
const showAddModal = ref(false)
const addInput = ref<HTMLInputElement | null>(null)
const newPageLabel = ref('&fNew Page')
const actionsOpen = ref<number | null>(null)

const renderedLabels = computed(() =>
  props.pages.map(p => renderFormattedText(parseAmpersandCodes(p.label)))
)

async function startRename(idx: number) {
  renamingIndex.value = idx
  pendingLabel.value = props.pages[idx].label
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

function toggleActions(idx: number) {
  actionsOpen.value = actionsOpen.value === idx ? null : idx
}

function confirmRemove(idx: number) {
  actionsOpen.value = null
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
    emit('add', label)
  }
  showAddModal.value = false
  newPageLabel.value = '&fNew Page'
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
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
}

.page-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 16px;
  background: var(--p-content-background, #1a1a2e);
  color: var(--p-text-color, #ccc);
  font-size: 0.8rem;
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
  position: relative;
}

.page-tab:hover {
  background: var(--p-content-hover-background, #2a2a4e);
}

.page-tab-active {
  background: var(--p-primary-color, #3b82f6);
  border-color: var(--p-primary-color, #3b82f6);
  color: #fff;
  box-shadow: 0 0 8px color-mix(in srgb, var(--p-primary-color, #3b82f6) 40%, transparent);
}

.page-tab-label {
  pointer-events: none;
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

.tab-remove-btn {
  background: none;
  border: none;
  color: var(--p-red-500, #ef4444);
  cursor: pointer;
  font-size: 0.9rem;
  line-height: 1;
  padding: 0 2px;
  opacity: 0.6;
  transition: opacity 0.15s;
}

.tab-remove-btn:hover {
  opacity: 1;
}

.tab-actions-dropdown {
  position: relative;
}

.tab-actions-toggle {
  background: none;
  border: none;
  color: var(--p-text-muted-color, #888);
  cursor: pointer;
  font-size: 0.85rem;
  line-height: 1;
  padding: 0 2px;
  opacity: 0.4;
  transition: opacity 0.15s;
}

.page-tab:hover .tab-actions-toggle {
  opacity: 0.8;
}

.tab-actions-toggle:hover {
  opacity: 1;
  color: var(--p-text-color, #ccc);
}

.tab-actions-menu {
  position: absolute;
  top: 100%;
  right: 0;
  z-index: 100;
  min-width: 120px;
  background: var(--p-content-background, #1a1a2e);
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  margin-top: 4px;
}

.action-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 6px 12px;
  background: none;
  border: none;
  color: var(--p-text-color, #ccc);
  font-size: 0.8rem;
  cursor: pointer;
  transition: background 0.1s;
}

.action-item:hover:not(:disabled) {
  background: var(--p-content-hover-background, #2a2a4e);
}

.action-item:disabled {
  opacity: 0.3;
  cursor: default;
}

.tab-add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px dashed var(--p-content-border-color, #555);
  border-radius: 50%;
  background: transparent;
  color: var(--p-primary-color, #3b82f6);
  font-size: 1.1rem;
  cursor: pointer;
  transition: all 0.15s;
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
  max-width: 320px;
  width: 90%;
}

.modal-sm {
  max-width: 280px;
}

.modal-title {
  margin: 0 0 8px;
  color: var(--p-text-color, #fff);
  font-size: 1rem;
  font-weight: 600;
}

.modal-dialog p {
  margin: 0 0 12px;
  color: var(--p-text-muted-color, #aaa);
  font-size: 0.85rem;
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
  margin-bottom: 12px;
}

.modal-input:focus {
  border-color: var(--p-primary-color, #3b82f6);
}

.modal-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
