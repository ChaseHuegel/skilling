# `XpBonusMechanic` is a no-op stub — returns `true` with no actual effect

## Issue

`XpBonusMechanic` reads the `multiplier` parameter and returns `true` if the multiplier is positive, but **never applies any XP multiplier** to anything. The mechanic is registered as `core:xp_bonus` and accepts a `multiplier` parameter, but it's a stub with no implementation.

**ISSUES.md reference:** Line 122

## Root Cause

The `execute()` method (lines 16-18):
```java
public boolean execute(Player player, Map<String, Object> params, Event event) {
    double mult = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
    return mult > 0;
}
```

This parses the multiplier, checks it's positive, and returns `true` — but never actually modifies XP gain. The calling code in `SkillEventListener.fireAbilities()` does not pass any XP-related event data to the mechanic, and the mechanic has no way to hook into the `grantXp()` path.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `XpBonusMechanic.java` | `engine/mechanic/impl/XpBonusMechanic.java` | 14-19 |
| `SkillEventListener.java` | `engine/listener/SkillEventListener.java` | 286-314 (XP grant path that needs hooking) |

## Development Plan

### Step 1: Design the integration approach

The XP grant path in `SkillEventListener.grantXp()` iterates skills and XP sources, computes the reward, and calls `profile.addXp()`. The `XpBonusMechanic` needs to modify the XP amount before it's added. Options:

**Option A: Per-player multiplier via profile**
- Add an `xpMultiplier` field to `PlayerProfile` (default `1.0`)
- `XpBonusMechanic.execute()` sets `profile.setXpMultiplier(multiplier)` for a duration or `multiplier`-based duration
- `grantXp()` reads `profile.getXpMultiplier()` and applies it to the reward

**Option B: Event-based modifier**
- Add a cancellable `SkillingXpGrantEvent` that mechanics can listen for
- `XpBonusMechanic` stores a map of player → multiplier and applies it via the event

**Option C: Synchronous hook**
- Store a `Map<UUID, Double>` in `XpBonusMechanic` (or a shared context)
- In `grantXp()`, check if the current player has an active XP multiplier
- `XpBonusMechanic.execute()` stores/updates the multiplier with a timeout

### Step 2: Implement Option C (simplest route)

```java
// In XpBonusMechanic:
private final Map<UUID, Double> multipliers = new ConcurrentHashMap<>();

public boolean execute(Player player, Map<String, Object> params, Event event) {
    double mult = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
    if (mult <= 0) return false;
    multipliers.put(player.getUniqueId(), mult);
    return true;
}

public double getMultiplier(UUID playerId) {
    return multipliers.getOrDefault(playerId, 1.0);
}

public void clear(UUID playerId) {
    multipliers.remove(playerId);
}
```

### Step 3: Hook into `grantXp()`

In `SkillEventListener.grantXp()`, after computing the reward:
```java
double xp = source.reward().evaluate(oldLevel, 1) * plugin.getGlobalXpModifier();
// Apply XP bonus from XpBonusMechanic
xp *= xpBonusMechanic.getMultiplier(player.getUniqueId());
```

The `XpBonusMechanic` instance needs to be accessible from `SkillEventListener`. Either inject it at construction or retrieve it from the mechanic registry.

### Step 4: Register the instance

In `Skilling.registerBuiltins()`, store the `XpBonusMechanic` instance as a field so `SkillEventListener` can access it, or pass it via the registry.

## Self-Review

- This is a non-trivial feature: the mechanic must interact with the XP grant pipeline outside the normal ability execution flow
- Option C is the least invasive — no new events, no profile schema changes
- The multiplier persists for the player's session; a future enhancement could add duration-based expiry
- Tests should verify that `getMultiplier()` returns the expected value after `execute()`, and that `grantXp()` applies it
