<!--
# Issue Resolution Protocol
1. Resolve one issue (including sub-bullets) at a time.
2. For complex items, plan first.
3. Build, test, self-review, mark complete, commit.
4. Then move to the next issue.
See AGENTS.md §Issue Resolution Workflow for details.
-->

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
- [x] The template skill has "passive ability" line baked into its description that should be removed, it is now redundant with the active/passive display baked into lore
- [x] The template skill states an exhaustion cost but it actually has a coal cost. It should have both costs and update its description appropriately.
- [x] Vein mining has regressed, it reports the following output for the vein_miner ability activation
  ```
  [18:57:21 INFO]: [Skilling] [DEBUG]   ability=vein_miner skillLevel=24 unlockLevel=15
  [18:57:21 INFO]: [Skilling] [DEBUG]     mechanic=core:chain_break skill=mining
  [18:57:21 INFO]: [Skilling] [DEBUG]     -> mechanic not found in registry, skipping
  ```
- [x] The fireworks from levelup fanfare are dealing damage. These should deal 0 damage to any entities.
  - Preferrably, this should be accomplished via some already available API
  - We can explore some external plugin library/API to use for this, but if that is the alternative then lets pause and discuss options.
  - If absolutely necessary, this may be accomplished via some damage event listener and tagging the entities but that feels like too much. A library is likely preferrable. Again, lets pause and discuss if this isn't achievable as-is with the paper/bukkit APIs.
- [x] The level-up title listing unlocked abilities isn't displaying content on a newline; the there is an `LF` character displayed by minecraft
  - This may not be possible, but from some light research tellraw supports displaying newline characters so this may be possible in some fashion via a plugin.
  - Perform some research on this topic if necessary, and if it doesn't look possible then lets pause and discuss instead of just trying something hoping that it might work.
- [x] When using `/skills setlevel 7_eleven mining 15` there is an off-by-one error that becomes apparent. The following is observed:
  - It plays the level up fanfare for lvl 15
  - It shows the level up title for lvl 15 and the new ability unlock
  - The bossbar still shows 14 with a full bar
  - The skill menu doesn't show vein miner (the lvl 15 ability) is unlocked
  - The skill menu shows the same as the bossbar; level 14 with a full bar
  - Gaining any amount of XP triggers the level-up fanfare again
  - It appears that level ups aren't occuring when the required XP is reached, but rather when it is exceeded.
  - It appears that the setlevel command is invoking fanfare directly instead of going thru the xp pipeline? I have not confirmed this, but seems likely based off the behavior. At the least, there is some mismatch in the source of truth for level ups it appears.
- [x] The debug flag being disabled is not hiding the XP current/total count in the bossbar
- [x] Level up notifications aren't being echoed in the chat
  - [x] These should only be sent to the player that leveled up, not all players
  - [x] This should be formatted similar to the vanilla minecraft achievement message
- [x] Level up unlocks aren't being echoed in the chat
  - [x] These should only be sent to the player that leveled up, not all players
  - [x] This should be formatted similar to the vanilla minecraft achievement message
- [x] Missing ability costs aren't gating the ability activation
  - Vein miner can be activated even if the player has no coal
- [x] The exhaustion parameter of vein miner is not actually reducing the player's hunger
  - Ensure this also prevents activating the skill if it isn't met
- [x] The color format tags aren't being displayed correctly for level 100 fanfare level ups
  - This affects the subtitle and the chat message
- [x] The chain_break mechanic currently affects all blocks
  - [x] Add a parameter to specify a tag for the blocks which should be affected by it
  - [x] Update the template skill's vein miner to specify the tag `#c:stone` so the ability only affects mining-related blocks
- [x] The bossbar for xp should not display for skills after their max level has been reached
- [x] After setting notify to true for chat and action bar on the passive `geologist` ability, it does not appear to be sending any messages 
  - Via server logs can confirm the skill is being checked and passing, and in-game it appears drops are increased as expected
