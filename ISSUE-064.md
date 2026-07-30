# ISSUE-064: Bundled Skills Design Review

## Executive Summary

The 6 bundled skills (mining, woodcutting, excavation, farming, fishing, archery) were implemented to ship pre-built, production-ready configurations. A cross-reference of the YAML files against the actual mechanic/trigger implementations reveals **significant mechanical misalignment**: 5 of 6 skills contain abilities that don't do what their descriptions claim, 4 of 6 are missing their L75 milestone entirely, and fishing has zero abilities that work with its trigger event. Additionally, exhaustion is underutilized as a cost mechanism across the board. Each issue is cataloged below with specific remediation.

---

## Skill-by-Skill Review

### 1. Mining (`src/main/resources/skills/mining.yml`)

| Level | Ability | Mechanic | Status | Notes |
|-------|---------|----------|--------|-------|
| 1 | Geologist | `core:yield_multiplier` | ✅ Implementable | Works on `BlockBreakEvent` with `#c:ores` filter |
| 15 | Prospector | `core:xp_bonus` | ✅ Implementable | Stores multiplier in concurrent map, consumed by XP pipeline |
| 25 | Vein Miner | `core:chain_break` | ✅ Implementable | BFS chain break with exhaustion cost; 5s cooldown, coal consumption |
| 50 | Magma Forge | `core:auto_smelt` | ✅ Implementable | Hardcoded smelt map (ores, cobble, sand, clay); L50 unlock at 25% is weak — consider adjusting base |
| **75** | **Totem Support** | **None (missing)** | ❌ **Not present** | ISSUE-058 proposed inventory totem activation. Needs new mechanic or special handler. **No L75 exists in YAML.** |
| 100 | Perfect Yield | `core:yield_multiplier` (const 100%) | ⚠️ Redundant | Guarantees double drops on every ore. Stacks with Geologist for 3× total. Functions but is just a flat capstone — no sub-scaling, no uniqueness. |

**Issues:** L75 milestone absent entirely. L100 capstone is a flat constant with no sub-scaling growth.

**Suggested Improvements:**
- **L75 Totem Support:** Replace with exhaustion-gated ability — e.g., "Mining consumes exhaustion instead of totem durability while in caves" using a `core:totem_support` or `core:exhaustion_shield` mechanic that lets totems proc from inventory during mining, fueled by food consumption.
- **L100 Perfect Yield:** Add sub-scaling (triple-chance: linear 0%→25%) as ISSUE-058 originally specified, so it grows from L100 instead of being static.
- Consider moving Vein Miner from L25 to L25 (good) but its exhaustion currently uses `linear {base: 2.0, step: -0.05, min: 0.5}` — this has a `step` of -0.05 which *decreases* exhaustion cost as you level. Good for balance, but the template-skill.yml had it at L15. The shift to L25 is correct per the 6-tier framework.

---

### 2. Woodcutting (`src/main/resources/skills/woodcutting.yml`)

| Level | Ability | Mechanic | Status | Notes |
|-------|---------|----------|--------|-------|
| 1 | Lumberjack | `core:yield_multiplier` | ✅ Implementable | Works on `BlockBreakEvent` with `#minecraft:logs` filter |
| 15 | Efficient Swing | `core:speed_bonus` | ❌ **Misaligned** | `SpeedBonusMechanic` applies `GENERIC_MOVEMENT_SPEED` — gives movement speed, not axe swing/mining speed. Description says "Increases axe swing speed by {multiplier}x." |
| 25 | Timber Feller | `core:chain_break` | ✅ Implementable | BFS chain break on logs with exhaustion cost |
| 50 | Forest Bounty | `core:yield_multiplier` | ⚠️ Partial | Target is `#minecraft:leaves`. Works on `BlockBreakEvent` but leaves are typically destroyed via decay (no event fires). Only applies when a player manually breaks leaves. |
| **75** | **Reforestation** | **None (missing)** | ❌ **Not present** | ISSUE-058 proposed auto-bone-meal dirt below broken logs. **No L75 exists in YAML.** |
| 100 | Ancient Timber | `core:modify_craft_output` | ✅ Implementable | Works on `CraftItemEvent` with planks filter. Flat 2× multiplier. |

