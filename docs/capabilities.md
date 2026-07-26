# Capabilities Catalog

This documents every built-in trigger, mechanic, and parameter evaluator shipped with Skilling.

---

## Triggers

Triggers hook into Paper events to grant XP or activate mechanics.

| ID | Event | Description |
|---|---|---|
| `block_break` | `BlockBreakEvent` | Player breaks a block |
| `block_place` | `BlockPlaceEvent` | Player places a block |
| `entity_damage` | `EntityDamageByEntityEvent` | Player damages an entity |
| `entity_kill` | `EntityDeathEvent` | Player kills an entity |
| `craft_item` | `CraftItemEvent` | Player crafts an item |
| `furnace_smelt` | `FurnaceExtractEvent` | Player collects smelted output |
| `fish_catch` | `PlayerFishEvent` | Player catches something |
| `crop_grow` | `BlockGrowEvent` | A crop grows (triggered by Farm/Herbalism skills) |
| `breed_mobs` | `EntityBreedEvent` | Player breeds animals |
| `enchant_item` | `EnchantItemEvent` | Player enchants an item |
| `consume_item` | `PlayerItemConsumeEvent` | Player eats or drinks |

### Trigger Configuration

```yaml
xp_sources:
  - trigger: "block_break"
    filters:
      - target: "#c:ores"         # Material/tag filter
      - state: "player_placed:false"  # State condition
    reward:
      constant: 15.0
```

---

## Mechanics

Mechanics are the executable effects triggered by abilities.

### `core:yield_multiplier`

Multiplies item drops from block break events.

**Parameters:**

| Parameter | Evaluator | Description |
|---|---|---|
| `yield_chance` | linear, milestone, constant | Extra yield percentage |

**Filters:**

| Filter | Description |
|---|---|
| `target` | Block types affected |
| `tool` | Required tool type |

### `core:chain_break`

Breaks connected blocks of the same type (vein miner).

**Parameters:**

| Parameter | Evaluator | Description |
|---|---|---|
| `chain_limit` | milestone, linear | Maximum blocks broken in a chain |
| `exhaustion` | linear | Hunger cost per activation |

### `core:modify_damage`

Modifies outgoing damage with multipliers or flat additions.

**Parameters:**

| Parameter | Evaluator | Description |
|---|---|---|
| `multiplier` | linear, milestone | Damage multiplier |
| `flat` | linear | Flat damage bonus |
| `armor_pierce` | milestone | Armor penetration percentage |

### `core:apply_status`

Applies a potion effect to the player or target.

**Parameters:**

| Parameter | Evaluator | Description |
|---|---|---|
| `effect` | string constant | Effect type (e.g., `SPEED`, `SLOWNESS`) |
| `duration` | linear | Duration in ticks |
| `amplifier` | linear | Effect amplifier |
| `target` | string constant | `self` or `target` |

### `core:cancel_damage`

Cancels incoming damage with a probability check.

**Parameters:**

| Parameter | Evaluator | Description |
|---|---|---|
| `chance` | linear, milestone | Evasion probability (0.0 - 1.0) |

---

## Parameter Evaluators

| Registry Key | Class | YAML Key | Description |
|---|---|---|---|
| `constant` | `ConstantEvaluator` | `constant` | Fixed value |
| `linear` | `LinearEvaluator` | `linear` | `base + step * (level - unlockLevel)`, clamped |
| `milestone` | `MilestoneEvaluator` | `milestones` | TreeMap tiered lookup |
| `random` | `RandomEvaluator` | `random` | Uniform random in [min, max] |
| `polynomial` | `PolynomialEvaluator` | `polynomial` | `baseXp * (level ^ exponent)` for XP curves |
