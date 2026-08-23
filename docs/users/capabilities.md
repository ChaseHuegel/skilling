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
| `core:locate` | `STRUCTURE_TYPES` | Built-in structure type keys for the compass re-point |
| `cause` filter | `fly_into_wall` keyword | Maps the elytra collision damage cause |

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

### Exhaustion cost requirement

An ability's `requirements:` block may declare an `exhaustion` entry that
consumes hunger points on a successful activation and gates activation on a
minimum food level. Both `amount` (hunger points to consume) and `minimum` (the
inclusive food-level floor to activate) accept a plain scalar or a
level-scaled evaluator block, so a hunger cost can sub-scale or flatten to zero
between the unlock level and level 100. For example, a "free at capstone" AOE
holds its cost flat until the mastery level:

```yaml
requirements:
  exhaustion:
    amount: { milestones: { 25: 1, 100: 0 } }
    minimum: { milestones: { 25: 3, 100: 0 } }
```

A zero `amount` consumes nothing; a zero `minimum` lifts the hunger gate
entirely. This pairs with the level-scaled `durability` entry below.

### Durability cost requirement

An ability's requirements block may also declare a `durability` entry that
damages the item in a slot by a flat point cost, consumed on a successful
activation alongside exhaustion and cost items. The amount is a level-scaled
evaluator; a damageable item in the slot is required when the amount is positive.

```yaml
requirements:
  durability:
    amount: { linear: { base: 30.0, step: -0.2, max: 10.0 } }
    slot: "MAIN_HAND"
```

An item that reaches max durability breaks like a vanilla break. Unlike normal
tool use, the cost is not routed through a cancellable `PlayerItemDamageEvent`,
so a durability-save ability elsewhere cannot dodge it.

### Enchanted item predicate

An item requirement may declare `enchanted: true`, which counts only items that
carry at least one enchantment toward the requirement. This pairs with an
enchantable-gear tag (e.g. `#c:enchantable` in `tags/base.yml`) so a requirement
can demand an actually-enchanted piece of gear while still excluding enchanted
books:

```yaml
items:
  - { action: "possession", tag: "#c:enchantable", slot: "MAIN_HAND", enchanted: true }
```

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
| `bonus` | double | `0` | Additive flat damage in engine half-hearts, added after multiplication. Use it to turn a held tool with a near-zero base (e.g. a hoe) into a real weapon |

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

### core:reduce_damage

Reduces the activating player's incoming `EntityDamageEvent` damage by a flat
percentage. Unlike `core:cancel_damage` there is no chance roll: a qualifying hit
is always blunted, so the mechanic reads as internalized armor rather than an
occasional dodge. Gate the sources with a `cause` state filter (for example
`cause:environmental`) or leave it unfiltered to soften every incoming blow.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `reduction` | double | `0` | Percentage (0-100) of incoming damage removed |

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

### core:modify_potion_amplifier

Adds a flat amplifier to every potion effect of a finished brew, raising its
potency (Speed I becomes Speed II) while leaving the duration untouched. Unlike
a duration multiplier it also strengthens instant potions (Instant Health,
Instant Harming), whose duration is effectively fixed. An amplifier of `0` is a
no-op, so a level-scaled evaluator (e.g. `milestones { 50: 1 }`) spends nothing
before crossing its threshold.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `amplifier` | double | `0` | Flat amplifier to add to each effect's level |

**Event:** `BrewEvent`

### core:modify_brew_output

The brewing analogue of `core:yield_multiplier`: a chance to grant a bonus
brewed potion on a finished batch. An extra bottle allows a chance (0-100) that
a batch yields a bonus bottle, cloned from an existing result. It is placed in a
free bottle slot of the stand, or granted to the player's inventory (dropped if
full) so no bonus item is ever lost. Reaching the chance roll counts as an
activation whether it succeeds or not, so the ability's shared cost/cooldown is
consumed once per batch and cannot be re-rolled for free.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) of an extra bottle; 100 = always |

**Event:** `BrewEvent`

### core:transmute

Converts a held stack of one material into another on a right-click of a
cauldron (the alchemy transmutation topic). Each `core:transmute` mechanic
defines a single source-&gt;product swap; an ability's "transmute table" is
expressed as several of these mechanics so it stays within the scalar evaluator
schema. The mechanic reads the player's main-hand item: when it matches `source`
with at least `source_count` items, `source_count` are consumed and
`product_count` of `product` are granted (inventory, dropped if full). The
vanilla cauldron interaction is cancelled. A mechanic whose `source` does not
match the held item is a no-op, so the remaining swap-mechanics in the same
ability spend nothing and one activation performs exactly the held swap.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `source` | string | required | Material held in the main hand, e.g. `minecraft:iron_ingot` |
| `product` | string | required | Material to grant, e.g. `minecraft:gold_ingot` |
| `source_count` | double | `1` | Number of source items consumed per activation |
| `product_count` | double | `1` | Number of product items granted per activation |

