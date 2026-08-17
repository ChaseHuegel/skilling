## Active Sprint / Current Milestone

### Bugs
- [x] [ISSUE-277](ISSUE-277.md) - E2E: fix remaining pre-existing flakes (save-replace dirty dialog and mobile lore)

### Improvements

### Research

## Backlog

### Bugs
- [x] [ISSUE-278](ISSUE-278.md) - Fix pre-existing ChainBreakMechanicTest budget failures
- [x] [ISSUE-279](ISSUE-279.md) - Shaded JAR omits the SQLite JDBC driver and ships un-remapped service files
- [x] [ISSUE-280](ISSUE-280.md) - SkillManager raw casts throw ClassCastException instead of fail-fast IllegalArgumentException
- [x] [ISSUE-281](ISSUE-281.md) - Unknown item-requirement slot throws on the event path and aborts ability dispatch
- [x] [ISSUE-282](ISSUE-282.md) - `level_up` XP sources can self-trigger an unbounded XP cascade
- [x] [ISSUE-283](ISSUE-283.md) - AttributeModifierHelper applies a modifier before scheduling removal, leaking a permanent stacking buff
- [x] [ISSUE-284](ISSUE-284.md) - Cooldowns and failure-feedback debounce keyed by `abilityId` alone collide across skills
- [x] [ISSUE-285](ISSUE-285.md) - Offline `/skills addxp` can double-grant XP when a login races the write
- [x] [ISSUE-286](ISSUE-286.md) - Level anchor mismatch between the threshold table and `setlevel`/XP-bar math
- [x] [ISSUE-287](ISSUE-287.md) - SQLite multi-writer pool with no busy-timeout tuning causes intermittent "database is locked"
- [x] [ISSUE-288](ISSUE-288.md) - `LevelThresholds` mutates the map inside `ConcurrentHashMap.computeIfAbsent`
- [x] [ISSUE-289](ISSUE-289.md) - Failed preference load silently overwrites the player's real preferences on the next flush
- [x] [ISSUE-290](ISSUE-290.md) - Deleting or renaming a nested skill through the GUI silently does nothing
- [x] [ISSUE-291](ISSUE-291.md) - Reload success clears the staging directory and wipes edits made while the reload was in flight
- [x] [ISSUE-292](ISSUE-292.md) - Frontend is silently omitted from the JAR on clean builds and never rebuilt on source changes
- [x] [ISSUE-293](ISSUE-293.md) - `skilling.use` permission used by player commands is undeclared, making `/skills` ops-only
- [x] [ISSUE-294](ISSUE-294.md) - Shipped `template-skill.yml` is rejected by the parser; it lacks the required ability `trigger`
- [x] [ISSUE-296](ISSUE-296.md) - Engine minor fixes: block metadata, event ordering, and mechanic edge cases
- [x] [ISSUE-297](ISSUE-297.md) - Database/profile minor fixes: leaks, shutdown ordering, and silent data-loss edges
- [x] [ISSUE-298](ISSUE-298.md) - Web backend security hardening: origin checks, auth timing, and resource bounds
- [x] [ISSUE-299](ISSUE-299.md) - Web GUI behavior fixes: reload rollback, staged reads, performance, and serialization

### Improvements
- [ ] [ISSUE-305](ISSUE-305.md) - Engine triggers and state filter for the gameplay-coverage batch (compost, trade, barter, recipe_discover, smith, mend, map_fill, cartography, vault_change, sniffer, potion_splash, honey_level)
- [ ] [ISSUE-306](ISSUE-306.md) - Excavation: archaeology XP sources (brushing, suspicious blocks, decorated pots)
- [ ] [ISSUE-307](ISSUE-307.md) - Building: redstone craft and place XP sources
- [ ] [ISSUE-308](ISSUE-308.md) - Farming: compost XP and the Compost Bloom ability (core:area_fertilize)
- [ ] [ISSUE-309](ISSUE-309.md) - Smithing: smithing-table and mending XP sources
- [ ] [ISSUE-310](ISSUE-310.md) - Alchemy: potion-throw XP sources (splash and lingering)
- [ ] [ISSUE-311](ISSUE-311.md) - Husbandry: honey harvest and sniffer XP sources (honey_level gated)
- [ ] [ISSUE-312](ISSUE-312.md) - New bundled skill: Trade (draft design, refinement required)
- [ ] [ISSUE-313](ISSUE-313.md) - New bundled skill: Exploration
- [x] [ISSUE-302](ISSUE-302.md) - Engine pieces for a persistent-attribute Survival skill (persistent_attribute, reconcile hardening, cause filter, chunk_load, sleep)
- [x] [ISSUE-303](ISSUE-303.md) - Bundled Survival skill: max hearts, environmental resilience, campfire/bed camping through-line
- [x] [ISSUE-295](ISSUE-295.md) - Plugin version is hardcoded to `1.0-SNAPSHOT`; tagged releases never report the tag version
- [x] [ISSUE-300](ISSUE-300.md) - Build/CI/frontend hygiene: duplicate tests, E2E artifacts, relocation, and doc drift

### Research
- [x] [ISSUE-304](ISSUE-304.md) - Research: Minecraft gameplay coverage audit and new-skill/ability proposals
- [x] [ISSUE-301](ISSUE-301.md) - Design decision: hardcoded gameplay tables vs the data-driven golden rule