# ISSUE-217: Reload atomicity and state-filter load validation

## Context & User Story
- **Goal:** As a server admin, I want a failed reload to leave the plugin in a consistent state, and a state-filter typo to fail at load instead of silently never matching (or throwing) on the event path.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] **Atomic reload:** `LockdownManager.reload` phase 4 swaps `tagResolver`/`entityTagResolver` into `Skilling`, `SkillManager`, `RequirementEngine`, and `SkillEventListener` (`LockdownManager.java:88-94`) and clears/re-registers registries **before** `skillManager.loadSkills` (`:97`) validates the YAML. If `loadSkills` throws, the previous skill set survives but runs against the new resolver — skills can silently stop matching after a failed reload. Only swap resolvers/registries after the new skills parse successfully, or roll back on failure.
- [ ] **State-key load validation:** unknown `state:` keys in XP-source filters, mechanic filters, and requirement states are not validated at load (`SkillManager.java:344-345`, `468-478`; only `equipped_*` targets are warmed at `:413-419`). A typo like `is_sneakingg` silently evaluates false at runtime. Validate state keys against `StateFilterRegistry` during parsing (fail-fast).
- [ ] **`biome` filter safety:** `Skilling.java:500-504` calls `NamespacedKey.fromString(v)` with no try/catch and no load validation; a malformed value throws `IllegalArgumentException` on the main thread during dispatch. Guard the parse and validate at load (or wrap in try/catch like other filters).
- [ ] **`player_placed` semantics:** `Skilling.java:429-434` ignores the filter value and always returns `!hasMetadata("player_placed")`; `player_placed:true` behaves like `false`, and the metadata is never cleared when a player-placed block is broken (regenerated blocks keep the flag). Make the filter value-aware (`true`/`false`) and clear/ignore stale metadata.
- [ ] **GUI apply-time validation gap:** `GuiLayoutHandler.update` (`GuiLayoutHandler.java:80-94`) validates only rows/slot bounds; the engine rejects duplicate slots in a page and reserved-navigation-row slots at apply (`GuiLayoutConfig.java:115-118`), which after a live reload can brick subsequent GUI reloads. Validate what the engine validates, before staging.
- [ ] Add load-validation tests for unknown state keys, malformed biome values, and `player_placed` values; add a reload-rollback test covering the resolver-swap ordering.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` (metadata write at :127)
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/GuiLayoutHandler.java` + `engine/ui/GuiLayoutConfig.java`
  - Tests: `LockdownManagerReloadTest`, `SkillManagerTagValidationTest`, `SkillManagerReloadTest`, `StateFilterTargetTypeTest`, `GuiLayoutHandlerValidationTest`
- **Dependencies:** none.
- **Constraints:** Bundled skills use `state: "player_placed:false"`; the fix must keep that intent while making `true` correct. Greenfield — adding load-time rejection of previously-"accepted" state keys is allowed.

## Verification & Definition of Done
- [ ] A reload that throws (bad skill YAML) leaves resolvers/registries on the previous consistent set.
- [ ] Unknown state keys and malformed biome values fail at load, not at runtime.
- [ ] `player_placed:true` and `player_placed:false` both behave per their value; stale metadata no longer blocks natural blocks.
- [ ] The GUI rejects the same gui.yml layouts the engine rejects, before apply.
- [ ] `./gradlew build` and `./gradlew test` pass.
