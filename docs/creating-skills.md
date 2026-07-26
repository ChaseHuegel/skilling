# Creating Skills

Skills are defined as YAML files in `plugins/Skilling/skills/`. Each file represents one skill with its progression curve, XP sources, and abilities.

## Schema Overview

```yaml
id: "mining"               # Unique identifier, used in commands and data storage
max_level: 100             # Maximum achievable level

display:
  name: "Mining"           # Display name in menus
  icon: "minecraft:iron_pickaxe"
  custom_model_data: 1001  # Optional custom model data for resource packs
  color: "GREEN"           # Boss bar color (GREEN, RED, BLUE, YELLOW, PURPLE, WHITE)
  style: "SEGMENTED_10"    # Boss bar style (SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20)

progression:
  curve: "polynomial"      # XP curve evaluator
  base_xp: 50              # Base XP required for level 1
  exponent: 2.5            # Polynomial exponent

xp_sources:                # List of triggers that grant XP
  - trigger: "block_break"
    filters:
      - target: "#c:ores"
      - state: "player_placed:false"
    reward:
      constant: 15.0

abilities:                 # List of abilities unlocked at certain levels
  - id: "geologist"
    display_name: "Geologist"
    unlock_level: 1
    ...
```

---

## XP Sources

Each XP source binds a [trigger](../capabilities.md#triggers) to an XP reward, optionally filtered by target tags or state conditions.

```yaml
xp_sources:
  - trigger: "block_break"           # Event type to listen for
    filters:
      - target: "#c:ores"            # Tag or material filter
      - state: "player_placed:false" # State condition
    reward:
      constant: 15.0                 # Flat XP per trigger fire
      # -- or --
      # linear:
      #   base: 10.0
      #   step: 0.5
      #   max: 50.0
```

### Filters

| Field | Type | Description |
|---|---|---|
| `target` | string | Material, `#minecraft:` tag, or `#c:` custom tag |
| `state` | string | State condition (e.g., `player_placed:false`, `is_sneaking`) |

### Reward Evaluators

| Type | Parameters | Description |
|---|---|---|
| `constant` | value | Flat reward regardless of level |
| `linear` | base, step, max | Scales linearly with level |
| `milestone` | level->value map | Tiered rewards at specific levels |
| `random` | min, max | Random value between min and max |

---

## Abilities

Abilities are the active or passive effects a player gains as they level up.

```yaml
abilities:
  - id: "vein_miner"
    display_name: "Vein Miner"
    unlock_level: 15        # Level required to unlock

    display:
      lore:
        - "&7Sneak-mine to break &a{chain_limit} &7connected ores."
        - "&7Exhaustion cost: &c{exhaustion} &7hunger."

    requirements:
      cooldown: 5.0          # Seconds between uses
      state:
        - "is_sneaking"      # Player must be sneaking
      items:
        - action: "possession"  # Must have this item
          tag: "#minecraft:pickaxes"
          slot: "MAIN_HAND"
        - action: "cost"        # Consumed on use
          tag: "minecraft:coal"
          amount: 1
          item_cooldown: 0.0

    on_failure:              # Override messages per failure type
      cooldown:
        action_bar: "&cVein Miner cooling down: {time}s"
      missing_item:
        action_bar: "&cRequires {amount}x {item}!"

    mechanics:
      - type: "core:chain_break"
        parameters:
          chain_limit:
            milestones:
              15: 3
              40: 8
              80: 16

    feedback:                # Success feedback
      notify:
        action_bar: true
        message: "&b✦ Vein Miner Activated! ✦"
      particles:
        - type: "BLOCK_CRACK"
          count: 15
          offset: [0.5, 0.5, 0.5]
          target: "target"
      sounds:
        - type: "ENTITY_ZOMBIE_BREAK_WOODEN_DOOR"
          volume: 0.5
          pitch: 1.2
          target: "self"
```

### Requirements

| Section | Sub-fields | Description |
|---|---|---|
| `cooldown` | `duration` (seconds) | Time between uses |
| `state` | list of strings | Player state requirements (`is_sneaking`, `is_in_water`) |
| `items` | list of item conditions | Item possession or cost requirements |

### Item Actions

| action | Description |
|---|---|
| `possession` | Player must have the item in the specified slot |
| `cost` | Item is consumed on ability use |

### Parameter Evaluators

| Type | Parameters | Behavior |
|---|---|---|
| `linear` | base, step, max | `value = base + step * (level - unlock_level)`, clamped to max |
| `milestone` | level->value map | TreeMap lookup: returns value for highest level ≤ current level |
| `constant` | value | Fixed value regardless of level |
| `random` | min, max | Uniform random between min and max |

### Feedback Types

| Section | Sub-fields | Description |
|---|---|---|
| `notify` | action_bar, chat, message | Text feedback |
| `particles` | type, count, offset, speed, target | Particle effects |
| `sounds` | type, volume, pitch, target | Sound effects |

Target values: `self` (player), `target` (affected block/entity), `origin` (ability source).
