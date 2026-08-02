# ISSUE-113: Persist off-hand durability in `OffhandStrikeMechanic` and filter interact actions

**Status:** Open
**Type:** Bug
**Severity:** Critical (durability exploit + ability fires on unintended interactions)

---

## Context & User Story

- **Goal:** As a player, I want the off-hand strike ability to consume the stated 1 durability from my off-hand item and to trigger only on right-click-with-item actions, so that repeated strikes wear down my tool and I cannot deal free damage through walls or on every chest/left-click.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements

- [ ] Write the mutated off-hand `ItemStack` back to the inventory slot (`setItemInOffHand`) so the durability decrement is actually applied
- [ ] Guard the interact action so the mechanic only fires on right-click-with-item (`event.getAction()` / `useItemInHand()`), not left-clicks, block clicks, or empty-hand interactions
- [ ] Handle the off-hand being unbreakable/air/empty gracefully (no-op, return false, no durability mutation)
- [ ] Add unit tests covering: durability decrement is persisted, non-right-click actions return false, air/unbreakable off-hand returns false

## Technical Specifications & Context

- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/OffhandStrikeMechanic.java:47-67`
- **Dependencies:** The mechanic is dispatched from `fireAbilities` (`SkillEventListener.java:439-515`) on `player_interact`.
- **Constraints:** Fail-fast on malformed params; keep the class Javadoc (`OffhandStrikeMechanic.java:14-27`) accurate after the change.

### Root Cause

`player.getInventory().getItemInOffHand()` returns a **copy** of the off-hand stack. The mechanic mutates that copy's `Damageable` meta (`damageable.setDamage(...)`, `offhand.setItemMeta(...)`) but never calls `player.getInventory().setItemInOffHand(...)`, so the durability change is discarded. Additionally the only event check is `event instanceof PlayerInteractEvent`, so the mechanic runs on any interact — including left-clicks (main-hand attack + free off-hand strike) and right-clicks on blocks like chests and doors.

### Proposed Fix

After applying damage, write the stack back via `player.getInventory().setItemInOffHand(offhand)`. Before executing, verify the action is a `RIGHT_CLICK_AIR`/`RIGHT_CLICK_BLOCK` with a `useItemInHand()` result, and bail (return false, no consume) otherwise. If the off-hand is air or unbreakable, return false without dealing damage.

## Verification & Definition of Done

- [ ] `./gradlew build && ./gradlew test` pass, including new unit tests
- [ ] Unit test: after `execute`, the off-hand slot contains a stack with damage incremented by 1
- [ ] Unit test: left-click and block-click interactions do not trigger the mechanic
- [ ] Unit test: air/unbreakable off-hand returns false and deals no damage
