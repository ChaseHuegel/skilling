<script setup lang="ts">
import { ref, computed } from 'vue'
import SectionToolbar from '../common/SectionToolbar.vue'
import AppCombobox from '../common/AppCombobox.vue'
import SoundConfigEditor, { type SoundConfig } from '../common/SoundConfigEditor.vue'
import MechanicsEditor, { type MechanicEntry } from './MechanicsEditor.vue'
import OnFailureEditor, { type OnFailure } from './OnFailureEditor.vue'
import FormattedText from '../common/FormattedText.vue'
import EvaluatorParameter from '../common/EvaluatorParameter.vue'
import { useDragReorder } from '../../composables/useDragReorder'
import { useRegistriesStore } from '../../stores/registries'
import { STATE_SUGGESTIONS } from '../common/stateFilters'
import { stableKey } from '../../utils/stableKey'
import { cooldownToNumber, isDynamicCooldown, type CooldownEvaluator } from '../../utils/cooldown'

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

const SLOT_SUGGESTIONS = ['HAND', 'OFF_HAND', 'FEET', 'LEGS', 'CHEST', 'HEAD']

// Offline fallback mirroring Skilling.registerBuiltinTriggers (Skilling.java).
// The live /api/triggers endpoint is the source of truth.
const FALLBACK_TRIGGERS = [
  'block_break', 'block_place', 'entity_damage', 'entity_damage_taken', 'entity_kill',
  'craft_item', 'furnace_extract', 'brew_potion', 'brew_start', 'repair',
  'player_interact', 'consume_item', 'fishing', 'crop_grow', 'breed_animals',
  'sprint', 'sneak', 'ride_horse', 'collect_xp', 'level_up', 'enchant_item',
  'shoot_bow', 'item_damage', 'player_shear', 'player_tame', 'launch_projectile',
  'projectile_hit', 'resurrect', 'cure_villager', 'elytra_glide',
]

const TRIGGER_SUGGESTIONS = computed(() =>
  registriesStore.triggers.length > 0 ? registriesStore.triggers : FALLBACK_TRIGGERS
)

