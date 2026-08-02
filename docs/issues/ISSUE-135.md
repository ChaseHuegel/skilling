# ISSUE-135: Pre-flatten and cache tag/material resolution for O(1) event lookups

**Status:** Open
**Type:** Improvement
**Severity:** High (per-event registry lookups and string parsing threaten 20 TPS)

---

## Context & User Story

- **Goal:** As a server owner, I want block/item filters to resolve once at load so hot events (block break, interact, inventory checks) run at O(1) instead of re-resolving tags per event.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Flatten every filter's tag/material references into `EnumSet<Material>`/`EnumSet<EntityType>` at plugin load (per `src/AGENTS.md` §5)
- [x] Cache vanilla-tag resolution (avoid `Bukkit.getTag` + `tag.getValues()` per call) and material name parsing
- [x] Cache resolved tag sets in the item-requirement path so `TagResolver.resolve` is not invoked per inventory slot
- [x] Keep custom `#c:` tag flattening consistent with vanilla tags
- [x] Add a benchmark or test asserting a single event dispatch does not re-resolve tags

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/tag/TagResolver.java:42-88`
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:537-565`
  - `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java:168,183,198`
  - `src/main/java/io/github/chasehuegel/skilling/engine/SkillManager.java:310-315` (raw filter storage)
- **Dependencies:** ISSUE-132 (load-time validation of these references).
- **Constraints:** O(1) lookups on the event path; no per-event string parsing or registry calls.

### Root Cause

`resolveVanillaTag` calls `Bukkit.getTag` + `tag.getValues()` on every call and `Material.matchMaterial` runs per inventory slot. Filters store raw strings and resolve per event. On a hot event (e.g. `block_break` with 32 skills × sources × filters) this is significant avoidable work, and item requirements rebuild tag sets per inventory slot.

### Proposed Fix

At load, compile each filter into a resolved `EnumSet` keyed by material/entity type. Cache vanilla tag lookups (they are static per server run) and reuse resolved sets across event dispatch and requirement checks.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass
- [x] Performance test: a `block_break` dispatch resolves no tags at runtime (verified by instrumentation or a captured-resolutions test)
- [x] Existing filter/tag tests still pass with the compiled representation
