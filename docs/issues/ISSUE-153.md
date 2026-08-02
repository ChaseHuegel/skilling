# ISSUE-153: Eliminate `ConcurrentModificationException`/torn reads on shared engine state during web reads

**Status:** Resolved
**Type:** Bug
**Severity:** High (web threads crash/500 during reload)

---

## Context & User Story

- **Goal:** As an admin, I want the web GUI to keep responding correctly even while `/skills reload` is running.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Ensure web handlers iterate **snapshot copies** of `SkillManager.getSkills()` and registry keys instead of live mutable maps (coordinate with ISSUE-139)
- [x] Ensure `MechanicRegistry.getAllParameterNames()`/`keys()` and `TriggerRegistry.keys()` are safe under concurrent reload mutation
- [x] Add a concurrency test: `GET /api/skills`/`/api/mechanics`/`/api/triggers` during a reload never throws CME or returns a torn result

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:34,436-438` (live `LinkedHashMap` wrapped in `unmodifiableMap`)
  - `skilling-api/src/main/java/io/github/chasehuegel/skilling/engine/registry/{MechanicRegistry,TriggerRegistry,EvaluatorRegistry}.java` (plain `HashMap`s)
  - `src/main/java/io/github/chasehuegel/skilling/web/WebServer.java:141-153`
  - `src/main/java/io/github/chasehuegel/skilling/web/handler/SkillHandler.java:32-59`
  - `src/main/java/io/github/chasehuegel/skilling/engine/lockdown/LockdownManager.java:70-82`
- **Dependencies:** ISSUE-139 (thread-safe registries) is the structural fix; this ticket covers the web-facing iteration.
- **Constraints:** Reads run on Jetty threads; writes run on the main thread during reload. Never block the main thread.

### Root Cause

`SkillManager.getSkills()` wraps the live `LinkedHashMap` in `Collections.unmodifiableMap`, which does not protect iteration. `SkillHandler.list` iterates it on a Jetty thread while `LockdownManager.reload()` mutates it via `clear()`/`loadSkills()` → `ConcurrentModificationException` (500s) or a half-populated map. The registries are plain `HashMap`s cleared/re-registered during reload while web handlers read them.

### Proposed Fix

Iterate immutable snapshots (`Map.copyOf`/`List.copyOf` taken on the web thread, or maintained as atomic references swapped on reload) in every web handler that touches shared engine state.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including the new concurrency test
- [x] Test: concurrent web reads during a reload produce no CME and no torn responses
- [x] Manual smoke: `GET /api/skills` in a loop while `/skills reload` runs stays clean
