# ISSUE-220: Fix milestone evaluator round-trip between the web GUI and the engine

## Context & User Story
- **Goal:** As a server admin, I want to view and edit milestone-based mechanic parameters (e.g. `chain_limit` on `core:chain_break`) in the web GUI without the values being silently dropped on reload or the skill page returning a 500.
- **Agent Role:** You are an expert full-stack engineer executing this task.

## Implementation Requirements
- [ ] `SkillSerializer.parseEvaluator` must represent milestone parameters as an ordered `Map<level, value>` on read (today it returns the raw SnakeYAML map, which Jackson serializes as a JSON object).
- [ ] `SkillSerializer.evaluatorToMap` must emit a YAML **map** (`milestones: { 25: 3, 50: 8 }`) for milestone evaluators. It must accept either an editor-array shape (`[{level, value}]`) or a map shape as input and normalize to the map form, so the engine's `parseInlineEvaluator` (`SkillManager.java:569-580`) always receives a map.
- [ ] `EvaluatorParameter.vue` must convert between the API's map shape and the editor's array-of-rows shape so existing milestone rows render (today `Array.isArray(entries)` is false for the map shape and the list shows empty, `EvaluatorParameter.vue:37-52`).
- [ ] Prevent the `ClassCastException` in `SkillSerializer.parseEvaluator` (`(Map) raw.get("milestones")` at line 437) when the YAML value is a list — this currently makes `GET /api/skills/{id}` return 500 after a milestone save round-trip.
- [ ] Add a round-trip regression test (web handler test): parse a skill with `milestones`, serialize it back, re-parse, and assert the milestone levels/values are identical; also assert an editor-array milestone payload is normalized to a map.
- [ ] Add an engine load test asserting a milestone evaluator written by the web (array form, if any path still produces one) is either rejected loudly or normalized — never silently empty.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/SkillSerializer.java` (lines 436-439, 462)
  - `web/frontend/src/components/common/EvaluatorParameter.vue`
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java` (lines 569-580)
  - `web/frontend/src/views/SkillEditorPage.vue` (mechanic `params` conversion, `enrichFormKeys`)
  - `web/frontend/src/components/skills/MechanicsEditor.vue` (`params` array shape)
- **Dependencies:** none. The engine already parses milestone maps correctly; only the web DTO serializer and editor shape handling are broken.
- **Constraints:** Greenfield — no backward-compatibility requirement. All bundled skills use milestone evaluators for `chain_break`/`level_break` `chain_limit`, so this is high-impact for editing the shipped skills.

## Verification & Definition of Done
- [ ] Opening `mining.yml` in the web GUI shows its 4 `chain_limit` milestone rows.
- [ ] Editing and saving a milestone evaluator, then applying via reload, results in the same thresholds in `SkillManager.getSkills()`.
- [ ] `GET /api/skills/{id}` returns 200 after a milestone save (no 500).
- [ ] `./gradlew build` and `./gradlew test` pass.
- [ ] `cd web/frontend && npm run build` passes.
