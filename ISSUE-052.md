# `SkillingAPI.getProfile(UUID)` returns `CompletableFuture` but performs a synchronous lookup

## Issue

`SkillingAPI.getProfile(UUID)` (lines 103-106) returns a `CompletableFuture<PlayerProfile>` but the implementation is a synchronous cache lookup wrapped in `CompletableFuture.completedFuture()`:

```java
public CompletableFuture<PlayerProfile> getProfile(UUID playerId) {
    PlayerProfile profile = profileManager.getProfile(playerId);
    return CompletableFuture.completedFuture(profile);
}
```

This violates the principle of least astonishment — callers expect a truly asynchronous operation (e.g., loading from DB), but get a synchronous method wrapped in a future. The method signature suggests async behavior but provides none.

**ISSUES.md reference:** Line 308

## Root Cause

The method was designed with async semantics in the return type but implemented as a synchronous cache lookup. The `docs/api-integration.md` already documents this discrepancy (ISSUES.md line 199), but the API itself should match its contract.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `SkillingAPI.java` | `api/SkillingAPI.java` | 103-106 |

## Development Plan

### Step 1: Option A — Return `PlayerProfile` directly

Change the return type to `PlayerProfile` (nullable):

```java
public PlayerProfile getProfile(UUID playerId) {
    return profileManager.getProfile(playerId);
}
```

This is honest — it's a synchronous cache lookup. Callers who need async can wrap it themselves.

### Step 2: Option B — Make it truly async

Change to delegate to `profileManager.loadProfile()` which is truly async:

```java
public CompletableFuture<PlayerProfile> getProfile(UUID playerId) {
    PlayerProfile cached = profileManager.getProfile(playerId);
    if (cached != null) {
        return CompletableFuture.completedFuture(cached);
    }
    return profileManager.loadProfile(playerId);
}
```

### Step 3: Choose approach

Option A is simpler and honest. Option B is more useful for addon developers who need to guarantee a loaded profile.

### Step 4: Update `docs/api-integration.md`

Document whichever approach is chosen.

## Self-Review

- The current implementation is misleading — wrapping a sync call in a future adds no value
- Option A is the minimal fix (change return type, remove `CompletableFuture` wrapper)
- Option B adds real async behavior for the uncached case
- Either choice is better than the current misleading signature
- This is a public API change — may break addons that depend on the `CompletableFuture` return type
