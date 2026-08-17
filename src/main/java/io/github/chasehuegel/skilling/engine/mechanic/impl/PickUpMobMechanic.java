package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.util.Map;

/**
 * Carries a mob as a rider of the player on a right-click of the mob.
 *
 * <p>On {@link PlayerInteractEntityEvent}, the right-clicked {@link LivingEntity}
 * is mounted onto the player via {@code addPassenger}, so the mob rides the
 * player like a boat or minecart passenger. Any non-player living mob can be
 * carried: hostile, neutral, passive, or friendly, including villagers. Entity
 * selection is data-driven: bind the ability to the {@code right_click_entity}
 * trigger and gate it with the {@code target_type} state filter and a
 * {@code #...} entity tag, so server owners decide exactly which mobs are
 * portable.
 *
 * <p>The player must have an empty main hand, the empty-hand carry gesture, so
 * the pickup never collides with vanilla feed/breed/shear/tame/trade
 * interactions on the clicked mob. The number of simultaneously carried mobs is
 * capped by {@code max_passengers} (a level-scalable count), and a target that
 * is already a passenger of the player is a no-op.
 *
 * <p>Per the {@link SkillMechanic} return contract, {@code false} is returned
 * when the mechanic could not act (wrong event, a non-living or player target,
 * a non-empty hand, or a full passenger load), so the ability's cost and
 * cooldown are only consumed on a genuine pickup attempt.
 *
 * <p><b>YAML key:</b> {@code core:pick_up_mob}
 * <p><b>Optional parameters:</b> {@code max_passengers} (how many mobs the
 * player can carry, default 1)
 */
public final class PickUpMobMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEntityEvent interact)) return false;
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return false;
        if (!(interact.getRightClicked() instanceof LivingEntity target)) return false;
        if (target instanceof Player) return false;

        int maxPassengers = ((Number) params.getOrDefault("max_passengers", 1.0)).intValue();
        if (maxPassengers <= 0) return false;
        if (player.getPassengers().size() >= maxPassengers) return false;

        return player.addPassenger(target);
    }
}
