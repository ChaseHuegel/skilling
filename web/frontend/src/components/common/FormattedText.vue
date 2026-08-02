<script setup lang="ts">
import { parseAmpersandCodes, segmentStyle, type FormattedSegment } from '../../utils/minecraftColors'

const props = defineProps<{
  text: string
}>()

function styled(seg: FormattedSegment): boolean {
  return Boolean(seg.color || seg.bold || seg.italic || seg.underline || seg.strikethrough)
}
</script>

<template>
  <!-- Render Minecraft color codes as styled spans; all user text is bound via
       interpolation (escaped by Vue), never via a raw HTML binding. -->
  <template v-for="(seg, i) in parseAmpersandCodes(props.text)" :key="i">
    <span v-if="styled(seg)" :style="segmentStyle(seg)">{{ seg.text }}</span>
    <template v-else>{{ seg.text }}</template>
  </template>
</template>
