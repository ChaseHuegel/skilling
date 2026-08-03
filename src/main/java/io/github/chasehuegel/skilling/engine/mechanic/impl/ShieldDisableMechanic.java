package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Triggers the vanilla shield raise-lockout cooldown on a target player.
 *
 * <p>The target is explicit via the {@code target} parameter, independent of the
 * trigger the ability is bound to:
 * <ul>
 *   <li>{@code victim} (default) — the damaged player on
 *       {@link EntityDamageByEntityEvent}; otherwise the activating player</li>
 *   <li>{@code attacker} — the player attacker on {@link EntityDamageByEntityEvent}
 *       (projectile shooters count); otherwise the activating player</li>
 *   <li>{@code self} — always the activating player</li>
 * </ul>
 *
 * <p>A target that is not a player (e.g. a hostile mob victim or attacker) is a
 * safe no-op, so a wrong binding never throws.
 *
 * <p>YAML key: {@code core:shield_disable}
 * <br>Params: {@code ticks} (double, must be &gt; 0 to act), {@code target}
 * (default {@code victim})
 */
public final class ShieldDisableMechanic implements SkillMechanic {
    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        double ticks = ((Number) params.getOrDefault("ticks", 0.0)).doubleValue();
        if (ticks <= 0) return false;

        String target = String.valueOf(params.getOrDefault("target", "victim"));
        Player recipient = resolveRecipient(player, target, event);
        if (recipient == null) return false;
        recipient.setCooldown(Material.SHIELD, (int) ticks);
        return true;
    }

    private static Player resolveRecipient(Player player, String target, Event event) {
        if (event instanceof EntityDamageByEntityEvent de) {
            return switch (target) {
                case "attacker" -> EntityDamageResolver.resolveDamagerPlayer(de);
                case "self" -> player;
                default -> de.getEntity() instanceof Player victim ? victim : null;
            };
        }
        // No victim or attacker off the damage path: the activating player is the
        // only well-defined target.
        return player;
    }
}
