<script setup lang="ts">
import { ref, computed } from 'vue'
import SectionToolbar from '../common/SectionToolbar.vue'
import FilterBuilder from '../common/FilterBuilder.vue'
import EvaluatorParameter from '../common/EvaluatorParameter.vue'
import AppCombobox from '../common/AppCombobox.vue'
import { useDragReorder } from '../../composables/useDragReorder'
import { useRegistriesStore } from '../../stores/registries'

const registriesStore = useRegistriesStore()

const PARTICLE_SUGGESTIONS = [
  'minecraft:flame', 'minecraft:smoke', 'minecraft:large_smoke', 'minecraft:campfire_cosy_smoke',
  'minecraft:campfire_signal_smoke', 'minecraft:cloud', 'minecraft:crit', 'minecraft:enchanted_hit',
  'minecraft:enchant', 'minecraft:dragon_breath', 'minecraft:end_rod', 'minecraft:explosion',
  'minecraft:explosion_emitter', 'minecraft:firework', 'minecraft:glow', 'minecraft:glow_squid_ink',
  'minecraft:heart', 'minecraft:happy_villager', 'minecraft:angry_villager', 'minecraft:instant_effect',
  'minecraft:effect', 'minecraft:item_slime', 'minecraft:item_snowball', 'minecraft:lava',
  'minecraft:dripping_lava', 'minecraft:falling_lava', 'minecraft:landing_lava', 'minecraft:note',
  'minecraft:poof', 'minecraft:portal', 'minecraft:rain', 'minecraft:splash',
  'minecraft:sweep_attack', 'minecraft:totem_of_undying', 'minecraft:witch',
  'minecraft:dripping_water', 'minecraft:falling_water', 'minecraft:bubble', 'minecraft:bubble_pop',
  'minecraft:fishing', 'minecraft:nautilus', 'minecraft:sonic_boom', 'minecraft:sculk_soul',
  'minecraft:sculk_charge', 'minecraft:sculk_charge_pop', 'minecraft:shriek', 'minecraft:trail',
  'minecraft:dust', 'minecraft:dust_color_transition', 'minecraft:vibration',
]

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

const SLOT_SUGGESTIONS = ['HAND', 'OFF_HAND', 'FEET', 'LEGS', 'CHEST', 'HEAD']

const MECHANIC_SUGGESTIONS = computed(() =>
  registriesStore.mechanicKeys.length > 0 ? registriesStore.mechanicKeys : FALLBACK_MECHANICS
)

const FALLBACK_MECHANICS = [
  'core:yield_multiplier', 'core:apply_status', 'core:chain_break', 'core:projectile',
  'core:modify_brew_time', 'core:modify_potion_duration', 'core:modify_furnace_output',
  'core:modify_attribute',
]

const MECHANIC_PARAM_NAMES = computed(() =>
  Object.keys(registriesStore.mechanicParams).length > 0
    ? registriesStore.mechanicParams
    : FALLBACK_PARAM_NAMES
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
}

interface FilterEntry {
  target?: string
  state?: string
  tool?: string
}

interface RequirementItem {
  action: string
  tag: string
  slot: string
  amount: number
  itemCooldown: number
}

interface MechanicEntry {
  type: string
  filters: FilterEntry[]
  params: { name: string; evaluator: { type: string; params: Record<string, any> } }[]
}

interface ParticleConfig {
  type: string
  count: number
  target: string
  offsetX: number
  offsetY: number
  offsetZ: number
  speed: number
}

interface SoundConfig {
  type: string
  volume: number
  pitch: number
  target: string
}

interface FailureFeedback {
  actionBar: string
  sounds: SoundConfig[]
}

interface OnFailure {
  reasons: Record<string, FailureFeedback>
}

interface Ability {
  id: string
  displayName: string
  unlockLevel: number
  lore: string[]
  requirements: {
    cooldown: number
    state: string[]
    items: RequirementItem[]
  }
  mechanics: MechanicEntry[]
  onFailure?: OnFailure
  feedback: {
    actionBar: boolean
    chat: boolean
    message: string
    particles: ParticleConfig[]
    sounds: SoundConfig[]
  }
}

const props = defineProps<{
  modelValue: Ability[]
  tagSuggestions: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Ability[]]
}>()

const abilities = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})
const { dragIndex, onDragStart, onDragOver, onDragEnd } = useDragReorder(abilities)

const STATE_OPTIONS = ['is_sneaking', 'is_sprinting', 'is_in_water', 'is_on_ground'] as const

const expanded = ref<Record<number, boolean>>({})
const pendingRemoveAbility = ref<number | null>(null)
const newFailureReason = ref<Record<number, string>>({})
const loreDragIndex = ref<string | null>(null)
const pendingState = ref<Record<number, string>>({})

function toggleExpand(idx: number) {
  expanded.value[idx] = !expanded.value[idx]
}

function isAbilityActive(ability: Ability): boolean {
  const r = ability.requirements
  return r.cooldown > 0 || r.state.length > 0 || r.items.length > 0
}

