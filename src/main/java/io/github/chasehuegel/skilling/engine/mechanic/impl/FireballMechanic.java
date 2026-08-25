package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import java.util.Map;

/**
 * Launches a non-griefing fireball from the player on a right-click cast.
 *
 * <p>The launched {@link SmallFireball} is deliberately read as a spell, not a
 * ghast blast: {@code setYield(0)} removes its block-destruction radius and
 * {@code setIsIncendiary(false)} stops it from setting fires, so it never
 * corrupts terrain (Pillar I safe unplug). Its configured {@code damage} is
 * carried in the shared {@code skilling:projectile_damage} persistent key, so
 * the engine's normal {@code ProjectileHitEvent} handler applies flat damage to
 * the struck entity while the tiny non-griefing explosion supplies a mild
 * knockback splash.
 *
 * <p>Only a right-click (air or block) launches; a left-click is a no-op so it
 * never consumes the ability cost.
 *
 * <p><b>YAML key:</b> {@code core:fireball}
 * <br>Params: {@code speed} (default 2.0), {@code damage} (default 6.0)
 */
public final class FireballMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interactEvent)) return false;
        Action action = interactEvent.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return false;

        double speed = ((Number) params.getOrDefault("speed", 2.0)).doubleValue();
        double damage = ((Number) params.getOrDefault("damage", 6.0)).doubleValue();

        SmallFireball fireball = player.launchProjectile(SmallFireball.class);
        fireball.setVelocity(player.getLocation().getDirection().multiply(speed));
        fireball.setYield(0.0f);
        fireball.setIsIncendiary(false);
        fireball.getPersistentDataContainer().set(
                ProjectileMechanic.DAMAGE_KEY, PersistentDataType.DOUBLE, damage);
        return true;
    }
}