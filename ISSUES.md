# Bugs
- [x] Level up fanfare uses a bossbar instead of a titlebar
  - [x] The title should be "Level up!"
  - [x] The subtitle should be "{skill} increased to {level}"
- [x] Players can break blocks they've placed for XP
- [x] Abilities and their bonuses aren't displayed in the lore of skills in the skills menu
- [x] The `/skills addxp` command doesn't autocomplete player names
- [x] The `/skills addxp <player> <skill>` command doesn't autocomplete skill names
- [x] The `/skills addxp <player> <skill>` command doesn't give usage guidance when it fails. It just shows "Unknown or incomplete command, see below for error" and doesn't clarify the error or show the user the intended usage.
- [x] The `/skills setlevel <player> <skill>` command doesn't autocomplete player names
- [x] The `/skills setlevel <player> <skill>` command doesn't autocomplete skill names
- [x] The `/skills setlevel <player> <skill>` command doesn't give usage guidance when it fails. It just shows "Unknown or incomplete command, see below for error" and doesn't clarify the error or show the user the intended usage.
- [x] The commands to addxp and setlevel don't trigger any fanfare. These should be effectively go through the same pipeline as any other XP gain to trigger all side-effects of a levelup.
- [x] The XP bossbar is showing current out of total needed XP instead of the relative XP needed for the current level
- [x] The XP bossbar is going into negative values when nearing a level-up
- [x] Skills commands autocomplete display names but actually require IDs (ex: `mining` works but `Mining` is autocompleted and doesn't work)
  - [x] ID and display names should both be valid
  - [x] Auto complete will prefer display names but fallback to ids for any skills that have a null or empty display name
- [x] Abilities that haven't been unlocked are displaying raw styling codes in the skill menu item's lore
  - [x] This appears to be because unlocked abilities are forced to appear as a dark grey and the style-codes written into the config file are being ignored
- [x] The sneak mine trigger for the template mining skill is not having any effect
  - [x] Introduce logging for the ability pipeline similar to the logging that was added for the XP pipeline
- [x] The `saveResource("tags.yml", false)` and `saveResource("template-skill.yml", false)` calls in `onEnable()` log spurious warnings on every startup when the files already exist. Flip to log an info message only when files don't exist (i.e. when they are first generated fresh).
- [x] The cooldown action bar message is not formatting correctly. It all reads in unstyled plain text without any of the string replacements.
- [x] The vein miner ability is firing on left clicking a block instead of on breaking it
- [x] The action bar message for ability activation is not applying the style characters, they are appearing inline in the message
- [ ] The template skill has "passive ability" line baked into its description that should be removed, it is now redundant with the active/passive display baked into lore
- [ ] The template skill states an exhaustion cost but it actually has a coal cost. It should have both costs and update its description appropriately.
- [ ] Vein mining has regressed, it reports the following output for the vein_miner ability activation
  ```
  [18:57:21 INFO]: [Skilling] [DEBUG]   ability=vein_miner skillLevel=24 unlockLevel=15
  [18:57:21 INFO]: [Skilling] [DEBUG]     mechanic=core:chain_break skill=mining
  [18:57:21 INFO]: [Skilling] [DEBUG]     -> mechanic not found in registry, skipping
  ```
- [ ] The fireworks from levelup fanfare are dealing damage and causing knockback. These should be visual only.

# Improvements
- [x] `/skills <skill>` should display the exact same lore displayed in the skill menu for a given skill. This will provide UX consistency and reduce code duplication.
- [x] Add a `/skills help` command to details the commands and briefly how to use them
- [x] Improve the styling of skills in the menu
  - [x] Skill names will display without any styling (no bold, no italics)
  - [x] Skill names will be colored to match the skill color
  - [x] Skill icons will use stack size to reflect the skill's current level
    - [x] Skill icons will show a barrier block instead of their normal icon if they are level 0
  - [x] A line noting whether an ability is active or passive will appear the line after the ability name
  - [x] ASCII text will be used to create a styled XP bar for the current level
  - [x] A number will indicate the total XP the player has for the skill
  - [x] A red crossed-out mark will indicate that a skill isn't unlocked
    - [x] The skill name will appear light grey
  - [x] A green check will indicate a skill is unlocked
    - [x] The skill name will appear green
- [x] `all` will be added as a tab completion option to `/skills reset <player> <skill>` which will instead reset ALL skill data for a player rather than a specific skill.
- [x] Adjust the fanfare chat log for level ups to be formatted as "Level up! {skill} increased to {level}!" without changing the current styling
- [x] Adjust the xp bossbar to be formatted as "<skill color>{skill} <light grey>- <white>{level}"
  - [x] The XP number display will only appear in debug is enabled in the plugin config
- [x] Level up fanfare should be reduced for levelups that don't unlock anything new
  - [x] These minor level ups will use the default minecraft level sound and a small firework display
  - [x] Major level ups, where something is unlocked, will use the current fanfare and additional fireworks
- [x] Add some information logging to the startup of the plugin, such as:
  - [x] Indicate how many entries are in each registry
  - [x] Indicate how many skills are loaded
  - [x] Indicate how many custom tags are loaded
  - [x] Anything else that seems useful for identifying if everything has loaded & started up correctly
  - [x] Anything else that seems useful for troubleshooting startup issues off of just logs
- [ ] In skill lore, the line noting whether an ability is active or passive will appear on the same line appending to the ability name in dark grey and separated by a dot
- [ ] The fireworks fanfare from leveling up should be random, bright colors
- [ ] The level up title should show the display name of newly unlocked abilities under the subtitle, if any
- [ ] The green check next to the name of unlocked skills should be removed
  - [ ] The grey crossed-out mark next to the name of locked skills should be replaced by a tag on the same line appended to the skill name in dark grey and separated by a dot

# Ideas
- [ ] Web GUI that is hosted on the server
  - [ ] Configurable port from the plugin config
    - [ ] Defaults to 8082
  - [ ] Can be disabled from the plugin config
    - [ ] Disabled by default
  - [ ] Requires authentication to access
    - [ ] Basic auth is configurable with a username and password in the plugin config
  - [ ] Can be used to view:
    - [ ] Skills
    - [ ] Tags
    - [ ] Config
  - [ ] Can be used to edit and stage changes for:
    - [ ] Skills
    - [ ] Tags
    - [ ] Config
  - [ ] Changes aren't applied automatically:
    - [ ] User must click an apply button which will save the changes
    - [ ] After applying changes, the user is prompted with an optional reload button
      - [ ] The button will execute a reload of the plugin (ie. `skills reload`)
- [ ] An in-game way to open up and view the skills GUI that feels vanilla+ as an alternative to the `/skills` command
- [ ] A config section for setting a global XP modifier. Default is `1.0`
- [ ] Commands to modify config values at runtime (`/skills set <config key> <value>`)
  - [ ] These will apply immediately
  - [ ] These will save to the config file
- [ ] Show skill item icons in the bossbar for XP gains
