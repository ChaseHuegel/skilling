# Skilling — Bundled Skill Audit & Redesign Report

**Auditor:** Senior Minecraft Plugin Architect
**Scope:** All 32 skill YAMLs under `src/main/resources/skills/`
**References:** `SKILL-DESIGN-FRAMEWORK.md` (§1–§5), `AGENTS.md`, `docs/capabilities.md`, `Skilling.java` (L254–430), `template-skill.yml`, `docs/creating-skills.md`

---

## Audit Methodology

Every ability in every skill was traced against the engine source (`SkillEventListener.java`, all mechanic implementations, `Skilling.java` registrations) to verify actual runtime behavior — not just the YAML intent or `capabilities.md` claims. Seven audit lenses were applied (A–G, per the assignment). Issue tags: **[BUG]**, **[SHORTCOMING]**, **[VIOLATION]**, **[DESIGN]**.

### The #1 Systemic Finding (read first)

**Guardless mechanics fire on every dispatched event, and there is no per-ability trigger field.**

`SkillEventListener.dispatch()` calls `fireAbilities()` on **every** event. `fireAbilities()` iterates **all** abilities of **all** skills. An ability has only three gates before its mechanic runs: (1) mechanic-level `filters`, (2) `requirements` (cooldown/state/items), and (3) the mechanic's own `event instanceof` guard. There is **no `trigger` field** on abilities — the schema has none (`docs/creating-skills.md`, `template-skill.yml`).

Nine mechanics have **no `event instanceof` guard** and therefore execute on every event that passes their (often empty or non-event-specific) filters:

| Mechanic | Has event guard? | Documented event | Actual behavior |
|---|---|---|---|
| `core:speed_bonus` | ❌ | `PlayerToggleSprintEvent` | Fires on ANY event; **stacks** new UUID `MOVEMENT_SPEED` modifiers each call |
| `core:armor_bonus` | ❌ | *(none documented)* | Fires on ANY event; **stacks** `ARMOR`/`ARMOR_TOUGHNESS` modifiers |
| `core:knockback_resist` | ❌ | *(none documented)* | Fires on ANY event; **stacks** `KNOCKBACK_RESISTANCE` modifiers |
| `core:modify_attribute` | ❌ | *(none documented)* | Fires on ANY event; **stacks** the attribute modifier |
| `core:haste_effect` | ❌ | `BlockBreakEvent` | Fires on ANY event; refreshes Haste potion (does not stack, but re-applies constantly) |
| `core:modify_jump` | ❌ | *(none documented)* | Fires on ANY event; **stacks** `JUMP_STRENGTH` modifiers |
| `core:field_aura` | ❌ | "Varies" | Fires on ANY event; re-applies potion effects to **all nearby living entities (including hostile mobs)** |
| `core:aoe_effect` | ❌ | *(none documented)* | Same as field_aura |
| `core:xp_bonus` | ❌ | "Varies" | Fires on ANY event; resets the session multiplier (harmless, but wasteful) |

**Consequence:** Every passive ability using these mechanics accumulates duplicate transient attribute modifiers on every tick-equivalent event. A player with several passive armor/speed abilities will accrue dozens or hundreds of stacked modifiers within minutes of normal play, producing extreme stat inflation and TPS overhead from modifier bookkeeping.

**Root-cause fix:** Add a `trigger` field to the ability schema so each ability declares the single trigger event it responds to, and enforce it in `fireAbilities()`. This is proposed as **New Engine Piece #1** (Part 2). All redesigns below assume this field exists. Until it lands, every guardless-mechanic passive is effectively broken. This is classified **P0**.

### The #2 Systemic Finding: `target` filter on melee `entity_damage` never matches

`resolveEventMaterial()` for `EntityDamageByEntityEvent` calls `projectileToMaterial(de.getDamager())`, which returns `null` for a `Player` damager (melee). Any `target:` filter on a melee `entity_damage` ability therefore **always fails** — the ability never fires and the XP source never grants. This breaks **light_weapons** and **heavy_weapons** entirely (they use `target: "#c:light_weapons"` / `target: "#c:heavy_weapons"` to detect the held weapon, but `target` matches event-material, not the held tool). The correct filter is `tool:` (main-hand item). **P0** for those two skills.

### The #3 Systemic Finding: legacy numeric effect/attribute IDs

`PotionEffectResolver` maps integers 1–39 to namespaced effect names via a hardcoded array. `ModifyAttributeMechanic` uses a hardcoded `switch(id)` for 1–10. Both are **fragile** (any removed/reordered effect silently breaks), not fail-fast per `AGENTS.md` (no `IllegalArgumentException`), and force content authors to memorize numeric IDs. Part 2 proposes migrating the `effect` and `attribute` parameters to accept namespaced keys (`minecraft:regeneration`, `minecraft:movement_speed`). **P1**.

---

## Part 2 — Proposed New Engine Pieces

All proposals satisfy Pillars I–V (§1) and pass the 6-Step Checklist (§5). No per-tick runnables, no NMS, no packet spam.

### P2-1. Ability `trigger` field  *(Schema / Engine enhancement)*

| Field | Value |
|---|---|
| **What** | New top-level key on each `abilities[]` entry: `trigger: "<trigger_key>"` |
| **Purpose** | Gates the ability so `fireAbilities()` only evaluates it when `dispatch()` is called for the matching trigger key. Fixes the guardless-mechanic stacking bug. |
| **Event** | None (routing logic in `SkillEventListener.fireAbilities`) |
| **Parameters** | The trigger key string (e.g. `block_break`, `entity_damage_taken`, `player_interact`) |
| **Why not existing** | Filters (`target`/`tool`/`state`) cannot distinguish event *type* for events that share the same player state (e.g. `is_sneaking` is true during sneak, interact, break, damage-taken, etc.). Only a trigger key on the ability can bind it to one event class. |
| **Register site** | `SkillDefinition.Ability` record gains a `trigger()` field; `fireAbilities()` adds `if (!ability.trigger().equals(triggerKey)) continue;` before evaluating mechanics. |

### P2-2. `core:knockback`  *(Mechanic)*

| Field | Value |
|---|---|
| **YAML key** | `core:knockback` |
| **Purpose** | Applies a directional velocity impulse (knockback) to the damaged entity or all entities in a radius. Backs Shield Bash, Heavy Weapons shove, etc. |
| **Event** | `EntityDamageByEntityEvent` (single target) **or** `PlayerInteractEvent` (radial shove from player). |
| **Parameters** | `force` (double, velocity magnitude), `radius` (double, 0 = single target only), `vertical` (double, upward component, default 0.3) |
| **Behavior** | `target.setVelocity(player.getLocation().getDirection().multiply(force).add(0, vertical, 0))`; for radius > 0 iterate `getNearbyLivingEntities(radius)`. |
| **Why not existing** | No velocity/knockback mechanic exists. `core:thorns_damage` only deals flat damage. |
| **Register** | `mechReg.register("core:knockback", KnockbackMechanic.class, List.of("force","radius","vertical"));` |

### P2-3. `core:shield_disable`  *(Mechanic)*

| Field | Value |
|---|---|
| **YAML key** | `core:shield_disable` |
| **Purpose** | Sets the vanilla shield raise-lockout cooldown (`player.setCooldown(Material.SHIELD, ticks)`) on a target. |
| **Event** | `EntityDamageByEntityEvent` (apply to victim) or `PlayerInteractEvent` (self/area). |
| **Parameters** | `ticks` (double, disable duration in ticks) |
| **Behavior** | If victim is a `Player`, call `victim.setCooldown(Material.SHIELD, ticks)`. Renders the vanilla shield-raised animation/lockout. |
| **Why not existing** | No shield-cooldown mechanic. Cancel/block mechanics negate one hit; they don't disable the shield. |
| **Register** | `mechReg.register("core:shield_disable", ShieldDisableMechanic.class, List.of("ticks"));` |

### P2-4. `core:offhand_strike`  *(Mechanic)*