**Event:** `right_click_block` (`PlayerInteractEvent`, main-hand only)

### core:block_transform

Transforms a clicked block into another block when the player right-clicks it
while holding a matching catalyst, consuming one catalyst per affected block.
This is the terrain analogue of `core:transmute`: instead of swapping a held
item for a product stack, it swaps the clicked *block* for a `result` block
type. Which blocks may be transformed is scoped by the ability's own `target`
filter (e.g. a `#c:herbal_soil` tag), and a catalyst that does not match the
held item is a no-op that spends nothing. The vanilla click is cancelled so the
terrain interaction does not also run. An ability's "transform table" is
expressed as several of these mechanics, one per catalyst->result pair.

```yaml
mechanics:
  - type: "core:block_transform"
    filters: [ { target: "#c:herbal_soil" } ]
    parameters:
      catalyst: { constant: "minecraft:wheat_seeds" }
      result: { constant: "minecraft:grass_block" }
      catalyst_count: { constant: 1 }
```

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `catalyst` | string | required | Held main-hand material consumed per transform (e.g. `minecraft:wheat_seeds`) |
| `result` | string | required | Block material the clicked block becomes (e.g. `minecraft:grass_block`) |
| `catalyst_count` | double | `1` | How many catalysts are consumed per transform |

**Event:** `right_click_block` (`PlayerInteractEvent`, main-hand only)

### core:potion_self_immunity

Shields the thrower and allied players from the potion they threw. On a
`splash_potion` event, every affected living entity that is the throwing player
or another player is zeroed out of the splash, so a thrown splash/lingering
potion never harms the thrower or their own players. Hostile mobs are still hit
normally. This is the friction-elimination counterpart to brewing offensive
potions: throw poison or harming at a crowd without friendly fire on your group.

**Parameters:** None

**Event:** `potion_splash` (covers both `PotionSplashEvent` and `LingeringPotionSplashEvent`)

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

### core:aoe_damage

Deals a scaled share of the triggering melee hit to every living entity in a
radius around the damaged entity, so a single blow cleaves through a pack. The
primary target already receives the event's own damage through vanilla (and any
`core:modify_damage` scalar), so this mechanic strikes only the **other** foes in
`radius` — never the primary victim, the caster, or players. Each adjacent target
takes `multiplier` times the event's raw damage through the normal damage
pipeline (armor/protection still reduce it).

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `radius` | double | `3` | Radius in blocks to scan (clamped to [0, 32]) |
| `multiplier` | double | `1.0` | Fraction of the event's damage dealt to each adjacent foe |
| `targets` | string | `hostiles` | Who is hit: `allies`, `hostiles` (default, monsters and angered neutrals), or `all`. Players are always excluded |

**Event:** `EntityDamageByEntityEvent`

### core:fury

Ramps the player's damage by stacking a growing multiplier with each landed hit,
held for a short window. Each qualifying hit increments the stack (capped at
`max_stacks`) and multiplies that hit's outgoing damage by
`1 + stacks * multiplier_step`. Every hit refreshes the window; a hit that lands
after the window lapsed resets the ramp to zero first, so a player who stops
fighting must rebuild the bonus. The decay is lazy — no scheduled task runs — so
a stale ramp never applies to the next hit. The state is kept per player and
cleared on quit and reload.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier_step` | double | `0.1` | Damage multiplier added per stack (e.g. `0.1` = +10% per stack) |
| `max_stacks` | double | `5` | Maximum concurrent stacks (e.g. cap 5 = +50% at peak) |
| `window` | double | `4` | Seconds before the ramp resets when the player stops hitting |

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

### core:fishing_speed

Shortens the time between a cast and the fish bite. Fires on the cast (the
`fishing_cast` trigger, the `FISHING` state) and shrinks the single in-flight
hook's remaining wait time and its re-roll bounds by a percentage. Only the
current hook's timing is mutated — it never re-casts, never reels, and never
adds loot, so a cast cannot yield a second fish.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `reduction` | double | `0` | Percentage (0-100) to shorten the cast-to-bite wait. Values at or below 0 are a no-op |

**Event:** `PlayerFishEvent` (`fishing_cast` trigger, `FISHING` state)

### core:area_harvest

Breaks all matching blocks in a radius around the targeted block. The radius is
clamped to 32 and the scan stops at `max_blocks` (itself clamped to 128). Each
harvested block fires a synthetic `BlockBreakEvent`, so region/protection
plugins can cancel it, and consumes 1 tool durability (respecting Unbreaking,
with the tool breaking at max durability). Ageable crops still growing (not at
maximum age) are skipped, so a farm harvest never clears immature plants;
non-ageable targets (stone, sand, logs) are unaffected.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `radius` | double | `1` | Radius in blocks to harvest (clamped to 32) |
| `max_blocks` | double | `64` | Maximum number of blocks to break (clamped to 128) |

**Event:** `BlockBreakEvent`

### core:block_refund

Refunds a placed block back into the builder's inventory on a percentage roll,
so steady building never wastes material. On a successful roll the player nets
zero material cost for that placement; a full inventory drops the refund on the
ground so nothing is lost. Which blocks qualify is data-driven through the
ability's own filter (e.g. a `#c:construction_blocks` tag).

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) of refunding each placement |

