<template>
  <div class="chest-grid-wrapper">
    <div class="chest-grid" :style="gridStyle">
      <ChestSlot
        v-for="slotIdx in totalSlots"
        :key="slotIdx - 1"
        :skill="getSlotSkill(slotIdx - 1)"
        :slot-index="slotIdx - 1"
        :page-index="pageIndex"
        @assign="onAssign"
        @swap="onSwap"
        @remove="onRemove"
        @tooltip-show="onTooltipShow"
        @tooltip-hide="onTooltipHide"
      />
    </div>
    <div class="chest-navbar">
      <button class="nav-btn" :disabled="pageIndex <= 0" @click="$emit('prevPage')">
        &#9664;
      </button>
      <span class="nav-label" v-html="renderedPageLabel"></span>
      <button class="nav-btn" :disabled="pageIndex >= totalPages - 1" @click="$emit('nextPage')">
        &#9654;
      </button>
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
import { parseAmpersandCodes, renderFormattedText } from '../../utils/minecraftColors'

export interface ChestGridPage {
  label: string
  slots: Record<number, string>
}

const dragState = reactive({ sourceSlot: null as number | null, dropReceived: false })
provide('dragState', dragState)

const props = defineProps<{
  title: string
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

const renderedPageLabel = computed(() => {
  if (!props.page) return ''
  return renderFormattedText(parseAmpersandCodes(props.page.label))
})

function getSlotSkill(slotIndex: number): SlotSkill | null {
  if (!props.page) return null
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

.chest-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 4px 0;
  gap: 8px;
}

.nav-btn {
  background: var(--p-content-background, #1a1a2e);
  border: 1px solid var(--p-content-border-color, #333);
  color: var(--p-text-color, #ccc);
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
  transition: background 0.15s;
}

.nav-btn:hover:not(:disabled) {
  background: var(--p-content-hover-background, #2a2a4e);
}

.nav-btn:disabled {
  opacity: 0.3;
  cursor: default;
}

.nav-label {
  flex: 1;
  text-align: center;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--p-text-color, #ccc);
}
</style>
