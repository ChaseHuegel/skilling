# ISSUE-221: Align web progression serialization with the engine's base_xp/exponent schema

## Context & User Story
- **Goal:** As a server admin, I want a skill saved with a `linear` or `constant` progression curve from the web GUI to keep its curve parameters instead of silently resetting to the engine defaults after reload.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] Make `SkillSerializer.toMap` (lines 116-134) write progression keys that `SkillManager.parseProgression` (`SkillManager.java:186-207`) actually reads. The engine derives **every** curve from `base_xp` (+ `exponent` for the linear step), so the web must emit those keys — today it writes `base`/`step`/`min`/`max` for `linear` and `value` for `constant`, which the engine ignores, defaulting to `base_xp=50` / `exponent=2.5`.
- [x] Make `SkillSerializer.fromMap` (lines 55-65) parse the engine's real linear/constant key set (`base_xp`, `exponent`) so an existing linear/constant skill round-trips without losing its curve parameters.
- [x] Decide the canonical contract and document it in `docs/dev/template-skill.yml` and `docs/users/creating-skills.md`: either (a) keep the engine's `base_xp`/`exponent`-derived model and expose only those fields in the web editor, or (b) extend the engine's `Progression` to accept `base`/`step`/`min`/`max`/`value` and derive `LinearEvaluator`/`ConstantEvaluator` from them. Pick one and make both sides consistent.
- [x] Update `ProgressionSection.vue` so the fields offered for each curve match the chosen contract (no fields that serialize to nothing).
- [x] Add a round-trip regression test: a skill with `curve: linear` (and a `constant` curve) survives `fromMap` → `toYaml` → engine `parseSkill` with identical `ParameterEvaluator` behavior.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/SkillSerializer.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`
  - `web/frontend/src/components/skills/ProgressionSection.vue`
  - `docs/dev/template-skill.yml`, `docs/users/creating-skills.md`
- **Dependencies:** none.
- **Constraints:** All 32 bundled skills ship `curve: polynomial`, so no bundled content breaks; this affects user-authored linear/constant skills (the engine supports them and the web editor exposes them). Greenfield — pick the cleaner contract even if it is breaking.

## Verification & Definition of Done
- [x] A web-saved `linear` or `constant` progression reloads with the intended curve parameters (not the 50.0 default).
- [x] Round-trip test passes for linear and constant curves.
- [x] `./gradlew build` and `./gradlew test` pass.
- [x] `cd web/frontend && npm run build` passes.
