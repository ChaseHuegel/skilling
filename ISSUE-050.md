# 6 trigger implementations have grammatically incorrect Javadocs

## Issue

Six trigger classes have Javadocs with grammatical errors in their descriptions. Examples:
- `RideHorseTrigger`: "Trigger fired when a player ride horse." → "Triggers when a player mounts a vehicle."
- `LevelUpTrigger`: "Trigger fired when a player level up." → "Triggers when a player levels up."
- Other affected triggers: `collect_xp`, `enchant_item`, and others with similar patterns.

**ISSUES.md reference:** Line 306

## Root Cause

Javadocs were written hastily during initial implementation without proofreading.

## Affected Files

| File | Path |
|------|------|
| Various trigger implementations | `engine/trigger/impl/*.java` |

## Development Plan

### Step 1: Identify all 6 triggers with bad Javadocs

Check each trigger's class-level Javadoc for grammatical correctness. Common issues:
- Third-person singular missing 's' ("ride" → "rides", "level up" → "levels up")
- Incorrect article usage
- Missing verb tense agreement

### Step 2: Fix Javadocs

Rewrite to follow a consistent format: "Triggers when [description of event]." Examples:
- "Triggers when a player mounts a vehicle."
- "Triggers when a player collects experience orbs."
- "Triggers when a player enchants an item."

### Step 3: Verify

- `./gradlew javadoc` (if configured) should pass
- No code changes — Javadoc only

## Self-Review

- Documentation-only fix
- Improves professionalism of the codebase
- Establish a consistent Javadoc template for all triggers: `"Triggers when {actor} {action}."`
