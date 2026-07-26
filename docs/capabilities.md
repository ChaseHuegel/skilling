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
| `crop_grow` | `BlockGrowEvent` | Crop growth |
| `breed_animals` | `EntityBreedEvent` | Breeding animals |

## Built-In Evaluators

| Key | Description |
|---|---|
| `constant` | Fixed value regardless of level |
| `linear` | Scales linearly with level above unlock |
| `milestone` | Tiered values at specific level thresholds |
| `polynomial` | Power curve for XP progression |
