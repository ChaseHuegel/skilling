# External Plugin Integration (PlaceholderAPI, Vault, bStats)

## Issue

Skilling currently operates as a **fully self-contained plugin** with zero integration with the broader PaperMC ecosystem. This limits its utility on typical servers that rely on:

- **PlaceholderAPI** — The de-facto standard for displaying plugin data in chat, scoreboards, tab lists, and other UI elements. Without PlaceholderAPI expansion, server owners cannot display `%skilling_level_mining%` or `%skilling_xp_farming%` in their server's existing UI framework.
- **Vault** — The standard economy API. Without Vault integration, skills cannot offer economy-based features (buying XP with currency, selling skill levels, economy-based ability costs).
- **bStats** — The standard plugin metrics framework. Without bStats, there is no visibility into how many servers use Skilling, which versions they run, or what configurations are common — making regression detection and priority setting harder.

## Scope

Add **soft-dependency** integration for all three plugins. "Soft" means:
- The plugin loads and functions normally if the dependency is absent
- Integration features only activate when the dependency is detected at runtime
- No `depend:` or `loadbefore:` in `paper-plugin.yml`
- No compile-time dependency on external plugin APIs — use reflection or shaded lightweight stubs

## Affected Files

| File | Action |
|------|--------|
| `build.gradle.kts` | Add compile-only dependency on PlaceholderAPI; no change for Vault/bStats |
| `paper-plugin.yml` | Add `softdepend: [PlaceholderAPI, Vault, bStats]` |
| `engine/integration/PlaceholderAPIHook.java` | Create |
| `engine/integration/VaultHook.java` | Create |
| `engine/integration/bStatsHook.java` | Create |
| `engine/integration/IntegrationManager.java` | Create |
| `Skilling.java` | Initialize IntegrationManager in `onEnable()`, shutdown in `onDisable()` |
| `docs/api-integration.md` | Document available placeholders and economy hooks |
| `docs/configuration.md` | Document any new config keys for integration settings |

## Development Plan

### Step 1: Create IntegrationManager

A central manager that probes for available integrations at startup:

```java
// engine/integration/IntegrationManager.java
public class IntegrationManager {
    private final Skilling plugin;
    private PlaceholderAPIHook papiHook;
    private VaultHook vaultHook;
    private bStatsHook bStatsHook;

    public IntegrationManager(Skilling plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiHook = new PlaceholderAPIHook(plugin);
            papiHook.register();
            plugin.getLogger().info("PlaceholderAPI integration enabled");
        }
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            vaultHook = new VaultHook(plugin);
            vaultHook.initialize();
            plugin.getLogger().info("Vault economy integration enabled");
        }
        if (plugin.getServer().getPluginManager().getPlugin("bStats") != null) {
            bStatsHook = new bStatsHook(plugin);
            bStatsHook.initialize();
            plugin.getLogger().info("bStats metrics enabled");
        }
    }

    public void shutdown() {
        if (papiHook != null) papiHook.unregister();
        if (vaultHook != null) vaultHook.shutdown();
        // bStats doesn't need explicit shutdown
    }

    public boolean hasPlaceholderAPI() { return papiHook != null; }
    public boolean hasVault() { return vaultHook != null; }
    public boolean hasbStats() { return bStatsHook != null; }
}
```

### Step 2: PlaceholderAPI Integration

PlaceholderAPI uses an expansion registration pattern. The hook implements `PlaceholderExpansion`:

```java
// engine/integration/PlaceholderAPIHook.java
public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final Skilling plugin;

    public PlaceholderAPIHook(Skilling plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() { return "skilling"; }

    @Override
    public String getAuthor() { return "ChaseHuegel"; }

    @Override
    public String getVersion() { return plugin.getPluginMeta().getVersion(); }

    @Override
    public boolean persist() { return true; }  // Keep registered through reloads

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) return "";

        String[] parts = params.split("_", 2);
        if (parts.length < 2) return "";

        String action = parts[0];       // "level", "xp", "max_xp", "progress", "ability"
        String skillId = parts[1];       // e.g., "mining", "woodcutting"

        SkillDefinition skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) return "0";

        ProfileManager profiles = plugin.getProfileManager();
        PlayerProfile profile = profiles.get(player.getUniqueId());
        if (profile == null) return "0";

        long xp = profile.getXp(skillId);
        int level = skill.getLevelForXp(xp);

        return switch (action) {
            case "level" -> String.valueOf(level);
            case "xp" -> String.valueOf(xp);
            case "max_xp" -> String.valueOf(skill.getXpForLevel(level + 1));
            case "progress" -> {
                long current = skill.getXpForLevel(level);
                long next = skill.getXpForLevel(level + 1);
                double pct = (double)(xp - current) / (next - current) * 100;
                yield String.format("%.1f", Math.min(pct, 100.0));
            }
            case "remaining" -> {
                long next = skill.getXpForLevel(level + 1);
                yield String.valueOf(Math.max(0, next - xp));
            }
            default -> "";
        };
    }
}
```

