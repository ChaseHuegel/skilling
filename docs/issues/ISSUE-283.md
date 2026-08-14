# ISSUE-283: AttributeModifierHelper applies a modifier before scheduling removal, leaking a permanent stacking buff

## Context & User Story
- **Goal:** As a skill author, I want a level-scaled `duration` that evaluates to zero or negative to be a no-op, not to leave an attribute modifier that is never removed. `AttributeModifierHelper.applyTransient` calls `addTransientModifier` before `player.getScheduler().runDelayed(...)`; a non-positive `duration` makes `runDelayed` throw, the ability is marked failed, but the already-added modifier persists. With a random UUID per activation, repeated activations stack the buff without bound.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] Clamp `durationSeconds` to at least 0 and only apply the modifier when `durationSeconds > 0`.
- [ ] Ensure the removal is scheduled before or atomically with the apply so the modifier can never outlive its intent.
- [ ] Where feasible, run the per-mechanic parameter validators on parsed (possibly level-scaled) evaluator output, not only on constant parameters.
- [ ] Add a unit test: a negative and a zero `duration` produce no attribute modifier at all.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AttributeModifierHelper.java:89-104`, `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyAttributeMechanic.java:50`, `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/MechanicParamValidators.java` (load-time `nonNegative` validation at `SkillManager.java:617-621` only sees constant params).
- **Dependencies:** None.
- **Constraints:** Keep the transient-modifier semantics for positive durations unchanged.

## Verification & Definition of Done
- [ ] New tests cover negative, zero, and positive durations.
- [ ] `./gradlew test` and `./gradlew build` pass.
- [ ] Edge case handled: repeated activations of a zero-duration buff never accumulate modifiers.
