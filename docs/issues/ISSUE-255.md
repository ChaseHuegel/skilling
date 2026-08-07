# ISSUE-255: Web — Tolerate Id-Referenced Abilities in the Editor Serializers

## Context & User Story
- **Goal:** As a server owner, I want the web GUI to keep working after bundled skills reference abilities by id. Opening `mining.yml` in the editor must not error, even though the editor has no notion of ability references yet.
- **Agent Role:** You are an expert frontend and backend engineer executing this task. This is the minimal keep-working change. Editor support for referenced abilities and nested-skill file resolution is explicitly out of scope and tracked as a follow-up.

## Implementation Requirements
- [ ] `SkillSerializer.parseAbility(...)`: allow a reference-shaped ability entry (an `id` present with no `trigger`) instead of throwing `IllegalArgumentException` on the missing trigger.
- [ ] `SkillSerializer.abilityToMap(...)`: skip writing a `trigger` and other fields when they are null or blank, so a no-op GUI round-trip does not add spurious overrides to a referenced ability.
- [ ] Confirm `GET /api/skills/mining` returns the skill detail without a 500 after ISSUE-254.
- [ ] Document the no-editor-support scope in `web/AGENTS.md`.

## Technical Specifications & Context
- **Target Files:**
  - Modified: `src/main/java/io/github/chasehuegel/skilling/web/dto/SkillSerializer.java` and any DTO touched by the tolerance
  - Modified: `web/AGENTS.md`
- **Dependencies:** ISSUE-251 (tags paths) and ISSUE-254 (referenced `vein_miner`) must land first.
- **Constraints:** Per `web/AGENTS.md`, do not use `v-html`, keep the build free of dead code. No editor feature work in this issue. Saving a referenced ability through the GUI may expand it; that trade-off is accepted and documented.

## Verification & Definition of Done
- [ ] `cd web/frontend && npm run build` passes.
- [ ] `./gradlew build` and `./gradlew test` pass.
- [ ] `GET /api/skills/mining` returns a skill detail containing the `vein_miner` id reference without a server error.
