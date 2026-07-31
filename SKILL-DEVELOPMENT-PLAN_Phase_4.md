# SKILL-DEVELOPMENT-PLAN — Phase 4: Skill YAML Remediation (All 32)

**Review references:** Part 1 of `SKILL-REVIEW.md` (per-skill redesigns)
**Priority:** Consumes Phases 1–3 — applies the full corrected YAML content
**Estimated commits:** 8 (grouped by skill category)

---

## Objective

Replace all 32 bundled skill YAMLs in `src/main/resources/skills/` with the redesigned content from `SKILL-REVIEW.md` Part 1. Each redesigned file includes:
- `trigger` fields on every ability (P2-1, Phase 1)
- Namespaced effect keys (`minecraft:poison` etc.) instead of numeric IDs (P2-11, Phase 2)
- `core:ally_aura` for player-only auras in Bard/Piety (P2-5, Phase 2)
- `state:equipped` filters on armor skills (P2-6, Phase 2)
- New mechanic/trigger keys (P2-2 through P2-10, Phase 3)
- Active L25 abilities with native input + requirements (§4)
- Sub-scaling L100 capstones (§6)

---

## Prerequisites

- **Phase 1 complete:** `trigger` field exists and is enforced. Without this, no redesigned YAML can load (all abilities have `trigger` → fail-fast on missing field).
- **Phase 2 complete:** Namespaced effect keys resolve, `core:ally_aura` is registered, `state:equipped` filter is registered.
- **Phase 3 complete:** `core:knockback`, `core:shield_disable`, `core:offhand_strike`, `core:set_cooldown`, `core:modify_attack_speed`, `resurrect`, `elytra_glide` are all registered. Without these, abilities referencing them are silently skipped (mechanic not found).
- `SKILL-REVIEW.md` Part 1 — each skill's fenced YAML block is the authoritative content.

---

## Commit Batching Strategy

Skills are committed in category-aligned batches so each commit is independently loadable and testable. Within a batch, all skills reference only engine pieces that exist (from Phases 1–3). The batch order follows the design groups from `DESIGN.md` §9.

---

## Commit 4.1 — Harvesting & Gathering skills (6 files)

**Files replaced:**
| File | Redesign section |
|------|-----------------|
| `src/main/resources/skills/mining.yml` | `SKILL-REVIEW.md` → mining.yml — Mining |
| `src/main/resources/skills/woodcutting.yml` | → woodcutting.yml — Woodcutting |
| `src/main/resources/skills/excavation.yml` | → excavation.yml — Excavation |
| `src/main/resources/skills/farming.yml` | → farming.yml — Farming |
| `src/main/resources/skills/herbalism.yml` | → herbalism.yml — Herbalism |
| `src/main/resources/skills/husbandry.yml` | → husbandry.yml — Husbandry |

**Key changes across this batch:**
- All abilities get `trigger` fields (block_break, entity_damage_taken, player_tame, etc.)
- `core:haste_effect` abilities re-gated to `block_break` trigger (was firing on every event)
- `core:armor_bonus` abilities re-gated to `entity_damage_taken` (was stacking on every event)
- `core:yield_multiplier` and `core:chain_break` filters corrected
- `crop_grow` XP sources with no filter **removed** (C11: unbounded free XP)
- L25 slots converted to active abilities with requirements (where the redesign specifies)
- L100 capstones converted from constants to sub-scaling evaluators

**Validation:** `./gradlew build && ./gradlew test` — all 6 files must parse without `IllegalArgumentException` (all `trigger` fields present, all mechanic keys registered).

```
feat(skills): remediate gathering skills (mining, woodcutting, excavation, farming, herbalism, husbandry)

Apply redesigned YAMLs from SKILL-REVIEW.md Part 1:
- All abilities gated by trigger field (fixes guardless-mechanic stacking)
- Haste/armor_bonus abilities re-scoped to correct events
- Crop_grow XP sources removed (unbounded free XP)
- L25 slots converted to active abilities with native input
- L100 capstones use sub-scaling evaluators
```