function emptyAbility(): Ability {
  return {
    id: '',
    displayName: '',
    unlockLevel: 0,
    lore: [],
    requirements: {
      cooldown: 0,
      state: [],
      items: [],
    },
    mechanics: [],
    onFailure: { reasons: {} },
    feedback: {
      actionBar: false,
      chat: false,
      message: '',
      particles: [],
      sounds: [],
    },
  }
}

function updateAbility(index: number, patch: Partial<Ability>) {
  const copy = [...props.modelValue]
  copy[index] = { ...copy[index], ...patch }
  emit('update:modelValue', copy)
}

function updateRequirement(index: number, patch: Partial<Ability['requirements']>) {
  const ab = props.modelValue[index]
  updateAbility(index, { requirements: { ...ab.requirements, ...patch } })
}

function updateFeedback(index: number, patch: Partial<Ability['feedback']>) {
  const ab = props.modelValue[index]
  updateAbility(index, { feedback: { ...ab.feedback, ...patch } })
}

function confirmRemoveAbility(index: number) {
  pendingRemoveAbility.value = index
}

function executeRemoveAbility() {
  if (pendingRemoveAbility.value === null) return
  const copy = [...props.modelValue]
  copy.splice(pendingRemoveAbility.value, 1)
  emit('update:modelValue', copy)
  pendingRemoveAbility.value = null
}

function removeAbility(index: number) {
  const copy = [...props.modelValue]
  copy.splice(index, 1)
  emit('update:modelValue', copy)
}

function addAbility() {
  emit('update:modelValue', [...props.modelValue, emptyAbility()])
}

function duplicateAbility(index: number) {
  const source = props.modelValue[index]
  const cloned: Ability = {
    ...JSON.parse(JSON.stringify(source)),
    id: source.id ? source.id + '_copy' : '',
  }
  const copy = [...props.modelValue]
  copy.splice(index + 1, 0, cloned)
  emit('update:modelValue', copy)
}

function addLoreLine(index: number) {
  const ab = props.modelValue[index]
  updateAbility(index, { lore: [...ab.lore, ''] })
}

function onLoreDragStart(index: number, lineIdx: number) {
  loreDragIndex.value = index + '-' + lineIdx
}

function onLoreDragOver(e: DragEvent, index: number, lineIdx: number) {
  e.preventDefault()
  const key = index + '-' + lineIdx
  if (loreDragIndex.value === null || loreDragIndex.value === key) return
  const parts = loreDragIndex.value.split('-')
  const fromIdx = parseInt(parts[1], 10)
  const toIdx = lineIdx
  const ab = props.modelValue[index]
  const copy = [...ab.lore]
  const [removed] = copy.splice(fromIdx, 1)
  copy.splice(toIdx, 0, removed)
  updateAbility(index, { lore: copy })
  loreDragIndex.value = key
}

function onLoreDragEnd() {
  loreDragIndex.value = null
}

function removeLoreLine(index: number, lineIdx: number) {
  const ab = props.modelValue[index]
  const copy = [...ab.lore]
  copy.splice(lineIdx, 1)
  updateAbility(index, { lore: copy })
}

function loreSuggestions(index: number): string[] {
  const ab = props.modelValue[index]
  const params = new Set<string>()
  for (const mech of ab.mechanics || []) {
    for (const p of mech.params || []) {
      if (p.name) params.add('{' + p.name + '}')
    }
  }
  return Array.from(params)
}

function updateLoreLine(index: number, lineIdx: number, val: string) {
  const ab = props.modelValue[index]
  const copy = [...ab.lore]
  copy[lineIdx] = val
  updateAbility(index, { lore: copy })
}

function addState(index: number, state: string) {
  const trimmed = state.trim()
  if (!trimmed) return
  const ab = props.modelValue[index]
  if (ab.requirements.state.includes(trimmed)) return
  updateRequirement(index, { state: [...ab.requirements.state, trimmed] })
  pendingState.value[index] = ''
}

function removeState(index: number, sIdx: number) {
  const ab = props.modelValue[index]
  const copy = [...ab.requirements.state]
  copy.splice(sIdx, 1)
  updateRequirement(index, { state: copy })
}

function addItem(index: number) {
  const ab = props.modelValue[index]
  updateRequirement(index, {
    items: [
      ...ab.requirements.items,
      { action: 'possession', tag: '', slot: '', amount: 1, itemCooldown: 0 },
    ],
  })
}

function removeItem(index: number, itemIdx: number) {
  const ab = props.modelValue[index]
  const copy = [...ab.requirements.items]
  copy.splice(itemIdx, 1)
  updateRequirement(index, { items: copy })
}

function updateItem(index: number, itemIdx: number, patch: Partial<RequirementItem>) {
  const ab = props.modelValue[index]
  const copy = [...ab.requirements.items]
  copy[itemIdx] = { ...copy[itemIdx], ...patch }
  updateRequirement(index, { items: copy })
}

function addMechanic(index: number) {
  const ab = props.modelValue[index]
  updateAbility(index, {
    mechanics: [
      ...ab.mechanics,
      { type: '', filters: [], params: [] },
    ],
  })
}

function removeMechanic(index: number, mechIdx: number) {
  const ab = props.modelValue[index]
  const copy = [...ab.mechanics]
  copy.splice(mechIdx, 1)
  updateAbility(index, { mechanics: copy })
}

