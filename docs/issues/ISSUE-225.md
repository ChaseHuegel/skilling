# ISSUE-225: Web gui-layout API must reject reserved navigation slots

## Context & User Story
- **Goal:** As a server admin, I want the gui-layout API to reject assignments to the reserved navigation slots on the last row (prev arrow, page indicator, next arrow) instead of staging a layout the engine silently ignores.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [ ] `GuiLayoutHandler.validate` (lines 80-94) must reject any page slot that falls on the last row's reserved slots, matching the engine's rule in `GuiLayoutConfig.java:95` (`Set.of(dummy.prevSlot(), dummy.indicatorSlot(), dummy.nextSlot())`) and `GuiPage.java:70-90`. Reserved slots for `rows` R are `(R-1)*9`, `(R-1)*9+4`, `(R-1)*9+8`.
- [ ] Return HTTP 400 with a descriptive message naming the page and slot, consistent with the existing invalid-slot message.
- [ ] Add a regression test (web handler test): a PUT with a skill on a reserved slot returns 400; a PUT with a skill in a valid non-reserved slot still succeeds.
- [ ] Keep the frontend behavior unchanged (the grid already blocks assignment to nav slots via `ChestGrid.getNavRole`) — the API validation is defense in depth.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/GuiLayoutHandler.java`
  - Existing web handler tests under `src/test/java/io/github/chasehuegel/skilling/web/handler/`
- **Dependencies:** none. Engine is fail-soft (warning + skip); the web API should fail-fast instead of staging content the engine will drop.
- **Constraints:** Slot bounds are `0..(rows*9-1)`; rows is `1..6` in the web (the engine also accepts `0` = full 6-row chest, but the web already normalizes to 6).

## Verification & Definition of Done
- [ ] PUT with a skill on a reserved nav slot returns 400.
- [ ] PUT with valid slots still stages successfully.
- [ ] `./gradlew build` and `./gradlew test` pass.
