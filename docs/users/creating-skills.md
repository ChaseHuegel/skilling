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
| `max_level` | No | int | Maximum achievable level (default: 100, max 10000) |
| `display` | No | section | UI appearance (see below) |
| `progression` | Yes | section | XP curve configuration |
| `xp_sources` | No | list | Actions that grant XP |
| `abilities` | No | list | Unlockable abilities |
| `level_up_commands` | No | list | Console commands executed on every level-up (supports `{player}`, `{level}`, `{skill_id}`, `{skill_name}` placeholders) |

### display

| Key | Type | Default | Description |
|---|---|---|---|
| `name` | string | `"Unknown"` | Display name in the skill overview GUI |
| `icon` | string | `"minecraft:barrier"` | Material for the GUI icon |
| `custom_model_data` | int | `0` | Custom model data for resource packs |
| `color` | string | `"WHITE"` | BossBar color (GREEN, RED, BLUE, etc.) |
| `style` | string | `"SOLID"` | BossBar style (SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20) |
| `lore` | list | `[]` | Optional descriptive lore lines for the skill tooltip. Supports placeholders: `{level}`, `{max_level}`, `{skill_name}`, `{xp}` |

### progression

Every curve is derived from `base_xp` (plus `exponent` for polynomial). A
`linear` curve uses step `base_xp * 0.1`. A `constant` curve returns `base_xp`
for every level. There are no other per-curve keys. The web GUI and the engine
share this single schema.

| Key | Type | Default | Description |
|---|---|---|---|
| `curve` | string | `"polynomial"` | XP curve type: `polynomial`, `linear`, or `constant` |
| `base_xp` | double | `50.0` | XP required for level 1 |
| `exponent` | double | `2.5` | Exponent for the polynomial curve (ignored by `linear`/`constant`) |

### xp_sources

Each entry defines an action that grants XP.

| Key | Required | Type | Description |
|---|---|---|---|
| `trigger` | Yes | string | Event trigger key (e.g., `block_break`, `entity_kill`, `craft_item`) |
| `filters` | No | list | Conditions that must be met |
| `reward` | Yes | section | XP reward evaluator |
| `scaling` | No | string | `damage` to multiply the reward by the event's raw base damage (see below). Omitted for a flat reward |

#### filters

| Key | Type | Description |
|---|---|---|
| `target` | string | Material or tag filter (`minecraft:iron_ore` or `#c:ores`) |
| `tool` | string | Material or tag filter for the item in the player's hand |
| `state` | string | Player state condition (see available states below) |

> **Note:** The `target:` filter on `entity_damage` matches the **damager projectile
> material** only (e.g., arrows, tridents, fireballs). It does **not** match the
> player's held weapon. For held-weapon detection on melee `entity_damage`, use the
> `tool:` filter, which reads the main-hand item.

Available states:

| State | Description |
|---|---|
| `is_sneaking` | Player is sneaking |
| `is_sprinting` | Player is sprinting |
| `is_in_water` | Player is in water |
| `is_on_ground` | Player is on the ground |
| `is_on_fire` | Player is on fire |
| `is_riding` | Player is riding a vehicle/mount |
| `player_placed:false` | Block was not placed by a player (natural generation only) |
| `player_placed:true` | Block was placed by a player |
| `dimension:overworld` | Player is in the Overworld |
| `dimension:nether` | Player is in the Nether |
| `dimension:end` | Player is in The End |
| `weather:clear` | World has clear weather |
| `weather:rain` | World has rain/storm |
| `weather:thunder` | World has a thunderstorm |
| `time:day` | World time is between dawn and dusk |
| `time:night` | World time is between dusk and dawn |
| `light_level:below:\<value\>` | Block light level is below the threshold (0-15) |
| `light_level:above:\<value\>` | Block light level is above the threshold (0-15) |
| `light_level:exactly:\<value\>` | Block light level is exactly the value (0-15) |
| `health:below:\<pct\>%` | Player health percentage is below threshold |
| `health:above:\<pct\>%` | Player health percentage is above threshold |
| `hunger:below:\<value\>` | Player food level is below threshold (0-20) |
| `hunger:above:\<value\>` | Player food level is above threshold (0-20) |
| `biome:\<key\>` | Player is in a specific biome (e.g., `minecraft:plains`) |
| `target_type:\<key\>` | Damaged entity type matches (e.g., `minecraft:zombie`, `#minecraft:skeletons`) |
| `equipped_all:\<target\>` | Every armor slot holds an item matching `target` (a material like `minecraft:leather_helmet` or a `#...` tag like `#c:light_armor`) |
| `equipped_any:\<target\>` | At least one armor slot holds an item matching `target` |

