# ISSUE-152: Fail loudly (not silently) when a reload drops skills due to malformed YAML

**Status:** Resolved
**Type:** Bug
**Severity:** High (admin told success while skills vanished)

---

## Context & User Story

- **Goal:** As an admin, I want "Apply & Reload" to report a failure if any staged skill file is malformed, so I never believe a reload succeeded while some skills silently disappeared.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Make `SkillManager.loadSkills()` validate all skill files **before** clearing the live map (parse-first, then commit), so a malformed file cannot leave a partially populated registry
- [x] Surface the specific failing file and parse error through `LockdownManager.reload()` and `ReloadHandler` (HTTP 4xx/5xx with a clear message) instead of swallowing it in the broad `catch (Exception)`
- [x] Keep staging intact on failure so the admin can fix and retry (coordinate with ISSUE-151)
- [x] Add server-side validation of staged YAML in the staging path (triggers, evaluator types, mechanics) so malformed content is rejected before apply
- [x] Add a unit test covering: one malformed file among many → reload reports failure and no skills are silently dropped

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:58-72` (`skills.clear()` before parse)
  - `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java:68-86` (broad catch swallows)
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/ReloadHandler.java:67-79`
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/SkillHandler.java:150-171` (weak staging validation)
- **Dependencies:** ISSUE-133 (fail-fast parsing) and ISSUE-151 (reload fail-safe).
- **Constraints:** A reload must be all-or-nothing for skill loading: either all skills load or the previous set is preserved.

### Root Cause

`loadSkills()` calls `skills.clear()` first, then parses files in `listFiles` order, throwing `IllegalArgumentException` on the **first** malformed file. The broad `catch (Exception)` in `LockdownManager` swallows it, so Phase 4 continues with a partially populated registry. `ReloadHandler` sees no error → returns `success: true` and clears staging, while skills silently vanished.

### Proposed Fix

Parse into a fresh map first; only swap it into `SkillManager.skills` when every file parses. Propagate errors to `ReloadHandler` with the failing file name, and validate staged skill YAML at staging time.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including the new regression test
- [x] Test: one malformed skill among many → reload returns an error naming the file and the previous skill set is preserved
- [x] Test: `POST /api/skills` / `PUT /api/skills/{id}` reject YAML with unknown triggers/evaluators/mechanics
- [x] Manual smoke: stage a broken skill, reload, confirm a clear error and no lost skills
