# `LoreResolver` silently passes through unresolved placeholders as raw `{placeholder}` text

## Issue

In `LoreResolver.resolve()` (line 52), when a placeholder token is not found in the evaluators map, it passes through as raw text:

```java
} else {
    replacement = "{" + placeholder + "}";
}
```

This means a typo like `{yield_chace}` instead of `{yield_chance}` silently displays raw placeholder text in the UI instead of warning the skill designer. This makes configuration errors invisible.

**ISSUES.md reference:** Line 134

## Root Cause

The else branch reconstructs the original placeholder text instead of logging a warning or throwing. The method returns the unresolved string, which is displayed as-is in the lore.

## Affected Files

| File | Path | Lines |
|------|------|-------|
| `LoreResolver.java` | `engine/ui/LoreResolver.java` | 51-53 |

## Development Plan

### Step 1: Log a warning for unresolved placeholders

```java
} else {
    Skilling.getInstance().getLogger().warning(
        "Unresolved lore placeholder: {" + placeholder + "}"
    );
    replacement = "{" + placeholder + "}";
}
```

### Step 2: (Optional) Fail-fast during development

In debug mode, throw an `IllegalArgumentException` to catch errors immediately:

```java
} else if (plugin.isDebugLogging()) {
    throw new IllegalArgumentException("Unresolved lore placeholder: {" + placeholder + "}");
}
```

### Step 3: Verify

- Skill with valid `{yield_chance}` → resolves correctly
- Skill with typo `{yiel_chance}` → warning in console, raw `{yiel_chance}` in lore

## Self-Review

- Warnings are non-breaking and visible in console — helps skill authors debug
- Debug-mode exception is optional but recommended for development
- No performance impact — placeholders are resolved during UI construction, not in hot paths
