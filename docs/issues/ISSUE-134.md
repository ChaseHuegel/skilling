# ISSUE-134: Unify state-filter logic between the requirements engine and XP filters

**Status:** Open
**Type:** Bug
**Severity:** Medium (divergent semantics for the same state conditions)

---

## Context & User Story

- **Goal:** As a skill author, I want a state condition such as "must be sneaking" or "must be in the overworld" to behave the same whether it gates an ability requirement or an XP source filter.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Route ability requirement state checks through the same `StateFilterRegistry` used by XP/mechanic filters
- [ ] Resolve the format divergence (`dimension:overworld` inline vs. `dimension` + value) into one canonical form
- [ ] Decide and document the default behavior for unknown states (requirements currently pass; filters currently fail) and make both consistent
- [ ] Add unit tests covering: sneaking gating parity, dimension parity, unknown-state behavior parity

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java:144-162` (`checkState` hardcoded set)
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java:336-471` (XP/mechanic filters via `StateFilterRegistry`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/registry/StateFilterRegistry.java`
- **Dependencies:** `StateFilter` implementations.
- **Constraints:** Do not break existing bundled skill YAML; prefer extending the registry over hardcoding.

### Root Cause

`checkState` hardcodes a small set with `default -> true` (unknown states pass), while XP/mechanic filters go through `StateFilterRegistry` with `default -> false` (unknown filters fail). The same semantic (e.g. "dimension") is implemented in two different formats, so gating behaves differently between ability requirements and XP sources, and the implementations will drift.

### Proposed Fix

Replace the hardcoded `checkState` with `StateFilterRegistry` lookups, canonicalize the state format, and align the unknown-state default. Keep a single registry as the source of truth.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new regression tests
- [ ] Unit test: sneaking requirement and sneaking filter behave identically
- [ ] Unit test: dimension requirement and dimension filter behave identically
- [ ] Unit test: unknown state behavior is documented and consistent across both paths
- [ ] Bundled skill YAML still parses and behaves as before