function updateMechanic(index: number, mechIdx: number, patch: Partial<MechanicEntry>) {
  const ab = props.modelValue[index]
  const copy = [...ab.mechanics]
  copy[mechIdx] = { ...copy[mechIdx], ...patch }
  updateAbility(index, { mechanics: copy })
}

function addMechanicParam(index: number, mechIdx: number) {
  const ab = props.modelValue[index]
  const mech = ab.mechanics[mechIdx]
  const copy = [...ab.mechanics]
  copy[mechIdx] = {
    ...mech,
    params: [...mech.params, { name: '', evaluator: { type: 'constant', params: { value: 0 } } }],
  }
  updateAbility(index, { mechanics: copy })
}

function removeMechanicParam(index: number, mechIdx: number, paramIdx: number) {
  const ab = props.modelValue[index]
  const mech = ab.mechanics[mechIdx]
  const copy = [...ab.mechanics]
  const paramsCopy = [...mech.params]
  paramsCopy.splice(paramIdx, 1)
  copy[mechIdx] = { ...mech, params: paramsCopy }
  updateAbility(index, { mechanics: copy })
}

function updateMechanicParamName(index: number, mechIdx: number, paramIdx: number, name: string) {
  const ab = props.modelValue[index]
  const mech = ab.mechanics[mechIdx]
  const copy = [...ab.mechanics]
  const paramsCopy = [...mech.params]
  paramsCopy[paramIdx] = { ...paramsCopy[paramIdx], name }
  copy[mechIdx] = { ...mech, params: paramsCopy }
  updateAbility(index, { mechanics: copy })
}

function updateMechanicParamEvaluator(index: number, mechIdx: number, paramIdx: number, evaluator: MechanicEntry['params'][0]['evaluator']) {
  const ab = props.modelValue[index]
  const mech = ab.mechanics[mechIdx]
  const copy = [...ab.mechanics]
  const paramsCopy = [...mech.params]
  paramsCopy[paramIdx] = { ...paramsCopy[paramIdx], evaluator }
  copy[mechIdx] = { ...mech, params: paramsCopy }
  updateAbility(index, { mechanics: copy })
}

function addParticle(index: number) {
  const ab = props.modelValue[index]
  updateFeedback(index, {
    particles: [
      ...ab.feedback.particles,
      { type: '', count: 1, target: 'self', offsetX: 0, offsetY: 0, offsetZ: 0, speed: 0 },
    ],
  })
}

function removeParticle(index: number, pIdx: number) {
  const ab = props.modelValue[index]
  const copy = [...ab.feedback.particles]
  copy.splice(pIdx, 1)
  updateFeedback(index, { particles: copy })
}

function updateParticle(index: number, pIdx: number, patch: Partial<ParticleConfig>) {
  const ab = props.modelValue[index]
  const copy = [...ab.feedback.particles]
  copy[pIdx] = { ...copy[pIdx], ...patch }
  updateFeedback(index, { particles: copy })
}

function addSound(index: number) {
  const ab = props.modelValue[index]
  updateFeedback(index, {
    sounds: [
      ...ab.feedback.sounds,
      { type: '', volume: 1, pitch: 1, target: 'self' },
    ],
  })
}

function removeSound(index: number, sIdx: number) {
  const ab = props.modelValue[index]
  const copy = [...ab.feedback.sounds]
  copy.splice(sIdx, 1)
  updateFeedback(index, { sounds: copy })
}

function updateSound(index: number, sIdx: number, patch: Partial<SoundConfig>) {
  const ab = props.modelValue[index]
  const copy = [...ab.feedback.sounds]
  copy[sIdx] = { ...copy[sIdx], ...patch }
  updateFeedback(index, { sounds: copy })
}

const FAILURE_REASON_OPTIONS = ['cooldown', 'missing_item', 'missing_state']

function getOnFailure(index: number): OnFailure {
  return props.modelValue[index].onFailure || { reasons: {} }
}

function updateOnFailure(index: number, patch: Partial<OnFailure>) {
  const ab = props.modelValue[index]
  updateAbility(index, { onFailure: { ...getOnFailure(index), ...patch } })
}

function setFailureReason(index: number, reason: string, fb: FailureFeedback) {
  const of = getOnFailure(index)
  const reasons = { ...of.reasons, [reason]: fb }
  updateOnFailure(index, { reasons })
}

function removeFailureReason(index: number, reason: string) {
  const of = getOnFailure(index)
  const reasons = { ...of.reasons }
  delete reasons[reason]
  updateOnFailure(index, { reasons })
}

function updateFailureActionBar(index: number, reason: string, val: string) {
  const of = getOnFailure(index)
  const fb = of.reasons[reason] || { actionBar: '', sounds: [] }
  setFailureReason(index, reason, { ...fb, actionBar: val })
}

function addFailureSound(index: number, reason: string) {
  const of = getOnFailure(index)
  const fb = of.reasons[reason] || { actionBar: '', sounds: [] }
  setFailureReason(index, reason, {
    ...fb,
    sounds: [...fb.sounds, { type: '', volume: 1, pitch: 1, target: 'self' }],
  })
}

