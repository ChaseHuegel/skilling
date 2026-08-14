# ISSUE-294: Shipped `template-skill.yml` is rejected by the parser; it lacks the required ability `trigger`

## Context & User Story
- **Goal:** As a server owner, I want the template skill copied to my data folder on first run to be a valid starting point. Both inlined abilities in `src/main/resources/template-skill.yml` (`geologist`, `vein_miner`) define `mechanics` but no `trigger`; `parseAbilities` rejects any inlined ability without a `trigger` (`"Ability '...' missing required 'trigger' field"`). `docs/dev/template-skill.yml` includes the triggers, so the two templates have drifted, and `Skilling.java` copies the broken one to server data folders on first run.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] Add the missing `trigger` values to both inlined abilities in `src/main/resources/template-skill.yml` to match `docs/dev/template-skill.yml` and the parser.
- [x] Add a regression test that the shipped template parses cleanly (or a test that loads the resource and asserts parse success).
- [x] Confirm the shipped template stays in sync with `docs/dev/template-skill.yml`.

## Technical Specifications & Context
- **Target Files:** `src/main/resources/template-skill.yml:75-211` (inlined abilities at 80-104 and 108-211), parser requirement at `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:374-377`, first-run copy at `Skilling.java:130-133`, reference `docs/dev/template-skill.yml:75,104`.
- **Dependencies:** None.
- **Constraints:** Per `docs/AGENTS.md` and `src/AGENTS.md` section 8, every shipped YAML template must be self-consistent and documented.

## Verification & Definition of Done
- [x] The shipped template parses successfully through `SkillManager`.
- [x] `./gradlew test` and `./gradlew build` pass.
- [x] Edge case handled: the template remains a valid base for both the game engine and the web editor.
