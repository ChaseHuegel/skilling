<script setup lang="ts">
import { ref } from 'vue'
import SoundConfigEditor, { type SoundConfig } from '../common/SoundConfigEditor.vue'

export interface FailureFeedback {
  actionBar: string
  sounds: SoundConfig[]
}

export interface OnFailure {
  reasons: Record<string, FailureFeedback>
}

const props = defineProps<{
  modelValue: OnFailure
  namePrefix: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: OnFailure]
}>()

const FAILURE_REASON_OPTIONS = ['cooldown', 'missing_item', 'missing_state']

const FAILURE_REASON_LABELS: Record<string, string> = {
  cooldown: 'Cooldown',
  missing_item: 'Missing Item',
  missing_state: 'Missing State',
}

const newFailureReason = ref('')

function updateReason(reason: string, fb: FailureFeedback) {
  const reasons = { ...props.modelValue.reasons, [reason]: fb }
  emit('update:modelValue', { reasons })
}

function addFailureReason() {
  const reason = newFailureReason.value
  if (!reason) return
  const of = props.modelValue
  if (of.reasons[reason]) return
  updateReason(reason, { actionBar: '', sounds: [] })
  newFailureReason.value = ''
}

function removeFailureReason(reason: string) {
  const reasons = { ...props.modelValue.reasons }
  delete reasons[reason]
  emit('update:modelValue', { reasons })
}

function updateFailureActionBar(reason: string, val: string) {
  const fb = props.modelValue.reasons[reason] || { actionBar: '', sounds: [] }
  updateReason(reason, { ...fb, actionBar: val })
}

function updateFailureSounds(reason: string, sounds: SoundConfig[]) {
  const fb = props.modelValue.reasons[reason] || { actionBar: '', sounds: [] }
  updateReason(reason, { ...fb, sounds })
}

function failurePlaceholder(reason: string): string {
  if (reason === 'cooldown') return "&cCooling down: {time}s";
  if (reason === 'missing_item') return "&cRequires {amount}x {item}";
  return '&c' + reason + ' message...';
}
</script>

<template>
  <div class="on-failure-editor">
    <div v-for="(fb, reason) in modelValue.reasons" :key="reason" class="failure-card">
      <div class="failure-header">
        <span class="failure-reason-label">{{ FAILURE_REASON_LABELS[reason] || reason }}</span>
        <button
          class="btn btn-ghost btn-sm"
          style="color: var(--p-red-500, #ef4444)"
          @click="removeFailureReason(reason)"
        >
          &times;
        </button>
      </div>
      <div class="failure-body">
        <div class="field-row">
          <label class="field-label">Action Bar</label>
          <input
            class="field-input"
            type="text"
            :placeholder="failurePlaceholder(reason)"
            :value="fb.actionBar"
            @input="updateFailureActionBar(reason, ($event.target as HTMLInputElement).value)"
          />
        </div>
        <div class="sub-section">
          <label class="sub-label">Sounds</label>
          <SoundConfigEditor
            :model-value="fb.sounds"
            :name-prefix="namePrefix + '-' + reason"
            placeholder="minecraft:block_note_block_bass"
            @update:model-value="updateFailureSounds(reason, $event)"
          />
        </div>
      </div>
    </div>

    <div class="add-failure-row">
      <select v-model="newFailureReason" class="field-input-sm">
        <option value="" disabled>Select reason...</option>
        <option v-for="opt in FAILURE_REASON_OPTIONS" :key="opt" :value="opt">
          {{ FAILURE_REASON_LABELS[opt] }}
        </option>
      </select>
      <button
        class="btn btn-primary btn-sm"
        :disabled="!newFailureReason"
        @click="addFailureReason"
      >
        + Add
      </button>
    </div>
  </div>
</template>

<style scoped>
.on-failure-editor {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.failure-card {
  border: 1px solid var(--p-content-border-color, #333);
  border-left: 3px solid var(--p-red-400, #f87171);
  border-radius: 6px;
  background: var(--p-content-background, #1a1a2e);
  overflow: hidden;
}

.failure-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.4rem 0.75rem;
  background: var(--p-form-field-background, #111);
  border-bottom: 1px solid var(--p-content-border-color, #333);
}

.failure-reason-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--p-text-color, #fff);
}

.failure-body {
  padding: 0.75rem;
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
  min-width: 5rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--p-text-color);
}

.field-input {
  flex: 1;
  padding: 0.45rem 0.6rem;
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 4px;
  background: var(--p-form-field-background, #111);
  color: var(--p-text-color, #fff);
  font-size: 0.85rem;
}

.sub-section {
  margin-top: 0.25rem;
}

.sub-label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--p-form-field-placeholder-color);
  margin-bottom: 0.35rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.add-failure-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.field-input-sm {
  padding: 0.3rem 0.45rem;
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 4px;
  background: var(--p-form-field-background, #111);
  color: var(--p-text-color, #fff);
  font-size: 0.8rem;
}
</style>