function removeFailureSound(index: number, reason: string, sIdx: number) {
  const of = getOnFailure(index)
  const fb = of.reasons[reason]
  if (!fb) return
  const copy = [...fb.sounds]
  copy.splice(sIdx, 1)
  setFailureReason(index, reason, { ...fb, sounds: copy })
}

function updateFailureSound(index: number, reason: string, sIdx: number, patch: Partial<SoundConfig>) {
  const of = getOnFailure(index)
  const fb = of.reasons[reason]
  if (!fb) return
  const copy = [...fb.sounds]
  copy[sIdx] = { ...copy[sIdx], ...patch }
  setFailureReason(index, reason, { ...fb, sounds: copy })
}

function addFailureReason(index: number) {
  const reason = newFailureReason.value[index]
  if (!reason) return
  const of = getOnFailure(index)
  if (of.reasons[reason]) return
  setFailureReason(index, reason, { actionBar: '', sounds: [] })
  newFailureReason.value[index] = ''
}

function failurePlaceholder(reason: string): string {
  if (reason === 'cooldown') return "&cCooling down: {time}s";
  if (reason === 'missing_item') return "&cRequires {amount}x {item}";
  return '&c' + reason + ' message...';
}

const FAILURE_REASON_LABELS: Record<string, string> = {
  cooldown: 'Cooldown',
  missing_item: 'Missing Item',
  missing_state: 'Missing State',
}
</script>

