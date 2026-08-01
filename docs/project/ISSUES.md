<!--
# Issue Resolution Protocol
1. Resolve one issue (including sub-bullets) at a time.
2. For complex items, plan first.
3. Build, test, self-review, resolve issues and suggestions, mark complete, commit.
4. Then move to the next issue.
See AGENTS.md §Issue Resolution Workflow for details.
-->

# Bugs
- [ ] (plugin) Abilities using mechanics providing attribute modifiers can stack with themselves repeatedly. Mechanics that use attribute modifiers should have an optional parameter for a UUID string; if not provided a random one is generated. If one is provided, then we must check if it already exists on the player. If it does then we remove it before adding it again (to refresh any duration). See [ISSUE-102.md](ISSUE-102.md).
  - [ ] (plugin) In the bundled skill ymls, add a separate UUID parameter to each skill ability that is using an attribute mechanic
- [ ] (plugin) `SkillManager.parseInlineEvaluator` coerces string-valued parameters to `0.0` at parse time, so namespaced effect/attribute/material keys never reach mechanics at runtime (`effect: { constant: "minecraft:poison" }` → `ConstantEvaluator(0.0)`); affected abilities throw `IllegalArgumentException` when triggered. See [ISSUE-101.md](ISSUE-101.md).
  - [ ] (plugin) Add a string-capable evaluator (or raw-parameter passthrough) so string constants survive parsing and reach `PotionEffectResolver` / `ModifyAttributeMechanic` / `SetCooldownMechanic`
- [ ] (web) Ability cooldowns are failing to parse or serialize correctly when saving a cooldown on the skill editor page, the following error is received:
  ```
  Cannot construct instance of `io.github.chasehuegel.skilling.web.dto.SkillDetailDTO$EvaluatorDTO` (although at least one Creator exists): no int/Int-argument constructor/factory method to deserialize from Number value (5) at [Source: (String)"{"id":"heavy_armor","displayName":"Heavy Armor","maxLevel":100,"icon":"minecraft:diamond_chestplate","customModelData":0,"color":"BLUE","style":"SOLID","progression":{"curve":"polynomial","baseXp":50,"exponent":2.5,"base":null,"step":null,"min":null,"max":null,"value":null},"xpSources":[{"trigger":"entity_damage_taken","filters":[{"target":null,"state":"equipped:heavy","tool":null}],"reward":{"type":"constant","params":{"value":3}}}],"abilities":[{"id":"steel_plating","displayName":"Steel Platin"[truncated 4103 chars]; line: 1, column: 1991] (through reference chain: io.github.chasehuegel.skilling.web.dto.SkillDetailDTO["abilities"]->java.util.ArrayList[2]->io.github.chasehuegel.skilling.web.dto.SkillDetailDTO$AbilityDTO["requirements"]->io.github.chasehuegel.skilling.web.dto.SkillDetailDTO$RequirementsDTO["cooldown"])
  ```
  See [ISSUE-104.md](ISSUE-104.md).
- [ ] (web) The riding skill fails to parse in the web GUI when opening its skill editor or opening the abilities page. No error is logged. See [ISSUE-103.md](ISSUE-103.md).

# Improvements
- [ ] (plugin) Ensure there is thorough, useful event logging for troubleshooting when `debug_logging` is enabled in the config. See [ISSUE-108.md](ISSUE-108.md).
- [ ] (plugin) Ensure all ability lore lines include `Cost: ...` and `Requirements: ...` lines detailing their costs and requirements. See [ISSUE-109.md](ISSUE-109.md).
- [ ] (plugin) Add tooltips on hover of ability names logged in unlock chat messages that shows their configured lore. See [ISSUE-106.md](ISSUE-106.md).
- [ ] (web) In the skills navigation flyout, the skill items should be sorted by color then by name just like the skill dashboard page is. See [ISSUE-110.md](ISSUE-110.md).
- [ ] (plugin) triggers for events which may include some bulk operation should multiply the earnXp amount by the bulk operation's scalar, for ex: See [ISSUE-105.md](ISSUE-105.md).
  - [ ] (plugin) collect_xp triggered rewards should be multiplied by the amount of XP collected (ex: a reward of 2 skill XP for 3 minecraft XP = 6 skill XP)
  - [ ] (plugin) consume_item triggered rewards should be multiplied by the crafted item stack size (ex: a reward of 2 skill XP for 3 doors = 6 skill XP)
  - [ ] (plugin) furnace_extract triggered rewards should be multiplied by the amount of XP collected (ex: a reward of 2 skill XP for 3 minecraft XP = 6 skill XP)

# Research
- [ ] (plugin) Evaluate the XP curves and XP rewards of the bundled skills; identify estimated time to reach level 100 with explanations. Evaluate whether there is a consistent, fair and consistent time investment across all skills. Perform research on other similar RPG skill plugins (Ex: AuraSkills, MCMMO) and skilling games (ex: RuneScape, Valheim) for an informed base to compare to. Using your findings, provide suggested adjustments and build a consistent framework for designing XP source rewards. Produce the full report in a new markdown document named `REPORT_XP-CURVE.md`. See [ISSUE-107.md](ISSUE-107.md).