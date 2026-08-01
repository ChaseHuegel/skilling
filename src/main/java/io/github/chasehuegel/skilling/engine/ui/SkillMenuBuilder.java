package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.ui.GuiLayoutConfig.FillerConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
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
 * <p>If a {@link GuiLayoutConfig} is loaded from {@code gui.yml}, the
 * builder produces a paginated multi-page chest GUI. Otherwise it
 * falls back to the flat linear layout.
 *
 * <p>Lore is dynamically injected via {@link LoreResolver} to display
 * real-time evaluator outputs based on the player's current level.
 */
public final class SkillMenuBuilder {

    private static final int MENU_SIZE = 54;

    private final SkillManager skillManager;
    private volatile GuiLayoutConfig guiLayoutConfig;

    public SkillMenuBuilder(SkillManager skillManager, GuiLayoutConfig guiLayoutConfig) {
        this.skillManager = skillManager;
        this.guiLayoutConfig = guiLayoutConfig;
    }

    public void setGuiLayoutConfig(GuiLayoutConfig guiLayoutConfig) {
        this.guiLayoutConfig = guiLayoutConfig;
    }

    public Inventory buildOverview(PlayerProfile profile) {
        if (guiLayoutConfig.isPresent()) {
            return buildPaginatedOverview(profile);
        }
        return buildFlatOverview(profile);
    }

    private Inventory buildFlatOverview(PlayerProfile profile) {
        var player = Bukkit.getPlayer(profile.getPlayerId());
        Inventory inventory = Bukkit.createInventory(new SkillInventoryHolder(player, -1, null, 0), MENU_SIZE,
                Component.text("Skills", NamedTextColor.GOLD));

        int slot = 0;
        for (SkillDefinition skill : skillManager.getSkills().values()) {
            if (slot >= MENU_SIZE) break;
            inventory.setItem(slot, buildSkillIcon(skill, profile));
            slot++;
        }

        return inventory;
    }

    private Inventory buildPaginatedOverview(PlayerProfile profile) {
        Map<Integer, Inventory> cached = profile.getCachedPageInventories();
        if (cached != null && !cached.isEmpty()) {
            return cached.get(0);
        }

        var player = Bukkit.getPlayer(profile.getPlayerId());
        if (player == null) return buildFlatOverview(profile);

        List<String> pageOrder = guiLayoutConfig.getPageOrder();
        int pageCount = pageOrder.size();
        Map<Integer, Inventory> inventories = new HashMap<>();

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            GuiPage page = guiLayoutConfig.getPage(pageOrder.get(pageIndex));
            if (page == null) continue;

            int size = page.inventorySize();
            Inventory inventory = Bukkit.createInventory(
                    new SkillInventoryHolder(player, pageIndex, pageOrder, pageCount),
                    size,
                    LegacyComponentSerializer.legacyAmpersand().deserialize(page.displayTitle()));

            // Fill all slots with filler
            ItemStack filler = createFillerPane();
            for (int slot = 0; slot < size; slot++) {
                inventory.setItem(slot, filler);
            }

            // Place navigation arrows on the last row
            if (pageIndex > 0) {
                inventory.setItem(page.prevSlot(), createNavItem("◀ Prev Page"));
            }
            if (pageIndex < pageCount - 1) {
                inventory.setItem(page.nextSlot(), createNavItem("Next Page ▶"));
            }

            // Place page indicator at center of last row
            inventory.setItem(page.indicatorSlot(), buildPageIcon(page));

            // Place skill icons
            for (var entry : page.skillSlots().entrySet()) {
                String skillId = entry.getKey();
                int slot = entry.getValue();
                SkillDefinition skill = skillManager.getSkill(skillId);
                if (skill == null) {
                    Bukkit.getLogger().warning("Skill '" + skillId + "' from gui.yml not found in registry, skipping slot " + slot);
                    continue;
                }
                inventory.setItem(slot, buildSkillIcon(skill, profile));
            }

            // Tag everything with poison pill
            tagAllItems(inventory);

            inventories.put(pageIndex, inventory);
        }

