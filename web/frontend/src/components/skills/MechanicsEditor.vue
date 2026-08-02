<script setup lang="ts">
import { computed } from 'vue'
import AppCombobox from '../common/AppCombobox.vue'
import FilterBuilder from '../common/FilterBuilder.vue'
import EvaluatorParameter from '../common/EvaluatorParameter.vue'
import { useRegistriesStore } from '../../stores/registries'

export interface FilterEntry {
  target?: string
  state?: string
  tool?: string
}

export interface MechanicEntry {
  type: string
  filters: FilterEntry[]
  params: { name: string; evaluator: { type: string; params: Record<string, any> } }[]
}

const props = defineProps<{
  modelValue: MechanicEntry[]
  tagSuggestions: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: MechanicEntry[]]
}>()

const registriesStore = useRegistriesStore()

const FALLBACK_MECHANICS = [
  'core:yield_multiplier', 'core:apply_status', 'core:chain_break', 'core:projectile',
  'core:modify_brew_time', 'core:modify_potion_duration', 'core:modify_furnace_output',
  'core:modify_attribute', 'core:knockback', 'core:shield_disable', 'core:offhand_strike',
  'core:set_cooldown', 'core:modify_attack_speed', 'core:ally_aura',
]

const MECHANIC_SUGGESTIONS = computed(() =>
  registriesStore.mechanicKeys.length > 0 ? registriesStore.mechanicKeys : FALLBACK_MECHANICS
)

const FALLBACK_PARAM_NAMES: Record<string, string[]> = {
  'core:yield_multiplier': ['yield_chance'],
  'core:chain_break': ['chain_limit', 'exhaustion'],
  'core:apply_status': ['effect', 'duration', 'amplifier'],
  'core:modify_attribute': ['attribute', 'amount', 'duration'],
  'core:modify_damage': ['multiplier'],
  'core:cancel_damage': ['chance'],
  'core:modify_furnace_output': ['multiplier'],
  'core:modify_brew_time': ['multiplier'],
  'core:modify_potion_duration': ['multiplier'],
  'core:modify_craft_output': ['multiplier'],
  'core:saturation_inject': ['saturation'],
  'core:aoe_effect': ['effect', 'radius', 'duration', 'amplifier'],
  'core:projectile': ['speed', 'damage'],
  'core:teleport': ['range'],
  'core:knockback': ['force', 'radius', 'vertical'],
  'core:shield_disable': ['ticks'],
  'core:offhand_strike': ['multiplier', 'reach'],
  'core:set_cooldown': ['material', 'ticks'],
  'core:modify_attack_speed': ['multiplier', 'duration'],
  'core:ally_aura': ['effect', 'radius', 'duration', 'amplifier'],
}

const MECHANIC_PARAM_NAMES = computed(() =>
  Object.keys(registriesStore.mechanicParams).length > 0
    ? registriesStore.mechanicParams
    : FALLBACK_PARAM_NAMES
)

function addMechanic() {
  emit('update:modelValue', [...props.modelValue, { type: '', filters: [], params: [] }])
}

function removeMechanic(mechIdx: number) {
  const copy = [...props.modelValue]
  copy.splice(mechIdx, 1)
  emit('update:modelValue', copy)
}

function updateMechanic(mechIdx: number, patch: Partial<MechanicEntry>) {
  const copy = [...props.modelValue]
  copy[mechIdx] = { ...copy[mechIdx], ...patch }
  emit('update:modelValue', copy)
}

function addMechanicParam(mechIdx: number) {
  const mech = props.modelValue[mechIdx]
  const copy = [...props.modelValue]
  copy[mechIdx] = {
    ...mech,
    params: [...mech.params, { name: '', evaluator: { type: 'constant', params: { value: 0 } } }],
  }
  emit('update:modelValue', copy)
}

function removeMechanicParam(mechIdx: number, paramIdx: number) {
  const mech = props.modelValue[mechIdx]
  const copy = [...props.modelValue]
  const paramsCopy = [...mech.params]
  paramsCopy.splice(paramIdx, 1)
  copy[mechIdx] = { ...mech, params: paramsCopy }
  emit('update:modelValue', copy)
}

