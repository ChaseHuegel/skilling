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
        sender.sendMessage(MINI_MESSAGE.deserialize("<green>Set " + target.getName() + "'s " + skillId + " to level " + level + "."));
        showXpBossBar(target, def, profile);
        if (level > oldLevel) {
            broadcastLevelUp(target, def, level);
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
            if (xp < required) return level - 1;
        }
        return skill.maxLevel();
    }

    private void showXpBossBar(Player player, SkillDefinition skill, PlayerProfile profile) {
        String skillId = skill.id();
        long totalXp = profile.getXp(skillId);
        int level = getLevelForXp(skill, totalXp);
        int maxLevel = skill.maxLevel();
        String displayName = skill.display() != null && skill.display().name() != null
                ? skill.display().name() : skillId;

        BossBar bar = bossBarPool.getOrCreate(player, skillId);

        TextColor textColor = resolveBarColor(skill.display() != null ? skill.display().color() : null);
        Component title;

        if (level >= maxLevel) {
            title = Component.text(displayName + " - Maxed!", NamedTextColor.GOLD);
        } else {
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

    private void broadcastLevelUp(Player player, SkillDefinition skill, int newLevel) {
        String displayName = skill.display() != null && skill.display().name() != null
                ? skill.display().name() : skill.id();
        boolean major = isMajorLevelUp(skill, newLevel);
        player.showTitle(Title.title(
                MiniMessage.miniMessage().deserialize("<gold><bold>Level up!</bold></gold>"),
                MiniMessage.miniMessage().deserialize("<yellow>" + displayName + " increased to " + newLevel + "</yellow>"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000))
        ));
        if (major) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE,
                    org.bukkit.SoundCategory.PLAYERS, 1.0f, 1.2f);
            spawnFirework(player.getLocation(), org.bukkit.Color.ORANGE,
                    org.bukkit.FireworkEffect.Type.BURST, 3);
            spawnFirework(player.getLocation(), org.bukkit.Color.AQUA,
                    org.bukkit.FireworkEffect.Type.STAR, 2);
        } else {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,
                    org.bukkit.SoundCategory.PLAYERS, 1.0f, 1.0f);
            spawnFirework(player.getLocation(), org.bukkit.Color.WHITE,
                    org.bukkit.FireworkEffect.Type.BURST, 1);
        }
        plugin.getLogger().info("Level up! " + player.getName() + "'s " + skill.id() + " increased to " + newLevel + "!");
    }

    private static boolean isMajorLevelUp(SkillDefinition skill, int newLevel) {
        return skill.abilities().stream().anyMatch(a -> a.unlockLevel() == newLevel);
    }

    private static void spawnFirework(org.bukkit.Location location, org.bukkit.Color color,
                                       org.bukkit.FireworkEffect.Type type, int count) {
        var fwLoc = location.clone().add(
                (Math.random() - 0.5) * 2, 3, (Math.random() - 0.5) * 2);
        for (int i = 0; i < count; i++) {
            org.bukkit.entity.Firework fw = fwLoc.getWorld().spawn(fwLoc,
                    org.bukkit.entity.Firework.class);
            org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                    .withColor(color)
                    .with(type)
                    .build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        }
    }
}
