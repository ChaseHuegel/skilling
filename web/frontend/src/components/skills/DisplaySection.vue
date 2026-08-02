<script setup lang="ts">
import { ref } from 'vue'
import MaterialPicker from '../common/MaterialPicker.vue'
import { parseAmpersandCodes, segmentStyle } from '../../utils/minecraftColors'
import { stableKey } from '../../utils/stableKey'

interface LoreLine {
  _key: string
  text: string
}

interface DisplayConfig {
  icon: string
  customModelData: number
  color: string
  style: string
  lore: LoreLine[]
}

const props = defineProps<{
  modelValue: DisplayConfig
}>()

const emit = defineEmits<{
  'update:modelValue': [value: DisplayConfig]
}>()

const COLOR_OPTIONS = ['WHITE', 'RED', 'GREEN', 'BLUE', 'YELLOW', 'PINK', 'PURPLE'] as const
const STYLE_OPTIONS = ['SOLID', 'SEGMENTED_6', 'SEGMENTED_10', 'SEGMENTED_12', 'SEGMENTED_20'] as const
const LORE_PLACEHOLDERS = ['{level}', '{max_level}', '{skill_name}', '{xp}']

const loreExpanded = ref(true)
const loreDragIndex = ref<number | null>(null)

function setField<K extends keyof DisplayConfig>(key: K, val: DisplayConfig[K]) {
  emit('update:modelValue', { ...props.modelValue, [key]: val })
}

function loreLines(): LoreLine[] {
  return (props.modelValue.lore || []).map(line => {
    if (typeof line === 'string') return { _key: stableKey(), text: line }
    return line
  })
}

function addLoreLine() {
  setField('lore', [...loreLines(), { _key: stableKey(), text: '' }])
}

function removeLoreLine(index: number) {
  const newLore = [...loreLines()]
  newLore.splice(index, 1)
  setField('lore', newLore)
}

function updateLoreLine(index: number, value: string) {
  const newLore = [...loreLines()]
  newLore[index] = { ...newLore[index], text: value }
  setField('lore', newLore)
}

function insertPlaceholder(placeholder: string) {
  setField('lore', [...loreLines(), { _key: stableKey(), text: placeholder }])
}

function onLoreDragStart(event: DragEvent, index: number) {
  loreDragIndex.value = index
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function onLoreDragOver(event: DragEvent, index: number) {
  event.preventDefault()
  if (loreDragIndex.value === null || loreDragIndex.value === index) return
  const lore = [...loreLines()]
  const item = lore.splice(loreDragIndex.value, 1)[0]
  lore.splice(index, 0, item)
  loreDragIndex.value = index
  setField('lore', lore)
}

function onLoreDragEnd() {
  loreDragIndex.value = null
}
</script>

<template>
  <div class="display-section">
    <div class="field-row">
      <label class="field-label">Icon</label>
      <MaterialPicker
        :model-value="modelValue.icon"
        label=""
        @update:model-value="setField('icon', $event)"
      />
    </div>

    <div class="field-row">
      <label class="field-label">Custom Model Data</label>
      <input
        class="field-input"
        type="number"
        step="any"
        :value="modelValue.customModelData"
        @input="setField('customModelData', Number(($event.target as HTMLInputElement).value))"
      />
    </div>

    <div class="field-row">
      <label class="field-label">Color</label>
      <select
        class="field-select"
        :value="modelValue.color"
        @change="setField('color', ($event.target as HTMLSelectElement).value)"
      >
        <option v-for="c in COLOR_OPTIONS" :key="c" :value="c">{{ c }}</option>
      </select>
    </div>

    <div class="field-row">
      <label class="field-label">Style</label>
      <select
        class="field-select"
        :value="modelValue.style"
        @change="setField('style', ($event.target as HTMLSelectElement).value)"
      >
        <option v-for="s in STYLE_OPTIONS" :key="s" :value="s">{{ s }}</option>
      </select>
    </div>

    <div class="lore-section">
      <div class="lore-header" @click="loreExpanded = !loreExpanded">
        <span class="lore-toggle">{{ loreExpanded ? '▼' : '▶' }}</span>
        <span class="lore-title">Lore Lines</span>
        <span class="lore-count">{{ (modelValue.lore || []).length }} lines</span>
      </div>

      <div v-if="loreExpanded" class="lore-body">
        <div v-if="modelValue.lore && modelValue.lore.length > 0" class="lore-list">
          <div
            v-for="(line, i) in modelValue.lore"
            :key="line._key"
            class="lore-row"
            draggable="true"
            @dragstart="onLoreDragStart($event, i)"
            @dragover="onLoreDragOver($event, i)"
            @dragend="onLoreDragEnd"
          >
            <span class="drag-handle" title="Drag to reorder">&#8801;</span>
            <input
              class="lore-input"
              type="text"
              :value="line.text"
              @input="updateLoreLine(i, ($event.target as HTMLInputElement).value)"
              placeholder="&7Enter lore text..."
            />
            <button class="btn-remove-lore" @click="removeLoreLine(i)" title="Remove line">&times;</button>
          </div>
        </div>
        <div v-else class="lore-empty">No lore lines configured.</div>

        <div class="lore-preview" v-if="modelValue.lore && modelValue.lore.length > 0">
          <div class="preview-label">Preview:</div>
          <div
            v-for="line in modelValue.lore"
            :key="'preview-' + line._key"
            class="preview-line"
          >
            <span
              v-for="(seg, sIdx) in parseAmpersandCodes(line.text)"
              :key="sIdx"
              :style="segmentStyle(seg)"
            >{{ seg.text }}</span>
          </div>
        </div>

        <div class="lore-actions">
          <div class="placeholder-chips">
            <span class="placeholder-label">Insert:</span>
            <button
              v-for="ph in LORE_PLACEHOLDERS"
              :key="ph"
              class="chip"
              @click="insertPlaceholder(ph)"
            >{{ ph }}</button>
          </div>
          <button class="btn-add-lore" @click="addLoreLine">+ Add Line</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.display-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.field-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.field-label {
  min-width: 8rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--p-text-color);
}

