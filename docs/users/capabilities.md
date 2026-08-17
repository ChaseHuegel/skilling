# Capabilities Catalog

## Fixed Gameplay Tables

A few built-in mechanics carry small fixed data tables (design decision
ISSUE-301). They are immutable in Java and are not configurable in YAML:

| Mechanic | Fixed table | What it does |
|---|---|---|
| `core:auto_smelt` | `SMELT_MAP` | Raw-to-smelted product pairs mirroring vanilla furnace recipes |
| `core:offhand_strike` | `BASE_DAMAGE` | Base attack damage per vanilla weapon material |
| `core:auto_replant` | crop list | Which crops the mechanic replants |
| `core:apply_status` / `core:modify_attribute` | legacy numeric IDs | Deprecated numeric-ID fallback mapping |
| engine filter matching | `projectileToMaterial` | Maps a projectile type to a material for the `target` filter |

These are vanilla mirrors, capability boundaries, compatibility shims, or engine
plumbing, not skills or abilities, so they stay out of YAML. All author-facing
tuning (rewards, chances, durations, limits) remains configurable as evaluator
parameters in your skill files.

## Ability Cost & Cooldown Consumption

An ability's item costs are deducted and its cooldown is applied **exactly once
per activation attempt**. This happens after the ability's requirements pass and at least
one mechanic performs an activation. This rule applies uniformly to every
mechanic:

- **A mechanic that can act on the triggering event counts as an activation,
  even when its chance roll fails.** Chance-based mechanics (`core:dodge`,
  `core:block_damage`, `core:cancel_damage`, `core:auto_smelt`,
  `core:fishing_yield`, `core:durability_save`, `core:projectile_return`,
  `core:yield_multiplier`) consume the ability's cost/cooldown whether the roll
  succeeds or fails, so an ability cannot be spammed until the roll succeeds.
- **A mechanic that cannot act at all is a no-op** (wrong event type, missing
  target, inapplicable state) and does not spend the cost or cooldown.
- When an ability lists several mechanics, consumption happens once if **any**
  mechanic activates.
- **Cooldowns persist across a quit/relog.** Logging out does not reset an
  active cooldown. It expires on its own timer. Only time lifts a cooldown.

## Built-In Mechanics

### core:yield_multiplier

Multiplies block drops by a percentage chance on each break. An optional
`triple_chance` rolls first: when it succeeds the drops are tripled instead of
doubled, so a single mechanic expresses "always double, sometimes triple"
capstones without stacking two mechanics into a quadruple yield.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `yield_chance` | double | `0` | Probability (0-100%) of doubling the drops |
| `triple_chance` | double | `0` | Probability (0-100%) of tripling instead of doubling |

**Event:** `BlockBreakEvent`

### core:chain_break

Breaks connected blocks of the same type up to a limit (vein mining), expanding
in all six directions (including up/down). Each chained block consumes 1 tool
durability (respecting Unbreaking, with the tool breaking at max durability).
Chained blocks are broken without re-triggering XP or ability processing (only
the originating break awards XP).

The optional `target` parameter restricts the chain to a material or tag instead
of the origin block's own material, so one ability can fell a tree by chaining
the tagged `logs` blocks and every connected `leaves` block.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chain_limit` | double | `0` | Maximum connected blocks to break, not counting the origin (clamped to 128 so an oversized value cannot freeze the server) |
| `target` | string | (none) | Optional material or tag reference (`#minecraft:logs`, `#c:logs`, or `minecraft:oak_log`). When present, only blocks matching the resolved set chain |

**Event:** `BlockBreakEvent`

### core:level_break

Like `core:chain_break`, but expands only on the XZ plane (four horizontal
directions) and never along the Y axis. This suits vein/strip mining that must not
propagate up or down into adjacent layers. Same parameters and behavior as
`core:chain_break` (per-block tool durability, `chain_limit` cap, no double XP
for chained blocks).

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chain_limit` | double | `0` | Maximum connected blocks to break, not counting the origin (clamped to 128) |
| `target` | string | (none) | Optional material or tag reference (`#minecraft:logs`, `#c:logs`, or `minecraft:oak_log`). When present, only blocks matching the resolved set chain |

**Event:** `BlockBreakEvent`

### core:block_damage

Chance to fully negate incoming damage (shield/armor "block" flavor).

> **Note:** `core:block_damage`, `core:cancel_damage`, and `core:dodge` are aliases
> of the same implementation. All three roll a `chance` (0-100%) to cancel an
> incoming damage event. They are registered as separate keys purely for flavor:
> `block_damage` reads as a shield/armor block, `cancel_damage` as a dodge/evade,
> and `dodge` as an evasion.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) to fully block damage |

**Event:** `EntityDamageEvent`

### core:modify_damage

