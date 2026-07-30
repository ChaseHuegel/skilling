# Capabilities Catalog

## Built-In Mechanics

### core:yield_multiplier

Multiplies block drops by a percentage chance on each break.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `yield_chance` | double | `0` | Probability (0-100%) of bonus drops |

**Event:** `BlockBreakEvent`

### core:chain_break

Breaks connected blocks of the same type up to a limit (vein mining).

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chain_limit` | double | `0` | Maximum connected blocks to break |

**Event:** `BlockBreakEvent`

### core:block_damage

Damages blocks in an area for instant breaking.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `chance` | double | `0` | Probability (0-100%) of block damage |

**Event:** `BlockBreakEvent`

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
| `effect` | string | — | Potion effect type (e.g., `SLOWNESS`, `POISON`) |
| `duration` | double | `3` | Duration in seconds |
| `amplifier` | double | `0` | Effect amplifier (0 = level I) |

**Event:** `EntityDamageByEntityEvent`

### core:cancel_damage

Chance to completely cancel incoming damage (evasion/block).

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
| `attribute` | string | — | Attribute name (e.g., `GENERIC_MOVEMENT_SPEED`) |
| `amount` | double | `0` | Modifier value |
| `duration` | double | `5` | Duration in seconds |

### core:armor_bonus

Applies a temporary armor bonus attribute modifier.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `amount` | double | `0` | Additional armor points |

### core:knockback_resist

Applies a temporary knockback resistance attribute modifier.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `amount` | double | `0` | Knockback resistance (0-1) |

### core:speed_bonus

Applies a temporary movement speed attribute modifier.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Speed multiplier |

**Event:** `PlayerToggleSprintEvent`

### core:modify_craft_output

Multiplies the output of crafting recipes.

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

**Event:** `BrewEvent`

### core:modify_potion_duration

Modifies the duration of brewed potion effects.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `modifier` | double | `1.0` | Duration multiplier for potion effects |

**Event:** `BrewEvent`

### core:aoe_effect

Applies a potion effect to all entities within a radius.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `effect` | string | — | Potion effect type |
| `radius` | double | `5` | Effect radius in blocks |
| `duration` | double | `5` | Duration in seconds |
| `amplifier` | double | `0` | Effect amplifier |

### core:projectile

Launches a custom projectile from the player.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `speed` | double | `1.5` | Projectile velocity multiplier |
| `damage` | double | `4` | Damage dealt on hit |

**Event:** `PlayerInteractEvent`

### core:teleport

Short-range teleport in the player's looking direction.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `range` | double | `10` | Teleport distance in blocks |

**Event:** `PlayerInteractEvent`

### core:thorns_damage

Reflects a percentage of incoming damage back to the attacker.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `percentage` | double | `0` | Damage reflection percentage (0-100) |

**Event:** `EntityDamageEvent`

### core:dodge

Chance to completely dodge incoming damage.

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

Applies slowness to nearby enemies within a radius on damage.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `effect` | string | `SLOWNESS` | Potion effect type |
| `duration` | double | `3` | Duration in seconds |
| `amplifier` | double | `0` | Effect amplifier |
| `radius` | double | `5` | Effect radius in blocks |

**Event:** `EntityDamageByEntityEvent`

### core:execute

Instantly kills targets below a health threshold.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `threshold` | double | `0` | Health percentage threshold (0-100) |

**Event:** `EntityDamageByEntityEvent`

### core:auto_smelt

Automatically smelts mined blocks.

**Parameters:** None

**Event:** `BlockBreakEvent`

### core:xp_bonus

Applies a multiplicative XP bonus to all XP gains for a duration.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | XP multiplier applied to all gains |
| `duration` | double | `60` | Duration in seconds |

**Event:** Varies (triggered by ability activation)

## Built-In Triggers

| Key | Event | Description |
|---|---|---|
| `block_break` | `BlockBreakEvent` | Breaking a block |
| `block_place` | `BlockPlaceEvent` | Placing a block |
| `entity_damage` | `EntityDamageByEntityEvent` | Damaging an entity |
| `entity_damage_taken` | `EntityDamageEvent` | Taking damage |
| `entity_kill` | `EntityDeathEvent` | Killing an entity |
| `craft_item` | `CraftItemEvent` | Crafting an item |
| `furnace_extract` | `FurnaceExtractEvent` | Extracting from a furnace |
| `brew_potion` | `BrewEvent` | Brewing potions |
| `player_interact` | `PlayerInteractEvent` | Interacting (right/left click) |
| `consume_item` | `PlayerItemConsumeEvent` | Eating/drinking |
| `fishing` | `PlayerFishEvent` | Fishing |
| `crop_grow` | `BlockGrowEvent` | Natural crop growth |
| `breed_animals` | `EntityBreedEvent` | Breeding animals |
| `sprint` | `PlayerToggleSprintEvent` | Player starts/stops sprinting |
| `sneak` | `PlayerToggleSneakEvent` | Player starts/stops sneaking |
| `ride_horse` | `VehicleEnterEvent` | Player mounts a vehicle |
| `collect_xp` | `PlayerExpChangeEvent` | Collecting vanilla XP orbs |
| `level_up` | `PlayerLevelChangeEvent` | Vanilla Minecraft level change |
| `enchant_item` | `EnchantItemEvent` | Enchanting an item at an enchanting table |

## Built-In Evaluators

| Key | Description |
|---|---|
| `constant` | Fixed value regardless of level |
| `linear` | Scales linearly with level above unlock |
| `milestones` | Tiered values at specific level thresholds |
| `polynomial` | Power curve for XP progression |
