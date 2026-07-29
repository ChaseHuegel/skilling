# Cooldown entries in `RequirementEngine` are never cleaned up on player quit

## Issue

`RequirementEngine.cooldowns` is a `Map<String, Map<String, Long>>` keyed by player UUID → ability ID → expiry timestamp. When a player quits, `clearCooldowns()` is never called — the map entry for that UUID persists forever. This causes unbounded map growth on servers with many unique players.

**ISSUES.md reference:** Line 125

## Root Cause

`RequirementEngine.clearCooldowns(Player)` exists (line 192) and removes the player's entry, but it's never called from `PlayerListener.onPlayerQuit()` (or anywhere else). The cooldown map grows monotonically with each unique player that connects.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `RequirementEngine.java` | `engine/requirements/RequirementEngine.java` | 24, 192-194 |
| `PlayerListener.java` | `engine/listener/PlayerListener.java` | 32-47 (onPlayerQuit, needs cleanup call) |

## Development Plan

### Step 1: Wire cleanup into player quit flow

In `PlayerListener.onPlayerQuit()` (async branch or sync branch), call `Skilling.getInstance().getRequirementEngine().clearCooldowns(player)` before unloading the profile:

```java
@EventHandler(priority = EventPriority.MONITOR)
public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
    if (profile != null && profile.isDirty()) {
        CompletableFuture.runAsync(() -> {
            asyncBatchWorker.flushDirtyProfiles();
        }).whenComplete((v, ex) -> {
            if (ex != null) { ... }
            profileManager.unloadProfile(player.getUniqueId());
        });
    } else {
        profileManager.unloadProfile(player.getUniqueId());
    }
    // Clean up cooldowns
    Skilling.getInstance().getRequirementEngine().clearCooldowns(player);
}
```

### Step 2: Verify

- Player activates ability with cooldown → cooldown tracked
- Player quits → cooldown entry removed
- `cooldowns` map should not contain entry for quit player's UUID

## Self-Review

- `clearCooldowns` already exists and is safe to call at any time
- The call should happen after the profile flush but can happen outside the async block
- Runs on main thread (PlayerQuitEvent fires on main thread) — `clearCooldowns` does a single `ConcurrentHashMap.remove()`, which is thread-safe
