# ISSUE-011: Small Bug Fixes & Visual Tweaks

**Scope:** Quick fixes that don't need a full development plan.

---

## 1. Tags Header `#c:#c:` Bug

**Observation:** The tag editor shows tag headers as `"#c:#c:ores"` instead of `"#c:ores"`. The `#c:` prefix is being prepended twice.

**Root cause:** The `TagListEditor` component or the `TagHandler` API response is double-prefixing tags. The API returns tags as `"tags": { "#c:ores": [...] }` (with the prefix already in the key), and the frontend may be adding `#c:` again when rendering.

**File:** `src/components/tags/TagListEditor.vue`

**Fix:** Remove the hardcoded `#c:` prefix from the tag name display, since the API already returns keys with the prefix.

---

## 2. Ability Count Badge Text

**Observation:** The ability count badge on skill cards shows just a number (e.g., `"3"`). It's unclear what this number means without context.

**File:** `src/components/skills/SkillCard.vue`

**Fix:** Change the badge text from `{{ skill.abilityCount }}` to `{{ skill.abilityCount }} ability{{ skill.abilityCount !== 1 ? 'ies' : '' }}`. If space is limited, show it as a hover tooltip instead.

---

## 3. XP Sources Badge on Skill Cards

**Observation:** Skill cards show ability count but not XP source count. XP sources are equally important for understanding a skill at a glance.

**File:** `src/components/skills/SkillCard.vue` and `SkillDetailDTO.java`

**Fix:** 
1. Add `xpSourceCount` field to `SkillSummaryDTO` in `SkillHandler.list()`
2. Pass it through to the `SkillCard` component
3. Display as a second badge alongside the ability count badge

---

## 4. Ability Editor Regression

**Observation:** Clicking to expand an ability card in the skill editor causes it to disappear. Abilities were editable before but this regressed.

**Root cause:** Likely a Vue reactivity issue where the expand toggle incorrectly removes or hides the ability entry instead of toggling its expanded state.

**File:** `src/components/skills/AbilitiesSection.vue`
