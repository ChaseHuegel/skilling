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
    </div>
    <button class="tab-add-btn" title="Add page" @click="promptAddPage">
      +
    </button>

    <div v-if="confirmRemoveIndex !== null" class="confirm-overlay" @click.self="confirmRemoveIndex = null">
      <div class="confirm-dialog">
        <p>Remove page "{{ pages[confirmRemoveIndex]?.label }}"?</p>
        <div class="confirm-actions">
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
}>()

const renamingIndex = ref<number | null>(null)
const renameInput = ref<HTMLInputElement | null>(null)
const confirmRemoveIndex = ref<number | null>(null)
const pendingLabel = ref('')

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

function promptAddPage() {
  const label = prompt('New page label:', '&fNew Page')
  if (label && label.trim()) {
    emit('add', label.trim())
  }
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

// Custom directive for autofocus
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
}

.page-tab:hover {
  background: var(--p-content-hover-background, #2a2a4e);
}

.page-tab-active {
  background: color-mix(in srgb, var(--p-primary-color, #3b82f6) 25%, transparent);
  border-color: var(--p-primary-color, #3b82f6);
  color: white;
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

.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.confirm-dialog {
  background: var(--p-content-background, #1a1a2e);
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 8px;
  padding: 16px;
  max-width: 320px;
}

.confirm-dialog p {
  margin: 0 0 12px;
  color: #ccc;
  font-size: 0.9rem;
}

.confirm-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
