package io.github.chasehuegel.skilling.engine.command;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.parser.standard.IntegerParser;
import java.time.Duration;
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
                    if (sender.hasPermission("skilling.admin")) {
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
                .required("player", PlayerParser.playerParser())
                .required("skill", SkillParser.skillParser(skillManager))
                .required("level", IntegerParser.integerParser())
                .handler(ctx -> {
                    Player target = ctx.get("player");
                    String skillId = ctx.get("skill");
                    int level = ctx.get("level");
                    setLevel(ctx.sender().source(), target, skillId, level);
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("addxp")
                .permission("skilling.admin")
                .required("player", PlayerParser.playerParser())
                .required("skill", SkillParser.skillParser(skillManager))
                .required("amount", IntegerParser.integerParser())
                .handler(ctx -> {
                    Player target = ctx.get("player");
                    String skillId = ctx.get("skill");
                    int amount = ctx.get("amount");
                    addXp(ctx.sender().source(), target, skillId, amount);
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("reset")
                .permission("skilling.admin")
                .required("player", PlayerParser.playerParser())
                .optional("skill", SkillParser.skillParserAllowingAll(skillManager))
                .handler(ctx -> {
                    Player target = ctx.get("player");
                    String skillId = ctx.getOrDefault("skill", null);
                    if ("all".equals(skillId)) skillId = null;
                    reset(ctx.sender().source(), target, skillId);
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

    private void setLevel(CommandSender sender, Player target, String skillId, int level) {
        PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
        if (profile == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_SETLEVEL));
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Profile not loaded for " + target.getName() + "."));
            return;
        }
        SkillDefinition def = skillManager.getSkill(skillId);
        if (def == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_SETLEVEL));
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Unknown skill: " + skillId));
            return;
        }
        int oldLevel = getLevelForXp(def, profile.getXp(skillId));
        long xp = (long) def.progression().evaluator().evaluate(level, 0);
        profile.setXp(skillId, xp);
        int actualLevel = getLevelForXp(def, profile.getXp(skillId));
        sender.sendMessage(MINI_MESSAGE.deserialize("<green>Set " + target.getName() + "'s " + skillId + " to level " + actualLevel + "."));
        showXpBossBar(target, def, profile);
        if (actualLevel > oldLevel) {
            broadcastLevelUp(target, def, actualLevel);
        }
    }

    private void addXp(CommandSender sender, Player target, String skillId, int amount) {
        PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
        if (profile == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_ADDXP));
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Profile not loaded for " + target.getName() + "."));
            return;
        }
        SkillDefinition def = skillManager.getSkill(skillId);
        if (def == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_ADDXP));
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Unknown skill: " + skillId));
            return;
        }
        int oldLevel = getLevelForXp(def, profile.getXp(skillId));
        profile.addXp(skillId, amount);
        int newLevel = getLevelForXp(def, profile.getXp(skillId));
        sender.sendMessage(MINI_MESSAGE.deserialize("<green>Added " + amount + " XP to " + target.getName() + "'s " + skillId + "."));
        showXpBossBar(target, def, profile);
        if (newLevel > oldLevel) {
            broadcastLevelUp(target, def, newLevel);
        }
    }

    private void reset(CommandSender sender, Player target, String skillId) {
        PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
        if (profile == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize(USAGE_RESET));
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Profile not loaded for " + target.getName() + "."));
            return;
        }
        if (skillId != null) {
            profile.setXp(skillId, 0);
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>Reset " + target.getName() + "'s " + skillId + "."));
        } else {
            for (String id : new HashSet<>(profile.getXpMap().keySet())) {
                profile.setXp(id, 0);
            }
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>Reset all skills for " + target.getName() + "."));
        }
    }

    private int getLevelForXp(SkillDefinition skill, long xp) {
        for (int level = 1; level <= skill.maxLevel(); level++) {
            double required = skill.progression().evaluator().evaluate(level, 0);
            if (xp < (long) required) return level - 1;
        }
        return skill.maxLevel();
    }

    private void showXpBossBar(Player player, SkillDefinition skill, PlayerProfile profile) {
        String skillId = skill.id();
        long totalXp = profile.getXp(skillId);
        int level = getLevelForXp(skill, totalXp);
        int maxLevel = skill.maxLevel();

        if (level >= maxLevel) {
            bossBarPool.remove(player, skillId);
            return;
        }

        String displayName = skill.display() != null && skill.display().name() != null
                ? skill.display().name() : skillId;

        BossBar bar = bossBarPool.getOrCreate(player, skillId);

        TextColor textColor = resolveBarColor(skill.display() != null ? skill.display().color() : null);
        Component title;

            long xpForCurrent = (long) skill.progression().evaluator().evaluate(level, 0);
            long xpForNext = (long) skill.progression().evaluator().evaluate(level + 1, 0);
            long intoLevel = totalXp - xpForCurrent;
            long needed = xpForNext - xpForCurrent;
            double progress = needed > 0 ? Math.min((double) intoLevel / needed, 1.0) : 0;
            bar.setProgress(progress);

            Component nameComp = Component.text(displayName,
                    textColor != null ? textColor : NamedTextColor.WHITE);
            title = nameComp
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(level), NamedTextColor.WHITE));
            if (plugin.isDebugLogging()) {
                title = title
                        .append(Component.text(" (", NamedTextColor.GRAY))
                        .append(Component.text(String.valueOf(intoLevel), NamedTextColor.WHITE))
                        .append(Component.text("/", NamedTextColor.GRAY))
                        .append(Component.text(String.valueOf(needed), NamedTextColor.WHITE))
                        .append(Component.text(")", NamedTextColor.GRAY));
            }

        bar.setTitle(LegacyComponentSerializer.legacySection().serialize(title));

        if (skill.display() != null) {
            try {
                bar.setColor(BarColor.valueOf(skill.display().color()));
            } catch (IllegalArgumentException ignored) {}
            try {
                bar.setStyle(BarStyle.valueOf(skill.display().style()));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private static TextColor resolveBarColor(String colorName) {
        if (colorName == null || colorName.isBlank()) return null;
        return switch (colorName.toUpperCase()) {
            case "PINK" -> NamedTextColor.LIGHT_PURPLE;
            case "PURPLE" -> NamedTextColor.DARK_PURPLE;
            case "RED" -> NamedTextColor.RED;
            case "GREEN" -> NamedTextColor.GREEN;
            case "BLUE" -> NamedTextColor.BLUE;
            case "WHITE" -> NamedTextColor.WHITE;
            case "YELLOW" -> NamedTextColor.YELLOW;
            default -> null;
        };
    }

    private static final org.bukkit.Color[] BRIGHT_COLORS = {
            org.bukkit.Color.RED, org.bukkit.Color.ORANGE, org.bukkit.Color.YELLOW,
            org.bukkit.Color.LIME, org.bukkit.Color.GREEN, org.bukkit.Color.AQUA,
            org.bukkit.Color.BLUE, org.bukkit.Color.PURPLE, org.bukkit.Color.FUCHSIA
    };

    private void broadcastLevelUp(Player player, SkillDefinition skill, int newLevel) {
        String displayName = skill.display() != null && skill.display().name() != null
                ? skill.display().name() : skill.id();
        boolean major = isMajorLevelUp(skill, newLevel);
        var unlockedAbilities = skill.abilities().stream()
                .filter(a -> a.unlockLevel() == newLevel)
                .toList();

        String levelUpMsg = "<gray>[</gray><gold>Level Up!</gold><gray>]</gray> <yellow>" + displayName + " increased to " + newLevel + "</yellow>";
        player.sendMessage(MiniMessage.miniMessage().deserialize(levelUpMsg));
        int stayMs = plugin.getTitleStayDuration();
        player.showTitle(Title.title(
                MiniMessage.miniMessage().deserialize("<gold><bold>Level up!</bold></gold>"),
                MiniMessage.miniMessage().deserialize("<yellow>" + displayName + " increased to " + newLevel + "</yellow>"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(stayMs), Duration.ofMillis(500))
        ));

        long firstDelay = Math.min(stayMs + 500L, 3000L) / 50L;
        for (int i = 0; i < unlockedAbilities.size(); i++) {
            int idx = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Component line = SkillMenuBuilder.formatAbilityLine(unlockedAbilities.get(idx), newLevel);
                String unlockMsg = "<gray>[</gray><aqua>Ability Unlocked!</aqua><gray>]</gray> ";
                player.sendMessage(MiniMessage.miniMessage().deserialize(unlockMsg).append(line));
                player.showTitle(Title.title(
                        MiniMessage.miniMessage().deserialize("<gold><bold>New unlock!</bold></gold>"),
                        line.colorIfAbsent(NamedTextColor.WHITE),
                        Title.Times.times(
                                java.time.Duration.ZERO,
                                java.time.Duration.ofMillis(1500),
                                java.time.Duration.ofMillis(500)
                        )
                ));
            }, firstDelay + idx * 40L);
        }

        boolean maxed = newLevel >= skill.maxLevel();
        if (maxed) {
            spawnFirework(player.getLocation(), randomBrightColor(),
                    org.bukkit.FireworkEffect.Type.BURST, 10);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE,
                    org.bukkit.SoundCategory.PLAYERS, 1.0f, 1.2f);

            String skillColorName = skill.display() != null && skill.display().color() != null
                    ? mmColorName(skill.display().color()) : "green";
            String maxSubMsg = "<light green>" + player.getName() + " </light green><yellow>reached </yellow>"
                    + "<light green>" + newLevel + " </light green>"
                    + "<" + skillColorName + ">"
                    + displayName + "</" + skillColorName + ">";
            for (org.bukkit.entity.Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (!online.equals(player) || plugin.isDebugLogging()) {
                    online.showTitle(Title.title(
                            Component.empty(),
                            MiniMessage.miniMessage().deserialize(maxSubMsg),
                            Title.Times.times(
                                    java.time.Duration.ofMillis(500),
                                    java.time.Duration.ofMillis(3500),
                                    java.time.Duration.ofMillis(1000)
                            )
                    ));
                }
                if (!online.equals(player)) {
                    online.playSound(online.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE,
                            org.bukkit.SoundCategory.PLAYERS, 1.0f, 1.2f);
                    spawnFirework(online.getLocation(), randomBrightColor(),
                            org.bukkit.FireworkEffect.Type.BURST, 2);
                }
            }

            String broadcastMsg = "<gray>[</gray><gold>Max Level!</gold><gray>]</gray> "
                    + "<light green>" + player.getName() + " </light green><yellow>reached max "
                    + "<light green>" + displayName + " </light green><yellow>level!</yellow>";
            org.bukkit.Bukkit.broadcast(MiniMessage.miniMessage().deserialize(broadcastMsg));
        } else if (major) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE,
                    org.bukkit.SoundCategory.PLAYERS, 1.0f, 1.2f);
            spawnFirework(player.getLocation(), randomBrightColor(),
                    org.bukkit.FireworkEffect.Type.BURST, 3);
            spawnFirework(player.getLocation(), randomBrightColor(),
                    org.bukkit.FireworkEffect.Type.STAR, 2);
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,
                    org.bukkit.SoundCategory.PLAYERS, 1.0f, 1.0f);
            spawnFirework(player.getLocation(), randomBrightColor(),
                    org.bukkit.FireworkEffect.Type.BURST, 1);
        }

        StringBuilder logMsg = new StringBuilder("Level up! " + player.getName() + "'s " + skill.id() + " increased to " + newLevel);
        for (SkillDefinition.Ability a : unlockedAbilities) {
            logMsg.append("\n  ").append(
                    LegacyComponentSerializer.legacySection().serialize(
                            SkillMenuBuilder.formatAbilityLine(a, newLevel)));
        }
        plugin.getLogger().info(logMsg.toString());
    }

    private static boolean isMajorLevelUp(SkillDefinition skill, int newLevel) {
        return skill.abilities().stream().anyMatch(a -> a.unlockLevel() == newLevel);
    }

    private static org.bukkit.Color randomBrightColor() {
        return BRIGHT_COLORS[(int) (Math.random() * BRIGHT_COLORS.length)];
    }

    private static void spawnFirework(org.bukkit.Location location, org.bukkit.Color color,
                                       org.bukkit.FireworkEffect.Type type, int count) {
        var fwLoc = location.clone().add(
                (Math.random() - 0.5) * 2, 3, (Math.random() - 0.5) * 2);
        for (int i = 0; i < count; i++) {
            org.bukkit.entity.Firework fw = fwLoc.getWorld().spawn(fwLoc,
                    org.bukkit.entity.Firework.class);
            fw.getPersistentDataContainer().set(Skilling.FIREWORK_KEY, PersistentDataType.BOOLEAN, true);
            org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                    .withColor(color)
                    .with(type)
                    .build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        }
    }

    private static String mmColorName(String barColorName) {
        return switch (barColorName.toUpperCase()) {
            case "PINK" -> "light_purple";
            case "PURPLE" -> "dark_purple";
            default -> barColorName.toLowerCase();
        };
    }
}
