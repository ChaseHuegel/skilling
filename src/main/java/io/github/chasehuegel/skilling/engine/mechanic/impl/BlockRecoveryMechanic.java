package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.Skilling;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Reduces the shield's post-block recovery so a blocker re-arms faster.
 *
 * <p>Minecraft applies the shield-hit cooldown (a disabled shield clang) after the
 * damage event fires and applies damage, so the engine cannot shorten it in-place.
 * Instead this mechanic reads the resulting shield cooldown one tick later and
 * subtracts the configured {@code reduction} (in ticks, floored at zero). The
 * one-shot delayed task is not a per-tick loop, so it stays within the engine's
 * "zero constant ticking" pillar.
 *
 * <p>YAML key: {@code core:block_recovery}
 * <br>Params: {@code reduction} (ticks to shorten the shield's recovery cooldown by)
 */
public final class BlockRecoveryMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageEvent de)) return false;
        if (!de.getEntity().equals(player)) return false;
        if (!player.isBlocking()) return false;

        double reduction = ((Number) params.getOrDefault("reduction", 0.0)).doubleValue();
        if (reduction <= 0) return false;
        int reductionTicks = (int) Math.round(reduction);

        Skilling plugin = Skilling.getInstance();
        // No live plugin (e.g. plain-JUnit): nothing to schedule, but the block is
        // still a valid activation attempt.
        if (plugin == null) return true;

        new BukkitRunnable() {
            @Override
            public void run() {
                int current = player.getCooldown(Material.SHIELD);
                if (current > 0) {
                    player.setCooldown(Material.SHIELD, Math.max(0, current - reductionTicks));
                }
            }
        }.runTask(plugin);
        return true;
    }
}