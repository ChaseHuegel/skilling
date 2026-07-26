package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.HashMap;
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

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final int MENU_SIZE = 54;

    private final SkillManager skillManager;

    /**
     * Constructs a new menu builder.
     *
     * @param skillManager the skill manager providing skill definitions
     */
    public SkillMenuBuilder(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    /**
     * Builds the skill overview inventory for a player.
     *
     * <p>The inventory is lazy-built from all registered skill definitions
     * and cached in the player's profile.
     *
     * @param profile the player's profile
     * @return the built inventory
     */
    public Inventory buildOverview(PlayerProfile profile) {
        var player = Bukkit.getPlayer(profile.getPlayerId());
        Component title = MINI_MESSAGE.deserialize("<bold>Skills</bold>");
        Inventory inventory = Bukkit.createInventory(null, MENU_SIZE, title);

        int slot = 0;
        for (SkillDefinition skill : skillManager.getSkills().values()) {
            if (slot >= MENU_SIZE) break;

            ItemStack icon = buildSkillIcon(skill, profile);
            inventory.setItem(slot, icon);
            slot++;
        }

        return inventory;
    }

    private ItemStack buildSkillIcon(SkillDefinition skill, PlayerProfile profile) {
        Material material = Material.matchMaterial(skill.display().icon());
        if (material == null) {
            material = Material.BARRIER;
        }

        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(MINI_MESSAGE.deserialize("<bold>" + skill.display().name() + "</bold>"));

            long currentXp = profile.getXp(skill.id());
            // Build lore showing current level info

            var lore = new java.util.ArrayList<Component>();
            lore.add(Component.text("Level: " + getLevelForXp(skill, currentXp)));
            lore.add(Component.text("XP: " + currentXp));
            lore.add(Component.text("Max Level: " + skill.maxLevel()));

            meta.lore(lore);

            if (skill.display().customModelData() > 0) {
                meta.setCustomModelData(skill.display().customModelData());
            }

            // Apply poison pill tag
            PoisonPillTag.apply(meta);
        });

        return item;
    }

    private int getLevelForXp(SkillDefinition skill, long xp) {
        // Binary search to find level from XP using the progression evaluator
        for (int level = 1; level <= skill.maxLevel(); level++) {
            double required = skill.progression().evaluator().evaluate(level, 0);
            if (xp < required) {
                return level - 1;
            }
        }
        return skill.maxLevel();
    }
}