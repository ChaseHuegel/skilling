package io.github.chasehuegel.skilling.engine.feedback;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.profile.PlayerPreferences;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import java.time.Duration;
import java.util.List;

public final class LevelUpDispatcher {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final org.bukkit.Color[] BRIGHT_COLORS = {
            org.bukkit.Color.RED, org.bukkit.Color.ORANGE, org.bukkit.Color.YELLOW,
            org.bukkit.Color.LIME, org.bukkit.Color.GREEN, org.bukkit.Color.AQUA,
            org.bukkit.Color.BLUE, org.bukkit.Color.PURPLE, org.bukkit.Color.FUCHSIA
    };

    private LevelUpDispatcher() {}

    public static void showXpBossBar(Player player, SkillDefinition skill, PlayerProfile profile, BossBarPool bossBarPool, Skilling plugin) {
        String skillId = skill.id();
        long totalXp = profile.getXp(skillId);
        int level = skill.getLevelForXp(totalXp);
        int maxLevel = skill.maxLevel();

        if (level >= maxLevel) {
            bossBarPool.remove(player, skillId);
            return;
        }

        String displayName = skill.display() != null && skill.display().name() != null
                ? skill.display().name() : skillId;

        BossBar bar = bossBarPool.getOrCreate(player, skillId);
        // bossbar.max_active <= 0 disables the XP boss bar entirely.
        if (bar == null) return;

        TextColor textColor = resolveBarColor(skill.display() != null ? skill.display().color() : null);
        Component title;

        long xpForCurrent = (long) skill.progression().evaluator().evaluate(level, 0);
        long xpForNext = (long) skill.progression().evaluator().evaluate(level + 1, 0);
        long intoLevel = totalXp - xpForCurrent;
        long needed = xpForNext - xpForCurrent;
        // Clamp to [0,1]: negative total XP (from a stale/corrupt row) or a total
        // below the level threshold would otherwise compute a negative progress,
        // which Bukkit rejects with IllegalArgumentException.
        double progress = needed > 0 ? Math.min(Math.max((double) intoLevel / needed, 0.0), 1.0) : 0;
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

    public static void broadcastLevelUp(Player player, SkillDefinition skill, int newLevel, Skilling plugin, BossBarPool bossBarPool) {
        plugin.debug("Level-up for " + player.getName() + " in " + skill.id() + " -> level " + newLevel);
        String displayName = skill.display() != null && skill.display().name() != null
                ? skill.display().name() : skill.id();
        boolean major = isMajorLevelUp(skill, newLevel);
        var unlockedAbilities = skill.abilities().stream()
                .filter(a -> a.unlockLevel() == newLevel)
                .toList();

        String levelUpMsg = "<yellow>" + displayName + " increased to " + newLevel + "</yellow>";
        PlayerPreferences prefs = getPreferences(player, plugin);
        if (prefs.logLevels()) {
            player.sendMessage(MINI_MESSAGE.deserialize(levelUpMsg));
        }
        int stayMs = plugin.getTitleStayDuration();
        player.showTitle(Title.title(
                MINI_MESSAGE.deserialize("<gold><bold>Level up!</bold></gold>"),
                MINI_MESSAGE.deserialize("<yellow>" + displayName + " increased to " + newLevel + "</yellow>"),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(stayMs),
                        Duration.ofMillis(500)
                )
        ));

        long firstDelay = Math.min(stayMs + 500L, 3000L) / 50L;
        for (int i = 0; i < unlockedAbilities.size(); i++) {
            int idx = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // The player may log out before the delayed announcement fires;
                // touching a disconnected reference throws on the main thread.
                if (!player.isOnline()) return;
                Component line = SkillMenuBuilder.formatAbilityLine(unlockedAbilities.get(idx), newLevel);
                    String unlockMsg = "<gray>[</gray><aqua>Ability Unlocked!</aqua><gray>]</gray> ";
                if (prefs.logUnlocks()) {
                    player.sendMessage(MINI_MESSAGE.deserialize(unlockMsg)
                            .append(withAbilityLoreHover(line, unlockedAbilities.get(idx), newLevel)));
                }
                player.showTitle(Title.title(
                        MINI_MESSAGE.deserialize("<gold><bold>New unlock!</bold></gold>"),
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
            String maxSubMsg = player.getName() + " reached "
                    + "<light green>" + newLevel + " </light green>"
                    + "<" + skillColorName + ">"
                    + displayName + "</" + skillColorName + ">";
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player) || plugin.isDebugLogging()) {
                    online.showTitle(Title.title(
                            Component.empty(),
                            MINI_MESSAGE.deserialize(maxSubMsg),
                            Title.Times.times(
                                    Duration.ofMillis(500),
                                    Duration.ofMillis(3500),
                                    Duration.ofMillis(1000)
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

            Component broadcastComponent = MINI_MESSAGE.deserialize(
                    player.getName() + " has reached max level "
                    + "<" + skillColorName + ">[" + displayName + "]</" + skillColorName + ">");
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(broadcastComponent);
            }
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

        // Execute level-up commands
        io.github.chasehuegel.skilling.engine.command.LevelUpCommandExecutor.execute(skill, player, newLevel);

        StringBuilder logMsg = new StringBuilder("Level up! " + player.getName() + "'s " + skill.id() + " increased to " + newLevel);
        for (SkillDefinition.Ability a : unlockedAbilities) {
            logMsg.append("\n  ").append(
                    LegacyComponentSerializer.legacySection().serialize(
                            SkillMenuBuilder.formatAbilityLine(a, newLevel)));
        }
        plugin.getLogger().info(logMsg.toString());
    }

    /**
     * Attaches a lore hover tooltip to an ability line for the unlock chat
     * message, so hovering the ability name shows what it does. Abilities with
     * no lore render as a plain name without a hover.
     *
     * @param line        the formatted ability line component
     * @param ability     the ability whose lore to show
     * @param playerLevel the player's current skill level
     * @return the line with a {@code show_text} hover event, or the line unchanged
     */
    public static Component withAbilityLoreHover(Component line, SkillDefinition.Ability ability, int playerLevel) {
        List<String> lore = SkillMenuBuilder.resolveAbilityLore(ability, playerLevel);
        if (lore.isEmpty()) return line;
        Component tooltip = Component.empty();
        for (int i = 0; i < lore.size(); i++) {
            if (i > 0) tooltip = tooltip.append(Component.newline());
            tooltip = tooltip.append(LegacyComponentSerializer.legacyAmpersand().deserialize(lore.get(i)));
        }
        return line.hoverEvent(HoverEvent.showText(tooltip));
    }

    private static PlayerPreferences getPreferences(Player player, Skilling plugin) {
        PlayerProfile profile = plugin.getProfileManager().getProfile(player.getUniqueId());
        return profile != null ? profile.getPreferences() : PlayerPreferences.DEFAULTS;
    }

    public static boolean isMajorLevelUp(SkillDefinition skill, int newLevel) {
        return skill.abilities().stream().anyMatch(a -> a.unlockLevel() == newLevel);
    }

    public static org.bukkit.Color randomBrightColor() {
        return BRIGHT_COLORS[(int) (Math.random() * BRIGHT_COLORS.length)];
    }

    public static void spawnFirework(Location location, org.bukkit.Color color,
                                      org.bukkit.FireworkEffect.Type type, int count) {
        var fwLoc = location.clone().add(
                (Math.random() - 0.5) * 2, 3, (Math.random() - 0.5) * 2);
        for (int i = 0; i < count; i++) {
            org.bukkit.entity.Firework fw = fwLoc.getWorld().spawn(fwLoc,
                    org.bukkit.entity.Firework.class);
            fw.getPersistentDataContainer().set(Skilling.FIREWORK_KEY, PersistentDataType.BOOLEAN, true);
            org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                    .withColor(randomBrightColor())
                    .with(type)
                    .build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        }
    }

    public static TextColor resolveBarColor(String colorName) {
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

    public static String mmColorName(String barColorName) {
        return switch (barColorName.toUpperCase()) {
            case "PINK" -> "light_purple";
            case "PURPLE" -> "dark_purple";
            default -> barColorName.toLowerCase();
        };
    }

    public static int getLevelForXp(SkillDefinition skill, long xp) {
        return skill.getLevelForXp(xp);
    }
}