Multiplies outgoing entity damage.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Damage multiplier (1.5 = +50%) |

**Event:** `EntityDamageByEntityEvent`

### core:apply_status

Applies a potion effect to the damaged entity on hit.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `effect` | string | none | Namespaced potion effect key (e.g., `minecraft:slowness`). See [Effect & Attribute Parameter Keys](#effect--attribute-parameter-keys) below |
| `duration` | double | `3` | Duration in seconds |
| `amplifier` | double | `0` | Effect amplifier (0 = level I) |

**Event:** `EntityDamageByEntityEvent`

### core:cancel_damage

Chance to completely cancel incoming damage (dodge/evade flavor).

> **Note:** `core:cancel_damage`, `core:block_damage`, and `core:dodge` are aliases
> of the same implementation. All three roll a `chance` (0-100%) to cancel an
> incoming damage event. They are registered as separate keys purely for flavor:
> `cancel_damage` reads as a dodge/evade, `block_damage` as a shield/armor block,
> and `dodge` as an evasion.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) to cancel damage |

**Event:** `EntityDamageEvent`

### core:modify_attribute

Temporarily modifies a player attribute.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `attribute` | string | none | Namespaced attribute key (e.g., `minecraft:movement_speed`). Legacy numeric IDs (1-10) remain supported but are deprecated. See [Effect & Attribute Parameter Keys](#effect--attribute-parameter-keys) below |
| `amount` | double | `0` | Modifier value |
| `duration` | double | `5` | Duration in seconds |
| `uuid` | string | random | Stable modifier UUID. Repeated activations with the same UUID replace the previous modifier instead of stacking |

### core:armor_bonus

Applies a temporary armor bonus attribute modifier.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `amount` | double | `0` | Additional armor points |
| `duration` | double | `300` | Duration in seconds |
| `uuid` | string | random | Stable modifier UUID. Repeated activations with the same UUID replace the previous modifier instead of stacking |

### core:knockback_resist

Applies a temporary knockback resistance attribute modifier.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `amount` | double | `0` | Knockback resistance (0-1) |
| `duration` | double | `300` | Duration in seconds |
| `uuid` | string | random | Stable modifier UUID. Repeated activations with the same UUID replace the previous modifier instead of stacking |

### core:speed_bonus

Applies a temporary movement speed attribute modifier.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Multiplicative speed multiplier (1.5 = 50% faster, not a percentage) |
| `duration` | double | `300` | Duration in seconds |
| `uuid` | string | random | Stable modifier UUID. Repeated activations with the same UUID replace the previous modifier instead of stacking |

**Event:** Fires on the trigger declared by the ability (e.g., `entity_damage_taken`, `consume_item`). Gives the player a temporary movement speed boost for the configured `duration`.

### core:modify_craft_output

Multiplies the output of crafting recipes. The result slot is capped at one
stack. Any bonus overflow beyond it is granted as an extra stack in the player's
inventory (or dropped on the ground if the inventory is full), so no bonus items
are lost on shift-click batches.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Output multiplier |

**Event:** `CraftItemEvent`

### core:modify_furnace_output

Multiplies furnace extraction output.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Output multiplier |

**Event:** `FurnaceExtractEvent`

### core:saturation_inject

Injects bonus saturation when consuming food.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `saturation` | double | `0` | Bonus saturation to add |

**Event:** `PlayerItemConsumeEvent`

### core:modify_brew_time

Modifies the brewing time of potions in a brewing stand.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | double | `1.0` | Brewing time multiplier (<1.0 speeds up, >1.0 slows down) |

**Event:** `BrewingStartEvent`

### core:modify_potion_duration

Modifies the duration of brewed potion effects, including the base potion
type's effects (the vanilla path a normal brewed potion such as Swiftness
stores its effect under). Same-type effects merge with the longest duration
winning at consumption, so the potion's identity is preserved.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | double | `1.0` | Duration multiplier for potion effects |

**Event:** `BrewEvent`

### core:aoe_effect