**Supported placeholders:**
- `%skilling_level_mining%` — Player's level in the "mining" skill
- `%skilling_xp_farming%` — Player's raw XP in "farming"
- `%skilling_max_xp_fishing%` — XP needed for next level in "fishing"
- `%skilling_progress_woodcutting%` — Percentage progress to next level (e.g., "45.2")
- `%skilling_remaining_excavation%` — XP remaining for next level
- `%skilling_total_levels%` — Sum of all skill levels (convenience)

**Note for compilation:** PlaceholderAPI is available on Maven Central. Add as compile-only:

```kotlin
// build.gradle.kts
dependencies {
    compileOnly("me.clip:placeholderapi:2.11.6")
}
```

### Step 3: Vault Economy Integration

Vault provides abstract economy access. The hook provides:

```java
// engine/integration/VaultHook.java
public class VaultHook {
    private final Skilling plugin;
    private Economy economy;
    private boolean enabled;

    public VaultHook(Skilling plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        RegisteredServiceProvider<Economy> rsp = 
            plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            enabled = true;
        }
    }

    public boolean hasAccount(OfflinePlayer player) {
        return enabled && economy.hasAccount(player);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!enabled) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean canAfford(OfflinePlayer player, double amount) {
        return enabled && economy.has(player, amount);
    }

    public void shutdown() {
        // Vault doesn't require cleanup
    }
}
```

**Usage in skill mechanics:**
- **Economy cost requirement:** New `RequirementType.ECONOMY` in `RequirementEngine` — check `canAfford()` during `check()`, call `withdraw()` during `consume()`
- **XP-to-currency conversion:** New admin command or mechanic parameter to pay players for leveling up
- **Ability unlock fee:** Additional requirement gate for premium abilities

**Config keys (in config.yml):**

```yaml
economy:
  enabled: true               # Requires Vault
  xp_reward_amount: 50.0      # Currency earned per level-up
  currency_name: "Coins"      # Display name for messages
  ability_cost_enabled: false # Allow abilities to cost currency
```

**Note on compilation:** Vault is not on Maven Central. Options:
1. Use reflection to avoid compile-time dependency entirely (preferred, since Vault is a soft-dependency)
2. Add a Vault Jar to a local `libs/` directory
3. Include Vault API as a shaded dependency (not recommended — Vault may conflict)

**Recommended approach:** Use reflection for Vault. Since Vault's API is stable and simple (just `Economy` interface), reflection access is straightforward and avoids any build-time dependency:

```java
public void initialize() {
    try {
        Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
        RegisteredServiceProvider<?> rsp = plugin.getServer().getServicesManager()
            .getRegistration(economyClass);
        if (rsp != null) {
            economy = rsp.getProvider();
            enabled = true;
        }
    } catch (ClassNotFoundException e) {
        // Vault not installed
    }
}
```

### Step 4: bStats Integration

bStats provides anonymous usage metrics. Implementation follows the bStats Maven/Local class approach:

```java
// engine/integration/bStatsHook.java
public class bStatsHook {
    private final Skilling plugin;
    private Metrics metrics;

    public bStatsHook(Skilling plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        int pluginId = 33002;
        metrics = new Metrics(plugin, pluginId);

        // Custom charts
        metrics.addCustomChart(new Metrics.SimplePie("loaded_skills", () -> 
            String.valueOf(plugin.getSkillManager().getSkillCount())));

        metrics.addCustomChart(new Metrics.SimplePie("use_papi", () -> 
            plugin.getIntegrationManager().hasPlaceholderAPI() ? "yes" : "no"));

        metrics.addCustomChart(new Metrics.SimplePie("use_vault", () -> 
            plugin.getIntegrationManager().hasVault() ? "yes" : "no"));
    }
}
```

