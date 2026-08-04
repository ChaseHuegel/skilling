# ISSUE-223: Migrate sound/particle feedback identifiers from legacy enum names to modern 1.21 namespaced values

## Context & User Story
- **Goal:** As a server admin, I want sound and particle feedback configured anywhere (plugin, bundled skills, web GUI) to use the current Minecraft 1.21 namespaced identifiers (e.g. `minecraft:entity_zombie_break_wooden_door`, `minecraft:block_crack`) and actually play, instead of legacy `Sound.valueOf`/`Particle.valueOf` enum names that are opaque, case-fragile, and silently skipped.
- **Agent Role:** You are an expert backend + content engineer executing this task.

## Implementation Requirements

### Plugin resolvers
- [x] `FanfareDispatcher.dispatchSounds`/`dispatchParticles` (`FanfareDispatcher.java:49-99`) must resolve identifiers via the 1.21 registries (`Registry.SOUND_EVENT`, `Registry.PARTICLE_TYPE`, keyed by `NamespacedKey`) instead of `Sound.valueOf`/`Particle.valueOf`, so namespaced values like `minecraft:entity_zombie_break_wooden_door` work.
- [x] `BlockParticlesMechanic.java:40` and `MechanicParamValidators.java:118` must validate/resolve particle identifiers the same way (namespaced, not `Particle.valueOf(type.toUpperCase())`).
- [x] Invalid identifiers should fail fast at load time (per `src/AGENTS.md` fail-fast rule) via the mechanic/feedback validation path rather than being silently swallowed at dispatch. Add load-time validation for `feedback.sounds[].type`, `feedback.particles[].type`, `on_failure.*.sounds[].type`, and the `core:block_particles` `particle` parameter.
- [x] Keep a clear, documented behavior for any identifier that cannot be resolved (reject at load is preferred; never a silent no-op).

### Bundled content
- [x] Update every sound/particle value in `src/main/resources/skills/*.yml` to the namespaced 1.21 form (currently 22 sound entries and 2 particle entries use legacy enum names, e.g. `ITEM_SHIELD_BLOCK`, `ENTITY_ZOMBIE_BREAK_WOODEN_DOOR`, `ENTITY_PLAYER_LEVELUP`, `ENTITY_ENDERMAN_TELEPORT`, `SNOWBALL`, `PORTAL`, `HAPPY_VILLAGER`, plus the `core:block_particles` `particle: { constant: "HAPPY_VILLAGER" }` in `piety.yml:137`).
- [x] Update the same values in `src/main/resources/template-skill.yml` and `docs/dev/template-skill.yml` (e.g. `BLOCK_NOTE_BLOCK_BASS`, `ENTITY_PLAYER_BURP`, `BLOCK_CRACK`, `ENTITY_ZOMBIE_BREAK_WOODEN_DOOR`).
- [x] Update `docs/users/capabilities.md` (and any other user doc) that documents legacy sound/particle names.

### Web GUI
- [x] Ensure the web editor sound/particle suggestions and placeholders are the authoritative namespaced 1.21 identifiers. `SoundConfigEditor.vue:21-51` and `AbilitiesSection.vue:16-30` already use `minecraft:` prefixed values — verify they are current for 1.21 and consistent with the plugin's new registry-based resolution.
- [x] Because staged skills are validated through the live `SkillManager` (`SkillHandler.validateStagedSkill`), confirm a web save containing an invalid sound/particle value surfaces a 400 instead of staging content that would silently fail at dispatch.

## Technical Specifications & Context
- **Target Files:**
  - `src/main/java/io/github/chasehuegel/skilling/engine/feedback/FanfareDispatcher.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/BlockParticlesMechanic.java`
  - `src/main/java/io/github/chasehuegel/skilling/engine/mechanic/impl/MechanicParamValidators.java`
  - `src/main/resources/skills/*.yml` (all 32 bundled skills)
  - `src/main/resources/template-skill.yml`, `docs/dev/template-skill.yml`
  - `docs/users/capabilities.md`
  - `web/frontend/src/components/common/SoundConfigEditor.vue`, `web/frontend/src/components/skills/AbilitiesSection.vue`
  - `web/frontend/src/components/skills/OnFailureEditor.vue` (sound placeholder text)
- **Dependencies:** Paper 1.21 `Registry.SOUND_EVENT` / `Registry.PARTICLE_TYPE`. Entity/block-data particles (e.g. `minecraft:block_crack`, `minecraft:dust`) may need data arguments when spawned — keep dispatch behavior equivalent or document the limitation.
- **Constraints:** Greenfield, no backward-compatibility obligation — migrating every legacy name in shipped content is in scope. Verify each legacy name's 1.21 namespaced equivalent (e.g. `ENTITY_ENDERMAN_TELEPORT` → `minecraft:entity_enderman_teleport`, `BLOCK_CRACK` → `minecraft:block_crack`).

## Verification & Definition of Done
- [x] All bundled skills + templates load with namespaced sound/particle identifiers and no load-time validation failures.
- [x] `FanfareDispatcher`/`BlockParticlesMechanic` resolve namespaced values against the 1.21 registries.
- [x] An invalid identifier is rejected at load (fails `validateStagedSkill`), not silently skipped.
- [x] `./gradlew build` and `./gradlew test` pass.
- [x] `cd web/frontend && npm run build` passes.
