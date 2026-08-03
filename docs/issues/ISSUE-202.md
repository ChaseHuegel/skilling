# ISSUE-202: Validate mechanic parameter bounds at parse time

## Context & User Story
- **Goal:** As a server owner, I want out-of-range mechanic parameters (negative radius, negative chance, absurd durations) to be rejected at skill load, so they cannot cause runtime `IllegalArgumentException`s or undefined behavior inside event handlers.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] Define per-parameter bounds for the mechanics whose params are unbounded today and would otherwise flow into Bukkit APIs unchecked:
  - Radius params: `AoeEffectMechanic`, `FieldAuraMechanic`, `CrowdControlMechanic`, `KnockbackMechanic`, `AllyAuraMechanic` (the latter already clamps to `[0,32]`; negative values currently pass through unclamped on the others). Bukkit `getNearby*` rejects negative radii.
  - Chance params (0-100): `DodgeMechanic`, `BlockDamageMechanic`, `CancelDamageMechanic`, `AutoSmeltMechanic`, `FishingYieldMechanic`, `DurabilitySaveMechanic`, `ProjectileReturnMechanic`, `YieldMultiplierMechanic`, `ModifyTameChanceMechanic` (0-1).
  - Duration/`ticks` truncation: `SetCooldownMechanic` (`(int) ticks`) and duration-to-ticks conversions should validate non-negative and reasonable values.
- [x] Reject invalid values at parse time via the `SkillManager` load path (`IllegalArgumentException`), consistent with `src/AGENTS.md` §9 and ISSUE-194.
- [x] Handle negative radius on `AllyAuraMechanic` (currently `clampRadius` clamps negatives to 0 only after parse; ensure no runtime throw).

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`, the mechanics listed above in `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/`
- **Dependencies:** co-ordinates with ISSUE-194 (same load-time validation seam).
- **Constraints:** Defaults documented in each Javadoc stay unchanged. No behavior change for in-range configs.
- **Note (resolution):** Extended `MechanicParamValidators` with `radius` (non-negative), `chance(key, max)`, `nonNegative`, and `positive` checks, wired through the ISSUE-194 validator registrations. `ModifyTameChanceMechanic` is validated as strictly positive (its Javadoc documents multipliers above 1.0 as preserving tame success, so the ticket's "0-1" bound would reject valid configs). Bare YAML scalars are mangled to 0.0 by the evaluator parser, so bounds validation applies to `{ constant: ... }` blocks; level-scaled evaluators remain runtime-validated.

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [x] `./gradlew test` passes.
- [x] New tests assert out-of-range params (negative radius, chance > 100, etc.) fail skill load, and valid bounds load and execute.