**Event:** `BlockPlaceEvent`

### core:marked_demolition

Turns a TNT block the player **sneak-places** into a controlled demolition
charge. When that marked TNT detonates, only blocks the marking player placed
and that are in the configured `target` set are broken (and drop normally for
recovery); every other block in the blast — natural terrain and other players'
builds — survives. All TNT not sneak-placed by the player detonates 100% vanilla,
so mining/clearing TNT is untouched. This is the opt-in: sneak-placing a TNT is
the "mark for demolition" gesture.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `target` | string | none | A material or tag reference (`#c:demolition_blocks`) listing which placed blocks the demolition breaks and recovers |

**Event:** `BlockPlaceEvent` (marking) + engine `EntityExplodeEvent` (pruning)

### core:elytra_flight

Grants creative-mode flight while the player wears an elytra. A persistent,
re-evaluated unlock (bound to the `level_up` trigger and reconciled on join,
reload, `setlevel`, and `reset`): the capability is granted only when the chest
slot holds an elytra and removed when it comes off or the skill de-levels. Uses
the existing vanilla elytra rather than replacing it. No parameters.

**Event:** `level_up` (join/reload/setlevel/reset reconcile), with engine
inventory-change re-evaluation for equipment changes

### core:area_fertilize

When a player uses bonemeal on a block, also grows the matching same-type blocks
in a radius around it, so one bonemeal feeds a small patch. The radius is
clamped to 8. Bonemeal remains the required catalyst. The nested grow events
from the spread do not re-trigger abilities, so the effect cannot cascade
recursively.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `radius` | double | `1` | Radius in blocks to fertilize (clamped to 8) |

**Event:** `BlockFertilizeEvent` (`fertilize` trigger)

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

### core:bonus_enchant

Adds a single random, compatible enchantment to the item being enchanted at the table. The bonus enchant never overwrites a chosen enchant and respects mutually-exclusive rules.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Percentage chance to add the extra enchant (0-100) |

**Event:** `EnchantItemEvent`

### core:enchant_level_up

Raises each chosen enchantment by one level, capped at the enchant's own maximum, with an independent percentage chance per enchant.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Percentage chance for each enchant to gain +1 level (0-100) |

**Event:** `EnchantItemEvent`

### core:extract_enchant

Strips the highest-level enchantment off the held item and writes it into an enchanted book on a sneak + right-click of a grindstone. The vanilla grindstone GUI is suppressed for the sneak-interact so the act reads as an extraction; a normal interact keeps the vanilla GUI. Enchanted books are never valid sources (a single book cannot be duplicated into two). The item keeps its remaining enchantments; the extracted book is added to the inventory (dropped if full).

**No parameters.**

**Event:** `PlayerInteractEvent` (right-click block on a grindstone, set by the mechanic `filters`)

### core:keep_on_death

Gives a percentage chance to keep the entire inventory on death. A single roll is made per death; on success the death keeps the inventory and its drop list is cleared. No-ops harmlessly on servers that already keep inventory.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Percentage chance to keep the whole inventory (0-100) |

**Event:** `PlayerDeathEvent`

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

### core:equipment_attribute

