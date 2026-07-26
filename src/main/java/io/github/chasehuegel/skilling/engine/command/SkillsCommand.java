package io.github.chasehuegel.skilling.engine.command;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.lockdown.LockdownManager;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import java.util.HashSet;

public final class SkillsCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Skilling plugin;
    private final SkillManager skillManager;
    private final ProfileManager profileManager;
    private final SkillMenuBuilder skillMenuBuilder;
    private final LockdownManager lockdownManager;

    public SkillsCommand(Skilling plugin, SkillManager skillManager, ProfileManager profileManager,
                         SkillMenuBuilder skillMenuBuilder, LockdownManager lockdownManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.profileManager = profileManager;
        this.skillMenuBuilder = skillMenuBuilder;
        this.lockdownManager = lockdownManager;
    }

    public void register() {
        var commandManager = PaperCommandManager.<Source>builder(
                PaperSimpleSenderMapper.simpleSenderMapper()
        ).executionCoordinator(ExecutionCoordinator.<Source>simpleCoordinator())
         .buildOnEnable(plugin);

        var skills = commandManager.commandBuilder("skills");

        commandManager.command(skills
                .permission("skilling.use")
                .optional("skill", StringParser.stringParser())
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
                .required("player", StringParser.stringParser())
                .required("skill", StringParser.stringParser())
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
                .required("player", StringParser.stringParser())
                .required("skill", StringParser.stringParser())
                .required("amount", IntegerParser.integerParser())
                .handler(ctx -> {
                    String playerName = ctx.get("player");
                    String skillId = ctx.get("skill");
                    int amount = ctx.get("amount");
                    addXp(ctx.sender().source(), playerName, skillId, amount);
                }));

        commandManager.command(commandManager.commandBuilder("skills")
                .literal("reset")
                .permission("skilling.admin")
                .required("player", StringParser.stringParser())
                .optional("skill", StringParser.stringParser())
                .handler(ctx -> {
                    String playerName = ctx.get("player");
                    String skillId = ctx.getOrDefault("skill", null);
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
        long xp = profile.getXpMap().getOrDefault(def.id(), 0L);
        int level = getLevelForXp(def, xp);
        int maxLevel = def.maxLevel();
        long nextLevelXp = level < maxLevel
                ? (long) def.progression().evaluator().evaluate(level + 1, 0) : 0;
        String name = def.display() != null && def.display().name() != null
                ? def.display().name() : def.id();
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== " + name + " ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Level: " + level + " / " + maxLevel, NamedTextColor.GREEN));
        player.sendMessage(Component.text("Total XP: " + xp, NamedTextColor.AQUA));
        if (level < maxLevel) {
            player.sendMessage(Component.text("XP to next level: " + nextLevelXp, NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Mastered!", NamedTextColor.YELLOW));
        }
    }

    private void setLevel(CommandSender sender, String playerName, String skillId, int level) {
        Player target = Bukkit.getPlayerExact(playerName);
        if (target != null && target.isOnline()) {
            PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
            if (profile == null) {
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>Profile not loaded for " + playerName + "."));
                return;
            }
            SkillDefinition def = skillManager.getSkill(skillId);
            if (def == null) {
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>Unknown skill: " + skillId));
                return;
            }
            long xp = 0;
            for (int i = 1; i <= level; i++) {
                xp += (long) def.progression().evaluator().evaluate(i, 0);
            }
            profile.setXp(skillId, xp);
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>Set " + playerName + "'s " + skillId + " to level " + level + "."));
        } else {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Player not found: " + playerName));
        }
    }

    private void addXp(CommandSender sender, String playerName, String skillId, int amount) {
        Player target = Bukkit.getPlayerExact(playerName);
        if (target != null && target.isOnline()) {
            PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
            if (profile == null) {
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>Profile not loaded for " + playerName + "."));
                return;
            }
            profile.addXp(skillId, amount);
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>Added " + amount + " XP to " + playerName + "'s " + skillId + "."));
        } else {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Player not found: " + playerName));
        }
    }

    private void reset(CommandSender sender, String playerName, String skillId) {
        Player target = Bukkit.getPlayerExact(playerName);
        if (target != null && target.isOnline()) {
            PlayerProfile profile = profileManager.getProfile(target.getUniqueId());
            if (profile == null) {
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
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Player not found: " + playerName));
        }
    }

    private int getLevelForXp(SkillDefinition skill, long xp) {
        for (int level = 1; level <= skill.maxLevel(); level++) {
            double required = skill.progression().evaluator().evaluate(level, 0);
            if (xp < required) return level - 1;
        }
        return skill.maxLevel();
    }
}
