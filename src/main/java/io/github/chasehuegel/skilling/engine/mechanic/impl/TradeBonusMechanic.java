package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Gives the player a bonus emerald on {@link PlayerTradeEvent} with a
 * configurable chance.
 *
 * <p>The emerald is added directly to the player's inventory rather than
 * mutating the merchant recipe result, so the merchant's stock and the
 * trade's economics stay vanilla. The roll runs once per trade; a failed roll
 * still counts as an activation attempt (cost/cooldown consumed per the
 * {@link SkillMechanic} contract).
 *
 * <p><b>YAML key:</b> {@code core:trade_bonus}
 * <p><b>Required parameters:</b> {@code chance} (percent, 0-100)
 */
public final class TradeBonusMechanic implements SkillMechanic {

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
        if (!(event instanceof PlayerTradeEvent)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0.0)).doubleValue();
        if (chance <= 0) return false;
        if (randomSource.getAsDouble() * 100 >= chance) return true;
        player.getInventory().addItem(emeraldSource.get());
        return true;
    }
}
