# ISSUE-280: SkillManager raw casts throw ClassCastException instead of fail-fast IllegalArgumentException

## Context & User Story
- **Goal:** As a server owner, I want one malformed skill file to be skipped with a warning, not to disable the whole plugin. `loadSkills()` only catches `IllegalArgumentException` (the documented warn-and-skip contract), but `parseSkill` casts untrusted YAML values directly, so a typo such as `trigger: 5` or `display_name: 3` throws `ClassCastException`. That escapes `onEnable()` and disables Skilling entirely; on `/skills reload` it also corrupts the rebuild path.
- **Agent Role:** You are an expert backend/QA engineer executing this task.

## Implementation Requirements
- [x] Replace every unchecked raw cast in `SkillManager.parseSkill` (and its helpers) with type-checked accessors that throw `IllegalArgumentException` with a descriptive message on non-string/scalar/map/list mismatches.
- [x] Cover at minimum: `trigger`, `display_name`, `id`, `state`, `items`, `sounds`, and `target`/`tool` filter keys.
- [x] Keep the existing `instanceof Map` guards and `String.valueOf`-style sites consistent with the new accessors.
- [x] Add unit tests feeding numeric/scalar values for these keys and assert the failure is `IllegalArgumentException` and that `loadSkills` skips the file with a warning rather than propagating.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:280, 294-296, 350, 371, 374, 451, 454, 568, 668-670`. `loadSkills()` catch site at line ~122. The `LockdownManager.rebuild()` rollback path (`LockdownManager.java:185-200`) also tolerates a generic `Exception`, so a CCE currently rolls back state non-deterministically.
- **Dependencies:** None beyond `SkillManager`.
- **Constraints:** Per `src/AGENTS.md` section 9, parsing must fail fast with `IllegalArgumentException`; only the aggregate folder loaders may warn-and-skip. Match the message style already used for missing `trigger` / unknown-trigger failures.

## Verification & Definition of Done
- [x] New tests pass: each malformed-value case throws `IllegalArgumentException` from parsing.
- [x] A folder containing one malformed file plus valid files loads the valid files and logs a warning for the bad one.
- [x] `./gradlew test` and `./gradlew build` pass.
- [x] Edge case handled: `LockdownManager` reload completes cleanly when a file in the data folder is malformed.
