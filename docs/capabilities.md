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
| `effect` | double | — | Legacy numeric potion effect ID (e.g., `2` for Slowness, `10` for Regeneration) |
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
| `attribute` | double | — | Legacy numeric attribute ID (1=MAX_HEALTH, 2=FOLLOW_RANGE, 3=KNOCKBACK_RESISTANCE, 4=MOVEMENT_SPEED, 5=FLYING_SPEED, 6=ARMOR, 7=ARMOR_TOUGHNESS, 8=ATTACK_DAMAGE, 9=ATTACK_SPEED, 10=LUCK) |
| `amount` | double | `0` | Modifier value |
| `duration` | double | `5` | Duration in seconds |

### core:armor_bonus

Applies a temporary armor bonus attribute modifier.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `amount` | double | `0` | Additional armor points |
| `duration` | double | `300` | Duration in seconds |

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
| `multiplier` | double | `1.0` | Multiplicative speed multiplier (1.5 = 50% faster, not a percentage) |
| `duration` | double | `300` | Duration in seconds |

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

Applies a potion effect to all entities within a radius (excluding the player).

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `effect` | double | — | Legacy numeric potion effect ID |
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

Applies a potion effect to nearby enemies within a radius on damaging an entity.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `effect` | double | — | Legacy numeric potion effect ID |
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

Applies a multiplicative XP bonus to all XP gains for the player's session.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Multiplicative XP multiplier applied to all gains (1.5 = +50%, 2.0 = double; not a percentage increase) |

**Event:** Varies (triggered by ability activation)

### core:fishing_yield

Grants bonus catch items when fishing.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `yield_chance` | double | `0` | Probability (0-100%) of bonus catch |

**Event:** `PlayerFishEvent`

### core:fishing_loot

Multiplies the quality or quantity of loot from fishing treasure.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Loot multiplier |

**Event:** `PlayerFishEvent`

### core:area_harvest

Breaks all matching blocks in a radius around the targeted block.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `radius` | double | `1` | Radius in blocks to harvest |
| `max_blocks` | double | `8` | Maximum number of blocks to break |

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

**Event:** `BlockBreakEvent`

### core:repair_discount

Reduces the experience level cost of anvil repairs.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `discount` | double | `0` | Percentage discount (0-100) |

**Event:** `PrepareAnvilEvent`

### core:modify_tame_chance

Multiplies the chance of successfully taming an animal.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Taming chance multiplier |

**Event:** `EntityTameEvent`

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

Applies a potion effect to the player and all nearby living entities within a radius.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `effect` | double | — | Legacy numeric potion effect ID (e.g., `10` for Regeneration) |
| `radius` | double | `8` | Aura radius in blocks |
| `duration` | double | `5` | Duration in seconds |
| `amplifier` | double | `0` | Effect amplifier |

**Event:** Varies (triggered by ability activation)

### core:modify_jump

Temporarily increases the player's jump strength.

**Parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| `multiplier` | double | `1.0` | Jump multiplier (1.5 = 50% higher) |
| `duration` | double | `300` | Duration in seconds |

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
| `shoot_bow` | `EntityShootBowEvent` | Shooting a bow or crossbow |
| `item_damage` | `PlayerItemDamageEvent` | Item durability loss |
| `player_shear` | `PlayerShearEntityEvent` | Shearing a sheep or other shearable entity |
| `player_tame` | `EntityTameEvent` | Taming a wild animal |
| `launch_projectile` | `ProjectileLaunchEvent` | Launching a projectile (trident, snowball, etc.) |

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
| `player_placed` | `false` | Block was not placed by a player |
| `dimension` | `overworld`, `nether`, `end` | Player's current dimension |
| `weather` | `clear`, `rain`, `thunder` | Current weather in player's world |
| `time` | `day`, `night` | Time of day in player's world |
| `light_level` | `below:N`, `above:N`, `exactly:N` | Block light level comparison |
| `health` | `below:N%`, `above:N%` | Player health percentage |
| `hunger` | `below:N`, `above:N` | Player food level |
| `biome` | `minecraft:biome_id` | Player's current biome |
| `target_type` | `minecraft:entity_id` | Type of entity being damaged |
| `offhand` | `empty`, `weapon` | Offhand item state |
| `hand` | `empty`, `main_empty`, `off_empty` | Hand emptiness check |
| `armor` | `empty` | All armor slots are empty |

## Built-In Evaluators

| Key | Description |
|---|---|
| `constant` | Fixed value regardless of level |
| `linear` | Scales linearly with level above unlock |
| `milestones` | Tiered values at specific level thresholds |
| `polynomial` | Power curve for XP progression |
