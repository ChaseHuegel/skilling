# `PlayerProfile.getXpMap()` exposes mutable `ConcurrentHashMap` to `AsyncBatchWorker` while `addXp()` concurrently modifies it

## Issue

`PlayerProfile.getXpMap()` returns the raw `ConcurrentHashMap` reference (line 139). The `AsyncBatchWorker.doFlush()` iterates this map via `profile.getXpMap().entrySet()` (line 80) to read XP values. Meanwhile, the main thread may be calling `addXp()` or `setXp()` on the same profile, which modifies the map via `merge()` or `put()`. While `ConcurrentHashMap` guarantees safe per-operation visibility, the batch worker iterates without a snapshot — it may see an inconsistent view of entries (e.g., missing a newly added skill, or reading a value mid-merge).

Additionally, the profile's `modCount` is incremented by `addXp()`/`setXp()`, but the batch worker reads `modCount` AFTER iterating the map. If a concurrent modification changes the map between iteration and `markSaved()`, the `markSaved()` snapshots a modCount that reflects the concurrent change, potentially causing a lost update on the next cycle.

**ISSUES.md reference:** Line 126

## Root Cause

`AsyncBatchWorker.doFlush()` iterates `profile.getXpMap().entrySet()` directly without synchronization or snapshot. The `getXpMap()` method returns the raw reference to a live map being modified from the main thread.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `PlayerProfile.java` | `engine/profile/PlayerProfile.java` | 139-141 (getXpMap exposes mutable map) |
| `AsyncBatchWorker.java` | `engine/db/AsyncBatchWorker.java` | 80 (iteration) |

## Development Plan

### Step 1: Add a snapshot method to `PlayerProfile`

Replace raw `getXpMap()` exposure with a snapshot copy:

```java
public Map<String, Long> getXpSnapshot() {
    return new HashMap<>(xpMap);
}
```

Update `AsyncBatchWorker.doFlush()` to use `profile.getXpSnapshot()` instead of `profile.getXpMap()`.

### Step 2: Preserve `getXpMap()` for initial hydration

The `ProfileManager.loadProfile()` uses `profile.getXpMap().put()` for initial DB loading. This is safe because no concurrent access exists during profile creation. Keep `getXpMap()` but mark it `@Deprecated` with a Javadoc warning, or make it package-private.

Alternatively, add a bulk-load method: `profile.loadXp(Map<String, Long> data)` that copies the data without exposing the internal map.

### Step 3: Fix modCount race

In `AsyncBatchWorker.doFlush()`, capture the modCount before the iteration and only call `markSaved()` if nothing changed during the flush:

```java
// Not directly fixable via simple approach — the true fix is to take the snapshot
// and only mark saved if the modCount hasn't changed since snapshot was taken:
for (var entry : dirty.entrySet()) {
    UUID uuid = entry.getKey();
    PlayerProfile profile = entry.getValue();
    Map<String, Long> xpSnapshot = profile.getXpSnapshot();
    for (var xpEntry : xpSnapshot.entrySet()) {
        // write to batch
    }
}
// After successful batch:
for (var entry : dirty.entrySet()) {
    entry.getValue().markSaved();
}
```

The snapshot eliminates the inconsistent-iteration risk. The `markSaved()` call after the batch is fine — if new XP arrives between snapshot and markSaved, the profile will be dirty again on the next cycle.

## Self-Review

- The snapshot is a lightweight `HashMap` copy — O(n) where n is skill count per player (typically ≤ 20)
- Eliminates the inconsistent iteration issue
- The `modCount` race is a separate concern but partially mitigated by the snapshot
- `getXpMap()` kept for hydration but should be marked with deprecation warning
