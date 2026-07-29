# ISSUE-012: Comprehensive Mechanic & Trigger Coverage Proposal

## Current Coverage

- **14 Mechanics** across 7 categories (yield, chain break, damage, status, craft, brew, movement)
- **13 Triggers** across 9 Paper events (block, entity, inventory, interact, consume, fishing, farming, breeding)
- **Gaps:** Combat defense, enchanting, riding, building, environmental interaction, equipment, movement, trading, growing, and many more Paper API events

## Proposed New Mechanics

### Combat
| YAML Key | Paper Event Hook | Params | Description |
|----------|-----------------|--------|-------------|
| `core:block_damage` | `EntityDamageEvent` | `chance` (0-100%) | Chance to block incoming damage entirely (shield-like) |
| `core:thorns_damage` | `EntityDamageByEntityEvent` | `damage` (flat) | Reflect damage back to the attacker |
| `core:armor_bonus` | (self, passive) | `amount` | Permanently increase `GENERIC_ARMOR` / `GENERIC_ARMOR_TOUGHNESS` |
| `core:dodge` | `EntityDamageEvent` | `chance` (0-100%) | Chance to completely negate damage (evasion) |
| `core:knockback_resist` | (self, passive) | `amount` (0-1) | Increase `GENERIC_KNOCKBACK_RESISTANCE` |
| `core:lifesteal` | `EntityDamageByEntityEvent` | `percentage` | Heal for % of damage dealt |
| `core:crowd_control` | `EntityDamageByEntityEvent` | `effect`, `duration`, `amplifier`, `radius` | Apply AoE status to nearby targets on hit |
| `core:execute` | `EntityDamageByEntityEvent` | `threshold` (% HP) | Instantly kill target below HP threshold |

### Harvesting & Gathering
| YAML Key | Params | Description |
|----------|--------|-------------|
| `core:area_harvest` | `radius` | Break all harvestable blocks in radius (vein-miner alternative) |
| `core:shear_entity` | (none) | Auto-shear sheep/pumpkins when interacting |
| `core:strip_log` | (none) | Auto-strip logs when mining (instant) |
| `core:hoe_till` | `radius` | Till soil in area when using hoe |

### Crafting & Processing
| YAML Key | Params | Description |
|----------|--------|-------------|
| `core:auto_smelt` | `chance` | Auto-smelt mined blocks on break |
| `core:auto_repair` | `amount` | Repair tool durability on XP gain or timer |
| `core:bonus_drops` | `chance`, `multiplier`, `target_tag` | Multiplies any item drop (generic version of yield_multiplier) |
| `core:enchant_bonus` | `chance`, `levels` | Free enchantment levels on `EnchantItemEvent` |

### Movement & Mobility
| YAML Key | Paper Event | Params | Description |
|----------|-------------|--------|-------------|
| `core:double_jump` | `PlayerToggleSneakEvent` | `max_jumps`, `height` | Allow mid-air double jump |
| `core:speed_bonus` | (self, passive) | `multiplier` | Increase `GENERIC_MOVEMENT_SPEED` |
| `core:step_assist` | (self, passive) | `height` | Auto-step up blocks |
| `core:elytra_boost` | `PlayerToggleSneakEvent` | `power` | Elytra speed boost on toggle |
| `core:water_walk` | (self, passive) | `duration` | Temporary water walking |
| `core:conduit_power` | (self, passive) | `radius` | Permanent conduit power effect in a radius |

### Arcane & Magic
| YAML Key | Paper Event | Params | Description |
|----------|-------------|--------|-------------|
| `core:xp_bonus` | (self, passive) | `multiplier` | Multiply all XP gained |
| `core:xp_bottle` | `ProjectileLaunchEvent` | `xp` (amount) | Throw XP bottle on interact |
| `core:anvil_discount` | `AnvilInteractionEvent` | `multiplier` | Reduce anvil repair/rename costs |
| `core:grindstone_disinchant` | `GrindstoneEvent` | `retention` (%) | Chance to retain books/items from disenchanting |

## Proposed New Triggers

