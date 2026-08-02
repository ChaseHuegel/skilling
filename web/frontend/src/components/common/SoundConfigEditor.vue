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
  'minecraft:entity_experience_orb_pickup', 'minecraft:entity_player_levelup',
  'minecraft:entity_player_attack_crit', 'minecraft:entity_player_attack_strong',
  'minecraft:entity_player_attack_sweep', 'minecraft:entity_player_attack_knockback',
  'minecraft:entity_player_attack_weak', 'minecraft:entity_arrow_shoot',
  'minecraft:entity_arrow_hit', 'minecraft:entity_firework_rocket_blast',
  'minecraft:entity_firework_rocket_twinkle', 'minecraft:entity_firework_rocket_large_blast',
  'minecraft:entity_firework_rocket_launch', 'minecraft:entity_generic_explode',
  'minecraft:entity_lightning_bolt_thunder', 'minecraft:entity_lightning_bolt_impact',
  'minecraft:entity_wither_spawn', 'minecraft:entity_wither_death',
  'minecraft:entity_wither_shoot', 'minecraft:entity_ender_dragon_death',
  'minecraft:entity_ender_dragon_growl', 'minecraft:entity_ender_dragon_fireball_explode',
  'minecraft:item_trident_thunder', 'minecraft:item_trident_riptide_1',
  'minecraft:item_trident_riptide_2', 'minecraft:item_trident_riptide_3',
  'minecraft:block_anvil_land', 'minecraft:block_anvil_place',
  'minecraft:block_anvil_break', 'minecraft:block_anvil_destroy',
  'minecraft:block_anvil_fall', 'minecraft:block_anvil_hit',
  'minecraft:block_anvil_step', 'minecraft:block_anvil_use',
  'minecraft:block_brewing_stand_brew', 'minecraft:block_chest_open',
  'minecraft:block_chest_close', 'minecraft:block_ender_chest_open',
  'minecraft:block_ender_chest_close', 'minecraft:block_furnace_fire_crackle',
  'minecraft:block_note_block_bell', 'minecraft:block_note_block_chime',
  'minecraft:block_note_block_flute', 'minecraft:block_note_block_guitar',
  'minecraft:block_note_block_harpsichord', 'minecraft:block_note_block_hat',
  'minecraft:block_note_block_basedrum', 'minecraft:block_note_block_snare',
  'minecraft:block_note_block_pling', 'minecraft:block_note_block_xylophone',
  'minecraft:block_note_block_iron_xylophone', 'minecraft:block_note_block_cow_bell',
  'minecraft:block_note_block_didgeridoo', 'minecraft:block_note_block_bit',
  'minecraft:block_note_block_banjo', 'minecraft:ui_button_click',
  'minecraft:ui_toast_in', 'minecraft:ui_toast_out', 'minecraft:ui_toast_challenge_complete',
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
