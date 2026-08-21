# ISSUE-2: Excavation skill redesign + level-scalable exhaustion cost

## Context & User Story
- **Goal:** As a server owner, I want the bundled excavation skill to have a coherent, non-overlapping six-milestone progression with a grindable XP loop and archaeology identity, so that every ability stays useful from its unlock through level 100.
- **Agent Role:** You are an expert backend + content engineer redesigning the bundled `excavation.yml` and adding one small engine capability it needs.

## Implementation Requirements
- [x] Make `exhaustion.amount` and `exhaustion.minimum` level-scalable `ParameterEvaluator`s (mirroring `cooldown`/`durability`), so a hunger cost can sub-scale or flatten to zero between the unlock level and level 100.
- [x] Update the web layer to round-trip evaluator exhaustion (`SkillSerializer`, `SkillDetailDTO.ExhaustionDTO`, `RequirementsDTODeserializer`).
- [x] Rebalance excavation XP sources: keep `block_break #c:excavatable` as the main grind, remove the cheap/spammable shovel-craft source entirely, and keep the infrequent archeology sources (brush, suspicious break, decorated pot) high-value.
- [x] Rewrite excavation abilities into six distinct, non-overlapping milestones:
  - L1 `dig_double` (yield, primary scalar)
  - L15 `shovel_strike` (`core:modify_damage` on shovels, making them a viable weapon)
  - L25 `wide_sweep` (`core:area_harvest`, sneak-mine, hunger-only gate, no cooldown, exhaustion flattened to free at 100)
  - L50 `relic_hunter` (yield on `#c:suspicious_blocks`, the archaeology identity)
  - L75 `earthen_guard` (armor-bonus while holding a shovel, matching the Mining/Woodcutting L75 synergy slot)
  - L100 `excavator` (no-op display conveyer for the Wide Sweep transformation; no mechanic, so it cannot stack)
- [x] Add unit tests for level-scaled exhaustion (check flatten-to-free at max level, consume-nothing at zero amount).
- [x] Update `docs/dev/template-skill.yml` and `docs/users/capabilities.md` for exhaustion evaluator support.

## Technical Specifications & Context
- **Target Files:** `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/SkillDefinition.java`, `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java`, `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java`, `src/main/java/io/github/chasehuegel/skilling/web/dto/{SkillSerializer,SkillDetailDTO,RequirementsDTODeserializer}.java`, `src/main/resources/skills/excavation.yml`, `src/test/java/io/github/chasehuegel/skilling/requirements/RequirementEngineTest.java`, and the two docs files above.
- **Dependencies:** None new. Reuses `core:modify_damage`, `core:area_harvest`, `core:yield_multiplier`, `core:armor_bonus`, and the existing `MilestoneEvaluator`.
- **Constraints:** Follows the 6-step ability audit in `SKILL-DESIGN-FRAMEWORK.md` and the vanilla+ cooldown/hunger guidelines in the design review.

## Verification & Definition of Done
- [x] `./gradlew build` passes.
- [x] `./gradlew test` passes, including new `RequirementEngineTest` cases for evaluator exhaustion and updated `AcrobaticsSkillYamlTest` / `ArcherySkillYamlTest`.
- [x] `cd web/frontend && npm run build` passes.
- [x] Edge case handled: a zero exhaustion `minimum` lifts the hunger gate entirely; a zero `amount` consumes no hunger at the capstone level; the L100 `excavator` carry no mechanic, so it can never double-stack with `wide_sweep`.
- [x] Docs updated: `docs/dev/template-skill.yml` and `docs/users/capabilities.md`.