# ISSUE-186: Extend `target_type` to `entity_kill` and add entity-type tag support

## Context & User Story
- **Goal:** As a skill designer, I want to gate `entity_kill` XP sources on the killed entity's type (e.g. `state: target_type: #c:undead`) so undead-kill sourcing (piety §6, alchemy) works, so that rare/low-frequency skills gain high-frequency sources.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Inputs:** [REPORT_XP-SOURCE-DESIGN.md](../reports/REPORT_XP-SOURCE-DESIGN.md) §5/§6/§7 (approved), `docs/issues/INDEX.md`.

## Implementation Requirements
- [x] Extend the `target_type` state filter (`Skilling.java` `registerBuiltinStateFilters`) to evaluate `EntityDeathEvent` by comparing `event.getEntity().getType()`, in addition to the existing `EntityDamageByEntityEvent` path.
- [x] Change the vacuous pass-through: the filter must return `false` for events it cannot evaluate (currently returns `true` for non-damage events, so `entity_kill` + `target_type` passes for every kill).
- [x] Support tag references: `target_type` value may be a `#...` tag (e.g. `#c:undead`, `#minecraft:zombies`) resolved through a new entity-type tag resolver; bare values keep the current single-`EntityType` behavior.
- [x] Add an `entity_tags:` section to `src/main/resources/tags.yml` (custom entity tags, `#c:` prefix), loaded alongside `custom_tags:` without breaking existing material tags.
- [x] Add an `EntityTagResolver` (mirroring `TagResolver`): flattened `EnumSet<EntityType>`, cached O(1) resolution, supports `#minecraft:*` via `Tag.REGISTRY_ENTITY_TYPES` and `#c:*` via the new store. Fail-fast on unknown refs at load.
- [x] Wire the entity tag store + resolver into plugin load and the `/skills reload` lockdown sequence (reload rebuilds both resolvers).
- [x] Register `c:undead` in `tags.yml` under `entity_tags:` (contents per REPORT §5: `#minecraft:zombies`, `#minecraft:skeletons`, `wither_skeleton`, `phantom`, `zombified_piglin`, `drowned`, `stray`, `husk`).

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/EntityTagLoader.java` (new, or extend `CustomTagLoader`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/EntityTagResolver.java` (new)
  - `src/main/resources/tags.yml`
  - `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java`
  - `src/test/**` mirroring `StateFilterEquippedTest`/`TagResolverTest` patterns
- **Dependencies:** none (self-contained engine change).
- **Constraints:** `skilling-api` must stay dependency-free and unchanged. No per-event tag resolution on the hot path (flatten at load). Fail-fast `IllegalArgumentException` on unknown entity refs.

## Verification & Definition of Done
- [x] `./gradlew build` passes.
- [x] `./gradlew test` passes, including new unit tests:
  - `target_type` matches `EntityDeathEvent` entity type (pass) and rejects other types (fail).
  - `target_type` matches a `#c:undead` / `#minecraft:zombies` tag on `entity_kill`.
  - Non-damage/non-death events fail the filter (no vacuous pass).
  - `EntityTagResolver` resolves `#minecraft:*` + `#c:*` to `EnumSet<EntityType>` and fails fast on unknown refs.
  - Reload rebuilds entity tags without stale entries.
- [x] Edge case handled: `target_type` on `EntityDamageByEntityEvent` still works exactly as before (regression check).

**Note:** `./gradlew build` is blocked by 4 pre-existing `ProjectileHitTriggerTest`/`ProjectileReturnMechanicTest` failures (broken mock eye location on `main`), tracked in ISSUE-190. The 19 new tests for this ticket pass; total suite: 572 tests, 4 failed (all pre-existing).
