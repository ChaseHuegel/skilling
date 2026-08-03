# ISSUE-201: Fix `ModifyCraftOutputMechanic` shift-click overflow dropping bonus items

## Context & User Story
- **Goal:** As a player, I want a craft-output bonus on a shift-click batch to never lose bonus items, so overflow beyond a single stack is granted as additional stacks instead of silently capped away.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] Current behavior caps the modified `currentItem` at `maxStack` (`ModifyCraftOutputMechanic.java`), discarding any bonus overflow beyond one stack.
- [ ] Grant the full bonus: when the scaled result exceeds `maxStack`, place the remainder as an additional stack(s) in the player's inventory (or drop on the ground if inventory is full), matching the pattern in `ModifyFurnaceOutputMechanic`.
- [ ] Keep the per-recipe (not batch) bonus base for shift-clicks so the bonus is not computed on top of the whole batch.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ModifyCraftOutputMechanic.java`
- **Dependencies:** none.
- **Constraints:** Normal (non-shift) crafts keep current behavior. The bonus must not create items out of thin air beyond the configured multiplier; overflow placement must be deterministic.

## Verification & Definition of Done
- [ ] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [ ] `./gradlew test` passes.
- [ ] New/updated tests assert a shift-click batch whose scaled output exceeds `maxStack` yields the full bonus (extra stacks / drops), and that a normal craft still respects the cap only when the inventory cannot hold more.