---

## Commit 4.2 — Crafting & Trade skills (6 files)

**Files replaced:**
| File | Redesign section |
|------|-----------------|
| `src/main/resources/skills/building.yml` | → building.yml — Building |
| `src/main/resources/skills/masonry.yml` | → masonry.yml — Masonry |
| `src/main/resources/skills/carpentry.yml` | → carpentry.yml — Carpentry |
| `src/main/resources/skills/smithing.yml` | → smithing.yml — Smithing |
| `src/main/resources/skills/tailoring.yml` | → tailoring.yml — Tailoring |
| `src/main/resources/skills/cooking.yml` | → cooking.yml — Cooking |

**Key changes:**
- Unbounded `craft_item` / `furnace_extract` / `block_place` XP sources get targeted `target`/`tool` filters (C11)
- `core:haste_effect` in building/masonry re-described honestly (Haste = break speed, not placement speed) (C7)
- `core:modify_craft_output` abilities get `target` filters for skill-specific items (C13)
- `tailoring` `dye_master` fixed from `yield_multiplier` (break) to `modify_craft_output` (craft)
- `cooking` `core:speed_bonus` re-gated to `consume_item` trigger (was stacking on every event)
- L100 capstones use sub-scaling evaluators

**Validation:** `./gradlew build && ./gradlew test`

```
feat(skills): remediate crafting skills (building, masonry, carpentry, smithing, tailoring, cooking)

Apply redesigned YAMLs from SKILL-REVIEW.md Part 1:
- XP sources get targeted filters (no XP for trivial crafts)
- Haste-as-placement bugs fixed (haste honestly described as break speed)
- modify_craft_output abilities filtered to skill-specific items
- tailoring dye_master uses craft mechanic (was break mechanic)
- cooking speed_bonus gated to consume_item trigger
- L100 capstones use sub-scaling evaluators
```

---

## Commit 4.3 — Combat Offense skills (8 files)

**Files replaced:**
| File | Redesign section |
|------|-----------------|
| `src/main/resources/skills/light_weapons.yml` | → light_weapons.yml — Light Weapons |
| `src/main/resources/skills/heavy_weapons.yml` | → heavy_weapons.yml — Heavy Weapons |
| `src/main/resources/skills/one_handed.yml` | → one_handed.yml — One Handed |
| `src/main/resources/skills/dual_wield.yml` | → dual_wield.yml — Dual Wield |
| `src/main/resources/skills/unarmed.yml` | → unarmed.yml — Unarmed |
| `src/main/resources/skills/archery.yml` | → archery.yml — Archery |
| `src/main/resources/skills/throwing.yml` | → throwing.yml — Throwing |
| `src/main/resources/skills/smithing.yml` | *(already in Commit 4.2 — moved here)* |

> **Correction:** `smithing.yml` is a crafting skill per `DESIGN.md` §9 grouping and is committed in 4.2. This commit has 7 files: light_weapons, heavy_weapons, one_handed, dual_wield, unarmed, archery, throwing.

