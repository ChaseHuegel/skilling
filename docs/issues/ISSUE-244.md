# ISSUE-244: SkillsGuideBook recipe is not removed when the book is disabled

## Context & User Story
- **Goal:** As an admin, I want disabling the skills guide book to actually remove it from the game, not just make it unopenable.
- **Agent Role:** You are an expert backend engineer executing this task.
- **Severity:** Medium — `SkillsGuideBook.setEnabled(false)` only flips the flag; the interact handler goes inert but the registered recipe stays craftable and discovered (`SkillsGuideBook.java:53-67`, `registerRecipe`). `/skills set skills_guide_book.enabled false` therefore leaves the book in the game, and `onDisable` never removes the recipe.

## Implementation Requirements
- [ ] Remove the recipe from the server's recipe manager when the book is disabled at runtime, and on plugin disable (Bukkit does not auto-remove recipes).
- [ ] Re-register the recipe when the book is re-enabled.
- [ ] Add a test asserting the recipe is removed on disable.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/SkillsGuideBook.java`
  - `src/main/java/io/github/chasehuegel/skilling/Skilling.java` (disable hook)
  - `src/test/java/io/github/chasehuegel/skilling/ui/SkillsGuideBookTest.java`
- **Dependencies:** none.
- **Constraints:** Keep recipe discovery (`onPlayerJoin`) behavior consistent with the enabled/disabled state.

## Verification & Definition of Done
- [ ] Disabling the book removes its recipe; re-enabling restores it.
- [ ] `./gradlew build` and `./gradlew test` pass.
