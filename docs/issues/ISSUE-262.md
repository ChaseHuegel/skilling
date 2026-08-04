# ISSUE-262: CraftItemEvent null-recipe NPE in resolveEventMaterial

## Context & User Story
- **Goal:** As an engine maintainer, I want `resolveEventMaterial` to tolerate a null `CraftItemEvent` recipe the same way the sibling bulk-scalar resolver does, so a `craft_item` XP source or mechanic filter never aborts dispatch on an NPE.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — unguarded dereference on a path the code already defends elsewhere for the same event type.

## Implementation Requirements
- [x] Guard `CraftItemEvent` in `resolveEventMaterial`: return `null` when `getRecipe()` or its result is null, mirroring `resolveEventBulkScalar`'s guard.
- [x] Add a unit test covering a `CraftItemEvent` with a null recipe through the material resolver.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:809` (`ce.getRecipe().getResult().getType()` — unguarded), vs `:762-766` (`resolveEventBulkScalar` guards `recipe != null && recipe.getResult() != null`)
  - `src/test/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListenerParamsTest.java`
- **Dependencies:** none.
- **Constraints:** Return `null` (treated as "no match") rather than throwing; do not alter other event branches.

## Verification & Definition of Done
- [x] Null-recipe `CraftItemEvent` resolves to no material without throwing.
- [x] `./gradlew build` and `./gradlew test` pass.
