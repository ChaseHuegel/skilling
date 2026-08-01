# ISSUE-101: Preserve string-valued parameter constants through inline evaluator parsing

**Status:** Open
**Type:** Bug
**Severity:** High (runtime exception on ability activation for every bundled status/aura ability)

---

## Context & User Story

- **Goal:** As a player, I want every bundled skill ability that applies a namespaced effect, attribute, or material (e.g. `minecraft:poison`, `minecraft:shield`) to actually apply it when triggered, so that abilities do not throw errors and behave as their YAML configures them.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Add a string-capable evaluator (or raw-parameter passthrough) so string constants survive YAML parsing and reach `PotionEffectResolver` / `ModifyAttributeMechanic` / `SetCooldownMechanic` at runtime
- [x] Wire `SkillManager.parseInlineEvaluator` to return the new evaluator for non-numeric `constant` values instead of coercing them to `ConstantEvaluator(0.0)`
- [x] Update `SkillEventListener.evaluateParams` so string-valued evaluators emit the raw string (not a `double`) into the params map handed to `mechanic.execute(...)`
- [x] Ensure numeric parameters remain `double` values (no behavior change for existing numeric params)
- [x] Add unit tests covering parse → evaluate for string constants (`effect`, `attribute`, `material`)

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java` (`parseInlineEvaluator`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` (`evaluateParams`, ~line 660)
  - New evaluator under `src/main/java/io/github/chasehuegel/skilling/engine/evaluator/impl/`
  - Corresponding tests under `src/test/`
- **Dependencies:** `ParameterEvaluator` interface (`double evaluate(int, int)`); `PotionEffectResolver`, `ModifyAttributeMechanic`, `SetCooldownMechanic` already accept strings (Phase 2 commits `4537121`, `4c60d0f`). See `src/AGENTS.md` §1, §3, §7.
- **Constraints:** Every `ParameterEvaluator` implementation must have a class-level Javadoc; fail-fast on malformed configs; no inline comments that restate code; keep numeric params as doubles.

### Root Cause

`SkillManager.parseInlineEvaluator` only preserves numeric `constant` values:

```java
if (map.containsKey("constant")) {
    Object val = map.get("constant");
    if (val instanceof Number n) {
        return new ConstantEvaluator(n.doubleValue());
    }
    Map<String, Object> nested = castMap(val);   // <-- String is cast to an empty map
    return new ConstantEvaluator(((Number) nested.getOrDefault("value", 0.0)).doubleValue());
}
```

When `val` is a `String` (e.g. `"minecraft:poison"`), `castMap(val)` returns an empty map, so `nested.getOrDefault("value", 0.0)` resolves to `0.0`. The string is thrown away with no warning.

### Runtime Path

1. `SkillEventListener.fireAbilities` → `evaluateParams(...)` evaluates every parameter evaluator to a `double` and builds the `Map<String, Object>` handed to `mechanic.execute(...)`.
2. For `effect`, the map now contains `0.0` (a `Double`), not `"minecraft:poison"`.
3. `ApplyStatusMechanic` / `CrowdControlMechanic` / `AoeEffectMechanic` / `FieldAuraMechanic` / `AllyAuraMechanic` call `PotionEffectResolver.resolve(params.get("effect"))`.
4. `PotionEffectResolver.parseKey(0.0)` treats it as a legacy numeric ID; `0` is not in the `LEGACY_IDS` array → throws `IllegalArgumentException("Unknown potion effect key: 0.0")`.
5. The exception propagates out of `mechanic.execute` (no try/catch in `fireAbilities`) into the event handler, surfacing as an error each time the ability fires.

The same failure mode hits any other string parameter:
- `attribute: { constant: "minecraft:armor" }` → `ModifyAttributeMechanic`
- `material: { constant: "minecraft:shield" }` → `SetCooldownMechanic`

### Impact

Every bundled skill ability that uses a namespaced effect throws at runtime when triggered. Affected skills/abilities include, but are not limited to:

| Skill | Ability | Mechanic | Effect / param |
|---|---|---|---|
| light_weapons | flurry | `core:apply_status` | `minecraft:poison` |
| heavy_weapons | stun_impact / cleave | `core:apply_status` / `core:crowd_control` | `minecraft:slowness` / `minecraft:weakness` |
| one_handed | fencing | `core:apply_status` | `minecraft:slowness` |
| dual_wield | whirlwind / offhand_strike | `core:crowd_control` / `core:apply_status` | `minecraft:weakness` / `minecraft:poison` |
| throwing | trick_shot | `core:apply_status` | `minecraft:slowness` |
| archery | mark_target | `core:apply_status` | `minecraft:slowness` |
| unarmed | pressure_points | `core:apply_status` | `minecraft:weakness` |
| riding | cavalry_charge | `core:apply_status` | `minecraft:slowness` |
| bard | all auras | `core:ally_aura` | speed / strength / regeneration / resistance / haste |
| wizardry | frost_nova | `core:crowd_control` | `minecraft:slowness` |
| piety | holy_light / consecrate / divine_intervention / bless | `core:ally_aura` / `core:apply_status` | regeneration / strength / resistance / weakness |
| shields | shield_bash | `core:set_cooldown` | `material: minecraft:shield` |

### Why the existing tests miss it

- The mechanic unit tests (e.g. `ApplyStatusMechanicTest`) pass raw strings directly into the `params` map, bypassing `SkillManager.parseInlineEvaluator` and `SkillEventListener.evaluateParams`. They validate the resolvers, not the parse→evaluate pipeline.
- `SkillYamlValidationTest` (the Phase 4 sweep) validates the raw YAML for assertion 4 precisely because the parsed evaluator has already lost the string; it cannot detect this bug, by design.

### Proposed Fix Directions

1. **String-capable evaluator.** Add an evaluator that carries an `Object`/`String` (e.g. `ConstantValueEvaluator`) and have `parseInlineEvaluator` return it for non-numeric constants. `evaluateParams` must then emit the raw string (rather than `evaluate(...)`) for such evaluators, so `PotionEffectResolver` / `ModifyAttributeMechanic` / `SetCooldownMechanic` receive the string. This is the most faithful to the existing `ParameterEvaluator` contract (double `evaluate`) and keeps numeric params as doubles.
2. **Raw-parameter passthrough.** Store raw YAML parameter values alongside the evaluators on `MechanicEntry`, and have `evaluateParams` prefer the raw value for non-numeric entries.
3. **Special-case known string params.** Handle the finite set of string params (`effect`, `attribute`, `material`) in the parser. Least general but smallest change.

> Note: `ParameterEvaluator.evaluate(int, int)` returns `double` only. Any fix that routes strings through the evaluator must either widen the interface or carry the raw value out-of-band (options 1/2 above).
>
> Related work: the web GUI has the same string-constant failure in `SkillSerializer.parseEvaluator` (see ISSUE-103). Prefer a fix that can be shared or mirrored on both sides to avoid two divergent implementations.

## Verification & Definition of Done

- [x] A unit test proves `parseInlineEvaluator` preserves a string constant (e.g. `{ constant: "minecraft:poison" }`)
- [x] A unit test drives `SkillEventListener.evaluateParams` (or the equivalent wiring) and asserts the params map contains the string, not `0.0`
- [x] `ApplyStatusMechanic`-style execution with a namespaced `effect` resolves via `PotionEffectResolver` without throwing
- [x] All `./gradlew build && ./gradlew test` pass, including the `SkillYamlValidationTest` sweep
- [x] Runtime smoke check: each affected ability applies its effect without an `IllegalArgumentException` in the server log
- [x] Edge case handled: numeric constants (e.g. `{ constant: 3 }`) still parse to `ConstantEvaluator(3.0)` — no numeric regressions
