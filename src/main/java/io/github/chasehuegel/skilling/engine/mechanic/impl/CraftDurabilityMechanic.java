package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

/**
 * Increases the max durability of a freshly crafted item by a level-scaled
 * amount. Powers the Smithing skill's "gear lasts longer" a tier ability.
 *
 * <p>On {@link CraftItemEvent} the crafted result's {@code max_damage} data
 * component is raised by {@code amount}. Because a freshly crafted damageable
 * item is undamaged, the extra durability is purely a larger total pool rather
 * than a repair. No persistent marker is written, so the boost lives on the
 * item itself and works in any hands; re-crafting produces a fresh item whose
 * max durability is re-evaluated at the player's current smithing level.
 *
 * <p><b>YAML key:</b> {@code core:craft_durability}
 * <br>Params: {@code amount} (level-scaled, positive; durability points added)
 */
public final class CraftDurabilityMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof CraftItemEvent craftEvent)) return false;

        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        if (amount <= 0) return false;

        ItemStack result = craftEvent.getCurrentItem();
        if (result == null || result.isEmpty()) return false;

        Integer currentMax = result.getData(DataComponentTypes.MAX_DAMAGE);
        int base = currentMax != null ? currentMax : result.getType().getMaxDurability();
        if (base <= 0) return false;

        result.setData(DataComponentTypes.MAX_DAMAGE, base + (int) Math.round(amount));
        craftEvent.setCurrentItem(result);
        return true;
    }
}