<template>
  <div class="abilities-section">
    <SectionToolbar
      section-name="Ability"
      :can-delete="false"
      :can-duplicate="false"
      @add="addAbility"
    />

    <div v-if="modelValue.length === 0" class="empty-warning">
      No abilities defined. Add some to give players unlockable perks.
    </div>

    <div
      v-for="(ability, idx) in modelValue"
      :key="idx"
      :id="'ability-' + ability.id"
      class="ability-card"
      :class="{ 'drag-over': dragIndex !== null && dragIndex !== idx }"
      draggable="true"
      @dragstart="onDragStart(idx)"
      @dragover="onDragOver($event, idx)"
      @dragend="onDragEnd"
    >
      <div
        class="ability-header"
        @click="toggleExpand(idx)"
      >
        <span class="drag-handle" title="Drag to reorder" @click.stop>&#8801;</span>
        <span class="ability-title">
          {{ ability.id || 'Unnamed Ability' }}
        </span>
        <span class="ability-unlock-level">Lv.{{ ability.unlockLevel }}</span>
        <span class="editor-ability-type-badge" :class="isAbilityActive(ability) ? 'badge-active' : 'badge-passive'">
          {{ isAbilityActive(ability) ? 'Active' : 'Passive' }}
        </span>
        <span class="expand-toggle">{{ expanded[idx] ? '▼' : '▶' }}</span>
        <button
          class="btn btn-ghost btn-sm"
          title="Duplicate"
          @click.stop="duplicateAbility(idx)"
        >
          <svg viewBox="0 0 16 16" width="14" height="14" fill="none">
            <rect x="3" y="5" width="9" height="10" rx="1" stroke="currentColor" stroke-width="1.2" />
            <path d="M5 5V3a1 1 0 011-1h6a1 1 0 011 1v7a1 1 0 01-1 1h-1" stroke="currentColor" stroke-width="1.2" />
          </svg>
        </button>
        <button
          class="btn btn-ghost btn-sm"
          style="color: var(--p-red-500, #ef4444)"
          @click.stop="confirmRemoveAbility(idx)"
        >
          &times;
        </button>
      </div>

      <div
        v-if="expanded[idx]"
        class="ability-body"
      >
        <div class="field-row">
          <label class="field-label">ID</label>
          <input
            class="field-input"
            type="text"
            required
            placeholder="e.g. vein_miner"
            :value="ability.id"
            @input="updateAbility(idx, { id: ($event.target as HTMLInputElement).value })"
          />
        </div>

        <div class="field-row">
          <label class="field-label">Display Name</label>
          <input
            class="field-input"
            type="text"
            placeholder="e.g. Vein Miner"
            :value="ability.displayName"
            @input="updateAbility(idx, { displayName: ($event.target as HTMLInputElement).value })"
          />
        </div>

        <div class="field-row">
          <label class="field-label">Unlock Level</label>
          <input
            class="field-input"
            type="number"
            step="any"
            min="0"
            :value="ability.unlockLevel"
            @input="updateAbility(idx, { unlockLevel: Number(($event.target as HTMLInputElement).value) })"
          />
        </div>

        <div class="section-block">
          <label class="section-label">Lore Lines</label>
          <div
            v-for="(line, lIdx) in ability.lore"
            :key="lIdx"
            class="lore-line-row"
            :class="{ 'lore-drag-over': loreDragIndex === idx + '-' + lIdx }"
            draggable="true"
            @dragstart="onLoreDragStart(idx, lIdx)"
            @dragover="onLoreDragOver($event, idx, lIdx)"
            @dragend="onLoreDragEnd"
          >
            <span class="lore-drag-handle" title="Drag to reorder">&#8801;</span>
            <AppCombobox
              :model-value="line"
              :suggestions="loreSuggestions(idx)"
              placeholder="{chain_break} / &a green / &l bold / &o italic"
              :name="'lore-' + idx + '-' + lIdx"
              @update:model-value="updateLoreLine(idx, lIdx, $event)"
            />
            <button
              class="btn btn-ghost btn-sm"
              style="color: var(--p-red-500, #ef4444)"
              @click="removeLoreLine(idx, lIdx)"
            >
              &times;
            </button>
          </div>
          <button
            class="btn btn-primary btn-sm"
            @click="addLoreLine(idx)"
          >
            + Add Lore Line
          </button>
        </div>

        <div class="section-block">
          <label class="section-label">Requirements</label>

          <div class="field-row">
            <label class="field-label">Cooldown (s)</label>
            <input
              class="field-input"
              type="number"
              step="any"
              min="0"
              :value="ability.requirements.cooldown"
              @input="updateRequirement(idx, { cooldown: Number(($event.target as HTMLInputElement).value) })"
            />
          </div>

          <div class="states-group">
            <label class="field-label">States</label>
            <div class="states-chips">
              <span v-for="(state, sIdx) in ability.requirements.state" :key="sIdx" class="state-chip">
                {{ state }}
                <button class="btn btn-ghost btn-sm chip-remove" @click="removeState(idx, sIdx)">&times;</button>
              </span>
            </div>
            <div class="state-add-row">
              <AppCombobox
                :model-value="pendingState[idx] || ''"
                :suggestions="STATE_OPTIONS as unknown as string[]"
                placeholder="is_sneaking or custom state"
                :name="'state-' + idx"
                @update:model-value="addState(idx, $event)"
              />
            </div>
          </div>

          <div class="sub-section">
            <label class="sub-label">Items</label>
            <div
              v-for="(item, iIdx) in ability.requirements.items"
              :key="iIdx"
              class="item-card"
            >
              <div class="item-fields">
                <div class="item-field">
                  <label class="field-label-sm">Action</label>
                  <select
                    class="field-input-sm"
                    :value="item.action"
                    @change="updateItem(idx, iIdx, { action: ($event.target as HTMLSelectElement).value })"
                  >
                    <option value="possession">possession</option>
                    <option value="cost">cost</option>
                  </select>
                </div>
                <div class="item-field">
                  <label class="field-label-sm">Tag</label>
                  <AppCombobox
                    :model-value="item.tag"
                    :suggestions="tagSuggestions"
                    placeholder="#minecraft:logs or minecraft:stone"
                    :name="'tag-' + idx + '-' + iIdx"
                    @update:model-value="updateItem(idx, iIdx, { tag: $event })"
                  />
                </div>
                <div class="item-field">
                  <label class="field-label-sm">Slot</label>
                  <AppCombobox
                    :model-value="item.slot"
                    :suggestions="SLOT_SUGGESTIONS"
                    placeholder="HAND"
                    :name="'slot-' + idx + '-' + iIdx"
                    @update:model-value="updateItem(idx, iIdx, { slot: $event })"
                  />
                </div>
                <div class="item-field">
                  <label class="field-label-sm">Amount</label>
                  <input
                    class="field-input-sm"
                    type="number"
                    step="any"
                    min="1"
                    :value="item.amount"
                    @input="updateItem(idx, iIdx, { amount: Number(($event.target as HTMLInputElement).value) })"
                  />
                </div>
                <div class="item-field">
                  <label class="field-label-sm">Item Cooldown</label>
                  <input
                    class="field-input-sm"
                    type="number"
                    step="any"
                    min="0"
                    :value="item.itemCooldown"
                    @input="updateItem(idx, iIdx, { itemCooldown: Number(($event.target as HTMLInputElement).value) })"
                  />
                </div>
                <button
                  class="btn btn-ghost btn-sm"
                  style="color: var(--p-red-500, #ef4444); align-self: flex-end"
                  @click="removeItem(idx, iIdx)"
                >
                  &times;
                </button>
              </div>
            </div>
            <button
              class="btn btn-primary btn-sm"
              @click="addItem(idx)"
            >
              + Add Item
            </button>
          </div>
        </div>

        <div class="section-block">
          <label class="section-label">Mechanics</label>
          <div
            v-for="(mech, mIdx) in ability.mechanics"
            :key="mIdx"
            class="mechanic-card"
          >
            <div class="mechanic-header">
              <span class="mechanic-title">Mechanic #{{ mIdx + 1 }}</span>
              <button
                class="btn btn-ghost btn-sm"
                style="color: var(--p-red-500, #ef4444)"
                @click="removeMechanic(idx, mIdx)"
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
                  :name="'mech-' + idx + '-' + mIdx"
                  @update:model-value="updateMechanic(idx, mIdx, { type: $event })"
                />
              </div>

              <div class="sub-section">
                <label class="sub-label">Filters</label>
                <FilterBuilder
                  :model-value="mech.filters"
                  :tag-suggestions="tagSuggestions"
                  @update:model-value="updateMechanic(idx, mIdx, { filters: $event })"
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
                      :name="'param-' + idx + '-' + mIdx + '-' + pIdx"
                      @update:model-value="updateMechanicParamName(idx, mIdx, pIdx, $event)"
                    />
                    <button
                      class="btn btn-ghost btn-sm"
                      style="color: var(--p-red-500, #ef4444)"
                      @click="removeMechanicParam(idx, mIdx, pIdx)"
                    >
                      &times;
                    </button>
                  </div>
                  <EvaluatorParameter
                    :model-value="param.evaluator"
                    :label="param.name || 'param'"
                    :name="param.name || 'param'"
                    @update:model-value="updateMechanicParamEvaluator(idx, mIdx, pIdx, $event)"
                  />
                </div>
                <button
                  class="btn btn-primary btn-sm"
                  @click="addMechanicParam(idx, mIdx)"
                >
                  + Add Parameter
                </button>
              </div>
            </div>
          </div>
          <button
            class="btn btn-primary btn-sm"
            @click="addMechanic(idx)"
          >
            + Add Mechanic
          </button>
        </div>

        <div class="section-block">
          <label class="section-label">Feedback</label>

          <div class="feedback-toggles">
            <label class="toggle-check">
              <input
                type="checkbox"
                :checked="ability.feedback.actionBar"
                @change="updateFeedback(idx, { actionBar: ($event.target as HTMLInputElement).checked })"
              />
              Action Bar
            </label>
            <label class="toggle-check">
              <input
                type="checkbox"
                :checked="ability.feedback.chat"
                @change="updateFeedback(idx, { chat: ($event.target as HTMLInputElement).checked })"
              />
              Chat
            </label>
          </div>

          <div class="field-row">
            <label class="field-label">Message</label>
            <input
              class="field-input"
              type="text"
              placeholder="&aSkill activated!"
              :value="ability.feedback.message"
              @input="updateFeedback(idx, { message: ($event.target as HTMLInputElement).value })"
            />
          </div>

          <div class="sub-section">
            <label class="sub-label">Particles</label>
            <div
              v-for="(particle, pIdx) in ability.feedback.particles"
              :key="pIdx"
              class="particle-card"
            >
              <div class="particle-type-row">
                <AppCombobox
                  :model-value="particle.type"
                  :suggestions="PARTICLE_SUGGESTIONS"
                  placeholder="minecraft:flame"
                  :name="'particle-' + idx + '-' + pIdx"
                  @update:model-value="updateParticle(idx, pIdx, { type: $event })"
                />
                <button
                  class="btn btn-ghost btn-sm"
                  style="color: var(--p-red-500, #ef4444); flex-shrink: 0"
                  @click="removeParticle(idx, pIdx)"
                >
                  &times;
                </button>
              </div>
              <div class="particle-fields">
                <div class="particle-field">
                  <label class="field-label-sm">Count</label>
                  <input
                    class="field-input-sm"
                    type="number"
                    step="any"
                    min="1"
                    :value="particle.count"
                    @input="updateParticle(idx, pIdx, { count: Number(($event.target as HTMLInputElement).value) })"
                  />
                </div>
                <div class="particle-field">
                  <label class="field-label-sm">Target</label>
                  <select
                    class="field-input-sm"
                    :value="particle.target"
                    @change="updateParticle(idx, pIdx, { target: ($event.target as HTMLSelectElement).value })"
                  >
                    <option value="self">self</option>
                    <option value="target">target</option>
                  </select>
                </div>
                <div class="particle-field">
                  <label class="field-label-sm">Offset X</label>
                  <input
                    class="field-input-sm"
                    type="number"
                    step="any"
                    :value="particle.offsetX"
                    @input="updateParticle(idx, pIdx, { offsetX: Number(($event.target as HTMLInputElement).value) })"
                  />
                </div>
                <div class="particle-field">
                  <label class="field-label-sm">Offset Y</label>
                  <input
                    class="field-input-sm"
                    type="number"
                    step="any"
                    :value="particle.offsetY"
                    @input="updateParticle(idx, pIdx, { offsetY: Number(($event.target as HTMLInputElement).value) })"
                  />
                </div>
                <div class="particle-field">
                  <label class="field-label-sm">Offset Z</label>
                  <input
                    class="field-input-sm"
                    type="number"
                    step="any"
                    :value="particle.offsetZ"
                    @input="updateParticle(idx, pIdx, { offsetZ: Number(($event.target as HTMLInputElement).value) })"
                  />
                </div>
                <div class="particle-field">
                  <label class="field-label-sm">Speed</label>
                  <input
                    class="field-input-sm"
                    type="number"
                    step="any"
                    :value="particle.speed"
                    @input="updateParticle(idx, pIdx, { speed: Number(($event.target as HTMLInputElement).value) })"
                  />
                </div>
              </div>
            </div>
            <button
              class="btn btn-primary btn-sm"
              @click="addParticle(idx)"
            >
              + Add Particle
            </button>
          </div>

          <div class="sub-section">
            <label class="sub-label">Sounds</label>
            <div
              v-for="(sound, sIdx) in ability.feedback.sounds"
              :key="sIdx"
              class="sound-card"
            >
              <div class="sound-type-row">
                <AppCombobox
                  :model-value="sound.type"
                  :suggestions="SOUND_SUGGESTIONS"
                  placeholder="minecraft:entity_experience_orb_pickup"
                  :name="'sound-' + idx + '-' + sIdx"
                  @update:model-value="updateSound(idx, sIdx, { type: $event })"
                />
                <button
                  class="btn btn-ghost btn-sm"
                  style="color: var(--p-red-500, #ef4444); flex-shrink: 0"
                  @click="removeSound(idx, sIdx)"
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
                    @input="updateSound(idx, sIdx, { volume: Number(($event.target as HTMLInputElement).value) })"
                  />
                </div>
                <div class="sound-field">
                  <label class="field-label-sm">Pitch</label>
                  <input
                    class="field-input-sm"
                    type="number"
                    step="any"
                    :value="sound.pitch"
                    @input="updateSound(idx, sIdx, { pitch: Number(($event.target as HTMLInputElement).value) })"
                  />
                </div>
                <div class="sound-field">
                  <label class="field-label-sm">Target</label>
                  <select
                    class="field-input-sm"
                    :value="sound.target"
                    @change="updateSound(idx, sIdx, { target: ($event.target as HTMLSelectElement).value })"
                  >
                    <option value="self">self</option>
                    <option value="target">target</option>
                  </select>
                </div>
              </div>
            </div>
            <button
              class="btn btn-primary btn-sm"
              @click="addSound(idx)"
            >
              + Add Sound
            </button>
          </div>
        </div>

        <div class="section-block">
          <label class="section-label">On Failure</label>

          <div v-for="(fb, reason) in getOnFailure(idx).reasons" :key="reason" class="failure-card">
            <div class="failure-header">
              <span class="failure-reason-label">{{ FAILURE_REASON_LABELS[reason] || reason }}</span>
              <button
                class="btn btn-ghost btn-sm"
                style="color: var(--p-red-500, #ef4444)"
                @click="removeFailureReason(idx, reason)"
              >
                &times;
              </button>
            </div>
            <div class="failure-body">
              <div class="field-row">
                <label class="field-label">Action Bar</label>
                <input
                  class="field-input"
                  type="text"
                  :placeholder="failurePlaceholder(reason)"
                  :value="fb.actionBar"
                  @input="updateFailureActionBar(idx, reason, ($event.target as HTMLInputElement).value)"
                />
              </div>
              <div class="sub-section">
                <label class="sub-label">Sounds</label>
                <div v-for="(sound, sIdx) in fb.sounds" :key="sIdx" class="sound-card">
                  <div class="sound-type-row">
                    <AppCombobox
                      :model-value="sound.type"
                      :suggestions="SOUND_SUGGESTIONS"
                      placeholder="minecraft:block_note_block_bass"
                      :name="'of-sound-' + idx + '-' + reason + '-' + sIdx"
                      @update:model-value="updateFailureSound(idx, reason, sIdx, { type: $event })"
                    />
                    <button class="btn btn-ghost btn-sm" style="color: var(--p-red-500, #ef4444); flex-shrink: 0"
                      @click="removeFailureSound(idx, reason, sIdx)">&times;</button>
                  </div>
                  <div class="sound-fields">
                    <div class="sound-field">
                      <label class="field-label-sm">Volume</label>
                      <input class="field-input-sm" type="number" step="any"
                        :value="sound.volume"
                        @input="updateFailureSound(idx, reason, sIdx, { volume: Number(($event.target as HTMLInputElement).value) })" />
                    </div>
                    <div class="sound-field">
                      <label class="field-label-sm">Pitch</label>
                      <input class="field-input-sm" type="number" step="any"
                        :value="sound.pitch"
                        @input="updateFailureSound(idx, reason, sIdx, { pitch: Number(($event.target as HTMLInputElement).value) })" />
                    </div>
                    <div class="sound-field">
                      <label class="field-label-sm">Target</label>
                      <select class="field-input-sm"
                        :value="sound.target"
                        @change="updateFailureSound(idx, reason, sIdx, { target: ($event.target as HTMLSelectElement).value })">
                        <option value="self">self</option>
                        <option value="target">target</option>
                      </select>
                    </div>
                  </div>
                </div>
                <button class="btn btn-primary btn-sm" @click="addFailureSound(idx, reason)">+ Add Sound</button>
              </div>
            </div>
          </div>

          <div class="add-failure-row">
            <select v-model="newFailureReason[idx]" class="field-input-sm">
              <option value="" disabled>Select reason...</option>
              <option v-for="opt in FAILURE_REASON_OPTIONS" :key="opt" :value="opt">
                {{ FAILURE_REASON_LABELS[opt] }}
              </option>
            </select>
            <button
              class="btn btn-primary btn-sm"
              :disabled="!newFailureReason[idx]"
              @click="addFailureReason(idx)"
            >
              + Add
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="pendingRemoveAbility !== null" class="modal-overlay" @click.self="pendingRemoveAbility = null">
      <div class="modal">
        <h3>Delete ability?</h3>
        <p>This will permanently remove this ability and all its mechanics.</p>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="pendingRemoveAbility = null">Cancel</button>
          <button class="btn btn-danger" @click="executeRemoveAbility">Delete</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.empty-warning {
  padding: 0.75rem;
  background: color-mix(in srgb, var(--p-primary-color) 8%, transparent);
  border: 1px dashed var(--p-content-border-color);
  border-radius: 6px;
  color: var(--p-text-muted-color);
  font-size: 0.8rem;
  text-align: center;
}
.abilities-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ability-card {
  border: 1px solid var(--p-content-border-color);
  border-radius: 6px;
  background: var(--p-content-background);
  overflow: hidden;
}
.ability-card[draggable="true"] {
  cursor: default;
}
.ability-card.drag-over {
  opacity: 0.5;
}
.drag-handle {
  cursor: grab;
  color: var(--p-form-field-placeholder-color);
  font-size: 1.1rem;
  line-height: 1;
  user-select: none;
}
.drag-handle:active {
  cursor: grabbing;
}
.ability-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--p-form-field-background);
  border-bottom: 1px solid var(--p-content-border-color);
  cursor: pointer;
  user-select: none;
}

