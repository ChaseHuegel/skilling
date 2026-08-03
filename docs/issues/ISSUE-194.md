# ISSUE-194: Fail-fast validation of string mechanic parameters at load, not on the event path

## Context & User Story
- **Goal:** As a server owner, I want a typo in a YAML mechanic parameter (e.g. `effect: "minecraft:poisn"`) to be rejected at skill load time with a clear error, so a malformed config never throws `IllegalArgumentException` inside an event handler and spams the error log mid-game.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Inputs:** Code review of `SkillManager`, `MechanicRegistry`, and `mechanic/impl/**`.

## Implementation Requirements
- [x] Add a load-time validation hook so string-valued mechanic parameters are resolved/validated when the skill YAML is parsed (per `src/AGENTS.md` §9 fail-fast) instead of inside `execute`.
  - Affected mechanics resolve their params at runtime today: `AllyAuraMechanic`, `AoeEffectMechanic`, `ApplyStatusMechanic`, `CrowdControlMechanic`, `FieldAuraMechanic` (`PotionEffectResolver.resolve`), `ModifyAttributeMechanic` (`resolveAttribute`), `SetCooldownMechanic` (throws on unknown `material`), `BlockParticlesMechanic` (silently swallows unknown `particle`).
- [x] Make the runtime resolution paths consistent: unknown values should be impossible after load (validated), so any residual runtime failure is a real bug, not a config typo.
- [x] Decide and apply a single behavior for unknown values (fail load vs. documented fallback); `BlockParticlesMechanic`'s silent no-op currently diverges from the throwing mechanics.
- [x] Consider a per-mechanic validation callback in `MechanicRegistry` (or a validation pass in `SkillManager.parseMechanics`) so custom addon mechanics can opt in the same way.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/PotionEffectResolver.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyAttributeMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/SetCooldownMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/BlockParticlesMechanic.java`
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/registry/MechanicRegistry.java` (if a validation hook is added)
- **Dependencies:** none.
- **Constraints:** `skilling-api` stays dependency-free. Existing valid configs keep byte-identical behavior. Fail-fast errors must surface through the existing `loadSkills` `IllegalArgumentException` path.
- **Note (resolution):** Added `MechanicValidator` (skilling-api) and a validator overload on `MechanicRegistry`; `SkillManager.parseMechanics` builds a constant-valued parameter map and calls `validate`. New `MechanicParamValidators` (main) validates `effect`/`attribute` via injected `Predicate<NamespacedKey>` key checks and `material`/`particle` via `Material.matchMaterial`/`Particle.valueOf`. `Skilling.registerBuiltins` wires the predicates to the live registries; until configured the validators skip, so the JUnit suite (where the live registry cannot initialize) stays deterministic. `BlockParticlesMechanic` now fails fast on an unknown particle instead of silently no-oping.

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [x] `./gradlew test` passes.
- [x] New tests: a skill YAML with an unknown `effect`/`attribute`/`material`/`particle` fails to load with a descriptive `IllegalArgumentException`; valid params load and execute unchanged.
- [x] Edge case handled: no `IllegalArgumentException` originates from within an event handler for these mechanics under a valid config.
