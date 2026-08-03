# ISSUE-206: Fix `AutoSmeltMechanic` collapsing multiple drop types into one stack

## Context & User Story
- **Goal:** As a player, I want auto-smelt to preserve every distinct drop type of a block, so a block that drops multiple item types is not reduced to a single smelted stack that loses items.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] `AutoSmeltMechanic` sums the amounts of ALL captured drops into one stack and retypes it to the smelted product (`AutoSmeltMechanic.java`), which is correct only for single-drop-type ores.
- [ ] Process each distinct drop type independently: smelt only the drop types with a mapping in `SMELT_MAP` (per-stack, per-type), sum by type, and emit one smelted stack per source type so no drop is lost or merged across types.
- [ ] Keep the existing behaviors: Silk-Touch bypass, `setDropItems(false)` suppression, Fortune-aware drops, and a single naturally-dropped bonus set.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AutoSmeltMechanic.java`
- **Dependencies:** none.
- **Constraints:** Single-type ores (iron, gold, copper, nether quartz, ancient debris) keep identical output; only multi-type blocks change.

## Verification & Definition of Done
- [ ] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [ ] `./gradlew test` passes.
- [ ] New/updated test asserts a block yielding multiple distinct drop types produces one smelted stack per mapped type with the correct total, and single-type ores still yield exactly one smelted stack.