.ability-title {
  flex: 1;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--p-form-field-placeholder-color);
}

.expand-toggle {
  font-size: 0.75rem;
  color: var(--p-form-field-placeholder-color);
}

.ability-unlock-level {
  font-size: 0.7rem;
  color: var(--p-form-field-placeholder-color);
  white-space: nowrap;
}
.editor-ability-type-badge {
  font-size: 0.65rem;
  font-weight: 600;
  padding: 0.1rem 0.4rem;
  border-radius: 3px;
  white-space: nowrap;
  line-height: 1.4;
}

.ability-body {
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
  min-width: 6rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--p-text-color);
}

.field-input {
  flex: 1;
  padding: 0.4rem 0.5rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-form-field-background);
  color: var(--p-text-color);
  font-size: 0.85rem;
}

.field-select {
  flex: 1;
  padding: 0.4rem 0.5rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  background: var(--p-form-field-background);
  color: var(--p-text-color);
  font-size: 0.85rem;
}

.section-block {
  border-top: 1px solid var(--p-content-border-color);
  padding-top: 0.75rem;
}

.section-label {
  display: block;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--p-text-color);
  margin-bottom: 0.5rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.sub-section {
  margin-top: 0.5rem;
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

.lore-line-row {
  display: flex;
  gap: 0.4rem;
  margin-bottom: 0.35rem;
  align-items: center;
}
.lore-line-row[draggable="true"] {
  cursor: default;
}
.lore-line-row.lore-drag-over {
  opacity: 0.5;
}
.lore-drag-handle {
  cursor: grab;
  color: var(--p-form-field-placeholder-color);
  font-size: 1.1rem;
  line-height: 1;
  user-select: none;
  flex-shrink: 0;
}
.lore-drag-handle:active {
  cursor: grabbing;
}

.states-group {
  margin-top: 0.5rem;
}

.states-chips {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
  margin-bottom: 0.4rem;
}

.state-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.15rem 0.5rem;
  background: var(--p-primary-color);
  color: #fff;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 500;
}

