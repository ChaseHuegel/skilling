# ISSUE-106: Add lore tooltips to ability names in unlock chat messages

**Status:** Open
**Type:** Improvement
**Severity:** Low (nice-to-have UX polish)

---

## Context & User Story

- **Goal:** As a player, I want to hover over an ability name in an unlock chat message to see its configured lore, so that I can understand what the newly unlocked ability does without opening the skills menu.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Attach a `HoverEvent.showText(...)` to the ability-name component in the unlock chat message, showing the ability's resolved lore lines
- [ ] Resolve the ability's lore `{placeholder}` parameters using the same `ParameterEvaluator` map construction used by `SkillMenuBuilder.buildSkillLore`
- [ ] Decide and document scope: hover on the ability name in `formatAbilityLine` (shared, also appears in the GUI menu) vs. wrapping the line only in `LevelUpDispatcher.broadcastLevelUp` (chat-only); pick one and keep it consistent
- [ ] Keep the existing GUI skill-menu lore rendering unchanged
- [ ] Add a unit test asserting the unlock message component carries a hover event with the expected lore text

## Technical Specifications & Context

- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/LevelUpDispatcher.java` (`broadcastLevelUp`, lines 112-130)
  - `src/main/java/io/github/chasehuegel/skilling/engine/ui/SkillMenuBuilder.java` (`formatAbilityLine`, lines 319-339; `buildSkillLore` placeholder pattern, lines 256-261)
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/LoreResolver.java` (`resolveAll`)
- **Dependencies:** Adventure Component API is already used throughout; no hover events exist in the codebase yet, so this introduces the first `HoverEvent` usage. Chat lines are serialized via `LegacyComponentSerializer` for console at `LevelUpDispatcher.java:188-194` — hover data should not affect the console text.
- **Constraints:** `src/AGENTS.md` §7 Javadoc rules; use the Component API (no legacy color translations where avoidable). Do not break the shared `formatAbilityLine` usage in the GUI menu.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including the new hover-event unit test
- [ ] Unit test: the ability-name component in the unlock message has a hover event whose text equals the resolved lore
- [ ] Edge case handled: abilities with empty or absent lore still render a plain name (no crash, no empty hover tooltip)
- [ ] Runtime check: hovering an ability name in an unlock chat message shows the configured lore; the GUI skill menu is unaffected
