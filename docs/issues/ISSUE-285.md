# ISSUE-285: Offline `/skills addxp` can double-grant XP when a login races the write

## Context & User Story
- **Goal:** As an admin, I want `/skills addxp <player> <skill> <amount>` to grant exactly `amount` XP even if the target logs in while the command runs. The offline path writes `xp = xp + amount` directly to the DB, then folds the grant into any profile that hydrated during the write via `addXp(skillId, amount)`. If hydration read the row after the write committed, the live profile already contains the grant and `addXp` adds it a second time; the next write-behind flush persists the doubled value.
- **Agent Role:** You are an expert backend engineer executing this task.

## Implementation Requirements
- [x] Make the offline grant fold idempotent. Capture the target profile's session generation (or the pre-write DB value) before the direct write and only fold when the live profile predates the write, or `setXp` the absolute post-write value instead of adding.
- [x] Confirm `handleOfflineSetLevel` (idempotent) needs no change but document the asymmetry.
- [x] Add a unit/integration test simulating the race: hydration completing after the write must not double the grant.

## Technical Specifications & Context
- **Target Files:** `src/main/java/io/github/chasehuegel/skilling/engine/command/SkillsCommand.java:399-429` (`handleOfflineAddXp`; the SQL `INSERT ... ON CONFLICT ... xp = xp + ?` at ~413 and the fold at 424-428). Related: `ProfileManager.loadProfile`/`installHydrated` and `sessionGenerations` guards.
- **Dependencies:** None.
- **Constraints:** The offline command must still work when the player is fully offline (no profile in the cache). Keep the fanfare-pending flag behavior.

## Verification & Definition of Done
- [x] Race test passes: no double grant.
- [x] `./gradlew test` and `./gradlew build` pass.
- [x] Edge case handled: player offline (no profile) still receives the full grant on next login.