The bundled tags `#c:unarmored`, `#c:light_armor`, `#c:medium_armor`, and
`#c:heavy_armor` reproduce the historical armor tiers as data. `#c:unarmored`
includes empty slots (`minecraft:air`), the elytra, and headwear (pumpkins,
skulls), so `equipped_all:#c:unarmored` passes when every slot is empty or
holds such an item.

#### reward

Uses evaluator syntax (see Evaluators below). A scalar value (e.g. `reward: 50`)
is rejected at load with an `IllegalArgumentException`. It must be an evaluator
block such as `reward: { constant: 50 }`.

#### scaling

The optional `scaling: damage` value multiplies the configured `reward` by the
raw base damage of the triggering event (`getDamage()`, pre-mitigation, in
half-hearts: a 5-heart fall is `10.0`). It is only valid on damage triggers
(`fall_damage`, `entity_damage_taken`, and `entity_damage` outgoing). It is
rejected at load with an `IllegalArgumentException` on any other trigger, so a
typo cannot silently disable a source. Example:

```yaml
xp_sources:
  - trigger: "fall_damage"
    reward: { constant: 25.0 }
    scaling: damage
```

A source with `scaling: damage` grants `round(reward × damage × global modifier)`
XP. A non-positive result (e.g. a fully-negated hit) grants nothing. Omit
`scaling` for the default flat reward.

> **Composting:** the `compost` trigger fires when an item is composted into a
> composter and routes to nearby players of the composter, so a farm run by
> anyone present awards XP. Bind a low flat reward to keep a large compost farm
> from flooding the skill. The `fertilize` trigger fires when a player uses
> bonemeal on a block; bind `core:area_fertilize` abilities to it.
>
> **Trading:** the `trade` trigger fires when a player completes a villager
> trade, and the `barter` trigger fires when a piglin barters with a player
> (routed to nearby players). Bind flat rewards to both, and bind the
> `core:trade_bonus`, `core:villager_xp`, and `core:barter_luck` abilities to
> them.

### abilities

Each entry defines an unlockable ability with mechanics.

| Key | Required | Type | Description |
|---|---|---|---|
| `id` | Yes | string | Unique ability identifier |
| `display_name` | No | string | Human-readable name (default: same as `id`) |
| `unlock_level` | No | int | Level required to unlock (default: 1) |
| `trigger` | Yes | string | The event that activates this ability. Each ability must declare exactly one trigger key that determines which event dispatch activates it. See the Triggers table in [capabilities.md](capabilities.md) for valid keys. This field is required and fail-fast validated for an inline (non-referenced) ability. An ability that references a registered id (see below) may omit `trigger` and inherit it from the registered ability. Omitting `trigger` on an inline ability throws `IllegalArgumentException` during skill loading. The trigger must match the mechanic's expected event (see [capabilities.md](capabilities.md) for each mechanic's event) |
| `display` | No | section | UI lore configuration |
| `requirements` | No | section | Pre-execution requirements |
| `on_failure` | No | section | Failure feedback overrides |
| `mechanics` | Yes | list | Executable mechanic actions |
| `feedback` | No | section | Success feedback (particles, sounds, messages) |

