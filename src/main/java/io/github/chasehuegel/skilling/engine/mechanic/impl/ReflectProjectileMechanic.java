package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.ProcAwareMechanic;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Rebounds a blocked enemy projectile back at its shooter.
 *
 * <p>On an {@link EntityDamageByEntityEvent} where the damager is a {@link Projectile}
 * (arrow, trident, fireball) and the victim is the activating player, this reverses the
 * projectile's velocity toward the shooter and deals flat {@code damage} to that
 * shooter. The shield stance is gated by the ability's {@code state: is_blocking}
 * filter in YAML, so this mechanic only concerns itself with projectile sourcing.
 *
 * <p>Reaching the chance roll counts as an activation attempt (the projectile-based
 * damage was recognized and reflected or not), consistent with the {@link SkillMechanic}
 * consumption contract. {@code false} is returned only when the event is not a
 * projectile damage event on the player, the chance is zero, or there is no living
 * shooter to rebound onto.
 *
 * <p>YAML key: {@code core:reflect_projectile}
 * <br>Params: {@code chance} (0-100, percentage to rebound), {@code damage} (flat
 * damage to the shooter)
 */
public final class ReflectProjectileMechanic implements ProcAwareMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);

    private boolean procced;

    /**
     * Test-only seam (marked {@code @VisibleForTesting}) to force a deterministic
     * roll; production always uses {@link ThreadLocalRandom}.
     *
     * @param source the roll source returning a percentage in [0, 100)
     */
    static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent de)) return false;
        if (!de.getEntity().equals(player)) return false;
        if (!(de.getDamager() instanceof Projectile projectile)) return false;

        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;

        double damage = ((Number) params.getOrDefault("damage", 0.0)).doubleValue();
        if (!(projectile.getShooter() instanceof LivingEntity shooter)) return false;
        if (shooter.equals(player)) return false;

        procced = randomSource.getAsDouble() <= chance;
        if (procced) {
            // Reverse the projectile back to its owner, then guarantee the damage so
            // the rebound lands even if the reversed projectile misses on its own.
            projectile.setVelocity(projectile.getVelocity().multiply(-1));
            if (damage > 0) {
                shooter.damage(damage, player);
            }
        }
        return true;
    }

    @Override
    public boolean didProc() {
        return procced;
    }
}