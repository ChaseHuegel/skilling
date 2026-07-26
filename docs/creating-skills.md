# Creating Skills

Skill definitions are YAML files placed in `plugins/Skilling/skills/`. Each file defines one skill with its progression, XP sources, and abilities.

## Minimal Example

```yaml
id: "mining"
progression:
  curve: "polynomial"
  base_xp: 50
  exponent: 2.5
xp_sources:
  - trigger: "block_break"
    reward:
      constant: 15.0
```

## Full Schema

### Top-Level Fields

| Key | Required | Type | Description |
|---|---|---|---|
| `id` | Yes | string | Unique skill identifier (used in commands and DB) |
| `max_level` | No | int | Maximum achievable level (default: 100) |
| `display` | No | section | UI appearance (see below) |
| `progression` | Yes | section | XP curve configuration |
| `xp_sources` | No | list | Actions that grant XP |
| `abilities` | No | list | Unlockable abilities |

### display

| Key | Type | Default | Description |
|---|---|---|---|
| `name` | string | `"Unknown"` | Display name in the skill overview GUI |
| `icon` | string | `"minecraft:barrier"` | Material for the GUI icon |
| `custom_model_data` | int | `0` | Custom model data for resource packs |
| `color` | string | `"WHITE"` | BossBar color (GREEN, RED, BLUE, etc.) |
| `style` | string | `"SOLID"` | BossBar style (SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20) |

### progression

| Key | Type | Default | Description |
|---|---|---|---|
| `curve` | string | `"polynomial"` | XP curve type: `polynomial`, `linear`, or `constant` |
| `base_xp` | double | `50.0` | XP required for level 1 |
| `exponent` | double | `2.5` | Exponent for polynomial curve |

### xp_sources

Each entry defines an action that grants XP.

| Key | Required | Type | Description |
|---|---|---|---|
| `trigger` | Yes | string | Event trigger key (e.g., `block_break`, `entity_kill`, `craft_item`) |
| `filters` | No | list | Conditions that must be met |
| `reward` | Yes | section | XP reward evaluator |

#### filters

| Key | Type | Description |
|---|---|---|
| `target` | string | Material or tag filter (`minecraft:iron_ore` or `#c:ores`) |
| `state` | string | Player state condition (`is_sneaking`, `is_sprinting`) |

#### reward

Uses evaluator syntax (see Evaluators below).

### abilities

Each entry defines an unlockable ability with mechanics.

| Key | Required | Type | Description |
|---|---|---|---|
| `id` | Yes | string | Unique ability identifier |
| `display_name` | No | string | Human-readable name (default: same as `id`) |
| `unlock_level` | No | int | Level required to unlock (default: 1) |
| `display` | No | section | UI lore configuration |
| `requirements` | No | section | Pre-execution requirements |
| `on_failure` | No | section | Failure feedback overrides |
| `mechanics` | Yes | list | Executable mechanic actions |
| `feedback` | No | section | Success feedback (particles, sounds, messages) |

#### requirements

| Key | Type | Default | Description |
|---|---|---|---|
| `cooldown` | double | `0` | Cooldown in seconds between uses |
| `state` | list | `[]` | Required player states (`is_sneaking`, `is_sprinting`, `is_in_water`) |
| `items` | list | `[]` | Item requirements |

##### items

| Key | Type | Description |
|---|---|---|
| `action` | string | `possession` (must have) or `cost` (consumed on use) |
| `tag` | string | Material or tag identifier |
| `slot` | string | Inventory slot (`HAND`, `OFF_HAND`, etc.) |
| `amount` | int | Required quantity |
| `item_cooldown` | double | Visual cooldown in seconds |

#### mechanics

| Key | Type | Description |
|---|---|---|
| `type` | string | Mechanic registry key (e.g., `core:yield_multiplier`) |
| `filters` | list | Material/state filters |
| `parameters` | section | Evaluator parameters for the mechanic |

#### feedback

| Key | Type | Description |
|---|---|---|
| `notify.action_bar` | bool | Show action bar message |
| `notify.chat` | bool | Show chat message |
| `notify.message` | string | Message text |
| `particles` | list | Particle effect configurations |
| `sounds` | list | Sound effect configurations |

## Evaluators

Evaluators compute dynamic numeric values based on the player's level.

### constant

A fixed value.

```yaml
reward:
  constant: 15.0
```

### linear

Scales linearly with level above the unlock point.

```yaml
parameters:
  yield_chance:
    linear:
      base: 0.5      # Value at unlock level
      step: 0.5      # Added per level above unlock
      max: 50.0      # Hard ceiling
```

### milestones

Tiered values at specific levels using a TreeMap for O(log n) lookup.

```yaml
parameters:
  chain_limit:
    milestones:
      15: 3
      40: 8
      80: 16
```

### polynomial

Used for XP progression curves.

```yaml
progression:
  curve: "polynomial"
  base_xp: 50
  exponent: 2.5
```

## Built-In Mechanics

See [capabilities.md](capabilities.md) for a full catalog of available mechanics, triggers, and evaluators.
