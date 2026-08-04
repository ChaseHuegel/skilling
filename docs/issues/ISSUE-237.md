# ISSUE-237: `%skilling_evaluator_%` placeholders break for skill IDs with underscores

## Context & User Story
- **Goal:** As a server owner, I want `%skilling_evaluator_<skill>_<ability>_<param>%` to work for skills like `heavy_weapons`, not silently return `0`.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — `PlaceholderAPIHook.resolveEvaluator` splits the placeholder on the first `_` (`PlaceholderAPIHook.java:111`), so `heavy_weapons_double_strike_multiplier` resolves skill `heavy` and fails. Many bundled skill IDs contain underscores.

## Implementation Requirements
- [x] Resolve the skill ID in the evaluator placeholder against the full placeholder string instead of a blind `_` split (e.g. match the longest registered skill-id prefix, or a delimiter scheme that distinguishes skill/ability/param). `total_levels` is already special-cased at `:55`; extend the same awareness to this path.
- [x] Add unit tests covering a skill ID with underscores (e.g. `%skilling_evaluator_heavy_weapons_<ability>_<param>%`).

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/integration/PlaceholderAPIHook.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/integration/PlaceholderAPIHookTest.java` (new)
- **Dependencies:** none.
- **Constraints:** Keep the existing `%skilling_level_<skill>%` / `%skilling_xp_<skill>%` syntax working (those split once and treat the rest as the skill ID, which already handles underscores — do not regress them).

## Verification & Definition of Done
- [x] Evaluator placeholders resolve for underscore-containing skill IDs.
- [x] Existing level/xp placeholders still work.
- [x] `./gradlew build` and `./gradlew test` pass.
