# SKILL-DEVELOPMENT-PLAN — Phase 1: Ability `trigger` Field + P0 Critical Fixes

**Review references:** P2-1, C1, C2, C3, C4 (§3.1 of `SKILL-REVIEW.md`)
**Priority:** P0 — unblocks all Phase 4 skill YAML remediation
**Estimated commits:** 6

---

## Objective

Add a required `trigger` field to abilities so `fireAbilities()` only evaluates them on the matching event dispatch, fixing the guardless-mechanic stacking bug (C1/C2). Then apply the two remaining P0 bug fixes: melee `target:`→`tool:` for light/heavy weapons (C3) and unbounded `player_interact` XP sources (C4). Keep the Web GUI in parity by surfacing the `trigger` field in the ability editor.

---

## Prerequisites

- `SKILL-REVIEW.md` Part 2 (P2-1) and Part 3 (§3.2 items 1–4)
- `AGENTS.md` §6 (Check/Execute/Consume), §1 (composition), Testing & Validation
- `web/AGENTS.md` (frontend architecture, REST API, E2E fixtures)

---

## Commit 1.1 — Add `trigger` field to `SkillDefinition.Ability`

**Files:**
| File | Change |
|------|--------|
| `skilling-api/.../engine/SkillDefinition.java` | Add `String trigger` to the `Ability` record (after `unlockLevel`, before `display`). Update canonical constructor and accessor. |
| `src/main/java/.../engine/SkillManager.java` | Parse `trigger` from YAML `abilities[].trigger` key. **Fail-fast:** throw `IllegalArgumentException` if the key is missing or blank (per `AGENTS.md` fail-fast rule). |

**Validation:** `./gradlew build` — all existing skills currently lack the `trigger` field, so this commit will cause load failures. This is intentional: it forces Phase 4 to add the field to every YAML.

**Test:** Add unit test `SkillManagerTriggerFieldTest` verifying:
- A skill YAML with a missing `trigger` on an ability throws `IllegalArgumentException`.
- A skill YAML with a valid `trigger` parses the field correctly.

```
feat(api): add trigger field to ability schema

Abilities must declare a single trigger key that binds them to one
event dispatch, preventing guardless mechanics from firing on every
event. The field is required and fail-fast validated during YAML
parsing.
```

---

## Commit 1.2 — Enforce `trigger` matching in `fireAbilities()`

**Files:**
| File | Change |
|------|--------|
| `src/main/java/.../engine/listener/SkillEventListener.java` | In `fireAbilities()`, after the unlock-level check and before the mechanics loop, add: `if (!ability.trigger().equals(triggerKey)) continue;`. This is the root-cause fix for C1/C2. |

**Validation:** `./gradlew build && ./gradlew test`