#### Reusable Abilities (the `abilities/` folder)

The optional `abilities/` data folder defines reusable abilities. Each `.yml`
file registers one ability by its `id`. The plugin scans the folder
recursively on startup and on `/skills reload`. A file that is not a YAML map
or lacks a non-blank string `id` logs a warning and is skipped. On an id
conflict the first file loaded wins and a warning is logged.

A skill references a registered ability by its `id`. The registered ability
acts as the base. The skill's own fields overwrite the inherited fields
(top-level overwrite only, no deep merge). This lets one field be overridden
per skill while everything else is inherited:

```yaml
abilities:
  - id: "vein_miner"
    unlock_level: 40
```

Here `vein_miner` inherits its `trigger`, `requirements`, `mechanics`, and
`feedback` from `abilities/vein_miner.yml`. The skill's `unlock_level` (40)
overwrites the ability file's value (25).

If a skill references an id that is not registered and the inline definition
is incomplete, parsing fails with a descriptive error. An id-referenced
ability with no other fields is valid only when the id is registered.

#### on_failure

| Key | Type | Description |
|---|---|---|
| `cooldown` | section | Feedback when ability is on cooldown |
| `missing_item` | section | Feedback when item requirements are not met |
| `missing_state` | section | Feedback when player state requirements are not met |
| `insufficient_items` | section | Feedback when items are held but not enough |

Each key supports the same sub-keys as `feedback` (`action_bar`, `sounds`).

#### requirements

| Key | Type | Default | Description |
|---|---|---|---|
| `cooldown` | double *or* evaluator | `0` | Cooldown in seconds between uses. Accepts a plain number or evaluator syntax (see below) |
| `state` | list | `[]` | Required player states |
| `items` | list | `[]` | Item requirements |
| `exhaustion` | section | none | Hunger/food cost for ability activation |

The `cooldown` field accepts full evaluator syntax in addition to a plain double,
enabling inverse-cooldown sub-scaling as the player levels up:

```yaml
requirements:
  cooldown:
    linear: { base: 5.0, step: -0.02, max: 1.0 }
```

A scalar (e.g., `5.0`) is equivalent to `constant: 5.0`. Use `linear` with a
negative `step` for inverse-cooldown sub-scaling. The cooldown shrinks as the
player levels up.

##### exhaustion

| Key | Type | Default | Description |
|---|---|---|---|
| `amount` | double | none | Hunger points to consume on use (0-20) |
| `minimum` | double | none | Minimum food level required to activate, inclusive (0-20). A food level equal to `minimum` passes |

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
| `parameters` | section | Evaluator parameters for the mechanic. Every parameter must be an evaluator block (e.g. `yield_chance: { constant: 2 }`). A scalar value (e.g. `yield_chance: 2`) is rejected at load |

#### feedback

| Key | Type | Description |
|---|---|---|
| `notify.action_bar` | bool | Show action bar message |
| `notify.chat` | bool | Show chat message |
| `notify.message` | string | Message text |
| `particles` | list | Particle effect configurations |
| `sounds` | list | Sound effect configurations |

