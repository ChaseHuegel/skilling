<!--
# Issue Resolution Protocol
1. Resolve one issue (including sub-bullets) at a time.
2. For complex items, plan first.
3. Build, test, self-review, mark complete, commit.
4. Then move to the next issue.
See AGENTS.md §Issue Resolution Workflow for details.
-->

# Bugs
- [x] The archery skill is not granting XP for damaging or killing entities with a bow
- [x] (plugin) The skill guide item is opening GUI on left click; it should only open on right click
- [x] (web) Parameters fields can only be input as int values but should be floating point values
- [x] (plugin) SkillBonusMechanic says it is a % increase 
- [ ] (plugin) Abilities using mechanics providing attribute modifiers can stack with themselves repeatedly. Mechanics that use attribute modifiers should have an optional parameter for a UUID string; if not provided a random one is generated. If one is provided, then we must check if it already exists on the player. If it does then we remove it before adding it again (to refresh any duration).
  - [ ] (plugin) In the bundled skill ymls, add a separate UUID parameter to each skill ability that is using an attribute mechanic

# Improvements
- [x] (web) Sort skills on the skills dashboard page by color -> name
- [ ] (plugin) Ensure there is thorough, useful event logging for troubleshooting when `debug_logging` is enabled in the config
- [ ] (plugin) Ensure all ability lore lines include `Cost: ...` and `Requirements: ...` lines detailing their costs and requirements.
- [ ] (plugin) Add tooltips on hover of ability names logged in unlock chat messages that shows their configured lore
- [ ] (web) In the skills navigation flyout, the skill items should be sorted by color then by name just like the skill dashboard page is
- [ ] (plugin) triggers for events which may include some bulk operation should multiply the earnXp amount by the bulk operation's scalar, for ex:
  - [ ] (plugin) collect_xp triggered rewards should be multiplied by the amount of XP collected (ex: a reward of 2 skill XP for 3 minecraft XP = 6 skill XP)
  - [ ] (plugin) consume_item triggered rewards should be multiplied by the crafted item stack size (ex: a reward of 2 skill XP for 3 doors = 6 skill XP)
  - [ ] (plugin) furnace_extract triggered rewards should be multiplied by the amount of XP collected (ex: a reward of 2 skill XP for 3 minecraft XP = 6 skill XP)
- [ ] (plugin) Update bundled skill ymls that grant XP for crafting to have a filter where `target` is a tag containing items relevant to that skill (ex: carpentry should only grant XP for crafting wooden items and blocks)
- [ ] (plugin) Tailoring should only grant item_damage XP if the target is leather or chain armor