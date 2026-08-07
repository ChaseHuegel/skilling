# ISSUE-256: Automated Coverage for the Portability Loading Features

## Context & User Story
- **Goal:** As a maintainer, I want unit and end-to-end tests that lock in the new loading semantics: recursive tag loading with additive merge, recursive skill loading with first-wins conflicts, and polymorphic ability inheritance. Phases 1 through 4 shipped minimal regression updates; this issue adds the dedicated coverage.
- **Agent Role:** You are an expert QA and backend engineer executing this task. Add the tests, then run the full suite.

## Implementation Requirements
- [x] `CustomTagLoaderTest`: add tests for `loadDirectory` — recursion into subfolders, additive merge when two files define the same tag key, cross-file `#c:` references resolving in one pass, and a malformed file being skipped while the other files still load.
- [x] New `AbilityManagerTest`: recursion into subfolders, first-wins on a duplicate id with a warning, a malformed file skipped, a missing `id` skipped, and a missing directory yielding an empty registry.
- [x] New `SkillManager` ability-inheritance tests: a pure id reference inherits the full base definition; a field such as `unlock_level` overrides the base; a nested field such as `mechanics` is replaced wholesale when the skill supplies it; an unknown id with an incomplete inline definition fails at parse time; two skills inheriting the same ability get independent `Ability` instances with their own `unlock_level`.
- [x] `SkillManager` recursive-skill tests: a skill in a subfolder of `skills/` loads; a duplicate skill id keeps the first loaded skill; a malformed skill file logs a warning and the remaining skills load.
- [x] Web test updates for the `tags/base.yml` path and the reference-tolerance round-trip (GET without a trigger does not error).
- [x] E2E fixture data seeds `tags/base.yml` and a skill inside a `skills/` subfolder; the dashboard lists both.

## Technical Specifications & Context
- **Target Files:**
  - Modified: `src/test/.../tag/CustomTagLoaderTest.java`, `src/test/.../web/handler/HandlerErrorHandlingTest.java`, `src/test/.../web/handler/TagHandlerEntityTagsRoundTripTest.java`, `src/test/.../web/staging/StagingManagerConcurrencyTest.java`
  - New: `src/test/.../engine/AbilityManagerTest.java`, `src/test/.../skill/SkillManagerAbilityInheritanceTest.java`, `src/test/.../skill/SkillManagerRecursiveLoadTest.java`
  - E2E: `web/frontend/e2e/test-data/` fixtures and any affected spec
- **Dependencies:** ISSUE-251 through ISSUE-255 must land first.
- **Constraints:** Follow the existing test conventions (`TestSkillManager`, `@TempDir`, `MockedStatic<Bukkit>` with type-aware `FakeRegistryAccess`). Tests live in `src/test/` mirroring the main tree.

## Verification & Definition of Done
- [x] `./gradlew build` passes.
- [x] `./gradlew test` passes, including all new coverage.
- [x] `cd web/frontend && npm run build` passes.
- [x] `cd web/frontend && npm run e2e` passes for the sprint's new coverage (the seeded base skill and the subfolder skill both appear on the dashboard). The full suite still has two pre-existing failures surfaced by the warn-and-skip server-start fix — the auth redirect on direct guarded-route navigation and the topbar overflow at 375px. Both are out of scope here and tracked as ISSUE-258.
