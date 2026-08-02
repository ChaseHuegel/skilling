# ISSUE-171: Fix `resolveEventBulkScalar` — drop the `consume_item` scalar and add `craft_item` scaling

**Status:** Open
**Type:** Bug
**Severity:** High (XP inflation exploit for `consume_item` sources; `craft_item` bulk scaling never applied)

---

## Context & User Story

- **Goal:** As a server owner, I want a `consume_item` XP source to grant its configured reward once per item consumed (a stack of 64 must not grant 64× XP), and a `craft_item` XP source to scale with how many items were crafted in one operation.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Remove the `PlayerItemConsumeEvent` case from `resolveEventBulkScalar` so consuming any item returns scalar `1` (only a single item is consumed from the stack at a time)
- [ ] Add a `CraftItemEvent` case that returns the count of the item stack that was crafted (result-slot stack count, including shift-click batch totals), with a null-guard fallback to the single-craft recipe result
- [ ] Update the method Javadoc (`SkillEventListener.java:617-628`) — it currently documents `consume_item` as a bulk trigger and omits `craft_item`
- [ ] Update `SkillEventListenerBulkScalarTest`: change `consumeItemScalarUsesStackSize` to expect `1`, add craft-scalar tests (single craft and shift-click batch)
- [ ] Update `docs/users/capabilities.md` bulk-operation scaling note (lines ~572-590) to list `craft_item` instead of `consume_item`
- [ ] Add a regression test proving a consume of a stack of 64 grants 1× the reward

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:629-640` (`resolveEventBulkScalar`)
  - `src/main/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListener.java:617-628` (Javadoc)
  - `src/test/java/io/github/chasehuegel/skilling/engine/listener/SkillEventListenerBulkScalarTest.java:19-23,33-40`
  - `docs/users/capabilities.md:572,576,590`
- **Dependencies:** This corrects the `consume_item` half of ISSUE-105 ("Multiply XP source rewards by bulk-operation scalars (collect_xp, consume_item, furnace_extract)"), which mistakenly applied stack-size scaling to consume events. The `craft_item` trigger already dispatches through `onCraftItem` → `dispatch(player, event, "craft_item")` → `grantXp` (`SkillEventListener.java:171-176`), so only the scalar resolution changes.
- **Constraints:** Non-bulk triggers must stay at scalar `1`. `collect_xp` and `furnace_extract` scalars are unchanged. No schema/YAML changes; bundled `consume_item` sources (`cooking.yml`, `farming.yml`, `herbalism.yml`) automatically normalize to per-action rewards.

### Root Cause

`resolveEventBulkScalar` returns `e.getItem().getAmount()` for `PlayerItemConsumeEvent` (`SkillEventListener.java:633-635`), i.e. the **stack size**. But vanilla consumption always removes exactly one item from the stack per event, so a player eating from a stack of 64 gets 64× the configured `consume_item` XP reward. The bulk-scaling capability was originally intended for the **craft** event, but `craft_item` fell through to the default scalar of `1` — so crafting bulk was never scaled and consume was incorrectly scaled.

### Proposed Fix

- Delete the `PlayerItemConsumeEvent` branch; consume events fall through to the default `1`.
- Add `if (event instanceof CraftItemEvent ce)` returning the crafted count: `ItemStack current = ce.getCurrentItem(); double scalar = current != null && !current.isEmpty() ? current.getAmount() : ce.getRecipe().getResult().getAmount();` — the result-slot amount reflects how many items were produced, including shift-click batch totals.
- Keep `collect_xp` (`PlayerExpChangeEvent.getAmount()`) and `furnace_extract` (`FurnaceExtractEvent.getExpToDrop()`) unchanged.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the updated and new tests
- [ ] Unit test: a `PlayerItemConsumeEvent` with a stack of 64 yields scalar `1`
- [ ] Unit test: a single `CraftItemEvent` (result stack of N) yields scalar N
- [ ] Unit test: a shift-click `CraftItemEvent` (batch result stack) yields the batch count
- [ ] Unit test: `collect_xp` and `furnace_extract` scalars are unchanged
- [ ] `docs/users/capabilities.md` no longer lists `consume_item` as bulk-scaled and lists `craft_item`
- [ ] `SkillEventListenerBulkScalarTest` class Javadoc reflects the corrected trigger set
