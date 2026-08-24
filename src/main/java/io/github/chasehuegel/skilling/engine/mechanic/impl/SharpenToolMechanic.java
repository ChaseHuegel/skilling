package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Map;
import java.util.UUID;

/**
 * Applies a permanent mining-efficiency attribute modifier to the tool the
 * player holds, modeled as a "sharpen" that hones the edge at the grindstone.
 *
 * <p>On a right-click of a grindstone the held tool's {@code Attribute
 * .MINING_EFFICIENCY} modifier is set (added) with a stable {@code uuid}. The
 * ability rewrites any existing modifier carrying the same UUID first, so
 * re-sharpening replaces rather than stacks, and the bonus stacks with a Haste
 * potion because it is a separate attribute source. The amount is evaluated at
 * the player's current level, so a higher-level smith hones a stronger edge.
 *
 * <p><b>YAML key:</b> {@code core:sharpen_tool}
 * <br>Params: {@code amount} (level-scaled), {@code uuid} (stable modifier UUID)
 */
public final class SharpenToolMechanic implements SkillMechanic {

    private static final String MODIFIER_NAME = "skilling_sharpen";

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interact)) return false;
        if (interact.getAction() != Action.RIGHT_CLICK_BLOCK) return false;

        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        if (amount <= 0) return false;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.isEmpty()) return false;
        ItemMeta meta = held.getItemMeta();
        if (meta == null) return false;

        UUID uuid = AttributeModifierHelper.resolveUuid(params.get("uuid"));
        removeExisting(meta, uuid);

        meta.addAttributeModifier(Attribute.MINING_EFFICIENCY,
                new AttributeModifier(uuid, MODIFIER_NAME, amount,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        held.setItemMeta(meta);
        player.getInventory().setItemInMainHand(held);
        return true;
    }

    private static void removeExisting(ItemMeta meta, UUID uuid) {
        var existing = meta.getAttributeModifiers(Attribute.MINING_EFFICIENCY);
        if (existing == null) return;
        for (AttributeModifier modifier : existing) {
            if (modifier.getUniqueId().equals(uuid)) {
                meta.removeAttributeModifier(Attribute.MINING_EFFICIENCY, modifier);
            }
        }
    }
}