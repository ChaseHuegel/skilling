# `CustomTagLoader` does not log a warning when circular tag references are detected

## Issue

In `CustomTagLoader.resolve()` (line 66), circular references are silently handled:

```java
if (!resolving.add(key)) return EnumSet.noneOf(Material.class);
```

When a circular reference is detected (e.g., `#c:a → #c:b → #c:a`), the method returns an empty set without any warning. This makes circular tag definitions invisible to the skill designer — they silently get empty tag resolutions.

**ISSUES.md reference:** Line 135

## Root Cause

The `resolving` set guards against infinite recursion, but the silent return of an empty set provides no feedback that a circular reference exists.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `CustomTagLoader.java` | `engine/tag/CustomTagLoader.java` | 62-75 |

## Development Plan

### Step 1: Add warning on circular reference

```java
if (!resolving.add(key)) {
    Skilling.getInstance().getLogger().warning(
        "Circular tag reference detected in custom tag '#c:" + key + "' — " +
        "resolving to empty set to prevent infinite loop. " +
        "Resolving chain: " + resolving + " → " + key
    );
    return EnumSet.noneOf(Material.class);
}
```

### Step 2: Consider tracking the chain for better diagnostics

The `resolving` set can be passed through or a `Deque<String>` could track the full chain for a more informative error message. However, logging the current set gives enough context to debug.

### Step 3: Verify

- Create `#c:a: [#c:b]` and `#c:b: [#c:a]` in tags.yml
- Load the plugin — warning should be logged with the circular chain
- `#c:a` should resolve to empty set

## Self-Review

- Minimal change (one additional log line in an existing branch)
- Greatly improves debuggability for tag configuration errors
- No behavioral change — empty set is still returned
