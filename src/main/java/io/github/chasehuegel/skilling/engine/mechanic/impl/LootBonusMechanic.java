package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.ProcAwareMechanic;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

/**
 * On generated world loot (a chest, trial-vault container, or other loot
 * source), a chance to pocket a single bonus copy of one of the generated
 * items. The scavenging analogue of {@code core:yield_multiplier}: it amplifies
 * what looting yields rather than replacing the vanilla container.
 *
 * <p>The bonus item is a fresh one-count copy of a random non-air item from the
 * event's generated loot, placed in the player's inventory (dropped at the
 * player if the inventory is full). Reaching the chance roll counts as an
 * activation attempt whether or not it lands, so a failed roll cannot be retried
 * for free.
 *
 * <p>Because the {@code loot} trigger routes to nearby players, this supports
 * group play: every player near the loot location rolls independently.
 *
 * <p><b>YAML key:</b> {@code core:loot_bonus}
 * <br>Params: {@code chance} (0-100, percentage chance to pocket a bonus item)
 */
public final class LootBonusMechanic implements ProcAwareMechanic {

    private static volatile DoubleSupplier randomSource = () -> ThreadLocalRandom.current().nextDouble(100);

    private boolean procced;

    /**
     * Test-only seam to force a deterministic roll; production always uses
     * {@link ThreadLocalRandom}.
     *
     * @param source the roll source returning a percentage in [0, 100)
     */
    static void setRandomSource(DoubleSupplier source) {
        randomSource = source;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof LootGenerateEvent le)) return false;
        double chance = ((Number) params.getOrDefault("chance", 0)).doubleValue();
        if (chance <= 0) return false;
        // Reaching the roll is an activation attempt whether or not it lands.
        procced = randomSource.getAsDouble() <= chance;
        if (!procced) return true;

        List<ItemStack> loot = le.getLoot();
        List<ItemStack> candidates = loot.stream()
                .filter(item -> item != null && !item.getType().isAir())
                .toList();
        if (candidates.isEmpty()) return true;
        ItemStack bonus = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())).clone();
        bonus.setAmount(1);
        player.getInventory().addItem(bonus).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        return true;
    }

    @Override
    public boolean didProc() {
        return procced;
    }
}