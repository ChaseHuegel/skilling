### Project Summary

**Skilling** is a purely data-driven RPG skills engine for modern PaperMC servers. Traditional skill plugins hardcode abilities, progression curves, and skill trees into the Java backend. Skilling does not. It acts as a modular rules engine. It provides a library of event triggers, mechanical effects, and parameter evaluators. Server owners and designers use them to build complex, 1-to-100 skill webs entirely through YAML configuration. It features asynchronous SQLite data saving, dynamic GUI generation, and a Requirements Engine that gates ability activations. Together these maintain 20 TPS under heavy multiplayer load.

---

# Skilling ⛏️✨

Skilling is a fully data-driven vanilla+ skills engine for modern PaperMC servers. It brings the architectural flexibility of an Entity-Component-System (ECS) to Minecraft gameplay mechanics.

There are zero hardcoded skills, levels, or abilities in this plugin. Instead, Skilling provides an optimized backend of **Triggers**, **Mechanics**, and **Evaluators**. Server designers construct their entire skill web through YAML. This covers gathering yields, custom active abilities, and dynamic progression curves.

## 🚀 Core Features

* **100% Data-Driven Architecture:** Define skills, custom XP curves, milestone abilities, and custom item tags via YAML without touching a single line of Java.
* **Dynamic Parameter Evaluators:** Progression is not limited to flat numbers. Use Linear, Milestone (tiered), Constant, or Random evaluators to scale ability power (e.g., cooldowns, damage, AoE size) based on the player's exact level.
* **The Requirements Engine:** Gate abilities behind complex conditions. Require players to have specific items, consume resources, wait out cooldowns, or hold certain states (e.g., sneaking) before an ability fires.
* **Fast Persistence:** Powered by SQLite in WAL (Write-Ahead Logging) mode and HikariCP connection pooling. Gameplay state is cached in-memory and asynchronously batched. Database I/O never blocks the main server thread.
* **Smart UI & Feedback:**
  * **Dynamic Chest GUIs:** Menus build themselves from your configs. Lore automatically parses and displays the math for a player's current level.
  * **Boss Bar Pool:** Real-time XP tracking uses a Least Recently Used (LRU) pool to manage screen space.
  * **Data-Driven Fanfare:** Configure custom particles, sounds, and action bar text for level-ups and ability activations directly in your YAML.

## 🛠️ For Server Owners & Designers

Skilling treats configuration like a scripting language. Using vanilla item tags (and custom defined tags), you can build complex mechanics in minutes.

### Example: Creating a "Vein Miner" Ability
```yaml
abilities:
  - id: "vein_miner"
    display_name: "Vein Miner"
    unlock_level: 15
    requirements:
      cooldown: 5.0
      state: [ "is_sneaking" ]
    mechanics:
      - type: "core:chain_break"
        filters:
          - target: "#c:ores"
        parameters:
          chain_limit:
            milestones:
              15: 3
              40: 8
              80: 16

```

### Uncapped Scaling

Configure a `max_level` for your skills, but let players accumulate XP infinitely. If you raise the level cap in a future update, players' overflow XP instantly pushes them to their new, correct level without any database migrations.

## 💻 For Developers (API)

Skilling is built to be extended. Addon developers can easily inject their own custom mechanics, triggers, or parameter evaluators into the engine's central registry.

```java
// 1. Implement the SkillMechanic interface
public class LifestealMechanic implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent damageEvent)) return false;
        double percentage = ((Number) params.getOrDefault("percentage", 0.1)).doubleValue();
        double heal = damageEvent.getDamage() * percentage;
        player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
        return true;
    }
}

// 2. Register it with the API
SkillingAPI api = Bukkit.getServicesManager().load(SkillingAPI.class);
api.getRegistries().registerMechanic("myaddon:lifesteal", LifestealMechanic.class);

```

Once registered, server owners can immediately use `type: "myaddon:lifesteal"` in their YAML configurations.

## 📦 Installation & Setup

1. Download the latest release from the [Releases](https://www.google.com/search?q=%23) page.
2. Drop the `.jar` into your Paper server's `plugins/` folder.
3. Start the server to generate the default configuration files.
4. (Optional) Edit `tags.yml` to define custom resource groupings for your specific server economy.
5. Build your skills in the `plugins/Skilling/skills/` directory.

## ⚙️ Technical Requirements

* **Target API:** Latest Paper API (Uses native component APIs for UI max-stack sizes and custom model data).
* **Java:** Java 21 LTS
* **Database:** SQLite (embedded, no external SQL server required).
