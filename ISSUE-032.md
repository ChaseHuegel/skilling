# `Skilling.titleStayDuration` and `globalXpModifier` are not `volatile` — stale values possible after reload

## Issue

`Skilling` fields `titleStayDuration` (line 71) and `globalXpModifier` (line 72) are plain `int`/`double` fields without `volatile`. They are written in `reloadConfigSettings()` (line 345) during a reload and read from gameplay code potentially running on other threads (e.g., async XP processing, title display). Without `volatile`, there is no happens-before guarantee, and a reading thread may see stale values after reload.

**ISSUES.md reference:** Line 127

## Root Cause

`reloadConfigSettings()` updates the values but other threads may have cached the old values in CPU-local cache. The `reloading` and `debugLogging` fields ARE `volatile` (lines 69-70), but `titleStayDuration` and `globalXpModifier` are not.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `Skilling.java` | `Skilling.java` | 71-72, 345-351 |

## Development Plan

### Step 1: Add `volatile` to both fields

```java
private volatile int titleStayDuration;
private volatile double globalXpModifier;
```

### Step 2: Verify

- Run `/skills reload` and confirm new values take effect immediately
- No performance impact — `volatile` on `int`/`double` is cheap

## Self-Review

- Trivial fix (two `volatile` keywords)
- Consistent with the existing `volatile` pattern on `reloading` and `debugLogging`
- No behavioral change — just correctness of visibility
