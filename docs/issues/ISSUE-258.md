# ISSUE-258: Empty feedback scalars cause NPEs that abort the whole ability dispatch

## Context & User Story
- **Goal:** As a skill author, I want a feedback map entry written as an empty scalar (`action_bar:` / `message:`) to be treated as empty text, never to crash the dispatch loop and silently skip the remaining abilities and XP sources for that event.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** High — SnakeYAML gives a `null` value for a key written as `action_bar:` or `message:` (present but empty). `getOrDefault(key, "")` only supplies the default when the key is *absent*, not when its value is `null`, so the null reaches `.isBlank()` on the event path.

## Implementation Requirements
- [ ] Coalesce null feedback strings to `""` at parse time in `SkillManager.parseOnFailure` (action_bar) and `SkillManager.parseFeedback` (message), OR guard the call sites in `SkillEventListener` (`failure.actionBar()`, `ability.feedback().message()`).
- [ ] Add a unit test feeding a `feedback.notify.message:` / `on_failure.<reason>.action_bar:` empty-scalar YAML and asserting the skill still loads and dispatches without an NPE.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:483` (`feedbackMap.getOrDefault("action_bar", "")`), `:580` (`notify.getOrDefault("message", "")`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:552` (`failure.actionBar().isBlank()`), `:614` (`abilityMsg.isBlank()`)
  - `src/test/java/io/github/chasehuegel/skilling/engine/SkillYamlValidationTest.java`
- **Dependencies:** none.
- **Constraints:** The null deref in `fireAbilities` occurs *outside* the per-mechanic try/catch (`SkillEventListener.java:588-598`), so the NPE is not contained; fix must cover the failure-feedback and success-feedback paths.

## Verification & Definition of Done
- [ ] No NPE when feedback strings are empty scalars; dispatch completes normally.
- [ ] `./gradlew build` and `./gradlew test` pass.