Grants a level-scaled attribute modifier **only while the player wears a named
armor set**, and strips it as soon as any slot is swapped out. This is the
armor-gated cousin of `core:persistent_attribute` — the bonus (e.g. a heavy-armor
skill's always-on armor and armor-toughness mastery) lives only while the player
is actually armored in `equip_tag`, not as a free unconditional stat.

The modifier is transient (never saved to NBT), uses the same stable-UUID
replace-not-stack semantics as `core:persistent_attribute`, and is reconciled
event-driven on join, reload, `setlevel`/`reset`, and armor-changing inventory
events — never on a per-tick task.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `attribute` | string | none (required) | Namespaced attribute key (e.g., `minecraft:armor`, `minecraft:armor_toughness`) |
| `amount` | double | `0` | Additive modifier amount (evaluated at the current level). Applied only while armored; stripped otherwise |
| `uuid` | string | none (required) | Stable modifier UUID constant. Repeated executions with the same UUID refresh, never stack |
| `equip_tag` | material or `#...` tag | none (required) | Armor set every slot must match (e.g., `#c:heavy_armor`). Empty slots count as `AIR` |

**YAML usage:**

```yaml
abilities:
  - id: vanguard
    display_name: "Vanguard"
    unlock_level: 1
    trigger: "level_up"
    mechanics:
      - type: "core:equipment_attribute"
        parameters:
          attribute: { constant: "minecraft:armor" }
          amount: { linear: { base: 1.0, step: 0.07, max: 8.0 } }
          uuid: { constant: "7b24cdbd-a100-5976-8d7b-a8a39f69e3e7" }
          equip_tag: { constant: "#c:heavy_armor" }
```

**Event:** `level_up` (the level where the ability unlocks). Join, reload,
online `setlevel`, `reset`, and armor-slot changes also reconcile the bonus.

### core:trade_bonus

Gives the player a bonus emerald on a completed villager trade, added directly
to the player's inventory. The merchant recipe result is never changed.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) to pay the bonus emerald |

**Event:** `PlayerTradeEvent` (`trade` trigger)

### core:villager_xp

Grants the traded villager bonus experience on a completed trade, so its offers
tier up faster than the vanilla trade loop alone. No-op on merchants without a
villager experience concept (for example a wandering trader).

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `amount` | double | `0` | Bonus experience points added to the villager's total |

**Event:** `PlayerTradeEvent` (`trade` trigger)

### core:summon_villager

Summons a jobless villager (profession `NONE`) at the player's location on a
right-click. The villager is a normal vanilla entity with no Skilling state.

**Parameters:** None

**Event:** `PlayerInteractEvent` (`right_click_air` or `right_click_block` trigger)

### core:barter_luck

Improves the outcome of a piglin barter by adding a bonus emerald to the
outcome list with a configurable chance.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) to add the bonus emerald |

**Event:** `PiglinBarterEvent` (`barter` trigger)

### core:reroll_trades

Re-rolls a villager's trade offers when the player right-clicks it, by cycling
its profession through `NONE` and back. The villager keeps its profession,
type, and level. A jobless villager is left untouched.

**Parameters:** None

**Event:** `PlayerInteractEntityEvent` (`right_click_entity` trigger)

### core:summon_wandering_trader

Summons a wandering trader at the player's location on a right-click. The
trader keeps vanilla despawn behavior and carries no Skilling state.

**Parameters:** None

**Event:** `PlayerInteractEvent` (`right_click_air` or `right_click_block` trigger)

### core:pick_up_mob

Carries a mob as a rider of the player on a right-click of the mob. Any
non-player living mob can be carried: hostile, neutral, passive, or friendly,
including villagers. Which mobs are portable is data-driven: bind the ability to
the `right_click_entity` trigger and gate it with a `target_type` filter and a
`#...` entity tag. The player must have an empty main hand (the empty-hand
carry gesture), so vanilla feed/breed/shear/tame/trade interactions are never
interrupted.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `max_passengers` | double | `1` | How many mobs the player can carry at once |

**Event:** `PlayerInteractEntityEvent` (`right_click_entity` trigger)

 ### core:drop_passengers

Sets down every carried mob on an empty-hand right-click of air, the companion
gesture to `core:pick_up_mob`.

**Parameters:** None

**Event:** `PlayerInteractEvent` (`right_click_air` trigger)

### core:open_crafting

Opens a temporary, full-screen 3x3 crafting window for the player without
needing a crafting table block, so a "mobile workshop" ability can be used
anywhere. The window is a standard vanilla workbench GUI ([`Player.openWorkbench`]),
so it stores nothing and cannot persist anything when the plugin is removed.
Bind it to a right-click trigger; a left-click is a no-op. Gate it (held item,
hunger, cooldown) through the ability's `requirements:` block.

