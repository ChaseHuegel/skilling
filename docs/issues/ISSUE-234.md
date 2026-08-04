# ISSUE-234: Enforce mechanic registered parameter lists at load (missing/string-valued params fail at runtime)

## Context & User Story
- **Goal:** As a skill author, I want a typo'd or missing mechanic parameter (e.g. a missing `effect`) rejected at load time, so a broken ability fails fast instead of throwing on every activation and spamming warnings.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** High — `MechanicRegistry` param lists are purely informational and never enforced for presence, and the load validators skip absent keys, so a config missing `effect`/`attribute` passes load then throws `IllegalArgumentException` per activation (and quoted numerics like `duration: "3"` throw `ClassCastException` from `(Number)` casts). The per-mechanic catch in `SkillEventListener` (`:588-598`) turns each into WARNING spam while the ability never consumes cost/cooldown and can be spammed free.

## Implementation Requirements
- [ ] Enforce the registered param list at parse time in `SkillManager.parseMechanics` (`SkillManager.java:477-532`): reject mechanic entries that supply parameters not in the registered list, and reject **missing required parameters** where the impl/validator requires them (the registry currently never validates presence).
- [ ] Reject string-valued parameters where a `Number` is required at load, so quoted numerics fail fast instead of `ClassCastException` at runtime (mirror the fail-fast behavior of `MechanicParamValidators.number`, but reject instead of skip).
- [ ] Reconcile the registered lists with what each impl actually reads: `core:shield_disable` registers `ticks` but reads `target`; `core:speed_bonus` registers `multiplier` but reads `duration`/`uuid`; `modify_attack_speed`/`modify_jump`/`armor_bonus`/`knockback_resist`/`modify_attribute` read `uuid` not listed; `aoe_effect`/`field_aura`/`crowd_control` read `targets` not listed. Decide required-vs-optional per param and enforce.
- [ ] Add tests proving a missing `effect` and a string-valued `duration` both fail at load, not at activation.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java` (registration param lists)
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/registry/MechanicRegistry.java` (presence/type enforcement API)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/MechanicParamValidators.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/SkillManagerMechanicParamValidationTest.java`
- **Dependencies:** none.
- **Constraints:** Follow the fail-fast convention in `src/AGENTS.md` §9. Reconcile the param lists in the same change so legitimately-used params (e.g. `uuid`, `targets`) are not rejected.

## Verification & Definition of Done
- [ ] A missing `effect`, missing `attribute`, and string-valued numeric all fail during `loadSkills`.
- [ ] All bundled skill YAML still parses after the list reconciliation.
- [ ] `./gradlew build` and `./gradlew test` pass.
