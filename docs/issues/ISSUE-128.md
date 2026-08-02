# ISSUE-128: Fix `ChainBreakMechanic` durability cost and pipeline re-dispatch

**Status:** Open
**Type:** Bug
**Severity:** Medium (free tool durability + per-block XP/ability re-processing)

---

## Context & User Story

- **Goal:** As a player, I want chain-break to consume tool durability for each broken block and to not re-trigger the full XP/ability pipeline per chained block.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Consume tool durability for each chained block (or document the intended free-durability behavior and cap it)
- [ ] Stop re-dispatching the full event pipeline per chained block; break remaining blocks without re-entering `onBlockBreak` (or scope the re-dispatch so XP/fanfare do not repeat per block)
- [ ] Keep the existing `CHAINING_PLAYERS` re-entry guard as a safety net
- [ ] Add unit tests covering: durability consumption per block and single pipeline pass

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/ChainBreakMechanic.java:47-57`
- **Dependencies:** `BlockBreakEvent` (re-dispatched via `Bukkit.callEvent` at line 50), `AutoReplantMechanic` interaction, and the `fireAbilities`/`grantXp` pipeline.
- **Constraints:** Do not change the YAML key or `chain_limit` semantics.

### Root Cause

`breakNaturally` never consumes tool durability, so one durability point breaks the whole chain. Each chained block also fires a synthetic `BlockBreakEvent`, re-entering `onBlockBreak` → XP grant + ability firing per chained block; only `ChainBreakMechanic` itself is guarded via `CHAINING_PLAYERS`.

### Proposed Fix

Damage the player's tool per broken block (respecting unbreaking/vanilla durability rules), and break the remaining blocks directly (with protection checks) instead of synthesizing a full `BlockBreakEvent` per block, or gate the re-entrant grant so it runs once per source event.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new regression tests
- [ ] Unit test: tool durability decreases once per chained block
- [ ] Unit test: a chained break does not grant XP/fanfare per block
- [ ] Unit test: the `CHAINING_PLAYERS` guard still prevents infinite recursion
