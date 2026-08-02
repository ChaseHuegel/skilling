# ISSUE-178: Stop the poison-pill net from vaporizing the Skills Guide book when dropped

## Context & User Story
- **Goal:** As a player, I want to drop the craftable Skills Guide book like any normal item so that it stays on the ground and can be picked up again.
- **Agent Role:** You are an expert backend (Paper) engineer executing this task.

The craftable "Skills Guide" book is a legitimate held item, not a chest GUI item. It is currently being deleted when a player drops it. The book must never be vaporized by the poison-pill anti-dupe net.

## Implementation Requirements
- [ ] Confirm the guide book item never carries the `PoisonPillTag` (it must use the distinct `GuideBookTag`).
- [ ] Ensure no vaporization path in the UI protection net fires for a dropped/pickup'd/cursor-held guide book.
- [ ] Keep the book recognizable on right-click so it still opens the skill overview (it needs a tag, just not the poison pill).
- [ ] Handle the reload/legacy edge case: books crafted before this fix may already carry the old `PoisonPillTag`; dropping one must not delete it.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/SkillsGuideBook.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/GuideBookTag.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/PoisonPillTag.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/UIProtectionListener.java`
- **Dependencies:** `Skilling.registerBuiltins` / recipe registration path.
- **Constraints:**
  - `SkillsGuideBook.create()` already applies `GuideBookTag` (`SkillsGuideBook.java:77-88`); the vaporization net (`UIProtectionListener.java`) checks `PoisonPillTag` only. Verify there is no remaining path that still deletes the book (drop spawn, attempt pickup, hopper move, cursor deposit on click, close-with-item-on-cursor).
  - The shapeless recipe result (`registerRecipe()`) is the `create()` stack, so the tag must survive into the crafted item.
  - GUI menu items (`SkillMenuBuilder`) must keep `PoisonPillTag`; only the book is exempt.
  - Do not weaken the anti-dupe guarantees for actual UI items.

## Verification & Definition of Done
- [ ] `./gradlew build` and `./gradlew test` pass
- [ ] Manual: craft the guide book, drop it — the item stays on the ground and can be re-picked
- [ ] Right-click still opens the skill overview; opening/closing the GUI does not delete a held book
- [ ] Books that still carry a legacy `PoisonPillTag` (from before the fix) are not deleted on drop
- [ ] Poison-pill vaporization still works for actual GUI items