Applies a potion effect to all living entities within a radius of the player, excluding the player themselves. By default (`targets: allies`) hostile mobs are never affected.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `effect` | string | none | Namespaced potion effect key (e.g., `minecraft:poison`). See [Effect & Attribute Parameter Keys](#effect--attribute-parameter-keys) below |
| `radius` | double | `5` | Effect radius in blocks |
| `duration` | double | `5` | Duration in seconds |
| `amplifier` | double | `0` | Effect amplifier |
| `targets` | string | `allies` | Who receives the effect: `allies` (default, excludes hostile mobs), `hostiles` (only monsters and angered neutrals), or `all` |

**Event:** Fires on the trigger declared by the ability. Applies the effect to all living entities within `radius` (excluding the player).

### core:projectile

Launches a custom projectile from the player. Fires only on a right-click
(right-click air or right-click block). A left-click is a no-op.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `speed` | double | `1.5` | Projectile velocity multiplier |
| `damage` | double | `4` | Damage dealt on hit |

**Event:** `PlayerInteractEvent` (right-click only)

### core:teleport

Short-range teleport in the player's looking direction. Fires only on a
right-click (right-click air or right-click block). A left-click is a no-op.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `range` | double | `10` | Teleport distance in blocks |

**Event:** `PlayerInteractEvent` (right-click only)

### core:thorns_damage

Reflects a flat amount of incoming damage back to the attacker.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `damage` | double | `0` | Flat damage reflected to the attacker |

**Event:** `EntityDamageByEntityEvent`

### core:knockback

Applies a directional velocity impulse (knockback) to the damaged entity, or to all living entities in a radius around the player. Targets are gated by the `targets` filter, and other players are never knocked.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `force` | double | `0` | Horizontal impulse strength (velocity magnitude), clamped to [0, 3] |
| `radius` | double | `0` | Radius in blocks, clamped to [0, 32]. `0` = single target only (the damaged entity) |
| `vertical` | double | `0.3` | Upward component added to the impulse, clamped to [0, 1.5] |
| `targets` | string | `hostiles` | Which living entities receive the knockback: `hostiles` (default, monsters and angered neutrals), `allies`, or `all`. Players are always excluded |

**Event:** `EntityDamageByEntityEvent` (single target) / `PlayerInteractEvent` (radial shove)

### core:shield_disable

Triggers the vanilla shield raise-lockout cooldown on a target player, rendering them unable to block with a shield for the duration. The target is explicit via the `target` parameter and independent of the trigger binding.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `ticks` | double | `0` | Shield disable duration in ticks |
| `target` | string | `victim` | Who gets disabled: `victim` (the damaged player on `EntityDamageByEntityEvent`. Falls back to the activating player elsewhere), `attacker` (the player attacker, projectile shooters count. Falls back to the activating player elsewhere), or `self` (always the activating player). A non-player victim/attacker is a safe no-op |

**Event:** `EntityDamageByEntityEvent` / `PlayerInteractEvent`

### core:offhand_strike

Deals a melee hit using the base attack damage of the off-hand weapon to the entity the player is looking at, consuming 1 off-hand durability. Targets are gated by the `targets` filter, and other players are never struck.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Scales the off-hand weapon's base damage, clamped to [0, 4] |
| `reach` | double | `4` | Maximum targeting distance in blocks, clamped to [0, 4.5] |
| `targets` | string | `hostiles` | Which living entities may be struck: `hostiles` (default, monsters and angered neutrals), `allies`, or `all`. Players are always excluded |

**Event:** `PlayerInteractEvent`

### core:offhand_swing

Plays the off-hand swing animation for the activating player. It deals no damage
and changes no state. Use it for animation feedback only, typically alongside
another mechanic such as `core:offhand_strike`.

**Parameters:** None

**Event:** any

### core:dodge

Chance to completely dodge incoming damage.

> **Note:** `core:dodge`, `core:block_damage`, and `core:cancel_damage` are aliases
> of the same implementation. All three roll a `chance` (0-100%) to cancel an
> incoming damage event. They are registered as separate keys purely for flavor.
> `dodge` reads as an evasion.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Dodge probability (0-100%) |

**Event:** `EntityDamageEvent`

### core:lifesteal

Heals the player for a percentage of damage dealt.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `percentage` | double | `0.1` | Heal percentage of damage dealt |

**Event:** `EntityDamageByEntityEvent`

### core:crowd_control

Applies an AoE status effect to nearby enemies when damaging a target. This is
the offensive counterpart to the buff auras. It defaults to `targets: hostiles`
so a debuff lands only on monsters and angered neutrals. It never lands on your own
allies.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `effect` | string | none | Namespaced potion effect key (e.g., `minecraft:poison`). See [Effect & Attribute Parameter Keys](#effect--attribute-parameter-keys) below |
| `duration` | double | `3` | Duration in seconds |
| `amplifier` | double | `0` | Effect amplifier |
| `radius` | double | `5` | Effect radius in blocks (clamped to [0, 32]) |
| `targets` | string | `hostiles` | Who receives the effect: `hostiles` (default, monsters and angered neutrals), `allies`, or `all` |

**Event:** `EntityDamageByEntityEvent`

### core:execute

Instantly kills targets below a health threshold.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `threshold` | double | `0` | Health percentage threshold (0-100) |

**Event:** `EntityDamageByEntityEvent`

### core:damage

Deals damage to the target through the normal damage pipeline, so armor,
protection enchantments, potion effects, and absorption reduce it.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `damage` | double | `0` | Flat damage in engine points (half-hearts, matching the rest of the engine; an iron sword deals 6.0) |
| `percent` | double | `0` | Bonus damage as a fraction of the target's max health (e.g. `0.1` = 10% of max health). Added to `damage` |

**Event:** `EntityDamageByEntityEvent` (the victim, when the player is the attacker) / `PlayerInteractEntityEvent` (the right-clicked entity) / `PlayerInteractEvent` right-click (the entity the player is looking at)

### core:true_damage

Deals damage to the target that ignores damage mitigations: armor, protection
enchantments, potion effects, absorption, and damage-cancel mechanics are all
bypassed. The target's hard invulnerability flag (e.g. creative mode) is still
honored.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `damage` | double | `0` | Flat damage in engine points (half-hearts, matching the rest of the engine; an iron sword deals 6.0) |
| `percent` | double | `0` | Bonus damage as a fraction of the target's max health (e.g. `0.1` = 10% of max health). Added to `damage` |

**Event:** `EntityDamageByEntityEvent` (the victim, when the player is the attacker) / `PlayerInteractEntityEvent` (the right-clicked entity) / `PlayerInteractEvent` right-click (the entity the player is looking at)

### core:auto_smelt

Automatically smelts mined blocks (e.g. iron ore -> iron ingot). Each distinct
drop type with a smelt mapping is converted independently. One smelted stack
per product, with unmapped drop types re-dropped unchanged, so multi-type blocks
never merge or lose drops. Nugget/quartz drops (already the smelted product)
pass through unchanged.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) of smelting |

**Event:** `BlockBreakEvent`

### core:xp_bonus

Applies a multiplicative XP bonus to all XP gains for a fixed duration. The bonus
expires after `duration` seconds, refreshes (does not stack) on re-activation, and
is cleared when the player quits or the plugin reloads.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Multiplicative XP multiplier applied to all gains (1.5 = +50%, 2.0 = double. Not a percentage increase) |
| `duration` | double | `30.0` | Seconds the bonus lasts before expiring |

**Event:** Fires on the trigger declared by the ability.

### core:fishing_yield

Grants bonus catch items when fishing.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `yield_chance` | double | `0` | Probability (0-100%) of bonus catch |

**Event:** `PlayerFishEvent`

### core:fishing_loot

Multiplies the quantity of caught fishing loot. A fractional result (e.g. 1
fish &times; 1.5) rounds up probabilistically, so a small stack under a
fractional multiplier yields its expected value instead of flooring to nothing.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Catch multiplier (2.0 = double) |

**Event:** `PlayerFishEvent`

### core:area_harvest

Breaks all matching blocks in a radius around the targeted block. The radius is
clamped to 32 and the scan stops at `max_blocks` (itself clamped to 128). Each
harvested block fires a synthetic `BlockBreakEvent`, so region/protection
plugins can cancel it, and consumes 1 tool durability (respecting Unbreaking,
with the tool breaking at max durability).

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `radius` | double | `1` | Radius in blocks to harvest (clamped to 32) |
| `max_blocks` | double | `64` | Maximum number of blocks to break (clamped to 128) |

**Event:** `BlockBreakEvent`

### core:auto_replant

Automatically replants crops after harvesting.

**Parameters:** None

**Event:** `BlockBreakEvent`

### core:durability_save

Chance to negate durability loss on the held item when it takes damage.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) to save durability |

