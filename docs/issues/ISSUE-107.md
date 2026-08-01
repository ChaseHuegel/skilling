# ISSUE-107: Produce an XP-curve balance research report (REPORT_XP-CURVE.md)

**Status:** Open
**Type:** Research
**Severity:** Medium (content balance; informs the skill-design framework)

---

## Context & User Story

- **Goal:** As a content designer, I want a data-backed evaluation of the bundled skills' XP curves and rewards, so that I can adjust them toward a consistent and fair time investment across all skills.
- **Agent Role:** You are an expert game-content analyst executing this task (research and writing; no production code changes).

## Implementation Requirements

- [x] Evaluate the XP curves and XP source rewards of every bundled skill in `src/main/resources/skills/`
- [x] Estimate the time to reach level 100 for each skill, with the assumptions and methodology explained
- [x] Assess whether the time investment is consistent, fair, and consistent across all skills
- [x] Research comparable RPG skill plugins (e.g. AuraSkills, mcMMO) and skilling games (e.g. RuneScape, Valheim) as an informed baseline for comparison
- [x] Produce suggested adjustments and a reusable framework for designing XP source rewards
- [x] Deliver the full report as `docs/issues/REPORT_XP-CURVE.md`

## Technical Specifications & Context

- **Target Files:**
  - `docs/issues/REPORT_XP-CURVE.md` (deliverable)
  - Inputs: `src/main/resources/skills/*.yml` (all bundled skills), `docs/dev/SKILL-DESIGN-FRAMEWORK.md` (dual-layer scaling curves, milestone template)
- **Dependencies:** The skill YAML schema (progression curves: `polynomial`/`linear`/`milestones`, and `xpSources` rewards) is documented in `docs/dev/template-skill.yml` and `docs/users/creating-skills.md`. Evaluation should use the real evaluator math (`docs/dev/REQUIREMENTS.md`).
- **Constraints:** Read-only research; do not modify skill YAML in this issue (adjustments are suggested, not applied). Use the comparison games' publicly documented progression behavior; no fabricated numbers. If the report recommends YAML changes, list them as concrete proposals for follow-up issues.

## Verification & Definition of Done

- [x] `docs/issues/REPORT_XP-CURVE.md` exists and documents the methodology (assumptions, rates, hours-per-level math)
- [x] Every bundled skill has a time-to-100 estimate with reasoning
- [x] A comparison section covers at least AuraSkills/mcMMO and one of RuneScape/Valheim
- [x] Concrete, prioritized suggestions and a framework for designing XP source rewards are included
- [x] Report renders as clean Markdown with valid cross-references; cross-references to skill YAML use real file paths