function updateMechanicParamName(mechIdx: number, paramIdx: number, name: string) {
  const mech = props.modelValue[mechIdx]
  const copy = [...props.modelValue]
  const paramsCopy = [...mech.params]
  paramsCopy[paramIdx] = { ...paramsCopy[paramIdx], name }
  copy[mechIdx] = { ...mech, params: paramsCopy }
  emit('update:modelValue', copy)
}

function updateMechanicParamEvaluator(mechIdx: number, paramIdx: number, evaluator: MechanicEntry['params'][0]['evaluator']) {
  const mech = props.modelValue[mechIdx]
  const copy = [...props.modelValue]
  const paramsCopy = [...mech.params]
  paramsCopy[paramIdx] = { ...paramsCopy[paramIdx], evaluator }
  copy[mechIdx] = { ...mech, params: paramsCopy }
  emit('update:modelValue', copy)
}
</script>

<template>
  <div class="mechanics-editor">
    <div
      v-for="(mech, mIdx) in modelValue"
      :key="mIdx"
      class="mechanic-card"
    >
      <div class="mechanic-header">
        <span class="mechanic-title">Mechanic #{{ mIdx + 1 }}</span>
        <button
          class="btn btn-ghost btn-sm"
          style="color: var(--p-red-500, #ef4444)"
          @click="removeMechanic(mIdx)"
        >
          &times;
        </button>
      </div>

      <div class="mechanic-body">
        <div class="field-row">
          <label class="field-label">Type</label>
          <AppCombobox
            :model-value="mech.type"
            :suggestions="MECHANIC_SUGGESTIONS"
            placeholder="core:yield_multiplier"
            :name="'mech-' + mIdx"
            @update:model-value="updateMechanic(mIdx, { type: $event })"
          />
        </div>

        <div class="sub-section">
          <label class="sub-label">Filters</label>
          <FilterBuilder
            :model-value="mech.filters"
            :tag-suggestions="tagSuggestions"
            @update:model-value="updateMechanic(mIdx, { filters: $event })"
          />
        </div>

        <div class="sub-section">
          <label class="sub-label">Parameters</label>
          <div
            v-for="(param, pIdx) in mech.params"
            :key="pIdx"
            class="param-entry"
          >
            <div class="param-header">
              <AppCombobox
                :model-value="param.name"
                :suggestions="MECHANIC_PARAM_NAMES[mech.type] || []"
                placeholder="Parameter name"
                :name="'param-' + mIdx + '-' + pIdx"
                @update:model-value="updateMechanicParamName(mIdx, pIdx, $event)"
              />
              <button
                class="btn btn-ghost btn-sm"
                style="color: var(--p-red-500, #ef4444)"
                @click="removeMechanicParam(mIdx, pIdx)"
              >
                &times;
              </button>
            </div>
            <EvaluatorParameter
              :model-value="param.evaluator"
              :label="param.name || 'param'"
              :name="param.name || 'param'"
              @update:model-value="updateMechanicParamEvaluator(mIdx, pIdx, $event)"
            />
          </div>
          <button
            class="btn btn-primary btn-sm"
            @click="addMechanicParam(mIdx)"
          >
            + Add Parameter
          </button>
        </div>
      </div>
    </div>
    <button
      class="btn btn-primary btn-sm"
      @click="addMechanic"
    >
      + Add Mechanic
    </button>
  </div>
</template>

<style scoped>
.mechanics-editor {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.mechanic-card {
  border: 1px solid var(--p-content-border-color, #333);
  border-radius: 6px;
  background: var(--p-content-background, #1a1a2e);
  overflow: hidden;
}

.mechanic-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.4rem 0.75rem;
  background: var(--p-form-field-background, #111);
  border-bottom: 1px solid var(--p-content-border-color, #333);
}

.mechanic-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--p-text-color, #fff);
}

.mechanic-body {
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.field-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.field-label {
  min-width: 5rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--p-text-color);
}

.sub-section {
  margin-top: 0.25rem;
}

.sub-label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--p-form-field-placeholder-color);
  margin-bottom: 0.35rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.param-entry {
  margin-bottom: 0.5rem;
}

.param-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}
</style>
