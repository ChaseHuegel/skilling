package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds Bukkit {@link Inventory} objects from {@link SkillDefinition}
 * records with lazy instantiation and cache invalidation.
 *
 * <p>Menus are generated on demand and cached inside the
 * {@link PlayerProfile}. The cache is invalidated when the player's
 * level changes, forcing a rebuild on the next menu open.
 *
 * <p>Lore is dynamically injected via {@link LoreResolver} to display
 * real-time evaluator outputs based on the player's current level.
 */
public final class SkillMenuBuilder {

    private static final int MENU_SIZE = 54;

    private final SkillManager skillManager;

    public SkillMenuBuilder(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public Inventory buildOverview(PlayerProfile profile) {
        var player = Bukkit.getPlayer(profile.getPlayerId());
        Inventory inventory = Bukkit.createInventory(null, MENU_SIZE,
                Component.text("Skills", NamedTextColor.GOLD));

        int slot = 0;
        for (SkillDefinition skill : skillManager.getSkills().values()) {
            if (slot >= MENU_SIZE) break;
            inventory.setItem(slot, buildSkillIcon(skill, profile));
            slot++;
        }

        return inventory;
    }

    public List<Component> buildSkillLore(SkillDefinition skill, PlayerProfile profile) {
        long currentXp = profile.getXp(skill.id());
        int level = getLevelForXp(skill, currentXp);
        var lore = new ArrayList<Component>();
        TextColor skillColor = resolveColor(skill.display().color());

        lore.add(Component.text("Level " + level + " / " + skill.maxLevel(),
                skillColor != null ? skillColor : NamedTextColor.GREEN));

        long xpForCurrent = level > 0
                ? (long) skill.progression().evaluator().evaluate(level, 0) : 0;
        long xpForNext = level < skill.maxLevel()
                ? (long) skill.progression().evaluator().evaluate(level + 1, 0) : 0;
        int barWidth = 20;
        double progress = xpForNext > xpForCurrent
                ? (double) (currentXp - xpForCurrent) / (xpForNext - xpForCurrent) : 0;
        progress = Math.min(Math.max(progress, 0), 1);
        int filled = (int) Math.round(progress * barWidth);
        StringBuilder barStr = new StringBuilder().append('[');
        for (int i = 0; i < barWidth; i++) {
            barStr.append(i < filled ? '|' : '.');
        }
        barStr.append(']');
        Component barFull;
        if (filled > 0) {
            Component filledPart = Component.text(barStr.substring(1, 1 + filled), NamedTextColor.GREEN);
            barFull = Component.text("[").color(NamedTextColor.GRAY)
                    .append(filledPart)
                    .append(Component.text(barStr.substring(1 + filled), NamedTextColor.GRAY));
        } else {
            barFull = Component.text(barStr.toString(), NamedTextColor.GRAY);
        }
        lore.add(barFull);

        if (level >= skill.maxLevel()) {
            lore.add(Component.text("Total XP: " + currentXp + " (Maxed)", NamedTextColor.AQUA));
        } else {
            long xpInto = currentXp - xpForCurrent;
            long xpNeeded = xpForNext - xpForCurrent;
            lore.add(Component.text("XP: " + xpInto + " / " + xpNeeded, NamedTextColor.AQUA));
        }

        for (SkillDefinition.Ability ability : skill.abilities()) {
            lore.add(Component.empty());
            lore.add(formatAbilityLine(ability, level));

            Map<String, ParameterEvaluator> allParams = new HashMap<>();
            for (SkillDefinition.MechanicEntry me : ability.mechanics()) {
                allParams.putAll(me.parameters());
            }
            List<String> resolved = LoreResolver.resolveAll(
                    ability.display().lore(), allParams, level, ability.unlockLevel());
            for (String line : resolved) {
                Component deserialized = LegacyComponentSerializer.legacyAmpersand().deserialize(line);
                lore.add(level >= ability.unlockLevel() ? deserialized : deserialized.colorIfAbsent(NamedTextColor.DARK_GRAY));
            }
        }

        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, lore.get(i).decoration(TextDecoration.ITALIC, false));
        }
        return lore;
    }

    private ItemStack buildSkillIcon(SkillDefinition skill, PlayerProfile profile) {
        long currentXp = profile.getXp(skill.id());
        int level = getLevelForXp(skill, currentXp);
        boolean unlocked = level > 0;

        Material material = Material.matchMaterial(skill.display().icon());
        if (material == null || level == 0) {
            material = Material.BARRIER;
        }

        ItemStack item = new ItemStack(material);
        item.setAmount(Math.max(1, Math.min(level, 99)));

        item.editMeta(meta -> {
            TextColor skillColor = resolveColor(skill.display().color());
            String skillName = skill.display().name() != null ? skill.display().name() : skill.id();
            if (unlocked) {
                meta.displayName(Component.text(skillName, NamedTextColor.GREEN));
            } else {
                meta.displayName(Component.text(skillName, NamedTextColor.GRAY)
                        .append(Component.text(" · Locked", NamedTextColor.DARK_GRAY)));
            }

            meta.lore(buildSkillLore(skill, profile));

            if (skill.display().customModelData() > 0) {
                meta.setCustomModelData(skill.display().customModelData());
            }

            PoisonPillTag.apply(meta);
        });

        return item;
    }

    private static TextColor resolveColor(String colorName) {
        if (colorName == null || colorName.isBlank()) return null;
        try {
            return NamedTextColor.NAMES.value(colorName.toLowerCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int getLevelForXp(SkillDefinition skill, long xp) {
        for (int level = 1; level <= skill.maxLevel(); level++) {
            double required = skill.progression().evaluator().evaluate(level, 0);
            if (xp < (long) required) return level - 1;
        }
        return skill.maxLevel();
    }

    public static Component formatAbilityLine(SkillDefinition.Ability ability, int playerLevel) {
        boolean unlocked = playerLevel >= ability.unlockLevel();
        boolean isActive = ability.requirements().cooldown() > 0
                || !ability.requirements().state().isEmpty()
                || !ability.requirements().items().isEmpty();
        Component abilityPart = Component.text(
                (unlocked ? "✔ " : "✗ ") + ability.displayName(),
                unlocked ? NamedTextColor.GREEN : NamedTextColor.GRAY);
        Component typePart = Component.text(
                isActive ? " · Active" : " · Passive",
                NamedTextColor.DARK_GRAY);
        return abilityPart.append(typePart);
    }
}