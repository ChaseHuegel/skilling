<!--
# Issue Resolution Protocol
1. Resolve one issue (including sub-bullets) at a time.
2. For complex items, plan first.
3. Build, test, self-review, mark complete, commit.
4. Then move to the next issue.
See AGENTS.md §Issue Resolution Workflow for details.
-->

# Bugs
- [x] (Plugin) Players should be getting XP chat messages sent to them when their `/skills log xp true` is enabled
- [x] (Plugin) Opening the foldouts for any of the archery abilities, except `percing_shot`, breaks the ability editor display in the skill editor. Not certain if there is an issue with the skill ymls or the web GUI.
  - After checking other skills, wide_sweep from excavation is the only one that works there, timber_feller for woodcutting works, and harvest_wave for farming works. One similarity I can see from the GUI is all of them are the 3rd ability in their lists.
- [x] (Web) Some config options are missing from the config editor page. Come up with a development plan then ensure all config options are supported.

# Improvements
- [x] (Plugin) Add tab autocomplete support to `/skills log`
  - Ensure these aren't logged to the console or broadcast to other players
- [x] (Plugin) Players logging profile preferences should all be true by default except for XP gains
- [x] (Plugin) The skill guide recipe should be `book` + `coal` and shapeless
- [ ] (Plugin) Ensure all documentation in `docs/` is accurate and up to date with the plugin's actual src
- [ ] (Plugin & Design) The default bundled skills are missing level 75 abilities - follow the framework in SKILL-DESIGN-FRAMEWORK.md to craft well-fitting abilities and update the bundled skill ymls in the plugin appropriately.