**Event:** `PlayerItemDamageEvent`

### core:haste_effect

Applies the HASTE potion effect to the player, increasing mining/digging speed.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `amplifier` | double | `0` | Effect amplifier (0 = level I) |
| `duration` | double | `300` | Duration in seconds |

**Event:** Fires on the trigger declared by the ability. Applies the HASTE potion effect to the player, increasing mining/digging speed. It does not affect movement or placement speed.

### core:repair_discount

Reduces the experience level cost of anvil repairs.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `discount` | double | `0` | Percentage discount (0-100) |

**Event:** `PrepareAnvilEvent`

### core:modify_tame_chance

Scales the chance of successfully taming an animal. The tame event fires only
after a successful vanilla roll, so a multiplier above `1.0` preserves the
success and a multiplier below `1.0` cancels an otherwise-successful tame with
probability `1 - multiplier` (e.g. `0.5` → half of tames are undone).

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Taming chance multiplier (1.0 = vanilla) |

**Event:** `EntityTameEvent`

### core:instant_tame

Tames an untamed tameable mob on right-click. The vanilla tame event fires only
after a successful roll, so a "tame more easily" bonus cannot be built on it;
this mechanic hooks the earlier right-click and assigns ownership with the
configured `chance`. Acting on an untamed mob cancels the interaction, so the
ability's `requirements.items` food cost is the only item consumed (the vanilla
feed/tame attempt never adds a second charge). The ability must provide that
food cost (e.g. via the `#c:tame_offerings` tag) to preserve the item economy.
Players are never targets, and another player's pet is already tamed.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `100` | Probability (0-100%) of taming |

