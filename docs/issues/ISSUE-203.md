# ISSUE-203: Fix off-by-one in the exhaustion (hunger) requirement check

## Context & User Story
- **Goal:** As a player, I want an ability with `exhaustion.minimum` to activate when my food level equals the minimum, so the configured minimum is inclusive as documented rather than requiring strictly more food.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] `RequirementEngine#check` currently fails the exhaustion gate when `player.getFoodLevel() <= exhaustion.minimum()` (`RequirementEngine.java`). If `minimum` is documented as the minimum required food level, it should be inclusive: fail only when `foodLevel < minimum`.
- [x] Confirm the intended semantics against `docs/dev/template-skill.yml` / `docs/users/creating-skills.md` and update the docs if the current wording already implies inclusive.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java`, `docs/users/creating-skills.md`, `docs/dev/template-skill.yml`
- **Dependencies:** none.
- **Constraints:** This is a small, deliberate semantics fix; document the chosen boundary.
- **Note (resolution):** Changed the gate to `foodLevel < minimum` (inclusive). Docs already implied inclusivity ("3+ hunger" lore, "minimum hunger level required"); both `creating-skills.md` and `template-skill.yml` now state the inclusive boundary explicitly.

## Verification & Definition of Done
- [x] `./gradlew build` passes (excluding the pre-existing ISSUE-190 failures).
- [x] `./gradlew test` passes.
- [x] New/updated test asserts the exhaustion check passes when `foodLevel == minimum` and fails when `foodLevel < minimum`.
