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
import io.github.chasehuegel.skilling.engine.ui.branding.BrandingConfig;
import io.github.chasehuegel.skilling.engine.ui.branding.TemplateRenderer;
import net.kyori.adventure.text.Component;
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
import java.util.Map;
import java.util.UUID;

public final class SkillsCommand {

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
                        render("error", Map.of("message", "Invalid syntax. Usage: /skills <command> [arguments]"))));
        commandManager.exceptionController().registerHandler(
                ArgumentParseException.class,
                ctx -> {
                    String msg = ctx.exception().getCause() != null
                            ? ctx.exception().getCause().getMessage()
                            : ctx.exception().getMessage();
                    ctx.context().sender().source().sendMessage(
                            render("error", Map.of("message", msg != null ? msg : "Invalid argument")));
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
                        commandSender.sendMessage(render("error", Map.of("message", "Only players can use this command.")));
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
                    sender.sendMessage(render("header", Map.of("title", "Skills Commands")));
                    sender.sendMessage(render("command", Map.of("command", "/skills"))
                            .append(render("description", Map.of("description", " - Open the skill overview menu, or show skill progress with a skill name"))));
                    sender.sendMessage(render("command", Map.of("command", "/skills help"))
                            .append(render("description", Map.of("description", " - Show this help"))));
                    sender.sendMessage(render("command", Map.of("command", "/skills log <type> <true/false>"))
                            .append(render("description", Map.of("description", " - Set logging preferences (xp, levels, unlocks, abilities)"))));
                    if (sender.hasPermission("skilling.admin")) {
                        sender.sendMessage(render("command", Map.of("command", "/skills set <key> <value>"))
                                .append(render("description", Map.of("description", " - Modify a config value at runtime"))));
                        sender.sendMessage(render("command", Map.of("command", "/skills reload"))
                                .append(render("description", Map.of("description", " - Reload the plugin configuration and skills"))));
                        sender.sendMessage(render("command", Map.of("command", "/skills setlevel <player> <skill> <level>"))
                                .append(render("description", Map.of("description", " - Set a player's skill level"))));
                        sender.sendMessage(render("command", Map.of("command", "/skills addxp <player> <skill> <amount>"))
                                .append(render("description", Map.of("description", " - Add XP to a player's skill"))));
                        sender.sendMessage(render("command", Map.of("command", "/skills reset <player> [skill]"))
                                .append(render("description", Map.of("description", " - Reset a player's skill(s). Omit skill to reset all."))));
                    }
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("log")
                .permission("skilling.use")
                .handler(ctx -> {
                    ctx.sender().source().sendMessage(render("usage", Map.of("usage", "/skills log <type> <true/false>")));
                    ctx.sender().source().sendMessage(render("info", Map.of("message", "Types: xp, levels, unlocks, abilities")));
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
                        commandSender.sendMessage(render("error", Map.of("message", "Only players can use this command.")));
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
                        player.sendMessage(render("success", Map.of("message",
                                "Set " + type + " logging to " + value)));
                    }
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("reload")
                .permission("skilling.admin")
                .handler(ctx -> {
                    ctx.sender().source().sendMessage(render("info", Map.of("message", "Reloading Skilling...")));
                    // The reload completes asynchronously (DB flush on a worker,
                    // then the rebuild back on the main thread); report completion
                    // on the main thread so the admin is told when it actually
                    // finished instead of when the command returned.
                    CommandSender source = ctx.sender().source();
                    lockdownManager.reloadAsync().whenComplete((v, ex) ->
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    source.sendMessage(ex == null
                                            ? render("success", Map.of("message", "Skilling reloaded."))
                                            : render("error", Map.of("message", "Skilling reload failed. Check the console.")))));
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
                .handler(ctx -> handleSetConfig(ctx.sender().source(), ctx.get("key"), ctx.get("value"))));

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
            player.sendMessage(render("error", Map.of("message", "Unknown skill: " + input)));
            return;
        }
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(render("error", Map.of("message", "Profile not loaded.")));
            return;
        }
        String name = def.display() != null && def.display().name() != null
                ? def.display().name() : def.id();
        player.sendMessage(Component.empty());
        player.sendMessage(render("header", Map.of("title", name)));
        for (Component line : skillMenuBuilder.buildSkillLore(def, profile)) {
            player.sendMessage(line);
        }
    }

    private static final String USAGE_SETLEVEL = "/skills setlevel <player> <skill> <level>";
    private static final String USAGE_ADDXP = "/skills addxp <player> <skill> <amount>";
    private static final String USAGE_RESET = "/skills reset <player> [<skill>]";

    private void setLevel(CommandSender sender, String playerName, String skillId, int level) {
        if (level < 0) {
            sendUsage(sender, USAGE_SETLEVEL);
            sender.sendMessage(render("error", Map.of("message", "Level must be a non-negative integer.")));
            return;
        }
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
            if (profile == null) {
                sendUsage(sender, USAGE_SETLEVEL);
                sender.sendMessage(render("error", Map.of("message", "Profile not loaded for " + playerName + ".")));
                return;
            }
            SkillDefinition def = skillManager.getSkill(skillId);
            if (def == null) {
                sendUsage(sender, USAGE_SETLEVEL);
                sender.sendMessage(render("error", Map.of("message", "Unknown skill: " + skillId)));
                return;
            }
            int oldLevel = def.getLevelForXp(profile.getXp(skillId));
            long xp = def.getXpForLevel(level);
            profile.setXp(skillId, xp);
            profile.invalidatePageCache();
            int actualLevel = def.getLevelForXp(profile.getXp(skillId));
            sender.sendMessage(render("success", Map.of("message",
                    "Set " + playerName + "'s " + skillId + " to level " + actualLevel + ".")));
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
            sendUsage(sender, USAGE_SETLEVEL);
            sender.sendMessage(render("error", Map.of("message", "Unknown skill: " + skillId)));
            return;
        }
        long xp = def.getXpForLevel(level);
        var offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sendUsage(sender, USAGE_SETLEVEL);
            sender.sendMessage(render("error", Map.of("message", "Player not found: " + playerName)));
            return;
        }
        UUID uuid = offlinePlayer.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Coordinate with the write-behind cache: if the target joined while
            // the command ran, mutate the live cached profile instead of the DB.
            // A direct DB write could otherwise be overwritten (and its pending
            // fanfare cleared) by the profile's next flush.
            PlayerProfile live = profileManager.getProfile(uuid);
            if (live != null) {
                live.setXp(skillId, xp);
                live.invalidatePageCache();
                live.addPendingFanfare(skillId);
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(render("success", Map.of("message",
                            "Set " + playerName + "'s " + skillId + " to level " + level + " (live profile, fanfare pending)."))));
                return;
            }
            String uuidStr = uuid.toString();
            String sql = "INSERT INTO player_skills (player_uuid, skill_id, xp, fanfare_pending) VALUES (?, ?, ?, 1) ON CONFLICT(player_uuid, skill_id) DO UPDATE SET xp = ?, fanfare_pending = 1";
            try (var conn = plugin.getDatabaseManager().getConnection();
                 var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuidStr);
                stmt.setString(2, skillId);
                stmt.setLong(3, xp);
                stmt.setLong(4, xp);
                stmt.executeUpdate();
                // The target may have joined while the write was in flight; fold
                // the grant into the now-live profile so a stale flush cannot
                // overwrite it. Its baseline matches the pre-write DB value.
                PlayerProfile joined = profileManager.getProfile(uuid);
                if (joined != null) {
                    joined.setXp(skillId, xp);
                    joined.invalidatePageCache();
                    joined.addPendingFanfare(skillId);
                }
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(render("success", Map.of("message",
                            "Set " + playerName + "'s " + skillId + " to level " + level + " (offline, fanfare pending)."))));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(render("error", Map.of("message", "Database error: " + e.getMessage()))));
            }
        });
    }

    private void addXp(CommandSender sender, String playerName, String skillId, int amount) {
        if (amount < 0) {
            sendUsage(sender, USAGE_ADDXP);
            sender.sendMessage(render("error", Map.of("message", "Amount must be a non-negative integer.")));
            return;
        }
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
            if (profile == null) {
                sendUsage(sender, USAGE_ADDXP);
                sender.sendMessage(render("error", Map.of("message", "Profile not loaded for " + playerName + ".")));
                return;
            }
            SkillDefinition def = skillManager.getSkill(skillId);
            if (def == null) {
                sendUsage(sender, USAGE_ADDXP);
                sender.sendMessage(render("error", Map.of("message", "Unknown skill: " + skillId)));
                return;
            }
            int oldLevel = def.getLevelForXp(profile.getXp(skillId));
            profile.addXp(skillId, amount);
            profile.invalidatePageCache();
            int newLevel = def.getLevelForXp(profile.getXp(skillId));
            sender.sendMessage(render("success", Map.of("message",
                    "Added " + amount + " XP to " + playerName + "'s " + skillId + ".")));
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
            sendUsage(sender, USAGE_ADDXP);
            sender.sendMessage(render("error", Map.of("message", "Unknown skill: " + skillId)));
            return;
        }
        var offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sendUsage(sender, USAGE_ADDXP);
            sender.sendMessage(render("error", Map.of("message", "Player not found: " + playerName)));
            return;
        }
        UUID uuid = offlinePlayer.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Coordinate with the write-behind cache (see handleOfflineSetLevel):
            // apply to a live cached profile when the target joined mid-command.
            PlayerProfile live = profileManager.getProfile(uuid);
            if (live != null) {
                live.addXp(skillId, amount);
                live.invalidatePageCache();
                live.addPendingFanfare(skillId);
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(render("success", Map.of("message",
                            "Added " + amount + " XP to " + playerName + "'s " + skillId + " (live profile, fanfare pending)."))));
                return;
            }
            String uuidStr = uuid.toString();
            String sql = "INSERT INTO player_skills (player_uuid, skill_id, xp, fanfare_pending) VALUES (?, ?, ?, 1) ON CONFLICT(player_uuid, skill_id) DO UPDATE SET xp = xp + ?, fanfare_pending = 1";
            try (var conn = plugin.getDatabaseManager().getConnection();
                 var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuidStr);
                stmt.setString(2, skillId);
                stmt.setLong(3, amount);
                stmt.setLong(4, amount);
                stmt.executeUpdate();
                // Fold the grant into a profile that joined during the write.
                // The profile may have hydrated from a snapshot taken after the
                // write committed (already containing the grant) or before it
                // (missing it), so a relative addXp could double the amount. Set
                // the absolute post-write value read back from the DB instead,
                // which is idempotent under either timing.
                PlayerProfile joined = profileManager.getProfile(uuid);
                if (joined != null) {
                    long postWriteXp = readXp(conn, uuidStr, skillId);
                    if (postWriteXp >= 0) {
                        joined.setXp(skillId, postWriteXp);
                        joined.invalidatePageCache();
                        joined.addPendingFanfare(skillId);
                    }
                }
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(render("success", Map.of("message",
                            "Added " + amount + " XP to " + playerName + "'s " + skillId + " (offline, fanfare pending)."))));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(render("error", Map.of("message", "Database error: " + e.getMessage()))));
            }
        });
    }

    /**
     * Reads a player's current XP for a skill from the database.
     *
     * @param conn     an open connection (autocommit, so prior writes are visible)
     * @param uuidStr  the player's UUID string
     * @param skillId  the skill id
     * @return the persisted XP, or -1 when the read fails (caller must not fold)
     */
    private static long readXp(java.sql.Connection conn, String uuidStr, String skillId) {
        String sql = "SELECT xp FROM player_skills WHERE player_uuid = ? AND skill_id = ?";
        try (var sel = conn.prepareStatement(sql)) {
            sel.setString(1, uuidStr);
            sel.setString(2, skillId);
            try (var rs = sel.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (Exception e) {
            // The grant is already committed. A failed read-back must not set the
            // live profile to a wrong value; skip the fold and let the write-behind
            // flush from the (possibly stale) profile reconcile on the next cycle.
            java.util.logging.Logger.getLogger("skilling.command").log(java.util.logging.Level.WARNING,
                    "Failed to read back XP for " + uuidStr + "/" + skillId
                            + " after an offline grant; skipping the live-profile fold", e);
            return -1L;
        }
    }

    private void reset(CommandSender sender, String playerName, String skillId) {
        Player target = Bukkit.getPlayer(playerName);
        if (target != null) {
            PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
            if (profile == null) {
                sendUsage(sender, USAGE_RESET);
                sender.sendMessage(render("error", Map.of("message", "Profile not loaded for " + playerName + ".")));
                return;
            }
            if (skillId != null) {
                profile.setXp(skillId, 0);
                profile.invalidatePageCache();
                bossBarPool.remove(target, skillId);
                sender.sendMessage(render("success", Map.of("message", "Reset " + playerName + "'s " + skillId + ".")));
            } else {
                for (String id : new HashSet<>(profile.getXpMap().keySet())) {
                    profile.setXp(id, 0);
                }
                profile.invalidatePageCache();
                bossBarPool.removeAll(target);
                sender.sendMessage(render("success", Map.of("message", "Reset all skills for " + playerName + ".")));
            }
        } else {
            handleOfflineReset(sender, playerName, skillId);
        }
    }

    private void handleOfflineReset(CommandSender sender, String playerName, String skillId) {
        var offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore()) {
            sendUsage(sender, USAGE_RESET);
            sender.sendMessage(render("error", Map.of("message", "Player not found: " + playerName)));
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
                        sender.sendMessage(render("success", Map.of("message",
                                "Reset " + playerName + "'s " + skillId + " (offline)."))));
                } else {
                    String sql = "DELETE FROM player_skills WHERE player_uuid = ?";
                    try (var stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, uuid);
                        stmt.executeUpdate();
                    }
                    Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(render("success", Map.of("message",
                                "Reset all skills for " + playerName + " (offline)."))));
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                    sender.sendMessage(render("error", Map.of("message", "Database error: " + e.getMessage()))));
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

    /**
     * Saves a config key and reports whether it took effect live or needs a
     * restart, so {@code /skills set} never claims success for a key the running
     * server cannot apply.
     *
     * @param sender the command sender to report to
     * @param key    the config key
     * @param value  the raw value
     */
    void handleSetConfig(CommandSender sender, String key, String value) {
        var config = plugin.getConfig();
        config.set(key, parseConfigValue(value));
        plugin.saveConfig();
        if (requiresRestart(key)) {
            sender.sendMessage(render("info", Map.of("message",
                    "Set " + key + " to " + value + " requires a server restart to take effect.")));
            return;
        }
        plugin.reloadConfigSettings();
        sender.sendMessage(render("success", Map.of("message", "Set " + key + " to " + value)));
    }

    /**
     * Whether a config key is read only at construction (e.g. the Hikari pool
     * size) and therefore cannot be applied to the running server.
     *
     * @param key the config key
     * @return true when a restart is required for the change to take effect
     */
    static boolean requiresRestart(String key) {
        return "database.pool_size".equals(key);
    }

    private void sendUsage(CommandSender sender, String usage) {
        sender.sendMessage(render("usage", Map.of("usage", usage)));
    }

    /**
     * Renders a command feedback template from the active branding config.
     *
     * @param role    the template role (header, command, description, usage,
     *                success, error, info)
     * @param scalars placeholder values for the template
     * @return the rendered component
     */
    private Component render(String role, Map<String, String> scalars) {
        BrandingConfig.Command cmd = currentCommand();
        String template = switch (role) {
            case "header" -> cmd.header();
            case "command" -> cmd.command();
            case "description" -> cmd.description();
            case "usage" -> cmd.usage();
            case "success" -> cmd.success();
            case "error" -> cmd.error();
            default -> cmd.info();
        };
        return TemplateRenderer.toComponent(TemplateRenderer.renderLine(template, scalars));
    }

    private BrandingConfig.Command currentCommand() {
        BrandingConfig branding = plugin.getBranding();
        return (branding != null ? branding : BrandingConfig.DEFAULT).command();
    }

    private void showXpBossBar(Player player, SkillDefinition skill, PlayerProfile profile) {
        LevelUpDispatcher.showXpBossBar(player, skill, profile, bossBarPool, plugin);
    }

    private void broadcastLevelUp(Player player, SkillDefinition skill, int newLevel) {
        LevelUpDispatcher.broadcastLevelUp(player, skill, newLevel, plugin, bossBarPool);
    }
}