**Issues:** L15 gives wrong attribute (movement instead of swing speed). L50 only triggers on manual leaf break. L75 missing.

**Suggested Improvements:**
- **L15 Efficient Swing:** Replace `speed_bonus` with a new `core:mining_speed` mechanic hooking `BlockBreakEvent` to modify `block.getBreakSpeed()` or apply `HASTE` effect while holding an axe. Alternatively, add a configurable `core:haste_effect` mechanic that applies `CONDUIT_POWER`/`HASTE` status effect for `duration` seconds when holding relevant tool. Paper API has `PlayerAttemptPickupItemEvent` but for break speed, use `addPotionEffect(new PotionEffect(HASTE, ...))` when the player equips the tool.
- **L50 Forest Bounty:** Change trigger to also fire on leaf decay events (or use `BlockBreakEvent` with a manual decay listener). Add guaranteed apple/sapling drop mechanics.
- **L75 Reforestation:** Implement as an exhaustion-gated passive — breaking a log has a `chance` (linear 10%→40%) to auto-bone-meal the block beneath. Could use `BlockFertilizeEvent` simulation. Consume exhaustion instead of bone meal.
- **L100 Ancient Timber:** Add sub-scaling (e.g., multiplier linear 1.5→2.5 or add stick bonus).

---

### 3. Excavation (`src/main/resources/skills/excavation.yml`)

| Level | Ability | Mechanic | Status | Notes |
|-------|---------|----------|--------|-------|
| 1 | Dig Double | `core:yield_multiplier` | ✅ Implementable | Works on `BlockBreakEvent` with `#c:excavatable` |
| 15 | Quick Dig | `core:speed_bonus` | ❌ **Misaligned** | Same problem as Woodcutting L15 — gives `MOVEMENT_SPEED`, not dig speed. |
| 25 | Wide Sweep | `core:block_damage` | ❌ **Wrong mechanic** | `BlockDamageMechanic` cancels `EntityDamageEvent` (shield-like block). Does **nothing** for block breaking. Per ISSUE-012, `core:area_harvest` (radius-based) is the correct mechanic for area block breaking. |
| 50 | Fossil Hunter | `core:yield_multiplier` | ⚠️ Partial | Targets `#minecraft:sand`. Sand drops itself; flint is only from gravel. Either the tag or the intent needs fixing. |
| **75** | **Eerie Dust** | **None (missing)** | ❌ **Not present** | ISSUE-058 proposed XP bonus for gravel/sand. **No L75 exists in YAML.** |
| 100 | Excavator | `core:block_damage` | ❌ **Wrong mechanic** | Same as Wide Sweep. Should enhance Wide Sweep (expand radius, remove cooldown). |

**Issues:** Core active ability (Wide Sweep) uses completely wrong mechanic. L75 missing. L100 repeats the same wrong mechanic.

**Suggested Improvements:**
- **L25 Wide Sweep / L100 Excavator:** Requires `core:area_harvest` mechanic (ISSUE-012 §2.2). Parameterize with `radius` (milestone-based: L25=1→L100=2 for 3×3→5×5) and `cooldown` (inverse reduction). Add exhaustion cost per block broken.
- **L15 Quick Dig:** Same fix as Woodcutting — replace with `HASTE` effect mechanic or new `core:dig_speed`.
- **L50 Fossil Hunter:** Fix target to include both `#minecraft:sand` and `#minecraft:gravel`. Add guaranteed flint/bone drops.
- **L75 Eerie Dust:** Implement as `xp_bonus` tied to `#c:excavatable` blocks (already exists). Add visual particle feedback.

---

### 4. Farming (`src/main/resources/skills/farming.yml`)

