<script setup lang="ts">
defineProps<{
  sectionName: string
  canDelete: boolean
  canDuplicate: boolean
}>()

const emit = defineEmits<{
  add: []
  duplicate: []
  delete: []
}>()
</script>

<template>
  <div class="section-toolbar">
    <button
      class="toolbar-btn btn-add"
      @click="emit('add')"
    >
      + Add {{ sectionName }}
    </button>

    <button
      v-if="canDuplicate"
      class="toolbar-btn btn-duplicate"
      @click="emit('duplicate')"
    >
      Duplicate {{ sectionName }}
    </button>

    <button
      v-if="canDelete"
      class="toolbar-btn btn-delete"
      @click="emit('delete')"
    >
      Delete {{ sectionName }}
    </button>
  </div>
</template>

<style scoped>
.section-toolbar {
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem 0;
  flex-wrap: wrap;
}

.toolbar-btn {
  padding: 0.4rem 0.75rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  font-size: 0.85rem;
  cursor: pointer;
  color: var(--p-text-color);
  background: var(--p-content-background);
  transition: background 0.15s;
}

.toolbar-btn:hover {
  filter: brightness(1.2);
}

.btn-add {
  border-color: var(--p-primary-color);
  color: var(--p-primary-color);
}

.btn-duplicate {
  border-color: var(--p-primary-color);
  color: var(--p-primary-color);
  background: color-mix(in srgb, var(--p-primary-color) 10%, transparent);
}

.btn-duplicate:hover {
  filter: brightness(1.2);
}

.btn-delete {
  border-color: var(--p-red-600, #dc2626);
  color: var(--p-red-600, #dc2626);
  background: color-mix(in srgb, var(--p-red-600, #dc2626) 10%, transparent);
}

.btn-delete:hover {
  filter: brightness(1.2);
}
</style>
