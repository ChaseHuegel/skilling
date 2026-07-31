# SKILL-DEVELOPMENT-PLAN — Phase Overview

**Source:** `SKILL-REVIEW.md` (the audit report)
**Goal:** Remediate all 32 bundled skills, implement 12 new engine pieces, update the Web GUI to parity, and synchronize all documentation.

---

## Phase Index

| Phase | Document | Focus | Priority | New Engine Pieces | Web Parity | Docs |
|-------|----------|-------|----------|-------------------|------------|------|
| 1 | `SKILL-DEVELOPMENT-PLAN_Phase_1.md` | Ability `trigger` field + P0 critical bug fixes | P0 | P2-1 (trigger field) | Trigger field UI support | Schema docs |
| 2 | `SKILL-DEVELOPMENT-PLAN_Phase_2.md` | Namespaced effect/attribute IDs, `core:ally_aura`, `state:equipped` | P1 | P2-5, P2-6, P2-11 | State-filter API endpoint, filter suggestions | Capabilities catalog |
| 3 | `SKILL-DEVELOPMENT-PLAN_Phase_3.md` | New mechanics & triggers | P3 | P2-2, P2-3, P2-4, P2-7, P2-8, P2-9, P2-10, P2-12 | Mechanic/trigger fallback lists | Capabilities catalog |
| 4 | `SKILL-DEVELOPMENT-PLAN_Phase_4.md` | Skill YAML remediation (all 32 redesigned files) | P0–P2 | *(consumes pieces from Phases 1–3)* | E2E fixtures | — |
| 5 | `SKILL-DEVELOPMENT-PLAN_Phase_5.md` | Documentation sweep + capabilities corrections | — | — | — | All docs updated |

---

## Ordering Rationale

Per `AGENTS.md` Issue Resolution Workflow and the review's prioritized fix order (§3.2):
- **Phase 1** must land first because the `trigger` field is the root-cause fix for C1/C2 (guardless-mechanic stacking) — all skill YAML remediation in Phase 4 depends on it existing.
- **Phase 2** lands before Phase 4 because several redesigned skills reference namespaced effect keys (`minecraft:poison`), `core:ally_aura`, and `state:equipped` — those engine pieces must exist before the YAMLs can be loaded.
- **Phase 3** lands before Phase 4 because the redesigned Dual Wield, Shields, Acrobatics, and Piety skills reference `core:offhand_strike`, `core:knockback`, `core:shield_disable`, `resurrect`, `elytra_glide`, `core:set_cooldown`, and `core:modify_attack_speed`.
- **Phase 4** applies all 32 redesigned YAMLs once the engine can load them.
- **Phase 5** is a final sweep to ensure every doc reflects the shipped state.

Within each phase, commits are broken into the smallest logical units that independently compile and pass tests, following `CONVENTIONS-COMMITS.md`.