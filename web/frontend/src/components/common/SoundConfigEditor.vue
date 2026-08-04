<script setup lang="ts">
import AppCombobox from './AppCombobox.vue'

export interface SoundConfig {
  type: string
  volume: number
  pitch: number
  target: string
}

const props = defineProps<{
  modelValue: SoundConfig[]
  namePrefix: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: SoundConfig[]]
}>()

const SOUND_SUGGESTIONS = [
  'minecraft:entity.experience_orb.pickup', 'minecraft:entity.player.levelup',
  'minecraft:entity.player.attack.crit', 'minecraft:entity.player.attack.strong',
  'minecraft:entity.player.attack.sweep', 'minecraft:entity.player.attack.knockback',
  'minecraft:entity.player.attack.weak', 'minecraft:entity.arrow.shoot',
  'minecraft:entity.arrow.hit', 'minecraft:entity.firework_rocket.blast',
  'minecraft:entity.firework_rocket.twinkle', 'minecraft:entity.firework_rocket.large_blast',
  'minecraft:entity.firework_rocket.launch', 'minecraft:entity.generic.explode',
  'minecraft:entity.lightning_bolt.thunder', 'minecraft:entity.lightning_bolt.impact',
  'minecraft:entity.wither.spawn', 'minecraft:entity.wither.death',
  'minecraft:entity.wither.shoot', 'minecraft:entity.ender_dragon.death',
  'minecraft:entity.ender_dragon.growl', 'minecraft:entity.dragon_fireball.explode',
  'minecraft:item.trident.thunder', 'minecraft:item.trident.riptide_1',
  'minecraft:item.trident.riptide_2', 'minecraft:item.trident.riptide_3',
  'minecraft:block.anvil.land', 'minecraft:block.anvil.place',
  'minecraft:block.anvil.break', 'minecraft:block.anvil.destroy',
  'minecraft:block.anvil.fall', 'minecraft:block.anvil.hit',
  'minecraft:block.anvil.step', 'minecraft:block.anvil.use',
  'minecraft:block.brewing_stand.brew', 'minecraft:block.chest.open',
  'minecraft:block.chest.close', 'minecraft:block.ender_chest.open',
  'minecraft:block.ender_chest.close', 'minecraft:block.furnace.fire_crackle',
  'minecraft:block.note_block.bell', 'minecraft:block.note_block.chime',
  'minecraft:block.note_block.flute', 'minecraft:block.note_block.guitar',
  'minecraft:block.note_block.harp', 'minecraft:block.note_block.hat',
  'minecraft:block.note_block.basedrum', 'minecraft:block.note_block.snare',
  'minecraft:block.note_block.pling', 'minecraft:block.note_block.xylophone',
  'minecraft:block.note_block.iron_xylophone', 'minecraft:block.note_block.cow_bell',
  'minecraft:block.note_block.didgeridoo', 'minecraft:block.note_block.bit',
  'minecraft:block.note_block.banjo', 'minecraft:ui.button.click',
  'minecraft:ui.toast.in', 'minecraft:ui.toast.out', 'minecraft:ui.toast.challenge_complete',
]

function addSound() {
  emit('update:modelValue', [
    ...props.modelValue,
    { type: '', volume: 1, pitch: 1, target: 'self' },
  ])
}

function removeSound(index: number) {
  const copy = [...props.modelValue]
  copy.splice(index, 1)
  emit('update:modelValue', copy)
}

function updateSound(index: number, patch: Partial<SoundConfig>) {
  const copy = [...props.modelValue]
  copy[index] = { ...copy[index], ...patch }
  emit('update:modelValue', copy)
}
</script>

<template>
  <div class="sound-config-editor">
    <div
      v-for="(sound, sIdx) in modelValue"
      :key="sIdx"
      class="sound-card"
    >
      <div class="sound-type-row">
        <AppCombobox
          :model-value="sound.type"
          :suggestions="SOUND_SUGGESTIONS"
          :placeholder="placeholder || 'minecraft:entity_experience_orb_pickup'"
          :name="namePrefix + '-' + sIdx"
          @update:model-value="updateSound(sIdx, { type: $event })"
        />
        <button
          class="btn btn-ghost btn-sm"
          style="color: var(--p-red-500, #ef4444); flex-shrink: 0"
          @click="removeSound(sIdx)"
        >
          &times;
        </button>
      </div>
      <div class="sound-fields">
        <div class="sound-field">
          <label class="field-label-sm">Volume</label>
          <input
            class="field-input-sm"
            type="number"
            step="any"
            :value="sound.volume"
            @input="updateSound(sIdx, { volume: Number(($event.target as HTMLInputElement).value) })"
          />
        </div>
        <div class="sound-field">
          <label class="field-label-sm">Pitch</label>
          <input
            class="field-input-sm"
            type="number"
            step="any"
            :value="sound.pitch"
            @input="updateSound(sIdx, { pitch: Number(($event.target as HTMLInputElement).value) })"
          />
        </div>
        <div class="sound-field">
          <label class="field-label-sm">Target</label>
          <select
            class="field-input-sm"
            :value="sound.target"
            @change="updateSound(sIdx, { target: ($event.target as HTMLSelectElement).value })"
          >
            <option value="self">self</option>
            <option value="target">target</option>
          </select>
        </div>
      </div>
    </div>
    <button
      class="btn btn-primary btn-sm"
      @click="addSound"
    >
      + Add Sound
    </button>
  </div>
</template>

<style scoped>
.sound-config-editor {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sound-card {
  border: 1px solid var(--p-content-border-color, #333);
  border-left: 3px solid var(--p-cyan-400, #22d3ee);
  border-radius: 6px;
  padding: 0.6rem;
  background: var(--p-content-background, #1a1a2e);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sound-type-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.sound-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.sound-field {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.field-label-sm {
  font-size: 0.75rem;
  color: var(--p-form-field-placeholder-color, #888);
}

.field-input-sm {
  width: 5.5rem;
  padding: 0.3rem 0.45rem;
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 4px;
  background: var(--p-form-field-background, #111);
  color: var(--p-text-color, #fff);
  font-size: 0.8rem;
}
</style>
