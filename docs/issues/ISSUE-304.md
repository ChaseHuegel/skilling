# ISSUE-304: Research — Minecraft gameplay coverage audit and new-skill/ability proposals

## Context & User Story
- **Goal:** As the design lead, I want a research report that audits which Minecraft gameplay loops the bundled skills already cover, identifies the uncovered or under-utilized loops (archaeology, redstone, composting, trading, beekeeping, exploration, crafting recipes, and others), and proposes concrete XP sources and abilities — for existing skills, folded skills, or new skills — so the skill set covers the whole game with a tight Vanilla+ feel.
- **Agent Role:** You are an expert game-design researcher and Paper-engine engineer writing a deliverable report only. No production code changes.

## Implementation Requirements
- [x] Produce `docs/reports/REPORT_SKILL-COVERAGE.md` with the owning ticket and date in the header and a cross-link to this ticket.
- [x] Audit every bundled skill's XP sources and ability themes against Minecraft's gameplay areas.
- [x] Verify the Paper/Bukkit events needed for each proposed loop (player attribution, event-driven, O(1)).
- [x] Propose: new skills, integrations into existing skills, folding candidates, and cross-cutting engine additions (triggers, mechanics, recipe gating).
- [x] For each proposal, give XP sources and ability ideas grounded in existing mechanics or minimal new mechanics, with Vanilla+ restraint and anti-grind notes.
- [x] Recommend a priority roadmap.

## Technical Specifications & Context
- **Target Files:** `docs/reports/REPORT_SKILL-COVERAGE.md` (new), `docs/issues/INDEX.md` (research ticket entry).
- **Dependencies:** Bundle skill inventory (`src/main/resources/skills/*.yml`), capabilities catalog (`docs/users/capabilities.md`), engine event/trigger surface (`src/main/java/.../engine/trigger/impl`), Paper API event inventory.
- **Constraints:** Report-only ticket (no production code). Prose in STE per `docs/AGENTS.md`. Design pillars from `docs/dev/SKILL-DESIGN-FRAMEWORK.md` apply to every proposal.

## Verification & Definition of Done
- [x] Report renders as clean Markdown and cross-links to `../issues/ISSUE-304.md`.
- [x] Every proposed trigger/mechanic is checked against a real Paper/Bukkit event or an existing engine capability.
- [x] Anti-grind and player-attribution risks are called out for each event-driven proposal.
- [x] INDEX.md carries the research ticket under Backlog > Research.