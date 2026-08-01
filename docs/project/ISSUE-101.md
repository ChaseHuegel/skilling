# ISSUE-101 — `parseInlineEvaluator` discards string-valued parameters

**Status:** Open
**Type:** Bug
**Severity:** High (runtime exception on ability activation for every bundled status/aura ability)
**Discovered:** Phase 4 YAML remediation (all bundled skills now use namespaced effect keys)

---

## Summary

`SkillManager.parseInlineEvaluator` only preserves **numeric** `constant` values. Any
string-valued parameter — e.g. `effect: { constant: "minecraft:poison" }` — is silently
coerced to `ConstantEvaluator(0.0)` during YAML parsing. The namespaced key never
survives to runtime, so mechanics that consume it receive `0.0` instead of the string
and fail fast with an `IllegalArgumentException`.

This is the engine-side counterpart of the Phase 2 work (commits `4537121`,
`4c60d0f`) which added namespaced-key support to `PotionEffectResolver` and
`ModifyAttributeMechanic` — the resolvers accept strings, but the parsing layer was
never wired to keep them.

## Root Cause

`SkillManager.parseInlineEvaluator` (`src/main/java/.../engine/SkillManager.java`:

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

When `val` is a `String` (e.g. `"minecraft:poison"`), `castMap(val)` returns
`Map.of()` (empty), so `nested.getOrDefault("value", 0.0)` resolves to `0.0`. The
string is thrown away with no warning. Only `Number` constants are handled
correctly.

## Runtime Path

1. `SkillEventListener.fireAbilities` → `evaluateParams(...)` evaluates every
   parameter evaluator to a `double` (`SkillEventListener.java:660`) and builds the
   `Map<String, Object>` handed to `mechanic.execute(...)`.
2. For `effect`, the map now contains `0.0` (a `Double`), not
   `"minecraft:poison"`.
3. `ApplyStatusMechanic` / `CrowdControlMechanic` / `AoeEffectMechanic` /
   `FieldAuraMechanic` / `AllyAuraMechanic` call
   `PotionEffectResolver.resolve(params.get("effect"))`.
4. `PotionEffectResolver.parseKey(0.0)` treats it as a legacy numeric ID; `0` is
   not in the `LEGACY_IDS` array → throws
   `IllegalArgumentException("Unknown potion effect key: 0.0")`.
5. The exception propagates out of `mechanic.execute` (no try/catch in
   `fireAbilities`) into the event handler, surfacing as an error each time the
   ability fires.

The same failure mode hits any other string parameter:
- `attribute: { constant: "minecraft:armor" }` → `ModifyAttributeMechanic`
- `material: { constant: "minecraft:shield" }` → `SetCooldownMechanic`

## Verification / Reproduction

A unit-level probe confirms the coercion (parsed through the real `SkillManager`):

```
parameters: { effect: { constant: "minecraft:poison" } }
```
parses to:
```
effect=ConstantEvaluator[value=0.0]
```

## Impact

Every bundled skill ability that uses a namespaced effect (P2-11) throws at runtime
when triggered. Affected skills/abilities include, but are not limited to:

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

## Why the existing tests miss it

- The mechanic unit tests (e.g. `ApplyStatusMechanicTest`) pass **raw strings**
  directly into the `params` map, bypassing `SkillManager.parseInlineEvaluator` and
  `SkillEventListener.evaluateParams`. They validate the resolvers, not the
  parse→evaluate pipeline.
- `SkillYamlValidationTest` (the Phase 4 sweep) validates the **raw YAML** for
  assertion 4 precisely because the parsed evaluator has already lost the string;
  it cannot detect this bug, by design.

## Proposed Fix Directions

1. **String-capable evaluator.** Add an evaluator that carries an `Object`/`String`
   (e.g. `ConstantValueEvaluator`) and have `parseInlineEvaluator` return it for
   non-numeric constants. `evaluateParams` must then emit the raw string (rather
   than `evaluate(...)`) for such evaluators, so `PotionEffectResolver` /
   `ModifyAttributeMechanic` / `SetCooldownMechanic` receive the string. This is the
   most faithful to the existing `ParameterEvaluator` contract (double `evaluate`)
   and keeps numeric params as doubles.
2. **Raw-parameter passthrough.** Store raw YAML parameter values alongside the
   evaluators on `MechanicEntry`, and have `evaluateParams` prefer the raw value for
   non-numeric entries.
3. **Special-case known string params.** Handle the finite set of string params
   (`effect`, `attribute`, `material`) in the parser. Least general but smallest
   change.

> Note: `ParameterEvaluator.evaluate(int, int)` returns `double` only. Any fix that
> routes strings through the evaluator must either widen the interface or carry the
> raw value out-of-band (options 1/2 above).

## Acceptance Criteria

- [ ] A unit test proves `parseInlineEvaluator` preserves a string constant
      (e.g. `{ constant: "minecraft:poison" }`).
- [ ] A unit test drives `SkillEventListener.evaluateParams` (or the equivalent
      wiring) and asserts the params map contains the string, not `0.0`.
- [ ] `ApplyStatusMechanic`-style execution with a namespaced `effect` resolves via
      `PotionEffectResolver` without throwing.
- [ ] All `./gradlew build && ./gradlew test` pass, including the `SkillYamlValidationTest`
      sweep.
- [ ] Runtime smoke check: each affected ability applies its effect without an
      `IllegalArgumentException` in the server log.
