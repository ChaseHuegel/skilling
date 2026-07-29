# ISSUE-009: Skill Schema Gaps in Web GUI

## Description

Cross-reference every field in the YAML skill schema (`template-skill.yml`,
`SkillDefinition.java`, `SkillManager.java`) against the web editor UI
components to identify missing controls, data format mismatches, and
extensibility gaps.

## Gap Analysis

### Critical Gaps

#### GAP 1: Particle `offset` Format Mismatch — Data Loss on Edit

**Problem:** The YAML/engine stores particle offset as `offset: [x, y, z]` (list),
but the frontend `ParticleConfig` interface uses `offsetX`, `offsetY`, `offsetZ`
(three separate number fields). When a skill is loaded from YAML, the `offset`
array is mapped to `undefined` fields in the frontend form. Any edit to the
particle drops the original offset values.

**Fix:** Add converter in `SkillEditorPage.vue` `apiAbilityToForm()` and
`formAbilityToApi()` to translate between `offset: [x,y,z]` ↔ `offsetX/Y/Z`.

#### GAP 2: `display.name` vs Root `display_name` — Wrong Name on Initial Load

**Problem:** Template YAML uses `display.name: "Mining"` but the DTO
`fromMap()` reads `str(raw, "display_name", id)` which is null in the template,
falling back to the skill ID. The initial load shows the ID instead of the
display name.

**Fix:** In `SkillSerializer.fromMap()`, fall back to `display.name` when root
`display_name` is null.

### Moderate Gaps

#### GAP 3: Ability Requirements States Hardcoded to 4 Checkboxes

**Problem:** The ability requirements section uses hardcoded checkboxes for
only 4 states (`is_sneaking`, `is_sprinting`, `is_in_water`, `is_on_ground`),
but the engine accepts any arbitrary state string (e.g. `player_placed:false`,
`is_gliding`, `has_effect:minecraft:speed`).

**Fix:** Add a free-text fallback input or replace checkboxes with the same
`AppCombobox` + tag-chip pattern used in `FilterBuilder`.

#### GAP 4: No Dynamic Registry Suggestions

**Problem:** Mechanic types, trigger types, particle types, sound types are
hardcoded in frontend arrays. Custom mechanics/triggers added via the API
are not discoverable in the UI.

**Fix:** Add API endpoints (`GET /api/mechanics`, `GET /api/triggers`,
`GET /api/evaluators`) and fetch them on app load.

### Minor Gaps

#### GAP 5: Progression `milestone` Curve Offered but Unsupported

**Problem:** The `ProgressionSection` curve selector includes `milestone`
but the engine only supports `polynomial`, `linear`, and `constant` for
progression curves.

**Fix:** Remove `milestone` from the progression curve selector.

#### GAP 6: No Lore Placeholder Autocomplete

**Problem:** The lore editor is a plain text input with no suggestions for
available `{placeholder}` tokens based on mechanic parameters.

**Fix:** Add an `AppCombobox` or suggestion dropdown showing available
placeholders from the ability's mechanics.

#### GAP 7: YAML Comments Stripped on Save

**Problem:** SnakeYAML discards all comments on serialization. After the
first web edit, all inline documentation comments in the skill YAML are lost.

**Fix:** Accept as a known limitation; document it.

## Implementation Order

1. **GAP 1** (Critical — data loss) → Particle offset converter
2. **GAP 2** (Moderate — wrong display) → Fallback in `fromMap()`
3. **GAP 3** (Moderate — blocked UX) → Free-text state input
4. **GAP 4** (Moderate — discoverability) → API endpoints + fetch
5. **GAP 5** (Minor) → Remove milestone from progression
6. **GAP 6** (Minor) → Lore placeholder suggestions

Risk: Low to Moderate. All changes are backward-compatible.
