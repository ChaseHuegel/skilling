# ISSUE-148: Fix UI navigation slot collision and close poison-pill vaporization gaps

**Status:** Open
**Type:** Bug
**Severity:** Medium (accidental page navigation + incomplete anti-dupe net)

---

## Context & User Story

- **Goal:** As a player, I want clicking my own inventory below a short skills page to never silently flip pages, and I want tagged UI items to be vaporized in every place they could leak.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Guard `handleNavigation` with `event.getClickedInventory() == top` so clicks in the player's own inventory cannot collide with nav slots on short pages (rows <= 4)
- [ ] Extend the poison-pill vaporization net to the missing paths: `PlayerAttemptPickupItemEvent`, `InventoryMoveItemEvent` (hopper transfer), cursor-deposit into a normal chest, and `InventoryCloseEvent` cursor cleanup
- [ ] Keep the existing blanket cancel inside Skilling holders unchanged
- [ ] Add unit tests covering: bottom-inventory click on a 4-row page does not navigate; a tagged item picked up/deposited outside the UI is vaporized

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/ui/UIProtectionListener.java:37-58,65-83,99-126`
- **Dependencies:** `PoisonPillTag`, `GuiPage` slot helpers.
- **Constraints:** Do not break legitimate guide-book usage (see note below — the guide book reuses the UI tag and is destroyed by the net; coordinate with a tag change if needed).

### Root Cause

`handleNavigation` uses `event.getSlot()`, which for a bottom-inventory click returns the 0-35 normalized slot; on pages with `rows <= 4` those collide with `prevSlot`/`nextSlot` and silently navigate. The vaporization net covers `ItemSpawnEvent`/`InventoryPickupItemEvent` but misses player pickup, hopper moves, cursor deposit into a normal chest, and close-with-item-on-cursor.

### Proposed Fix

Check `event.getClickedInventory()` identity before navigating. Add handlers for the missing leak paths that vaporize `PoisonPillTag`-tagged items. Note the `SkillsGuideBook` reuses the UI tag (it is destroyed by the net) — either introduce a distinct book tag or exempt book contexts.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new regression tests
- [ ] Unit test: clicking a bottom-inventory slot on a 4-row page does not navigate
- [ ] Unit test: tagged item on the cursor deposited into a normal chest is vaporized
- [ ] Unit test: hopper move / player pickup of a tagged item is vaporized
- [ ] Guide book still functions as a legitimately held item