.field-input {
  flex: 1;
  padding: 0.45rem 0.6rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-content-background);
  color: var(--p-text-color);
  font-size: 0.875rem;
}

.field-select {
  flex: 1;
  padding: 0.45rem 0.6rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-form-field-background);
  color: var(--p-text-color);
  font-size: 0.875rem;
}

.lore-section {
  border: 1px solid var(--p-content-border-color);
  border-radius: 6px;
  overflow: hidden;
}

.lore-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  background: var(--p-content-background);
  border-bottom: 1px solid var(--p-content-border-color);
  user-select: none;
}

.lore-header:hover {
  background: color-mix(in srgb, var(--p-primary-color, #3b82f6) 5%, transparent);
}

.lore-toggle {
  font-size: 0.7rem;
  color: var(--p-text-muted-color);
}

.lore-title {
  font-size: 0.85rem;
  font-weight: 600;
}

.lore-count {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
  margin-left: auto;
}

.lore-body {
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.lore-list {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.lore-row {
  display: flex;
  gap: 0.4rem;
  align-items: center;
}

.drag-handle {
  cursor: grab;
  color: var(--p-text-muted-color, #888);
  font-size: 1.1rem;
  user-select: none;
  flex-shrink: 0;
}
.drag-handle:active {
  cursor: grabbing;
}

.lore-input {
  flex: 1;
  padding: 0.35rem 0.5rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-form-field-background);
  color: var(--p-text-color);
  font-size: 0.85rem;
}

.lore-input:focus {
  outline: none;
  border-color: var(--p-primary-color, #3b82f6);
  box-shadow: 0 0 0 1px var(--p-primary-color, #3b82f6);
}

.btn-remove-lore {
  background: transparent;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  cursor: pointer;
  color: var(--p-red-500, #ef4444);
  font-size: 1rem;
  width: 1.8rem;
  height: 1.8rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.btn-remove-lore:hover {
  background: color-mix(in srgb, var(--p-red-500, #ef4444) 10%, transparent);
  border-color: var(--p-red-500, #ef4444);
}

.lore-empty {
  color: var(--p-text-muted-color, #888);
  font-style: italic;
  font-size: 0.85rem;
}

.lore-preview {
  padding: 0.5rem;
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
}

.preview-label {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
  margin-bottom: 0.25rem;
}

.preview-line {
  font-size: 0.85rem;
  line-height: 1.4;
}

.lore-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.placeholder-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  align-items: center;
}

.placeholder-label {
  font-size: 0.8rem;
  color: var(--p-text-muted-color, #888);
}

.chip {
  background: var(--p-primary-color, #3b82f6);
  color: var(--p-primary-contrast-color, #fff);
  border: none;
  border-radius: 4px;
  padding: 0.2rem 0.5rem;
  font-size: 0.75rem;
  cursor: pointer;
  font-family: monospace;
}

.chip:hover {
  opacity: 0.85;
}

.btn-add-lore {
  align-self: flex-start;
  background: transparent;
  border: 1px dashed var(--p-content-border-color);
  border-radius: 4px;
  padding: 0.35rem 0.65rem;
  cursor: pointer;
  color: var(--p-primary-color, #3b82f6);
  font-size: 0.85rem;
}

.btn-add-lore:hover {
  background: color-mix(in srgb, var(--p-primary-color, #3b82f6) 5%, transparent);
  border-color: var(--p-primary-color, #3b82f6);
}
</style>
