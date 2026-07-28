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
      class="btn btn-primary btn-sm"
      @click="emit('add')"
    >
      + Add {{ sectionName }}
    </button>

    <button
      v-if="canDuplicate"
      class="btn btn-secondary btn-sm"
      @click="emit('duplicate')"
    >
      Duplicate {{ sectionName }}
    </button>

    <button
      v-if="canDelete"
      class="btn btn-ghost btn-sm"
      style="color: var(--p-red-500, #ef4444)"
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


</style>