### Combat & Damage
| YAML Key | Paper Event | Description |
|----------|-------------|-------------|
| `shoot_bow` | `EntityShootBowEvent` | When player fires a bow/crossbow |
| `take_damage` | `EntityDamageEvent` | Generic damage taken (already partially covered by `entity_damage_taken`) |
| `player_kill` | `PlayerDeathEvent` (as damager) | When player kills another player |
| `projectile_hit` | `ProjectileHitEvent` | When a player's projectile hits something |
| `block_place` | `BlockPlaceEvent` | When placing a block (already exists) |
| `enchant_item` | `EnchantItemEvent` | When enchanting at a table |
| `anvil_use` | `AnvilPrepareEvent` | When using anvil (renaming/repairing) |
| `grindstone_use` | `GrindstoneEvent` | When using grindstone |
| `shear_entity` | `ShearEntityEvent` | When shearing sheep/mushrooms/pumpkins |
| `milk_cow` | `PlayerInteractEntityEvent` | When milking a cow (with bucket) |
| `villager_trade` | `VillagerTradeEvent` | When trading with a villager |
| `smithing_transform` | `SmithItemEvent` | When using smithing template |
| `strider_tick` | (`PlayerMoveEvent` + `checkVehicle`) | When riding a strider |
| `vehicle_enter` | `VehicleEnterEvent` | When entering any vehicle/ mount |
| `ride_horse` | `PlayerInteractEntityEvent` + horse check | When mounting a horse |
| `jump` | `PlayerJumpEvent` (Paper API) | When the player jumps |
| `sprint` | `PlayerToggleSprintEvent` | When toggling sprint |
| `sneak` | `PlayerToggleSneakEvent` | When toggling sneak |
| `fly` | `PlayerToggleFlightEvent` | When toggling flight |
| `glide` | `PlayerToggleSneakEvent` + `isGliding` | When toggling elytra glide (sneak to descend) |
| `swim` | `PlayerMoveEvent` + water check | When swimming / moving in water |
| `climb` | `PlayerMoveEvent` + ladder/twisting_vine check | When climbing a ladder or vine |
| `sleep` | `PlayerBedEnterEvent` | When entering a bed |
| `feed_animal` | `PlayerInteractEntityEvent` + breedable check | When feeding/breeding animals |
| `tame_animal` | `EntityTameEvent` | When taming a wild animal |
| `bone_meal` | `BlockFertilizeEvent` | When using bone meal |
| `plant_seed` | `BlockPlaceEvent` + seed material check | When planting seeds/crops |
| `harvest_crop` | `BlockBreakEvent` + crop material check | When harvesting mature crops |
| `collect_xp` | `PlayerExpChangeEvent` | When collecting XP orbs |
| `level_up` | `PlayerLevelChangeEvent` | When the player's vanilla level changes |
| `fish` | `PlayerFishEvent` | Already exists (`fishing` trigger) |
| `fill_bucket` | `PlayerBucketFillEvent` | When filling a bucket |
| `empty_bucket` | `PlayerBucketEmptyEvent` | When emptying a bucket |
| `potion_brew` | `BrewEvent` | Already exists (`brew_potion` trigger) |
| `potion_effect` | `PotionEffectAddEvent` | When a potion effect is applied |
| `item_pickup` | `PlayerAttemptPickupItemEvent` | When picking up an item |
| `item_drop` | `PlayerDropItemEvent` | When dropping an item from inventory |
| `item_break` | `PlayerItemBreakEvent` | When a tool/armor breaks from use |
| `item_consume` | `PlayerItemConsumeEvent` | Already exists (`consume_item` trigger) |
| `equip_change` | `PlayerItemHeldEvent` | When changing held items |
| `armor_change` | `InventoryClickEvent` + armor slots | When equipping/unequipping armor |

## Implementation Strategy

### Phase 1: Core Mechanics (8 new)
- All combat-related mechanics
- `core:auto_smelt`, `core:bonus_drops`
- `core:speed_bonus`, `core:step_assist`

### Phase 2: Core Triggers (10 new)
- Combat triggers (shoot_bow, projectile_hit)
- Mount/movement triggers (jump, sprint, sneak, ride_horse)
- Vanilla XP triggers (collect_xp, level_up)
- Crafting triggers (enchant_item, anvil_use)

### Phase 3: Advanced Mechanics (8 new)
- Magic and arcane mechanics
- Movement mechanics (double_jump, water_walk, elytra_boost)

### Phase 4: Advanced Triggers (15+ new)
- Interaction triggers (fill_bucket, feed_animal, plant_seed, harvest_crop)
- Inventory triggers (item_pickup, item_drop, equip_change, armor_change)
- Remaining triggers

## Risk

Low-Medium. Each mechanic/trigger is self-contained. The main risk is
Paper API compatibility for newer events like `PlayerJumpEvent`.
