# ISSUE-284: Cooldowns and failure-feedback debounce keyed by `abilityId` alone collide across skills

## Context & User Story
- **Goal:** As a skill author, I want two different skills that both use an ability id such as `haste` (a shared base ability from `abilities/`) to have independent cooldowns. `RequirementEngine.cooldowns` and the failure-feedback debounce are keyed by `(playerUuid, abilityId)` with no skill dimension, so activating one skill's `haste` cools down the other, and `{time}` failure messages can be suppressed for the wrong ability.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] Key cooldown and feedback-debounce state by `(skillId, abilityId)` instead of `abilityId` alone.
- [x] Thread the owning skill id through the `check`/`consume` calls and `tryDebounce`.
- [x] Add a unit test: two skills sharing an ability id cool down independently, and each keeps its own failure feedback.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java:257-269` (`cooldowns`), `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:598` (`feedbackDebouncer`). The `AbilityRef`/`XpSourceRef` types already carry the owning skill.
- **Dependencies:** `AbilityManager` shared-ability merge (`SkillManager.parseAbilities`, `SkillManager.java:340-369`).
- **Constraints:** Preserve the existing bounded-prune behavior for cooldown storage.

## Verification & Definition of Done
- [x] New cross-skill cooldown test passes.
- [x] `./gradlew test` and `./gradlew build` pass.
- [x] Edge case handled: a single skill with the id still behaves exactly as before.
