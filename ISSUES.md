# Bugs
- [x] Level up fanfare uses a bossbar instead of a titlebar
  - [x] The title should be "Level up!"
  - [x] The subtitle should be "{skill} increased to {level}"
- [x] Players can break blocks they've placed for XP
- [x] Abilities and their bonuses aren't displayed in the lore of skills in the skills menu
- [x] The `/skills addxp` command doesn't autocomplete player names
- [ ] The `/skills addxp` command doesn't autocomplete skills
- [x] The `/skills addxp` command doesn't give usage guidance when it fails
- [x] The `/skills setlevel` command doesn't autocomplete player names
- [ ] The `/skills setlevel` command doesn't autocomplete skills
- [x] The `/skills setlevel` command doesn't give usage guidance when it fails
- [ ] The XP bossbar is showing current out of total needed XP instead of the relative XP needed for the current level

# Improvements
- [ ] Add a `/skills help` command to details the commands and briefly how to use them
- [ ] Improve the styling of skills in the menu
  - [ ] By default, skill names will not be styled. Currently they appear as bold and italic.
  - [ ] Display names in the config should support color and styling encoding (ex: `&aMining`)
- [ ] Add a `/skills reset`
- [ ] Adjust the fanfare chat log for level ups to be formatted as "Level up! {skill} increased to {level}!" without changing the current styling
- [ ] Adjust the xp bossbar to be formatted as "<skill color>{skill} <light grey>- <white>{level}"