**bStats setup:**
1. Copy the latest `Metrics.java` from https://github.com/Bastian/bStats-Metrics/tree/master/bukkit into `engine/integration/`
2. Add custom charts for: loaded skills count, mechanics count, triggers count, Paper version, Java version
3. Follow the bStats license (LGPL-3.0 — compatible with Apache 2.0, just include the license notice)

Unlike PlaceholderAPI and Vault, bStats uses a **shaded local class** (not an external plugin dependency). No compile-time dependency needed.

### Step 5: Update `paper-plugin.yml`

```yaml
name: Skilling
version: ${version}
main: io.github.chasehuegel.skilling.Skilling
api-version: '1.21'
authors: [ChaseHuegel]
description: A high-performance, data-driven RPG skills engine for PaperMC
website: https://github.com/chasehuegel/skilling

softdepend:
  - PlaceholderAPI
  - Vault

permissions:
  skilling.admin:
    description: Access to administrative /skills commands
    default: op
```

Note: bStats is NOT listed as a softdepend — its Metrics.java class is shaded into the plugin JAR and communicates directly with the bStats server.

### Step 6: Integrate with existing systems

**RequirementEngine economy integration:**
Add `EconomyRequirement` as a new requirement type:

```yaml
abilities:
  - id: "premium_strike"
    requirements:
      cooldown: 10.0
      economy_cost: 100.0   # Vault currency cost per activation
```

The `RequirementEngine.check()` and `consume()` paths need a new `FailureReason.ECONOMY` and the corresponding `canAfford()`/`withdraw()` calls gated by `hasVault()`.

### Step 7: Documentation updates

**`docs/api-integration.md`:** Add section on placeholders:

```
## PlaceholderAPI

Skilling registers a `%skilling_*%` placeholder expansion:

| Placeholder | Description | Example Output |
|---|---|---|
| `%skilling_level_{skill}%` | Player's level | `47` |
| `%skilling_xp_{skill}%` | Raw XP | `12840` |
| `%skilling_progress_{skill}%` | Percent to next level | `62.5` |
| `%skilling_total_levels%` | Sum of all levels | `312` |
```

**`docs/configuration.md`:** Add economy config keys.

## Testing

- **PlaceholderAPI:** Start server with PlaceholderAPI installed. Run `/papi parse me %skilling_level_mining%` and verify output matches `/skills mining`
- **Vault:** Start server with Vault + economy plugin (e.g., EssentialsX). Create a skill with `economy_cost` requirement, verify currency is deducted on ability use
- **bStats:** Start server, verify the plugin appears on bStats.org dashboard after ~5 minutes
- **No-dependency test:** Start server without PlaceholderAPI/Vault — Skilling loads without errors, no warnings, all features work normally
- **Reload test:** Run `/skills reload` — integrations re-initialize correctly
- **Reflection safety:** If Vault classes aren't present, `NoClassDefFoundError` and `ClassNotFoundException` must be caught and handled silently

## Self-Review

- All three integrations are **soft dependencies** — the plugin works perfectly on a bare Paper server
- PlaceholderAPI is the most impactful integration (scoreboard/chat/tab) and requires only ~50 lines of hook code
- Vault integration is the most complex due to reflection-based access; the economy requirement type adds a new `FailureReason` and paths in `RequirementEngine`
- bStats is the simplest and provides long-term maintenance value (crash tracking, version adoption)
- Using reflection for Vault avoids any build-time dependency while keeping integration robust
- The `IntegrationManager` pattern keeps integration code isolated from core engine logic — if an integration fails, it doesn't affect core gameplay
- PlaceholderAPI has a `persist()` flag to survive reloads without re-registration
- Economy cost requirements follow the existing Check → Execute → Consume pipeline — the cost is checked in `check()` and deducted in `consume()`

## Future Considerations

- **Advanced placeholders:** `%skilling_evaluator_{skill}_{ability}_{param}%` — expose dynamic evaluator output for millisecond-precision display
- **Multi-economy:** Some servers run multiple Vault-compatible economies (e.g., tokens + coins) — support configurable economy name
- **Leaderboard integration:** PlaceholderAPI expansion could feed into leaderboard plugins like LeaderHeads or MiniGames
- **Discord integration:** Webhook-based level-up announcements (separate plugin or addon)
