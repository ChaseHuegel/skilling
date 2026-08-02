<template>
  <div
    ref="tooltipEl"
    class="skill-tooltip"
    :class="{ 'tooltip-flip': flipped }"
    :style="positionStyle"
    v-if="visible && skill"
  >
    <div class="tooltip-title" v-html="renderedName"></div>
    <div class="tooltip-separator">&mdash;&mdash;&mdash;&mdash;&mdash;&mdash;&mdash;&mdash;</div>
    <div class="tooltip-abilities" v-if="abilityLines.length > 0">
      <div
        v-for="(line, i) in abilityLines"
        :key="'a' + i"
        class="tooltip-ability-line"
        v-html="line"
      ></div>
    </div>
    <div class="tooltip-footer" v-html="renderedId"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'
import { parseAmpersandCodes, renderFormattedText } from '../../utils/minecraftColors'

interface SkillTooltipData {
  displayName: string
  id: string
  abilities?: { name: string }[]
}

const props = defineProps<{
  skill: SkillTooltipData | null
  visible: boolean
  x?: number
  y?: number
}>()

const tooltipEl = ref<HTMLElement | null>(null)
const flipped = ref(false)

const TOOLTIP_MARGIN = 8

const positionStyle = computed(() => {
  if (props.x === undefined || props.y === undefined) return {}
  return {
    left: `${props.x}px`,
    top: `${props.y}px`,
  }
})

watch(() => props.visible, async (visible) => {
  if (visible && tooltipEl.value) {
    await nextTick()
    const rect = tooltipEl.value.getBoundingClientRect()
    if (rect.top < TOOLTIP_MARGIN) {
      flipped.value = true
    } else {
      flipped.value = false
    }
  } else {
    flipped.value = false
  }
})

const renderedName = computed(() =>
  renderFormattedText(parseAmpersandCodes(props.skill?.displayName || ''))
)

const renderedId = computed(() =>
  renderFormattedText(parseAmpersandCodes(`&8${props.skill?.id || ''}`))
)

const abilityLines = computed(() => {
  if (!props.skill?.abilities) return []
  return props.skill.abilities.map(ab =>
    renderFormattedText(parseAmpersandCodes(`&a${ab.name}`))
  )
})
</script>

<style scoped>
.skill-tooltip {
  position: fixed;
  z-index: 9999;
  max-width: 280px;
  padding: 8px 10px;
  background: rgba(0, 0, 0, 0.85);
  border: 1px solid #2a2a2a;
  border-radius: 4px;
  word-wrap: break-word;
  pointer-events: none;
  transform: translate(-50%, -100%);
  margin-top: -8px;
}

.skill-tooltip.tooltip-flip {
  transform: translate(-50%, 0);
  margin-top: 8px;
}

.tooltip-title {
  font-size: 0.9rem;
  font-weight: 700;
  margin-bottom: 2px;
}

.tooltip-separator {
  color: #aaaaaa;
  font-size: 0.7rem;
  margin: 2px 0;
  letter-spacing: -1px;
}

.tooltip-ability-line {
  font-size: 0.8rem;
  line-height: 1.3;
  color: #ffffff;
}

.tooltip-footer {
  font-size: 0.75rem;
  margin-top: 4px;
}
</style>
