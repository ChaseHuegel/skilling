# ISSUE-228: Web skill editor cannot represent addon-registered evaluator types

## Context & User Story
- **Goal:** As an addon author, I want skill parameters using my registered custom evaluator type (e.g. `logistic`) to be preserved and editable in the web GUI instead of being silently rewritten to a constant `0`.
- **Agent Role:** You are an expert full-stack engineer executing this task.

## Implementation Requirements
- [ ] `SkillSerializer.parseEvaluator` (lines 407-451) must not collapse unknown evaluator shapes to `constant 0`. When a YAML parameter is a single-key object whose type is not one of the built-ins (`constant`, `linear`, `milestones`, `polynomial`), preserve it generically (type name + raw params) rather than defaulting to a constant.
- [ ] `SkillSerializer.evaluatorToMap` must round-trip those generic evaluator types so a web save does not rewrite them.
- [ ] The `EvaluatorDTO` representation should carry the raw custom type/params through the JSON so the frontend can at least round-trip them untouched; ideally the editor renders an "advanced/custom evaluator" read-only or raw-JSON view for unknown types.
- [ ] Add a round-trip regression test: a skill with a parameter `{ logistic: { ... } }` (registered evaluator) survives `fromMap` → `toYaml` → engine `parseSkill` with the same evaluator type.
- [ ] Cross-check with `SkillManager.parseInlineEvaluator` (`SkillManager.java:589-596`), which already supports a registered custom evaluator type key — the web side is the only place that loses it.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/SkillSerializer.java`
  - `web/frontend/src/components/common/EvaluatorParameter.vue` (add an unknown-type fallback view)
  - Reference: `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:589-596`
- **Dependencies:** Addon-registered evaluators surface via `EvaluatorRegistry`; the web's `/api/mechanics` registry feed does not currently expose evaluator types, so the DTO must be schema-agnostic for unknown types.
- **Constraints:** Never silently convert an unknown evaluator to `ConstantEvaluator(0)` — that is the bug (it zeroes mechanic parameters). Preserve or reject loudly.

## Verification & Definition of Done
- [ ] A custom evaluator type round-trips through the web GET/PUT unchanged and loads in the engine.
- [ ] The editor does not convert unknown types to a constant `0` on load or save.
- [ ] `./gradlew build` and `./gradlew test` pass.
- [ ] `cd web/frontend && npm run build` passes.
