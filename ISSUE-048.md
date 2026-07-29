# YAML config key literals scattered across `Skilling.java` — extract as `private static final` constants

## Issue

`Skilling.java` uses raw string literals for YAML config keys in multiple places:

- Line 101: `config.getBoolean("debug_logging", false)`
- Line 105: `config.getInt("titles.stay_duration", 5000)`
- Line 106: `config.getDouble("global_xp_modifier", 1.0)`
- Line 147: `config.getInt("debouncer.interval_ms", 500)`
- Line 149: `config.getInt("bossbar.max_active", 2)`
- Line 150: `config.getInt("bossbar.fade_ticks", 40)`

These are scattered through `onEnable()` and `reloadConfigSettings()`. If a config key changes, it must be updated in multiple places. Extract them as `private static final` constants.

**ISSUES.md reference:** Line 304

## Root Cause

Config keys were inlined during development without being centralized.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `Skilling.java` | `Skilling.java` | 101, 105, 106, 147, 149, 150, 348-350 |

## Development Plan

### Step 1: Extract constants

```java
private static final String CONFIG_DEBUG_LOGGING = "debug_logging";
private static final String CONFIG_TITLES_STAY_DURATION = "titles.stay_duration";
private static final String CONFIG_GLOBAL_XP_MODIFIER = "global_xp_modifier";
private static final String CONFIG_DEBOUNCER_INTERVAL_MS = "debouncer.interval_ms";
private static final String CONFIG_BOSSBAR_MAX_ACTIVE = "bossbar.max_active";
private static final String CONFIG_BOSSBAR_FADE_TICKS = "bossbar.fade_ticks";
```

### Step 2: Replace inline strings

Replace `config.getBoolean("debug_logging", false)` with `config.getBoolean(CONFIG_DEBUG_LOGGING, false)`, etc. Apply to both `onEnable()` and `reloadConfigSettings()`.

## Self-Review

- Mechanical refactor — no behavior change
- Prevents typos and makes key changes centralized
- Follows standard Java constant pattern
- Config keys are also documented in `docs/configuration.md` — the constants provide a single source of truth
