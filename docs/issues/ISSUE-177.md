# ISSUE-177: Research & design a plan to revise XP sources for all skills (multi-source, 50-hour-to-100 target)

**Status:** Open
**Type:** Research
**Severity:** Medium (design study; produces the plan that subsequent `feat(skills)` tickets execute — no production code changes)

---

## Context & User Story

- **Goal:** As a content designer, I want a design plan that revises the XP sources for **all** bundled skills so that every skill is comprehensive — a primary source plus at least secondary and tertiary sources — with expanded tag matching and **reward amounts derived from a 50-hour-to-level-100 time target** for each skill.
- **Agent Role:** You are an expert game-content/backend design analyst executing this task (research and writing; no production code changes).

## Implementation Requirements

- [x] Catalog the current XP-source state of all 32 bundled skills (`src/main/resources/skills/*.yml`): trigger, filters, reward, and how many sources each skill has; flag single-source and narrow-grindability skills
- [x] Define the 50h target math against the current progression curve and recommend one approach: (a) keep `base_xp: 50, exponent: 2.5` (5,000,000 XP to 100 → **~1,667 XP/min sustained**) and compute per-source reward constants from action rates; (b) re-tune the progression curve so the 50h target yields sane per-action rewards; or (c) a hybrid — with justification, given the existing REPORT_XP-CURVE.md recommended ~500 h focused
- [x] For every skill, propose an expanded XP-source set (primary + at least secondary + tertiary) using distinct triggers with `target`/`state`/`tool` filters, cross-referencing the trigger and state-filter catalogs in `docs/users/capabilities.md`
- [x] Specify tag expansion: new custom tags in `src/main/resources/tags.yml` and the vanilla tags to reference (e.g. `#minecraft:undead`, `#minecraft:soul_fire_base_blocks`, holy/decorative block tags) to widen item/block/entity matching
- [x] Work the **piety** example fully: bone burying (ISSUE-176), killing undead, curing villagers, totem activations (`resurrect`), and placing "holy" blocks/decorations (`block_place`) — each with a computed reward constant
- [x] Produce the reward formula and a per-skill source table: skill → sources → trigger/filters → reward constant → actions-per-minute → derived XP/min (target ≈ 1,667 XP/min for the shared curve)
- [x] Document engine capability gaps the plan depends on (e.g. block-target filters on `player_interact` from ISSUE-176, `target_type` filtering on `entity_kill`/`EntityDeathEvent`, a villager-cure trigger if none exists) and list follow-up implementation tickets
- [x] Sanity-check the plan against milestone pacing (levels 25/50/75/100 ≈ 3%/18%/49%/100% of the journey) and reconcile with REPORT_XP-CURVE.md

## Technical Specifications & Context

- **Deliverable:** `docs/reports/REPORT_XP-SOURCE-DESIGN.md`
- **Inputs:**
  - `src/main/resources/skills/*.yml` (all 32 skills and their current sources)
  - `docs/reports/REPORT_XP-CURVE.md` (per-skill time-to-100 estimates, §2.3 action-rate table, §7.3 reward-design framework)
  - `docs/users/capabilities.md` (built-in trigger catalog incl. `entity_kill`, `resurrect`, `block_place`, `player_interact`; state filters incl. `target_type`, `dimension`, `biome`)
  - `src/main/resources/tags.yml` (current custom tags: `c:ores`, `c:logs`, `c:stone`, `c:herbs`, etc.)
  - `docs/issues/ISSUE-176.md` (bone burying — the piety source the user explicitly wants included)
  - `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java` and `engine/listener/SkillEventListener.java` (filter/trigger semantics the plan must target)
- **Constraints:** Research only — no production code changes in this ticket. Data-driven design (no hardcoded skills). Rewards must be derived from the 50h target, not hand-picked. Keep the plan executable as follow-up `feat(skills)` tickets.

### Key Questions to Resolve

1. **Curve vs. rewards.** At the current curve, 50 h to 100 means ~1,667 XP/min sustained (≈23 XP/sec) — far above today's fastest skill (~215 XP/min riding, ~388 h). Does the plan scale rewards ~8-10×, flatten the curve, or both? The report must decide and justify with math.
2. **Trigger coverage.** Which existing triggers map naturally to each skill's identity (e.g. `entity_kill` + `target_type` for undead)? Does the engine's `target_type` filter work on `EntityDeathEvent` (currently wired only to `EntityDamageByEntityEvent`, `Skilling.java:424-431`), and does a villager-cure event/trigger exist in the Paper API?
3. **Tag strategy.** Which vanilla tags are version-stable for entity and block matching, and which new `c:` tags (e.g. `c:holy_blocks`, `c:undead`, `c:decorations`) should be added to widen matching?

## Verification & Definition of Done

- [x] Report delivered at `docs/reports/REPORT_XP-SOURCE-DESIGN.md`
- [x] Report covers **all 32 skills** with an expanded (primary + secondary + tertiary) source set
- [x] Report contains the 50h reward math and a per-source reward-constant table (skill → trigger → filter → reward → XP/min)
- [x] Report includes the fully-worked **piety** example (bone burying via ISSUE-176, undead kills, villager curing, totem activations, holy-block placement)
- [x] Report lists the `tags.yml` additions and the engine-capability gaps + follow-up tickets
- [x] Report reconciles with REPORT_XP-CURVE.md (milestone pacing, action-rate table) and renders as clean Markdown
- [x] No production code changed by this ticket
