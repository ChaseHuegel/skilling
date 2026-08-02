# ISSUE-140: Harden the registry API surface (typed generics, defensive copies, immutable views)

**Status:** Open
**Type:** Improvement
**Severity:** Medium (untyped `Object`/raw `Class<?>` surface and live collection views)

---

## Context & User Story

- **Goal:** As an addon developer, I want a type-safe registry API that fails at registration (not first use) and never lets me mutate or corrupt registry state through returned collections.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Type `MechanicRegistry.register(String, Class<? extends SkillMechanic>)` and `create(String)` returns `SkillMechanic` (or a documented wrapper) instead of raw `Class<?>` / `Object`
- [ ] Type `TriggerRegistry` and `EvaluatorRegistry` similarly (or add typed typed-registry accessors alongside the untyped ones, marked for removal)
- [ ] Validate at registration that the class has a public no-arg constructor (fail at register, not first use)
- [ ] Return defensive copies / immutable views from `keys()` (use `Set.copyOf`) and `getAllParameterNames()` (already copies; extend to all exposures)
- [ ] Store defensive copies of caller-supplied `paramNames` lists
- [ ] Add unit tests covering: invalid registration fails fast, returned key sets cannot mutate the registry, `paramNames` caller mutation does not affect the registry

## Technical Specifications & Context

- **Target Files:**
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/registry/{MechanicRegistry,TriggerRegistry,EvaluatorRegistry}.java`
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/registry/Registries.java:83-103`
- **Dependencies:** This is a public API surface; signature changes are a binary-compatibility consideration per `CONVENTIONS-COMMITS.md`. Coordinate with ISSUE-137/138.
- **Constraints:** Do not break existing addon code that uses the current untyped methods without a deprecation path.

### Root Cause

The registries expose `Map<String, Object>`/raw `Class<?>` types, `create(String)` returns `Object`, and `keys()` returns a live mutable `keySet()` of the internal map. Addons can register arbitrary classes (failing at first use with a wrapped `RuntimeException`) and can corrupt the registry through the returned sets. Caller mutation of registered `paramNames` lists mutates registry state.

### Proposed Fix

Introduce typed generics on the registry methods, validate constructor availability at registration, return immutable snapshots from all read methods, and defensively copy stored lists. Provide a deprecation window for the untyped methods if needed.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new registry tests
- [ ] Unit test: registering a class without a public no-arg constructor fails at registration
- [ ] Unit test: `keys().clear()` does not empty the registry
- [ ] Unit test: mutating a caller-owned `paramNames` list after registration does not change the registry
- [ ] `skilling-api` javadoc build (`:skilling-api:build`) passes
