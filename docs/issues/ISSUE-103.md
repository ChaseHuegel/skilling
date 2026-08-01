# ISSUE-103: Fix web skill parsing of string-valued evaluator constants (breaks riding and 12 other skills)

**Status:** Open
**Type:** Bug
**Severity:** Medium (web GUI editor and abilities page break for any skill using a string constant parameter)

---

## Context & User Story

- **Goal:** As a server admin, I want to open any skill (including `riding`) in the web GUI's skill editor and abilities page, so that I can view and edit its abilities without the page erroring out.
- **Agent Role:** You are an expert full-stack engineer executing this task.

## Implementation Requirements

- [ ] Add a string-`constant` branch to `SkillSerializer.parseEvaluator` so a `constant` value that is a `String` (e.g. `"minecraft:slowness"`) is preserved as `EvaluatorDTO("constant", { value: "<string>" })` instead of throwing
- [ ] Update `SkillSerializer.evaluatorToMap` to serialize a string-valued constant back to YAML as a quoted string (round-trip parity)
- [ ] Add an error log in `SkillHandler.get` so parsing failures are visible server-side instead of a silent 500
- [ ] Add unit tests for `parseEvaluator` / `evaluatorToMap` round-trip with string constants (`effect`, `attribute`, `material`)
- [ ] Confirm the riding skill (and the other affected skills) open correctly in the skill editor and abilities page

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/SkillSerializer.java` (`parseEvaluator`, lines 407-444; `evaluatorToMap`, lines 446-454)
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/SkillHandler.java` (`get`, lines 57-71)
  - `web/frontend/src/views/AbilitiesPage.vue` (sequential fetch of every skill, lines ~123-137)
  - Tests under `src/test/`
- **Dependencies:** The engine already handles string constants safely via `SkillManager.castMap` — this is purely the web serializer side. Related work: ISSUE-101 (engine) and ISSUE-104 (web cooldown DTO). See `web/AGENTS.md` for web ownership rules.
- **Constraints:** Javalin 7 backend; web backend is owned by `web/AGENTS.md`. Do not degrade the engine's `SkillManager`. Prefer a fix that mirrors the engine's evaluator model so ISSUE-101 and this issue converge rather than diverge.

### Root Cause

`SkillSerializer.parseEvaluator` (line 412-419) only handles numeric `constant` values:

```java
if (raw.containsKey("constant")) {
    Object val = raw.get("constant");
    if (val instanceof Number n) {
        return new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", n.doubleValue()));
    }
    Map<String, Object> nested = (Map<String, Object>) val;   // <-- ClassCastException for String
    return new SkillDetailDTO.EvaluatorDTO("constant", Map.of("value", doubleVal(nested, "value", 0)));
}
```

When `val` is a `String`, the cast `(Map<String, Object>) val` throws a `ClassCastException`. `SkillHandler.get` catches it and returns HTTP 500 with `e.getMessage()`, but never logs it (matching the reported "No error is logged").

### Impact

Any skill YAML using a string constant parameter fails to parse. Because `AbilitiesPage.vue` fetches every skill sequentially, one failing skill breaks the entire abilities page. The riding skill (`cavalry_charge` → `core:apply_status`, `effect: { constant: "minecraft:slowness" }`) is the reported trigger, but the same pattern breaks at least 25 entries across ~13 skills (shields `material`, bard auras, unarmed, archery, wizardry, piety, dual_wield, one_handed, throwing, light_weapons, heavy_weapons).

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new serializer round-trip tests
- [ ] Unit test: `parseEvaluator({ constant: "minecraft:slowness" })` yields `EvaluatorDTO("constant", { value: "minecraft:slowness" })`
- [ ] Unit test: `evaluatorToMap` round-trips a string constant back to `{ constant: "minecraft:slowness" }`
- [ ] `cd web/frontend && npm run build` passes (type-check)
- [ ] Edge case handled: numeric constants (e.g. `{ constant: 3 }`) still parse to a numeric `value` — no numeric regressions
- [ ] Runtime check: the riding skill opens in the skill editor and the abilities page renders all skills
