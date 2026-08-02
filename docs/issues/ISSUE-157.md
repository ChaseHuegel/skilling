# ISSUE-157: Fix `GuiLayout` round-trip data loss and add server-side validation

**Status:** Open
**Type:** Bug
**Severity:** Medium (GUI row/version settings silently dropped; editor reads back different values than it wrote)

---

## Context & User Story

- **Goal:** As an admin, I want the GUI layout I configure (including rows) to survive a reload and be read back exactly as saved.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Preserve `GuiLayoutDTO.rows` and `version` through `GuiLayoutSerializer.serialize` (currently `rows` is hardcoded to 0 and `version` ignored)
- [ ] Stop overwriting filler with defaults on serialize
- [ ] Add server-side validation in `GuiLayoutHandler.update` for `rows` range and slot indices (fail fast with 400 instead of relying on engine clamp)
- [ ] Add round-trip tests asserting serialize → parse → serialize is lossless for rows/version/filler

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/dto/GuiLayoutSerializer.java:152-184`
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/GuiLayoutHandler.java:58-67`
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/GuiLayoutConfig.java:87-91,105-109` (engine clamp)
- **Dependencies:** None.
- **Constraints:** Keep the wire format backward compatible where possible; engine must accept the preserved values.

### Root Cause

`GuiLayoutSerializer.serialize` hardcodes `pageMap.put("rows", 0)`, ignores `GuiLayoutDTO.rows` and `version`, and overwrites filler with defaults. The GUI's row setting has no effect after a reload, and the editor reads back different values than it wrote.

### Proposed Fix

Serialize `rows`/`version` from the DTO and preserve filler entries. Validate `rows` (1-6) and slot indices server-side, returning 400 on invalid input.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new round-trip tests
- [ ] Test: serialize → parse → serialize is lossless for rows, version, and filler
- [ ] Test: invalid `rows`/slot indices return 400
- [ ] Manual smoke: set GUI rows to 4, reload, editor shows 4