| Level | Ability | Mechanic | Status | Notes |
|-------|---------|----------|--------|-------|
| 1 | Green Thumb | `core:yield_multiplier` | ✅ Implementable | Works on `BlockBreakEvent` with `#minecraft:crops` |
| 15 | Seasoned Hand | `core:saturation_inject` | ✅ Implementable | Works on `PlayerItemConsumeEvent` |
| 25 | Harvest Wave | `core:chain_break` | ⚠️ Design mismatch | Crops are planted in rows with 1-block spacing. `chain_break` uses 6-direction adjacency (up/down included). It chains along a row but can't spread to adjacent rows. A radius-based `area_harvest` (3→5→7→9) would match crop farm layouts better. |
| 50 | Nutrient Rich | `core:yield_multiplier` | ✅ Implementable | Second yield multiplier stacks with Green Thumb. Fine. |
| **75** | **Bone Meal Expert** | **None (missing)** | ❌ **Not present** | ISSUE-058 proposed 3×3→5×5 bone meal radius. **No L75 exists in YAML.** |
| 100 | Living Earth | `core:yield_multiplier` (const 100%) | ❌ **Wrong behavior** | Just doubles drops. Should auto-replant crops. ISSUE-058 identified `core:auto_replant` as a needed new mechanic. |

**Issues:** L25 adjacency doesn't match crop layout. L75 missing. L100 does the wrong thing entirely.

**Suggested Improvements:**
- **L25 Harvest Wave:** Replace `chain_break` with `chain_break` using a radius-based limit (`total_blocks` parameter) or implement `core:area_harvest` with crop filter. Alternatively, change `chain_break` to support Manhattan/horizontal-only adjacency for crops.
- **L75 Bone Meal Expert:** Implement via `PlayerInteractEvent` + bone meal in hand: increase affected radius from 1 (single block) to up to 2 (5×5). Use exhaustion as cost per block fertilized.
- **L100 Living Earth:** Implement `core:auto_replant`. On `BlockBreakEvent` for mature crops: cancel drop, set block to seed-stage equivalent, drop items via `Block.dropNaturally()`. Add exhaustion cost for balance.

---

### 5. Fishing (`src/main/resources/skills/fishing.yml`)

| Level | Ability | Mechanic | Status | Notes |
|-------|---------|----------|--------|-------|
| 1 | Angler | `core:yield_multiplier` | ❌ **Wrong event** | `YieldMultiplierMechanic` only handles `BlockBreakEvent`. `PlayerFishEvent` is not processed. **Does nothing.** |
| 15 | Light Line | `core:modify_attribute` | ❌ **Wrong behavior** | `ModifyAttributeMechanic` adds a transient attribute modifier (e.g. `MOVEMENT_SPEED`). Does not affect fishing rod durability loss. |
| 25 | Lucky Catch | `core:yield_multiplier` | ❌ **Wrong event** | Same as L1 — won't fire on `PlayerFishEvent`. |
| 50 | Sea Bounty | `core:yield_multiplier` | ❌ **Wrong event** | Same as L1. |
| **75** | **Enchanted Waters** | **None (missing)** | ❌ **Not present** | ISSUE-058 proposed bonus potion duration from fishing treasure. **No L75 exists in YAML.** |
| 100 | Leviathan | `core:yield_multiplier` (const 100%) | ❌ **Wrong event** | Same problem. Constant 100% doesn't help if the mechanic never fires on fishing events. |

**Issues:** **Every ability is broken.** The `yield_multiplier` mechanic only works on `BlockBreakEvent` and does nothing for `PlayerFishEvent`. Only 5 abilities instead of 6 (missing L75). The XP source (`fishing` trigger) works, but no abilities do.

**Suggested Improvements (requires new mechanics):**
- **L1 Angler:** New `core:fishing_yield` mechanic hooked to `PlayerFishEvent`. Increase treasure catch rate, bonus fish, or item quality. The `PlayerFishEvent` provides `getCaught()`, `getState()` (FISHING, CAUGHT_FISH, CAUGHT_ENTITY, etc.).
- **L15 Light Line:** New `core:durability_save` mechanic that works on `PlayerItemDamageEvent` or `PlayerItemBreakEvent`. Chance to negate rod durability loss. Sub-scale chance linear 50%→100%.
- **L25 Lucky Catch:** Implement via `core:fishing_yield` with loot table enhancement (better enchantments on books/bows, more treasure items).
- **L50 Sea Bounty:** Implement via `core:fishing_yield` with fish multiplier.
- **L75 Enchanted Waters:** Use existing `modify_potion_duration` but triggered on `PlayerFishEvent` when caught item is a potion. Or create a `fishing_treasure` trigger.
- **L100 Leviathan:** Implement as capstone: guaranteed max-size fish, bonus treasure, particle fanfare. Exhaustion-based activation.

