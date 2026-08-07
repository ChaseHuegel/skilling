# ISSUE-253: Skills — Load Recursively with Warn-and-Skip and First-Wins Conflicts

## Context & User Story
- **Goal:** As a server owner, I want to organize skill definitions into subfolders of the `skills` data folder. I want every `.yml` file, including files in subfolders, to load. I want a duplicate skill id to keep the first loaded skill and log a warning. I want a skill file that cannot be parsed to log a warning and be skipped, never failing the load.
- **Agent Role:** You are an expert backend engineer executing this task. This issue intentionally relaxes the previous fail-fast contract for skill content so skill packs can hold work-in-progress files. Catastrophic file-system I/O errors may still throw.

## Implementation Requirements
- [x] `SkillManager.loadSkills(File skillsDir)`: collect `.yml` files recursively, sorted by relative path for a deterministic order.
- [x] A file whose parse throws `IllegalArgumentException` logs a warning with the file name and message and is skipped. It does NOT fail the load.
- [x] A parsed skill whose id already exists keeps the first loaded skill and logs a warning. The later file is skipped.
- [x] Preserve the build-into-a-local-map-then-atomic-swap pattern so concurrent readers never see a half-loaded snapshot and reload stays atomic.
- [x] Update the existing tests that assert fail-fast behavior on duplicate skill ids to assert first-wins plus a warning, so the suite stays green.
- [x] `Skilling.loadSkills()` keeps creating the `skills` directory when missing.

## Technical Specifications & Context
- **Target Files:**
  - Modified: `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`
  - Test updates: `src/test/.../skill/SkillManagerTest.java`, `src/test/.../skill/SkillManagerReloadTest.java`, and any other test that asserts a duplicate skill id throws
- **Dependencies:** ISSUE-252 must land first so `parseSkill` can resolve ability references against the live registry.
- **Constraints:** Per `src/AGENTS.md` section 2, do not block the Bukkit main thread; file enumeration runs at load time on the main thread as today. New automated coverage for recursion and skip behavior lives in ISSUE-256.

## Verification & Definition of Done
- [x] `./gradlew build` passes.
- [x] `./gradlew test` passes.
- [x] A skill in a subfolder of `skills/` loads. A malformed skill file logs a warning and the rest of the skills load. A duplicate skill id keeps the first and logs a warning.
