package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.ProcAwareMechanic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Rolls a referenced loot table and drops the result naturally at the target.
 *
 * <p>This is a broad, reusable loot mechanic: the target can be a right-clicked
 * entity, a right-clicked/stepped-on block, or a damaged entity, so any entity-
 * or block-targeting event can drive it. It works with any {@link LootTable}
 * key — vanilla tables ({@code minecraft:chests/simple_dungeon}) and datapack
 * or plugin-driven ones ({@code data/&lt;ns&gt;/loot_table/...}) — resolved via
 * {@link Bukkit#getLootTable}. Unlike the damage mechanics, players are valid
 * targets (typical use is pickpocketing / looting, not combat), and nothing is
 * removed from the target.
 *
* <p>The {@code chance} (0-100) is optional: absent means the table always
     * rolls. A rolled attempt counts as an activation, so the ability's cost and
     * cooldown are consumed once per use and a failed roll cannot be retried for
     * free.
     *
     * <p>On a {@link PlayerFishEvent} (typically the {@code fishing} trigger) the
     * drop lands at the fishing player's location, so a skill can roll a custom
     * catch-quality table on a completed catch without a dedicated treasure
     * mechanic.
     *
     * <p><b>YAML key:</b> {@code core:drop_loot}
     * <br>Params: {@code table} (namespaced loot table key), {@code chance}
     * (optional, 0-100, percentage to actually roll the table)
 */
public final class DropLootMechanic implements ProcAwareMechanic {

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

        DropTarget drop = resolveDropTarget(player, event);
        if (drop == null) return false;

        // Absent chance always rolls.
        double chance = ((Number) params.getOrDefault("chance", 101.0)).doubleValue();
        if (chance <= 0) return false;
        procced = randomSource.getAsDouble() <= Math.min(chance, 100.0);
        if (!procced) {
            return true;
        }

        LootTable loot = Bukkit.getLootTable(key);
        if (loot == null) return false;
        LootContext context = new LootContext.Builder(drop.location())
                .killer(player)
                .lootedEntity(drop.entity())
                .build();
        Collection<ItemStack> drops = loot.populateLoot(ThreadLocalRandom.current(), context);
        World world = drop.location().getWorld();
        if (world == null) return true;
        Location dropLoc = drop.location().clone().add(0, 1, 0);
        for (ItemStack item : drops) {
            if (item == null || item.getType().isAir()) continue;
            world.dropItemNaturally(dropLoc, item);
        }
        return true;
    }

    /** The drop location and (optional) looted entity bound to the triggering event. */
    private record DropTarget(Location location, Entity entity) {}

    private static NamespacedKey parseKey(String value) {
        try {
            return NamespacedKey.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private DropTarget resolveDropTarget(Player player, Event event) {
        if (event instanceof PlayerInteractEntityEvent ie
                && ie.getRightClicked() instanceof LivingEntity living) {
            return new DropTarget(living.getLocation(), living);
        }
        if (event instanceof PlayerInteractEvent ie) {
            org.bukkit.event.block.Action action = ie.getAction();
            if (action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                    || action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                Entity hit = player.getTargetEntity((int) OffhandStrikeMechanic.MAX_REACH);
                if (hit instanceof LivingEntity living) {
                    return new DropTarget(living.getLocation(), living);
                }
                if (ie.getClickedBlock() != null) {
                    return new DropTarget(ie.getClickedBlock().getLocation(), null);
                }
            }
            if (action == org.bukkit.event.block.Action.PHYSICAL && ie.getClickedBlock() != null) {
                return new DropTarget(ie.getClickedBlock().getLocation(), null);
            }
        }
        if (event instanceof EntityDamageByEntityEvent de
                && de.getEntity() instanceof LivingEntity victim) {
            return new DropTarget(victim.getLocation(), victim);
        }
        if (event instanceof PlayerFishEvent) {
            return new DropTarget(player.getLocation(), null);
        }
        return null;
    }

    @Override
    public boolean didProc() {
        return procced;
    }
}
