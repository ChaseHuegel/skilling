# ISSUE-173: Fix `resolveEventBulkScalar` — `furnace_extract` must scale by extracted item count, not dropped XP orbs

**Status:** Resolved
**Type:** Bug
**Severity:** Medium (XP reward miscalculation for `furnace_extract` sources — yields do not match the documented bulk semantics)

---

## Context & User Story

- **Goal:** As a skill author, I want a `furnace_extract` XP source to grant the configured reward once per item pulled from a furnace — extracting 4 iron ingots at once grants 4× the reward — not per the small, material-dependent XP-orb value the extraction happens to drop.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [x] Change the `FurnaceExtractEvent` branch of `resolveEventBulkScalar` to return `e.getItemAmount()` (the number of items extracted) instead of `e.getExpToDrop()` (the vanilla XP orbs dropped)
- [x] Update the method Javadoc (`SkillEventListener.java:618-621`) — "furnace XP extracted" becomes "items extracted from a furnace"
- [x] Update `SkillEventListenerBulkScalarTest.furnaceExtractScalarUsesExpToDrop` to mock `getItemAmount()` (e.g. amount 4 → scalar 4) and rename it accordingly
- [x] Update the bulk-scaling note in `docs/users/capabilities.md:572-577` ("the XP extracted from a furnace" → "the number of items extracted from a furnace")
- [x] Update the `furnace_extract` description in `docs/reports/REPORT_XP-CURVE.md` (§2.2) to reflect item-count scaling instead of stored-furnace-XP scaling
- [x] Add a regression test proving a `FurnaceExtractEvent` with item amount 4 yields scalar 4 (4 ingots = 4× XP)

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:636-638` (the branch) and `:617-628` (Javadoc)
  - `src/test/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListenerBulkScalarTest.java:42-47`
  - `docs/users/capabilities.md:572-577`
  - `docs/reports/REPORT_XP-CURVE.md:62-64`
- **Dependencies:** This is a correction to the ISSUE-105 bulk-scalar feature, in the same family as ISSUE-171 (which fixed the `consume_item`/`craft_item` scalars). Bundled `furnace_extract` sources (`cooking.yml`, `masonry.yml`, `smithing.yml`) will see their effective per-action yields change; re-tune their constants if needed (see REPORT_XP-CURVE §7.2 P2).
- **Constraints:** `collect_xp` (`PlayerExpChangeEvent.getAmount()`) is unchanged. Non-bulk triggers stay at scalar `1`. `FurnaceExtractEvent.getItemAmount()` returns the number of items pulled in one extraction, which is exactly the intended bulk size.

### Root Cause

The `FurnaceExtractEvent` branch returns `e.getExpToDrop()` — the vanilla XP orbs dropped by the extraction, a small material-dependent value (e.g. ~0.7 for iron ore, 0.1 for glass). The intended bulk scalar is the **number of items extracted**, `e.getItemAmount()`. As implemented, extracting 4 iron ingots grants only ~0.7× the reward instead of 4×, so `furnace_extract` sources do not match the documented "bulk" semantics.

### Proposed Fix

Return `e.getItemAmount()` from the `FurnaceExtractEvent` branch and update the Javadoc and tests accordingly. Because the report and capabilities docs describe the old stored-XP behavior, update them in the same change.

## Verification & Definition of Done

- [x] `./gradlew build && ./gradlew test` pass, including the updated test
- [x] Unit test: `FurnaceExtractEvent` with `getItemAmount()` = 4 yields scalar 4 (4 ingots = 4× XP)
- [x] Unit test: `collect_xp` and non-bulk events are unchanged
- [x] `docs/users/capabilities.md` and `docs/reports/REPORT_XP-CURVE.md` describe item-count scaling
- [x] Bundled `furnace_extract` reward constants are reviewed for the new effective yields (adjust in a follow-up ticket if out of band) — re-tuning is tracked as REPORT_XP-CURVE §7.2 P2 / backlog
