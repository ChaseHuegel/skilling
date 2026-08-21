package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.ProcAwareMechanic;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

/**
 * On a trial-vault state change (a key unlocking it and dispensing a reward), a
 * chance to grant the player a bonus item from a configured vault reward loot
 * table. The raider's-pack counterpart to {@code core:loot_bonus}, scoped to the
 * {@code vault_change} trigger where no generated-loot list is available.
 *
 * <p>The bonus is a single natural drop from the referenced table, handed to the
 * player's inventory (dropped at the player if full). Reaching the chance roll
 * counts as an activation attempt whether or not it lands.
 *
 * <p><b>YAML key:</b> {@code core:vault_bonus}
 * <br>Params: {@code chance} (0-100, percentage chance of the bonus), {@code table}
 * (namespaced loot table key, e.g. {@code minecraft:chests/trial_chambers_reward})
 */
public final class VaultBonusMechanic implements ProcAwareMechanic {

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
        Object tableRaw = params.get("table");
        if (tableRaw == null) return false;
        NamespacedKey key = parseKey(String.valueOf(tableRaw));
        if (key == null) return false;

        double chance = ((Number) params.getOrDefault("chance", 0)).doubleValue();
        if (chance <= 0) return false;
        procced = randomSource.getAsDouble() <= chance;
        if (!procced) return true;

        LootTable loot = Bukkit.getLootTable(key);
        if (loot == null) return true;
        LootContext context = new LootContext.Builder(player.getLocation()).killer(player).build();
        Collection<ItemStack> drops = loot.populateLoot(ThreadLocalRandom.current(), context);
        ItemStack bonus = drops.stream()
                .filter(item -> item != null && !item.getType().isAir())
                .findFirst()
                .map(ItemStack::clone)
                .orElse(null);
        if (bonus != null) {
            bonus.setAmount(1);
            player.getInventory().addItem(bonus).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
        return true;
    }

    private static NamespacedKey parseKey(String value) {
        try {
            return NamespacedKey.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public boolean didProc() {
        return procced;
    }
}