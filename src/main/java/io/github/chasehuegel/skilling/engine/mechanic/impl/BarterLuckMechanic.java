package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Improves the outcome of a piglin barter on {@link PiglinBarterEvent} by
 * adding a bonus emerald to the outcome list with a configurable chance.
 *
 * <p>The barter outcome is a mutable list, so the emerald is appended to what
 * the player would already receive rather than replacing it. A failed roll
 * still counts as an activation attempt per the {@link SkillMechanic} contract.
 *
 * <p><b>YAML key:</b> {@code core:barter_luck}
 * <p><b>Required parameters:</b> {@code chance} (percent, 0-100)
 */
public final class BarterLuckMechanic implements SkillMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble();
    private static volatile Supplier<ItemStack> emeraldSource = () -> new ItemStack(Material.EMERALD);

    /**
     * Test-only seam to force a deterministic roll; production always uses
     * {@link ThreadLocalRandom}.
     *
     * @param source the roll source returning a value in [0, 1)
     */
    static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    /**
     * Test-only seam to inject a mock emerald item; production always builds a
     * real emerald stack. Kept because a plain-JUnit JVM cannot construct an
     * {@link ItemStack} from a material (the item registry is absent).
     *
     * @param source the emerald item supplier
     */
    static void setEmeraldSource(Supplier<ItemStack> source) {
        emeraldSource = source;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PiglinBarterEvent barterEvent)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;
        if (randomSource.getAsDouble() * 100 >= chance) return true;
        barterEvent.getOutcome().add(emeraldSource.get());
        return true;
    }
}