interface RequirementItem {
  action: string
  tag: string
  slot: string
  amount: number
  itemCooldown: number
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

interface Ability {
  _key?: string
  id: string
  displayName: string
  unlockLevel: number
  trigger: string
  lore: string[]
  requirements: {
    cooldown: number | CooldownEvaluator
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

const STATE_OPTIONS = computed(() =>
  registriesStore.stateFilters.length > 0 ? registriesStore.stateFilters : STATE_SUGGESTIONS
)

const expanded = ref<Record<string, boolean>>({})
const pendingRemoveAbility = ref<number | null>(null)
const loreDragIndex = ref<string | null>(null)
const pendingState = ref<Record<string, string>>({})
const sectionExpanded = ref<Record<string, boolean>>({})

/** Stable per-row identity used for expanded-state and v-for keys. */
function abilityKey(ability: Ability): string {
  if (!ability._key) {
    ability._key = stableKey()
  }
  return ability._key
}

function toggleSection(abilityIdx: number, sectionKey: string) {
  const key = `${abilityKey(props.modelValue[abilityIdx])}-${sectionKey}`
  sectionExpanded.value[key] = !sectionExpanded.value[key]
}

function isSectionExpanded(abilityIdx: number, sectionKey: string): boolean {
  const key = `${abilityKey(props.modelValue[abilityIdx])}-${sectionKey}`
  return sectionExpanded.value[key] !== false
}

function toggleExpand(idx: number) {
  const key = abilityKey(props.modelValue[idx])
  expanded.value[key] = !expanded.value[key]
}

function isAbilityActive(ability: Ability): boolean {
  const r = ability.requirements
  return cooldownToNumber(r.cooldown) > 0
    || isDynamicCooldown(r.cooldown)
    || r.state.length > 0
    || r.items.length > 0
}

/** Converts a scalar cooldown into a constant evaluator so it can be edited dynamically. */
function setDynamicCooldown(index: number) {
  const ab = props.modelValue[index]
  const current = cooldownToNumber(ab.requirements.cooldown)
  updateRequirement(index, { cooldown: { type: 'constant', params: { value: current } } })
}

function emptyAbility(): Ability {
  return {
    _key: stableKey(),
    id: '',
    displayName: '',
    unlockLevel: 0,
    trigger: '',
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

function addAbility() {
  emit('update:modelValue', [...props.modelValue, emptyAbility()])
}

function duplicateAbility(index: number) {
  const source = props.modelValue[index]
  const cloned: Ability = {
    ...JSON.parse(JSON.stringify(source)),
    _key: stableKey(),
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

function getOnFailure(index: number): OnFailure {
  return props.modelValue[index].onFailure || { reasons: {} }
}

function updateOnFailure(index: number, patch: Partial<OnFailure>) {
  updateAbility(index, { onFailure: { ...getOnFailure(index), ...patch } })
}
</script>

<template>
  <div class="abilities-section">
    <SectionToolbar
      section-name="Ability"
      @add="addAbility"
    />

    <div v-if="modelValue.length === 0" class="empty-warning">
      No abilities defined. Add some to give players unlockable perks.
    </div>

    <div
      v-for="(ability, idx) in modelValue"
      :key="abilityKey(ability)"
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
        <span class="expand-toggle">{{ expanded[abilityKey(ability)] ? '▼' : '▶' }}</span>
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
        v-if="expanded[abilityKey(ability)]"
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

        <div class="field-row">
          <label class="field-label">Trigger</label>
          <AppCombobox
            :model-value="ability.trigger || ''"
            :suggestions="TRIGGER_SUGGESTIONS"
            placeholder="e.g. block_break"
            :name="'trigger-' + idx"
            @update:model-value="updateAbility(idx, { trigger: $event })"
          />
        </div>

        <div class="section-block section-block--lore">
          <div class="section-header" @click="toggleSection(idx, 'lore')">
            <span class="section-toggle">{{ isSectionExpanded(idx, 'lore') ? '▼' : '▶' }}</span>
            <svg class="section-icon" viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.2">
              <rect x="2" y="2" width="12" height="12" rx="1"/>
              <line x1="4.5" y1="6" x2="11.5" y2="6"/>
              <line x1="4.5" y1="8.5" x2="11.5" y2="8.5"/>
              <line x1="4.5" y1="11" x2="9" y2="11"/>
            </svg>
            <label class="section-label">Lore Lines</label>
            <span class="section-count">({{ ability.lore.length }})</span>
          </div>
          <template v-if="isSectionExpanded(idx, 'lore')">
            <div
              v-for="(line, lIdx) in ability.lore"
              :key="lIdx"
              class="lore-line-block"
              :class="{ 'lore-drag-over': loreDragIndex === idx + '-' + lIdx }"
              draggable="true"
              @dragstart="onLoreDragStart(idx, lIdx)"
              @dragover="onLoreDragOver($event, idx, lIdx)"
              @dragend="onLoreDragEnd"
            >
              <div class="lore-line-row">
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
            </div>
            <div v-if="ability.lore.length > 0" class="lore-full-preview">
              <div class="full-preview-label">Preview:</div>
              <div
                v-for="(line, lIdx) in ability.lore"
                :key="'full-' + lIdx"
                class="full-preview-line"
              ><FormattedText :text="line" /></div>
            </div>
            <button
              class="btn btn-primary btn-sm"
              @click="addLoreLine(idx)"
            >
              + Add Lore Line
            </button>
          </template>
        </div>

        <div class="section-block section-block--requirements">
          <div class="section-header" @click="toggleSection(idx, 'requirements')">
            <span class="section-toggle">{{ isSectionExpanded(idx, 'requirements') ? '▼' : '▶' }}</span>
            <svg class="section-icon" viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.2">
              <path d="M3 2.5h10L13 8a6 6 0 01-5 5.5A6 6 0 013 8L3 2.5z"/>
              <line x1="5.5" y1="7" x2="7.5" y2="9"/>
              <line x1="7.5" y1="9" x2="10.5" y2="5.5"/>
            </svg>
            <label class="section-label">Requirements</label>
          </div>
          <template v-if="isSectionExpanded(idx, 'requirements')">
            <div class="field-row">
              <label class="field-label">Cooldown (s)</label>
              <template v-if="!isDynamicCooldown(ability.requirements.cooldown)">
                <input
                  class="field-input"
                  type="number"
                  step="any"
                  min="0"
                  :value="ability.requirements.cooldown"
                  @input="updateRequirement(idx, { cooldown: Number(($event.target as HTMLInputElement).value) })"
                />
                <button
                  class="btn btn-ghost btn-sm"
                  title="Edit as an evaluator (linear/milestones)"
                  @click="setDynamicCooldown(idx)"
                >
                  dynamic
                </button>
              </template>
            </div>
            <div v-if="isDynamicCooldown(ability.requirements.cooldown)" class="cooldown-evaluator">
              <EvaluatorParameter
                :model-value="ability.requirements.cooldown"
                label="Cooldown"
                name="Cooldown (s)"
                :types="['constant', 'linear', 'milestones']"
                @update:model-value="updateRequirement(idx, { cooldown: $event })"
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
          </template>
        </div>

        <div class="section-block section-block--mechanics">
          <div class="section-header" @click="toggleSection(idx, 'mechanics')">
            <span class="section-toggle">{{ isSectionExpanded(idx, 'mechanics') ? '▼' : '▶' }}</span>
            <svg class="section-icon" viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.2">
              <circle cx="8" cy="8" r="3"/>
              <path d="M8 2v2M8 12v2M2 8h2M12 8h2M3.76 3.76l1.41 1.41M10.83 10.83l1.41 1.41M3.76 12.24l1.41-1.41M10.83 5.17l1.41-1.41" stroke-linecap="round"/>
            </svg>
            <label class="section-label">Mechanics</label>
            <span class="section-count">({{ ability.mechanics.length }})</span>
          </div>
          <template v-if="isSectionExpanded(idx, 'mechanics')">
            <MechanicsEditor
              :model-value="ability.mechanics"
              :tag-suggestions="tagSuggestions"
              @update:model-value="updateAbility(idx, { mechanics: $event })"
            />
          </template>
        </div>

        <div class="section-block section-block--feedback">
          <div class="section-header" @click="toggleSection(idx, 'feedback')">
            <span class="section-toggle">{{ isSectionExpanded(idx, 'feedback') ? '▼' : '▶' }}</span>
            <svg class="section-icon" viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.2">
              <path d="M4 8a4 4 0 018 0c0 2 1 3 1 3H3s1-1 1-3z"/>
              <line x1="6.5" y1="12" x2="9.5" y2="12" stroke-linecap="round"/>
              <line x1="8" y1="2" x2="8" y2="3.5" stroke-linecap="round"/>
            </svg>
            <label class="section-label">Feedback</label>
          </div>
          <template v-if="isSectionExpanded(idx, 'feedback')">
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
              <div class="section-header section-header--sm" @click="toggleSection(idx, 'particles')">
                <span class="section-toggle">{{ isSectionExpanded(idx, 'particles') ? '▼' : '▶' }}</span>
                <label class="sub-label" style="margin-bottom: 0; cursor: pointer">Particles</label>
                <span class="section-count">({{ ability.feedback.particles.length }})</span>
              </div>
              <template v-if="isSectionExpanded(idx, 'particles')">
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
              </template>
            </div>

            <div class="sub-section">
              <div class="section-header section-header--sm" @click="toggleSection(idx, 'sounds')">
                <span class="section-toggle">{{ isSectionExpanded(idx, 'sounds') ? '▼' : '▶' }}</span>
                <label class="sub-label" style="margin-bottom: 0; cursor: pointer">Sounds</label>
                <span class="section-count">({{ ability.feedback.sounds.length }})</span>
              </div>
              <template v-if="isSectionExpanded(idx, 'sounds')">
                <SoundConfigEditor
                  :model-value="ability.feedback.sounds"
                  :name-prefix="'sound-' + idx"
                  @update:model-value="updateFeedback(idx, { sounds: $event })"
                />
              </template>
            </div>
          </template>
        </div>

        <div class="section-block section-block--on-failure">
          <div class="section-header" @click="toggleSection(idx, 'on-failure')">
            <span class="section-toggle">{{ isSectionExpanded(idx, 'on-failure') ? '▼' : '▶' }}</span>
            <svg class="section-icon" viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" stroke-width="1.2">
              <circle cx="8" cy="8" r="6"/>
              <line x1="5.5" y1="5.5" x2="10.5" y2="10.5" stroke-linecap="round"/>
              <line x1="10.5" y1="5.5" x2="5.5" y2="10.5" stroke-linecap="round"/>
            </svg>
            <label class="section-label">On Failure</label>
            <span class="section-count">({{ Object.keys(getOnFailure(idx).reasons).length }})</span>
          </div>
          <template v-if="isSectionExpanded(idx, 'on-failure')">
            <OnFailureEditor
              :model-value="getOnFailure(idx)"
              :name-prefix="'of-' + idx"
              @update:model-value="updateOnFailure(idx, $event)"
            />
          </template>
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
  border-left: 4px solid transparent;
  padding-top: 0.75rem;
  padding-left: 1rem;
  margin-left: -0.25rem;
  transition: background 0.15s ease;
}

.section-block--lore { border-left-color: var(--p-cyan-400); --section-accent: var(--p-cyan-400); }
.section-block--requirements { border-left-color: var(--p-orange-400); --section-accent: var(--p-orange-400); }
.section-block--mechanics { border-left-color: var(--p-purple-400); --section-accent: var(--p-purple-400); }
.section-block--feedback { border-left-color: var(--p-green-400); --section-accent: var(--p-green-400); }
.section-block--on-failure { border-left-color: var(--p-red-400); --section-accent: var(--p-red-400); }

.section-block {
  background: color-mix(in srgb, var(--section-accent) 3%, var(--p-content-background));
}

.section-label {
  display: inline;
  font-size: 0.85rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.section-block--lore .section-label { color: var(--p-cyan-400); }
.section-block--requirements .section-label { color: var(--p-orange-400); }
.section-block--mechanics .section-label { color: var(--p-purple-400); }
.section-block--feedback .section-label { color: var(--p-green-400); }
.section-block--on-failure .section-label { color: var(--p-red-400); }

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

.lore-line-block {
  margin-bottom: 0.35rem;
}
.lore-line-block[draggable="true"] {
  cursor: default;
}
.lore-line-block.lore-drag-over {
  opacity: 0.5;
}
.lore-line-row {
  display: flex;
  gap: 0.4rem;
  align-items: center;
}
.lore-full-preview {
  margin-top: 0.5rem;
  padding: 0.5rem;
  background: var(--p-content-background);
  border: 1px solid var(--p-content-border-color);
  border-radius: 4px;
}
.full-preview-label {
  font-size: 0.75rem;
  color: var(--p-text-muted-color);
  margin-bottom: 0.25rem;
}
.full-preview-line {
  font-size: 0.85rem;
  line-height: 1.4;
  margin-bottom: 0.1rem;
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
}

.item-card { border-left: 3px solid var(--p-orange-400); }
.mechanic-card { border-left: 3px solid var(--p-purple-400); }
.particle-card { border-left: 3px solid var(--p-green-400); }
.sound-card { border-left: 3px solid var(--p-cyan-400); }

.item-card,
.mechanic-card,
.particle-card,
.sound-card,
.failure-card {
  background: color-mix(in srgb, var(--section-accent) 7%, var(--p-content-background));
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
  border-left: 2px dotted var(--p-indigo-300);
  border-radius: 4px;
  padding: 0.5rem;
  margin-left: 0.5rem;
  margin-bottom: 0.5rem;
  background: color-mix(in srgb, var(--section-accent) 10%, var(--p-content-background));
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
  border-left: 3px solid var(--p-red-400);
  border-radius: 4px;
  padding: 0.5rem;
  margin-bottom: 0.5rem;
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

.section-header {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  cursor: pointer;
  user-select: none;
  margin-bottom: 0.5rem;
}
.section-header:hover .section-label {
  opacity: 0.8;
}
.section-header--sm {
  margin-bottom: 0.35rem;
}
.section-header--sm .section-label {
  font-size: 0.8rem;
}
.section-toggle {
  font-size: 0.65rem;
  color: var(--p-form-field-placeholder-color);
  flex-shrink: 0;
  width: 0.75rem;
  text-align: center;
}
.section-count {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--p-form-field-placeholder-color);
  margin-left: 0.25rem;
}
.section-icon {
  flex-shrink: 0;
  opacity: 0.7;
}
.section-header:hover .section-icon {
  opacity: 1;
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
