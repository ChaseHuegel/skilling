package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Marks any TNT a player places as a harvesting charge, then converts that
 * charge's explosion from a pure space-clearing blast into a mining tool: the
 * destroyed natural target blocks keep their raw drops and item entities in the
 * blast survive, so blast mining yields ore instead of vaporizing it.
 *
 * <p><b>Marking (the {@code block_place} trigger):</b> when the player places
 * {@code minecraft:tnt} the block location is recorded with the resolved
 * {@code target} material set. Any player-placed TNT is charged — no sneaking
 * required — so ordinary TNT from other players or command-spawned blasts are
 * untouched and detonate 100% vanilla.
 *
 * <p><b>Detonation (the engine {@code EntityExplodeEvent} handler):</b> when a
 * charged TNT explodes, every block in the explosion's {@code blockList} that is
 * natural (no {@code player_placed} metadata) and within the {@code target} set
 * has its raw {@code getDrops()} spawned at its location. The blocks stay in the
 * block list, so they still break and clear space; the mechanic rescues the loot
 * vanilla explosions would destroy. Drops are base only (no Fortune or Silk
 * Touch), so the TNT's material cost stays the honest tradeoff of blast mining.
 * Player-placed blocks and other players' builds are never harvested.
 *
 * <p><b>Item shielding:</b> on detonation a short-lived protected region is
 * registered around the blast. The engine's {@code EntityDamageEvent} handler
 * cancels explosion damage to item entities inside it, so both pre-existing
 * drops and the freshly harvested loot survive the blast. Regions expire after a
 * few seconds and are pruned lazily on the next event, so there is no per-tick
 * task and no leaked state.
 *
 * <p><b>YAML key:</b> {@code core:blast_harvest}
 * <br>Params: {@code target} (material or tag reference listing which natural
 * blocks' drops the blast rescues)
 */
public final class BlastHarvestMechanic implements SkillMechanic {

    /** How long an item-shielded blast region stays active, in milliseconds. */
    private static final long PROTECT_TTL_MS = 3000L;

    /** An active harvest charge keyed by the placed TNT's block location. */
    private static final Map<Location, HarvestCharge> CHARGES = new ConcurrentHashMap<>();

    /** Active item-shield regions: blast origin -> [radius, expiry millis]. */
    private static final Map<Location, long[]> PROTECTED = new ConcurrentHashMap<>();

    private record HarvestCharge(UUID owner, Set<Material> targets) {}

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockPlaceEvent place)) return false;
        if (place.getBlockPlaced().getType() != Material.TNT) return false;
        Set<Material> targets = resolveTarget(params);
        CHARGES.put(place.getBlockPlaced().getLocation().toBlockLocation(),
                new HarvestCharge(player.getUniqueId(), targets));
        return true;
    }

    /**
     * Resolves the {@code target} reference (a material or tag) into a flattened
     * material set through the live plugin {@link TagResolver}; an empty set when
     * the parameter is absent (safe: nothing counts as harvestable, so the blast
     * clears space normally).
     *
     * @param params the evaluated mechanic parameters
     * @return the flattened harvest target material set
     */
    private static Set<Material> resolveTarget(Map<String, Object> params) {
        Object raw = params.get("target");
        if (raw == null) return Set.of();
        String reference = String.valueOf(raw);
        if (reference.isBlank()) return Set.of();
        TagResolver resolver = Skilling.getInstance().getTagResolver();
        if (resolver == null) {
            throw new IllegalStateException("Cannot resolve blast_harvest 'target' without a live TagResolver");
        }
        return resolver.resolve(reference);
    }

    /**
     * Applies the harvest to a charged TNT explosion. Called by the engine's
     * {@code EntityExplodeEvent} handler; harmless for any other entity.
     *
     * @param event the entity explosion event
     */
    public static void handleExplosion(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed)) return;
        HarvestCharge charge = CHARGES.remove(event.getLocation().toBlockLocation());
        if (charge == null) return;
        pruneExpired();
        Location origin = event.getLocation().toBlockLocation();
        // The event exposes no radius; derive the blast coverage from the
        // farthest destroyed block so item shielding matches where the blast hit.
        double radius = 0.0;
        for (Block destroyed : event.blockList()) {
            radius = Math.max(radius, destroyed.getLocation().distance(origin));
        }
        PROTECTED.put(origin, new long[]{ (long) Math.ceil(radius),
                System.currentTimeMillis() + PROTECT_TTL_MS });
        for (Block block : event.blockList()) {
            if (block.hasMetadata("player_placed")) continue;
            if (!charge.targets().contains(block.getType())) continue;
            // Base drops (blast has no tool, so no Fortune or Silk Touch).
            Collection<ItemStack> drops = block.getDrops();
            if (drops.isEmpty()) continue;
            Location dropAt = block.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack drop : drops) {
                if (drop != null && !drop.isEmpty()) {
                    event.getLocation().getWorld().dropItemNaturally(dropAt, drop);
                }
            }
        }
    }

    /**
     * Cancels explosion damage to item entities inside a protected harvest blast,
     * so existing drops and the freshly harvested loot survive. Called by the
     * engine's {@code EntityDamageEvent} handler.
     *
     * @param event the entity damage event
     */
    public static void handleItemDamage(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        org.bukkit.entity.Entity entity = event.getEntity();
        if (!(entity instanceof org.bukkit.entity.Item)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }
        pruneExpired();
        Location itemLoc = entity.getLocation();
        for (var region : PROTECTED.entrySet()) {
            long radius = region.getValue()[0];
            if (itemLoc.distanceSquared(region.getKey()) <= (double) radius * radius) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Removes expired item-shield regions lazily on each event so the map never
     * grows without bound and no per-tick cleanup is required.
     */
    private static void pruneExpired() {
        long now = System.currentTimeMillis();
        PROTECTED.entrySet().removeIf(e -> e.getValue()[1] < now);
    }

    /**
     * Clears a harvest charge at a block location, called when a charged TNT
     * block is broken without exploding so a defused charge cannot leak.
     *
     * @param block the block whose charge to clear
     */
    public static void clearMark(Block block) {
        Location loc = block.getLocation();
        if (loc != null) {
            CHARGES.remove(loc.toBlockLocation());
        }
    }

    /** Clears every harvest charge and item-shield region (reload cleanup). */
    public static void clearAll() {
        CHARGES.clear();
        PROTECTED.clear();
    }
}