# ISSUE-186: Research — design plan for visual polish and presentation cohesion

## Context & User Story
- **Goal:** As a server owner, I want the plugin to look cohesive and polished at a premium-plugin standard, with a consistent visual language across the GUI, chat, titles, action bars, and tooltips.
- **Agent Role:** You are an expert UI/UX + backend (Paper) engineer producing a design plan — no production code changes.

This ticket produces a comprehensive design plan (research report) that catalogs visual improvements and sets a direction for a polished, refined presentation. It should be detailed enough to drive follow-up implementation tickets.

## Implementation Requirements
- [ ] Inventory every current presentation surface and note where each is implemented:
  - Skill chest GUI: `SkillMenuBuilder` (skill icons, tooltip XP bars, page icons, nav arrows, filler panes, titles, locked states), `gui.yml` layout config
  - Feedback: `LevelUpDispatcher` (title messages, boss bar, action bar, fireworks), `FanfareDispatcher` (chat/action bar messages, sounds, particles), `FeedbackDebouncer` messages
  - Bundled skill YAML feedback/lore text (colors, message formats)
- [ ] Propose replacing the tooltip XP bar characters — currently `|` and `.` in `SkillMenuBuilder.buildSkillLore()` (e.g., `[|||||.....]`) — with block-like characters (e.g., `█`/`░` or Unicode blocks), including the exact mapping, font/render considerations, and any color handling (filled vs empty segments).
- [ ] Propose adding a close button to the skill GUI: placement per page layout (gui.yml), wiring through `UIProtectionListener` navigation, poison-pill tagging, and interaction with the pagination nav row.
- [ ] Define a cohesive color palette shared across the skill GUI, chat messages, title messages, action bar, and tooltips, and map which color is used where; identify any config surfaces needed to let admins tune it.
- [ ] Catalog remaining polish opportunities with rationale and rough effort/priority (e.g., inventory title/naming consistency, item display names, level-up fanfare refinement, page indicator styling, locked-skill presentation, feedback debouncing copy).
- [ ] Deliver the report to `docs/reports/REPORT_VISUAL-POLISH.md` following the `REPORT_<TOPIC>.md` pattern, with the owning ticket and date in the header and a cross-link back to this ticket.

## Technical Specifications & Context
- **Target Files (read-only for research):**
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/SkillMenuBuilder.java` (`buildSkillLore` bar at lines 211-225)
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/UIProtectionListener.java` (navigation)
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/GuiLayoutConfig.java`, `src/main/resources/gui.yml`
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/LevelUpDispatcher.java`, `FanfareDispatcher.java`, `FeedbackDebouncer.java`
  - `src/main/resources/skills/**` (bundled feedback/lore styling)
  - `docs/reports/REPORT_XP-CURVE.md` / `REPORT_DUALWIELD-API.md` as report format references
- **Dependencies:** none (research only).
- **Constraints:**
  - Research ticket: no production code changes; deliverable is the report.
  - Each proposed change must reference the file/config location so follow-up implementation tickets are actionable.
  - Prefer solutions that keep everything configurable (gui.yml / skill YAML) and consistent with the plugin's data-driven architecture (no new hard-coded skill-specifics).

## Verification & Definition of Done
- [ ] Report exists at `docs/reports/REPORT_VISUAL-POLISH.md` with owning ticket + date header and a cross-link back
- [ ] Report covers all Implementation Requirements (surface inventory, XP-bar block characters, GUI close button, cohesive color palette, extra polish catalog with effort/priority)
- [ ] Every proposed change is actionable (location + rationale) for follow-up implementation tickets
- [ ] No production code changes
