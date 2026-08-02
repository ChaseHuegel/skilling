# ISSUE-149: Validate `{id}` path params in `/api/skills/{id}` against path traversal

**Status:** Open
**Type:** Bug
**Severity:** Critical (arbitrary file read/delete primitive)

---

## Context & User Story

- **Goal:** As a server owner, I want the admin web API to reject any skill ID that could escape the skills directory, so a crafted request cannot read or delete files outside it.
- **Agent Role:** You are an expert security/backend engineer executing this task.

## Implementation Requirements

- [ ] Apply the `[a-z_][a-z0-9_]*` regex to the **path param** `{id}` in `SkillHandler.get`, `update` (the `oldId`), and `delete` **before** any `File` construction
- [ ] Return HTTP 400 for non-matching IDs instead of proceeding
- [ ] Canonicalize/resolve and verify the final `File` stays within `skillsDir` (defense-in-depth against future param sources)
- [ ] Add regression tests asserting `..%2F..%2Fconfig` and other traversal payloads are rejected with 400 on GET/PUT/DELETE

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/web/handler/SkillHandler.java:61-148`
- **Dependencies:** Javalin 7 URL-decodes path params (`%2F` → `/`), which makes traversal reachable. `StagingManager.stageSkillDeletion` (`StagingManager.java:137-151`) builds files from the id.
- **Constraints:** Keep the documented ID contract (`web/AGENTS.md`: "Skill IDs are validated against `[a-z_][a-z0-9_]*`"). Do not weaken the JSON-body validation.

### Root Cause

The regex guard exists only in `validateSkill(dto)` applied to the JSON body id. The `{id}` path param is never validated in `get`, `update` (oldId), or `delete`. Because Javalin URL-decodes captured params, `GET /api/skills/..%2F..%2Fconfig` yields `id = "../../config"` → `new File(skillsDir, id + ".yml")` = `dataFolder/config.yml`. GET becomes a file-existence oracle (and discloses skill-shaped files); DELETE deletes staged files and, with enough `../`, arbitrary files as the server user.

### Proposed Fix

Validate every `{id}` path param with the same regex before constructing paths, reject with 400, and additionally verify the canonical path is under `skillsDir`.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new security regression tests
- [ ] Test: `GET /api/skills/..%2F..%2Fconfig` → 400 (no file read)
- [ ] Test: `DELETE /api/skills/..%2F..%2Fconfig` → 400 (no file delete)
- [ ] Test: legitimate IDs still work unchanged
- [ ] Path is canonicalized and confined to `skillsDir` in all three handlers