Particle and sound entries support a `target` field set to `"self"` (played at the player's location) or `"target"` (played at the target entity/location).

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

> **Note:** The YAML configuration key is `milestones` (plural), while the internal evaluator registry key is `milestone` (singular).

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

## Effect & Attribute Parameter Keys

When a mechanic accepts an `effect` parameter (e.g., `core:apply_status`), use a
**namespaced key** like `minecraft:poison` rather than a legacy numeric ID (e.g., `19`).
The same applies to the `attribute` parameter on `core:modify_attribute`
(e.g., `minecraft:movement_speed` instead of `4`). Numeric IDs are deprecated and log
a warning on use. Unknown keys fail fast at load time. See [capabilities.md](capabilities.md)
for the full list of mechanics and their parameters.

## Full Annotated Example

The following skill definition exercises every section of the schema, including the
required `trigger` field on abilities and namespaced effect keys.

```yaml
id: "mining"
max_level: 100
display:
  name: "Mining"
  icon: "minecraft:iron_pickaxe"
  color: "GREEN"
  style: "SEGMENTED_10"
progression: { curve: "polynomial", base_xp: 50, exponent: 2.5 }

xp_sources:
  - trigger: "block_break"
    filters:
      - target: "#c:ores"
      - state: "player_placed:false"
    reward: { constant: 15.0 }

abilities:
  - id: "geologist"
    display_name: "Geologist"
    unlock_level: 1
    trigger: "block_break"              # Required: the event that activates this ability
    display:
      lore: [ "&7Increases raw ore yield by &a{yield_chance}%&7." ]
    mechanics:
      - type: "core:yield_multiplier"
        filters:
          - target: "#c:ores"
          - tool: "#minecraft:pickaxes"
        parameters:
          yield_chance: { linear: { base: 0.5, step: 0.5, max: 50.0 } }
    feedback: { notify: { action_bar: false } }

  - id: "ore_crush"
    display_name: "Ore Crush"
    unlock_level: 30
    trigger: "entity_damage"            # Required: the event that activates this ability
    display:
      lore: [ "&7Your swings apply Slowness to the target." ]
    mechanics:
      - type: "core:apply_status"
        parameters:
          effect: { constant: "minecraft:slowness" }    # Namespaced key (legacy numeric 2 is deprecated)
          duration: { constant: 3.0 }
          amplifier: { constant: 0.0 }
    feedback: { notify: { action_bar: false } }

  - id: "vein_miner"
    display_name: "Vein Miner"
    unlock_level: 15
    trigger: "block_break"              # Required: the event that activates this ability
    display:
      lore:
        - "&7Sneak-mine to break up to &a{chain_limit} &7connected ores."
        - "&7Costs 2 hunger."
        - "&8Requires: Sneaking, pickaxe, 5s cooldown, 3+ hunger."
    requirements:
      # Requirements.cooldown accepts evaluator syntax for inverse sub-scaling:
      #   cooldown: { linear: { base: 5.0, step: -0.02, max: 1.0 } }
      cooldown: 5.0
      state:
        - "is_sneaking"
        # - "equipped_all:#c:heavy_armor"   # Must be wearing full heavy armor
      items:
        - { action: "possession", tag: "#minecraft:pickaxes", slot: "MAIN_HAND" }
      exhaustion: { amount: 2.0, minimum: 3.0 }
    on_failure:
      cooldown: { action_bar: "&eVein Miner cooling down: {time}s" }
    mechanics:
      - type: "core:chain_break"
        parameters:
          chain_limit: { milestones: { 15: 3, 40: 8, 80: 16 } }
    feedback:
      notify: { action_bar: true, chat: false, message: "&b✦ Vein Miner Activated! ✦" }
```

Note that every `abilities[]` entry declares a `trigger` key, and any mechanic
parameters that take a potion effect use namespaced keys.

**Milestone unlocks:** to unlock recipes as a passive one-time milestone, bind a
`core:unlock_recipe` mechanic to the `level_up` trigger with the milestone's
`unlock_level`. The unlock fires at the moment the skill crosses that level, and
the engine re-runs it on player join and after `/skills reload` so a player
already past the milestone is caught up. It is a no-op once the recipe is
already unlocked, so it never re-fires feedback. See the recipe below.

```yaml
abilities:
  - id: "master_blacksmith"
    display_name: "Master Blacksmith"
    unlock_level: 50
    trigger: "level_up"                  # The milestone moment
    display:
      lore: [ "&7Learn to craft the netherite pickaxe." ]
    mechanics:
      - type: "core:unlock_recipe"
        parameters:
          recipe: { constant: "minecraft:netherite_pickaxe" }
    feedback: { notify: { action_bar: false } }
```

**Persistent attributes (max hearts and similar):** to grow a player's max
hearts (or any attribute) as the skill levels, bind a `core:persistent_attribute`
mechanic to the `level_up` trigger. The engine re-evaluates the level-scaled
`amount` at the player's current level on join, reload, online `setlevel`, and
`reset`, and strips the bonus when the level drops below `unlock_level`. The
`uuid` must be a stable constant so re-applies replace the modifier instead of
stacking. `minecraft:max_health` is in half-hearts: `20.0` is the vanilla 10
hearts, so `amount: 25.0` at level 100 is an extra 12.5 hearts.

```yaml
abilities:
  - id: "wildborn"
    display_name: "Wildborn"
    unlock_level: 1
    trigger: "level_up"                  # Re-evaluated on every level change
    display:
      lore: [ "&7Wildborn vitality: +&a{amount}&7 max health." ]
    mechanics:
      - type: "core:persistent_attribute"
        parameters:
          attribute: { constant: "minecraft:max_health" }
          amount: { linear: { base: 0.0, step: 0.25, max: 25.0 } }
          uuid: { constant: "7f1c6e2a-9b4d-4a6f-b8e2-3d5a0c1f9e77" }
    feedback: { notify: { action_bar: false } }
```

**Environmental damage:** bind XP sources and abilities to specific damage
causes with the `cause` state filter on the `entity_damage_taken` trigger.
`burn` matches fire, fire ticks, and lava; `fire`, `lava`, `drowning`,
`suffocation`, `cactus`, and `starvation` each match their single cause. The
filter fails closed on any other event, so a `cause` reference is only
meaningful on `entity_damage_taken`. Damage-scaled rewards use
`scaling: damage`.

```yaml
xp_sources:
  - trigger: "entity_damage_taken"
    filters: [ { state: "cause:burn" } ]
    reward: { constant: 14.0 }
    scaling: damage
  - trigger: "entity_damage_taken"
    filters: [ { state: "cause:drowning" } ]
    reward: { constant: 18.0 }
    scaling: damage

abilities:
  - id: "endure"
    display_name: "Endure"
    unlock_level: 15
    trigger: "entity_damage_taken"
    mechanics:
      - type: "core:cancel_damage"
        filters: [ { state: "cause:burn" } ]
        parameters:
          chance: { linear: { base: 10.0, step: 0.35, max: 40.0 } }
```

**Exploration and camping triggers:** the `chunk_load` trigger fires when a
player explores freshly generated terrain (`isNewChunk()`), `sleep` fires when a
player passes the night, and `sprint` combined with the `is_in_water` state
filter fires when a player starts swimming. These pair with campfire-themed
XP sources: `player_interact` on `#minecraft:campfires` with a
`#c:campfire_foods` tool places food to cook, and `craft_item` filtered on
`#c:campfires` rewards crafting campfires.

**Ability lore convention:** any ability with a `requirements:` block must surface
its costs and conditions in its lore so players see them before using the ability.
A `&7Costs` line lists what the ability consumes (exhaustion hunger, `cost` items).
An `&8Requires` line lists the activation conditions (states, held items,
cooldown, minimum hunger). Keep both in sync with the YAML.

**Placeholder resolution:** ability lore placeholders resolve against the
union of that ability's mechanic parameter keys. For example, a
`core:yield_multiplier` mechanic with a `yield_chance` parameter makes
`{yield_chance}` available in that ability's lore, injecting the live evaluator
output for the player's current level. Only skill-level lore supports
`{level}`, `{max_level}`, `{skill_name}`, and `{xp}`. A placeholder that does
not match any available key is rejected at load time (fail-fast), so a typo or
stale token fails the skill file parse instead of rendering raw in the tooltip.

## Built-In Mechanics

See [capabilities.md](capabilities.md) for a full catalog of available mechanics, triggers, and evaluators.