---

### 6. Archery (`src/main/resources/skills/archery.yml`)

| Level | Ability | Mechanic | Status | Notes |
|-------|---------|----------|--------|-------|
| 1 | Steady Hand | `core:modify_damage` | ✅ Implementable | Works on `EntityDamageByEntityEvent` with arrow filter |
| 15 | Quick Reload | `core:speed_bonus` | ❌ **Misaligned** | Gives `MOVEMENT_SPEED` while holding bow. ISSUE-058 identified `core:bow_draw_speed` is needed. |
| 25 | Piercing Shot | `core:modify_damage` | ⚠️ Partial | Just increases damage. Doesn't implement arrow piercing through 1→3 targets. Requires modifying arrow `setPierceLevel()` on `EntityShootBowEvent`. |
| 50 | Mark Target | `core:apply_status` | ✅ Implementable | Applies slowness effect on arrow hit. Works via `EntityDamageByEntityEvent`. |
| **75** | **Flame Arrow** | **None (missing)** | ❌ **Not present** | ISSUE-058 proposed arrow-ignite on hit. **No L75 exists in YAML.** |
| 100 | Eagle Eye | `core:modify_damage` (const 3.5×) | ⚠️ Partial | Applies 3.5× damage unconditionally. Should only apply to shots >15 blocks distance. No sub-scaling (static 3.5× at all L100+). |

**Issues:** L15 misaligned. L25/L100 don't implement the described behavior (just flat damage boosts). L75 missing. Requires `EntityShootBowEvent` trigger (ISSUE-012 §2.5 — `shoot_bow` trigger not yet implemented). The XP sources use `entity_damage` with `#minecraft:arrows` filter which works, but L25 piercing and L15 draw speed both need the shoot event.

**Suggested Improvements:**
- **L15 Quick Reload:** Implement `core:bow_draw_speed` mechanic. Paper API: modify bow's `Attribute.GENERIC_ATTACK_SPEED` or use `EntityShootBowEvent` to reduce charge time. Alternatively, existing `core:speed_bonus` could be extended to support `ATTACK_SPEED` attribute. NOTE: `GENERIC_ATTACK_SPEED` affects melee attack speed, not bow draw. Draw speed is controlled by the bow's `useDuration` in `PlayerItemCooldownEvent`. A custom listener in the mechanic is needed.
- **L25 Piercing Shot:** Add `EntityShootBowEvent` trigger. On bow release, apply `Arrow.setPierceLevel(pierceCount)` where pierceCount sub-scales 1→3. Remove damage multiplier — piercing is the feature, not damage.
- **L75 Flame Arrow:** Implement via `EntityDamageByEntityEvent` — set target on fire for `duration` seconds (sub-scale 2→5). Use existing `modify_damage` or `apply_status` with `FIRE` effect, or create a simple fire-arrow mechanic.
- **L100 Eagle Eye:** Check distance between shooter and target on `EntityDamageByEntityEvent`. Only apply crit multiplier when `distance > 15`. Sub-scale multiplier linear 2.0→3.5. Add visual feedback (crit particles, sound).

---

## Cross-Cutting Concerns

### Which Abilities Need New Mechanics That Don't Exist Yet

| Skill | Ability | Required Mechanic | ISSUE-012 Ref | Priority |
|-------|---------|-------------------|---------------|----------|
| Excavation | Wide Sweep / Excavator | `core:area_harvest` | Yes (§2.2) | **Critical** — core ability broken |
| Farming | Living Earth (L100) | `core:auto_replant` | Identified in ISSUE-058 (§3) | **High** — capstone does wrong thing |
| Archery | Quick Reload (L15) | `core:bow_draw_speed` or `EntityShootBowEvent` listener | Yes (§2.5) | **High** — core ability broken |
| Fishing | All abilities | `core:fishing_yield` (or make existing mechanics event-agnostic) | No (new need) | **High** — whole skill non-functional |
| Fishing | Light Line (L15) | `core:durability_save` | No (new need) | **Medium** — needs rod durability handling |
| Archery | Piercing Shot (L25) | `shoot_bow` trigger (`EntityShootBowEvent`) | Yes (§2.5) | **Medium** — current XP source works without it |
| Mining | Totem Support (L75) | `core:inventory_totem` or exhaustion-shield | No (new need) | **Low** — milestone is missing entirely |

