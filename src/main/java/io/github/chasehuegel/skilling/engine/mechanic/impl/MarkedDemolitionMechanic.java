package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Marks a sneak-placed TNT block for controlled demolition, then prunes that
 * TNT's explosion to only break blocks the marking player placed and configured.
 *
 * <p><b>Marking (the {@code block_place} trigger):</b> when a player sneak-places
 * {@code minecraft:tnt}, its block location is recorded with the owner UUID and
 * the resolved {@code target} material set. All other TNT is untouched, so normal
 * mining/clearing TNT and any other player's TNT detonate 100% vanilla — this is
 * the opt-in: only TNT you sneak-place becomes a demolition charge.
 *
 * <p><b>Detonation (the engine {@code EntityExplodeEvent} handler):</b> when a
 * marked TNT explodes, every block in the explosion's {@code blockList} that is
 * not a player-placed, owner-matched, {@code target}-set construction block is
 * removed, so natural terrain and other players' builds survive intact while the
 * owner's flagged blocks break and drop normally for recovery. The mark is
 * consumed on detonation and on any later break of the TNT block, so entries
 * cannot leak.
 *
 * <p><b>YAML key:</b> {@code core:marked_demolition}
 * <br>Params: {@code target} (material or tag reference listing which placed
 * blocks the demolition breaks and recovers)
 */
public final class MarkedDemolitionMechanic implements SkillMechanic {

    /** Block-metadata key recording the UUID of the player who placed a block. */
    public static final String OWNER_META_KEY = "player_placed_owner";

    /** Marked demolition charges, keyed by the block location of the sneak-placed TNT. */
    private static final Map<Location, DemolitionMark> MARKS = new ConcurrentHashMap<>();

    private record DemolitionMark(UUID owner, Set<Material> targets) {}

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockPlaceEvent placeEvent)) return false;
        if (placeEvent.getBlockPlaced().getType() != Material.TNT) return false;
        if (!player.isSneaking()) return false;
        Set<Material> targets = resolveTarget(params);
        MARKS.put(placeEvent.getBlockPlaced().getLocation().toBlockLocation(),
                new DemolitionMark(player.getUniqueId(), targets));
        return true;
    }

    /**
     * Resolves the {@code target} reference (a material or tag) into a material
     * set through the live plugin {@link TagResolver}, or an empty set when the
     * parameter is absent (safe: nothing counts as refundable, so the explosion
     * is fully pruned).
     *
     * @param params the evaluated mechanic parameters
     * @return the flattened demolition target material set
     */
    private static Set<Material> resolveTarget(Map<String, Object> params) {
        Object raw = params.get("target");
        if (raw == null) return Set.of();
        String reference = String.valueOf(raw);
        if (reference.isBlank()) return Set.of();
        TagResolver resolver = Skilling.getInstance().getTagResolver();
        if (resolver == null) {
            throw new IllegalStateException("Cannot resolve marked_demolition 'target' without a live TagResolver");
        }
        return resolver.resolve(reference);
    }

    /**
     * Applies the demolition pruning to a marked TNT explosion. Called by the
     * engine's {@code EntityExplodeEvent} handler; harmless for any other entity.
     *
     * @param event the entity explosion event
     */
    public static void handleExplosion(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.TNTPrimed)) return;
        DemolitionMark mark = MARKS.remove(event.getLocation().toBlockLocation());
        if (mark == null) return;
        event.blockList().removeIf(b -> !isOwnedConstruction(b, mark));
    }

    /**
     * Whether a block should be destroyed by a demolition charge: it must be
     * player-placed, owned by the charge's owner, and in the charge's target set.
     *
     * @param block the candidate block
     * @param mark  the demolition charge owning the target set
     * @return true if the block is the owner's refundable construction block
     */
    private static boolean isOwnedConstruction(Block block, DemolitionMark mark) {
        if (!block.hasMetadata("player_placed")) return false;
        if (!block.hasMetadata(OWNER_META_KEY)) return false;
        Object ownerValue = block.getMetadata(OWNER_META_KEY).get(0).value();
        if (!(ownerValue instanceof UUID owner)) return false;
        if (!mark.owner().equals(owner)) return false;
        return mark.targets().contains(block.getType());
    }

    /**
     * Clears a demolition mark at a block location, called when a marked TNT
     * block is broken without exploding so a defused charge cannot leak.
     *
     * @param block the block whose mark to clear
     */
    public static void clearMark(Block block) {
        Location loc = block.getLocation();
        if (loc != null) {
            MARKS.remove(loc.toBlockLocation());
        }
    }

    /** Clears every demolition mark (reload cleanup). */
    public static void clearAll() {
        MARKS.clear();
    }
}
