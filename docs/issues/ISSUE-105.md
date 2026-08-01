# ISSUE-105: Multiply XP source rewards by bulk-operation scalars (collect_xp, consume_item, furnace_extract)

**Status:** Open
**Type:** Improvement
**Severity:** Medium (XP rewards for bulk operations ignore the operation's size)

---

## Context & User Story

- **Goal:** As a server admin authoring skills, I want XP rewards for bulk operations to scale with the operation's magnitude, so that collecting 3 XP, consuming 3 doors, or extracting 3 furnace XP grants 3× the configured reward instead of a flat amount.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Add a helper that resolves the bulk scalar from the event (mirroring the existing `resolveEventMaterial` pattern): `PlayerExpChangeEvent.getAmount()` for `collect_xp`, `PlayerItemConsumeEvent.getItem().getAmount()` for `consume_item`, `FurnaceExtractEvent.getExpToDrop()` for `furnace_extract`
- [ ] Apply the scalar as a multiplier in `grantXp` so a reward of 2 skill XP for a bulk of 3 yields 6 skill XP
- [ ] Ensure events that are not bulk operations return a scalar of `1` (no behavior change)
- [ ] Add unit tests for the scalar resolution and for XP math (including the global XP modifier and `XpBonusMechanic` multiplier interplay)
- [ ] Add a note to `docs/users/capabilities.md` documenting that bulk-operation triggers scale rewards by operation size

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java` (`grantXp`, lines 366-405; XP computed at lines 378-380; `resolveEventMaterial` pattern at lines 606-637)
  - `src/main/java/io/github/chasehuegel/skilling/engine/trigger/impl/CollectXpTrigger.java`, `ConsumeItemTrigger.java`, `FurnaceExtractTrigger.java`
  - `docs/users/capabilities.md`
- **Dependencies:** The raw Bukkit events already carry the scalars: `PlayerExpChangeEvent.getAmount()`, `FurnaceExtractEvent.getExpToDrop()`, `PlayerItemConsumeEvent.getItem().getAmount()`. XP is currently computed as `reward.evaluate(oldLevel, 1) * globalXpModifier * XpBonusMultiplier` then rounded.
- **Constraints:** Non-bulk events must be unaffected. Keep rounding behavior consistent (round once, after scaling). No schema/API changes.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new unit tests
- [ ] Unit test: a `collect_xp` event with amount 3 and a reward of 2 grants 6 skill XP
- [ ] Unit test: a `consume_item` event consuming a stack of 3 doors and a reward of 2 grants 6 skill XP
- [ ] Unit test: a `furnace_extract` event with 3 XP and a reward of 2 grants 6 skill XP
- [ ] Unit test: a non-bulk trigger (e.g. `block_break`) still grants the flat configured reward (scalar 1)
- [ ] Edge case handled: a bulk scalar of `0` grants `0` XP and does not round up to a positive amount
