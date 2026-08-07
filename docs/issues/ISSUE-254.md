# ISSUE-254: Extract the Bundled `vein_miner` Ability to `abilities/vein_miner.yml`

## Context & User Story
- **Goal:** As a skill author, I want a real example of a reusable ability. I want the `vein_miner` ability extracted from `mining.yml` into a bundled `abilities/vein_miner.yml`, and referenced by id from `mining.yml`. The skill still defines `unlock_level` so that field overwrites the ability file's value, demonstrating the override path.
- **Agent Role:** You are an expert backend engineer executing this task. This is the sample extraction from the plan, phase 4. No other bundled skills change in this issue.

## Implementation Requirements
- [x] Add `src/main/resources/abilities/vein_miner.yml` containing the full `vein_miner` definition lifted from `mining.yml` (id, display_name, unlock_level, trigger, display, requirements, on_failure, mechanics, feedback). Add commented schema documentation per `src/AGENTS.md` section 8.
- [x] In `src/main/resources/skills/mining.yml`, replace the inline `vein_miner` block with an id reference that still sets `unlock_level: 25` to overwrite the ability file's value.
- [x] The bundled experience is unchanged: `vein_miner` unlocks at level 25 with the same requirements, mechanics, and feedback as before.
- [x] First-run provisioning in `Skilling.onEnable` generates `abilities/vein_miner.yml` when missing.
- [x] `TestSkillManager` preloads the bundled `abilities/` resources from the classpath so bundled-skill sweeps such as `SkillYamlValidationTest` stay green after `mining.yml` references `vein_miner` by id.

## Technical Specifications & Context
- **Target Files:**
  - New: `src/main/resources/abilities/vein_miner.yml`
  - Modified: `src/main/resources/skills/mining.yml`, `src/main/java/io/github/chasehuegel/skilling/Skilling.java`, `src/test/java/io/github/chasehuegel/skilling/TestSkillManager.java`
- **Dependencies:** ISSUE-252 (ability registry and base-merge) and ISSUE-253 must land first.
- **Constraints:** Per `src/AGENTS.md` section 1, no hardcoded abilities in the backend; the ability stays pure YAML. The `vein_miner.yml` base may keep `unlock_level: 25` so the override is a no-op for the bundled default.

## Verification & Definition of Done
- [x] `./gradlew build` passes.
- [x] `./gradlew test` passes, including `SkillYamlValidationTest` against the referenced `vein_miner`.
- [x] A fresh data folder generates `abilities/vein_miner.yml`. `mining` parses with `vein_miner` present and unlocking at level 25.