**Key changes:**
- **light_weapons & heavy_weapons**: `target:` → `tool:` filter fix (C3 — was fully broken, no XP/abilities fired)
- **dual_wield**: L25 `offhand_strike` uses new `core:offhand_strike` mechanic (P2-4) for a genuine two-weapon loop
- **archery**: `quick_reload` `core:haste_effect` replaced (Haste doesn't affect bow draw) with `arrow_recovery` using `core:projectile_return`; `piercing_shot` description corrected (no piercing mechanic exists)
- **throwing**: `master_thrower` `core:speed_bonus` re-gated to `entity_damage` trigger (was stacking on every event)
- **unarmed**: `lightning_reflexes` dodge gets `hand:empty` filter (was dodging while holding a sword)
- **one_handed**: `duelist` uses new `core:modify_attack_speed` (was `core:lifesteal` — description/behavior mismatch C14)
- L25 slots converted to active abilities; L100 capstones sub-scaled
- All status effects use namespaced keys (`minecraft:poison`, `minecraft:slowness`, `minecraft:weakness`)

**Validation:** `./gradlew build && ./gradlew test`

```
feat(skills): remediate combat offense skills (light_weapons, heavy_weapons, one_handed, dual_wield, unarmed, archery, throwing)

Apply redesigned YAMLs from SKILL-REVIEW.md Part 1:
- light/heavy weapons target→tool filter fix (was fully non-functional)
- dual_wield offhand_strike uses new core:offhand_strike mechanic
- archery quick_reload replaced (haste≠bow draw)
- one_handed duelist uses core:modify_attack_speed (was lifesteal)
- throwing speed_bonus gated to entity_damage trigger
- All status effects use namespaced effect keys
- L25 slots active with native input; L100 capstones sub-scaled
```

---

## Commit 4.4 — Combat Defense skills (5 files)

**Files replaced:**
| File | Redesign section |
|------|-----------------|
| `src/main/resources/skills/shields.yml` | → shields.yml — Shields |
| `src/main/resources/skills/light_armor.yml` | → light_armor.yml — Light Armor |
| `src/main/resources/skills/medium_armor.yml` | → medium_armor.yml — Medium Armor |
| `src/main/resources/skills/heavy_armor.yml` | → heavy_armor.yml — Heavy Armor |
| `src/main/resources/skills/unarmored.yml` | → unarmored.yml — Unarmored |

**Key changes:**
- **Armor skills**: All XP sources and abilities gated by `state:equipped:light/medium/heavy/none` (P2-6, Phase 2) — fixes C12 (Heavy Armor XP while wearing leather)
- All `core:armor_bonus`, `core:knockback_resist`, `core:speed_bonus`, `core:modify_jump` abilities re-gated to `entity_damage_taken` trigger (was stacking on every event)
- **shields**: L25 `shield_bash` uses new `core:knockback` + `core:shield_disable` + `core:set_cooldown` mechanics (was passive `core:thorns_damage` labeled as "bash")
- L25 slots converted to active abilities with native input (sneak+jump for evasion, sneak+right-click for shield bash)
- L100 capstones sub-scaled

**Validation:** `./gradlew build && ./gradlew test`

```
feat(skills): remediate defense skills (shields, light_armor, medium_armor, heavy_armor, unarmored)

Apply redesigned YAMLs from SKILL-REVIEW.md Part 1:
- Armor XP/abilities gated by state:equipped filter (correct gear type)
- All stat-bonus mechanics re-gated to entity_damage_taken trigger
- shields L25 shield_bash uses core:knockback + core:shield_disable
  (was passive thorns_damage labeled as bash)
- L25 slots active with native input; L100 capstones sub-scaled
```

---

## Commit 4.5 — Arcane & Support skills (4 files)

**Files replaced:**
| File | Redesign section |
|------|-----------------|
| `src/main/resources/skills/alchemy.yml` | → alchemy.yml — Alchemy |
| `src/main/resources/skills/enchanting.yml` | → enchanting.yml — Enchanting |
| `src/main/resources/skills/bard.yml` | → bard.yml — Bard |
| `src/main/resources/skills/wizardry.yml` | → wizardry.yml — Wizardry |
| `src/main/resources/skills/piety.yml` | → piety.yml — Piety |

> **Correction:** This batch has 5 files: alchemy, enchanting, bard, wizardry, piety.

**Key changes:**
- **bard**: All `core:field_aura` replaced with `core:ally_aura` (P2-5) — no longer buffs hostile mobs (C6). All abilities gated by `trigger: player_interact` + `tool: #minecraft:instruments` + item cost (Pillar III). `player_interact` XP filtered to instruments (C4). Haste effect (id 3) corrected to Speed (`minecraft:speed`) where "haste" was misleadingly used for movement.
- **piety**: All auras replaced with `core:ally_aura`. L1/L50/L100 re-keyed to `resurrect` trigger (P2-7) for totem synergy. L25 `prayer` is active (sneak+right-click with totem).
- **wizardry**: `arcane_missile` and `blink` get requirements (cooldown + is_sneaking + item cost: redstone/ender pearl) — no more free magic (C5). `mana_shield` re-gated to `player_interact`. All abilities gated by `trigger: player_interact`. `player_interact` XP source removed (was XP-for-existing). L25 `arcane_ward` is active (sneak+right-click with amethyst shard).
- **enchanting**: `experience_attraction` (speed_bonus falsely described as "XP orb attraction") replaced with `soul_binding` active ability (sneak+enchant for cost discount). `soulbound_armor` re-gated to `enchant_item` trigger.
- **alchemy**: `alchemical_haste` speed_bonus re-gated to `brew_potion` trigger. L25 converted to active `catalyst` ability.
- L100 capstones sub-scaled across all 5.

**Validation:** `./gradlew build && ./gradlew test`

```
feat(skills): remediate arcane skills (alchemy, enchanting, bard, wizardry, piety)

Apply redesigned YAMLs from SKILL-REVIEW.md Part 1:
- bard/piety field_aura → core:ally_aura (no more buffing hostile mobs)
- bard/piety/wizardry all get item economy (Pillar III: instruments, totems, redstone, ender pearls)
- wizardry arcane_missile/blink gated by requirements (no free magic)
- piety auras re-keyed to resurrect trigger (totem synergy)
- enchanting experience_attraction replaced (was speed_bonus mislabeled)
- All effects use namespaced keys; L100 capstones sub-scaled
```

---

## Commit 4.6 — Mobility skills (2 files) + `core:speed_bonus` event-guard audit

**Files replaced:**
| File | Redesign section |
|------|-----------------|
| `src/main/resources/skills/acrobatics.yml` | → acrobatics.yml — Acrobatics |
| `src/main/resources/skills/riding.yml` | → riding.yml — Riding |

**Key changes:**
- **acrobatics**: `sprint`/`sneak` XP sources **removed** (C11: XP for toggling state). `core:speed_bonus`/`core:modify_jump` re-gated to `entity_damage_taken` and `elytra_glide` triggers. L25 `leap` is active (sneak+jump). L75 `endurance` uses new `elytra_glide` trigger (P2-8).
- **riding**: Speed/armor/knockback-resist mechanics re-gated to `ride_horse`/`entity_damage`/`entity_damage_taken` triggers (was stacking on every event while riding). L25 `cavalry_charge` active (sneak+attack while mounted).

**Validation:** `./gradlew build && ./gradlew test`

```
feat(skills): remediate mobility skills (acrobatics, riding)

Apply redesigned YAMLs from SKILL-REVIEW.md Part 1:
- acrobatics sprint/sneak XP removed (was XP for state-toggle spam)
- speed_bonus/modify_jump re-gated to entity_damage_taken + elytra_glide
- acrobatics L75 endurance uses new elytra_glide trigger
- riding stat mechanics re-gated to ride_horse/entity_damage triggers
- L25 slots active with native input; L100 capstones sub-scaled
```

---

## Commit 4.7 — Update E2E test fixtures

**Files:**
| File | Change |
|------|--------|
| `web/frontend/e2e/test-data/skills/mining.yml` | Update fixture to include `trigger` fields on all abilities. Use the Phase 4 redesigned `mining.yml` content (or a representative subset that exercises the new schema). |
| `web/frontend/e2e/test-data/tags.yml` | Add any new custom tags referenced by the redesigned skill fixtures (e.g., if `#c:light_weapons` is used, ensure it's present — though it's in `tags.yml` already, verify it's in the fixture). |
| `web/frontend/e2e/specs/*.spec.ts` | Update any specs that assert specific ability structures to account for the `trigger` field. Add a new spec asserting `trigger` is present on all abilities in the loaded mining skill. |

**Validation:** `cd web/frontend && npm run e2e` (requires server with the updated skill files)

```
test(e2e): update fixtures and specs for trigger field schema

E2E fixture mining.yml now includes trigger fields on all abilities.
A new spec verifies that every ability in the loaded skill has a
non-blank trigger key. Tags fixture verified for any new custom
tags referenced by redesigned skills.
```

---

## Commit 4.8 — Final YAML validation sweep

After all 32 files are committed, run a final validation pass to ensure:
1. Every ability in every skill file has a non-blank `trigger` field.
2. Every `effect:` value is either a namespaced key (starts with `minecraft:`) or a legacy numeric (deprecated but accepted).
3. No ability references a mechanic key that isn't registered in `Skilling.java`.
4. No XP source has trigger `player_interact` without a filter (C4 regression guard).
5. No ability uses `target:` on `entity_damage` trigger with a weapon tag (C3 regression guard — should be `tool:`).

**Files:**
| File | Change |
|------|--------|
| `src/test/java/.../engine/SkillYamlValidationTest.java` (new) | A JUnit 5 test that loads every skill YAML from `src/main/resources/skills/` and asserts the above rules. This is a regression guard for future skill additions. |

**Validation:** `./gradlew test` — the new test must pass.

```
test(engine): add YAML validation sweep for all bundled skills

A new test loads every skill YAML in src/main/resources/skills/ and
asserts: every ability has a non-blank trigger, no player_interact
XP source is unfiltered, no entity_damage ability uses target for
weapon detection (should use tool), and all effect/attribute params
use namespaced keys. This is a regression guard for future skill
additions.
```

---

## Phase 4 Completion Checklist

- [ ] All 32 skill YAMLs replaced with redesigned content from `SKILL-REVIEW.md` Part 1
- [ ] Every ability has a `trigger` field
- [ ] All status effects use namespaced keys
- [ ] All armor skills use `state:equipped` filters
- [ ] Bard/Piety use `core:ally_aura` (no `core:field_aura`)
- [ ] Wizardry abilities have requirements (no free magic)
- [ ] L25 slots are active with native input + requirements
- [ ] L100 capstones use sub-scaling evaluators
- [ ] `./gradlew build && ./gradlew test` passes
- [ ] E2E fixtures updated and tests pass
- [ ] YAML validation sweep test passes

## Skill File Checklist (32 files)

<details>
<summary>Expand for full file list</summary>

**Gathering (Commit 4.1):**
- [ ] mining.yml
- [ ] woodcutting.yml
- [ ] excavation.yml
- [ ] farming.yml
- [ ] herbalism.yml
- [ ] husbandry.yml

**Crafting (Commit 4.2):**
- [ ] building.yml
- [ ] masonry.yml
- [ ] carpentry.yml
- [ ] smithing.yml
- [ ] tailoring.yml
- [ ] cooking.yml

**Combat Offense (Commit 4.3):**
- [ ] light_weapons.yml
- [ ] heavy_weapons.yml
- [ ] one_handed.yml
- [ ] dual_wield.yml
- [ ] unarmed.yml
- [ ] archery.yml
- [ ] throwing.yml

**Combat Defense (Commit 4.4):**
- [ ] shields.yml
- [ ] light_armor.yml
- [ ] medium_armor.yml
- [ ] heavy_armor.yml
- [ ] unarmored.yml

**Arcane & Support (Commit 4.5):**
- [ ] alchemy.yml
- [ ] enchanting.yml
- [ ] bard.yml
- [ ] wizardry.yml
- [ ] piety.yml

**Mobility (Commit 4.6):**
- [ ] acrobatics.yml
- [ ] riding.yml

**Validation (Commits 4.7–4.8):**
- [ ] E2E fixtures updated
- [ ] YAML validation sweep test passes

</details>