### Missing Triggers

| Trigger | Skill Need | ISSUE-012 Ref | Status |
|---------|-----------|---------------|--------|
| `shoot_bow` (`EntityShootBowEvent`) | Archery L15 (draw speed), L25 (piercing modifier) | §2.5 | **Not implemented.** 19 triggers exist but this one isn't in the `impl/` directory. |
| `item_break` (`PlayerItemBreakEvent`) | Fishing L15 (rod durability save) | §2.5 | Not implemented but needed for fishing L15. |

### Exhaustion as a Cost Mechanism

Currently, exhaustion is only used inside `ChainBreakMechanic` (Vein Miner, Timber Feller). It is **not** a first-class requirement type — it cannot be declared in YAML outside of `chain_break` parameters.

**Proposal:** Elevate exhaustion to a first-class `requirement` type, parallel to `cooldown`, `state`, and `items`:

```yaml
requirements:
  cooldown: 8.0
  state: ["is_sneaking"]
  exhaustion:
    amount: 2.0           # hunger points consumed
    minimum: 6.0          # refuses to activate if food <= this
```

This would:
1. Allow any ability to cost hunger (Wide Sweep, Harvest Wave, Piercing Shot, etc.)
2. Give food a systemic value boost — aligns perfectly with SKILL-DESIGN-FRAMEWORK Pillar III (Item Economy Preservation: "Amplify, Never Replace")
3. Follow the Check → Execute → Consume pattern: `requirements.check()` verifies `player.getFoodLevel() > minimum`, `requirements.consume()` deducts `amount`
4. Remove the ad-hoc exhaustion logic currently embedded in `ChainBreakMechanic` (line 82-85), making it a standalone requirement evaluator

**Skills that should use exhaustion:**
- **Excavation L25 Wide Sweep:** 1.0 exhaustion per 3×3 activation
- **Farming L25 Harvest Wave:** 0.5 exhaustion per crop harvested
- **Farming L100 Living Earth:** 2.0 exhaustion per auto-replant cycle
- **Archery L25 Piercing Shot:** 1.0 exhaustion per piercing shot
- **Mining L75 (new):** Exhaustion-based totem shield (consume food instead of totem)

### Alignment with the 6-Tier Milestone Progression Framework

Per SKILL-DESIGN-FRAMEWORK.md §3:

| Milestone | Role | Current State |
|-----------|------|---------------|
| **L1** | Foundational Passive (Primary Scalar) | ✅ All 6 skills have a working L1 passive |
| **L15** | QoL Passive (Early Friction Reduction) | ❌ Woodcutting, Excavation, Archery L15s give movement speed instead of the described benefit. Fishing L15 is non-functional. |
| **L25** | Primary Ability (Core Gameplay Hook) | ❌ Excavation L25 uses wrong mechanic. Farming L25 has layout mismatch. Archery L25 is a flat damage boost. Fishing L25 is non-functional. |
| **L50** | Major Passive (Mid-Game Efficiency) | ✅ Most L50s work (Mining, Woodcutting, Excavation, Farming). Archery L50 works. Fishing L50 is non-functional. |
| **L75** | Synergy Passive (Cross-System Vanilla Link) | ❌❌❌ **4 of 6 skills are missing L75 entirely.** Only Mining (absent) and Archery (absent) have them — wait, neither has one. Actually 0 of 6 have an L75. Mining YAML has only 5 abilities. All 6 skills are missing L75. |
| **L100** | Mastery Capstone (Game-Changing Perk) | ❌ Mining L100 is redundant with L1. Excavation L100 uses wrong mechanic. Farming L100 is a yield multiplier instead of auto-replant. Fishing L100 is non-functional. Archery L100 is unconditional. |

**Correction:** After cross-checking YAML files, **zero of six skills have a complete 6-milestone set.** Every skill has exactly 5 abilities (L1, L15, L25, L50, L100), skipping L75 entirely.

