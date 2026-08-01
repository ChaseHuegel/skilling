# ISSUE-102: Prevent attribute-modifier abilities from stacking with themselves on repeated activation

**Status:** Open
**Type:** Bug
**Severity:** Medium (stat inflation and unintended stacking when an ability fires repeatedly)

---

## Context & User Story

- **Goal:** As a player, I want an ability that temporarily modifies an attribute to refresh its effect when re-triggered rather than stacking a new modifier on top of the old one, so that repeated activations do not inflate my stats beyond the configured value.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Add an optional `uuid` string parameter to every attribute-modifier mechanic; when absent, generate a random UUID (preserving current behavior)
- [x] When a `uuid` is provided, check whether a modifier with that UUID already exists on the player's attribute instance; if it does, remove it before adding the new one (refreshing any duration)
- [x] Extract the parse/check/remove/re-add logic into a shared helper to avoid duplicating it across all six mechanics
- [x] Add the `uuid` parameter to each bundled skill ability that uses an attribute mechanic, using a stable UUID string per ability
- [x] Add unit tests covering: no-`uuid` behavior (random modifier, unchanged), re-trigger with same `uuid` (replace, no stacking), and different `uuid` values (independent modifiers may coexist)

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyAttributeMechanic.java` (`skilling_modifier`, lines 57-69)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyAttackSpeedMechanic.java` (`skilling_attack_speed`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/SpeedBonusMechanic.java` (`skilling_speed`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/KnockbackResistMechanic.java` (`skilling_knockback`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ArmorBonusMechanic.java` (`skilling_armor_bonus`, applied to both `ARMOR` and `ARMOR_TOUGHNESS`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyJumpMechanic.java` (`skilling_jump`)
  - Bundled skill YAML under `src/main/resources/skills/`
- **Dependencies:** All six mechanics use `AttributeModifier(UUID.randomUUID(), name, amount, ADD_NUMBER)` + `addTransientModifier`; removal is handled by a delayed task. Current parameter docs are in each mechanic's class Javadoc (per `src/AGENTS.md` §7). Registry registration lives in `Skilling.java` (`core:modify_attribute` etc.).
- **Constraints:** Fail-fast on malformed configs (invalid UUID string → `IllegalArgumentException` at parse). Javadoc required on new public helpers. No hardcoded abilities — the mechanics stay generic; only the bundled YAML gains explicit `uuid` values.

### Root Cause

Every attribute-modifier mechanic builds a `new AttributeModifier(UUID.randomUUID(), ...)` on each execution. There is no check against the player's attribute instance for an existing modifier, so every activation appends another transient modifier until that activation's own delay expires. Rapid or repeated triggers (e.g. a passive `speed_bonus` re-firing on every relevant event) accumulate unbounded stat bonuses.

### Proposed Fix

Add an optional `uuid` parameter to each mechanic. At execution:

1. If `uuid` is absent → keep `UUID.randomUUID()` (backward compatible).
2. If `uuid` is present → `AttributeInstance.getModifier(uuid)`; if non-null, call `instance.removeModifier(...)` before `addTransientModifier(...)`, then schedule the delayed removal exactly as today.

The six mechanics share this shape, so implement one helper (e.g. `AttributeModifierHelper` or a static method) to avoid six divergent copies. Then add a stable `uuid` string to each bundled ability using an attribute mechanic (~30 entries across ~20 skills).

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new unit tests
- [x] Unit test: re-triggering an ability with the same `uuid` results in a single active modifier (no stacking)
- [x] Unit test: no `uuid` provided behaves exactly as before (fresh random modifier each activation)
- [x] Unit test: two different `uuid` values on the same attribute can coexist
- [x] Edge case handled: malformed `uuid` string in YAML fails fast with a clear `IllegalArgumentException` at parse time
- [x] Runtime smoke check: repeatedly triggering a bundled ability (e.g. `speed_bonus`) shows the attribute returning to base after the configured duration, with no growth over repeated activations
