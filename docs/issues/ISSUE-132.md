# ISSUE-132: Fail-fast at load for unknown tags/materials instead of throwing inside event handlers

**Status:** Open
**Type:** Bug
**Severity:** High (runtime exceptions in hot paths crash event handling)

---

## Context & User Story

- **Goal:** As a server owner, I want a typo in a filter tag or material to fail loudly when the plugin loads, not crash the block-break or inventory handlers mid-game.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Validate every filter/material/requirement tag at load time against known vanilla and custom tags, throwing `IllegalArgumentException` on unknown entries
- [ ] Make `TagResolver` resolution safe at runtime: `#c:` references to nonexistent custom tags and unknown vanilla tags must behave consistently (both fail fast at load)
- [ ] Ensure no `IllegalArgumentException` from `TagResolver.resolve` can escape into per-event filter matching or per-slot item checks
- [ ] Add unit tests covering: unknown material rejected at load, unknown vanilla tag rejected at load, missing custom tag handled consistently

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/TagResolver.java:42-88` (throws per call)
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/CustomTagLoader.java:57-59,118` (silently empty on failure)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:537,563` (per-event filter matching)
  - `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java:168,183,198` (per-slot item checks)
- **Dependencies:** `SkillManager` parsing (where load-time validation belongs).
- **Constraints:** Keep the fail-fast convention; the exception surface must move from runtime event handlers to load time.

### Root Cause

`TagResolver.resolve` throws for an unknown material, missing namespace, or unknown vanilla tag. These calls run **per event** (filter matching) and **per inventory slot** (item requirements). A typo crashes the event listener chain at runtime — the opposite of the fail-fast-at-load convention. `#c:` references to nonexistent custom tags instead silently resolve to an empty set, so the two paths behave inconsistently.

### Proposed Fix

Validate all tag/material references during `SkillManager.parseSkill`/`parseXpSource`/`parseAbility` at load. Make the runtime resolution paths non-throwing (or keep throwing only for programmer error) and align custom-tag miss behavior with the chosen convention (fail at load).

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new regression tests
- [ ] Unit test: unknown material in a filter fails load with a clear message
- [ ] Unit test: unknown vanilla tag fails load
- [ ] Unit test: missing custom tag is handled consistently with other unknown tags
- [ ] Runtime review: event handlers contain no reachable `TagResolver` throw path
