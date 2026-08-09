# ISSUE-273: Web — Allow and Preserve String Values in Constant Parameter Fields

## Context & User Story
- **Goal:** As a server admin, I want to enter string values into "constant" parameter fields in the web GUI (e.g. `minecraft:poison`, `#c:ores`, an effect key) and have them survive saving, so I can configure string-typed mechanic parameters from the GUI instead of editing raw yml.
- **Agent Role:** You are an expert frontend engineer executing this task. The backend already round-trips string constants end-to-end; this ticket removes the frontend blocker that makes them read-only.

## Implementation Requirements
- [x] Replace the number-only constant input so it accepts and preserves raw text. The blocker is `EvaluatorParameter.vue` (constant branch, lines 122-131), which binds the "Value" field to `DecimalInput`, and `DecimalInput.vue` (`onInput`, lines 25-32) only emits `parseFloat` results, so any non-numeric input never updates the model.
- [x] Preserve the runtime value type: a numeric constant stays a number and a non-numeric constant stays a string, so the serialized yml stays clean (`constant: 2` vs `constant: "minecraft:poison"`) and numeric mechanic parameters keep their numeric behavior.
- [x] Do not regress numeric fields: `linear`, `milestones`, and `polynomial` evaluator sub-fields (base/step/min/max, milestone level/value, base_xp/exponent) must keep number-only input via `DecimalInput`.
- [x] Handle the ability cooldown exception: the cooldown field uses `EvaluatorParameter` with types restricted to `['constant', 'linear', 'milestones']` (`AbilitiesSection.vue` line ~546) and is numeric by contract (`utils/cooldown.ts` `cooldownToNumber` collapses constants to numbers). A string constant must not be allowed to silently break the cooldown. Decide and implement one consistent approach, e.g. keep the cooldown field number-only while enabling text for mechanic parameter constants and XP-source reward constants, or validate on save that the cooldown constant is numeric. Document the choice.
- [x] Ensure the save round-trip survives: the editor already sends the evaluator object verbatim through `formAbilityToApi` (`SkillEditorPage.vue` lines 386-406), and the backend (`SkillSerializer`, `EvaluatorDTO.params` as `Map<String, Object>`) already emits/parses string constants. Verify a string constant saved via the GUI is present verbatim after Apply & Reload.
- [x] Update the frontend evaluator typing so a constant `value` is `number | string` where it feeds the mechanics/XP-source evaluator (currently loose `Record<string, any>`; type only what type-checks, per the dead-code/typecheck build).

## Technical Specifications & Context
- **Target Files:**
  - `web/frontend/src/components/common/EvaluatorParameter.vue` (constant branch lines 122-131; shared by `MechanicsEditor.vue` line ~227, `XpSourcesSection.vue` line ~221, `AbilitiesSection.vue` line ~546)
  - `web/frontend/src/components/common/DecimalInput.vue` (number-only emit, lines 25-32)
  - `web/frontend/src/components/skills/AbilitiesSection.vue` (cooldown call site, line ~546; empty constant default `{ type: 'constant', params: { value: 0 } }`)
  - `web/frontend/src/components/skills/MechanicsEditor.vue` (param rows default constant, line ~132)
  - `web/frontend/src/components/skills/XpSourcesSection.vue` (reward constant default, line ~115)
- **Dependencies:** none. No backend change is required: `ConstantEvaluator` has a `stringValue` variant, `SkillSerializer` round-trips string constants (parse lines ~423-437, emit lines ~475-491), and `MechanicParamValidators` already validates string-typed params (effect, attribute, material, particle, sound) at load.
- **Constraints:** Per `web/AGENTS.md`, use the Composition API, no `v-html`, and `npm run build` (vue-tsc) must pass with `noUnusedLocals`/`noUnusedParameters`. The GUI must not produce a skill that fails load-time validation; rely on the existing backend validators to reject genuinely invalid strings.

## Verification & Definition of Done
- [x] `cd web/frontend && npm run build` passes.
- [x] A mechanic parameter using a string constant (e.g. `material: { constant: "minecraft:poison" }`) can be typed in the GUI, saves, and survives Apply & Reload verbatim in the skill yml. (Backend round-trip already covered by `SkillSerializerEvaluatorConstantTest`; the editor now emits the string verbatim through `formAbilityToApi`.)
- [x] A numeric constant still saves as a number, and `linear`/`milestones`/`polynomial` fields remain number-only.
- [x] The ability cooldown field stays numeric-safe (no way to save a string cooldown that collapses or errors).
- [x] Round-trip check: open a skill containing a string constant, edit another field, save, and confirm the string constant is not lost or coerced.
