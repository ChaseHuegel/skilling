# ISSUE-231: Attribute-modifier refresh truncates the buff duration

## Context & User Story
- **Goal:** As a player, I want re-activating a passive/ability that refreshes its attribute buff to keep the full duration, not cut it back to the previous expiry.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** High — the documented "replace-not-stack, refresh duration" behavior (`AttributeModifierHelper` Javadoc) is broken for every UUID-based attribute mechanic.

## Implementation Requirements
- [ ] `AttributeModifierHelper.applyTransient` (`AttributeModifierHelper.java:69-74`) schedules `inst.removeModifier(modifier)` with the old `AttributeModifier` instance captured at scheduling time, but `removeModifier` matches by UUID. A re-activation replaces the modifier with a new instance under the same UUID; when the stale task fires it removes the replacement early, truncating the refreshed duration. Cancel/replace the pending removal task on refresh (track the scheduled task per player+UUID) instead of capturing the instance.
- [ ] Cover all six consumers: `ModifyAttributeMechanic`, `ArmorBonusMechanic`, `KnockbackResistMechanic`, `ModifyAttackSpeedMechanic`, `ModifyJumpMechanic`, `SpeedBonusMechanic`.
- [ ] Add a unit test that re-applies the same buff twice with the same `uuid` and asserts the modifier lives for the full second duration.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AttributeModifierHelper.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/{ModifyAttribute,ArmorBonus,KnockbackResist,ModifyAttackSpeed,ModifyJump,SpeedBonus}Mechanic.java`
  - `src/test/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AttributeModifierHelperTest.java` (new)
- **Dependencies:** none.
- **Constraints:** Keep the stable-per-ability `uuid` behavior; greenfield, no compat obligation.

## Verification & Definition of Done
- [ ] Re-applying a buff with the same `uuid` keeps it for the full new duration.
- [ ] No removal task leaks for a player who quits mid-buff (Paper's entity scheduler cancels on despawn — verify and document).
- [ ] `./gradlew build` and `./gradlew test` pass.
