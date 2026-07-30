<template>
  <div class="chest-grid-wrapper">
    <div class="chest-grid" :style="gridStyle">
      <ChestSlot
        v-for="slotIdx in totalSlots"
        :key="slotIdx - 1"
        :skill="getSlotSkill(slotIdx - 1)"
        :slot-index="slotIdx - 1"
        :page-index="pageIndex"
        :nav-role="getNavRole(slotIdx - 1)"
        :nav-icon="navIcon"
        @assign="onAssign"
        @swap="onSwap"
        @remove="onRemove"
        @nav-prev="$emit('prevPage')"
        @nav-next="$emit('nextPage')"
        @tooltip-show="onTooltipShow"
        @tooltip-hide="onTooltipHide"
      />
    </div>
    <SkillTooltip
      :skill="tooltipSkill"
      :visible="tooltipVisible"
      :x="tooltipX"
      :y="tooltipY"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, provide } from 'vue'
import ChestSlot, { type SlotSkill } from './ChestSlot.vue'
import SkillTooltip from './SkillTooltip.vue'

export interface ChestGridPage {
  label: string
  slots: Record<number, string>
  icon?: string
}

const dragState = reactive({ sourceSlot: null as number | null, dropReceived: false })
provide('dragState', dragState)

const props = defineProps<{
  rows: number
  page: ChestGridPage | null
  pageIndex: number
  totalPages: number
  skillMap: Record<string, SlotSkill>
}>()

const emit = defineEmits<{
  assign: [pageIndex: number, slot: number, skillId: string]
  swap: [pageIndex: number, fromSlot: number, toSlot: number]
  remove: [pageIndex: number, slot: number]
  prevPage: []
  nextPage: []
}>()

const totalSlots = computed(() => props.rows * 9)
const cols = 9

const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${cols}, 1fr)`,
  gridTemplateRows: `repeat(${props.rows}, 1fr)`,
}))

const prevSlot = computed(() => (props.rows - 1) * 9)
const indicatorSlot = computed(() => (props.rows - 1) * 9 + 4)
const nextSlot = computed(() => (props.rows - 1) * 9 + 8)

const navIcon = computed(() => props.page?.icon || 'minecraft:book')

function getNavRole(slotIndex: number): 'prev' | 'next' | 'indicator' | null {
  if (slotIndex === prevSlot.value) return 'prev'
  if (slotIndex === indicatorSlot.value) return 'indicator'
  if (slotIndex === nextSlot.value) return 'next'
  return null
}

function getSlotSkill(slotIndex: number): SlotSkill | null {
  if (!props.page) return null
  const navRole = getNavRole(slotIndex)
  if (navRole) return null
  const skillId = props.page.slots[slotIndex]
  if (!skillId) return null
  return props.skillMap[skillId] || null
}

function onAssign(pageIndex: number, slot: number, skillId: string) {
  emit('assign', pageIndex, slot, skillId)
}

function onSwap(pageIndex: number, fromSlot: number, toSlot: number) {
  emit('swap', pageIndex, fromSlot, toSlot)
}

function onRemove(pageIndex: number, slot: number) {
  emit('remove', pageIndex, slot)
}

const tooltipVisible = ref(false)
const tooltipSkill = ref<SlotSkill | null>(null)
const tooltipX = ref(0)
const tooltipY = ref(0)

function onTooltipShow(skill: SlotSkill, x: number, y: number) {
  tooltipSkill.value = skill
  tooltipX.value = x
  tooltipY.value = y
  tooltipVisible.value = true
}

function onTooltipHide() {
  tooltipVisible.value = false
  tooltipSkill.value = null
}
</script>

<style scoped>
.chest-grid-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  background: var(--p-content-background, #0d0d1a);
  border: 1px solid var(--p-content-border-color, #1a1a2e);
  border-radius: 8px;
  padding: 8px;
  width: 100%;
  max-width: 700px;
  box-sizing: border-box;
}

.chest-grid {
  display: grid;
  gap: 2px;
  width: 100%;
  aspect-ratio: 9 / 6;
}
</style>
