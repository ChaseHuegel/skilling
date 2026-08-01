# ISSUE-109: Add `Cost:` and `Requirements:` lines to all ability lore

**Status:** Open
**Type:** Improvement
**Severity:** Low (content clarity in the skills menu)

---

## Context & User Story

- **Goal:** As a player, I want every ability in the skills menu to display `Cost:` and `Requirements:` lore lines, so that I can see what an ability costs and what conditions it needs before unlocking/using it.
- **Agent Role:** You are an expert content engineer executing this task (bundled skill YAML).

## Implementation Requirements

- [x] For every bundled ability with requirements (cooldown, states, items, exhaustion), add a `Requirements:` lore line listing them in a consistent format
- [x] For every bundled ability with a cost (e.g. `exhaustion.amount`, item consumption), add a `Cost:` lore line
- [x] Use a consistent wording and color scheme across all skills (match the existing `&8Requires: ...` style where present)
- [x] Keep the `template-skill.yml` example updated to document the convention

## Technical Specifications & Context

- **Target Files:**
  - `src/main/resources/skills/*.yml` (all bundled skills; abilities with `requirements:` blocks, e.g. `light_weapons.yml` flurry at lines 44-48)
  - `docs/dev/template-skill.yml` (document the `Cost:`/`Requirements:` lore convention)
  - `docs/dev/SKILL-DESIGN-FRAMEWORK.md` (ability audit checklist)
- **Dependencies:** Ability lore is rendered in `SkillMenuBuilder.buildSkillLore` with `{placeholder}` resolution. Requirements/cost data comes from each ability's `requirements:` YAML block.
- **Constraints:** Lore lines must remain user-facing plain text consistent with the existing ampersand-color style. No engine changes; this is content + docs only.

## Verification & Definition of Done

- [x] Every bundled ability that has a `requirements:` block also has a `Requirements:` lore line matching its YAML
- [x] Every bundled ability with an exhaustion or item cost has a `Cost:` lore line
- [x] `./gradlew build && ./gradlew test` pass (content is validated by `SkillYamlValidationTest`)
- [x] Runtime check: opening the skills menu shows `Cost:` and `Requirements:` lines on affected abilities