```yaml
mechanics:
  - type: "core:open_crafting"
```

**Parameters:** None

**Event:** `PlayerInteractEvent` (`right_click_air` or `right_click_block` trigger)

### core:ingredient_refund

Refunds one crafting ingredient on a percentage roll, the "no-waste craftsman"
perk. The refunded ingredient is identified programmatically from the placed
grid and needs no per-recipe configuration, so it works for any craft (shaped,
shapeless, vanilla, or data-pack). One random used ingredient is restored by a
single item, granted to the player's inventory (dropped when full); the result
slot and the grid are never touched, so the roll cannot interfere with the
craft's own consumption.

The trigger's own filter scopes which crafts the perk applies to. The optional
`ingredient` parameter (a material or `#...` tag) narrows which used ingredients
are refundable; when absent, any used ingredient of the craft is eligible.

```yaml
mechanics:
  - type: "core:ingredient_refund"
    filters: [ { target: "#c:wooden_products" } ]
    parameters:
      chance: { linear: { base: 5.0, step: 1.0, max: 30.0 } }
      # ingredient: { constant: "#minecraft:planks" }  # optional restriction
```

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) of refunding one ingredient |
| `ingredient` | string | (none) | Optional material or `#...` tag restricting which used ingredients may be refunded |

**Event:** `CraftItemEvent` (`craft_item` trigger)

### core:sneak_speed

Applies a movement-speed bonus that lasts only while the player sneaks. On the
`sneak` trigger it adds the modifier; the engine strips it when the player stops
sneaking, so it is never left behind.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Speed multiplier, e.g. `1.25` = 25% faster. Values `<= 1` are a no-op |
| `uuid` | string | *(random)* | Stable modifier UUID so repeated applications replace instead of stack |

**Event:** `PlayerToggleSneakEvent` (`sneak` trigger)

### core:sneak_effect

Applies a potion effect that lasts only while the player sneaks. On the `sneak`
trigger it applies the effect; the engine removes it when the player stops
sneaking, so it never lingers after the crouch. The potion-effect sibling of
`core:sneak_speed`, useful for a cloak-style effect.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `effect` | string | none | Namespaced potion effect key, e.g. `minecraft:invisibility` |
| `amplifier` | int | `0` | Effect amplifier |
| `duration` | int | `5` | Effect duration in seconds |

**Event:** `PlayerToggleSneakEvent` (`sneak` trigger)

### core:cancel_event

Cancels the triggering event, optionally gated by a percentage chance. This is
the generic suppression hammer: bind it to any cancellable event (physical
interaction, sculpt receive) and scope it with `target` / `state` filters.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | *(always)* | Percentage chance to cancel (0-100). Absent cancels unconditionally |

**Event:** The trigger the ability binds to; the event must be cancellable.

### core:drop_loot

Rolls a referenced loot table and drops the result naturally at the target. The
target can be a right-clicked entity, a right-clicked or stepped-on block, a
damaged entity, or (on a fishing catch) the fishing player. Works with vanilla
and datapack/plugin loot tables (resolved by namespaced key). Players are valid
targets and nothing is removed from them.

The plugin ships a bundled datapack, `datapacks/fishing.zip`, seeded into the
plugin data folder on first run and enabled automatically. It provides the
Fishing skill's drop-loot tables:

| Table key | Ability | Contents |
|---|---|---|
| `skilling:fishing/bait` | Plump Bait (L25) | A baited catch: cod, salmon, pufferfish, bone, string, glow ink sac |
| `skilling:fishing/master` | Master Angler (L100) | A rare angler's prize: emerald, name tag, enchanted book, saddle, nautilus shell, golden apple, echo shard |

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `table` | string | none | Namespaced loot table key, e.g. `minecraft:chests/simple_dungeon` or `stealth:pickpocket/pocket` |
| `chance` | double | *(always)* | Percentage chance to actually roll the table (0-100). Absent always rolls |

**Event:** `PlayerInteractEntityEvent`, `PlayerInteractEvent` (right/left click or physical), `EntityDamageByEntityEvent`, or `PlayerFishEvent` (drops at the fishing player's location)

> The target can also come from a `block_break` event: breaking a block drops
> the rolled loot one block above the broken block. This powers a "forage" loot
> reveal on any gather-style block set (see the Herbalism skill's Hidden Bounty
> and Rare Bloom), and lets any digging skill roll its own treasure table.

### core:loot_bonus

On generated world loot (a chest, trial-vault container, or other loot source),
a chance to pocket a single bonus copy of one of the generated items. The
scavenging analogue of `core:yield_multiplier`: it amplifies what looting yields
rather than replacing the vanilla container. The bonus is a fresh one-count copy
of a random non-air generated item, placed in the player's inventory (dropped at
the player if full). Because the `loot` trigger routes to nearby players, every
player near the loot location rolls independently. Reaching the chance roll
counts as an activation attempt whether or not it lands.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) of pocketing a bonus item |