| Field | Value |
|---|---|
| **YAML key** | `core:offhand_strike` |
| **Purpose** | Deals damage equal to the off-hand weapon's base attack damage to the entity the player is looking at (left-click intent). Enables a genuine two-weapon loop for Dual Wield. |
| **Event** | `PlayerInteractEvent` (left-click block/entity). |
| **Parameters** | `multiplier` (double, scales the offhand item's damage), `reach` (double, max distance, default 4) |
| **Behavior** | Raycast `player.getTargetEntityExact(reach)`; if a `LivingEntity` is found, `target.damage(baseDmg * multiplier, player)` plus a sweep particle. Durability is consumed on the offhand item by 1. |
| **Why not existing** | `core:modify_damage` only modifies an existing `EntityDamageByEntityEvent`; it cannot originate an offhand hit. |
| **Register** | `mechReg.register("core:offhand_strike", OffhandStrikeMechanic.class, List.of("multiplier","reach"));` |

### P2-5. `core:ally_aura`  *(Mechanic)*

| Field | Value |
|---|---|
| **YAML key** | `core:ally_aura` |
| **Purpose** | Applies a potion effect to the player and **nearby players only** (not hostile mobs), consuming a held instrument/item catalyst. Replaces the broken `core:field_aura` usage in Bard/Piety. |
| **Event** | `PlayerInteractEvent` (must be triggered by an active input; see P2-1 trigger field). |
| **Parameters** | `effect` (namespaced key), `radius` (double), `duration` (double, seconds), `amplifier` (double) |
| **Behavior** | Iterate `getNearbyPlayers(radius)`, apply effect. Returns `true` (consume handled by requirements `items: cost`). |
| **Why not existing** | `core:field_aura` hits **all** `LivingEntity` including hostile mobs, has no cost, and fires on every event. It cannot be filtered to players-only. |
| **Register** | `mechReg.register("core:ally_aura", AllyAuraMechanic.class, List.of("effect","radius","duration","amplifier"));` |

### P2-6. `state:equipped`  *(StateFilter)*

| Field | Value |
|---|---|
| **YAML key** | `equipped` (state filter: `equipped:light` / `equipped:medium` / `equipped:heavy` / `equipped:none`) |
| **Purpose** | Verifies the armor *type* worn matches the skill (light=leather, medium=chain/iron/gold/turtle, heavy=diamond/netherite, none=empty slots). Gates armor-skill XP and abilities to the correct gear. |
| **Event** | Any (reads `player.getInventory().getArmorContents()`). |
| **Parameters** | value string after `:`. |
| **Behavior** | Map Material→tier; require **all four** slots to match (or be empty for `none`). |
| **Why not existing** | Only `armor:empty` exists. There is no way to require light/medium/heavy armor, so Heavy Armor grants XP while wearing leather. |
| **Register** | `sf.register("equipped", (p,e,v) -> { … switch(v) … });` |

### P2-7. `resurrect`  *(Trigger)*

| Field | Value |
|---|---|
| **YAML key** | `resurrect` |
| **Purpose** | Fires when a Totem of Undying activates (`EntityResurrectEvent`). Enables L75 totem-synergy passives. |
| **Event** | `EntityResurrectEvent` |
| **Parameters** | none |
| **Behavior** | If `event.getEntity() instanceof Player player` and `event.getHand()` confirms totem use, dispatch. |
| **Why not existing** | No existing trigger covers totem activation. |
| **Register** | `trigReg.register("resurrect", ResurrectTrigger.class);` + `@EventHandler` in `SkillEventListener`. |

### P2-8. `elytra_glide`  *(Trigger)*

| Field | Value |
|---|---|
| **YAML key** | `elytra_glide` |
| **Purpose** | Fires when a player starts/stops gliding with an elytra (`EntityToggleGlideEvent`). Enables Acrobatics L75 synergy and elytra+firework boosts. |
| **Event** | `EntityToggleGlideEvent` |
| **Parameters** | none |
| **Behavior** | If `event.getEntity() instanceof Player` and `event.isGliding()`, dispatch. |
| **Why not existing** | No glide trigger registered. |
| **Register** | `trigReg.register("elytra_glide", ElytraGlideTrigger.class);` |

### P2-9. `core:set_cooldown`  *(Mechanic)*

| Field | Value |
|---|---|
| **YAML key** | `core:set_cooldown` |
| **Purpose** | Sets a visual item-stack cooldown (`player.setCooldown(material, ticks)`) — for Shield Bash self-disable, firework recovery timing, etc. |
| **Event** | `PlayerInteractEvent` or `EntityDamageByEntityEvent`. |
| **Parameters** | `material` (string, e.g. `minecraft:shield`), `ticks` (double) |
| **Behavior** | `player.setCooldown(Material.matchMaterial(material), (int) ticks)`. |
| **Why not existing** | No item-cooldown mechanic. |
| **Register** | `mechReg.register("core:set_cooldown", SetCooldownMechanic.class, List.of("material","ticks"));` |

### P2-10. `core:modify_attack_speed`  *(Mechanic)*

| Field | Value |
|---|---|
| **YAML key** | `core:modify_attack_speed` |
| **Purpose** | Temporarily modifies `ATTACK_SPEED` attribute for a duration. Backs One-Handed "Duelist," Dual Wield rapid strikes, etc. (avoids the fragile numeric `core:modify_attribute` ID 9). |
| **Event** | Any (applies transient modifier). |
| **Parameters** | `multiplier` (double, 1.2 = +20%), `duration` (double, seconds) |
| **Behavior** | Add transient `AttributeModifier` to `ATTACK_SPEED`, schedule removal. |
| **Why not existing** | `core:modify_attribute` can do this via legacy ID 9, but it is uncategorized, fragile, and fires on every event. This is a clean, purpose-built mechanic. |
| **Register** | `mechReg.register("core:modify_attack_speed", ModifyAttackSpeedMechanic.class, List.of("multiplier","duration"));` |

### P2-11. Namespaced `effect` / `attribute` parameter migration  *(Parameter format)*

| Field | Value |
|---|---|
| **What** | Accept namespaced-key **strings** (e.g. `"minecraft:regeneration"`, `"minecraft:movement_speed"`) for the `effect` parameter (on `apply_status`, `aoe_effect`, `field_aura`, `crowd_control`, `ally_aura`) and the `attribute` parameter (on `modify_attribute`). |
| **Purpose** | Eliminates fragile legacy numeric IDs; fail-fast `IllegalArgumentException` on unknown keys (per `AGENTS.md`). |
| **Behavior** | `PotionEffectResolver.resolve` and `ModifyAttributeMechanic` detect string→`Registry.POTION_EFFECT_TYPE.get(key)` / `Registry.ATTRIBUTE.get(key)`; fall back to numeric for backward compat with a deprecation warning. |
| **Why not existing** | Both currently only accept numeric `double` via the evaluator output. |

### P2-12. `requirements.cooldown` evaluator support  *(Schema enhancement)*

| Field | Value |
|---|---|
| **What** | Allow the `cooldown` field under `requirements` to accept evaluator syntax (linear/milestones) in addition to a flat double, enabling inverse-CD sub-scaling per §2B. |
| **Purpose** | Satisfies §6 (sub-scaling test) for active abilities whose only natural growth axis is cooldown reduction. |
| **Register** | `RequirementEngine` evaluates the cooldown evaluator against `(level, unlockLevel)` before the CD check. |

---

## Part 3 — Cross-Skill Summary

### 3.1 Recurring Bug Classes

| # | Class | Prevalence | Severity |
|---|---|---|---|
| C1 | **Unguarded-mechanic stacking** — `speed_bonus`/`armor_bonus`/`knockback_resist`/`modify_jump`/`haste_effect`/`field_aura` fire on every dispatch and stack transient attribute modifiers | ~25 abilities across 28 skills | **P0** (stat inflation + TPS) |
| C2 | **No per-ability `trigger` field** — abilities cannot bind to one event class | engine-wide | **P0** |
| C3 | **`target` filter on melee `entity_damage` returns null** → ability/XP never fires | light_weapons, heavy_weapons (2 skills, fully non-functional) | **P0** |
| C4 | **`player_interact` XP with no guard** — XP for existing (every interaction) | bard, wizardry | **P0** |
| C5 | **Free-magic abilities with no requirements** — projectiles/teleports/auras fire on every interact with no cooldown/state/item cost | wizardry (arcane_missile, blink), bard (all auras), piety (all auras) | **P1** (Pillar III) |
| C6 | **`field_aura`/`aoe_effect` buff hostile mobs** — applies potion to ALL nearby LivingEntity | bard, piety, wizardry | **P1** |
| C7 | **Haste ≠ placement/draw speed** — `haste_effect` only affects break/dig, used for block-place, bow-draw, herb-harvest claims | building, masonry, archery | **P1** (lens D) |
| C8 | **Legacy numeric effect/attribute IDs** — fragile, undocumented for authors | all status/attribute mechanics | **P1** |
| C9 | **L25 is passive, not primary active** — §3 milestone role violated (no Shift+RClick hook) | herbalism, bard, archery, throwing, unarmed, acrobatics, riding, dual_wield, one_handed, light_weapons, heavy_weapons, shields, smithing | **P2** |
| C10 | **L100 capstone is a flat constant** (no sub-scaling) — §2B/§6 violated | mining, excavation, carpentry, woodcutting, farming, herbalism, fishing, alchemy, enchanting, smithing, tailoring, light_armor, medium_armor, heavy_armor, unarmored, shields, light_weapons, one_handed, heavy_weapons, dual_wield, throwing, archery, unarmed, acrobatics, riding, bard, wizardry, piety, building, masonry, cooking, husbandry | **P2** |
| C11 | **XP source unbounded** — generic `block_place`, `craft_item`, `furnace_extract`, `entity_damage_taken`, `sprint`, `sneak` with no targeted filters | building, carpentry, cooking, smithing, tailoring, all armor skills, acrobatics, enchanting, husbandry | **P2** |
| C12 | **Armor skills don't verify worn type** — Heavy Armor XP/abilities trigger in leather | light_armor, medium_armor, heavy_armor | **P2** |
| C13 | **`modify_craft_output` affects ALL recipes** — no target filter for skill-specific items | every L100 "master" crafter | **P2** |
| C14 | **Description ↔ behavior mismatch** — lore claims effects the mechanic doesn't deliver | one_handed (duelist: lifesteal≠atk spd), archery (quick_reload: haste≠bow draw), enchanting (speed_bonus≠orb attract), excavation L100 (area_harvest≠place), tailoring (yield_mult≠craft) | **P1** |

### 3.2 Prioritized Fix Order

**P0 — Functional correctness (do first):**
1. Implement **P2-1** (`trigger` field on abilities) — unblocks all guardless-mechanic fixes.
2. Fix **light_weapons / heavy_weapons** `target:` → `tool:` (C3).
3. Fix **wizardry arcane_missile / blink** — add requirements (cooldown, is_sneaking, item catalyst, exhaustion). Remove free-magic.
4. Fix **bard / wizardry / piety** `player_interact` XP — add item/state filters or move to instrument-cost triggers.
5. Fix **field_aura buffing mobs** — gate behind P2-5 `core:ally_aura` for Bard/Piety; or add `players_only` flag.

**P1 — Pillar & correctness violations:**
6. Migrate **legacy numeric IDs → namespaced** (P2-11).
7. Replace **haste-as-placement/draw** abilities with correct mechanics or redesign (building, masonry, archery).
8. Add **item economy** to wizardry/bard/piety (Pillar III).
9. Implement **P2-6 `state:equipped`** and gate armor skills.

**P2 — Milestone & sub-scaling:**
10. Convert all **L25 slots** to active abilities with native input (§4) + requirements.
11. Convert all **L100 capstones** from constants to sub-scaling evaluators.
12. Introduce **P2-12** (cooldown evaluator) for inverse-CD sub-scaling.

**P3 — New engine pieces (post-stabilization):**
13. `core:knockback`, `core:shield_disable`, `core:offhand_strike`, `resurrect` trigger, `elytra_glide` trigger, `core:set_cooldown`, `core:modify_attack_speed` — implement and register per Part 2.

### 3.3 Framework / `capabilities.md` Corrections Required

| Location | Correction |
|---|---|
| `capabilities.md` → `core:speed_bonus` | Documented Event `PlayerToggleSprintEvent` is **wrong** — the implementation has no event guard. Either add the guard or remove the claim. |
| `capabilities.md` → `core:haste_effect` | Documented Event `BlockBreakEvent` is **wrong** — implementation fires on all events. |
| `capabilities.md` → `core:field_aura` / `core:aoe_effect` / `core:xp_bonus` | "Varies (triggered by ability activation)" is misleading — they fire on **every** dispatch. Must be gated by the new `trigger` field. |
| `capabilities.md` → effect/attribute params | Must document the namespaced-key migration (P2-11) and deprecate numeric form. |
| `docs/creating-skills.md` → abilities schema | Must add the `trigger` field (P2-1) and document its necessity. |
| `docs/creating-skills.md` → filters | Must note `target:` on melee `entity_damage` matches the **damager projectile material only**, not the held tool — `tool:` is correct for held-weapon gating. |
| `Skilling.java` L275 | `core:thorns_damage` param list is `List.of("damage")` but `capabilities.md` says `percentage`. Pick one. |
| `docs/capabilities.md` → `core:block_damage` vs `core:cancel_damage` | Both negate damage; `block_damage` and `cancel_damage` are nearly identical `BaseDamageCancelMechanic` subclasses with the same `chance` param. Document the semantic difference (or merge them). |

---
---

## Part 1 — Per-Skill Audit

> **Convention for redesigns:** Every ability includes a `trigger:` field (P2-1). Legacy numeric effect IDs are replaced with namespaced keys (P2-11). L25 abilities include `requirements` with native input (§4). L100 capstones use sub-scaling evaluators (§6). New mechanic/filter keys introduced below are defined in Part 2 and referenced as `(P2-N)`.

(file continuation — Part 1 follows; assembled below)
### building.yml — Building

**Issues**
1. [BUG][D][G] `scaffolding` L1 lore "place blocks {amplifier}x faster" but `core:haste_effect` only increases **break/dig** speed, never placement. Haste also has no event guard → re-applies on every dispatched event while sneaking. *(Known issue #1.)*
2. [BUG][G] `swift_build` L25 `core:speed_bonus` — no event guard, no filters → stacks `MOVEMENT_SPEED` modifiers on **every** event (sprint, sneak, break, interact, damage…). Severe stat inflation.
3. [BUG][G] `reinforced_structure` L50 `core:modify_attribute` (knockback resist, ID 3) — no event guard, no filters → stacks `KNOCKBACK_RESISTANCE` modifiers on every event.
4. [BUG][A][VIOLATION-I][D] `architects_vision` L75 lore "sneak-paste to place a radius of blocks" but uses `core:area_harvest` which **breaks** blocks (checks `BlockBreakEvent`) — there is no placement-AOE mechanic. Pillar I: ability cannot function as described. Needs `core:place_assist` (not proposed — redesign to a break-based or buff-based ability instead).
5. [SHORTCOMING][F] `master_builder` L100 — `constant: 2.5`, no sub-scaling; `modify_craft_output` affects **all** recipes, not decorative blocks only.
6. [BUG][C] `xp_sources`: `block_place` 2.0 XP for **any** block; `craft_item` 6.0 for **any** craft. No filters → XP for trivial actions (placing dirt, crafting sticks).
7. [SHORTCOMING][F] `structural_knowledge` L15 `core:xp_bonus` has no event guard; multiplier grows but applies to all XP, not building-specific.

**Redesign**

```yaml
id: "building"
max_level: 100
display:
  name: "Building"
  icon: "minecraft:bricks"
  color: "YELLOW"
  style: "SEGMENTED_10"
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "block_place"
    filters:
      - target: "#c:stone_products"
    reward: { constant: 2.0 }
  - trigger: "craft_item"
    filters:
      - target: "#minecraft:stone_bricks"      # decorative block crafts only
    reward: { constant: 6.0 }
abilities:
  - id: "scaffolding"
    display_name: "Scaffolding"
    unlock_level: 1
    trigger: "block_place"           # P2-1: only fires on placement
    display:
      lore:
        - "&7Placing decorative blocks grants &a{amplifier}&7 Haste"
        - "&7(mining/digging speed) for {duration}s to speed teardown."
    mechanics:
      - type: "core:haste_effect"     # honestly described: Haste = break speed
        parameters:
          amplifier: { linear: { base: 0, step: 0.01, max: 2 } }
          duration: { constant: 30.0 }
    feedback: { notify: { action_bar: false } }
  - id: "structural_knowledge"
    display_name: "Structural Knowledge"
    unlock_level: 15
    trigger: "block_place"
    display:
      lore: [ "&7Bonus XP from building &a{multiplier}x&7." ]
    mechanics:
      - type: "core:xp_bonus"
        filters:
          - target: "#c:stone_products"
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.01, max: 1.5 } }
    feedback: { notify: { action_bar: false } }
  - id: "swift_build"
    display_name: "Swift Build"
    unlock_level: 25
    trigger: "block_place"
    display:
      lore:
        - "&7Shift+Place to gain &a{multiplier}x&7 move speed for {duration}s."
        - "&8Requires: Sneaking, holding a decorative block."
    requirements:
      cooldown: 12.0
      state: [ "is_sneaking" ]
      items:
        - { action: "possession", tag: "#c:stone_products", slot: "MAIN_HAND" }
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure:
      cooldown: { action_bar: "&eSwift Build cooling down: {time}s" }
      exhaustion: { action_bar: "&cToo exhausted to build!" }
    mechanics:
      - type: "core:speed_bonus"
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.003, max: 1.35 } }
          duration: { linear: { base: 8.0, step: 0.1, max: 20.0 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&e✦ Swift Build! ✦" } }
  - id: "reinforced_structure"
    display_name: "Reinforced Structure"
    unlock_level: 50
    trigger: "block_place"
    display:
      lore: [ "&7While standing on a placed block, gain &a{amount}&7 knockback resistance." ]
    mechanics:
      - type: "core:knockback_resist"
        parameters:
          amount: { linear: { base: 0.1, step: 0.005, max: 0.6 } }
          duration: { constant: 10.0 }
    feedback: { notify: { action_bar: false } }
  - id: "architects_vision"
    display_name: "Architect's Vision"
    unlock_level: 75
    trigger: "block_place"
    display:
      lore:
        - "&7Shift+Place a decorative block to instantly"
        - "&7break &a{max_blocks}&7 adjacent decorative blocks for re-use."
        - "&7Restores block to inventory for rapid re-placement."
    requirements:
      cooldown: 15.0
      state: [ "is_sneaking" ]
      items:
        - { action: "possession", tag: "#c:stone_products", slot: "MAIN_HAND" }
      exhaustion: { amount: 4.0, minimum: 6.0 }
    on_failure:
      cooldown: { action_bar: "&eArchitect's Vision cooling down: {time}s" }
      exhaustion: { action_bar: "&cToo exhausted to build!" }
    mechanics:
      - type: "core:area_harvest"     # now honestly a teardown-for-rebuild tool
        parameters:
          radius: { milestones: { 75: 1, 85: 2, 95: 3, 100: 4 } }
          max_blocks: { linear: { base: 16.0, step: 1.0, max: 64.0 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&e✦ Architect's Vision ✦" } }
  - id: "master_builder"
    display_name: "Master Builder"
    unlock_level: 100
    trigger: "craft_item"
    display:
      lore: [ "&7Decorative block crafts yield &a{multiplier}x&7 output." ]
    mechanics:
      - type: "core:modify_craft_output"
        filters:
          - target: "#minecraft:stone_bricks"
        parameters:
          multiplier: { linear: { base: 1.5, step: 0.01, max: 2.5 } }
    feedback: { notify: { action_bar: false } }
```

---

### masonry.yml — Masonry

**Issues**
1. [BUG][D][G] `geometric_eye` L50 — `core:haste_effect` filtered `target: #c:stone_products` and lore "place stone blocks faster." Haste = break/dig speed only; filtered to BlockBreakEvent material; fires on every event (no guard). Same haste-as-placement bug.
2. [SHORTCOMING][F] `master_mason` L100 `constant: 3.0` on **all** crafts, no sub-scaling, no target filter.
3. [BUG][C] `furnace_extract` 5.0 XP has no filter → XP for **any** furnace product (food, iron, etc.), not just stone/clay.
4. [BUG][C] `block_place` filter `#c:stone_products` — resolves correctly on BlockPlaceEvent (block material). OK.
5. [SHORTCOMING][F] `glaze_artisan` L75 and `stone_folding` L1 both `modify_furnace_output` — redundant scaling on same mechanic; no distinct identity.

**Redesign**

```yaml
id: "masonry"
max_level: 100
display: { name: "Masonry", icon: "minecraft:stone_bricks", color: "YELLOW", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "furnace_extract"
    filters:
      - target: "minecraft:terracotta"
    reward: { constant: 5.0 }
  - trigger: "furnace_extract"
    filters:
      - target: "minecraft:glass"
    reward: { constant: 4.0 }
  - trigger: "block_place"
    filters: [ { target: "#c:stone_products" } ]
    reward: { constant: 1.0 }
abilities:
  - id: "stone_folding"
    display_name: "Stone Folding"
    unlock_level: 1
    trigger: "furnace_extract"
    display: { lore: [ "&7Smelting stone/clay yields &a{multiplier}x&7 output." ] }
    mechanics:
      - type: "core:modify_furnace_output"
        filters: [ { target: "minecraft:smooth_stone" } ]
        parameters:
          multiplier: { linear: { base: 1.0, step: 0.005, max: 1.5 } }
    feedback: { notify: { action_bar: false } }
  - id: "kiln_master"
    display_name: "Kiln Master"
    unlock_level: 15
    trigger: "furnace_extract"
    display: { lore: [ "&7Bonus XP from smelting stone &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:xp_bonus"
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.01, max: 1.6 } }
    feedback: { notify: { action_bar: false } }
  - id: "reinforced_walls"
    display_name: "Reinforced Walls"
    unlock_level: 25
    trigger: "craft_item"
    display:
      lore:
        - "&7Shift+Craft stone bricks to gain &a{multiplier}x&7 output."
        - "&8Requires: Sneaking."
    requirements:
      cooldown: 8.0
      state: [ "is_sneaking" ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&eReinforced Walls cooling down: {time}s" } }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "#minecraft:stone_bricks" } ]
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.01, max: 1.75 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&e✦ Reinforced Walls! ✦" } }
  - id: "geometric_eye"
    display_name: "Geometric Eye"
    unlock_level: 50
    trigger: "block_break"
    display:
      lore: [ "&7Breaking stone with a pick grants &a{amplifier}&7 Haste for {duration}s." ]
    mechanics:
      - type: "core:haste_effect"
        filters: [ { tool: "#minecraft:pickaxes" }, { target: "#c:stone" } ]
        parameters:
          amplifier: { linear: { base: 0, step: 0.01, max: 2 } }
          duration: { constant: 30.0 }
    feedback: { notify: { action_bar: false } }
  - id: "glaze_artisan"
    display_name: "Glaze Artisan"
    unlock_level: 75
    trigger: "furnace_extract"
    display: { lore: [ "&7Terracotta & glass output &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_furnace_output"
        filters: [ { target: "minecraft:terracotta" } ]
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.01, max: 2.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "master_mason"
    display_name: "Master Mason"
    unlock_level: 100
    trigger: "craft_item"
    display: { lore: [ "&7Stone brick crafts yield &a{multiplier}x&7 output." ] }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "#minecraft:stone_bricks" } ]
        parameters:
          multiplier: { linear: { base: 2.0, step: 0.01, max: 3.0 } }
    feedback: { notify: { action_bar: false } }
```

---

### carpentry.yml — Carpentry

**Issues**
1. [BUG][C] `craft_item` 8.0 XP with **no filter** → XP for crafting any item (swords, iron, etc.).
2. [BUG][G] `iron_bark_touch` L75 `core:armor_bonus` + `tool:#minecraft:axes` → no event guard; fires on every event while holding an axe, stacking `ARMOR` modifiers.
3. [SHORTCOMING][F] `master_joiner` L100 `constant: 2.0` on **all** crafts, no sub-scaling, no wood filter.
4. [DESIGN][F] `wood_weave` L50 and `efficient_crafting` L1 are both `modify_craft_output` with no target filter — identical mechanic repeated.

**Redesign**

```yaml
id: "carpentry"
max_level: 100
display: { name: "Carpentry", icon: "minecraft:crafting_table", color: "YELLOW", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "craft_item"
    filters: [ { target: "#minecraft:planks" } ]
    reward: { constant: 8.0 }
  - trigger: "block_break"
    filters: [ { target: "#minecraft:logs" }, { state: "player_placed:false" } ]
    reward: { constant: 2.0 }
abilities:
  - id: "efficient_crafting"
    display_name: "Efficient Crafting"
    unlock_level: 1
    trigger: "craft_item"
    display: { lore: [ "&7Plank & fence crafts yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "#minecraft:planks" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.005, max: 1.5 } }
    feedback: { notify: { action_bar: false } }
  - id: "precise_cut"
    display_name: "Precise Cut"
    unlock_level: 15
    trigger: "item_damage"
    display: { lore: [ "&7Axes have &a{chance}%&7 chance to lose no durability." ] }
    mechanics:
      - type: "core:durability_save"
        filters: [ { tool: "#minecraft:axes" } ]
        parameters:
          chance: { linear: { base: 15.0, step: 0.4, max: 60.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "timber_reclaim"
    display_name: "Timber Reclaim"
    unlock_level: 25
    trigger: "block_break"
    display:
      lore:
        - "&7Shift+fell a tree to double drops &a{yield_chance}%&7."
        - "&8Requires: Sneaking, axe in hand."
    requirements:
      cooldown: 6.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "#minecraft:axes", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&eTimber Reclaim cooling down: {time}s" } }
    mechanics:
      - type: "core:yield_multiplier"
        filters: [ { target: "#minecraft:logs" } ]
        parameters:
          yield_chance: { linear: { base: 10.0, step: 0.4, max: 50.0 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&6✦ Timber Reclaim! ✦" } }
  - id: "wood_weave"
    display_name: "Wood Weave"
    unlock_level: 50
    trigger: "craft_item"
    display: { lore: [ "&7Fence/gate/trapdoor crafts yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "#minecraft:fences" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.01, max: 2.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "iron_bark_touch"
    display_name: "Iron Bark Touch"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&7While holding an axe, gain &a{amount}&7 armor on hit." ] }
    mechanics:
      - type: "core:armor_bonus"
        filters: [ { tool: "#minecraft:axes" } ]
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "master_joiner"
    display_name: "Master Joiner"
    unlock_level: 100
    trigger: "craft_item"
    display: { lore: [ "&7All wood crafts yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "#minecraft:planks" } ]
        parameters:
          multiplier: { linear: { base: 1.8, step: 0.01, max: 2.5 } }
    feedback: { notify: { action_bar: false } }
```

---

### woodcutting.yml — Woodcutting

**Issues**
1. [BUG][G] `efficient_swing` L15 `core:haste_effect` + `tool:#minecraft:axes` → no event guard; re-applies Haste on every dispatched event while an axe is held. (Lore "axe swing speed" is broadly correct since Haste does speed log-breaking — but the firing scope is wrong.)
2. [BUG][G] `iron_bark` L75 `core:armor_bonus` + `tool:#minecraft:axes` → no event guard; stacks `ARMOR` on every event while holding an axe.
3. [SHORTCOMING][F] `ancient_timber` L100 `constant: 2.0` on plank crafts only (filter present — good), but no sub-scaling.
4. [DESIGN][E-I] `timber_feller` L25 is well-gated (sneak + cooldown + axe + exhaustion) and uses `chain_break` via real `BlockBreakEvent` passes — satisfies Pillar I. Good baseline.
5. [BUG][C] `forest_bounty` L50 `core:yield_multiplier` + `target:#minecraft:leaves` — runs on BlockBreakEvent ✓; but `leaves` is not in `#minecraft:logs`. Chain-break does not apply (it uses `yield_multiplier`, not `chain_break`). OK. Minor: no `player_placed:false` guard → XP for player-placed leaves (sapling farms exploit).

**Redesign**

```yaml
id: "woodcutting"
max_level: 100
display: { name: "Woodcutting", icon: "minecraft:iron_axe", color: "GREEN", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "block_break"
    filters: [ { target: "#minecraft:logs" }, { state: "player_placed:false" } ]
    reward: { constant: 7.0 }
abilities:
  - id: "lumberjack"
    display_name: "Lumberjack"
    unlock_level: 1
    trigger: "block_break"
    display: { lore: [ "&7Log yield +&a{yield_chance}%&7." ] }
    mechanics:
      - type: "core:yield_multiplier"
        filters: [ { target: "#minecraft:logs" }, { tool: "#minecraft:axes" } ]
        parameters:
          yield_chance: { linear: { base: 0.5, step: 0.5, max: 50.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "efficient_swing"
    display_name: "Efficient Swing"
    unlock_level: 15
    trigger: "block_break"
    display: { lore: [ "&7Breaking logs grants &a{amplifier}&7 Haste for {duration}s." ] }
    mechanics:
      - type: "core:haste_effect"
        filters: [ { target: "#minecraft:logs" }, { tool: "#minecraft:axes" } ]
        parameters:
          amplifier: { linear: { base: 0, step: 0.01, max: 2 } }
          duration: { constant: 20.0 }
    feedback: { notify: { action_bar: false } }
  - id: "timber_feller"
    display_name: "Timber Feller"
    unlock_level: 25
    trigger: "block_break"
    display:
      lore:
        - "&7Shift+mine to fell up to &a{chain_limit}&7 connected logs."
        - "&7Costs 2 hunger."
        - "&8Requires: Sneaking, axe."
    requirements:
      cooldown: 10.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "#minecraft:axes", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 2.0, minimum: 3.0 }
    on_failure:
      cooldown: { action_bar: "&cTimber Feller cooling down: {time}s" }
      exhaustion: { action_bar: "&cToo exhausted!" }
    mechanics:
      - type: "core:chain_break"
        filters: [ { target: "#minecraft:logs" } ]
        parameters:
          chain_limit: { milestones: { 25: 3, 50: 8, 75: 16, 100: 32 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&2✦ Timber Feller! ✦" }
      sounds: [ { type: "ENTITY_ZOMBIE_BREAK_WOODEN_DOOR", volume: 0.5, pitch: 1.2, target: "self" } ]
  - id: "forest_bounty"
    display_name: "Forest Bounty"
    unlock_level: 50
    trigger: "block_break"
    display: { lore: [ "&7Leaf breaks yield +&a{yield_chance}%&7 sapling drops." ] }
    mechanics:
      - type: "core:yield_multiplier"
        filters: [ { target: "#minecraft:leaves" }, { state: "player_placed:false" } ]
        parameters:
          yield_chance: { linear: { base: 2.0, step: 0.05, max: 8.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "iron_bark"
    display_name: "Iron Bark"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&7While wielding an axe, gain &a{amount}&7 armor on hit." ] }
    mechanics:
      - type: "core:armor_bonus"
        filters: [ { tool: "#minecraft:axes" } ]
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "ancient_timber"
    display_name: "Ancient Timber"
    unlock_level: 100
    trigger: "craft_item"
    display: { lore: [ "&7Plank crafts yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "#minecraft:planks" } ]
        parameters:
          multiplier: { linear: { base: 1.8, step: 0.01, max: 2.5 } }
    feedback: { notify: { action_bar: false } }
```

---

### mining.yml — Mining

**Issues**
1. [BUG][G] `sturdy_miner` L75 `core:armor_bonus` + `tool:#minecraft:pickaxes` → no event guard; stacks `ARMOR` on every event while holding a pick.
2. [SHORTCOMING][F] `perfect_yield` L100 `constant: 100.0` — binary guaranteed-doubles; no sub-scaling, no triple component despite lore claiming "chance for triple."
3. [BUG][D] `perfect_yield` lore "chance for triple" but `yield_multiplier` only doubles drops — no triple mechanic available.
4. [DESIGN][E-I] `vein_miner` is well-gated (sneak + cooldown + coal cost + exhaustion) and uses `chain_break` issuing real `BlockBreakEvent`s — Pillar I compliant. Good baseline.
5. [BUG][C] `prospector` L15 `core:xp_bonus` has no target filter → multiplier applies to **all** XP sources, not mining only.

**Redesign**

```yaml
id: "mining"
max_level: 100
display: { name: "Mining", icon: "minecraft:iron_pickaxe", color: "GREEN", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "block_break"
    filters: [ { target: "#c:ores" }, { state: "player_placed:false" } ]
    reward: { constant: 15.0 }
  - trigger: "block_break"
    filters: [ { target: "#c:stone" }, { state: "player_placed:false" } ]
    reward: { constant: 2.0 }
abilities:
  - id: "geologist"
    display_name: "Geologist"
    unlock_level: 1
    trigger: "block_break"
    display: { lore: [ "&7Ore yield +&a{yield_chance}%&7." ] }
    mechanics:
      - type: "core:yield_multiplier"
        filters: [ { target: "#c:ores" }, { tool: "#minecraft:pickaxes" } ]
        parameters:
          yield_chance: { linear: { base: 0.5, step: 0.5, max: 50.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "prospector"
    display_name: "Prospector"
    unlock_level: 15
    trigger: "block_break"
    display: { lore: [ "&7Bonus ore XP &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:xp_bonus"
        filters: [ { target: "#c:ores" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.01, max: 1.6 } }
    feedback: { notify: { action_bar: false } }
  - id: "vein_miner"
    display_name: "Vein Miner"
    unlock_level: 25
    trigger: "block_break"
    display:
      lore:
        - "&7Sneak-mine to break up to &a{chain_limit}&7 connected ore/stone."
        - "&7Costs 2 hunger + 1 Coal."
        - "&8Requires: Sneaking, pickaxe."
    requirements:
      cooldown: 5.0
      state: [ "is_sneaking" ]
      items:
        - { action: "possession", tag: "#minecraft:pickaxes", slot: "MAIN_HAND" }
        - { action: "cost", tag: "minecraft:coal", amount: 1 }
      exhaustion: { amount: 2.0, minimum: 3.0 }
    on_failure:
      cooldown: { action_bar: "&cVein Miner cooling down: {time}s" }
      missing_item: { action_bar: "&cRequires 1x Coal to power Vein Miner!" }
      exhaustion: { action_bar: "&cToo exhausted!" }
    mechanics:
      - type: "core:chain_break"
        filters: [ { target: "#c:veinminer" } ]
        parameters:
          chain_limit: { milestones: { 25: 3, 50: 8, 75: 16, 100: 32 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&b✦ Vein Miner! ✦" }
      sounds: [ { type: "ENTITY_ZOMBIE_BREAK_WOODEN_DOOR", volume: 0.5, pitch: 1.2, target: "self" } ]
  - id: "magma_forge"
    display_name: "Magma Forge"
    unlock_level: 50
    trigger: "block_break"
    display: { lore: [ "&7Ores have &a{chance}%&7 chance to auto-smelt." ] }
    mechanics:
      - type: "core:auto_smelt"
        filters: [ { target: "#c:ores" } ]
        parameters:
          chance: { linear: { base: 25.0, step: 0.5, max: 75.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "sturdy_miner"
    display_name: "Sturdy Miner"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&7While holding a pick, gain &a{amount}&7 armor on hit." ] }
    mechanics:
      - type: "core:armor_bonus"
        filters: [ { tool: "#minecraft:pickaxes" } ]
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "perfect_yield"
    display_name: "Perfect Yield"
    unlock_level: 100
    trigger: "block_break"
    display: { lore: [ "&7Ore breaks always double; &a{yield_chance}%&7 chance to triple." ] }
    mechanics:
      - type: "core:yield_multiplier"
        filters: [ { target: "#c:ores" } ]
        parameters:
          yield_chance: { constant: 100.0 }   # guaranteed double (binary = justified capstone identity)
      - type: "core:yield_multiplier"
        filters: [ { target: "#c:ores" } ]
        parameters:
          yield_chance: { linear: { base: 10.0, step: 0.9, max: 80.0 } }  # sub-scaling triple chance
    feedback: { notify: { action_bar: false } }
```

---

### excavation.yml — Excavation

**Issues**
1. [BUG][F] `wide_sweep` L25 milestones are `25:1, 50:1, 75:2, 100:2` — L25 and L50 identical (radius 1), L75 and L100 identical (radius 2). No growth between consecutive tiers violates §2B.
2. [BUG][A][F] `excavator` L100 is a **second** `core:area_harvest` ability that fires **independently** on every BlockBreakEvent alongside `wide_sweep` → double area-break with no requirements. Also lore "Wide Sweep cooldown removed" is impossible (the engine cannot remove another ability's cooldown). Constant radius, no sub-scaling.
3. [BUG][G] `quick_dig` L15 and `swift_digger` L75 — both `core:haste_effect` + `tool:#minecraft:shovels`; no event guard → re-applies Haste on every event while shovels held.
4. [BUG][C] `fossil_hunter` L50 filters `target:#minecraft:sand` only — lore claims "gravel and sand" but gravel is excluded.
5. [DESIGN][E-I] `wide_sweep` L25 is well-gated (sneak + cooldown + shovel + exhaustion + area_harvest). Good.

**Redesign**

```yaml
id: "excavation"
max_level: 100
display: { name: "Excavation", icon: "minecraft:iron_shovel", color: "GREEN", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "block_break"
    filters: [ { target: "#c:excavatable" }, { state: "player_placed:false" } ]
    reward: { constant: 3.0 }
abilities:
  - id: "dig_double"
    display_name: "Dig Double"
    unlock_level: 1
    trigger: "block_break"
    display: { lore: [ "&7Dirt/gravel/sand yield +&a{yield_chance}%&7." ] }
    mechanics:
      - type: "core:yield_multiplier"
        filters: [ { target: "#c:excavatable" }, { tool: "#minecraft:shovels" } ]
        parameters:
          yield_chance: { linear: { base: 0.5, step: 0.5, max: 50.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "quick_dig"
    display_name: "Quick Dig"
    unlock_level: 15
    trigger: "block_break"
    display: { lore: [ "&7Soft-block breaks grant &a{amplifier}&7 Haste for {duration}s." ] }
    mechanics:
      - type: "core:haste_effect"
        filters: [ { tool: "#minecraft:shovels" }, { target: "#c:excavatable" } ]
        parameters:
          amplifier: { linear: { base: 0, step: 0.01, max: 2 } }
          duration: { constant: 20.0 }
    feedback: { notify: { action_bar: false } }
  - id: "wide_sweep"
    display_name: "Wide Sweep"
    unlock_level: 25
    trigger: "block_break"
    display:
      lore:
        - "&7Sneak-mine to break a &a{radius}&7-block radius of soft blocks (max {max_blocks})."
        - "&8Requires: Sneaking, shovel."
    requirements:
      cooldown: 10.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "#minecraft:shovels", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&cWide Sweep cooling down: {time}s" } }
    mechanics:
      - type: "core:area_harvest"
        filters: [ { target: "#c:excavatable" } ]
        parameters:
          radius: { linear: { base: 1.0, step: 0.02, max: 3.0 } }   # continuous growth 25→100
          max_blocks: { linear: { base: 9.0, step: 0.5, max: 49.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&e✦ Wide Sweep! ✦" }
      sounds: [ { type: "ENTITY_ZOMBIE_BREAK_WOODEN_DOOR", volume: 0.5, pitch: 1.5, target: "self" } ]
  - id: "fossil_hunter"
    display_name: "Fossil Hunter"
    unlock_level: 50
    trigger: "block_break"
    display: { lore: [ "&7Sand & gravel breaks yield +&a{yield_chance}%&7 flint/bone." ] }
    mechanics:
      - type: "core:yield_multiplier"
        filters:
          - target: "#minecraft:sand"            # tag covers sand + red sand
      - type: "core:yield_multiplier"
        filters: [ { target: "minecraft:gravel" } ]
        parameters:
          yield_chance: { linear: { base: 2.0, step: 0.05, max: 8.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "swift_digger"
    display_name: "Swift Digger"
    unlock_level: 75
    trigger: "block_break"
    display: { lore: [ "&7Haste amplifier &a{amplifier}&7 for {duration}s on soft breaks." ] }
    mechanics:
      - type: "core:haste_effect"
        filters: [ { tool: "#minecraft:shovels" }, { target: "#c:excavatable" } ]
        parameters:
          amplifier: { linear: { base: 2, step: 0.02, max: 4 } }
          duration: { constant: 30.0 }
    feedback: { notify: { action_bar: false } }
  - id: "excavator"
    display_name: "Excavator"
    unlock_level: 100
    trigger: "block_break"
    display: { lore: [ "&7Wide Sweep radius grows to &a{radius}&7 (max {max_blocks} blocks) passively." ] }
    mechanics:
      - type: "core:area_harvest"
        filters: [ { target: "#c:excavatable" } ]
        parameters:
          radius: { linear: { base: 2.0, step: 0.02, max: 3.0 } }
          max_blocks: { linear: { base: 25.0, step: 0.5, max: 49.0 } }
    feedback: { notify: { action_bar: false } }
```

---

### farming.yml — Farming

**Issues**
1. [BUG][C] `crop_grow` XP 2.0 with **no filter** → grants XP to **all nearby players** within `cropGrowRadius` when **any** crop grows (wheat, melon stem, kelp, etc.). Players who didn't plant get XP; multiple players near a farm all gain.
2. [BUG][C] `block_break` XP 4.0 `#minecraft:crops` lacks `player_placed:false` → XP for breaking player-placed crops (auto-farm exploit). Note: many crops don't retain PDC, but the guard should still be present.
3. [SHORTCOMING][F] `living_earth` L100 `core:auto_replant` — binary (replants or not), no parameter to sub-scale. No sub-scaling at L100 violates §6.
4. [BUG][G] `seasoned_hand` L15 and `hearty_meal` L75 both `core:saturation_inject` — check event guard (`PlayerItemConsumeEvent`). Confirmed guarded (per `SaturationInjectMechanic`). But both target **all food** with no filter — `hearty_meal` should target cooked-food only.
5. [DESIGN][A] `harvest_wave` L25 `chain_break` + `#minecraft:crops` — chain-break issues real `BlockBreakEvent`s, which recursively trigger other `block_break` listeners (XP re-grant loop). Acceptable since `CHAINING_PLAYERS` re-entrancy guard exists.

**Redesign**

```yaml
id: "farming"
max_level: 100
display: { name: "Farming", icon: "minecraft:iron_hoe", color: "GREEN", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "block_break"
    filters: [ { target: "#minecraft:crops" }, { state: "player_placed:false" } ]
    reward: { constant: 4.0 }
  # crop_grow removed: unbounded free XP source — replaced by harvest XP only
abilities:
  - id: "green_thumb"
    display_name: "Green Thumb"
    unlock_level: 1
    trigger: "block_break"
    display: { lore: [ "&7Crop yield +&a{yield_chance}%&7." ] }
    mechanics:
      - type: "core:yield_multiplier"
        filters: [ { target: "#minecraft:crops" }, { tool: "#minecraft:hoes" } ]
        parameters:
          yield_chance: { linear: { base: 0.5, step: 0.5, max: 50.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "seasoned_hand"
    display_name: "Seasoned Hand"
    unlock_level: 15
    trigger: "consume_item"
    display: { lore: [ "&7Eating raw crops grants +&a{saturation}&7 saturation." ] }
    mechanics:
      - type: "core:saturation_inject"
        filters: [ { target: "#minecraft:crops" } ]
        parameters:
          saturation: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "harvest_wave"
    display_name: "Harvest Wave"
    unlock_level: 25
    trigger: "block_break"
    display:
      lore:
        - "&7Sneak-harvest up to &a{chain_limit}&7 connected crops."
        - "&8Requires: Sneaking, hoe."
    requirements:
      cooldown: 8.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "#minecraft:hoes", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 0.5, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&cHarvest Wave cooling down: {time}s" } }
    mechanics:
      - type: "core:chain_break"
        filters: [ { target: "#minecraft:crops" } ]
        parameters:
          chain_limit: { milestones: { 25: 3, 50: 6, 75: 10, 100: 16 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&a✦ Harvest Wave! ✦" }
      sounds: [ { type: "ITEM_CROP_PLANT", volume: 0.5, pitch: 1.2, target: "self" } ]
  - id: "nutrient_rich"
    display_name: "Nutrient Rich"
    unlock_level: 50
    trigger: "block_break"
    display: { lore: [ "&7Crop breaks yield +&a{yield_chance}%&7 bonus crops." ] }
    mechanics:
      - type: "core:yield_multiplier"
        filters: [ { target: "#minecraft:crops" } ]
        parameters:
          yield_chance: { linear: { base: 1.0, step: 0.03, max: 5.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "hearty_meal"
    display_name: "Hearty Meal"
    unlock_level: 75
    trigger: "consume_item"
    display: { lore: [ "&7Cooked food grants +&a{saturation}&7 saturation." ] }
    mechanics:
      - type: "core:saturation_inject"
        filters: [ { target: "minecraft:cooked_beef" } ]   # representative; extend to cooked-food tag
        parameters:
          saturation: { linear: { base: 2.0, step: 0.05, max: 6.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "living_earth"
    display_name: "Living Earth"
    unlock_level: 100
    trigger: "block_break"
    display:
      lore:
        - "&7Crops auto-replant &a{replant_chance}%&7 of the time."
        - "&7Harvest Wave chain limit +{chain_bonus}."
    mechanics:
      - type: "core:auto_replant"
      - type: "core:yield_multiplier"      # sub-scaling proxy: bonus seed return chance
        filters: [ { target: "#minecraft:crops" } ]
        parameters:
          yield_chance: { linear: { base: 20.0, step: 0.8, max: 100.0 } }
    feedback: { notify: { action_bar: false } }
```

---

### herbalism.yml — Herbalism

**Issues**
1. [BUG][C] `crop_grow` XP 2.0 with no filter — same unbounded free-XP farming issue as farming.
2. [BUG][G] `rapid_growth` L15 `core:haste_effect` + `target:#c:herbs` → no event guard; re-applies on every event. Lore "harvest herbs faster" is loosely correct (Haste speeds breaking) but firing scope is wrong.
3. [VIOLATION][B][F] `herbalist_knowledge` L25 is a **passive** `core:xp_bonus` placed in the L25 **primary active** slot (§3: must be active gameplay hook, Shift+Right-Click).
4. [SHORTCOMING][F] `master_herbalist` L100 `constant: 100.0` — binary guaranteed doubles, no sub-scaling.
5. [DESIGN][A] `overgrowth` L75 `core:area_harvest` + `target:#c:herbs` with sneak/cooldown/exhaustion — well-gated ✓. But `area_harvest` only breaks horizontally (dx/dz); misses vertical herbs (vines, kelp, twisting-weeping vines).

**Redesign**

```yaml
id: "herbalism"
max_level: 100
display: { name: "Herbalism", icon: "minecraft:sugar_cane", color: "GREEN", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "block_break"
    filters: [ { target: "#c:herbs" }, { state: "player_placed:false" } ]
    reward: { constant: 4.0 }
  # crop_grow XP removed (unbounded)
abilities:
  - id: "green_thumb"
    display_name: "Green Thumb"
    unlock_level: 1
    trigger: "block_break"
    display: { lore: [ "&7Herb/flower yield +&a{yield_chance}%&7." ] }
    mechanics:
      - type: "core:yield_multiplier"
        filters: [ { target: "#c:herbs" } ]
        parameters:
          yield_chance: { linear: { base: 0.5, step: 0.5, max: 50.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "rapid_growth"
    display_name: "Rapid Growth"
    unlock_level: 15
    trigger: "block_break"
    display: { lore: [ "&7Breaking herbs grants &a{amplifier}&7 Haste for {duration}s." ] }
    mechanics:
      - type: "core:haste_effect"
        filters: [ { target: "#c:herbs" } ]
        parameters:
          amplifier: { linear: { base: 0, step: 0.01, max: 2 } }
          duration: { constant: 20.0 }
    feedback: { notify: { action_bar: false } }
  - id: "wild_harvest"
    display_name: "Wild Harvest"
    unlock_level: 25
    trigger: "block_break"
    display:
      lore:
        - "&7Sneak-break to harvest up to &a{chain_limit}&7 connected herbs."
        - "&8Requires: Sneaking."
    requirements:
      cooldown: 6.0
      state: [ "is_sneaking" ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&cWild Harvest cooling down: {time}s" } }
    mechanics:
      - type: "core:chain_break"
        filters: [ { target: "#c:herbs" } ]
        parameters:
          chain_limit: { milestones: { 25: 4, 50: 8, 75: 14, 100: 24 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&2✦ Wild Harvest! ✦" } }
  - id: "natural_remedy"
    display_name: "Natural Remedy"
    unlock_level: 50
    trigger: "consume_item"
    display: { lore: [ "&7Eating herbs grants +&a{saturation}&7 saturation." ] }
    mechanics:
      - type: "core:saturation_inject"
        filters: [ { target: "#c:herbs" } ]
        parameters:
          saturation: { linear: { base: 1.0, step: 0.03, max: 4.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "overgrowth"
    display_name: "Overgrowth"
    unlock_level: 75
    trigger: "block_break"
    display:
      lore:
        - "&7Sneak to harvest all herbs in a &a{radius}&7 radius (max {max_blocks})."
        - "&8Requires: Sneaking."
    requirements:
      cooldown: 20.0
      state: [ "is_sneaking" ]
      exhaustion: { amount: 2.0, minimum: 4.0 }
    on_failure: { cooldown: { action_bar: "&2Overgrowth cooling down: {time}s" } }
    mechanics:
      - type: "core:area_harvest"
        filters: [ { target: "#c:herbs" } ]
        parameters:
          radius: { linear: { base: 2.0, step: 0.06, max: 5.0 } }
          max_blocks: { linear: { base: 16.0, step: 1.0, max: 48.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&2✦ Overgrowth! ✦" }
      sounds: [ { type: "BLOCK_GRASS_BREAK", volume: 0.5, pitch: 1.2, target: "self" } ]
  - id: "master_herbalist"
    display_name: "Master Herbalist"
    unlock_level: 100
    trigger: "block_break"
    display: { lore: [ "&7Herb yield always doubles; +&a{yield_chance}%&7 chance to triple." ] }
    mechanics:
      - type: "core:yield_multiplier"
        filters: [ { target: "#c:herbs" } ]
        parameters:
          yield_chance: { constant: 100.0 }
      - type: "core:yield_multiplier"
        filters: [ { target: "#c:herbs" } ]
        parameters:
          yield_chance: { linear: { base: 10.0, step: 0.9, max: 80.0 } }
    feedback: { notify: { action_bar: false } }
```

---

### husbandry.yml — Husbandry

**Issues**
1. [BUG][G] `shearing_mastery` L15 `core:armor_bonus` — no event guard; fires on every event, stacking armor. Lore "while near animals" but no proximity check exists — just permanent armor.
2. [BUG][G] `herd_growth` L50 `core:knockback_resist` — no event guard; stacks knockback resist. Lore "from your herd" but no entity proximity check.
3. [BUG][G] `animal_whisperer` L75 `core:speed_bonus` — no event guard, no filter; stacks speed on every event. Lore "with animals nearby" but no proximity check.
4. [DESIGN][F] `master_husbander` L100 `constant: 100.0` tame chance — binary "tame instantly", no sub-scaling.
5. [DESIGN][C] XP sources (breed 15, shear 5, tame 20) are well-targeted triggers. Good.
6. [DESIGN][III] `gentle_hands` L25 is passive `modify_tame_chance` — not active. §3 wants L25 active. Native input: sneak + right-click on a tameable with food in hand to boost tame attempt.

**Redesign**

```yaml
id: "husbandry"
max_level: 100
display: { name: "Husbandry", icon: "minecraft:wheat", color: "GREEN", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "breed_animals"
    reward: { constant: 15.0 }
  - trigger: "player_shear"
    reward: { constant: 5.0 }
  - trigger: "player_tame"
    reward: { constant: 20.0 }
abilities:
  - id: "animal_bond"
    display_name: "Animal Bond"
    unlock_level: 1
    trigger: "breed_animals"
    display: { lore: [ "&7Breeding & shearing XP &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:xp_bonus"
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.01, max: 1.6 } }
    feedback: { notify: { action_bar: false } }
  - id: "shearing_mastery"
    display_name: "Shearing Mastery"
    unlock_level: 15
    trigger: "player_shear"
    display: { lore: [ "&7Shearing grants &a{amount}&7 armor for {duration}s (vs animal retaliation)." ] }
    mechanics:
      - type: "core:armor_bonus"
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
          duration: { constant: 15.0 }
    feedback: { notify: { action_bar: false } }
  - id: "gentle_hands"
    display_name: "Gentle Hands"
    unlock_level: 25
    trigger: "player_tame"
    display:
      lore:
        - "&7Taming succeeds &a{multiplier}x&7 more often."
        - "&7Lasts one tame attempt — shift-right-click a hungry animal."
    requirements:
      cooldown: 30.0
      state: [ "is_sneaking" ]
    on_failure: { cooldown: { action_bar: "&eGentle Hands cooling down: {time}s" } }
    mechanics:
      - type: "core:modify_tame_chance"
        parameters:
          multiplier: { linear: { base: 1.2, step: 0.02, max: 2.5 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&a✦ Gentle Hands! ✦" } }
  - id: "herd_growth"
    display_name: "Herd Growth"
    unlock_level: 50
    trigger: "breed_animals"
    display: { lore: [ "&7Breeding grants &a{amount}&7 knockback resistance for {duration}s." ] }
    mechanics:
      - type: "core:knockback_resist"
        parameters:
          amount: { linear: { base: 0.1, step: 0.005, max: 0.5 } }
          duration: { constant: 20.0 }
    feedback: { notify: { action_bar: false } }
  - id: "animal_whisperer"
    display_name: "Animal Whisperer"
    unlock_level: 75
    trigger: "ride_horse"
    display: { lore: [ "&7Mounting grants &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.003, max: 1.3 } }
          duration: { constant: 30.0 }
    feedback: { notify: { action_bar: false } }
  - id: "master_husbander"
    display_name: "Master Husbander"
    unlock_level: 100
    trigger: "player_tame"
    display: { lore: [ "&7Tame success +&a{multiplier}x&7 (near-guaranteed for vanilla mobs)." ] }
    mechanics:
      - type: "core:modify_tame_chance"
        parameters:
          multiplier: { linear: { base: 5.0, step: 0.1, max: 20.0 } }
    feedback: { notify: { action_bar: false } }
```

---

### cooking.yml — Cooking

**Issues**
1. [BUG][G] `gourmand` L75 `core:speed_bonus` — no event guard, no filter; stacks speed on every event. Lore "after eating" but the mechanic doesn't check for `consume_item`. Default duration is 300s (not "10 seconds").
2. [BUG][G] `master_chef` L100 `core:speed_bonus` — same stacking issue; constant multiplier, no sub-scaling.
3. [DESIGN][F] `master_chef` L100 all constants; "all food gives extra saturation" overlaps L1 `seasoning` and L25 `nutritionist` (all `saturation_inject`).
4. [BUG][C] `furnace_extract` 4.0 XP no filter → XP for extracting **any** furnace product (iron, stone, etc.), not food.
5. [DESIGN][D] `cooking` only covers cooked food; raw food saturation (`seasoned_hand`-style from farming) is split — acceptable but L25 `nutritionist` "all food" overlaps L1.

**Redesign**

```yaml
id: "cooking"
max_level: 100
display: { name: "Cooking", icon: "minecraft:furnace", color: "YELLOW", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "furnace_extract"
    filters: [ { target: "minecraft:cooked_beef" } ]   # representative cooked-food filter
    reward: { constant: 4.0 }
  - trigger: "consume_item"
    filters: [ { target: "minecraft:cooked_beef" } ]
    reward: { constant: 2.0 }
abilities:
  - id: "seasoning"
    display_name: "Seasoning"
    unlock_level: 1
    trigger: "consume_item"
    display: { lore: [ "&7Cooked food grants +&a{saturation}&7 saturation." ] }
    mechanics:
      - type: "core:saturation_inject"
        filters: [ { target: "minecraft:cooked_beef" } ]
        parameters:
          saturation: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "heat_control"
    display_name: "Heat Control"
    unlock_level: 15
    trigger: "furnace_extract"
    display: { lore: [ "&7Cooked-food smelting output &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_furnace_output"
        filters: [ { target: "minecraft:cooked_beef" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.005, max: 1.5 } }
    feedback: { notify: { action_bar: false } }
  - id: "nutritionist"
    display_name: "Nutritionist"
    unlock_level: 25
    trigger: "consume_item"
    display:
      lore:
        - "&7Shift+eat to gain &a{multiplier}x&7 XP from food for {duration}s."
        - "&8Requires: Sneaking."
    requirements:
      cooldown: 15.0
      state: [ "is_sneaking" ]
      exhaustion: { amount: 0.5, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&eNutritionist cooling down: {time}s" } }
    mechanics:
      - type: "core:xp_bonus"
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.01, max: 1.8 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&6✦ Nutritionist! ✦" } }
  - id: "hearty_stew"
    display_name: "Hearty Stew"
    unlock_level: 50
    trigger: "consume_item"
    display: { lore: [ "&7Cooked food grants +&a{saturation}&7 saturation." ] }
    mechanics:
      - type: "core:saturation_inject"
        filters: [ { target: "minecraft:cooked_beef" } ]
        parameters:
          saturation: { linear: { base: 2.0, step: 0.04, max: 6.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "gourmand"
    display_name: "Gourmand"
    unlock_level: 75
    trigger: "consume_item"
    display: { lore: [ "&7Eating grants &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.003, max: 1.3 } }
          duration: { linear: { base: 10.0, step: 0.2, max: 30.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "master_chef"
    display_name: "Master Chef"
    unlock_level: 100
    trigger: "consume_item"
    display: { lore: [ "&7Cooked food grants +&a{saturation}&7 saturation & speed burst." ] }
    mechanics:
      - type: "core:saturation_inject"
        filters: [ { target: "minecraft:cooked_beef" } ]
        parameters:
          saturation: { linear: { base: 4.0, step: 0.05, max: 8.0 } }
      - type: "core:speed_bonus"
        parameters:
          multiplier: { linear: { base: 1.2, step: 0.002, max: 1.4 } }
          duration: { linear: { base: 15.0, step: 0.2, max: 30.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&6✦ Master Chef! ✦" }
      sounds: [ { type: "ENTITY_PLAYER_LEVELUP", volume: 0.5, pitch: 1.5, target: "self" } ]
```

---

### fishing.yml — Fishing

**Issues**
1. [BUG][G] `tidal_resilience` L75 `core:knockback_resist` + `tool:#minecraft:fishing_rods` — no event guard; stacks knockback resist on every event while holding a rod.
2. [DESIGN][B] `lucky_catch` L25 is `core:fishing_yield` (passive) — duplicates L1 `angler`. §3 wants L25 to be the primary **active** hook.
3. [SHORTCOMING][F] `leviathan` L100 `constant: 3.0` — no sub-scaling.
4. [BUG][C] `fishing` XP 10.0 with no filter — fine (PlayerFishEvent is intrinsically targeted). OK.
5. [BUG][D] `lucky_catch` lore "better enchantments" — `fishing_yield` only increases bonus-catch **chance**, not enchantment quality. Misleading.

**Redesign**

```yaml
id: "fishing"
max_level: 100
display: { name: "Fishing", icon: "minecraft:fishing_rod", color: "GREEN", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "fishing"
    reward: { constant: 10.0 }
abilities:
  - id: "angler"
    display_name: "Angler"
    unlock_level: 1
    trigger: "fishing"
    display: { lore: [ "&7Treasure catch rate +&a{yield_chance}%&7." ] }
    mechanics:
      - type: "core:fishing_yield"
        parameters:
          yield_chance: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "light_line"
    display_name: "Light Line"
    unlock_level: 15
    trigger: "item_damage"
    display: { lore: [ "&7Rod has &a{chance}%&7 chance to lose no durability." ] }
    mechanics:
      - type: "core:durability_save"
        filters: [ { tool: "#minecraft:fishing_rods" } ]
        parameters:
          chance: { linear: { base: 20.0, step: 0.5, max: 75.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "big_pull"
    display_name: "Big Pull"
    unlock_level: 25
    trigger: "fishing"
    display:
      lore:
        - "&7Shift-cast to double your next catch loot &a{multiplier}x&7."
        - "&8Requires: Sneaking, fishing rod."
    requirements:
      cooldown: 20.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "#minecraft:fishing_rods", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&cBig Pull cooling down: {time}s" } }
    mechanics:
      - type: "core:fishing_loot"
        parameters:
          multiplier: { linear: { base: 1.5, step: 0.02, max: 2.5 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&b✦ Big Pull! ✦" } }
  - id: "sea_bounty"
    display_name: "Sea Bounty"
    unlock_level: 50
    trigger: "fishing"
    display: { lore: [ "&7Fish catches &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:fishing_loot"
        parameters:
          multiplier: { linear: { base: 1.5, step: 0.01, max: 2.5 } }
    feedback: { notify: { action_bar: false } }
  - id: "tidal_resilience"
    display_name: "Tidal Resilience"
    unlock_level: 75
    trigger: "fishing"
    display: { lore: [ "&7Casting grants &a{amount}&7 knockback resistance for {duration}s." ] }
    mechanics:
      - type: "core:knockback_resist"
        parameters:
          amount: { linear: { base: 0.2, step: 0.005, max: 0.7 } }
          duration: { constant: 15.0 }
    feedback: { notify: { action_bar: false } }
  - id: "leviathan"
    display_name: "Leviathan"
    unlock_level: 100
    trigger: "fishing"
    display: { lore: [ "&7Catch loot &a{multiplier}x&7; treasure rate +&a{yield_chance}%&7." ] }
    mechanics:
      - type: "core:fishing_loot"
        parameters:
          multiplier: { linear: { base: 2.5, step: 0.02, max: 3.5 } }
      - type: "core:fishing_yield"
        parameters:
          yield_chance: { linear: { base: 3.0, step: 0.05, max: 6.0 } }
    feedback: { notify: { action_bar: false } }
```

The following skills continue in the next insertion.

### alchemy.yml — Alchemy

**Issues**
1. [BUG][G] `alchemical_haste` L75 `core:speed_bonus` — no event guard, no filter; stacks speed on every event. Lore "while brewing" but brew_potion(dispatch for nearby players) + speed_bonus fires on all events.
2. [SHORTCOMING][F] `master_alchemist` L100 — `modify_brew_time multiplier: 0.5` (2x faster, not "instant") and `modify_potion_duration multiplier: 3.0`; both constants, no sub-scaling. Lore "instantly and forever" overpromises.
3. [BUG][C] `brew_potion` 10.0 XP dispatched to **all nearby players** within 5 blocks — a player walking past a brewing stand earns Alchemy XP. Should be the brewer only (BrewingStartEvent doesn't expose the player directly — limitation noted).
4. [DESIGN][D] `rapid_brewing` "brews faster" via `modify_brew_time multiplier < 1` — correct mechanic. But the trigger fires on `BrewingStartEvent` and the mechanic modifies the already-starting batch; brew time is set at start. Verify the mechanic actually intercepts in time (`BrewingStartEvent` at MONITOR priority may be too late to change total time — needs engine check).
5. [DESIGN][B] L25 `efficient_alchemy` is passive `xp_bonus` in the primary-active slot.

**Redesign**

```yaml
id: "alchemy"
max_level: 100
display: { name: "Alchemy", icon: "minecraft:brewing_stand", color: "PURPLE", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "brew_potion"
    reward: { constant: 10.0 }
abilities:
  - id: "rapid_brewing"
    display_name: "Rapid Brewing"
    unlock_level: 1
    trigger: "brew_potion"
    display: { lore: [ "&7Brew time &a{multiplier}x&7 (faster)." ] }
    mechanics:
      - type: "core:modify_brew_time"
        parameters:
          multiplier: { linear: { base: 0.9, step: -0.005, max: 0.5 } }   # <1 speeds up
    feedback: { notify: { action_bar: false } }
  - id: "extended_effects"
    display_name: "Extended Effects"
    unlock_level: 15
    trigger: "brew_potion"
    display: { lore: [ "&7Brewed potion durations &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_potion_duration"
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.005, max: 1.5 } }
    feedback: { notify: { action_bar: false } }
  - id: "catalyst"
    display_name: "Catalyst"
    unlock_level: 25
    trigger: "brew_potion"
    display:
      lore:
        - "&7Shift-brew to gain &a{multiplier}x&7 brewing XP for {duration}s."
        - "&8Requires: Sneaking, blaze powder in inventory."
    requirements:
      cooldown: 30.0
      state: [ "is_sneaking" ]
      items: [ { action: "cost", tag: "minecraft:blaze_powder", amount: 1 } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure:
      cooldown: { action_bar: "&eCatalyst cooling down: {time}s" }
      missing_item: { action_bar: "&cRequires 1x Blaze Powder!" }
    mechanics:
      - type: "core:xp_bonus"
        parameters:
          multiplier: { linear: { base: 1.2, step: 0.02, max: 2.5 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&d✦ Catalyst! ✦" } }
  - id: "concentrated_essence"
    display_name: "Concentrated Essence"
    unlock_level: 50
    trigger: "brew_potion"
    display: { lore: [ "&7Brewed potion durations &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_potion_duration"
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.01, max: 2.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "alchemical_haste"
    display_name: "Alchemical Haste"
    unlock_level: 75
    trigger: "brew_potion"
    display: { lore: [ "&7Brewing grants &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.003, max: 1.3 } }
          duration: { linear: { base: 10.0, step: 0.2, max: 30.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "master_alchemist"
    display_name: "Master Alchemist"
    unlock_level: 100
    trigger: "brew_potion"
    display: { lore: [ "&7Brew time &a{multiplier}x&7; durations &a{multiplier_dur}x&7." ] }
    mechanics:
      - type: "core:modify_brew_time"
        parameters:
          multiplier: { linear: { base: 0.4, step: -0.002, max: 0.2 } }
      - type: "core:modify_potion_duration"
        parameters:
          multiplier: { linear: { base: 2.0, step: 0.02, max: 3.0 } }
    feedback: { notify: { action_bar: false } }
```

---

### enchanting.yml — Enchanting

**Issues**
1. [BUG][D][G] `experience_attraction` L25 `core:speed_bonus` — lore "XP orbs travel faster toward you" but `speed_bonus` modifies **player movement speed**, which has nothing to do with XP orb attraction. Completely mismatched ability. No event guard → stacks speed on every event.
2. [BUG][G] `soulbound_armor` L75 `core:armor_bonus` — no event guard, no filter; stacks armor on every event. Lore "while holding enchanted gear" but no enchantment check exists as a filter.
3. [BUG][C] `collect_xp` 1.0 XP on **every** vanilla XP orb pickup — broad but low; acceptable. `enchant_item` 15.0 targeted. OK.
4. [SHORTCOMING][F] `master_enchanter` L100 `constant: 3.0` on all crafts (not enchanted books specifically).
5. [DESIGN][B] L25 `experience_attraction` is the primary slot but broken/mismatched; needs a real active or correct mechanic.

**Redesign**

```yaml
id: "enchanting"
max_level: 100
display: { name: "Enchanting", icon: "minecraft:enchanting_table", color: "PURPLE", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "enchant_item"
    reward: { constant: 15.0 }
  - trigger: "collect_xp"
    reward: { constant: 1.0 }
abilities:
  - id: "arcane_knowledge"
    display_name: "Arcane Knowledge"
    unlock_level: 1
    trigger: "enchant_item"
    display: { lore: [ "&7Enchanting XP &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:xp_bonus"
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.01, max: 1.6 } }
    feedback: { notify: { action_bar: false } }
  - id: "durable_enchantments"
    display_name: "Durable Enchantments"
    unlock_level: 15
    trigger: "item_damage"
    display: { lore: [ "&7Enchanted gear has &a{chance}%&7 chance to lose no durability." ] }
    mechanics:
      - type: "core:durability_save"
        parameters:
          chance: { linear: { base: 10.0, step: 0.3, max: 40.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "soul_binding"
    display_name: "Soul Binding"
    unlock_level: 25
    trigger: "enchant_item"
    display:
      lore:
        - "&7Shift-enchant to reduce enchant cost by &a{discount}%&7."
        - "&8Requires: Sneaking."
    requirements:
      cooldown: 30.0
      state: [ "is_sneaking" ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&eSoul Binding cooling down: {time}s" } }
    mechanics:
      - type: "core:modify_enchant_cost"
        parameters:
          discount: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&d✦ Soul Binding! ✦" } }
  - id: "ancient_secrets"
    display_name: "Ancient Secrets"
    unlock_level: 50
    trigger: "craft_item"
    display: { lore: [ "&7Enchanted-book crafts yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "minecraft:enchanted_book" } ]
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.01, max: 2.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "soulbound_armor"
    display_name: "Soulbound Armor"
    unlock_level: 75
    trigger: "enchant_item"
    display: { lore: [ "&7Enchanting grants &a{amount}&7 armor for {duration}s." ] }
    mechanics:
      - type: "core:armor_bonus"
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
          duration: { constant: 60.0 }
    feedback: { notify: { action_bar: false } }
  - id: "master_enchanter"
    display_name: "Master Enchanter"
    unlock_level: 100
    trigger: "craft_item"
    display: { lore: [ "&7Enchanted-book crafts yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "minecraft:enchanted_book" } ]
        parameters:
          multiplier: { linear: { base: 2.0, step: 0.02, max: 3.0 } }
    feedback: { notify: { action_bar: false } }
```

---

### smithing.yml — Smithing

**Issues**
1. [BUG][G] `master_smith` L75 `core:armor_bonus` + `tool:#minecraft:swords` — no event guard; stacks armor on every event while holding a sword.
2. [BUG][C] `craft_item` 12.0 XP with **no filter** → XP for crafting anything (bread, sticks, etc.), not tools/armor.
3. [DESIGN][B] `weighted_strike` L25 is passive `modify_damage` (no requirements). §3 wants L25 active; native input: shift+right-click to temper a held weapon for a damage buff.
4. [SHORTCOMING][F] `legendary_artisan` L100 `constant: 2.0` on all crafts, no target filter.
5. [BUG][A] `furnace_extract` filters `#minecraft:gold_ores` / `#minecraft:iron_ores` — OK, targeted.

**Redesign**

```yaml
id: "smithing"
max_level: 100
display: { name: "Smithing", icon: "minecraft:anvil", color: "YELLOW", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "craft_item"
    filters: [ { target: "#minecraft:tools" } ]   # representative; tools/armor only
    reward: { constant: 12.0 }
  - trigger: "furnace_extract"
    filters: [ { target: "#minecraft:iron_ores" } ]
    reward: { constant: 6.0 }
  - trigger: "furnace_extract"
    filters: [ { target: "#minecraft:gold_ores" } ]
    reward: { constant: 8.0 }
abilities:
  - id: "efficient_smith"
    display_name: "Efficient Smith"
    unlock_level: 1
    trigger: "craft_item"
    display: { lore: [ "&7Tool/armor crafts yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "#minecraft:tools" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.005, max: 1.5 } }
    feedback: { notify: { action_bar: false } }
  - id: "tempered_metal"
    display_name: "Tempered Metal"
    unlock_level: 15
    trigger: "item_damage"
    display: { lore: [ "&7Smithed gear has &a{chance}%&7 chance to lose no durability." ] }
    mechanics:
      - type: "core:durability_save"
        parameters:
          chance: { linear: { base: 10.0, step: 0.4, max: 50.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "weighted_strike"
    display_name: "Weighted Strike"
    unlock_level: 25
    trigger: "entity_damage"
    display:
      lore:
        - "&7Shift-attack with a sword for &a{multiplier}x&7 damage on next hit."
        - "&8Requires: Sneaking, sword."
    requirements:
      cooldown: 8.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "#minecraft:swords", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&eWeighted Strike cooling down: {time}s" } }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { tool: "#minecraft:swords" } ]
        parameters:
          multiplier: { linear: { base: 1.2, step: 0.01, max: 1.6 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&c✦ Weighted Strike! ✦" } }
  - id: "reforging"
    display_name: "Reforging"
    unlock_level: 50
    trigger: "furnace_extract"
    display: { lore: [ "&7Ore smelting yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_furnace_output"
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.01, max: 2.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "master_smith"
    display_name: "Master Smith"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&7While holding a sword, gain &a{amount}&7 armor on hit." ] }
    mechanics:
      - type: "core:armor_bonus"
        filters: [ { tool: "#minecraft:swords" } ]
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "legendary_artisan"
    display_name: "Legendary Artisan"
    unlock_level: 100
    trigger: "craft_item"
    display: { lore: [ "&7Tool/armor crafts yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "#minecraft:tools" } ]
        parameters:
          multiplier: { linear: { base: 1.8, step: 0.01, max: 2.5 } }
    feedback: { notify: { action_bar: false } }
```

---

### tailoring.yml — Tailoring

**Issues**
1. [BUG][A] `dye_master` L50 `core:yield_multiplier` — checks `BlockBreakEvent` and **doubles block drops on break**. Lore "double output on colored items and banners" — colored items/banners are **crafted** not broken. Wrong mechanic entirely; should be `modify_craft_output`.
2. [BUG][G] `padded_linings` L25 `core:armor_bonus` — no event guard; stacks armor on every event.
3. [BUG][C] `item_damage` 1.0 XP on **any** item durability loss (any tool, any armor, anything). `craft_item` 10.0 XP on **any** craft (no wool/leather filter).
4. [SHORTCOMING][F] `legendary_tailor` L100 `constant: 2.0` on all crafts, no target filter.
5. [DESIGN][C] `nimble_fingers` L75 `core:dodge` — dodge checks `EntityDamageEvent` ✓ (guarded), acceptable.

**Redesign**

```yaml
id: "tailoring"
max_level: 100
display: { name: "Tailoring", icon: "minecraft:leather_chestplate", color: "YELLOW", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "craft_item"
    filters: [ { target: "#minecraft:wool" } ]     # representative fabric crafts
    reward: { constant: 10.0 }
  - trigger: "item_damage"
    filters: [ { target: "#minecraft:leather_armor" } ]   # leather armor only
    reward: { constant: 1.0 }
abilities:
  - id: "spinner"
    display_name: "Spinner"
    unlock_level: 1
    trigger: "craft_item"
    display: { lore: [ "&7Wool/carpet crafts yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "#minecraft:wool" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.005, max: 1.5 } }
    feedback: { notify: { action_bar: false } }
  - id: "reinforced_stitch"
    display_name: "Reinforced Stitch"
    unlock_level: 15
    trigger: "item_damage"
    display: { lore: [ "&7Leather armor has &a{chance}%&7 chance to lose no durability." ] }
    mechanics:
      - type: "core:durability_save"
        filters: [ { target: "#minecraft:leather_armor" } ]
        parameters:
          chance: { linear: { base: 10.0, step: 0.3, max: 40.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "padded_linings"
    display_name: "Padded Linings"
    unlock_level: 25
    trigger: "entity_damage_taken"
    display:
      lore:
        - "&7Shift-craft leather armor to embed &a{amount}&7 armor for {duration}s on wear."
        - "&8Requires: Sneaking."
    requirements:
      cooldown: 15.0
      state: [ "is_sneaking" ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&ePadded Linings cooling down: {time}s" } }
    mechanics:
      - type: "core:armor_bonus"
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
          duration: { constant: 30.0 }
    feedback: { notify: { action_bar: true, chat: false, message: "&6✦ Padded Linings! ✦" } }
  - id: "dye_master"
    display_name: "Dye Master"
    unlock_level: 50
    trigger: "craft_item"
    display: { lore: [ "&7Banner & dye crafts yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_craft_output"
        filters: [ { target: "#minecraft:banners" } ]
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.01, max: 2.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "nimble_fingers"
    display_name: "Nimble Fingers"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&a{chance}%&7 chance to dodge incoming damage." ] }
    mechanics:
      - type: "core:dodge"
        filters: [ { target: "#minecraft:leather_armor", state: "equipped:light" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.3, max: 35.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "legendary_tailor"
    display_name: "Legendary Tailor"
    unlock_level: 100
    trigger: "craft_item"
    display: { lore: [ "&7All fabric/armor crafts yield &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_craft_output"
        parameters:
          multiplier: { linear: { base: 1.8, step: 0.01, max: 2.5 } }
    feedback: { notify: { action_bar: false } }
```

---

### light_armor.yml — Light Armor

**Issues**
1. [BUG][C][VIOLATION-G] `entity_damage_taken` 3.0 XP with **no armor filter** → grants XP while wearing heavy armor. Heavy Armor XP overlaps. Needs `state:equipped:light` (P2-6).
2. [BUG][G] `featherweight` L1, `sprint` L50 `core:speed_bonus` — no event guard, no filter; stacks speed on **every** event regardless of armor. The L1 foundational passive is always-on, no armor requirement.
3. [SHORTCOMING][F] `phantom` L100 — `constant: 50.0` dodge + `constant: 1.3` speed. No sub-scaling.
4. [DESIGN][B] No active abilities. L25 `nimble_feet` is passive `cancel_damage`. §3 wants L25 active (sneak+jump mobility vector for evasion fits).

**Redesign**

```yaml
id: "light_armor"
max_level: 100
display: { name: "Light Armor", icon: "minecraft:leather_chestplate", color: "BLUE", style: "SOLID" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage_taken"
    filters: [ { state: "equipped:light" } ]      # P2-6: light armor only
    reward: { constant: 3.0 }
abilities:
  - id: "featherweight"
    display_name: "Featherweight"
    unlock_level: 1
    trigger: "entity_damage_taken"
    display: { lore: [ "&7While in light armor, gain &a{multiplier}x&7 speed for {duration}s on hit." ] }
    mechanics:
      - type: "core:speed_bonus"
        filters: [ { state: "equipped:light" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.002, max: 1.25 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "dodge_roll"
    display_name: "Dodge Roll"
    unlock_level: 15
    trigger: "entity_damage_taken"
    display: { lore: [ "&a{chance}%&7 chance to fully dodge attacks in light armor." ] }
    mechanics:
      - type: "core:dodge"
        filters: [ { state: "equipped:light" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "evasive_roll"
    display_name: "Evasive Roll"
    unlock_level: 25
    trigger: "entity_damage_taken"
    display:
      lore:
        - "&7Sneak+Jump to negate the next hit &a{chance}%&7."
        - "&8Requires: Sneaking, on ground, light armor."
    requirements:
      cooldown: 6.0
      state: [ "is_sneaking", "is_on_ground", "equipped:light" ]
    on_failure: { cooldown: { action_bar: "&eEvasive Roll cooling down: {time}s" } }
    mechanics:
      - type: "core:cancel_damage"
        parameters:
          chance: { linear: { base: 50.0, step: 0.5, max: 90.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&b✦ Evasive Roll! ✦" }
      sounds: [ { type: "ITEM_SHIELD_BLOCK", volume: 0.4, pitch: 1.5, target: "self" } ]
  - id: "spryness"
    display_name: "Spryness"
    unlock_level: 50
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Hit in light armor grants &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        filters: [ { state: "equipped:light" } ]
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.002, max: 1.3 } }
          duration: { linear: { base: 6.0, step: 0.1, max: 12.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "evasion_mastery"
    display_name: "Evasion Mastery"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&a{chance}%&7 dodge chance in light armor." ] }
    mechanics:
      - type: "core:dodge"
        filters: [ { state: "equipped:light" } ]
        parameters:
          chance: { linear: { base: 10.0, step: 0.3, max: 40.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "phantom"
    display_name: "Phantom"
    unlock_level: 100
    trigger: "entity_damage_taken"
    display: { lore: [ "&a{chance}%&7 dodge & &a{multiplier}x&7 speed in light armor." ] }
    mechanics:
      - type: "core:dodge"
        filters: [ { state: "equipped:light" } ]
        parameters:
          chance: { linear: { base: 50.0, step: 0.5, max: 70.0 } }
      - type: "core:speed_bonus"
        filters: [ { state: "equipped:light" } ]
        parameters:
          multiplier: { linear: { base: 1.3, step: 0.002, max: 1.4 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
```

---

### medium_armor.yml — Medium Armor

**Issues**
1. [BUG][C][G] `entity_damage_taken` 3.0 XP, no armor filter → XP in any armor. `tempered_guard` L1 `core:armor_bonus` no event guard → stacks armor on every event.
2. [BUG][G] `swift_recovery` L50 `core:speed_bonus` no event guard; stacks speed on every event.
3. [SHORTCOMING][F] `iron_will` L100 constants (0.4 armor, 10.0 cancel).
4. [DESIGN][B] All passive. L25 `counter` (thorns) passive — should be active. Also `counter` uses `thorns_damage` which checks `EntityDamageByEntityEvent` ✓ but dispatches on `entity_damage_taken` (EntityDamageEvent); thorns works only when the damage source is an entity. OK semantically.

**Redesign**

```yaml
id: "medium_armor"
max_level: 100
display: { name: "Medium Armor", icon: "minecraft:iron_chestplate", color: "BLUE", style: "SOLID" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage_taken"
    filters: [ { state: "equipped:medium" } ]
    reward: { constant: 3.0 }
abilities:
  - id: "tempered_guard"
    display_name: "Tempered Guard"
    unlock_level: 1
    trigger: "entity_damage_taken"
    display: { lore: [ "&7In medium armor, gain &a{amount}&7 armor on hit for {duration}s." ] }
    mechanics:
      - type: "core:armor_bonus"
        filters: [ { state: "equipped:medium" } ]
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 4.0 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "deflect"
    display_name: "Deflect"
    unlock_level: 15
    trigger: "entity_damage_taken"
    display: { lore: [ "&a{chance}%&7 chance to block damage in medium armor." ] }
    mechanics:
      - type: "core:block_damage"
        filters: [ { state: "equipped:medium" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "riposte"
    display_name: "Riposte"
    unlock_level: 25
    trigger: "entity_damage_taken"
    display:
      lore:
        - "&7Sneak while hit to reflect &a{damage}&7 to the attacker."
        - "&8Requires: Sneaking, medium armor."
    requirements:
      cooldown: 5.0
      state: [ "is_sneaking", "equipped:medium" ]
    on_failure: { cooldown: { action_bar: "&eRiposte cooling down: {time}s" } }
    mechanics:
      - type: "core:thorns_damage"
        parameters:
          damage: { linear: { base: 2.0, step: 0.05, max: 6.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&b✦ Riposte! ✦" }
      sounds: [ { type: "ITEM_SHIELD_BLOCK", volume: 0.4, pitch: 1.2, target: "self" } ]
  - id: "swift_recovery"
    display_name: "Swift Recovery"
    unlock_level: 50
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Hit in medium armor grants &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        filters: [ { state: "equipped:medium" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.002, max: 1.2 } }
          duration: { constant: 6.0 }
    feedback: { notify: { action_bar: false } }
  - id: "endurance"
    display_name: "Endurance"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&a{chance}%&7 chance to fully negate damage in medium armor." ] }
    mechanics:
      - type: "core:cancel_damage"
        filters: [ { state: "equipped:medium" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 30.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "iron_will"
    display_name: "Iron Will"
    unlock_level: 100
    trigger: "entity_damage_taken"
    display: { lore: [ "&7In medium armor: +&a{amount}&7 armor & &a{chance}%&7 damage negate." ] }
    mechanics:
      - type: "core:armor_bonus"
        filters: [ { state: "equipped:medium" } ]
        parameters:
          amount: { linear: { base: 2.0, step: 0.05, max: 5.0 } }
          duration: { constant: 10.0 }
      - type: "core:cancel_damage"
        filters: [ { state: "equipped:medium" } ]
        parameters:
          chance: { linear: { base: 10.0, step: 0.3, max: 35.0 } }
    feedback: { notify: { action_bar: false } }
```

---

### heavy_armor.yml — Heavy Armor

**Issues**
1. [BUG][C][G] `entity_damage_taken` 3.0 XP no armor filter → XP in any armor. `steel_plating` L1 `core:armor_bonus` no event guard → stacks on every event.
2. [BUG][G] `fortified` L15, `immovable` L75 `core:knockback_resist` — no event guard; stacks knockback resist on every event.
3. [SHORTCOMING][F] `juggernaut` L100 constants (0.5 armor, 0.5 KB resist).
4. [DESIGN][B] All passive. L25 `thorns` passive — should be active. `shield_wall` L50 is `block_damage` (guarded EntityDamageEvent) ✓.

**Redesign**

```yaml
id: "heavy_armor"
max_level: 100
display: { name: "Heavy Armor", icon: "minecraft:diamond_chestplate", color: "BLUE", style: "SOLID" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage_taken"
    filters: [ { state: "equipped:heavy" } ]
    reward: { constant: 3.0 }
abilities:
  - id: "steel_plating"
    display_name: "Steel Plating"
    unlock_level: 1
    trigger: "entity_damage_taken"
    display: { lore: [ "&7In heavy armor, gain &a{amount}&7 armor on hit for {duration}s." ] }
    mechanics:
      - type: "core:armor_bonus"
        filters: [ { state: "equipped:heavy" } ]
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 4.0 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "fortified"
    display_name: "Fortified"
    unlock_level: 15
    trigger: "entity_damage_taken"
    display: { lore: [ "&7In heavy armor, gain &a{amount}&7 knockback resistance on hit." ] }
    mechanics:
      - type: "core:knockback_resist"
        filters: [ { state: "equipped:heavy" } ]
        parameters:
          amount: { linear: { base: 0.1, step: 0.005, max: 0.6 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "bulwark"
    display_name: "Bulwark"
    unlock_level: 25
    trigger: "entity_damage_taken"
    display:
      lore:
        - "&7Sneak while hit to reflect &a{damage}&7 to the attacker."
        - "&8Requires: Sneaking, heavy armor."
    requirements:
      cooldown: 6.0
      state: [ "is_sneaking", "equipped:heavy" ]
    on_failure: { cooldown: { action_bar: "&eBulwark cooling down: {time}s" } }
    mechanics:
      - type: "core:thorns_damage"
        parameters:
          damage: { linear: { base: 2.0, step: 0.04, max: 6.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&b✦ Bulwark! ✦" }
      sounds: [ { type: "ITEM_SHIELD_BLOCK", volume: 0.5, pitch: 1.0, target: "self" } ]
  - id: "shield_wall"
    display_name: "Shield Wall"
    unlock_level: 50
    trigger: "entity_damage_taken"
    display: { lore: [ "&a{chance}%&7 chance to block damage in heavy armor." ] }
    mechanics:
      - type: "core:block_damage"
        filters: [ { state: "equipped:heavy" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "immovable"
    display_name: "Immovable"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&7In heavy armor, knockback resist +&a{amount}&7 on hit." ] }
    mechanics:
      - type: "core:knockback_resist"
        filters: [ { state: "equipped:heavy" } ]
        parameters:
          amount: { linear: { base: 0.3, step: 0.005, max: 0.8 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "juggernaut"
    display_name: "Juggernaut"
    unlock_level: 100
    trigger: "entity_damage_taken"
    display: { lore: [ "&7In heavy armor: +&a{amount}&7 armor & +&a{kb}&7 KB resist on hit." ] }
    mechanics:
      - type: "core:armor_bonus"
        filters: [ { state: "equipped:heavy" } ]
        parameters:
          amount: { linear: { base: 3.0, step: 0.05, max: 6.0 } }
          duration: { constant: 10.0 }
      - type: "core:knockback_resist"
        filters: [ { state: "equipped:heavy" } ]
        parameters:
          amount: { linear: { base: 0.5, step: 0.005, max: 0.9 } }
          duration: { constant: 10.0 }
    feedback: { notify: { action_bar: false } }
```

---

### unarmored.yml — Unarmored

**Issues**
1. [BUG][G] `bare_necessities` L15 `core:speed_bonus` + `state:armor:empty` — no event guard; the `armor:empty` filter passes on **any** event when armor is empty → stacks speed on every event while unarmored.
2. [BUG][G] `hardened_body` L50 `core:armor_bonus` + `armor:empty` — same: stacks armor on every event unarmored.
3. [BUG][G] `enlightened_evasion` L100 `core:speed_bonus` + `core:dodge` — dodge is guarded (EntityDamageEvent) ✓, but speed_bonus stacks on every event.
4. [SHORTCOMING][F] `enlightened_evasion` L100 constants (50.0, 1.3).
5. [DESIGN][B] All passive. L25 `evasion` passive `cancel_damage`. §3 wants L25 active (sneak+jump mobility — fits "unarmored monk" fantasy).

**Redesign**

```yaml
id: "unarmored"
max_level: 100
display: { name: "Unarmored", icon: "minecraft:leather", color: "BLUE", style: "SOLID" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage_taken"
    filters: [ { state: "armor:empty" } ]
    reward: { constant: 5.0 }
abilities:
  - id: "nimble_defense"
    display_name: "Nimble Defense"
    unlock_level: 1
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Unarmored: &a{chance}%&7 dodge." ] }
    mechanics:
      - type: "core:dodge"
        filters: [ { state: "armor:empty" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.3, max: 35.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "bare_necessities"
    display_name: "Bare Necessities"
    unlock_level: 15
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Unarmored & hit: &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        filters: [ { state: "armor:empty" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.003, max: 1.3 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "wind_step"
    display_name: "Wind Step"
    unlock_level: 25
    trigger: "entity_damage_taken"
    display:
      lore:
        - "&7Sneak+Jump unarmored to negate next hit &a{chance}%&7."
        - "&8Requires: Sneaking, on ground, unarmored."
    requirements:
      cooldown: 6.0
      state: [ "is_sneaking", "is_on_ground", "armor:empty" ]
    on_failure: { cooldown: { action_bar: "&eWind Step cooling down: {time}s" } }
    mechanics:
      - type: "core:cancel_damage"
        parameters:
          chance: { linear: { base: 50.0, step: 0.5, max: 90.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&b✦ Wind Step! ✦" }
      sounds: [ { type: "ITEM_SHIELD_BLOCK", volume: 0.4, pitch: 1.5, target: "self" } ]
  - id: "hardened_body"
    display_name: "Hardened Body"
    unlock_level: 50
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Unarmored & hit: +&a{amount}&7 natural armor for {duration}s." ] }
    mechanics:
      - type: "core:armor_bonus"
        filters: [ { state: "armor:empty" } ]
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 4.0 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "deflection"
    display_name: "Deflection"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Unarmored: &a{chance}%&7 block chance." ] }
    mechanics:
      - type: "core:block_damage"
        filters: [ { state: "armor:empty" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "enlightened_evasion"
    display_name: "Enlightened Evasion"
    unlock_level: 100
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Unarmored: &a{chance}%&7 dodge & &a{multiplier}x&7 speed on hit." ] }
    mechanics:
      - type: "core:dodge"
        filters: [ { state: "armor:empty" } ]
        parameters:
          chance: { linear: { base: 50.0, step: 0.4, max: 70.0 } }
      - type: "core:speed_bonus"
        filters: [ { state: "armor:empty" } ]
        parameters:
          multiplier: { linear: { base: 1.3, step: 0.002, max: 1.4 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
```

The following skills continue in the next insertion.

### shields.yml — Shields

**Issues**
1. [VIOLATION][B][E-III][DESIGN] `shield_bash` L15 is implemented as `core:thorns_damage` (passive reflect on damage **taken**) — the name "Shield Bash" implies an **active** knockback attack, but it passively reflects flat damage to attackers while blocking. No active input, no knockback, no shield disable. Needs new `core:shield_bash` (`P2-2` knockback + `P2-3` shield_disable) mechanic.
2. [BUG][G] `wall_of_steel` L25 `core:knockback_resist` + `is_blocking` — no event guard; `is_blocking` passes on **any** event while shield is raised → stacks knockback resist on every event (sprint, sneak, break, etc.).
3. [BUG][G] `protective_aura` L75 `core:armor_bonus` + `is_blocking` — same stacking issue.
4. [SHORTCOMING][F] `indomitable` L100 constants (50.0 block, 5.0 thorns).
5. [DESIGN][B] No active abilities at all. L25 "primary ability" is passive `wall_of_steel`. §3 wants L25 active — Shield Bash (sneak+right-click with shield) is the canonical hook.
6. [DESIGN][E-IV] Shield Bash is PvP-flavored; PvE utility = knockback mobs away for breathing room. Passes §1-IV as secondary.

**Redesign**

```yaml
id: "shields"
max_level: 100
display: { name: "Shields", icon: "minecraft:shield", color: "BLUE", style: "SOLID" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage_taken"
    filters: [ { state: "is_blocking" } ]
    reward: { constant: 5.0 }
abilities:
  - id: "sturdy_guard"
    display_name: "Sturdy Guard"
    unlock_level: 1
    trigger: "entity_damage_taken"
    display: { lore: [ "&a{chance}%&7 chance to block damage while guarding." ] }
    mechanics:
      - type: "core:block_damage"
        filters: [ { state: "is_blocking" } ]
        parameters:
          chance: { linear: { base: 10.0, step: 0.2, max: 30.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "reflect"
    display_name: "Reflect"
    unlock_level: 15
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Attackers take &a{damage}&7 while you block." ] }
    mechanics:
      - type: "core:thorns_damage"
        filters: [ { state: "is_blocking" } ]
        parameters:
          damage: { linear: { base: 1.0, step: 0.03, max: 5.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "shield_bash"
    display_name: "Shield Bash"
    unlock_level: 25
    trigger: "player_interact"
    display:
      lore:
        - "&7Sneak+Right-Click shield to knock back nearby foes &a{force}&7"
        - "&7and disable their shields for {ticks} ticks."
        - "&8Requires: Sneaking, shield in hand."
    requirements:
      cooldown: 8.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "minecraft:shield", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 2.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&eShield Bash cooling down: {time}s" } }
    mechanics:
      - type: "core:knockback"            # P2-2
        parameters:
          force: { linear: { base: 1.0, step: 0.02, max: 2.0 } }
          radius: { linear: { base: 2.0, step: 0.03, max: 5.0 } }
          vertical: { constant: 0.4 }
      - type: "core:shield_disable"        # P2-3
        parameters:
          ticks: { linear: { base: 20.0, step: 0.5, max: 60.0 } }
      - type: "core:set_cooldown"          # P2-9 self-disable for balance
        parameters:
          material: { constant: "minecraft:shield" }
          ticks: { linear: { base: 40.0, step: -0.2, max: 20.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&b✦ Shield Bash! ✦" }
      sounds: [ { type: "ITEM_SHIELD_BLOCK", volume: 0.6, pitch: 0.8, target: "self" } ]
  - id: "wall_of_steel"
    display_name: "Wall of Steel"
    unlock_level: 50
    trigger: "entity_damage_taken"
    display: { lore: [ "&7While blocking: +&a{amount}&7 knockback resistance for {duration}s on hit." ] }
    mechanics:
      - type: "core:knockback_resist"
        filters: [ { state: "is_blocking" } ]
        parameters:
          amount: { linear: { base: 0.2, step: 0.005, max: 0.6 } }
          duration: { constant: 6.0 }
    feedback: { notify: { action_bar: false } }
  - id: "shield_slam"
    display_name: "Shield Slam"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&a{chance}%&7 chance to negate damage entirely while blocking +&a{amount}&7 armor." ] }
    mechanics:
      - type: "core:cancel_damage"
        filters: [ { state: "is_blocking" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
      - type: "core:armor_bonus"
        filters: [ { state: "is_blocking" } ]
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "indomitable"
    display_name: "Indomitable"
    unlock_level: 100
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Blocking: &a{chance}%&7 block & &a{damage}&7 reflect, knockback resist +&a{kb}&7." ] }
    mechanics:
      - type: "core:block_damage"
        filters: [ { state: "is_blocking" } ]
        parameters:
          chance: { linear: { base: 50.0, step: 0.5, max: 70.0 } }
      - type: "core:thorns_damage"
        filters: [ { state: "is_blocking" } ]
        parameters:
          damage: { linear: { base: 5.0, step: 0.05, max: 8.0 } }
      - type: "core:knockback_resist"
        filters: [ { state: "is_blocking" } ]
        parameters:
          amount: { linear: { base: 0.6, step: 0.005, max: 0.9 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
```

---

### light_weapons.yml — Light Weapons

**Issues**
1. [BUG][C][P0] `entity_damage` / `entity_kill` XP filtered `target: "#c:light_weapons"` — for melee, `resolveEventMaterial` returns `null` (Player damager is not a Projectile). **XP is never granted; no ability fires.** Must use `tool:` not `target:`.
2. [BUG][P0] ALL abilities (`quick_strike`, `nimble`, `bleed`, `vampiric_strike`, `evasive`, `blade_dancer`) use `target: "#c:light_weapons"` filter → **none ever fire** for melee. (Arrows work because the damager is an Arrow projectile.)
3. [BUG][G] `nimble` L15 `core:speed_bonus` — even with `tool:` fix, no event guard; stacks speed on every damage event.
4. [DESIGN][B] `bleed` L25 passive `apply_status` (poison, id 19) — L25 is the primary slot but passive. §3 wants active.
5. [SHORTCOMING][F] `blade_dancer` L100 `constant: 2.5`.

**Redesign**

```yaml
id: "light_weapons"
max_level: 100
display: { name: "Light Weapons", icon: "minecraft:golden_sword", color: "RED", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage"
    filters: [ { tool: "#c:light_weapons" } ]      # tool = held item (FIX: was target)
    reward: { constant: 5.0 }
  - trigger: "entity_kill"
    filters: [ { tool: "#c:light_weapons" } ]
    reward: { constant: 20.0 }
abilities:
  - id: "quick_strike"
    display_name: "Quick Strike"
    unlock_level: 1
    trigger: "entity_damage"
    display: { lore: [ "&7Light-weapon damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { tool: "#c:light_weapons" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.004, max: 1.4 } }
    feedback: { notify: { action_bar: false } }
  - id: "nimble"
    display_name: "Nimble"
    unlock_level: 15
    trigger: "entity_damage"
    display: { lore: [ "&7Light-weapon hits grant &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        filters: [ { tool: "#c:light_weapons" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.002, max: 1.25 } }
          duration: { constant: 6.0 }
    feedback: { notify: { action_bar: false } }
  - id: "flurry"
    display_name: "Flurry"
    unlock_level: 25
    trigger: "entity_damage"
    display:
      lore:
        - "&7Shift-attack with a light weapon for &a{multiplier}x&7 damage + poison."
        - "&8Requires: Sneaking, light weapon."
    requirements:
      cooldown: 5.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "#c:light_weapons", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&eFlurry cooling down: {time}s" } }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { tool: "#c:light_weapons" } ]
        parameters:
          multiplier: { linear: { base: 1.3, step: 0.01, max: 1.6 } }
      - type: "core:apply_status"
        filters: [ { tool: "#c:light_weapons" } ]
        parameters:
          effect: { constant: "minecraft:poison" }     # P2-11 namespaced
          duration: { linear: { base: 3.0, step: 0.04, max: 7.0 } }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: true, chat: false, message: "&c✦ Flurry! ✦" } }
  - id: "vampiric_strike"
    display_name: "Vampiric Strike"
    unlock_level: 50
    trigger: "entity_damage"
    display: { lore: [ "&7Heal &a{percentage}%&7 of damage dealt with light weapons." ] }
    mechanics:
      - type: "core:lifesteal"
        filters: [ { tool: "#c:light_weapons" } ]
        parameters:
          percentage: { linear: { base: 3.0, step: 0.06, max: 10.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "evasive"
    display_name: "Evasive"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Holding a light weapon: &a{chance}%&7 dodge." ] }
    mechanics:
      - type: "core:dodge"
        filters: [ { tool: "#c:light_weapons" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "blade_dancer"
    display_name: "Blade Dancer"
    unlock_level: 100
    trigger: "entity_damage"
    display: { lore: [ "&7Light-weapon damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { tool: "#c:light_weapons" } ]
        parameters:
          multiplier: { linear: { base: 2.0, step: 0.02, max: 2.8 } }
    feedback: { notify: { action_bar: false } }
```

---

### one_handed.yml — One Handed

**Issues**
1. [BUG][G] `quick_hands` L15 `core:speed_bonus` + `offhand:empty` — no event guard; `offhand:empty` passes on **any** event → stacks speed on every event when offhand is empty.
2. [BUG][D] `duelist` L75 — lore "increased attack speed" but uses `core:lifesteal` (heals % of damage). Lifesteal ≠ attack speed. Mismatched description; should use `core:modify_attack_speed` (P2-10) or fix the lore.
3. [DESIGN][B] `parry` L25 is passive `dodge` — §3 wants L25 active. Native input: sneak+left-click directional intersect to parry (cancel next hit).
4. [SHORTCOMING][F] `blade_master` L100 `constant: 2.0`.
5. [DESIGN][C] XP uses `state:offhand:empty` — works correctly (state filter). Good.

**Redesign**

```yaml
id: "one_handed"
max_level: 100
display: { name: "One Handed", icon: "minecraft:iron_sword", color: "RED", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage"
    filters: [ { state: "offhand:empty" }, { tool: "#minecraft:swords" } ]
    reward: { constant: 5.0 }
  - trigger: "entity_kill"
    filters: [ { state: "offhand:empty" }, { tool: "#minecraft:swords" } ]
    reward: { constant: 20.0 }
abilities:
  - id: "precise_strikes"
    display_name: "Precise Strikes"
    unlock_level: 1
    trigger: "entity_damage"
    display: { lore: [ "&7Sword damage &a{multiplier}x&7 with free offhand." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { state: "offhand:empty" }, { tool: "#minecraft:swords" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.003, max: 1.35 } }
    feedback: { notify: { action_bar: false } }
  - id: "quick_hands"
    display_name: "Quick Hands"
    unlock_level: 15
    trigger: "entity_damage"
    display: { lore: [ "&7Sword hits with free offhand grant &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        filters: [ { state: "offhand:empty" }, { tool: "#minecraft:swords" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.002, max: 1.25 } }
          duration: { constant: 6.0 }
    feedback: { notify: { action_bar: false } }
  - id: "parry"
    display_name: "Parry"
    unlock_level: 25
    trigger: "entity_damage_taken"
    display:
      lore:
        - "&7Sneak+Left-Click with a sword & free offhand to negate next hit &a{chance}%&7."
        - "&8Requires: Sneaking, sword, offhand empty."
    requirements:
      cooldown: 5.0
      state: [ "is_sneaking", "offhand:empty" ]
      items: [ { action: "possession", tag: "#minecraft:swords", slot: "MAIN_HAND" } ]
    on_failure: { cooldown: { action_bar: "&eParry cooling down: {time}s" } }
    mechanics:
      - type: "core:cancel_damage"
        parameters:
          chance: { linear: { base: 50.0, step: 0.4, max: 85.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&b✦ Parry! ✦" }
      sounds: [ { type: "ITEM_SHIELD_BLOCK", volume: 0.4, pitch: 1.5, target: "self" } ]
  - id: "fencing"
    display_name: "Fencing"
    unlock_level: 50
    trigger: "entity_damage"
    display: { lore: [ "&7Sword hits slow targets &a{duration}s&7." ] }
    mechanics:
      - type: "core:apply_status"
        filters: [ { state: "offhand:empty" }, { tool: "#minecraft:swords" } ]
        parameters:
          effect: { constant: "minecraft:slowness" }    # P2-11
          duration: { linear: { base: 2.0, step: 0.03, max: 5.0 } }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: false } }
  - id: "duelist"
    display_name: "Duelist"
    unlock_level: 75
    trigger: "entity_damage"
    display: { lore: [ "&7Sword hits with free offhand grant &a{multiplier}x&7 attack speed for {duration}s." ] }
    mechanics:
      - type: "core:modify_attack_speed"     # P2-10 (was lifesteal — mismatched)
        filters: [ { state: "offhand:empty" }, { tool: "#minecraft:swords" } ]
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.003, max: 1.4 } }
          duration: { constant: 6.0 }
    feedback: { notify: { action_bar: false } }
  - id: "blade_master"
    display_name: "Blade Master"
    unlock_level: 100
    trigger: "entity_damage"
    display: { lore: [ "&7Sword damage &a{multiplier}x&7 with free offhand." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { state: "offhand:empty" }, { tool: "#minecraft:swords" } ]
        parameters:
          multiplier: { linear: { base: 1.8, step: 0.02, max: 2.3 } }
    feedback: { notify: { action_bar: false } }
```

---

### heavy_weapons.yml — Heavy Weapons

**Issues**
1. [BUG][C][P0] `entity_damage` / `entity_kill` XP filtered `target: "#c:heavy_weapons"` — same melee `target` filter bug as light_weapons. **XP never granted; abilities never fire.** Use `tool:`.
2. [BUG][P0] All abilities (`heavy_strikes`, `stun_impact`, `cleave`, `berserker`, `execute`, `master_at_arms`) use `target: "#c:heavy_weapons"` → none fire for melee.
3. [DESIGN][B] `cleave` L25 is passive `crowd_control` (weakness, id 18 to nearby). §3 wants L25 active.
4. [SHORTCOMING][F] `master_at_arms` L100 `constant: 2.0`.
5. [BUG][D] `stun_impact` effect:2 (slowness) — "slow targets" correct. `cleave` effect:18 (weakness) — "weaken" correct. OK semantics.

**Redesign**

```yaml
id: "heavy_weapons"
max_level: 100
display: { name: "Heavy Weapons", icon: "minecraft:iron_sword", color: "RED", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage"
    filters: [ { tool: "#c:heavy_weapons" } ]       # FIX: was target
    reward: { constant: 5.0 }
  - trigger: "entity_kill"
    filters: [ { tool: "#c:heavy_weapons" } ]
    reward: { constant: 20.0 }
abilities:
  - id: "heavy_strikes"
    display_name: "Heavy Strikes"
    unlock_level: 1
    trigger: "entity_damage"
    display: { lore: [ "&7Heavy-weapon damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { tool: "#c:heavy_weapons" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.003, max: 1.35 } }
    feedback: { notify: { action_bar: false } }
  - id: "stun_impact"
    display_name: "Stun Impact"
    unlock_level: 15
    trigger: "entity_damage"
    display: { lore: [ "&7Heavy hits slow targets &a{duration}s&7." ] }
    mechanics:
      - type: "core:apply_status"
        filters: [ { tool: "#c:heavy_weapons" } ]
        parameters:
          effect: { constant: "minecraft:slowness" }    # P2-11
          duration: { linear: { base: 2.0, step: 0.02, max: 4.0 } }
          amplifier: { constant: 1.0 }
    feedback: { notify: { action_bar: false } }
  - id: "cleave"
    display_name: "Cleave"
    unlock_level: 25
    trigger: "entity_damage"
    display:
      lore:
        - "&7Shift-attack to cleave: &a{multiplier}x&7 damage + weakness in &a{radius}&7 radius."
        - "&8Requires: Sneaking, heavy weapon."
    requirements:
      cooldown: 6.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "#c:heavy_weapons", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 1.5, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&eCleave cooling down: {time}s" } }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { tool: "#c:heavy_weapons" } ]
        parameters:
          multiplier: { linear: { base: 1.2, step: 0.01, max: 1.5 } }
      - type: "core:crowd_control"
        filters: [ { tool: "#c:heavy_weapons" } ]
        parameters:
          effect: { constant: "minecraft:weakness" }   # P2-11
          radius: { linear: { base: 2.0, step: 0.03, max: 5.0 } }
          duration: { linear: { base: 3.0, step: 0.05, max: 6.0 } }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: true, chat: false, message: "&c✦ Cleave! ✦" } }
  - id: "berserker"
    display_name: "Berserker"
    unlock_level: 50
    trigger: "entity_damage"
    display: { lore: [ "&7Heal &a{percentage}%&7 of heavy-weapon damage." ] }
    mechanics:
      - type: "core:lifesteal"
        filters: [ { tool: "#c:heavy_weapons" } ]
        parameters:
          percentage: { linear: { base: 2.0, step: 0.05, max: 8.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "execute"
    display_name: "Execute"
    unlock_level: 75
    trigger: "entity_damage"
    display: { lore: [ "&7Heavy hits kill targets below &a{threshold}%&7 HP." ] }
    mechanics:
      - type: "core:execute"
        filters: [ { tool: "#c:heavy_weapons" } ]
        parameters:
          threshold: { linear: { base: 3.0, step: 0.05, max: 7.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&c✦ Execute! ✦" }
      sounds: [ { type: "ENTITY_PLAYER_ATTACK_CRIT", volume: 0.5, pitch: 0.8, target: "self" } ]
  - id: "master_at_arms"
    display_name: "Master at Arms"
    unlock_level: 100
    trigger: "entity_damage"
    display: { lore: [ "&7Heavy-weapon damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { tool: "#c:heavy_weapons" } ]
        parameters:
          multiplier: { linear: { base: 1.8, step: 0.02, max: 2.4 } }
    feedback: { notify: { action_bar: false } }
```

---

### dual_wield.yml — Dual Wield

**Issues**
1. [VIOLATION][B][E-III][DESIGN] No actual two-weapon loop exists — all abilities are passive buffs gated only by `state:offhand:weapon`. There is no offhand-attack trigger, no swap mechanic, no offhand durability consumption. Needs `core:offhand_strike` (P2-4) for a genuine two-weapon loop (left-click → offhand strike).
2. [BUG][G] `rapid_assault` L50 `core:speed_bonus` + `offhand:weapon` — no event guard; stacks speed on every event while offhand holds a weapon.
3. [DESIGN][B] `cross_cut` L25 is passive `apply_status` (poison, id 19). §3 wants L25 active — the offhand strike IS the canonical dual-wield hook.
4. [SHORTCOMING][F] `storm_blade` L100 `constant: 2.5`.
5. [DESIGN][E-IV] `dual_parry` L75 `block_damage` + `offhand:weapon` — guarded (EntityDamageEvent) ✓. PvE: parrying mobs. OK.

**Redesign**

```yaml
id: "dual_wield"
max_level: 100
display: { name: "Dual Wield", icon: "minecraft:netherite_sword", color: "RED", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage"
    filters: [ { state: "offhand:weapon" } ]
    reward: { constant: 6.0 }
  - trigger: "entity_kill"
    filters: [ { state: "offhand:weapon" } ]
    reward: { constant: 25.0 }
abilities:
  - id: "dual_strike"
    display_name: "Dual Strike"
    unlock_level: 1
    trigger: "entity_damage"
    display: { lore: [ "&7Main-hand damage &a{multiplier}x&7 while offhand holds a weapon." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { state: "offhand:weapon" }, { tool: "#minecraft:swords" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.003, max: 1.35 } }
    feedback: { notify: { action_bar: false } }
  - id: "whirlwind"
    display_name: "Whirlwind"
    unlock_level: 15
    trigger: "entity_damage"
    display: { lore: [ "&7Main-hand hits weaken nearby foes." ] }
    mechanics:
      - type: "core:crowd_control"
        filters: [ { state: "offhand:weapon" }, { tool: "#minecraft:swords" } ]
        parameters:
          effect: { constant: "minecraft:weakness" }     # P2-11
          radius: { linear: { base: 2.0, step: 0.03, max: 5.0 } }
          duration: { linear: { base: 3.0, step: 0.04, max: 6.0 } }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: false } }
  - id: "offhand_strike"
    display_name: "Offhand Strike"
    unlock_level: 25
    trigger: "player_interact"
    display:
      lore:
        - "&7Left-Click with a weapon in offhand to strike for &a{multiplier}x&7 offhand damage."
        - "&7Applies poison &a{duration}s&7."
        - "&8Requires: Sneaking, weapon in offhand."
    requirements:
      cooldown: 3.0
      state: [ "is_sneaking", "offhand:weapon" ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&eOffhand Strike cooling down: {time}s" } }
    mechanics:
      - type: "core:offhand_strike"        # P2-4
        parameters:
          multiplier: { linear: { base: 0.6, step: 0.01, max: 1.0 } }
          reach: { constant: 4.0 }
      - type: "core:apply_status"
        parameters:
          effect: { constant: "minecraft:poison" }     # P2-11
          duration: { linear: { base: 2.0, step: 0.03, max: 5.0 } }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: true, chat: false, message: "&c✦ Offhand Strike! ✦" } }
  - id: "rapid_assault"
    display_name: "Rapid Assault"
    unlock_level: 50
    trigger: "entity_damage"
    display: { lore: [ "&7Dual-wield hits grant &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        filters: [ { state: "offhand:weapon" }, { tool: "#minecraft:swords" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.002, max: 1.25 } }
          duration: { constant: 6.0 }
    feedback: { notify: { action_bar: false } }
  - id: "dual_parry"
    display_name: "Dual Parry"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&7With a weapon in offhand: &a{chance}%&7 block chance." ] }
    mechanics:
      - type: "core:block_damage"
        filters: [ { state: "offhand:weapon" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "storm_blade"
    display_name: "Storm Blade"
    unlock_level: 100
    trigger: "entity_damage"
    display: { lore: [ "&7Dual-wield main-hand damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { state: "offhand:weapon" }, { tool: "#minecraft:swords" } ]
        parameters:
          multiplier: { linear: { base: 2.0, step: 0.03, max: 2.8 } }
    feedback: { notify: { action_bar: false } }
```

---

### throwing.yml — Throwing

**Issues**
1. [BUG][G] `master_thrower` L75 `core:speed_bonus` — no event guard, no filter; stacks speed on **every** event (not just throwing). Lore "after throwing" but no `launch_projectile`/`shoot_bow` gating.
2. [DESIGN][B] `velocity` L25 is passive `modify_damage` — §3 wants L25 active. But throwing is projectile-based; an "active" trident throw is just throwing. Reasonable to make L25 a trident-charge buff (sneak+right-click to wind up).
3. [SHORTCOMING][F] `legendary_throw` L100 `constant: 3.0`.
4. [BUG][C] `launch_projectile` XP filtered `target:minecraft:trident` — works (ProjectileLaunchEvent resolves trident material via `projectileToMaterial`). OK.
5. [BUG][A] `precise_throw`, `velocity`, `trick_shot`, `legendary_throw` use `target:minecraft:trident` on `entity_damage` — on hit, the **damager** is the Trident projectile → `projectileToMaterial` returns `Material.TRIDENT` → matches. OK, these work (unlike light/heavy melee).

**Redesign**

```yaml
id: "throwing"
max_level: 100
display: { name: "Throwing", icon: "minecraft:trident", color: "RED", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "launch_projectile"
    filters: [ { target: "minecraft:trident" } ]
    reward: { constant: 8.0 }
  - trigger: "entity_damage"
    filters: [ { target: "minecraft:trident" } ]
    reward: { constant: 5.0 }
abilities:
  - id: "precise_throw"
    display_name: "Precise Throw"
    unlock_level: 1
    trigger: "entity_damage"
    display: { lore: [ "&7Trident damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { target: "minecraft:trident" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.004, max: 1.4 } }
    feedback: { notify: { action_bar: false } }
  - id: "return_chance"
    display_name: "Return Chance"
    unlock_level: 15
    trigger: "launch_projectile"
    display: { lore: [ "&a{chance}%&7 chance to recover thrown tridents." ] }
    mechanics:      # NOTE: projectile_return fires on ProjectileHitEvent, not launch — trigger field should be a separate "projectile_hit" trigger. See Part 3.
      - type: "core:projectile_return"
        parameters:
          chance: { linear: { base: 20.0, step: 0.5, max: 75.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "wind_up"
    display_name: "Wind Up"
    unlock_level: 25
    trigger: "shoot_bow"
    display:
      lore:
        - "&7Sneak-throw a trident for &a{multiplier}x&7 damage on next throw."
        - "&8Requires: Sneaking, trident in hand."
    requirements:
      cooldown: 5.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "minecraft:trident", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&eWind Up cooling down: {time}s" } }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { target: "minecraft:trident" } ]
        parameters:
          multiplier: { linear: { base: 1.3, step: 0.01, max: 1.6 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&c✦ Wind Up! ✦" } }
  - id: "trick_shot"
    display_name: "Trick Shot"
    unlock_level: 50
    trigger: "entity_damage"
    display: { lore: [ "&7Trident hits slow targets &a{duration}s&7." ] }
    mechanics:
      - type: "core:apply_status"
        filters: [ { target: "minecraft:trident" } ]
        parameters:
          effect: { constant: "minecraft:slowness" }      # P2-11
          duration: { linear: { base: 2.0, step: 0.03, max: 5.0 } }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: false } }
  - id: "master_thrower"
    display_name: "Master Thrower"
    unlock_level: 75
    trigger: "entity_damage"
    display: { lore: [ "&7Trident hits grant &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        filters: [ { target: "minecraft:trident" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.002, max: 1.25 } }
          duration: { constant: 6.0 }
    feedback: { notify: { action_bar: false } }
  - id: "legendary_throw"
    display_name: "Legendary Throw"
    unlock_level: 100
    trigger: "entity_damage"
    display: { lore: [ "&7Trident damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { target: "minecraft:trident" } ]
        parameters:
          multiplier: { linear: { base: 2.5, step: 0.03, max: 3.3 } }
    feedback: { notify: { action_bar: false } }
```

---

### archery.yml — Archery

**Issues**
1. [BUG][D][G] `quick_reload` L15 `core:haste_effect` + `tool:#minecraft:bows` — lore "reduces bow draw time" but **Haste does NOT affect bow draw speed**. Haste (PotionEffectType.HASTE) only modifies mining/digging. Bow draw is governed by the bow item's `bow` action timing, not Haste. Completely wrong mechanic; no available mechanic reduces draw time without NMS. Redesign to a different benefit (e.g., arrow damage or projectile return).
2. [BUG][G] `quick_reload` fires on every event holding a bow (no event guard). Even if the mechanic were correct, scope is wrong.
3. [BUG][D][A] `piercing_shot` L25 `core:modify_damage` — lore "arrows pierce through targets" but `modify_damage` only multiplies damage; there is no piercing mechanic. Misleading description.
4. [BUG][G] `fleet_footed` L75 `core:speed_bonus` + `tool:#minecraft:bows` — no event guard; stacks speed on every event holding a bow.
5. [SHORTCOMING][F] `eagle_eye` L100 `constant: 3.5` + lore "critical hits on distant targets" — no distance check exists; just flat damage.
6. [BUG][C] `entity_damage`/`entity_kill` XP filtered `target:#minecraft:arrows` — on hit, damager is Arrow projectile → `projectileToMaterial` returns `Material.ARROW` → matches `#minecraft:arrows`. Works (trident uses different material). OK.

**Redesign**

```yaml
id: "archery"
max_level: 100
display: { name: "Archery", icon: "minecraft:bow", color: "RED", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage"
    filters: [ { target: "#minecraft:arrows" } ]
    reward: { constant: 5.0 }
  - trigger: "entity_kill"
    filters: [ { target: "#minecraft:arrows" } ]
    reward: { constant: 15.0 }
abilities:
  - id: "steady_hand"
    display_name: "Steady Hand"
    unlock_level: 1
    trigger: "entity_damage"
    display: { lore: [ "&7Arrow damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { target: "#minecraft:arrows" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.0045, max: 1.5 } }
    feedback: { notify: { action_bar: false } }
  - id: "arrow_recovery"
    display_name: "Arrow Recovery"
    unlock_level: 15
    trigger: "shoot_bow"
    display: { lore: [ "&a{chance}%&7 chance arrows are not consumed on shoot." ] }
    mechanics:
      - type: "core:projectile_return"
        parameters:
          chance: { linear: { base: 10.0, step: 0.4, max: 50.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "power_shot"
    display_name: "Power Shot"
    unlock_level: 25
    trigger: "shoot_bow"
    display:
      lore:
        - "&7Sneak-fire a bow for &a{multiplier}x&7 arrow damage on next shot."
        - "&8Requires: Sneaking, bow."
    requirements:
      cooldown: 5.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "#minecraft:bows", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&ePower Shot cooling down: {time}s" } }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { target: "#minecraft:arrows" } ]
        parameters:
          multiplier: { linear: { base: 1.4, step: 0.01, max: 1.8 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&c✦ Power Shot! ✦" }
      sounds: [ { type: "ENTITY_ARROW_HIT_PLAYER", volume: 0.5, pitch: 1.2, target: "self" } ]
  - id: "mark_target"
    display_name: "Mark Target"
    unlock_level: 50
    trigger: "entity_damage"
    display: { lore: [ "&7Arrow hits slow targets &a{duration}s&7." ] }
    mechanics:
      - type: "core:apply_status"
        filters: [ { target: "#minecraft:arrows" } ]
        parameters:
          effect: { constant: "minecraft:slowness" }      # P2-11
          duration: { linear: { base: 2.0, step: 0.03, max: 5.0 } }
          amplifier: { constant: 1.0 }
    feedback: { notify: { action_bar: false } }
  - id: "fleet_footed"
    display_name: "Fleet-Footed"
    unlock_level: 75
    trigger: "shoot_bow"
    display: { lore: [ "&7Firing a bow grants &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        filters: [ { tool: "#minecraft:bows" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.002, max: 1.25 } }
          duration: { constant: 6.0 }
    feedback: { notify: { action_bar: false } }
  - id: "eagle_eye"
    display_name: "Eagle Eye"
    unlock_level: 100
    trigger: "entity_damage"
    display: { lore: [ "&7Arrow damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { target: "#minecraft:arrows" } ]
        parameters:
          multiplier: { linear: { base: 2.5, step: 0.03, max: 3.5 } }
    feedback: { notify: { action_bar: false } }
```

---

### unarmed.yml — Unarmed

**Issues**
1. [BUG][G] `chi_flow` L50 `core:speed_bonus` — no event guard, no filter; stacks speed on **every** event (not just combat). Lore "in combat" but no combat-context gate.
2. [DESIGN][B] `pressure_points` L25 passive `apply_status` (weakness, id 18) — §3 wants L25 active. Native input: sneak+left-click pressure-point strike.
3. [SHORTCOMING][F] `master_martial_artist` L100 `constant: 2.5`.
4. [BUG][C] `entity_damage`/`entity_kill` XP filtered `state:hand:empty` — works (state filter). Good. But `hand:empty` means **both** hands empty; a player holding food in offhand gets no XP. Acceptable for "unarmed" identity.
5. [DESIGN][E-IV] `lightning_reflexes` L15 `core:dodge` with no `hand:empty` filter → dodges while holding a sword. Should require empty hands for the "unarmed" identity.

**Redesign**

```yaml
id: "unarmed"
max_level: 100
display: { name: "Unarmed", icon: "minecraft:player_head", color: "RED", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage"
    filters: [ { state: "hand:empty" } ]
    reward: { constant: 5.0 }
  - trigger: "entity_kill"
    filters: [ { state: "hand:empty" } ]
    reward: { constant: 25.0 }
abilities:
  - id: "iron_fist"
    display_name: "Iron Fist"
    unlock_level: 1
    trigger: "entity_damage"
    display: { lore: [ "&7Unarmed damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { state: "hand:empty" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.005, max: 1.5 } }
    feedback: { notify: { action_bar: false } }
  - id: "lightning_reflexes"
    display_name: "Lightning Reflexes"
    unlock_level: 15
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Unarmored & empty-handed: &a{chance}%&7 dodge." ] }
    mechanics:
      - type: "core:dodge"
        filters: [ { state: "hand:empty" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "pressure_points"
    display_name: "Pressure Points"
    unlock_level: 25
    trigger: "entity_damage"
    display:
      lore:
        - "&7Sneak-strike unarmed to weaken targets &a{duration}s&7 & bonus damage."
        - "&8Requires: Sneaking, empty hands."
    requirements:
      cooldown: 5.0
      state: [ "is_sneaking", "hand:empty" ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&ePressure Points cooling down: {time}s" } }
    mechanics:
      - type: "core:apply_status"
        filters: [ { state: "hand:empty" } ]
        parameters:
          effect: { constant: "minecraft:weakness" }      # P2-11
          duration: { linear: { base: 3.0, step: 0.03, max: 6.0 } }
          amplifier: { constant: 1.0 }
      - type: "core:modify_damage"
        filters: [ { state: "hand:empty" } ]
        parameters:
          multiplier: { linear: { base: 1.2, step: 0.01, max: 1.5 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&c✦ Pressure Points! ✦" } }
  - id: "chi_flow"
    display_name: "Chi Flow"
    unlock_level: 50
    trigger: "entity_damage"
    display: { lore: [ "&7Unarmed hits grant &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        filters: [ { state: "hand:empty" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.002, max: 1.25 } }
          duration: { constant: 6.0 }
    feedback: { notify: { action_bar: false } }
  - id: "counter_strike"
    display_name: "Counter Strike"
    unlock_level: 75
    trigger: "entity_damage"
    display: { lore: [ "&7Heal &a{percentage}%&7 of unarmed damage dealt." ] }
    mechanics:
      - type: "core:lifesteal"
        filters: [ { state: "hand:empty" } ]
        parameters:
          percentage: { linear: { base: 3.0, step: 0.05, max: 10.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "master_martial_artist"
    display_name: "Master Martial Artist"
    unlock_level: 100
    trigger: "entity_damage"
    display: { lore: [ "&7Unarmed damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { state: "hand:empty" } ]
        parameters:
          multiplier: { linear: { base: 2.0, step: 0.03, max: 2.8 } }
    feedback: { notify: { action_bar: false } }
```

The following skills continue in the next insertion.

### acrobatics.yml — Acrobatics

**Issues**
1. [BUG][C] `sprint` XP 1.0 + `sneak` XP 1.0 → **XP for toggling sprint/sneak state** (PlayerToggleSprintEvent / PlayerToggleSneakEvent). Players can spam sprint/sneak toggle for unlimited XP. `entity_damage_taken` 2.0 for any damage (fall, fire, void, etc.).
2. [BUG][G] `light_feet` L1, `agile` L50, `gravity_defier` L100 `core:speed_bonus` — no event guard; stacks `MOVEMENT_SPEED` on every event. The L1 foundational passive is essentially permanent always-on with stacking.
3. [BUG][G] `spring_step` L25, `endurance` L75, `gravity_defier` L100 `core:modify_jump` — no event guard; stacks `JUMP_STRENGTH` on every event. 600s (10min) duration = effectively permanent.
4. [DESIGN][B] All passive. No active abilities. §3 wants L25 active (sneak+jump mobility vector is the canonical acrobatics hook — §4.3).
5. [DESIGN][C] `sprint`/`sneak` XP rewards trivial state toggles, not meaningful action. Should be fall-distance or distance-traveled based — but no such trigger exists. Remove sprint/sneak XP; keep fall-damage XP.

**Redesign**

```yaml
id: "acrobatics"
max_level: 100
display: { name: "Acrobatics", icon: "minecraft:feather", color: "WHITE", style: "SOLID" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "entity_damage_taken"
    filters: [ { state: "is_on_ground" } ]     # fall damage context (best available proxy)
    reward: { constant: 3.0 }
  # sprint/sneak XP removed — trivial toggle spam
abilities:
  - id: "light_feet"
    display_name: "Light Feet"
    unlock_level: 1
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Taking fall damage grants &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.002, max: 1.25 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "soft_landing"
    display_name: "Soft Landing"
    unlock_level: 15
    trigger: "entity_damage_taken"
    display: { lore: [ "&a{chance}%&7 chance to negate fall damage." ] }
    mechanics:
      - type: "core:cancel_damage"
        filters: [ { state: "is_on_ground" } ]
        parameters:
          chance: { linear: { base: 10.0, step: 0.3, max: 40.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "leap"
    display_name: "Leap"
    unlock_level: 25
    trigger: "sneak"
    display:
      lore:
        - "&7Sneak+Jump to leap &a{multiplier}x&7 higher for {duration}s."
        - "&8Requires: Sneaking, on ground."
    requirements:
      cooldown: 8.0
      state: [ "is_sneaking", "is_on_ground" ]
    on_failure: { cooldown: { action_bar: "&eLeap cooling down: {time}s" } }
    mechanics:
      - type: "core:modify_jump"
        parameters:
          multiplier: { linear: { base: 1.3, step: 0.01, max: 1.8 } }
          duration: { constant: 1.0 }   # single leap
    feedback:
      notify: { action_bar: true, chat: false, message: "&f✦ Leap! ✦" }
      sounds: [ { type: "ENTITY_ENDERMAN_TELEPORT", volume: 0.3, pitch: 1.5, target: "self" } ]
  - id: "agile"
    display_name: "Agile"
    unlock_level: 50
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Taking damage grants &a{chance}%&7 dodge & &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:dodge"
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
      - type: "core:speed_bonus"
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.003, max: 1.3 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "endurance"
    display_name: "Endurance"
    unlock_level: 75
    trigger: "elytra_glide"          # P2-8 synergy: gliding
    display: { lore: [ "&7Gliding grants &a{multiplier}x&7 speed & &a{jump}x&7 jump on landing for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.003, max: 1.3 } }
          duration: { constant: 15.0 }
      - type: "core:modify_jump"
        parameters:
          multiplier: { linear: { base: 1.1, step: 0.004, max: 1.5 } }
          duration: { constant: 15.0 }
    feedback: { notify: { action_bar: false } }
  - id: "gravity_defier"
    display_name: "Gravity Defier"
    unlock_level: 100
    trigger: "entity_damage_taken"
    display: { lore: [ "&7Permanent &a{multiplier}x&7 speed & &a{jump}x&7 jump while airborne-landed." ] }
    mechanics:
      - type: "core:speed_bonus"
        parameters:
          multiplier: { linear: { base: 1.3, step: 0.002, max: 1.4 } }
          duration: { linear: { base: 30.0, step: 0.5, max: 60.0 } }
      - type: "core:modify_jump"
        parameters:
          multiplier: { linear: { base: 1.6, step: 0.005, max: 1.8 } }
          duration: { linear: { base: 30.0, step: 0.5, max: 60.0 } }
    feedback: { notify: { action_bar: false } }
```

---

### riding.yml — Riding

**Issues**
1. [BUG][G] `swift_mount` L15, `legendary_rider` L100 `core:speed_bonus` + `is_riding` — no event guard; `is_riding` passes on **any** event while mounted → stacks speed on every event while riding.
2. [BUG][G] `horse_mastery` L75 `core:armor_bonus` + `is_riding` — same stacking issue.
3. [BUG][C] `entity_damage` 5.0 XP while riding — any damage while mounted (including taking hits), not just mounted combat. Acceptable but broad.
4. [BUG][D] `calvary_charge` (typo) L50 `apply_status` effect:2 (slowness) — "attacks slow targets" OK semantic.
5. [DESIGN][B] All passive. No active abilities. §3 wants L25 active (sneak+jump mounted charge fits).
6. [SHORTCOMING][F] `legendary_rider` L100 constants (2.0 damage, 1.3 speed).

**Redesign**

```yaml
id: "riding"
max_level: 100
display: { name: "Riding", icon: "minecraft:saddle", color: "WHITE", style: "SOLID" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "ride_horse"
    reward: { constant: 25.0 }
  - trigger: "entity_damage"
    filters: [ { state: "is_riding" } ]
    reward: { constant: 5.0 }
abilities:
  - id: "mounted_combat_training"
    display_name: "Mounted Combat Training"
    unlock_level: 1
    trigger: "entity_damage"
    display: { lore: [ "&7Mounted damage &a{multiplier}x&7." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { state: "is_riding" } ]
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.004, max: 1.35 } }
    feedback: { notify: { action_bar: false } }
  - id: "swift_mount"
    display_name: "Swift Mount"
    unlock_level: 15
    trigger: "ride_horse"
    display: { lore: [ "&7Mounting grants &a{multiplier}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:speed_bonus"
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.002, max: 1.25 } }
          duration: { constant: 30.0 }
    feedback: { notify: { action_bar: false } }
  - id: "cavalry_charge"
    display_name: "Cavalry Charge"
    unlock_level: 25
    trigger: "entity_damage"
    display:
      lore:
        - "&7Sneak-attack while mounted for &a{multiplier}x&7 damage + slow target."
        - "&8Requires: Sneaking, riding."
    requirements:
      cooldown: 6.0
      state: [ "is_sneaking", "is_riding" ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&eCavalry Charge cooling down: {time}s" } }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { state: "is_riding" } ]
        parameters:
          multiplier: { linear: { base: 1.3, step: 0.01, max: 1.6 } }
      - type: "core:apply_status"
        filters: [ { state: "is_riding" } ]
        parameters:
          effect: { constant: "minecraft:slowness" }     # P2-11
          duration: { linear: { base: 2.0, step: 0.03, max: 5.0 } }
          amplifier: { constant: 1.0 }
    feedback: { notify: { action_bar: true, chat: false, message: "&c✦ Cavalry Charge! ✦" } }
  - id: "mounted_defense"
    display_name: "Mounted Defense"
    unlock_level: 50
    trigger: "entity_damage_taken"
    display: { lore: [ "&7While riding: &a{chance}%&7 dodge." ] }
    mechanics:
      - type: "core:dodge"
        filters: [ { state: "is_riding" } ]
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "horse_mastery"
    display_name: "Horse Mastery"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&7While riding & hit: +&a{amount}&7 armor for {duration}s." ] }
    mechanics:
      - type: "core:armor_bonus"
        filters: [ { state: "is_riding" } ]
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
          duration: { constant: 8.0 }
    feedback: { notify: { action_bar: false } }
  - id: "legendary_rider"
    display_name: "Legendary Rider"
    unlock_level: 100
    trigger: "ride_horse"
    display: { lore: [ "&7Mounting grants &a{multiplier}x&7 damage & &a{speed}x&7 speed for {duration}s." ] }
    mechanics:
      - type: "core:modify_damage"
        filters: [ { state: "is_riding" } ]
        parameters:
          multiplier: { linear: { base: 1.8, step: 0.02, max: 2.3 } }
      - type: "core:speed_bonus"
        parameters:
          multiplier: { linear: { base: 1.3, step: 0.002, max: 1.4 } }
          duration: { constant: 60.0 }
    feedback: { notify: { action_bar: false } }
```

---

### bard.yml — Bard

**Issues**
1. [BUG][C][P0] `player_interact` 2.0 XP with **no filter** → XP for **every** interaction (opening doors, pressing buttons, clicking air). *(Known issue #2.)*
2. [VIOLATION][B][E-III][P0] ALL `core:field_aura` abilities (`melody_of_haste`, `rhythm_of_strength`, `song_of_protection`, `symphony`) have **no requirements, no input, no item cost** — they're passive auras that fire on **every** event (field_aura has no event guard). "Free magic" with no instrument economy. Pillar III violation. *(Known issue family.)*
3. [VIOLATION][E-III][G] `field_aura` applies effects to **all nearby LivingEntity including hostile mobs** — Bard buffs enemies. Repeated re-application on every event. Needs `core:ally_aura` (P2-5) restricting to players + instrument cost.
4. [BUG][D] `melody_of_haste` effect:3 → PotionEffectResolver id 3 = `minecraft:haste` = **mining/digging speed**, NOT movement swiftness. Lore "grants haste" — misleading (Haste ≠ Speed). Likely intended effect 1 (Speed).
5. [BUG][G] `ballad_of_endurance` L25 `core:armor_bonus` no event guard; stacks armor. `war_march` L75 `core:speed_bonus` no event guard; stacks speed.
6. [DESIGN][B] L25 `ballad_of_endurance` is a passive self-armor — §3 wants L25 active (instrument performance hook).

**Redesign**

```yaml
id: "bard"
max_level: 100
display: { name: "Bard", icon: "minecraft:jukebox", color: "PURPLE", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "player_interact"
    filters: [ { tool: "#minecraft:instrument" } ]      # goat horn / instrument items only
    reward: { constant: 2.0 }
  - trigger: "craft_item"
    filters: [ { target: "minecraft:jukebox" } ]
    reward: { constant: 10.0 }
abilities:
  - id: "melody_of_haste"
    display_name: "Melody of Haste"
    unlock_level: 1
    trigger: "player_interact"
    display: { lore: [ "&7Play an instrument to grant allies nearby Speed for {duration}s." ] }
    mechanics:
      - type: "core:ally_aura"            # P2-5 (players only)
        parameters:
          effect: { constant: "minecraft:speed" }       # P2-11 (was haste=id3, wrong)
          radius: { linear: { base: 5.0, step: 0.05, max: 10.0 } }
          duration: { constant: 5.0 }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: false } }
  - id: "rhythm_of_strength"
    display_name: "Rhythm of Strength"
    unlock_level: 15
    trigger: "player_interact"
    display: { lore: [ "&7Play an instrument to grant allies nearby Strength for {duration}s." ] }
    mechanics:
      - type: "core:ally_aura"
        parameters:
          effect: { constant: "minecraft:strength" }    # P2-11
          radius: { linear: { base: 5.0, step: 0.05, max: 10.0 } }
          duration: { constant: 5.0 }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: false } }
  - id: "perform"
    display_name: "Perform"
    unlock_level: 25
    trigger: "player_interact"
    display:
      lore:
        - "&7Sneak-play an instrument to grant allies Regeneration & armor."
        - "&7Consumes 1 goat horn durability-cooldown."
        - "&8Requires: Sneaking, instrument in hand."
    requirements:
      cooldown: 15.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "#minecraft:instruments", slot: "MAIN_HAND" } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure: { cooldown: { action_bar: "&ePerform cooling down: {time}s" } }
    mechanics:
      - type: "core:ally_aura"
        parameters:
          effect: { constant: "minecraft:regeneration" }   # P2-11
          radius: { linear: { base: 6.0, step: 0.06, max: 12.0 } }
          duration: { linear: { base: 4.0, step: 0.05, max: 8.0 } }
          amplifier: { constant: 0.0 }
      - type: "core:set_cooldown"            # P2-9 instrument reuse lockout
        parameters:
          material: { constant: "minecraft:goat_horn" }
          ticks: { linear: { base: 40.0, step: -0.2, max: 20.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&d✦ Perform! ✦" }
      sounds: [ { type: "ITEM_GOAT_HORN_PLAY", volume: 1.0, pitch: 1.0, target: "self" } ]
  - id: "song_of_protection"
    display_name: "Song of Protection"
    unlock_level: 50
    trigger: "player_interact"
    display: { lore: [ "&7Play an instrument to grant allies nearby Resistance for {duration}s." ] }
    mechanics:
      - type: "core:ally_aura"
        parameters:
          effect: { constant: "minecraft:resistance" }    # P2-11
          radius: { linear: { base: 6.0, step: 0.06, max: 12.0 } }
          duration: { linear: { base: 5.0, step: 0.05, max: 10.0 } }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: false } }
  - id: "war_march"
    display_name: "War March"
    unlock_level: 75
    trigger: "player_interact"
    display: { lore: [ "&7Play an instrument: allies gain Speed & the bard gains armor for {duration}s." ] }
    mechanics:
      - type: "core:ally_aura"
        parameters:
          effect: { constant: "minecraft:speed" }         # P2-11
          radius: { linear: { base: 8.0, step: 0.05, max: 14.0 } }
          duration: { linear: { base: 6.0, step: 0.05, max: 12.0 } }
          amplifier: { linear: { base: 0.0, step: 0.005, max: 1.0 } }
      - type: "core:armor_bonus"
        parameters:
          amount: { linear: { base: 1.0, step: 0.02, max: 3.0 } }
          duration: { linear: { base: 6.0, step: 0.1, max: 12.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "symphony"
    display_name: "Symphony"
    unlock_level: 100
    trigger: "player_interact"
    display: { lore: [ "&7Play an instrument: allies gain Haste & Strength II for {duration}s in &a{radius}&7." ] }
    mechanics:
      - type: "core:ally_aura"
        parameters:
          effect: { constant: "minecraft:haste" }         # P2-11 (here haste IS intended: mining aid)
          radius: { linear: { base: 12.0, step: 0.1, max: 16.0 } }
          duration: { linear: { base: 8.0, step: 0.1, max: 14.0 } }
          amplifier: { constant: 1.0 }
      - type: "core:ally_aura"
        parameters:
          effect: { constant: "minecraft:strength" }       # P2-11
          radius: { linear: { base: 12.0, step: 0.1, max: 16.0 } }
          duration: { linear: { base: 8.0, step: 0.1, max: 14.0 } }
          amplifier: { constant: 1.0 }
    feedback: { notify: { action_bar: false } }
```

---

### wizardry.yml — Wizardry

**Issues**
1. [BUG][C][P0] `player_interact` 1.0 XP with **no filter** → XP for every interaction. *(Known issue #2.)*
2. [BUG][B][E-III][P0] `arcane_missile` L1 `core:projectile` + **no requirements** → launches a snowball on **every** `PlayerInteractEvent` (right-click, left-click, clicking air, clicking blocks). Free snowball spell with no cooldown/state/item cost. Pillar III "free magic." *(Known issue #3.)*
3. [BUG][B][E-III][P0] `blink` L15 `core:teleport` + **no requirements** → teleports on every `PlayerInteractEvent`. Free teleport, no cooldown. Pillar III + Pillar I risk.
4. [BUG][G] `mana_shield` L25 `core:armor_bonus` — no event guard, no filter; stacks armor on every event.
5. [BUG][B][E-III] `frost_nova` L75 `core:crowd_control` — checks `EntityDamageByEntityEvent` ✓ but **no requirements, no item cost** → free AoE slow on every melee hit. Pillar III.
6. [BUG][G] `archmage` L100 `core:projectile` → fires snowball on every interact (no requirements). Plus `core:armor_bonus` stacks.
7. [VIOLATION][E-III] All wizardry abilities are **free magic** — no item cost, no catalyst, no consumption. Pillar III violation across the board. **Must require/consume vanilla items** (e.g., ender pearl for blink, redstone for arcane missile).
8. [DESIGN][B] "Active" abilities (arcane_missile, blink) have no requirements; they're active in mechanics but ungated.

**Redesign**

```yaml
id: "wizardry"
max_level: 100
display: { name: "Wizardry", icon: "minecraft:ender_pearl", color: "PURPLE", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "collect_xp"
    reward: { constant: 2.0 }
  - trigger: "launch_projectile"
    filters: [ { target: "minecraft:ender_pearl" } ]
    reward: { constant: 3.0 }
  # player_interact XP removed — was XP for existing
abilities:
  - id: "arcane_missile"
    display_name: "Arcane Missile"
    unlock_level: 1
    trigger: "player_interact"
    display:
      lore:
        - "&7Sneak+Right-Click to launch an arcane missile dealing &a{damage}&7."
        - "&7Costs 1 Redstone."
        - "&8Requires: Sneaking, redstone in inventory."
    requirements:
      cooldown: 4.0
      state: [ "is_sneaking" ]
      items: [ { action: "cost", tag: "minecraft:redstone", amount: 1 } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure:
      cooldown: { action_bar: "&eArcane Missile cooling down: {time}s" }
      missing_item: { action_bar: "&cRequires 1x Redstone!" }
    mechanics:
      - type: "core:projectile"
        parameters:
          speed: { constant: 2.0 }
          damage: { linear: { base: 2.0, step: 0.02, max: 5.0 } }
    feedback: { notify: { action_bar: true, chat: false, message: "&d✦ Arcane Missile! ✦" } }
  - id: "blink"
    display_name: "Blink"
    unlock_level: 15
    trigger: "player_interact"
    display:
      lore:
        - "&7Sneak+Right-Click to blink &a{range}&7 blocks (no ender-pearl damage)."
        - "&7Costs 1 Ender Pearl."
        - "&8Requires: Sneaking, ender pearl in inventory."
    requirements:
      cooldown: 10.0
      state: [ "is_sneaking" ]
      items: [ { action: "cost", tag: "minecraft:ender_pearl", amount: 1 } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure:
      cooldown: { action_bar: "&eBlink cooling down: {time}s" }
      missing_item: { action_bar: "&cRequires 1x Ender Pearl!" }
    mechanics:
      - type: "core:teleport"
        parameters:
          range: { linear: { base: 5.0, step: 0.05, max: 12.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&d✦ Blink! ✦" }
      particles: [ { type: "PORTAL", count: 15, offset: [0.5,0.5,0.5], speed: 0.5, target: "self" } ]
      sounds: [ { type: "ENTITY_ENDERMAN_TELEPORT", volume: 0.5, pitch: 1.5, target: "self" } ]
  - id: "arcane_ward"
    display_name: "Arcane Ward"
    unlock_level: 25
    trigger: "player_interact"
    display:
      lore:
        - "&7Sneak+Right-Click with a (consumed) Amethyst Shard for &a{amount}&7 armor &a{duration}s&7."
        - "&8Requires: Sneaking, amethyst shard."
    requirements:
      cooldown: 15.0
      state: [ "is_sneaking" ]
      items: [ { action: "cost", tag: "minecraft:amethyst_shard", amount: 1 } ]
      exhaustion: { amount: 1.0, minimum: 3.0 }
    on_failure:
      cooldown: { action_bar: "&eArcane Ward cooling down: {time}s" }
      missing_item: { action_bar: "&cRequires 1x Amethyst Shard!" }
    mechanics:
      - type: "core:armor_bonus"
        parameters:
          amount: { linear: { base: 2.0, step: 0.04, max: 5.0 } }
          duration: { linear: { base: 15.0, step: 0.2, max: 30.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&d✦ Arcane Ward! ✦" }
      sounds: [ { type: "BLOCK_AMETHYST_BLOCK_CHIME", volume: 0.6, pitch: 1.2, target: "self" } ]
  - id: "arcane_expertise"
    display_name: "Arcane Expertise"
    unlock_level: 50
    trigger: "collect_xp"
    display: { lore: [ "&7XP gains &a{multiplier}x&7 (arcane attunement)." ] }
    mechanics:
      - type: "core:xp_bonus"
        parameters:
          multiplier: { linear: { base: 1.05, step: 0.01, max: 1.6 } }
    feedback: { notify: { action_bar: false } }
  - id: "frost_nova"
    display_name: "Frost Nova"
    unlock_level: 75
    trigger: "entity_damage"
    display:
      lore:
        - "&7Sneak-hit to freeze nearby foes &a{radius}&7 for &a{duration}s&7."
        - "&7Costs 1 Blue Ice (or packed ice)."
        - "&8Requires: Sneaking, ice in inventory."
    requirements:
      cooldown: 12.0
      state: [ "is_sneaking" ]
      items: [ { action: "cost", tag: "minecraft:blue_ice", amount: 1 } ]
      exhaustion: { amount: 1.5, minimum: 3.0 }
    on_failure:
      cooldown: { action_bar: "&eFrost Nova cooling down: {time}s" }
      missing_item: { action_bar: "&cRequires 1x Blue Ice!" }
    mechanics:
      - type: "core:crowd_control"
        parameters:
          effect: { constant: "minecraft:slowness" }      # P2-11
          radius: { linear: { base: 3.0, step: 0.04, max: 6.0 } }
          duration: { linear: { base: 4.0, step: 0.05, max: 8.0 } }
          amplifier: { constant: 1.0 }
    feedback:
      notify: { action_bar: true, chat: false, message: "&b✦ Frost Nova! ✦" }
      particles: [ { type: "SNOWBALL", count: 20, offset: [1.0,0.5,1.0], speed: 0.5, target: "target" } ]
      sounds: [ { type: "ENTITY_SNOW_GOLEM_SHOOT", volume: 0.5, pitch: 0.8, target: "self" } ]
  - id: "archmage"
    display_name: "Archmage"
    unlock_level: 100
    trigger: "player_interact"
    display:
      lore:
        - "&7Sneak+Right-Click for an enhanced missile (&a{damage}&7) + arcane armor (&a{amount}&7)."
        - "&7Costs 1 Amethyst Shard."
        - "&8Requires: Sneaking."
    requirements:
      cooldown: 8.0
      state: [ "is_sneaking" ]
      items:
        - { action: "cost", tag: "minecraft:redstone", amount: 1 }
        - { action: "cost", tag: "minecraft:amethyst_shard", amount: 1 }
      exhaustion: { amount: 2.0, minimum: 3.0 }
    on_failure:
      cooldown: { action_bar: "&eArchmage cooling down: {time}s" }
      missing_item: { action_bar: "&cRequires Redstone + Amethyst!" }
    mechanics:
      - type: "core:projectile"
        parameters:
          speed: { constant: 3.0 }
          damage: { linear: { base: 8.0, step: 0.05, max: 12.0 } }
      - type: "core:armor_bonus"
        parameters:
          amount: { linear: { base: 3.0, step: 0.1, max: 6.0 } }
          duration: { constant: 10.0 }
    feedback: { notify: { action_bar: false } }
```

---

### piety.yml — Piety

**Issues**
1. [BUG][G][E-VI] `holy_light` L1 `core:field_aura` effect:10 (regeneration) → fires on **every** event (no guard), applies Regeneration to **all nearby LivingEntity including hostile mobs**. Piety is **healing enemies**.
2. [BUG][G][E-VI] `consecrate` L50 `core:field_aura` effect:5 (strength) → same: buffs nearby hostile mobs with Strength. Piety empowers enemies.
3. [BUG][G] `sanctuary` L25 `core:armor_bonus` — no event guard; stacks armor on every event.
4. [VIOLATION][B][E-III] All auras are passive "free miracles" with no active input, no item cost, no faith catalyst. Pillar III.
5. [BUG][C] `entity_damage_taken` 3.0 XP for any damage taken — broad but acceptable for a "faith under fire" identity.
6. [SHORTCOMING][F] `divine_intervention` L100 constants (regen, resistance, all radius/duration/amplifier flat).
7. [DESIGN][B] L25 `sanctuary` passive armor — §3 wants L25 active (totem/instrument performance hook, or sneak+right-click prayer).

**Redesign**

```yaml
id: "piety"
max_level: 100
display: { name: "Piety", icon: "minecraft:golden_apple", color: "PURPLE", style: "SEGMENTED_10" }
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }
xp_sources:
  - trigger: "breed_animals"
    reward: { constant: 8.0 }
  - trigger: "resurrect"            # P2-7 totem activation
    reward: { constant: 30.0 }
  # entity_damage_taken XP removed — too broad; replaced by totem synergy
abilities:
  - id: "holy_light"
    display_name: "Holy Light"
    unlock_level: 1
    trigger: "resurrect"
    display: { lore: [ "&7Totem activation grants allies nearby Regeneration for {duration}s." ] }
    mechanics:
      - type: "core:ally_aura"          # P2-5 (players only — was field_aura buffing mobs)
        parameters:
          effect: { constant: "minecraft:regeneration" }   # P2-11
          radius: { linear: { base: 4.0, step: 0.05, max: 8.0 } }
          duration: { linear: { base: 5.0, step: 0.05, max: 10.0 } }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: false } }
  - id: "bless"
    display_name: "Bless"
    unlock_level: 15
    trigger: "entity_damage"
    display: { lore: [ "&7Melee hits weaken targets &a{duration}s&7." ] }
    mechanics:
      - type: "core:apply_status"
        parameters:
          effect: { constant: "minecraft:weakness" }     # P2-11
          duration: { linear: { base: 2.0, step: 0.02, max: 4.0 } }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: false } }
  - id: "prayer"
    display_name: "Prayer"
    unlock_level: 25
    trigger: "player_interact"
    display:
      lore:
        - "&7Sneak+Right-Click (with a Totem in inventory) to gain &a{amount}&7 armor &a{duration}s&7."
        - "&8Requires: Sneaking, totem of undying in inventory."
    requirements:
      cooldown: 20.0
      state: [ "is_sneaking" ]
      items: [ { action: "possession", tag: "minecraft:totem_of_undying", slot: "OFF_HAND" } ]
      exhaustion: { amount: 2.0, minimum: 3.0 }
    on_failure:
      cooldown: { action_bar: "&ePrayer cooling down: {time}s" }
      missing_item: { action_bar: "&cRequires a Totem of Undying!" }
    mechanics:
      - type: "core:armor_bonus"
        parameters:
          amount: { linear: { base: 2.0, step: 0.03, max: 4.0 } }
          duration: { linear: { base: 15.0, step: 0.2, max: 30.0 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&6✦ Prayer! ✦" }
      sounds: [ { type: "BLOCK_BEACON_ACTIVATE", volume: 0.5, pitch: 1.2, target: "self" } ]
  - id: "consecrate"
    display_name: "Consecrate"
    unlock_level: 50
    trigger: "resurrect"
    display: { lore: [ "&7Totem activation grants allies Strength &a{duration}s&7." ] }
    mechanics:
      - type: "core:ally_aura"
        parameters:
          effect: { constant: "minecraft:strength" }     # P2-11
          radius: { linear: { base: 4.0, step: 0.05, max: 8.0 } }
          duration: { linear: { base: 5.0, step: 0.05, max: 10.0 } }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: false } }
  - id: "holy_protection"
    display_name: "Holy Protection"
    unlock_level: 75
    trigger: "entity_damage_taken"
    display: { lore: [ "&a{chance}%&7 chance to negate incoming damage." ] }
    mechanics:
      - type: "core:cancel_damage"
        parameters:
          chance: { linear: { base: 5.0, step: 0.2, max: 25.0 } }
    feedback: { notify: { action_bar: false } }
  - id: "divine_intervention"
    display_name: "Divine Intervention"
    unlock_level: 100
    trigger: "resurrect"
    display: { lore: [ "&7Totem activation: allies gain Regeneration II & Resistance for &a{duration}s&7 in &a{radius}&7." ] }
    mechanics:
      - type: "core:ally_aura"
        parameters:
          effect: { constant: "minecraft:regeneration" }  # P2-11
          radius: { linear: { base: 10.0, step: 0.1, max: 14.0 } }
          duration: { linear: { base: 10.0, step: 0.1, max: 15.0 } }
          amplifier: { constant: 1.0 }
      - type: "core:ally_aura"
        parameters:
          effect: { constant: "minecraft:resistance" }   # P2-11
          radius: { linear: { base: 10.0, step: 0.1, max: 14.0 } }
          duration: { linear: { base: 10.0, step: 0.1, max: 15.0 } }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: false } }
```

---

### Summary of skills audited: 32 / 32

| # | Skill | Critical issues | New pieces used |
|---|---|---|---|
| 1 | building | haste≠placement, stacking speed/armor, area_harvest≠place | P2-1 |
| 2 | masonry | haste≠placement, stacking, all-craft constants | P2-1 |
| 3 | carpentry | unbounded craft XP, stacking armor | P2-1 |
| 4 | woodcutting | haste re-applies, stacking armor | P2-1 |
| 5 | mining | stacking armor, constant capstone | P2-1 |
| 6 | excavation | flat milestones, double-area_harvest, stacking haste | P2-1 |
| 7 | farming | unbounded crop_grow XP, binary auto_replant | P2-1 |
| 8 | herbalism | unbounded crop_grow XP, passive L25, buff-mob auras | P2-1 |
| 9 | husbandry | stacking armor/kb/speed with no proximity check | P2-1 |
| 10 | cooking | stacking speed, all-furnace XP, constant capstone | P2-1 |
| 11 | fishing | stacking KB, passive duplicate L25 | P2-1 |
| 12 | alchemy | stacking speed, constant capstone | P2-1 |
| 13 | enchanting | speed_bonus≠orb attract, stacking armor | P2-1 |
| 14 | smithing | stacking armor, all-craft XP | P2-1 |
| 15 | tailoring | yield_multi≠craft, stacking armor | P2-1, P2-6 |
| 16 | light_armor | no armor-type filter, stacking speed | P2-1, P2-6 |
| 17 | medium_armor | no armor-type filter, stacking all stats | P2-1, P2-6 |
| 18 | heavy_armor | no armor-type filter, stacking all stats | P2-1, P2-6 |
| 19 | unarmored | stacking speed/armor via armor:empty on every event | P2-1 |
| 20 | shields | passive reflect labeled "bash", stacking kb/armor | P2-1, P2-2, P2-3, P2-9 |
| 21 | light_weapons | **target filter never matches melee → fully broken** | P2-1 |
| 22 | one_handed | lifesteal≠atk spd, stacking speed | P2-1, P2-10 |
| 23 | heavy_weapons | **target filter never matches melee → fully broken** | P2-1 |
| 24 | dual_wield | no real two-weapon loop, stacking speed | P2-1, P2-4 |
| 25 | throwing | stacking speed ungated | P2-1 |
| 26 | archery | haste≠bow draw, no piercing, stacking speed | P2-1 |
| 27 | unarmed | stacking speed ungated, dodge ignores hand state | P2-1 |
| 28 | acrobatics | sprint/sneak XP spam, stacking speed/jump | P2-1, P2-8 |
| 29 | riding | stacking speed/armor while riding | P2-1 |
| 30 | bard | **player_interact XP spam, free-magic auras buff mobs**, haste≠speed | P2-1, P2-5, P2-9 |
| 31 | wizardry | **free-magic projectiles/teleports on every interact**, stacking armor | P2-1 |
| 32 | piety | **free-magic auras buff mobs**, stacking armor | P2-1, P2-5, P2-7 |