        profile.setCachedPageInventories(inventories);
        return inventories.get(0);
    }

    private void tagAllItems(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null) {
                item.editMeta(meta -> PoisonPillTag.apply(meta));
            }
        }
    }

    private ItemStack createFillerPane() {
        FillerConfig filler = guiLayoutConfig.getFillerConfig();
        Material material = Material.matchMaterial(filler.material());
        if (material == null) {
            material = Material.BLACK_STAINED_GLASS_PANE;
        }
        ItemStack pane = new ItemStack(material);
        pane.editMeta(meta -> {
            meta.displayName(Component.empty());
            meta.setHideTooltip(true);
            if (filler.customModelData() > 0) {
                meta.setCustomModelData(filler.customModelData());
            }
            PoisonPillTag.apply(meta);
        });
        return pane;
    }

    private ItemStack createNavItem(String name) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        arrow.editMeta(meta -> {
            meta.displayName(Component.text(name, NamedTextColor.GOLD));
            PoisonPillTag.apply(meta);
        });
        return arrow;
    }

    private ItemStack buildPageIcon(GuiPage page) {
        Material material = Material.matchMaterial(page.icon());
        if (material == null) material = Material.BOOK;
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(page.displayTitle()));
            int skillCount = page.skillSlots().size();
            meta.lore(List.of(
                    Component.text(skillCount + " skill(s)", NamedTextColor.GRAY)
            ));
            if (page.customModelData() > 0) {
                meta.setCustomModelData(page.customModelData());
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            PoisonPillTag.apply(meta);
        });
        return item;
    }

    public List<Component> buildSkillLore(SkillDefinition skill, PlayerProfile profile) {
        long currentXp = profile.getXp(skill.id());
        int level = skill.getLevelForXp(currentXp);
        var lore = new ArrayList<Component>();
        TextColor skillColor = resolveColor(skill.display().color());

        lore.add(Component.text("Level " + level + " / " + skill.maxLevel(),
                skillColor != null ? skillColor : NamedTextColor.GREEN));

        long xpForCurrent = level > 0
                ? (long) skill.progression().evaluator().evaluate(level, 0) : 0;
        long xpForNext = level < skill.maxLevel()
                ? (long) skill.progression().evaluator().evaluate(level + 1, 0) : 0;
        int barWidth = 100;
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

        // Skill-level lore lines
        var skillLore = skill.display().lore();
        if (skillLore != null && !skillLore.isEmpty()) {
            lore.add(Component.empty());
            Map<String, ParameterEvaluator> skillParams = Map.of(
                "level", new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator(level),
                "max_level", new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator(skill.maxLevel()),
                "skill_name", new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator(0),
                "xp", new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator(currentXp)
            );
            List<String> resolved = LoreResolver.resolveAll(
                skillLore, skillParams, level, 0);
            for (String line : resolved) {
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
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
        int level = skill.getLevelForXp(currentXp);
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

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
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

    public static Component formatAbilityLine(SkillDefinition.Ability ability, int playerLevel) {
        boolean unlocked = playerLevel >= ability.unlockLevel();
        boolean isActive = ability.requirements().cooldown().evaluate(playerLevel, ability.unlockLevel()) > 0
                || !ability.requirements().state().isEmpty()
                || !ability.requirements().items().isEmpty();

        if (unlocked) {
            Component namePart = Component.text("✔ " + ability.displayName(), NamedTextColor.GREEN);
            Component typePart = Component.text(
                    isActive ? " · Active" : " · Passive",
                    NamedTextColor.DARK_GRAY);
            return namePart.append(typePart);
        }

        Component lockPart = Component.text("❌ " + ability.unlockLevel(), NamedTextColor.RED);
        Component namePart = Component.text(" · " + ability.displayName(), NamedTextColor.DARK_GRAY);
        Component typePart = Component.text(
                isActive ? " · Active" : " · Passive",
                NamedTextColor.DARK_GRAY);
        return lockPart.append(namePart).append(typePart);
    }
}
