# ISSUE-104: Accept scalar cooldown values in the web API (fix saving a cooldown in the skill editor)

**Status:** Open
**Type:** Bug
**Severity:** Medium (saving an ability with a cooldown fails with a deserialization error)

---

## Context & User Story

- **Goal:** As a server admin, I want to enter a numeric cooldown on an ability in the skill editor and save it, so that the ability's cooldown round-trips through the web GUI without an error.
- **Agent Role:** You are an expert full-stack engineer executing this task.

## Implementation Requirements

- [x] Make `SkillDetailDTO.RequirementsDTO.cooldown` deserializable from both a plain JSON number (the frontend's current payload) and the evaluator-object shape `{ type, params }` returned by `GET`
- [x] Implement the fix as a custom Jackson deserializer on `RequirementsDTO` (mirroring `SkillSerializer.parseCooldown`) rather than widening the field to `Object`, to keep the DTO typed
- [x] Normalize the frontend so the cooldown input shows the numeric value (currently `NaN`) and submits a shape consistent with what the backend accepts
- [x] Add a round-trip test: `GET` skill → edit cooldown → `PUT` → `GET` again yields the same cooldown value
- [x] Add an E2E test (or extend an existing spec) that saves an ability with a cooldown and reloads it

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/SkillDetailDTO.java` (`RequirementsDTO`, lines 74-83; `EvaluatorDTO`, lines 121-124)
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/SkillSerializer.java` (`parseCooldown`, lines 380-388 — the model the deserializer should mirror; `abilityToMap`, cooldown serialization)
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/SkillHandler.java` (`create`/`update` use `ctx.bodyAsClass(SkillDetailDTO.class)`, lines 73-90)
  - `web/frontend/src/components/skills/AbilitiesSection.vue` (cooldown input, lines 783-793; `updateRequirement`)
- **Dependencies:** Javalin 7's `bodyAsClass` uses Jackson for deserialization. The engine's `SkillManager.parseCooldown` and the web's `SkillSerializer.parseCooldown` both accept a scalar number or evaluator map in YAML — the API contract already supports scalars. Existing E2E spec `ability-trigger.spec.ts` sends the object shape.
- **Constraints:** Web backend owned by `web/AGENTS.md`. Keep `RequirementsDTO` strongly typed. Do not change the engine's cooldown model.

### Root Cause

The frontend sends `cooldown: Number(value)` — a plain number — from the cooldown input. Jackson deserializes into `RequirementsDTO`, whose canonical constructor expects `EvaluatorDTO cooldown`. A JSON `Number` cannot be coerced into the record type, producing:

```
Cannot construct instance of ...$EvaluatorDTO ... no int/Int-argument constructor/factory method to deserialize from Number value (5) ...
```

There is a compact `RequirementsDTO(double cooldown, ...)` constructor for programmatic use, but Jackson record deserialization uses the canonical constructor and never calls it. The engine and the file serializer both accept scalar numbers, so the DTO is the inconsistent layer.

### Secondary Defect

The cooldown input binds to `ability.requirements.cooldown`, but `GET` returns the evaluator object shape, so the input shows `NaN` until the user edits it.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including a round-trip test of scalar and object cooldown forms
- [x] Unit test: `bodyAsClass`-style deserialization accepts `"cooldown": 5` and yields an equivalent constant evaluator
- [x] `cd web/frontend && npm run build` passes (type-check)
- [x] `cd web/frontend && npm run e2e` passes, including saving an ability with a cooldown and reloading it
- [x] Edge case handled: a cooldown of `0` (no cooldown) saves and loads without error
- [x] Runtime check: editing and saving a cooldown in the skill editor produces no deserialization error
