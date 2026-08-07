# ISSUE-252: Abilities — Reusable `AbilityManager` Registry and Polymorphic Base-Merge

## Context & User Story
- **Goal:** As a skill author, I want to place reusable ability definitions in an optional `abilities` data folder and register them by id. I want any skill ability whose id exists in the registry to use the registered ability as a base, with the skill's own fields overwriting the inherited fields. This makes abilities portable and shareable, and lets me override one field (for example `unlock_level`) per skill.
- **Agent Role:** You are an expert backend engineer executing this task. Ability files load recursively from subfolders. On an id conflict, the first file loaded wins and a warning is logged. A file that cannot be read as an ability logs a warning and is skipped. Full schema validation happens when a skill references the ability, so standalone files only need shape plus a non-blank `id`.

## Implementation Requirements
- [ ] Add `engine/AbilityManager` holding `Map<String, Map<String, Object>>` raw ability YAML maps keyed by id.
- [ ] `loadAbilities(File dir)`: walk the directory recursively; collect `.yml` files sorted by relative path; a file that is not a YAML map or lacks a non-blank string `id` logs a warning and is skipped; a duplicate id keeps the first entry and logs a warning (no fail-fast); a missing directory is treated as an empty registry.
- [ ] Add `getRaw(String id)` (returns the raw map or null), `getAbilities()`, and `clear()` with full Javadoc per `src/AGENTS.md` section 7.
- [ ] `SkillManager` gains `setAbilityManager(AbilityManager)` and `getAbilityManager()`, mirroring the `setTagResolver` pattern so existing `SkillManager` constructor call sites do not change.
- [ ] `SkillManager.parseAbilities(...)`: for each inline ability map whose id exists in the registry, build the effective map as the base raw map overlaid with the inline map's keys (top-level field overwrite only, no deep merge of nested structures); otherwise parse the inline map alone; keep the within-skill duplicate-id fail-fast; each skill receives its own parsed `Ability` instance so per-skill `unlock_level`, trigger indexing, and state stay independent.
- [ ] Wire `AbilityManager` in `Skilling.onEnable`: construct it, load `abilities/` before `loadSkills`, and hand it to `SkillManager`.
- [ ] Wire `AbilityManager` in `LockdownManager.rebuild`: build a fresh manager from `abilities/` before `skillManager.loadSkills`, and roll back to the previous instance on failure (mirroring the resolver rollback).
- [ ] Do not change bundled skill YAML in this issue (extraction is ISSUE-254).

## Technical Specifications & Context
- **Target Files:**
  - New: `src/main/java/io/github/chasehuegel/skilling/engine/AbilityManager.java`
  - Modified: `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`, `src/main/java/io/github/chasehuegel/skilling/Skilling.java`, `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java`
- **Dependencies:** ISSUE-251 must land first (load order: tags -> abilities -> skills).
- **Constraints:** Per `src/AGENTS.md` section 1, no hardcoded abilities. The setter wiring avoids changing the seven `SkillManager` constructor call sites. New automated coverage for the merge semantics lives in ISSUE-256.

## Verification & Definition of Done
- [ ] `./gradlew build` passes.
- [ ] `./gradlew test` passes.
- [ ] An ability registered from `abilities/` acts as the base for a skill ability with the same id; the skill's explicit fields overwrite the inherited fields and inherited fields are preserved.
- [ ] A skill ability with an unknown id and an incomplete inline definition fails at parse time.
- [ ] A malformed or id-less file in `abilities/` logs a warning and does not abort the load.
