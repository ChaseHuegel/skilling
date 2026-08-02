# ISSUE-139: Make registries and the skill map thread-safe for reload

**Status:** Open
**Type:** Bug
**Severity:** High (concurrent `clear()`/`put()` during reload vs. reads can corrupt maps)

---

## Context & User Story

- **Goal:** As a server owner, I want `/skills reload` to never corrupt registries while the web server or addons read them concurrently.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Replace plain `HashMap`s in `EvaluatorRegistry`, `MechanicRegistry`, `TriggerRegistry`, and `SkillManager.skills` with concurrent structures (or synchronize all mutation/iteration)
- [ ] Ensure the reload sequence (`LockdownManager` phase 4) either pauses web reads or uses atomic swap of immutable snapshots so readers never see a half-cleared registry
- [ ] Make the web handlers and `SkillingAPI` registry iteration safe against concurrent modification
- [ ] Add a test that performs concurrent `list()`/registry iteration during a reload without `ConcurrentModificationException`

## Technical Specifications & Context

- **Target Files:**
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/registry/{EvaluatorRegistry,MechanicRegistry,TriggerRegistry}.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:34` (`LinkedHashMap`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java:68-83` (clear/repopulate)
  - `src/main/java/io/github/chasehuegel/skilling/web/WebServer.java:141-153`, `web/handler/SkillHandler.java:32-59`
- **Dependencies:** ISSUE-140 (defensive copies) and ISSUE-153 (web reads) overlap.
- **Constraints:** Reads are on web-server threads and addon threads; writes are on the main thread during reload. Avoid blocking the main thread.

### Root Cause

All three registries use plain `HashMap`s and `SkillManager.skills` is a `LinkedHashMap`. `/skills reload` clears and repopulates them on the main thread while the web server threads still serialize skills/mechanics and addons can register from any thread. Concurrent `clear()`/`put()` vs. `get()`/iteration can corrupt the maps (CME, infinite loop on resize).

### Proposed Fix

Use `ConcurrentHashMap` for the registries and swap-in immutable snapshots (`Map.copyOf`/`UnmodifiableMap` rebuilt after load) for the skill map so readers always see a consistent view. Iterate snapshots in web handlers.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new concurrency test
- [ ] Test: concurrent registry iteration during reload produces no CME and no torn reads
- [ ] Web `GET /api/skills`, `/api/mechanics`, `/api/triggers` remain correct during a reload
