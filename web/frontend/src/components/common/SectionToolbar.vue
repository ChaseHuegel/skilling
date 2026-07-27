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
  border: 1px solid #444;
  border-radius: 4px;
  font-size: 0.85rem;
  cursor: pointer;
  color: #e0e0e0;
  background: #2a2a2a;
  transition: background 0.15s;
}

.toolbar-btn:hover {
  filter: brightness(1.3);
}

.btn-add {
  border-color: #2a7a3f;
  background: #1e3f2a;
}

.btn-add:hover {
  background: #2a5f3f;
}

.btn-duplicate {
  border-color: #2a5a7f;
  background: #1e2f4f;
}

.btn-duplicate:hover {
  background: #2a4a7f;
}

.btn-delete {
  border-color: #7a2a2a;
  background: #3f1e1e;
}

.btn-delete:hover {
  background: #5f2a2a;
}
</style>