---

## Recommendations

### Skills Ready As-Is (after minor parameter tweaks)
- **None.** Every skill has at least one critical issue.

### Nearest to Ready (fixes are small)
1. **Mining** — Only missing L75. L100 needs sub-scaling. Exhaustion already correctly used in Vein Miner. **Estimated effort: 1 new mechanic + YAML edits.**
2. **Archery** — L15 needs new mechanic. L25 needs `EntityShootBowEvent`. L75 needs creation. L100 needs distance check. **Estimated effort: 2 new mechanics + 1 new trigger + YAML edits.**

### Needs New Mechanics
3. **Woodcutting** — L15 needs new `core:mining_speed`/haste mechanic. L50 needs leaf listener. L75 missing. **Estimated effort: 1 new mechanic + YAML edits.**
4. **Excavation** — L25 needs `core:area_harvest` mechanic. L15 needs dig speed. L75 missing. L100 same as L25. **Estimated effort: 1 new mechanic + YAML edits.**

### Needs Significant Rework
5. **Farming** — L25 needs radius-based harvest. L75 missing. L100 needs `core:auto_replant`. **Estimated effort: 1 new mechanic + YAML edits + design rethink.**
6. **Fishing** — **Complete rewrite required.** Zero abilities work. Needs `core:fishing_yield` and `core:durability_save` mechanics. **Estimated effort: 2-3 new mechanics + full YAML rewrite.**

### Priority Order for Implementation

| Priority | Skill | Rationale |
|----------|-------|-----------|
| 1 | **Fishing** | **Highest impact** — every ability is non-functional. A fishing skill with zero working abilities is the most user-facing failure. |
| 2 | **Excavation** | Core active ability (Wide Sweep) is completely broken — uses wrong mechanic. |
| 3 | **Archery** | Needs new trigger (`shoot_bow`), new mechanic (`bow_draw_speed`), and L15/L25/L75 fixes. But combat skills are high-visibility. |
| 4 | **Farming** | L100 does the wrong thing, L25 is suboptimal, L75 missing. But existing mechanics mostly work for XP gain. |
| 5 | **Woodcutting** | L15 is misaligned but `chain_break` and `modify_craft_output` work. L75 missing. |
| 6 | **Mining** | Closest to complete. Only missing L75 and L100 sub-scaling. |

### Exhaustion Implementation Priority

Add exhaustion as a first-class requirement evaluator in the `RequirementEngine` before fixing individual skills. This unlocks the cross-cutting design pattern and is a prerequisite for several recommended changes.

**Order:**
1. Add `core:exhaustion` requirement evaluator (parallel to `cooldown`, `state`, `items` in requirements block)
2. Remove ad-hoc exhaustion from `ChainBreakMechanic` — migrate to requirement evaluator
3. Add exhaustion costs to: Wide Sweep, Harvest Wave, Piercing Shot, Living Earth, new L75 abilities

---

## Summary of Findings

| Area | Finding |
|------|---------|
| **Broken mechanics** | 6 abilities across 3 skills use mechanics that don't match their intent (mostly `speed_bonus` for non-movement speed, `block_damage` for area breaking) |
| **Wrong event hooks** | Fishing's `yield_multiplier` only works on `BlockBreakEvent`, never fires on `PlayerFishEvent` — 5 fishing abilities are inert |
| **Missing L75 milestone** | All 6 skills skip L75 → only 5 abilities per skill instead of the mandated 6 |
| **Exhaustion underuse** | Only 2 of 18 abilities use exhaustion; it's hardcoded in `ChainBreakMechanic` instead of being a first-class requirement |
| **Missing mechanics** | `core:area_harvest`, `core:auto_replant`, `core:bow_draw_speed`, `core:fishing_yield`, `core:durability_save` needed |
| **Missing triggers** | `shoot_bow` (`EntityShootBowEvent`), `item_break` (`PlayerItemBreakEvent`) needed |
| **Sub-scaling violations** | Several L100 capstones use `constant: 100.0` with no growth from unlock level (Mining Perfect Yield, Fishing Leviathan, Archery Eagle Eye) |
