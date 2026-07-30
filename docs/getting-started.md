# Getting Started with Skilling

## Installation

1. Download the latest Skilling `.jar` from the releases page.
2. Place the jar in your Paper server's `plugins/` folder.
3. Restart the server to generate default configuration files.
4. Configure your skills in `plugins/Skilling/skills/`.

## First Run

On first launch, Skilling creates the following structure:

```
plugins/Skilling/
├── config.yml          # Global settings (database, boss bars, debounce)
├── tags.yml            # Custom tag definitions (#c:ores, #c:logs, etc.)
├── template-skill.yml  # Annotated example skill definition
└── skills/             # Place your .yml skill definitions here
```

## Basic Commands

| Command | Permission | Description |
|---|---|---|
| `/skills` | `skilling.use` | Opens the skill overview GUI, or shows skill progress with a skill name |
| `/skills help` | `skilling.use` | Shows command usage information |
| `/skills log <type> <true/false>` | `skilling.use` | Set logging preferences (xp, levels, unlocks, abilities) |
| `/skills setlevel <player> <skill> <level>` | `skilling.admin` | Sets a player's skill level |
| `/skills addxp <player> <skill> <amount>` | `skilling.admin` | Adds XP to a player's skill |
| `/skills reset <player> [skill]` | `skilling.admin` | Resets a player's skill(s). Omit skill to reset all. |
| `/skills set <key> <value>` | `skilling.admin` | Modify a config value at runtime |
| `/skills reload` | `skilling.admin` | Reload the plugin configuration and skills |

## Creating Your First Skill

1. Copy `template-skill.yml` to `plugins/Skilling/skills/my_skill.yml`.
2. Edit the file to define your skill's ID, progression curve, XP sources, and abilities.
3. Run `/skills reload` (requires `skilling.admin`) or restart the server.
4. Open the skill overview with `/skills` to see your new skill.

See [creating-skills.md](creating-skills.md) for the full YAML schema reference.