- [x] The fireworks launched for any fanfare should use a random color per firework launched, they appear to all use the same color currently
- [x] Geologist appears to be blocking the vein miner ability from activating when mining ores
  - [x] Ensure successfully activated abilities don't block others from activating
- [x] The chain_break mechanic should be treated as if the player cause the block break
  - [x] chain_break should still effectively raise a block break event, so plugins can respond normally
  - [x] chain_break should naturally respect any protections (vanilla protected chunks, antigrief plugins, etc.) so it can't be used as an exploit
  - [x] chain_break should naturally be respected by this plugin, granting skill XP (ie. mining) and possibly activate abilities (ie. geologist ability)
- [x] vein miner is now infinitely chaining on itself, reactivating in a loop until the player is out of hunger or coal
  - This is likely caused by the block break event changes in chain_break, or else the recent change to remove the "global" cooldown key

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
- [x] In skill lore, the line noting whether an ability is active or passive will appear on the same line appending to the ability name in dark grey and separated by a dot
- [x] The fireworks fanfare from leveling up should be random, bright colors
- [x] The level up title should show the display name of newly unlocked abilities under the subtitle, if any
- [x] The green check next to the name of unlocked skills should be removed
  - [x] The grey crossed-out mark next to the name of locked skills should be replaced by a tag on the same line appended to the skill name in dark grey and separated by a dot
- [x] The subtitle text for unlocked abilities should be formatted the exact same as they are displayed in the lore
  - [x] Format: `<light green>{check mark} {ability display name} <dark grey>{dot} {Active/Passive}`
    - [x] Use a shared utility for determining this display, which the lore and subtitle will both utilize to reduce code duplication. This is likely to be needed elsewhere as well later.
  - [x] If there are multiple unlocks for the level, ensure each is on a new line.
- [x] Remove the built-in default italics styling from skill menu item lore
- [x] Adjust the sequential level up titles
  - [x] Their duration will be configurable in `config.yml` with a default `stay` duration of 5 seconds
  - [x] For unlocks, the main title will change to "New unlock!"
  - [x] Add a delay between sequential titles that is long enough to allow the fade in and out to play for a smooth transition between titles
- [x] Increase the fanfare for reaching max level in a skill
  - [x] Shoot off 10 fireworks for the player that leveled up
  - [x] Play the major sound and fireworks fanfare for all other players on the server
  - [x] Send a subtitle to all other players on the server: "<light green>{player} <yellow>reached <light green>{level} <skill color>{skill}!"
    - [x] If the debug config flag is enabled, also send this to the player that reached max level for testing purposes
  - [x] Send a server-wide chat message similar to the vanilla minecraft achievement message indicating the player's name, the level they reached, and the skill
- [x] A config section for setting a global XP modifier. Default is `1.0`
- [x] Notify players when an ability is ready (comes off cooldown)
  - [x] In a chat message to just that player
  - [x] In the action bar
- [x] Send a chat message to players to log their ability activations in the same format as the action bar
- [ ] Custom tags should be able to reference other custom tags
  - [ ] Ignore recursive tags
  - [ ] Use-case example is the `veinminer` custom tag which is combining two custom tags into one overarching tag

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
- [ ] Commands to modify config values at runtime (`/skills set <config key> <value>`)
  - [ ] These will apply immediately
  - [ ] These will save to the config file
- [ ] Show skill item icons in the bossbar for XP gains
- [ ] Add an optional personal per-player setting to send a chat message to the player when they gain xp (`/skills log xp <true/false>`)
- [ ] Add an optional personal per-player setting to override sending players level up chat messages (`/skills log levels <true/false>`)
- [ ] Add an optional personal per-player setting to override sending players unlock chat messages (`/skills log unlocks <true/false>`)
- [ ] Add an optional personal per-player setting to override sending players ability chat messages (`/skills log abilities <true/false>`)