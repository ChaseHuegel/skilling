# ISSUE-130: Honor `slot` and `amount` in item requirements

**Status:** Open
**Type:** Bug
**Severity:** Medium (requirement semantics ignored)

---

## Context & User Story

- **Goal:** As a skill author, I want item requirements that specify a slot (e.g. off-hand) or an amount to be enforced exactly as written.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Make `hasItem(player, tag, slot)` honor the `slot` parameter instead of scanning the entire inventory
- [ ] Make the `possession` action honor the required `amount` (require that many items, not just 1)
- [ ] Keep `removeItems` consistent with the slot/amount semantics so costs are deducted from the same location and quantity
- [ ] Add unit tests covering: slot-scoped possession, amount-scoped possession, slot/amount-aware cost removal

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/requirements/RequirementEngine.java:76-93,164-209`
- **Dependencies:** `TagResolver` for `#`-prefixed tags.
- **Constraints:** Fail-fast on malformed slot names. Do not change the YAML schema.

### Root Cause

`hasItem(player, tag, slot)` scans the entire inventory and never uses the `slot` parameter, so `slot: "OFF_HAND"` requirements are meaningless. The `possession` action calls `hasItem` (any match) and ignores `itemReq.amount()`, so a possession requirement of 3 only verifies 1 is present.

### Proposed Fix

Resolve the slot to a concrete inventory index (or treat a specified slot as a filter during the scan) and pass the required amount through so both check and consume enforce quantity and location.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new regression tests
- [ ] Unit test: `slot: OFF_HAND` possession only matches the off-hand stack
- [ ] Unit test: `amount: 3` possession requires 3 matching items
- [ ] Unit test: cost removal respects slot and amount
