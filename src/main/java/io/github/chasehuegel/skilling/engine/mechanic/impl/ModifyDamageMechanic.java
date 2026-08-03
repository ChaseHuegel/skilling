package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.Map;

/**
 * Multiplies the activating player's outgoing damage on
 * {@link EntityDamageByEntityEvent}.
 *
 * <p>The player must be the attacker (resolved through
 * {@link EntityDamageResolver}, so bow/snowball shots count too); otherwise the
 * mechanic is a no-op. This keeps the mechanic's behavior independent of the
 * trigger it is bound to — a misbound {@code entity_damage_taken} ability must
 * never scale the player's incoming damage.
 *
 * <p><b>YAML key:</b> {@code core:modify_damage}
 * <br>Params: {@code multiplier} (multiplicative factor, must be &gt; 0)
 */
public final class ModifyDamageMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent damageEvent)) return false;
        // Only scale the activating player's outgoing damage; a wrong trigger
        // binding must not multiply incoming damage on the player.
        if (!player.equals(EntityDamageResolver.resolveDamagerPlayer(damageEvent))) return false;
        double multiplier = ((Number) params.getOrDefault("multiplier", 1.0)).doubleValue();
        if (multiplier <= 0) return false;
        damageEvent.setDamage(damageEvent.getDamage() * multiplier);
        return true;
    }
}
