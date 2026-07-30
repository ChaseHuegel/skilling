<template>
  <div
    class="chest-slot"
    :class="{
      'slot-occupied': !!skill,
      'slot-drag-over': dragOver,
      'slot-empty': !skill && !dragOver,
    }"
    :data-slot-index="slotIndex"
    :draggable="!!skill"
    @dragstart="onDragStart"
    @dragenter.prevent="onDragEnter"
    @dragover.prevent="onDragOver"
    @dragleave="onDragLeave"
    @drop.prevent="onDrop"
    @dragend="onDragEnd"
    @mouseenter="showTooltip"
    @mouseleave="hideTooltip"
    @contextmenu.prevent="onRightClick"
  >
    <div class="slot-background">
      <MinecraftIcon v-if="skill" :material="skill.icon || 'minecraft:barrier'" :color="skill.color" :size="36" />
      <span v-else class="slot-placeholder"></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, inject } from 'vue'
import MinecraftIcon from '../common/MinecraftIcon.vue'

export interface SlotSkill {
  id: string
  displayName: string
  icon: string
  color: string
  abilities?: { name: string; unlockLevel: number }[]
}

interface DragState {
  sourceSlot: number | null
  dropReceived: boolean
}

const props = defineProps<{
  skill: SlotSkill | null
  slotIndex: number
  pageIndex: number
}>()

const emit = defineEmits<{
  assign: [pageIndex: number, slot: number, skillId: string]
  swap: [pageIndex: number, fromSlot: number, toSlot: number]
  remove: [pageIndex: number, slot: number]
  tooltipShow: [skill: SlotSkill, x: number, y: number]
  tooltipHide: []
}>()

const dragState = inject('dragState') as DragState
const dragOver = ref(false)

function onDragStart(e: DragEvent) {
  if (!props.skill) return
  dragState.sourceSlot = props.slotIndex
  dragState.dropReceived = false
  e.dataTransfer?.setData('text/plain', props.skill.id)
  e.dataTransfer?.setData('application/x-slot-index', String(props.slotIndex))
  e.dataTransfer!.effectAllowed = 'move'
}

function onDragEnter() {
  dragOver.value = true
}

function onDragOver(e: DragEvent) {
  e.dataTransfer!.dropEffect = 'move'
}

function onDragLeave() {
  dragOver.value = false
}

function onDrop(e: DragEvent) {
  dragOver.value = false
  const skillId = e.dataTransfer?.getData('text/plain')
  if (!skillId) return

  const sourceSlot = e.dataTransfer?.getData('application/x-slot-index')
  if (sourceSlot) {
    const fromSlot = parseInt(sourceSlot, 10)
    if (fromSlot === props.slotIndex) return
    dragState.dropReceived = true
    emit('swap', props.pageIndex, fromSlot, props.slotIndex)
  } else {
    dragState.dropReceived = true
    emit('assign', props.pageIndex, props.slotIndex, skillId)
  }
}

function onDragEnd() {
  dragOver.value = false
  if (dragState.sourceSlot === props.slotIndex && !dragState.dropReceived && props.skill) {
    emit('remove', props.pageIndex, props.slotIndex)
  }
  if (dragState.sourceSlot === props.slotIndex) {
    dragState.sourceSlot = null
  }
}

function onRightClick() {
  if (props.skill) {
    emit('remove', props.pageIndex, props.slotIndex)
  }
}

function showTooltip(e: MouseEvent) {
  if (!props.skill) return
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  emit('tooltipShow', props.skill, rect.left + rect.width / 2, rect.top)
}

function hideTooltip() {
  emit('tooltipHide')
}
</script>

<style scoped>
.chest-slot {
  width: 100%;
  aspect-ratio: 1;
  background: #1a1a2e;
  border: 2px solid #2a2a3e;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.15s, background 0.15s;
  position: relative;
}

.chest-slot:hover {
  border-color: #555;
}

.slot-empty {
  border-style: dashed;
  border-color: #333;
}

.slot-occupied {
  border-color: #3a3a5e;
  background: #16162a;
}

.slot-drag-over {
  border-color: #55ff55;
  background: rgba(85, 255, 85, 0.08);
}

.slot-background {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.slot-placeholder {
  width: 16px;
  height: 16px;
  border: 1px dashed #333;
  border-radius: 2px;
  opacity: 0.4;
}
</style>
