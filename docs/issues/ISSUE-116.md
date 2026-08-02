# ISSUE-116: Move ability Check/Execute/Consume to per-ability in `fireAbilities`

**Status:** Resolved
**Type:** Bug
**Severity:** Critical (cooldown blocks all but the first mechanic; item costs deducted multiple times)

---

## Context & User Story

- **Goal:** As a player, I want an ability with multiple mechanics to run all of them once, with a single cooldown application and a single item-cost deduction per activation.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Perform `requirementEngine.check(...)` once per **ability**, before iterating mechanics
- [x] Execute all mechanics for the ability when the check passes, and `consume(...)` exactly once after the mechanics succeed
- [x] Decide and implement how partial mechanic failure behaves (e.g. consume if any mechanic executed; document the chosen contract)
- [x] Preserve per-mechanic filter matching and per-mechanic param evaluation
- [x] Preserve cooldown/failure feedback (action bar) without spurious "on cooldown" after a successful cast
- [x] Add unit tests covering: multi-mechanic ability with cooldown runs all mechanics once; item-cost ability deducts items exactly once

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:439-515`
- **Dependencies:** `RequirementEngine.check/consume` (`RequirementEngine.java`). This is the documented **Check, Execute, Consume** contract (`src/AGENTS.md` §3).
- **Constraints:** Do not change the per-mechanic `SkillMechanic.execute` signature. Keep behavior for single-mechanic abilities identical.

### Root Cause

`requirementEngine.check(...)` (line 452), `mechanic.execute(...)` (line 471), and `requirementEngine.consume(...)` (line 476) are all inside the `for (MechanicEntry entry : ability.mechanics())` loop. With a cooldown > 0, mechanic #1 consumes and applies the cooldown, so mechanic #2's check immediately fails with `COOLDOWN` — only mechanic #1 ever executes, plus a spurious "on cooldown" action bar. With no cooldown but an item cost, items are deducted once per executing mechanic — double/triple consumption.

### Proposed Fix

Hoist the check above the mechanics loop. If it fails, show failure feedback and skip the whole ability. Otherwise evaluate params and execute each mechanic; after the loop, call `consume` once (guarding on at least one mechanic having executed if partial-failure semantics are desired).

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new regression tests
- [x] Unit test: two-mechanic ability with cooldown executes both mechanics and sets the cooldown once
- [x] Unit test: item-cost ability with two mechanics deducts the cost exactly once
- [x] Unit test: single-mechanic behavior is unchanged (existing tests still pass)
