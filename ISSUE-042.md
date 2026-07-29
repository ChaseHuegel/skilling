# `PlayerListener.onPlayerQuit` has a race window — profile modified after flush but before unload

## Issue

`PlayerListener.onPlayerQuit()` (lines 32-47) performs an async flush followed by `profileManager.unloadProfile()` in the `whenComplete` callback. There's a race window between the flush completing and the profile being removed from the cache: a concurrent thread (e.g., from `AsyncBatchWorker`'s periodic run or an event handler) could modify the profile between these two operations. The modification would be lost because:

1. The flush already completed (captured stale data)
2. `unloadProfile` removes the profile from the cache
3. The concurrent modification is written to the in-memory profile but never saved

**ISSUES.md reference:** Line 137

## Root Cause

The async flush and unload are not atomic with respect to concurrent profile modifications. The `onPlayerQuit` handler:
1. Reads `profile.isDirty()` on the main thread (line 35)
2. Schedules an async `flushDirtyProfiles()` (line 36-38)
3. In the callback, calls `unloadProfile()` (line 42)

Between steps 2 and 3, another thread could modify the profile.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `PlayerListener.java` | `engine/listener/PlayerListener.java` | 32-47 |

## Development Plan

### Step 1: Use `setReloading`-style freeze per player

Before the async flush, mark the profile as "quitting" to prevent further modifications. Add a `volatile boolean quitting` flag to `PlayerProfile`:

```java
// In PlayerProfile:
private volatile boolean quitting;

public boolean isQuitting() { return quitting; }
public void setQuitting(boolean q) { this.quitting = q; }
```

### Step 2: Guard modifications

In all places that modify the profile (event handlers, `SkillsCommand`, etc.), check:

```java
if (profile.isQuitting()) return;  // or log a warning
```

### Step 3: Set quitting flag before flush

In `onPlayerQuit`:

```java
if (profile != null) {
    profile.setQuitting(true);
    if (profile.isDirty()) {
        CompletableFuture.runAsync(() -> {
            asyncBatchWorker.flushDirtyProfiles();
        }).whenComplete((v, ex) -> {
            if (ex != null) { ... }
            profileManager.unloadProfile(player.getUniqueId());
        });
    } else {
        profileManager.unloadProfile(player.getUniqueId());
    }
}
```

### Alternative Approach: Synchronous flush on main thread

Since `flushDirtyProfiles()` uses `ReentrantLock` internally and the periodic async task also uses `tryLock()`, flush synchronously on quit:

```java
if (profile != null && profile.isDirty()) {
    asyncBatchWorker.flushDirtyProfiles();  // synchronous on main thread
}
profileManager.unloadProfile(player.getUniqueId());
```

This eliminates the race window entirely — no async gap. The lock in `AsyncBatchWorker` prevents concurrent flushes.

### Step 4: Choose approach

- **Alternative (sync flush)** is simpler and eliminates the race completely
- **Step 1-3 (quitting flag)** is more defensive but adds complexity
- The sync flush approach is preferred: `flushDirtyProfiles()` is already designed to be callable synchronously (used in `LockdownManager.reload()`), and player quit happens rarely enough that the main thread impact is negligible

## Self-Review

- Switching to a sync flush on quit is the cleanest fix
- The current async approach with `CompletableFuture` was likely an overcorrection — main thread pauses during logout are acceptable (vanilla Minecraft does DB writes sync on logout)
- The `setQuitting` flag approach is more defensive but adds state complexity
- Recommend: sync flush + remove CompletableFuture wrapper