.app-dark .state-chip {
  color: #000;
}

.chip-remove {
  color: inherit !important;
  padding: 0;
  font-size: 1rem;
  line-height: 1;
  opacity: 0.7;
}

.chip-remove:hover {
  opacity: 1;
}

.state-add-row {
  display: flex;
  gap: 0.4rem;
  align-items: center;
}

.item-card,
.mechanic-card,
.particle-card,
.sound-card {
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  padding: 0.5rem;
  margin-bottom: 0.5rem;
  background: var(--p-content-background);
}

.mechanic-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.mechanic-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--p-form-field-placeholder-color);
}

.mechanic-body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.particle-type-row,
.sound-type-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}
.particle-type-row > :first-child,
.sound-type-row > :first-child {
  flex: 1;
}

.item-fields,
.particle-fields,
.sound-fields {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.item-field,
.particle-field,
.sound-field {
  flex: 1;
  min-width: 100px;
}

.field-label-sm {
  display: block;
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--p-form-field-placeholder-color);
  margin-bottom: 0.15rem;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.field-input-sm {
  width: 100%;
  padding: 0.3rem 0.4rem;
  border: 1px solid var(--p-content-border-color);
  border-radius: 3px;
  background: var(--p-form-field-background);
  color: var(--p-text-color);
  font-size: 0.8rem;
  box-sizing: border-box;
}

.param-entry {
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  padding: 0.5rem;
  background: var(--p-content-background);
  margin-bottom: 0.5rem;
}

.param-header {
  display: flex;
  gap: 0.4rem;
  align-items: center;
  margin-bottom: 0.25rem;
}

.param-name-input {
  flex: 1;
}

.failure-card {
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
  padding: 0.5rem;
  margin-bottom: 0.5rem;
  background: var(--p-content-background);
}
.failure-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}
.failure-reason-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--p-primary-color);
}
.failure-body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.add-failure-row {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  margin-top: 0.5rem;
}

.feedback-toggles {
  display: flex;
  gap: 1.5rem;
  margin-bottom: 0.5rem;
}

.toggle-check {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.85rem;
  color: var(--p-form-field-placeholder-color);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 8px;
  padding: 1.5rem;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}
.modal h3 {
  margin: 0 0 0.5rem;
  font-size: 1.05rem;
  color: var(--p-text-color);
}
.modal p {
  margin: 0 0 1.25rem;
  color: var(--p-text-muted-color, #888);
  font-size: 0.875rem;
  line-height: 1.4;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
