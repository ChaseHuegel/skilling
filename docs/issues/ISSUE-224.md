# ISSUE-224: Web GUI layout round-trip drops per-page `gui_title`

## Context & User Story
- **Goal:** As a server admin, I want the per-page inventory title override (`gui_title`) in `gui.yml` preserved when I view or save the layout through the web GUI.
- **Agent Role:** You are an expert full-stack engineer executing this task.

## Implementation Requirements
- [x] `GuiLayoutDTO.GuiPageDTO` must carry the per-page `gui_title` (the engine reads it at `GuiLayoutConfig.java:81` and it overrides the inventory title via `GuiPage.displayTitle()`, `GuiPage.java:41-43`).
- [x] `GuiLayoutSerializer.parseLegacyFormat` (lines 98-111) must parse `gui_title`; `serialize` (lines 176-195) must write it. Today neither touches it, so a server-authored `gui_title` is silently dropped on the next web save and the page reverts to its `title`.
- [x] The new-format parser (`parseNewFormat`) and frontend `GuiPageDTO` (`stores/gui-layout.ts:5-18`) should also carry `gui_title` for consistency.
- [x] Add a round-trip regression test: parse a legacy-format gui.yml with `gui_title`, serialize, and re-parse — the `gui_title` value survives.
- [x] Optionally expose the field in the layout editor (page rename dialog) so admins can edit it; if not exposed, document that it is preserved read-only.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/GuiLayoutDTO.java`
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/GuiLayoutSerializer.java`
  - `web/frontend/src/stores/gui-layout.ts`
  - `web/frontend/src/views/GuiLayoutPage.vue` / `web/frontend/src/components/layout/PageTabs.vue` (optional editor)
- **Dependencies:** Engine behavior is fixed (`GuiPage.guiTitle`, `GuiLayoutConfig.java:81`); this is web round-trip parity only.
- **Constraints:** The top-level `title`/`rows` in the DTO are web-only metadata the engine ignores; the per-page `title` maps to the engine's page `title` and must stay as-is.

## Verification & Definition of Done
- [x] A gui.yml with `gui_title` set on a page round-trips through the web GET/PUT with the value intact.
- [x] Round-trip test passes (`./gradlew test`).
- [x] `./gradlew build` and `cd web/frontend && npm run build` pass.
