### Project Summary

**Skilling** is a high-performance, purely data-driven RPG skills engine built for modern PaperMC servers. Unlike traditional skill plugins that hardcode abilities, progression curves, and skill trees into the Java backend, Skilling acts as a modular rules engine. It provides a robust library of event triggers, mechanical effects, and parameter evaluators, allowing server owners and designers to build complex, 1-to-100 skill webs entirely via YAML configuration. It features asynchronous SQLite data saving, dynamic GUI generation, and a rigorous Requirements Engine to gate ability activations—all optimized to maintain a flawless 20 TPS under heavy multiplayer load.

---

# Skilling ⛏️✨

Skilling is a next-generation, fully data-driven vanilla+ skills engine for modern PaperMC servers. It brings the architectural flexibility of an Entity-Component-System (ECS) to Minecraft gameplay mechanics. 

There are zero hardcoded skills, levels, or abilities in this plugin. Instead, Skilling provides a highly optimized backend of **Triggers**, **Mechanics**, and **Evaluators**. Server designers construct their entire skill web—from gathering yields and custom active abilities to dynamic progression curves—entirely through YAML.

## 🚀 Core Features

* **100% Data-Driven Architecture:** Define skills, custom XP curves, milestone abilities, and custom item tags via YAML without touching a single line of Java.
* **Dynamic Parameter Evaluators:** Progression isn't limited to flat numbers. Use Linear, Milestone (tiered), Constant, or Random evaluators to smoothly scale ability power (e.g., cooldowns, damage, AoE size) based on the player's exact level.
* **The Requirements Engine:** Gate abilities behind complex conditions. Require players to have specific items, consume resources, wait out cooldowns, or hold certain states (e.g., sneaking) before an ability fires.
* **High-Performance Persistence:** Powered by SQLite in WAL (Write-Ahead Logging) mode and HikariCP connection pooling. Gameplay state is cached in-memory and asynchronously batched, ensuring database I/O never blocks the main server thread.
* **Smart UI & Feedback:**
  * **Dynamic Chest GUIs:** Menus build themselves from your configs. Lore automatically parses and displays the math for a player's current level.
  * **Boss Bar Pool:** Real-time XP tracking utilizing a Least Recently Used (LRU) pool to manage screen real estate elegantly.
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
    public LifestealMechanic(ConfigurationSection config) { ... }
    
    @Override
    public void execute(Player player, int currentLevel, int unlockLevel, Event event) {
        // Your custom logic here
    }
}

// 2. Register it with the API
SkillingAPI.getRegistry().registerMechanic("myaddon:lifesteal", LifestealMechanic.class);

```

Once registered, server owners can immediately use `type: "myaddon:lifesteal"` in their YAML configurations.

## 📦 Installation & Setup

1. Download the latest release from the [Releases](https://www.google.com/search?q=%23) page.
2. Drop the `.jar` into your Paper server's `plugins/` folder.
3. Start the server to generate the default configuration files.
4. (Optional) Edit `tags.yml` to define custom resource groupings for your specific server economy.
5. Build your skills in the `plugins/Skilling/skills/` directory.

## ⚙️ Technical Requirements

* **Target API:** Latest Paper API (Utilizes native component APIs for UI max-stack sizes and custom model data).
* **Java:** Java 21 LTS
* **Database:** SQLite (embedded, no external SQL server required).
