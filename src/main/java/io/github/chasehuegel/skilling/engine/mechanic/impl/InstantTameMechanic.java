package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Tames an untamed tameable mob on right-click.
 *
 * <p>The vanilla {@code EntityTameEvent} fires only after a successful tame
 * roll, so a "tame more easily" bonus cannot be built on it. This mechanic
 * hooks the earlier {@link PlayerInteractEntityEvent}: when the player
 * right-clicks an untamed {@link Tameable}, it assigns ownership with the
 * configured {@code chance}. The natural-food cost is enforced by the ability's
 * {@code requirements.items} with {@code action: cost}, so the vanilla
 * item-economy pillar is preserved.
 *
 * <p>Reaching the chance roll counts as an activation attempt: the mechanic
 * returns {@code true} whether or not the roll succeeds, so the ability's cost
 * and cooldown are consumed exactly once per attempt and a failed roll cannot
 * be retried for free. {@code false} is only returned when the mechanic could
 * not act at all (wrong event type, a non-tameable or already-tamed target).
 *
 * <p>PvP-safe: {@link Tameable} only extends {@code Animals}, so a player is
 * never a target, and another player's pet is already tamed. Region/protection
 * plugins cancel {@link PlayerInteractEntityEvent} before dispatch, so a
 * cancelled interaction never tames.
 *
 * <p>Acting on an untamed mob cancels the interaction: the ability's
 * {@code requirements.items} {@code action: cost} deducts the tame food, and
 * the cancel stops the vanilla tame/feed attempt from consuming a second food
 * item on top of that cost.
 *
 * <p><b>YAML key:</b> {@code core:instant_tame}
 * <br>Params: {@code chance} (0-100, default 100, percentage chance to tame)
 */
public final class InstantTameMechanic implements SkillMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);

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
        if (!(event instanceof PlayerInteractEntityEvent interactEvent)) return false;
        if (!(interactEvent.getRightClicked() instanceof Tameable tameable)) return false;
        if (tameable.isTamed()) return false;

        double chance = ((Number) params.getOrDefault("chance", 100.0)).doubleValue();
        if (chance <= 0) return false;

        // The tame food is consumed by the ability's requirements. Cancel the
        // interaction so the vanilla tame/feed attempt cannot also consume a
        // second food item on top of that cost.
        interactEvent.setCancelled(true);

        // A failed roll is still an activation attempt: the ability's cost and
        // cooldown are consumed once, and the roll cannot be retried for free.
        if (chance < 100.0 && randomSource.getAsDouble() >= chance) return true;

        tameable.setOwner(player);
        tameable.setTamed(true);
        return true;
    }
}
