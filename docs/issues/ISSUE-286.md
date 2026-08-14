# ISSUE-286: Level anchor mismatch between the threshold table and `setlevel`/XP-bar math

## Context & User Story
- **Goal:** As a skill author using `curve: linear`, I want `/skills setlevel` and the in-game XP bar to agree with actual leveling. The threshold table is built with `evaluator.evaluate(level, 1)` while `setlevel` and the XP bar use `evaluate(level, 0)`. For anchor-insensitive curves (`polynomial`, `constant`) this is invisible, but `LinearEvaluator` uses `base + step * (currentLevel - unlockLevel)`, so `/skills setlevel 5` computes the threshold for level 6, and the XP bar reads 0%/negative immediately after a level-up.
- **Agent Role:** You are an expert backend/QA engineer executing this task.

## Implementation Requirements
- [x] Pick one anchor and apply it everywhere: either build `LevelThresholds` with `unlockLevel = 0` or compute `setlevel` XP and XP-bar values with `unlockLevel = 1`.
- [x] Audit every caller of the progression evaluator: `LevelThresholds.java:75`, `SkillsCommand.java:277,299`, `LevelUpDispatcher.java:55-56`, `SkillMenuBuilder.java:205-206`.
- [x] Add a unit test with a `linear` progression asserting `/skills setlevel <level>` lands at exactly that level and the XP bar shows consistent progress.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/...` — `SkillManager.parseProgression` (`SkillManager.java:259`, allows `curve: linear`), `LevelThresholds.java:72-87`, `engine/command/SkillsCommand.java:277,299`, `engine/feedback/LevelUpDispatcher.java:55-56`, `engine/ui/SkillMenuBuilder.java:205-206`, `engine/evaluator/impl/LinearEvaluator.java:67`.
- **Dependencies:** None.
- **Constraints:** This is a breaking behavior fix; greenfield rules allow changing the anchor, but the choice must be consistent and documented in `template-skill.yml`.

## Verification & Definition of Done
- [x] New `linear`-progression test passes for both `setlevel` and the XP bar.
- [x] `./gradlew test` and `./gradlew build` pass.
- [x] Edge case handled: `polynomial` and `constant` curves behave unchanged.