**Event:** `right_click_entity` (`PlayerInteractEntityEvent`)

### core:projectile_return

Chance to recover thrown projectiles (tridents, snowballs, eggs) after they hit.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Recovery probability (0-100%) |

**Event:** `ProjectileHitEvent`

### core:modify_enchant_cost

Reduces the experience level cost of enchanting at an enchanting table.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `discount` | double | `0` | Percentage discount (0-100) |

**Event:** `EnchantItemEvent`

### core:field_aura

Applies a potion effect to the player and nearby living entities within a radius. By default (`targets: allies`) hostile mobs are never affected.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `effect` | string | none | Namespaced potion effect key (e.g., `minecraft:regeneration`). See [Effect & Attribute Parameter Keys](#effect--attribute-parameter-keys) below |
| `radius` | double | `8` | Aura radius in blocks |
| `duration` | double | `5` | Duration in seconds |
| `amplifier` | double | `0` | Effect amplifier |
| `targets` | string | `allies` | Who receives the effect: `allies` (default, excludes hostile mobs), `hostiles` (only monsters and angered neutrals), or `all` |

**Event:** Fires on the trigger declared by the ability.

> **Caution:** set `targets: hostiles` or `targets: all` only if you deliberately
> want a regen/strength aura to reach hostile mobs. For player-only buffs, use
> `core:ally_aura` instead.

### core:ally_aura

Applies a potion effect to the casting player and all nearby **players** (allies) within a radius. Hostile mobs are never affected. A `radius: 0` config buffs nobody. The caster is only buffed as part of the aura, not unconditionally.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `effect` | string | none | Namespaced potion effect key (e.g., `minecraft:regeneration`). See [Effect & Attribute Parameter Keys](#effect--attribute-parameter-keys) below |
| `radius` | double | `8` | Aura radius in blocks (max 32) |
| `duration` | double | `5` | Duration in seconds |
| `amplifier` | double | `0` | Effect amplifier |

**Event:** Fires on the trigger declared by the ability (typically `player_interact`). Applies to nearby players only, not living entities.

### core:set_cooldown

Triggers the vanilla item-stack cooldown animation on the activating player for the given material.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `material` | string | none | Namespaced material key (e.g., `minecraft:shield`, `minecraft:goat_horn`) |
| `ticks` | double | `0` | Cooldown duration in ticks |

**Event:** Any. Applies to the activating player

### core:modify_jump

Temporarily increases the player's jump strength.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Jump multiplier (1.5 = 50% higher) |
| `duration` | double | `300` | Duration in seconds |
| `uuid` | string | random | Stable modifier UUID. Repeated activations with the same UUID replace the previous modifier instead of stacking |

### core:block_particles

Spawns a configured particle burst at the event's clicked/broken/placed block. Use it as the executable action for block-interaction abilities (e.g. burying items) so the requirement `consume` step runs and item costs are deducted.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `particle` | string | none | Namespaced particle identifier (e.g. `minecraft:happy_villager`, `minecraft:portal`) |
| `count` | double | `1` | Number of particles to spawn |
| `speed` | double | `0` | Particle speed/extra |

**Event:** `player_interact` (right-click on block), `block_break`, `block_place`. Requires a block location. Returns `false` (no-op) otherwise.

### core:modify_attack_speed

Temporarily increases the player's attack speed for a configurable duration.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Attack speed multiplier (1.2 = +20% faster) |
| `duration` | double | `300` | Duration in seconds |
| `uuid` | string | random | Stable modifier UUID. Repeated activations with the same UUID replace the previous modifier instead of stacking |

**Event:** Fires on the trigger declared by the ability (temporary attack speed bonus).

### core:unlock_recipe

Permanently unlocks a recipe book recipe for the player. The recipe is
identified by its namespaced key and may come from vanilla, a data pack, or
another plugin. The unlock writes to the player's persistent recipe book state,
so it survives restarts and plugin removal.

This mechanic is a persistent one-time unlock. It is a no-op when the recipe is
already discovered or not currently registered on the server. Use it as a
milestone: bind an ability to the `level_up` trigger with the desired
`unlock_level` (see [creating-skills.md](creating-skills.md)). The engine also
re-runs unlock mechanics on player join and after `/skills reload`, so a player
who is already past the milestone — including one whose level was set by an
offline admin command — is caught up. In-session, the ability fires only on the
real unlock, so configured feedback shows once at the milestone, not on every
later level-up.

A milestone that unlocks several recipes lists one mechanic entry per recipe.
Unlocks are inherently binary and are the deliberate exception to the
"No Static Milestones" sub-scaling rule.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `recipe` | string | none (required) | Namespaced recipe key (e.g., `minecraft:netherite_pickaxe`). Malformed keys fail at load; a syntactically valid key whose recipe is not registered yet logs a warning and no-ops until it exists |

**YAML usage:**

```yaml
abilities:
  - id: master_blacksmith
    display_name: "Master Blacksmith"
    unlock_level: 50
    trigger: "level_up"
    mechanics:
      - type: "core:unlock_recipe"
        parameters:
          recipe: { constant: "minecraft:netherite_pickaxe" }
    feedback: { notify: { action_bar: false } }
```

**Event:** `level_up` (the milestone moment). Join and `/skills reload` also
reconcile unlock mechanics for players already past the milestone.

### core:persistent_attribute

Permanently adjusts a player attribute (for example `minecraft:max_health`)
by a level-scaled amount, replacing any prior modifier with the same stable
UUID instead of stacking. This is the mechanic behind skills that grow a
player's max hearts or other persistent attributes as the skill levels.

The modifier is transient: it is never written to the player's NBT, so
removing the plugin (or restarting without it) returns the player's
attributes to vanilla values. Because the mechanic implements
`UnlockMechanic`, the engine re-runs it on player join, after
`/skills reload`, and after an online `/skills setlevel` or `/skills reset`,
evaluating `amount` at the player's current level. Re-running is idempotent
per amount, and a level that drops below the ability's `unlock_level`
strips the bonus, so a de-level or reset cannot leave stale extra hearts.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `attribute` | string | none (required) | Namespaced attribute key (e.g., `minecraft:max_health`, `minecraft:max_absorption`, `minecraft:armor_toughness`) |
| `amount` | double | `0` | Additive modifier amount (evaluated at the current level). Non-positive removes the modifier |
| `uuid` | string | none (required) | Stable modifier UUID constant. Repeated executions with the same UUID refresh, never stack |

**YAML usage:**

```yaml
abilities:
  - id: wildborn
    display_name: "Wildborn"
    unlock_level: 1
    trigger: "level_up"
    mechanics:
      - type: "core:persistent_attribute"
        parameters:
          attribute: { constant: "minecraft:max_health" }
          amount: { linear: { base: 0.0, step: 0.25, max: 25.0 } }
          uuid: { constant: "7f1c6e2a-9b4d-4a6f-b8e2-3d5a0c1f9e77" }
    feedback: { notify: { action_bar: false } }
```

**Event:** `level_up` (the level where the ability unlocks). Join, reload,
online `setlevel`, and `reset` also reconcile persistent attribute mechanics
for players already past the milestone.

## Effect & Attribute Parameter Keys

Mechanics that accept an `effect` parameter (`core:apply_status`, `core:aoe_effect`,
`core:crowd_control`, `core:field_aura`, `core:ally_aura`) or an `attribute` parameter
(`core:modify_attribute`) now accept **namespaced keys**:

```yaml
effect: { constant: "minecraft:poison" }
attribute: { constant: "minecraft:movement_speed" }
```

- **Namespaced keys** (e.g., `"minecraft:poison"`, `"minecraft:movement_speed"`) are
  resolved against the live Paper registry and are the recommended form.
- **Legacy numeric IDs** (e.g., `19` for Poison, `4` for Movement Speed) remain
  supported for backward compatibility but are **deprecated** and log a warning on use.
- **Unknown keys or IDs throw `IllegalArgumentException` at skill load time**
  (fail-fast), so a typo is rejected while the skill file is parsed. This never happens
  inside an event handler mid-game. The same applies to the `material` parameter of
  `core:set_cooldown` and the `particle` parameter of `core:block_particles`.

> **Parameter bounds are also validated at load:** negative radii
> (`core:aoe_effect`, `core:field_aura`, `core:crowd_control`, `core:knockback`,
> `core:ally_aura`), out-of-range chances (`chance`/`yield_chance` outside 0-100,
> or a non-positive `modify_tame_chance` multiplier), and negative durations or
> cooldown ticks are rejected when the skill file is parsed. Level-scaled
> evaluator parameters cannot be checked without a level context and are only
> validated at runtime.

## Built-In Triggers

> **Bulk-operation scaling:** XP rewards for the `collect_xp`, `craft_item`, and
> `furnace_extract` triggers are multiplied by the magnitude of the operation.
> The magnitude is the number of XP orbs collected, the number of items crafted (including
> shift-click batch totals), or the number of items extracted from a furnace,
> respectively. For example, a configured reward of 2 skill XP grants 6 skill XP
> for a bulk of 3. `consume_item` is not bulk-scaled. Eating grants the flat
> configured reward once per item consumed. All other triggers grant the flat
> configured reward.

> **Damage scaling:** any XP source on a damage trigger (`fall_damage`,
> `entity_damage_taken`, `entity_damage`) may declare `scaling: damage`, which
> multiplies the configured reward by the event's raw base damage
> (`getDamage()`, pre-mitigation, in half-hearts). It is rejected at load on
> non-damage triggers. See [creating-skills.md](creating-skills.md) for details.

| Key | Event | Description |
|---|---|---|
| `block_break` | `BlockBreakEvent` | Breaking a block |
| `block_place` | `BlockPlaceEvent` | Placing a block |
| `entity_damage` | `EntityDamageByEntityEvent` | Damaging an entity |
| `entity_damage_taken` | `EntityDamageEvent` | Taking damage |
| `fall_damage` | `EntityDamageEvent` (cause `FALL`) | Taking damage from a fall |
| `entity_kill` | `EntityDeathEvent` | Killing an entity |
| `craft_item` | `CraftItemEvent` | Crafting an item |
| `furnace_extract` | `FurnaceExtractEvent` | Extracting from a furnace |
| `brew_potion` | `BrewEvent` | A brewing stand finishes brewing a batch |
| `brew_start` | `BrewingStartEvent` | A brewing stand begins a new brewing cycle |
| `player_interact` | `PlayerInteractEvent` | Interacting (right/left click) with the main hand only. The off-hand duplicate of a two-handed interaction is skipped so abilities fire once. A `target` filter matches the clicked block on right-click. Left-clicks and air interactions never match a block target. Superseded by the six action-specific click triggers for precise routing |
| `right_click_air` | `PlayerInteractEvent` (action `RIGHT_CLICK_AIR`) | Right-clicking air with the main hand only. Never matches a block `target` filter |
| `right_click_block` | `PlayerInteractEvent` (action `RIGHT_CLICK_BLOCK`) | Right-clicking a block with the main hand only. A `target` filter matches the clicked block |
| `right_click_entity` | `PlayerInteractEntityEvent` | Right-clicking an entity with the main hand only. The off-hand duplicate of a two-handed interaction is skipped |
| `left_click_air` | `PlayerInteractEvent` (action `LEFT_CLICK_AIR`) | Left-clicking air with the main hand only. Never matches a block `target` filter |
| `left_click_block` | `PlayerInteractEvent` (action `LEFT_CLICK_BLOCK`) | Left-clicking a block with the main hand only. A `target` filter matches the clicked block |
| `left_click_entity` | `EntityDamageByEntityEvent` | Attacking an entity directly with a left-click (hand/punch only). Projectile attacks are not left-clicks and stay on the `entity_damage` and `shoot_bow` triggers. Supports `scaling: damage` |
| `consume_item` | `PlayerItemConsumeEvent` | Eating/drinking |
| `fishing` | `PlayerFishEvent` | Successfully catching a fish. Only the `CAUGHT_FISH` state dispatches. Casts, bites, reels, and failed attempts do not |
| `crop_grow` | `BlockGrowEvent` | Natural crop growth |
| `breed_animals` | `EntityBreedEvent` | Breeding animals |
| `sprint` | `PlayerToggleSprintEvent` | Player starts sprinting (release is not a trigger) |
| `sneak` | `PlayerToggleSneakEvent` | Player starts sneaking (release is not a trigger) |
| `ride_horse` | `VehicleEnterEvent` | Player mounts a vehicle |
| `collect_xp` | `PlayerExpChangeEvent` | Collecting vanilla XP orbs |
| `level_up` | `SkillingLevelUpEvent` | A Skilling skill levels up |
| `enchant_item` | `EnchantItemEvent` | Enchanting an item at an enchanting table |
| `shoot_bow` | `EntityShootBowEvent` | Shooting a bow or crossbow |
| `item_damage` | `PlayerItemDamageEvent` | Item durability loss |
| `player_shear` | `PlayerShearEntityEvent` | Shearing a sheep or other shearable entity |
| `repair` | `PrepareAnvilEvent` | Opening an anvil or changing its inputs |
| `player_tame` | `EntityTameEvent` | Taming a wild animal |
| `launch_projectile` | `ProjectileLaunchEvent` | Launching a projectile (trident, snowball, etc.) |
| `projectile_hit` | `ProjectileHitEvent` | A projectile lands on a block or entity (use for impact-time mechanics like `core:projectile_return`) |
| `resurrect` | `EntityResurrectEvent` | Totem of Undying activation |
| `cure_villager` | `EntityTransformEvent` | A zombie villager finishes converting into a villager (reason `CURED`). Attribution follows the player who initiated the cure (`ZombieVillager.getConversionPlayer()`). A cure that completes after that player logs off grants nothing |
| `elytra_glide` | `EntityToggleGlideEvent` | Player starts gliding with an elytra |
| `chunk_load` | `ChunkLoadEvent` | Exploring freshly generated terrain. Fires only when a chunk is generated for the first time (`isNewChunk()`), routed to nearby players. Loading a chunk from disk does not fire it |
| `sleep` | `PlayerDeepSleepEvent` | Player sleeps long enough to pass the night or storm. Checking into and back out of a bed does not fire it |
| `compost` | `CompostItemEvent` | An item is composted into a composter. Routed to nearby players of the composter |
| `trade` | `PlayerTradeEvent` | Trading with a villager |
| `barter` | `PiglinBarterEvent` | A piglin barters with a player. Routed to nearby players of the piglin |
| `recipe_discover` | `PlayerRecipeDiscoverEvent` | Unlocking a new crafting recipe |
| `smith` | `SmithItemEvent` | Taking an item out of a smithing table |
| `mend` | `PlayerItemMendEvent` | An item repairs itself with the Mending enchantment |
| `map_fill` | `PlayerMapFilledEvent` | A player's map fills with terrain for the first time |
| `cartography` | `CartographyItemEvent` | Taking an item out of a cartography table |
| `vault_change` | `VaultChangeStateEvent` | A trial vault changes state. Fires only when the change has a player cause |
| `sniffer` | `EntityFertilizeEggEvent` | Breeding a sniffer. Fires only when a player does the breeding |
| `potion_splash` | `PotionSplashEvent` / `LingeringPotionSplashEvent` | Throwing a splash or lingering potion that breaks. Fires only when a player threw it |

## Built-In State Filters

State filters are evaluated per-ability and per-XP source in YAML. The filter syntax is `key:value` in the `state:` field.

| Key | Value(s) | Description |
|---|---|---|
| `is_sneaking` | *(none)* | Player is sneaking |
| `is_sprinting` | *(none)* | Player is sprinting |
| `is_in_water` | *(none)* | Player is in water |
| `is_on_ground` | *(none)* | Player is on the ground |
| `is_on_fire` | *(none)* | Player is on fire |
| `is_riding` | *(none)* | Player is riding a vehicle/mount |
| `is_blocking` | *(none)* | Player is blocking with a shield |
| `player_placed` | `true`, `false` | `false` matches blocks not placed by a player (natural blocks). `true` matches blocks a player placed. Values are validated at load |
| `dimension` | `overworld`, `nether`, `end` | Player's current dimension |
| `weather` | `clear`, `rain`, `thunder` | Current weather in player's world |
| `time` | `day`, `night` | Time of day in player's world |
| `light_level` | `below:N`, `above:N`, `exactly:N` | Block light level comparison |
| `health` | `below:N%`, `above:N%` | Player health percentage |
| `hunger` | `below:N`, `above:N` | Player food level |
| `biome` | `minecraft:biome_id` | Player's current biome. Values are validated at load |
| `target_type` | `minecraft:entity_id` or `<#entity_tag>` | Type of the target entity. Matches the damaged entity on `entity_damage`/`entity_damage_taken` and the killed entity on `entity_kill`. A `#...` value (e.g. `#c:undead`, `#minecraft:zombies`) resolves through the `entity_tags` store in `tags/base.yml`. Fails closed on events with no target entity |
| `offhand` | `empty`, `weapon` | Offhand item state |
| `hand` | `empty`, `main_empty`, `off_empty` | Hand emptiness check |
| `equipped_all` | `<material>` or `<#tag>` | Every armor slot holds an item matching the target (e.g., `#c:light_armor`) |
| `equipped_any` | `<material>` or `<#tag>` | At least one armor slot holds an item matching the target |
| `cause` | `burn`, `fire`, `lava`, `drowning`, `suffocation`, `cactus`, `starvation` | The `EntityDamageEvent` damage cause on the `entity_damage_taken` trigger. `burn` matches fire, fire ticks, and lava. Fails closed on any non-damage event or other cause. Values are validated at load |
| `honey_level` | `below:N`, `above:N`, `exactly:N` | The honey level of a beehive clicked on `player_interact`. Fails closed on non-beehive clicks. Values are validated at load |

The `#c:light_armor`, `#c:medium_armor`, `#c:heavy_armor`, and `#c:unarmored`
custom tags (in `tags/base.yml`) reproduce the historical armor tiers as data. No
tier knowledge is hard-coded in Java. `#c:unarmored` includes empty slots
(`minecraft:air`), the elytra, and headwear.

State keys and value-restricted filters (`player_placed`, `biome`) are validated
when the skill YAML loads: an unknown state key or an invalid value fails the
reload with a descriptive error instead of silently never matching on the event
path.

## Built-In Evaluators

| Key | Description |
|---|---|
| `constant` | Fixed value regardless of level |
| `linear` | Scales linearly with level above unlock |
| `milestones` | Tiered values at specific level thresholds |
| `polynomial` | Power curve for XP progression |