**Event:** `loot` (`LootGenerateEvent`)

### core:vault_bonus

On a trial-vault state change (a key unlocking it and dispensing a reward), a
chance to grant the player a bonus item from a configured vault reward loot
table. The raider's-pack counterpart to `core:loot_bonus`, scoped to the
`vault_change` trigger where no generated-loot list is available. The bonus is a
single natural drop from the referenced table, handed to the player's inventory.
Reaching the chance roll counts as an activation attempt whether or not it lands.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) of the bonus |
| `table` | string | none (required) | Namespaced loot table key, e.g. `minecraft:chests/trial_chambers_reward` |

**Event:** `vault_change` (`VaultChangeStateEvent`)

### core:locate

Re-points the held compass to the nearest structure of a configured type,
persistently: the structure's position is written into the compass's
`minecraft:lodestone_tracker` data component, so the needle stays locked on it
(a "Wayfinder's Compass") until the compass is re-located or rebound to a real
lodestone. Fires only on a main-hand right-click with a `minecraft:compass`; a
non-compass held item or a structure with no instance within the search radius
is a no-op. The supported structure types are engine capability data (the list
below), not author-facing: a value outside the set fails at load.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `structure` | string | none (required) | A built-in structure type key: `buried_treasure`, `desert_pyramid`, `end_city`, `fortress`, `igloo`, `jungle_temple`, `mansion`, `mineshaft`, `monument`, `nether_fossil`, `ocean_ruin`, `ruined_portal`, `shipwreck`, `stronghold`, `swamp_hut` |

**Event:** `right_click_air` / `right_click_block` (main-hand `PlayerInteractEvent` with a compass)

### core:teleport_lodestone

Teleports the player back to the position a held compass is bound to — the
vanilla `minecraft:lodestone_tracker` the compass carries. The late-game
counterpart to `core:locate`: even where you planted a genuine lodestone, a
compass bound to it returns you there. Fires only on a main-hand right-click
with a `minecraft:compass`; a compass with no tracker bound is a no-op that
spends nothing. On a successful recall it also applies a vanilla item cooldown
to the compass's slot UI (`cooldown_ticks`), so the recall cannot be spammed.
Cross-dimension recall works by teleporting to the bound location directly.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `cooldown_ticks` | double | `0` | Optional vanilla item-cooldown ticks applied to the compass on a successful recall. While this cooldown is active, recent recalls are a no-op, so the slot UI both shows readiness and paces the ability |

**Event:** `right_click_air` / `right_click_block` (main-hand `PlayerInteractEvent` with a compass)

### core:biome_discovery

Grants a scaling movement-speed bonus per distinct biome a player has entered,
capped at `max_biomes`. Discovering a new biome (on the `map_explore` trigger, so
it rewards mapping the world) adds it to the player's persisted per-player
progress store and re-applies the attribute; the bonus grows both as the player
discovers more land and as the per-biome `amount` levels. Because it implements
`UnlockMechanic`, the engine re-runs it on join, reload, and level change,
re-applying the current bonus from the persisted count. The modifier is
transient and held under a stable UUID that replace-not-stacks, mirroring
`core:persistent_attribute`.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `attribute` | string | `minecraft:movement_speed` | Attribute to grow |
| `amount` | double | `0` | Level-scaled additive amount per discovered biome |
| `max_biomes` | double | `40` | Cap on how many distinct biomes count |
| `uuid` | string | none (required) | Stable modifier UUID so re-applies replace, never stack |

