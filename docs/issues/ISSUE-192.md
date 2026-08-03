# ISSUE-192: Define and enforce consistent cost/cooldown consumption for chance-based mechanics

## Context & User Story
- **Goal:** As a player, I want a failed chance roll on an ability (dodge, block, auto-smelt) to be treated consistently with a successful one, so that RNG abilities cannot be retried for free and the cost semantics do not differ between mechanics.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Inputs:** Code review of `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/**` and `SkillEventListener#fireAbilities`.

## Implementation Requirements
- [x] Pick a single, documented rule for when an ability activation consumes its cost and applies its cooldown, and apply it uniformly. Recommended: an activation attempt (requirements check passes) counts as an action — every mechanic that reaches its RNG/condition step returns `true` so `fireAbilities` consumes once; only mechanics that never acted (wrong event type, missing target, inapplicable state) return `false`.
- [x] Reconcile the current inconsistency in `fireAbilities` (`anyExecuted` gating in `SkillEventListener.java`):
  - Mechanics that return `false` on a failed roll → free retries: `DodgeMechanic`, `BlockDamageMechanic`, `CancelDamageMechanic`, `AutoSmeltMechanic`, `FishingYieldMechanic`, `DurabilitySaveMechanic`, `ProjectileReturnMechanic`.
  - Mechanics that return `true` even on a failed roll → always consume: `YieldMultiplierMechanic`, `ModifyCraftOutputMechanic`, `ModifyFurnaceOutputMechanic`.
- [x] Update the `SkillMechanic#execute` Javadoc contract to state the rule explicitly.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` (fireAbilities consume gating)
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/{Dodge,BlockDamage,CancelDamage,AutoSmelt,FishingYield,DurabilitySave,ProjectileReturn,YieldMultiplier,ModifyCraftOutput,ModifyFurnaceOutput}Mechanic.java`
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/mechanic/SkillMechanic.java`
  - `docs/users/capabilities.md` (document the consumption rule)
- **Dependencies:** none.
- **Constraints:** The "no-op ability must not spend its cost" guarantee (consumption only when a mechanic acted) stays intact; only the RNG/attempt classification changes. `./gradlew build` is currently blocked by pre-existing `ProjectileHitTriggerTest`/`ProjectileReturnMechanicTest` failures (ISSUE-190) — treat those as out of scope.
- **Note (resolution):** The shared roll lives in `BaseDamageCancelMechanic` (dodge/block/cancel). Chance mechanics now return `true` once they reach the roll; `AutoSmeltMechanic` and `ProjectileReturnMechanic` were reordered so the roll happens only after their applicability guards. A `@VisibleForTesting` `randomSource` seam (documented in each class, mirroring `ModifyTameChanceMechanic`) enables deterministic failed-roll tests. The existing `mechanic/AutoSmeltMechanicTest` registry mock was hardened to also stub `getRegistry(Class)` — `Registry.<clinit>` resolves legacy registries through that overload, so without it `Enchantment.SILK_TOUCH` init is order-dependent.

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [x] `./gradlew test` passes.
- [x] New/updated unit tests assert that a failed roll on a chance mechanic consumes the cooldown/cost once, and that a true no-op (wrong event type) does not.
- [x] Edge case handled: multiple mechanics on one ability still consume exactly once per activation.
