<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: number
  placeholder?: string
}>(), {
  modelValue: undefined,
  placeholder: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

const text = ref(props.modelValue == null ? '' : String(props.modelValue))

watch(() => props.modelValue, (value) => {
  const parsed = parseFloat(text.value)
  if (Number.isNaN(parsed) || parsed !== value) {
    text.value = value == null ? '' : String(value)
  }
})

function onInput(event: Event) {
  const el = event.target as HTMLInputElement
  text.value = el.value
  const parsed = parseFloat(text.value)
  if (!Number.isNaN(parsed)) {
    emit('update:modelValue', parsed)
  }
}
</script>

<template>
  <input
    class="decimal-input"
    type="text"
    inputmode="decimal"
    :placeholder="placeholder"
    :value="text"
    @input="onInput"
  />
</template>

<style scoped>
.decimal-input {
  flex: 1;
  min-width: 0;
  padding: 0.35rem 0.5rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-form-field-background);
  color: var(--p-text-color);
  font-size: 0.85rem;
}
</style>
