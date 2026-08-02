# ISSUE-127: Cap `AreaHarvestMechanic` scan bounds and gate per-block breaking

**Status:** Resolved
**Type:** Bug
**Severity:** Medium (unbounded chunk-loading scan + bypasses block protection)

---

## Context & User Story

- **Goal:** As a server owner, I want area-harvest abilities to have sane, configurable limits so a large radius cannot scan thousands of blocks in one event or bypass region protections.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Replace the `Integer.MAX_VALUE` default for `max_blocks` with a bounded default and clamp the radius
- [x] Stop the scan when `max_blocks` is reached
- [x] Gate each block break through the block-protection path (respect `BlockBreakEvent`/region plugins) or document and enforce a config opt-in for bypassing protections
- [x] Add unit tests covering: radius clamping, `max_blocks` cap enforcement, protection-gated breaks

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/AreaHarvestMechanic.java:24-46`
- **Dependencies:** `block_break` trigger context. Compare `ChainBreakMechanic` (ISSUE-128) which re-dispatches `BlockBreakEvent`.
- **Constraints:** Fail-fast on invalid config (negative radius/blocks). Do not change the YAML key.

### Root Cause

`max_blocks` defaults to `Integer.MAX_VALUE` and the scan is `(2r+1)²` blocks. A radius of 32 scans up to ~4200 blocks per event, each `getRelative` potentially loading chunks. `breakNaturally` fires no `BlockBreakEvent` and bypasses region protection.

### Proposed Fix

Clamp radius to a bounded maximum (e.g. Bukkit's 32-block search cap), default `max_blocks` to a small number, stop at the cap, and route each break through the same `BlockBreakEvent`-respecting path used elsewhere (or gate with a config flag).

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including new regression tests
- [x] Unit test: radius is clamped to the configured max
- [x] Unit test: breaking stops at `max_blocks`
- [x] Unit test: protection-plugin cancellations are respected
