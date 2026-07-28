<script setup lang="ts">
import { computed } from 'vue'

interface IdentityFields {
  id: string
  displayName: string
  maxLevel: number
}

const props = defineProps<{
  modelValue: IdentityFields
  readonly?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: IdentityFields]
}>()

const idPlaceholder = computed(() => {
  if (!props.modelValue.displayName || props.modelValue.id) return ''
  return props.modelValue.displayName.toLowerCase().replace(/\s+/g, '_')
})

function setField<K extends keyof IdentityFields>(key: K, val: IdentityFields[K]) {
  emit('update:modelValue', { ...props.modelValue, [key]: val })
}
</script>

<template>
  <div class="identity-section">
    <div class="field-row">
      <label class="field-label">ID</label>
      <input
        class="field-input"
        type="text"
        required
        :placeholder="idPlaceholder || 'e.g. mining, woodcutting'"
        :readonly="readonly"
        :value="modelValue.id"
        @input="setField('id', ($event.target as HTMLInputElement).value)"
      />
    </div>

    <div class="field-row">
      <label class="field-label">Display Name</label>
      <input
        class="field-input"
        type="text"
        placeholder="e.g. Mining, Woodcutting"
        :value="modelValue.displayName"
        @input="setField('displayName', ($event.target as HTMLInputElement).value)"
      />
    </div>

    <div class="field-row">
      <label class="field-label">Max Level</label>
      <input
        class="field-input"
        type="number"
        min="1"
        max="1000"
        :value="modelValue.maxLevel"
        @input="setField('maxLevel', Number(($event.target as HTMLInputElement).value))"
      />
    </div>
  </div>
</template>

<style scoped>
.identity-section {
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
  min-width: 7rem;
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

.field-input:read-only {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
