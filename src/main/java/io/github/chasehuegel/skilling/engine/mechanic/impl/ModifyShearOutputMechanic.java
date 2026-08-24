package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Multiplies the fleece (and other) drops a shearing yields on
 * {@link PlayerShearEntityEvent}. Powers the Tailoring skill's "shear more
 * fleece" economy spike, the analogue of {@code core:modify_furnace_output} on
 * the craft loop.
 *
 * <p>The event's drop list is rebuilt with extra copies of each drop, each
 * copy respecting the item's stack-size cap, and the result is written back via
 * {@code setDrops}. Sheep drop wool by color as separate entries, so each entry
 * is scaled independently and the colored wool stays intact.
 *
 * <p><b>YAML key:</b> {@code core:modify_shear_output}
 * <br>Params: {@code multiplier} (default 1.0; extra drops = original &times; (multiplier - 1))
 */
public final class ModifyShearOutputMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerShearEntityEvent shearEvent)) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 1.0) return false;

        List<ItemStack> drops = shearEvent.getDrops();
        if (drops == null || drops.isEmpty()) return false;

        List<ItemStack> amplified = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (drop == null || drop.isEmpty()) continue;
            amplified.add(drop);
            int extras = (int) Math.round(drop.getAmount() * (multiplier - 1));
            int maxStack = drop.getMaxStackSize();
            while (extras > 0) {
                ItemStack copy = drop.clone();
                copy.setAmount(Math.min(extras, maxStack));
                amplified.add(copy);
                extras -= Math.min(extras, maxStack);
            }
        }
        shearEvent.setDrops(amplified);
        return true;
    }
}