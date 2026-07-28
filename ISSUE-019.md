# ISSUE-019: Reusable Combobox Component (AppCombobox)

**Scope:** Create a reusable searchable combobox component using the native HTML `<datalist>` pattern, then apply it to the State, Target, and Tool fields. Enrich suggestion lists with known options.

---

## Background

The project already uses `<input list="id">` + `<datalist>` for Target and Tool fields in `FilterBuilder.vue`. The State field in FilterBuilder uses a plain text input with a hint string. The XP source trigger dropdown in `XpSourcesSection.vue` uses a `<select>` which doesn't allow custom values.

A reusable component will:
- Reduce code duplication (the input + datalist pattern appears 3+ times)
- Provide consistent styling
- Make it easy to enrich suggestion lists

## Requirements

### Component API

```html
<AppCombobox v-model="value" :suggestions="['a', 'b', 'c']" label="Field" placeholder="Type or select..." />
```

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `modelValue` | `string` | — | v-model |
| `suggestions` | `string[]` | `[]` | Dropdown suggestions |
| `label` | `string` | `''` | Label text above input |
| `placeholder` | `string` | `''` | Input placeholder |
| `name` | `string` | `''` | Datalist ID suffix (for multiple instances) |

### Component Behavior

- Renders a `<label>` + `<input list="id">` + `<datalist id="id">` structure
- Input accepts free-text (not restricted to suggestions)
- Datalist provides searchable dropdown suggestions as the user types
- Styling matches existing `.field-input` / `.filter-input` patterns
- Unique datalist ID generated from a combination of `name` prop + a generated UID

### Existing Patterns to Replace

| Location | Field | Current | Replace With |
|----------|-------|---------|-------------|
| `FilterBuilder.vue` | Target | `<input>` + `<datalist>` inline | `<AppCombobox>` |
| `FilterBuilder.vue` | Tool | `<input>` + `<datalist>` inline | `<AppCombobox>` |
| `FilterBuilder.vue` | State | `<input>` + hint text | `<AppCombobox>` |
| `XpSourcesSection.vue` | Trigger | `<select>` | `<AppCombobox>` (allows custom triggers) |
| `AbilitiesSection.vue` | Mechanic type | `<input>` | `<AppCombobox>` (optional, future) |

### Suggestion Enrichment

Suggestion lists should be enriched beyond current tag suggestions:

**State suggestions:** `is_sneaking`, `is_sprinting`, `is_in_water`, `is_on_ground`, `player_placed:false`, `player_placed:true`

**Target/Tool suggestions:** All custom tags (`#c:*`), all vanilla tag namespaces (`#minecraft:*`), and common material names (`minecraft:stone`, `minecraft:iron_ore`, etc.)

**Trigger suggestions:** `block_break`, `block_place`, `entity_damage`, `entity_damage_taken`, `entity_kill`, `craft_item`, `furnace_extract`, `brew_potion`, `player_interact`, `consume_item`, `fishing`, `crop_grow`, `breed_animals`

## Files

- `src/components/common/AppCombobox.vue` — new component
- `src/components/common/FilterBuilder.vue` — replace ALL three inline inputs (Target, State, Tool) with `<AppCombobox>`. This applies to every filter card across the app (XP sources, mechanic filters, etc.) since FilterBuilder is used by both `XpSourcesSection.vue` and `AbilitiesSection.vue`
- `src/components/skills/XpSourcesSection.vue` — replace trigger `<select>` with `<AppCombobox>`

## Implementation Notes

- Use `<script setup lang="ts">` and typed props/emits
- Generate unique ID via `useId()` (or a simple counter ref) to avoid datalist ID collisions when multiple comboboxes are on the same page
- The `<datalist>` `id` must match the `<input>` `list` attribute
- Keep scoped CSS using `var(--p-*)` custom properties for theming
