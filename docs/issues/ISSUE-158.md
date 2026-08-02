# ISSUE-158: Web error-handling hygiene (correct status codes, no internal message leakage)

**Status:** Open
**Type:** Bug
**Severity:** Medium (500s instead of 400s, stack/exception messages reflected to clients)

---

## Context & User Story

- **Goal:** As an admin, I want the web API to return sensible 4xx errors for bad requests and never leak internal exception/filename details in responses.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Return 400 (not 500) for malformed JSON bodies and shape-invalid payloads in `SkillHandler`, `GuiLayoutHandler`, and `TagHandler`
- [ ] Stop embedding `e.getMessage()` (which can contain parsed YAML content or resolved filenames) in 500 responses; return a generic message and log details server-side
- [ ] Handle null `cause.getMessage()` in `ReloadHandler` (avoid `"Reload error: null"`)
- [ ] Fix unchecked casts / missing type checks in `TagHandler.update` so wrong shapes yield 400, not ClassCastException/NPE
- [ ] Add tests covering: malformed JSON → 400, wrong-body-shape → 400, internal errors → generic message without path/exception text

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/SkillHandler.java:72-75,87-89`
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/GuiLayoutHandler.java:64-66`
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/TagHandler.java:40,44-68,65-67`
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/ConfigHandler.java:65`
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/ReloadHandler.java:60-61`
- **Dependencies:** None.
- **Constraints:** Keep logging detailed server-side; only the HTTP body becomes generic.

### Root Cause

Malformed JSON bodies fall through to 500 instead of 400; 500 bodies embed `e.getMessage()` (SnakeYAML parse failures can include file-content snippets; `SkillHandler.get` leaks resolved filenames); `TagHandler.update` uses an unchecked cast that throws on wrong shapes; `ReloadHandler` can emit `"Reload error: null"`.

### Proposed Fix

Add request-body validation returning 400, replace exception-message bodies with generic error text (full detail to the logger), and type-check payload shapes in the tag/config handlers.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new error-handling tests
- [ ] Test: malformed JSON → 400
- [ ] Test: 500 bodies never contain paths, exception messages, or YAML snippets
- [ ] Test: `ReloadHandler` never emits a null message
