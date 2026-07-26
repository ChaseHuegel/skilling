# Getting Started with Skilling

## Installation

1. Download the latest Skilling `.jar` from the releases page.
2. Place the jar in your Paper server's `plugins/` directory.
3. Restart the server (or use `/reload confirm` — though a restart is recommended).
4. On first load, Skilling generates:
   - `plugins/Skilling/config.yml` — Global plugin settings
   - `plugins/Skilling/tags.yml` — Custom tag definitions
   - `plugins/Skilling/skills/` — Directory for skill YAML files

## First Run

After installation, default configuration files are created automatically. You can verify the plugin loaded correctly with:

```
/skills
```

This opens the **skill overview UI**, showing all configured skills and their levels/XP.

## Basic Commands

| Command | Description |
|---|---|
| `/skills` | Open the skill overview UI |
| `/skills progress <skill>` | View XP and level progress for a specific skill |
| `/skills reload` | Reload all configurations (requires `skilling.admin` permission) |
| `/skills setlevel <player> <skill> <level>` | Set a player's skill level |
| `/skills addxp <player> <skill> <amount>` | Grant XP to a player |
| `/skills reset <player> [skill]` | Reset a player's progress |

## Next Steps

- [Configuration Reference](configuration.md) — Tune `config.yml` and `tags.yml`
- [Creating Skills](creating-skills.md) — Build your own skill definitions
- [API Integration](api-integration.md) — Extend Skilling with custom mechanics
- [Capabilities Catalog](capabilities.md) — All built-in triggers, mechanics, and evaluators