**Test:** No new test needed — this is a routing guard. Existing tests should pass (they don't test `fireAbilities` directly since it requires Bukkit). Manual verification: with all skills still missing `trigger` fields, no abilities fire (expected — Phase 4 adds the fields).

```
fix(engine): enforce trigger gating in fireAbilities

fireAbilities() now skips any ability whose trigger key does not
match the dispatching event's trigger. This prevents the nine
guardless mechanics (speed_bonus, armor_bonus, knockback_resist,
etc.) from firing on every event and stacking transient attribute
modifiers.
```

---

## Commit 1.3 — Web GUI: surface `trigger` field in ability editor

**Files:**
| File | Change |
|------|--------|
| `web/handler/SkillHandler.java` / `web/dto/...` | Add `trigger` field to the `AbilityDTO` (or equivalent) used by `SkillSerializer.parseAbility` and `toMap`. Read/write `trigger` as a top-level key on each abilities[] entry. |
| `web/frontend/src/api/client.ts` | No change needed (untyped `any`). |
| `web/frontend/src/components/skills/AbilitiesSection.vue` | 1. Add `trigger` to the inline `Ability` interface. 2. Add a trigger-key `<select>` or combobox at the top of each ability editor card, populated from `registriesStore.triggers` (already dynamic via `/api/triggers`) with `FALLBACK_TRIGGERS` as fallback. 3. Include `trigger` in the `emitAbility` / serialization output so PUT/POST sends it. 4. Validate non-blank before save. |

**Validation:** `./gradlew build` (Java), `cd web/frontend && npm run build` (frontend), E2E: `npm run e2e` — existing E2E tests should still pass since `trigger` is additive.

**Test:** Add an E2E spec verifying the trigger dropdown appears and can be set. Verify the round-trip: create a skill via API with `trigger` on an ability, GET it back, confirm `trigger` is preserved.

```
feat(web): surface ability trigger field in skill editor

The Web GUI ability editor now includes a trigger-key selector for
each ability, backed by the live /api/triggers endpoint. The
serializer reads and writes the trigger field to YAML, keeping the
web app in parity with the engine schema.
```

---

## Commit 1.4 — Web GUI: unify divergent state-filter suggestion lists

While editing the ability editor for the trigger field, fix the known divergence between `FilterBuilder.STATE_SUGGESTIONS` and `AbilitiesSection.STATE_OPTIONS` (per web exploration findings). These should be a single shared constant so both filters and requirements state chips offer the same autocomplete set.

**Files:**
| File | Change |
|------|--------|
| `web/frontend/src/components/common/FilterBuilder.vue` | Extract `STATE_SUGGESTIONS` to an exported constant (or move to a shared `constants.ts`). |
| `web/frontend/src/components/skills/AbilitiesSection.vue` | Import and use the shared constant instead of the local `STATE_OPTIONS`. Remove the divergent local list. |

**Validation:** `cd web/frontend && npm run build`

```
refactor(web): unify state-filter suggestion lists

FilterBuilder and AbilitiesSection previously maintained separate
hardcoded state-filter suggestion lists with divergent entries.
Both now reference a single shared constant to prevent drift.
```

---

## Commit 1.5 — Fix light_weapons & heavy_weapons `target:` → `tool:` filters (C3)

Per `SKILL-REVIEW.md` §#2 Finding: `resolveEventMaterial()` returns `null` for a `Player` melee damager, so `target:` filters on melee `entity_damage` abilities never match — light_weapons and heavy_weapons are **fully non-functional** (no XP, no abilities fire).

**Files:**
| File | Change |
|------|--------|
| `src/main/resources/skills/light_weapons.yml` | Change all `target: "#c:light_weapons"` filters (xp_sources + abilities) to `tool: "#c:light_weapons"`. Add `trigger` fields to all abilities (see Phase 4 redesigns for the full content, but this commit makes the minimal fix to unblock P0). |
| `src/main/resources/skills/heavy_weapons.yml` | Change all `target: "#c:heavy_weapons"` filters to `tool: "#c:heavy_weapons"`. Add `trigger` fields. |

**Validation:** `./gradlew build && ./gradlew test`

**Note:** This is a **minimal** fix to restore functionality — the full redesign from `SKILL-REVIEW.md` Part 1 is applied in Phase 4. The `trigger` fields added here must be consistent with the redesigns to avoid rework.

**Test:** Manual verification that XP is now granted for melee kills with light/heavy weapons.

```
fix(skills): change target to tool filter for light/heavy weapons

The target filter on melee entity_damage events never matches
because resolveEventMaterial() returns null for a Player damager.
Switching to the tool filter (which reads the held main-hand item)
restores XP granting and ability execution for light_weapons and
heavy_weapons. Trigger fields added per Phase 1 schema change.
```

---

## Commit 1.6 — Fix unbounded `player_interact` XP sources (C4)

Per `SKILL-REVIEW.md` Known Issue #2: `bard.yml` and `wizardry.yml` grant XP on every `player_interact` event with no filter — XP for opening doors, pressing buttons, clicking air.

**Files:**
| File | Change |
|------|--------|
| `src/main/resources/skills/bard.yml` | Add a `tool` filter (e.g., `tool: "#minecraft:instruments"` — note: this tag may not exist yet; use a material list like `minecraft:goat_horn` as a stopgap until Phase 2 adds it to `tags.yml`) to the `player_interact` XP source. Add `trigger` fields to all abilities. |
| `src/main/resources/skills/wizardry.yml` | Remove the `player_interact` XP source entirely (per the Phase 4 redesign, wizardry XP comes from `collect_xp` and `launch_projectile`). Add `trigger` fields. Add requirements to `arcane_missile` and `blink` (cooldown + is_sneaking + item cost) as a stopgap per the review's P0 item 3. |

**Validation:** `./gradlew build && ./gradlew test`

**Note:** These are **stopgap** fixes — the full redesigns from Phase 4 replace these files entirely. The stopgap ensures the P0 free-magic and XP-spam bugs don't ship.

```
fix(skills): gate player_interact XP sources and add wizardry requirements

Bard player_interact XP now requires an instrument item filter,
ending XP-for-existing. Wizardry arcane_missile and blink now
require sneaking, a cooldown, and an item catalyst (redstone/ender
pearl), ending the free-magic fire-on-every-interact bug. Trigger
fields added per Phase 1 schema change.
```

---

## Phase 1 Completion Checklist

- [ ] `SkillDefinition.Ability` has a `trigger` field
- [ ] `SkillManager` fails-fast on missing `trigger`
- [ ] `SkillEventListener.fireAbilities()` enforces trigger matching
- [ ] Web GUI ability editor surfaces and serializes `trigger`
- [ ] State-filter suggestion lists unified in frontend
- [ ] light_weapons & heavy_weapons use `tool:` filters (functional again)
- [ ] Bard `player_interact` XP gated by instrument filter
- [ ] Wizardry `arcane_missile` and `blink` have requirements (no free magic)
- [ ] `./gradlew build` passes
- [ ] `./gradlew test` passes
- [ ] `cd web/frontend && npm run build` passes
- [ ] E2E tests pass (or new fixtures updated for trigger field)

## Dependencies on Later Phases

- Phase 4 (skill YAML remediation) applies the **full** redesigned content for all 32 skills, including `trigger` fields. The stopgap YAML edits in commits 1.5 and 1.6 will be overwritten by Phase 4.
- Phase 2 adds `core:ally_aura` and `state:equipped`, which several Phase 4 redesigns reference.
- Phase 3 adds `core:knockback`, `core:shield_disable`, etc., which Phase 4 redesigns reference.