**Event:** `map_explore` (`ChunkLoadEvent`, new chunk with a map in hand). Join, reload, and `level_up` also reconcile it.

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
| `right_click` | `PlayerInteractEvent` (action `RIGHT_CLICK_AIR`/`RIGHT_CLICK_BLOCK`) or `PlayerInteractEntityEvent` | The union trigger: fires on any main-hand right-click regardless of surface (air, block, or entity), so a "right-click use" like playing a goat horn works no matter what the cursor happens to hit. Never fires for a left-click |
| `left_click_air` | `PlayerInteractEvent` (action `LEFT_CLICK_AIR`) | Left-clicking air with the main hand only. Never matches a block `target` filter |
| `left_click_block` | `PlayerInteractEvent` (action `LEFT_CLICK_BLOCK`) | Left-clicking a block with the main hand only. A `target` filter matches the clicked block |
| `left_click_entity` | `EntityDamageByEntityEvent` | Attacking an entity directly with a left-click (hand/punch only). Projectile attacks are not left-clicks and stay on the `entity_damage` and `shoot_bow` triggers. Supports `scaling: damage` |
| `consume_item` | `PlayerItemConsumeEvent` | Eating/drinking |
| `fishing` | `PlayerFishEvent` | Successfully catching a fish. Only the `CAUGHT_FISH` state dispatches. Casts, bites, reels, and failed attempts do not |
| `fishing_hook` | `PlayerFishEvent` | The bobber hooks a living mob. Only the `CAUGHT_ENTITY` state dispatches, once per hook |
| `fishing_cast` | `PlayerFishEvent` | A rod is cast into the water. Only the `FISHING` state dispatches, once per throw |
| `crop_grow` | `BlockGrowEvent` | Natural crop growth |
| `breed_animals` | `EntityBreedEvent` | Breeding animals |
| `sprint` | `PlayerToggleSprintEvent` | Player starts sprinting (release is not a trigger) |
| `sneak` | `PlayerToggleSneakEvent` | Player starts sneaking (release is not a trigger) |
| `jump` | `PlayerJumpEvent` | Player jumps (Packet-level jump detection) |
| `ride_horse` | `VehicleEnterEvent` | Player mounts a vehicle |
| `collect_xp` | `PlayerExpChangeEvent` | Collecting vanilla XP orbs |
| `level_up` | `SkillingLevelUpEvent` | A Skilling skill levels up |
| `enchant_item` | `EnchantItemEvent` | Enchanting an item at an enchanting table |
| `shoot_bow` | `EntityShootBowEvent` | Shooting a bow or crossbow |
| `item_damage` | `PlayerItemDamageEvent` | Item durability loss |
| `player_shear` | `PlayerShearEntityEvent` | Shearing a sheep or other shearable entity |
| `repair` | `PrepareAnvilEvent` | An anvil produces a valid repair (a result present with a positive level cost). Opening an anvil or shuffling its inputs alone does not fire, so it cannot be farmed |
| `player_tame` | `EntityTameEvent` | Taming a wild animal |
| `launch_projectile` | `ProjectileLaunchEvent` | Launching a projectile (trident, snowball, etc.) |
| `projectile_hit` | `ProjectileHitEvent` | A projectile lands on a block or entity (use for impact-time mechanics like `core:projectile_return`) |
| `resurrect` | `EntityResurrectEvent` | Totem of Undying activation |
| `cure_villager` | `EntityTransformEvent` | A zombie villager finishes converting into a villager (reason `CURED`). Attribution follows the player who initiated the cure (`ZombieVillager.getConversionPlayer()`). A cure that completes after that player logs off grants nothing |
| `elytra_glide` | `EntityToggleGlideEvent` | Player starts gliding with an elytra |
| `chunk_load` | `ChunkLoadEvent` | Exploring freshly generated terrain. Fires only when a chunk is generated for the first time (`isNewChunk()`), dispatched to players within their view distance (new chunks generate at the edge of the view, not at the player's feet). Throttled to once per player per 5 seconds because new terrain generates many chunks at once. Loading a chunk from disk does not fire it |
| `map_explore` | `ChunkLoadEvent` | Filling a map by carrying it into freshly generated terrain. Same dispatch rules and throttle as `chunk_load`, but fires only when the player holds a map element (an empty `minecraft:map` or a `minecraft:filled_map`) in either hand, so the cartography loop is the rewarded act |
| `sleep` | `PlayerDeepSleepEvent` | Player sleeps long enough to pass the night or storm. Checking into and back out of a bed does not fire it |
| `compost` | `CompostItemEvent` | An item is composted into a composter. Routed to nearby players of the composter |
| `loot` | `LootGenerateEvent` | World loot is generated (e.g., a container or trial-chamber vault fills). Routed to nearby players of the loot location, so group play counts for all present |
| `fertilize` | `BlockFertilizeEvent` | A player uses bonemeal on a block. Fires only when a player caused the fertilize. Nested grows from `core:area_fertilize` do not re-fire it |
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
| `sign_book` | `PlayerEditBookEvent` | Signing a book-and-quill into a written book (the "Sign and Close" action). Fires only on an actual signing, never on plain editing, so the resource/time cost is real |
| `jukebox_play` | `PlayerInteractEvent` | Inserting a music disc into an empty jukebox. Precise: right-clicking an occupied jukebox (an ejection) or clicking with no disc never fires, because dispatch checks the jukebox is populated afterwards |
| `lectern_place` | `PlayerInsertLecternBookEvent` (Paper) | Placing a book onto an empty lectern. Ejecting a book from an occupied lectern never fires |
| `physical_interaction` | `PlayerInteractEvent` (`Action.PHYSICAL`) | Stepping onto or into a block: pressure plates, weighted plates, and tripwires |
| `sensed` | `BlockReceiveGameEvent` (Paper) | A sculpt sensor or shrieker receives a vibration. Fires only when a player caused the vibration |
| `trip_trap` | `PlayerInteractEvent` (`Action.PHYSICAL`) **and** `BlockReceiveGameEvent` (Paper) | Combined silent-travel trigger covering both physical interactions (pressure plates, weighted plates, tripwires) and sculpt vibrations. One trigger for all travel hazards |
| `player_death` | `PlayerDeathEvent` | A player dies (before inventory drops process, so a mechanic can keep items) |

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
| `target_type` | `minecraft:entity_id` or `<#entity_tag>` | Type of the target entity. Matches the damaged entity on `entity_damage`/`entity_damage_taken`, the killed entity on `entity_kill`, and the clicked entity on `right_click_entity`. A `#...` value (e.g. `#c:undead`, `#minecraft:zombies`) resolves through the `entity_tags` store in `tags/base.yml`. Fails closed on events with no target entity |
| `offhand` | `empty`, `weapon`, or `<material>` / `<#tag>` | Offhand item state. `empty` = air, `weapon` = sword/axe/trident/mace. A material or `#...` tag value (e.g. `offhand:#c:melee_weapons`) matches the offhand item against that set, resolving through the `TagResolver` |
| `hand` | `empty`, `main_empty`, `off_empty` | Hand emptiness check |
| `equipped_all` | `<material>` or `<#tag>` | Every armor slot holds an item matching the target (e.g., `#c:light_armor`) |
| `equipped_any` | `<material>` or `<#tag>` | At least one armor slot holds an item matching the target |
| `cause` | `burn`, `fire`, `lava`, `drowning`, `suffocation`, `cactus`, `starvation`, `fly_into_wall`, `environmental` | The `EntityDamageEvent` damage cause on the `entity_damage_taken` trigger. `burn` matches fire, fire ticks, and lava; `fly_into_wall` matches elytra wall collisions; `environmental` is the compound survivable hazard set (fire, fire ticks, lava, drowning, suffocation, cactus, starvation, block and entity explosions, elytra wall collisions). Fails closed on any non-damage event or other cause. Values are validated at load |
| `honey_level` | `below:N`, `above:N`, `exactly:N` | The honey level of a beehive clicked on `player_interact`. Fails closed on non-beehive clicks. Values are validated at load |
| `instrument` | `minecraft:<instrument_key>` | Matches the specific goat-horn variant held in the main hand, read from the item's `minecraft:instrument` data component (e.g. `instrument:minecraft:sing_goat_horn`). Fails closed for a non-horn, a horn with no instrument data, or an unknown/blank value. Values are validated at load |
| `was_sneaking` | *(none)* | The triggering arrow was released while the player was sneaking. Reads the sneak stance stamped on the projectile at shot time (see `shoot_bow`), so it reflects how the shot was released rather than the player's stance when the arrow lands. Fails closed for non-projectile events, so it only matches bow shots |
| `target_status` | `minecraft:effect_key` | The event's target entity currently has the given potion effect (e.g. `state: "target_status:minecraft:glowing"`). Matches the damaged entity on `entity_damage`, the killed entity on `entity_kill`, and the clicked entity on `right_click_entity`. Fails closed on events without a living target or an unknown effect |
| `grown` | *(none)* | The broken block is a harvest-ready crop: an ageable crop (wheat, carrot, potato, beetroot, cocoa) at its maximum age, or a non-ageable crop (melon fruit, pumpkin fruit, sugar cane) which has no progress stage. Fails closed for non-crop blocks and non-block-break events. Gates harvest XP and farming yield so a place+break loop on immature plants can never be farmed |
| `target_unaware` | *(none)* | The event's damaged/clicked target is a hostile `Mob` that is not currently targeting the player (e.g. `state: "target_unaware"` in a requirement). Gates a backstab in `entity_damage`. Fails closed for non-mob victims and event-less requirements |

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
