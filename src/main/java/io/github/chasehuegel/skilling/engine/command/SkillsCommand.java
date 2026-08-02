package io.github.chasehuegel.skilling.engine.command;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.LevelUpDispatcher;
import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.engine.profile.PlayerPreferences;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.parser.standard.BooleanParser;
import java.util.HashSet;

public final class SkillsCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Skilling plugin;
    private final SkillManager skillManager;
    private final ProfileManager profileManager;
    private final SkillMenuBuilder skillMenuBuilder;
    private final LockdownManager lockdownManager;
    private final BossBarPool bossBarPool;

    public SkillsCommand(Skilling plugin, SkillManager skillManager, ProfileManager profileManager,
                         SkillMenuBuilder skillMenuBuilder, LockdownManager lockdownManager,
                         BossBarPool bossBarPool) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.profileManager = profileManager;
        this.skillMenuBuilder = skillMenuBuilder;
        this.lockdownManager = lockdownManager;
        this.bossBarPool = bossBarPool;
    }

    public void register() {
        var commandManager = PaperCommandManager.<Source>builder(
                PaperSimpleSenderMapper.simpleSenderMapper()
        ).executionCoordinator(ExecutionCoordinator.<Source>simpleCoordinator())
         .buildOnEnable(plugin);

        commandManager.exceptionController().registerHandler(
                InvalidSyntaxException.class,
                ctx -> ctx.context().sender().source().sendMessage(
                        MINI_MESSAGE.deserialize("<red>Invalid syntax. Usage: <yellow>/skills <command> [arguments]")));
        commandManager.exceptionController().registerHandler(
                ArgumentParseException.class,
                ctx -> {
                    String msg = ctx.exception().getCause() != null
                            ? ctx.exception().getCause().getMessage()
                            : ctx.exception().getMessage();
                    ctx.context().sender().source().sendMessage(
                            MINI_MESSAGE.deserialize("<red>" + (msg != null ? msg : "Invalid argument")));
                });

        var skills = commandManager.commandBuilder("skills");

        commandManager.command(skills
                .permission("skilling.use")
                .optional("skill", SkillParser.skillParser(skillManager))
                .handler(ctx -> {
                    Source sender = ctx.sender();
                    CommandSender commandSender = sender.source();
                    String skillId = ctx.getOrDefault("skill", null);
                    if (!(commandSender instanceof Player player)) {
                        commandSender.sendMessage(MINI_MESSAGE.deserialize("<red>Only players can use this command."));
                        return;
                    }
                    if (skillId == null) {
                        PlayerProfile profile = profileManager.getOrCreate(player);
                        player.openInventory(skillMenuBuilder.buildOverview(profile));
                    } else {
                        showProgress(player, skillId);
                    }
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("help")
                .permission("skilling.use")
                .handler(ctx -> {
                    CommandSender sender = ctx.sender().source();
                    sender.sendMessage(Component.empty());
                    sender.sendMessage(Component.text("=== Skills Commands ===", NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("/skills", NamedTextColor.YELLOW)
                            .append(Component.text(" - Open the skill overview menu, or show skill progress with a skill name", NamedTextColor.WHITE)));
                    sender.sendMessage(Component.text("/skills help", NamedTextColor.YELLOW)
                            .append(Component.text(" - Show this help", NamedTextColor.WHITE)));
                    sender.sendMessage(Component.text("/skills log <type> <true/false>", NamedTextColor.YELLOW)
                            .append(Component.text(" - Set logging preferences (xp, levels, unlocks, abilities)", NamedTextColor.WHITE)));
                    if (sender.hasPermission("skilling.admin")) {
                        sender.sendMessage(Component.text("/skills set <key> <value>", NamedTextColor.YELLOW)
                                .append(Component.text(" - Modify a config value at runtime", NamedTextColor.WHITE)));
                        sender.sendMessage(Component.text("/skills reload", NamedTextColor.YELLOW)
                                .append(Component.text(" - Reload the plugin configuration and skills", NamedTextColor.WHITE)));
                        sender.sendMessage(Component.text("/skills setlevel <player> <skill> <level>", NamedTextColor.YELLOW)
                                .append(Component.text(" - Set a player's skill level", NamedTextColor.WHITE)));
                        sender.sendMessage(Component.text("/skills addxp <player> <skill> <amount>", NamedTextColor.YELLOW)
                                .append(Component.text(" - Add XP to a player's skill", NamedTextColor.WHITE)));
                        sender.sendMessage(Component.text("/skills reset <player> [skill]", NamedTextColor.YELLOW)
                                .append(Component.text(" - Reset a player's skill(s). Omit skill to reset all.", NamedTextColor.WHITE)));
                    }
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("log")
                .permission("skilling.use")
                .handler(ctx -> {
                    ctx.sender().source().sendMessage(MINI_MESSAGE.deserialize(
                            "<yellow>Usage: /skills log <type> <true/false></yellow>"));
                    ctx.sender().source().sendMessage(MINI_MESSAGE.deserialize(
                            "<gray>Types: xp, levels, unlocks, abilities</gray>"));
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("log")
                .permission("skilling.use")
                .required("type", LogTypeParser.logTypeParser())
                .required("value", BooleanParser.booleanParser())
                .handler(ctx -> {
                    Source sender = ctx.sender();
                    CommandSender commandSender = sender.source();
                    if (!(commandSender instanceof Player player)) {
                        commandSender.sendMessage(MINI_MESSAGE.deserialize("<red>Only players can use this command."));
                        return;
                    }
                    String type = ctx.get("type");
                    boolean value = ctx.get("value");
                    PlayerProfile profile = profileManager.getOrCreate(player);
                    PlayerPreferences prefs = profile.getPreferences();
                    PlayerPreferences updated = switch (type) {
                        case "xp" -> new PlayerPreferences(value, prefs.logLevels(), prefs.logUnlocks(), prefs.logAbilities());
                        case "levels" -> new PlayerPreferences(prefs.logXp(), value, prefs.logUnlocks(), prefs.logAbilities());
                        case "unlocks" -> new PlayerPreferences(prefs.logXp(), prefs.logLevels(), value, prefs.logAbilities());
                        case "abilities" -> new PlayerPreferences(prefs.logXp(), prefs.logLevels(), prefs.logUnlocks(), value);
                        default -> prefs;
                    };
                    if (updated != prefs) {
                        // setPreferences flags the profile dirty; the async batch
                        // worker persists preferences on the next flush instead of
                        // blocking the command thread with a synchronous INSERT.
                        profile.setPreferences(updated);
                        player.sendMessage(MINI_MESSAGE.deserialize("<green>Set " + type + " logging to " + value));
                    }
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("reload")
                .permission("skilling.admin")
                .handler(ctx -> {
                    ctx.sender().source().sendMessage(MINI_MESSAGE.deserialize("<yellow>Reloading Skilling..."));
                    lockdownManager.reload();
                    ctx.sender().source().sendMessage(MINI_MESSAGE.deserialize("<green>Skilling reloaded."));
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("setlevel")
                .permission("skilling.admin")
                .required("player", PlayerNameParser.playerNameParser())
                .required("skill", SkillParser.skillParser(skillManager))
                .required("level", IntegerParser.integerParser())
                .handler(ctx -> {
                    String playerName = ctx.get("player");
                    String skillId = ctx.get("skill");
                    int level = ctx.get("level");
                    setLevel(ctx.sender().source(), playerName, skillId, level);
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("addxp")
                .permission("skilling.admin")
                .required("player", PlayerNameParser.playerNameParser())
                .required("skill", SkillParser.skillParser(skillManager))
                .required("amount", IntegerParser.integerParser())
                .handler(ctx -> {
                    String playerName = ctx.get("player");
                    String skillId = ctx.get("skill");
                    int amount = ctx.get("amount");
                    addXp(ctx.sender().source(), playerName, skillId, amount);
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("set")
                .permission("skilling.admin")
                .required("key", ConfigKeyParser.configKeyParser())
                .required("value", StringParser.stringParser())
                .handler(ctx -> {
                    String key = ctx.get("key");
                    String value = ctx.get("value");
                    var config = plugin.getConfig();
                    config.set(key, parseConfigValue(value));
                    plugin.saveConfig();
                    plugin.reloadConfigSettings();
                    ctx.sender().source().sendMessage(
                            MINI_MESSAGE.deserialize("<green>Set <yellow>" + key + " <green>to <yellow>" + value));
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("reset")
                .permission("skilling.admin")
                .required("player", PlayerNameParser.playerNameParser())
                .optional("skill", SkillParser.skillParserAllowingAll(skillManager))
                .handler(ctx -> {
                    String playerName = ctx.get("player");
                    String skillId = ctx.getOrDefault("skill", null);
                    if ("all".equals(skillId)) skillId = null;
                    reset(ctx.sender().source(), playerName, skillId);
                }));
    }

    private void showProgress(Player player, String input) {
        SkillDefinition def = skillManager.getSkill(input);
        if (def == null) {
            def = skillManager.getSkills().values().stream()
                    .filter(s -> s.id().equalsIgnoreCase(input)
                            || (s.display() != null && s.display().name() != null
                            && s.display().name().equalsIgnoreCase(input)))
                    .findFirst()
                    .orElse(null);
        }
        if (def == null) {
            player.sendMessage(MINI_MESSAGE.deserialize("<red>Unknown skill: <white>" + input));
            return;
        }
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(MINI_MESSAGE.deserialize("<red>Profile not loaded."));
            return;
        }
        String name = def.display() != null && def.display().name() != null
                ? def.display().name() : def.id();
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== " + name + " ===", NamedTextColor.GOLD));
        for (Component line : skillMenuBuilder.buildSkillLore(def, profile)) {
            player.sendMessage(line);
        }
    }

    private static final String USAGE_SETLEVEL = "<yellow>Usage: /skills setlevel <player> <skill> <level></yellow>";
    private static final String USAGE_ADDXP = "<yellow>Usage: /skills addxp <player> <skill> <amount></yellow>";
    private static final String USAGE_RESET = "<yellow>Usage: /skills reset <player> [<skill>]</yellow>";

    private void setLevel(CommandSender sender, String playerName, String skillId, int level) {
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
            if (profile == null) {
                sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_SETLEVEL));
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>Profile not loaded for " + playerName + "."));
                return;
            }
            SkillDefinition def = skillManager.getSkill(skillId);
            if (def == null) {
                sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_SETLEVEL));
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>Unknown skill: " + skillId));
                return;
            }
            int oldLevel = def.getLevelForXp(profile.getXp(skillId));
            long xp = (long) def.progression().evaluator().evaluate(level, 0);
            profile.setXp(skillId, xp);
            int actualLevel = def.getLevelForXp(profile.getXp(skillId));
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>Set " + playerName + "'s " + skillId + " to level " + actualLevel + "."));
            showXpBossBar(target, def, profile);
            if (actualLevel > oldLevel) {
                broadcastLevelUp(target, def, actualLevel);
            }
        } else {
            handleOfflineSetLevel(sender, playerName, skillId, level);
        }
    }

    private void handleOfflineSetLevel(CommandSender sender, String playerName, String skillId, int level) {
        SkillDefinition def = skillManager.getSkill(skillId);
        if (def == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_SETLEVEL));
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Unknown skill: " + skillId));
            return;
        }
        long xp = (long) def.progression().evaluator().evaluate(level, 0);
        var offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_SETLEVEL));
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Player not found: " + playerName));
            return;
        }
        String uuid = offlinePlayer.getUniqueId().toString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_skills (player_uuid, skill_id, xp, fanfare_pending) VALUES (?, ?, ?, 1) ON CONFLICT(player_uuid, skill_id) DO UPDATE SET xp = ?, fanfare_pending = 1";
            try (var conn = plugin.getDatabaseManager().getConnection();
                 var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid);
                stmt.setString(2, skillId);
                stmt.setLong(3, xp);
                stmt.setLong(4, xp);
                stmt.executeUpdate();
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MINI_MESSAGE.deserialize("<green>Set " + playerName + "'s " + skillId + " to level " + level + " (offline, fanfare pending).")));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MINI_MESSAGE.deserialize("<red>Database error: " + e.getMessage())));
            }
        });
    }

    private void addXp(CommandSender sender, String playerName, String skillId, int amount) {
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
            if (profile == null) {
                sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_ADDXP));
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>Profile not loaded for " + playerName + "."));
                return;
            }
            SkillDefinition def = skillManager.getSkill(skillId);
            if (def == null) {
                sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_ADDXP));
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>Unknown skill: " + skillId));
                return;
            }
            int oldLevel = def.getLevelForXp(profile.getXp(skillId));
            profile.addXp(skillId, amount);
            int newLevel = def.getLevelForXp(profile.getXp(skillId));
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>Added " + amount + " XP to " + playerName + "'s " + skillId + "."));
            showXpBossBar(target, def, profile);
            if (newLevel > oldLevel) {
                broadcastLevelUp(target, def, newLevel);
            }
        } else {
            handleOfflineAddXp(sender, playerName, skillId, amount);
        }
    }

    private void handleOfflineAddXp(CommandSender sender, String playerName, String skillId, int amount) {
        SkillDefinition def = skillManager.getSkill(skillId);
        if (def == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_ADDXP));
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Unknown skill: " + skillId));
            return;
        }
        var offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_ADDXP));
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Player not found: " + playerName));
            return;
        }
        String uuid = offlinePlayer.getUniqueId().toString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_skills (player_uuid, skill_id, xp, fanfare_pending) VALUES (?, ?, ?, 1) ON CONFLICT(player_uuid, skill_id) DO UPDATE SET xp = xp + ?, fanfare_pending = 1";
            try (var conn = plugin.getDatabaseManager().getConnection();
                 var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid);
                stmt.setString(2, skillId);
                stmt.setLong(3, amount);
                stmt.setLong(4, amount);
                stmt.executeUpdate();
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MINI_MESSAGE.deserialize("<green>Added " + amount + " XP to " + playerName + "'s " + skillId + " (offline, fanfare pending).")));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MINI_MESSAGE.deserialize("<red>Database error: " + e.getMessage())));
            }
        });
    }

    private void reset(CommandSender sender, String playerName, String skillId) {
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
            if (profile == null) {
                sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_RESET));
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>Profile not loaded for " + playerName + "."));
                return;
            }
            if (skillId != null) {
                profile.setXp(skillId, 0);
                sender.sendMessage(MINI_MESSAGE.deserialize("<green>Reset " + playerName + "'s " + skillId + "."));
            } else {
                for (String id : new HashSet<>(profile.getXpMap().keySet())) {
                    profile.setXp(id, 0);
                }
                sender.sendMessage(MINI_MESSAGE.deserialize("<green>Reset all skills for " + playerName + "."));
            }
        } else {
            handleOfflineReset(sender, playerName, skillId);
        }
    }

    private void handleOfflineReset(CommandSender sender, String playerName, String skillId) {
        var offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_RESET));
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Player not found: " + playerName));
            return;
        }
        String uuid = offlinePlayer.getUniqueId().toString();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (var conn = plugin.getDatabaseManager().getConnection()) {
                if (skillId != null) {
                    String sql = "UPDATE player_skills SET xp = 0 WHERE player_uuid = ? AND skill_id = ?";
                    try (var stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, uuid);
                        stmt.setString(2, skillId);
                        stmt.executeUpdate();
                    }
                    Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(MINI_MESSAGE.deserialize("<green>Reset " + playerName + "'s " + skillId + " (offline).")));
                } else {
                    String sql = "DELETE FROM player_skills WHERE player_uuid = ?";
                    try (var stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, uuid);
                        stmt.executeUpdate();
                    }
                    Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(MINI_MESSAGE.deserialize("<green>Reset all skills for " + playerName + " (offline).")));
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(MINI_MESSAGE.deserialize("<red>Database error: " + e.getMessage())));
            }
        });
    }

    private static Object parseConfigValue(String raw) {
        if (raw.equalsIgnoreCase("true")) return true;
        if (raw.equalsIgnoreCase("false")) return false;
        try {
            if (raw.contains(".")) return Double.parseDouble(raw);
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private void showXpBossBar(Player player, SkillDefinition skill, PlayerProfile profile) {
        LevelUpDispatcher.showXpBossBar(player, skill, profile, bossBarPool, plugin);
    }

    private void broadcastLevelUp(Player player, SkillDefinition skill, int newLevel) {
        LevelUpDispatcher.broadcastLevelUp(player, skill, newLevel, plugin, bossBarPool);
    }
}
