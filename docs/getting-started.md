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
| `/skills` | `skilling.use` | Opens the skill overview GUI |
| `/skills progress` | `skilling.use` | Shows XP and level progress in chat |

## Creating Your First Skill

1. Copy `template-skill.yml` to `plugins/Skilling/skills/my_skill.yml`.
2. Edit the file to define your skill's ID, progression curve, XP sources, and abilities.
3. Run `/skills reload` (requires `skilling.admin`) or restart the server.
4. Open the skill overview with `/skills` to see your new skill.

See [creating-skills.md](creating-skills.md) for the full YAML schema reference.
