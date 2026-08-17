# ISSUE-309: Smithing — smithing-table and mending XP sources

## Context & User Story
- **Goal:** As a server owner, I want using the smithing table (re-forging and armor trims) and the Mending enchantment rewarded in Smithing, so those under-used loops feed the skill.
- **Agent Role:** You are an expert content engineer editing bundled skill YAML. Engine triggers `smith` and `mend` come from ISSUE-305.

## Implementation Requirements
- [ ] Add a Smithing XP source on the `smith` trigger (~80), rewarding smithing-table use including armor trims.
- [ ] Add a Smithing XP source on the `mend` trigger (~30), rewarding Mending repairs.
- [ ] No new abilities are added to Smithing.

## Technical Specifications & Context
- **Target Files:** `src/main/resources/skills/smithing.yml`.
- **Dependencies:** ISSUE-305 `smith` and `mend` triggers.
- **Constraints:** Keep trim benefits to XP only. Trims are cosmetic; do not grant combat stats (Vanilla+ restraint).

## Verification & Definition of Done
- [ ] Bundled-skill auto-sweeps pass.
- [ ] `./gradlew build` and `./gradlew test` pass.