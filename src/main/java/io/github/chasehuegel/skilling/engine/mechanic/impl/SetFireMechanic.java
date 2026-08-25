package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Sets the aimed entity on fire for a configured number of ticks on a right-click
 * cast.
 *
 * <p>A mage's searing hex: the aimed creature is ignited for {@code ticks} of
 * native burn (20 ticks = 1 second), exhausting the target's own fire-facing
 * mechanics rather than injecting a custom damage source. PvP protection is
 * inherited from the shared {@link DamageTargetResolver}, so players and the
 * caster are never ignitable targets. Aiming at no acceptable target is a no-op
 * that spends nothing.
 *
 * <p><b>YAML key:</b> {@code core:set_fire}
 * <br>Params: {@code ticks} (default 100, native fire ticks the target burns for)
 */
public final class SetFireMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interactEvent)) return false;
        Action action = interactEvent.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return false;

        LivingEntity target = DamageTargetResolver.resolveTarget(player, event);
        if (target == null || target.isDead()) return false;

        int ticks = ((Number) params.getOrDefault("ticks", 100)).intValue();
        if (ticks <= 0) return false;

        target.setFireTicks(ticks);
        return true;
    }
}