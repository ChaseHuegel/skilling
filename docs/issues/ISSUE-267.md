# ISSUE-267: Staged-skill validation races the reload rebuild, causing spurious 400s

## Context & User Story
- **Goal:** As an admin, I want a valid staged skill to always validate successfully, never to be spuriously rejected with "unknown mechanic / trigger / state" because validation ran mid-reload while the registries were momentarily empty.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — intermittent false failures from a cross-thread race between the web layer and the engine.

## Implementation Requirements
- [ ] Serialize `validateStagedSkill` against the reload rebuild (e.g. take the staging lock or a reload read-lock during validation), or validate against a read-only snapshot of the registries so a concurrent `reloadAsync` clear/rebuild cannot empty them mid-parse.
- [ ] Add a test (or extend an existing concurrency test) proving validation during a reload window succeeds for valid content.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/SkillHandler.java:201-210` (`validateStagedSkill` runs engine parsing on the Jetty worker thread)
  - `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java:151-166` (rebuild clears then re-registers `MechanicRegistry`/`TriggerRegistry`/`EvaluatorRegistry`/`StateFilterRegistry`)
  - `src/test/java/io/github/chasehuegel/skilling/web/handler/SkillHandlerStagingValidationTest.java`
- **Dependencies:** none.
- **Constraints:** Do not block the main thread; keep the fail-fast behavior for genuinely invalid content. The concurrent `TagResolver` cache `warm` mutation is also owned by this fix.

## Verification & Definition of Done
- [ ] Valid staged skills validate even when saved during a reload window.
- [ ] Invalid skills still return 400.
- [ ] `./gradlew build` and `./gradlew test` pass.
