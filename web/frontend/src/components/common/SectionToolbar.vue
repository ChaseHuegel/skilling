<script setup lang="ts">
defineProps<{
  sectionName: string
}>()

const emit = defineEmits<{
  add: []
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
