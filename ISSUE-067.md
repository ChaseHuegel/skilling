# Proposal: Additional Filter States

## Motivation

Currently, ability and XP source filters support only 5 player states: `is_sneaking`, `is_sprinting`, `is_in_water`, `is_on_ground`, and `player_placed:false`. Adding more contextual state checks enables richer ability designs — e.g., an ability that only works at night, in the Nether, or when the player is low on health — while staying within the framework's existing "Check, Execute, Consume" pattern.

## Design Constraints

All proposed states must be implementable using **standard Paper API calls only** — no NMS, no per-tick runnables, no packet modification.

## Proposed States

### 1. Dimension Checks

```
dimension:overworld
dimension:nether
dimension:end
```

- **API:** `player.getWorld().getEnvironment()`
- **Returns:** `World.Environment` enum (`NORMAL`, `NETHER`, `THE_END`)
- **Use case:** Abilities that work only in specific dimensions, e.g., a Nether-only mining buff.

### 2. Light Level

```
light_level:below:7
light_level:above:7
light_level:exactly:0
```

- **API:** `player.getLocation().getBlock().getLightLevel()`
- **Returns:** int 0–15
- **Parsing:** Split the value on `:` — form is `light_level:{comparator}:{threshold}` where comparator is `above`, `below`, or `exactly`.
- **Use case:** Mob-fighting abilities that only work in darkness, or mining buffs that work in well-lit caves.

### 3. Biome Tag

```
biome:#minecraft:is_forest
biome:minecraft:plains
```

- **API:** `player.getLocation().getBlock().getBiome()` returns `Biome` enum. `Biome` has `BiomeTag` support via `Registry.BIOME.get(key).asString()` and `Tag<Biome>`.
- **Parsing:** If prefixed with `#`, resolve via `Tag<Biome>` (Paper's `Tags` class). Otherwise, match the biome name directly.
- **Use case:** Regional abilities — e.g., faster digging in deserts, better fishing in oceans.

### 4. Weather

```
weather:clear
weather:rain
weather:thunder
```

- **API:** `player.getWorld().isClearWeather()`, `hasStorm()`, `isThundering()`
- **Returns:** boolean checks on world weather state.
- **Use case:** Abilities that are stronger in rain, or fishing bonuses during thunderstorms.

### 5. Health Threshold

```
health:below:50%
health:above:75%
```

- **API:** `player.getHealth() / player.getMaxHealth() * 100`
- **Parsing:** `health:{comparator}:{percentage}%` — comparator is `above` or `below`.
- **Use case:** "Last stand" damage boosts when low health, or defensive buffs triggered above a threshold.

### 6. Hunger Threshold

```
hunger:below:6
hunger:above:15
```

- **API:** `player.getFoodLevel()` (0–20)
- **Parsing:** `hunger:{comparator}:{value}` — comparator is `above` or `below`.
- **Use case:** Abilities that consume hunger or only work when well-fed.

### 7. On Fire

```
is_on_fire
```

- **API:** `player.getFireTicks() > 0`
- **Returns:** boolean
- **Use case:** Damage bonuses when the player is burning (risky playstyle).

### 8. Is Riding

```
is_riding
```

- **API:** `player.isInsideVehicle()`
- **Returns:** boolean
- **Use case:** Abilities that trigger while mounted (horse archery, lance damage).

### 9. Time of Day

```
time:day
time:night
```

- **API:** `player.getWorld().getTime()` — day is 0–12300 (ticks), night is 13000–23900.
- **Returns:** boolean based on tick range.
- **Use case:** Night-only stealth/combat abilities, day-only farming buffs.

### 10. Target Entity Type (for damage events)

```
target_type:#minecraft:zombies
target_type:minecraft:creeper
```

- **API:** Cast event to `EntityDamageByEntityEvent`, check `event.getEntity().getType()` against tag or material.
- **Parsing:** Same as `target` filter in XP sources — supports `#` tag prefix or direct entity type name.
- **Use case:** Species-specific damage bonuses (e.g., "undead slayer" for skeletons/zombies).

## Implementation Plan

### Phase 1: Core Engine (`matchFilter`)

Add new `case` entries to the switch expression in `SkillEventListener.matchFilter()` (line 462). Each new state follows the same pattern: parse the filter string, run the Paper API check, return boolean.

States with comparators (`above`/`below`/`exactly`) parse the filter string by splitting on `:` and extracting the comparator + threshold. Invalid formats fall through to `default -> true`.

### Phase 2: FilterDTO + Serialization (if needed)

No changes needed — the `Filter` record already stores `state` as a free-form string. The parsing is entirely inside `matchFilter`.

### Phase 3: Frontend State Options

Update `STATE_OPTIONS` in `AbilitiesSection.vue` (line 178) to include the new states. States with comparators need special UI treatment — either a secondary input for the threshold value, or a combined string like `"light_level:below:7"` entered as a single option with an appended input.

For the initial implementation, I recommend a simpler approach:
- Add boolean states (`is_on_fire`, `is_riding`, `time:day`, `time:night`, `dimension:*`, `weather:*`) directly to `STATE_OPTIONS`.
- Add parameterized states (`light_level:*`, `health:*`, `hunger:*`, `biome:*`, `target_type:*`) as a follow-up phase with proper combobox/input widgets.

### Phase 4: Documentation

Update `docs/creating-skills.md` filter section and `docs/capabilities.md` triggers section with the new state options.

## Backward Compatibility

All existing `state` values continue to work unchanged. New states are additive. The `default -> true` fallback in the switch expression ensures unknown state strings are silently ignored (existing behavior).

## Files to Modify

| File | Change |
|---|---|
| `src/.../listener/SkillEventListener.java` | Add switch cases in `matchFilter()` |
| `web/.../AbilitiesSection.vue` | Add entries to `STATE_OPTIONS` |
| `docs/creating-skills.md` | Document new states |
| `docs/capabilities.md` | Document new states |

## Out of Scope

- Dynamic player properties (inventory contents, team color, scoreboard tags) — these add complexity with minimal gameplay gain.
- Event-specific state (e.g., block hardness, projectile velocity) — these couple the state system too tightly to specific event types.
