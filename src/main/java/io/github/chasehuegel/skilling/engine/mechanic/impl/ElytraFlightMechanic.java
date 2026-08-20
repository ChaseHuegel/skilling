package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.UnlockMechanic;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * Grants creative-mode flight while the player wears an elytra, as the building
 * skill's mastery capstone.
 *
 * <p>Flight is unlocked on the {@code level_up} trigger and reconciled on join,
 * reload, and {@code /skills setlevel}/{@code reset} (via {@link UnlockMechanic}),
 * which re-runs this mechanic at the player's current level. The fly capability
 * is only granted when the chest slot actually holds an elytra — a persistent,
 * level-scaled synergy with the existing vanilla item rather than free flight —
 * and removed when the elytra comes off or the skill de-levels.
 *
 * <p>Because the ability is persistent, the engine strips all granted flight at
 * the start of each reconcile (re-evaluating from scratch) and re-grants it to
 * players still past the milestone. Mid-session equipment changes are handled by
 * the engine's inventory-change handler calling {@link #reevaluate(Player)}:
 * event-driven, no per-tick task, per the zero-constant-ticking pillar.
 *
 * <p><b>YAML key:</b> {@code core:elytra_flight}
 * <br>Params: none.
 */
public final class ElytraFlightMechanic implements UnlockMechanic {

    /** Players whose building skill has unlocked flight (in-memory; rebuilt on join). */
    private static final Set<UUID> ELIGIBLE = ConcurrentHashMap.newKeySet();

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        ELIGIBLE.add(player.getUniqueId());
        applyFlight(player);
        return true;
    }

    /**
     * Applies the flight capability based on whether the player currently wears
     * an elytra in the chest slot.
     *
     * @param player the player to update
     */
    private static void applyFlight(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        boolean wearElytra = chestplate != null && chestplate.getType() == Material.ELYTRA;
        player.setAllowFlight(wearElytra);
    }

    /**
     * Re-evaluates the flight capability for a player after an inventory change.
     * Does nothing for a player who has not unlocked flight.
     *
     * @param player the player whose equipment may have changed
     */
    public static void reevaluate(Player player) {
        if (ELIGIBLE.contains(player.getUniqueId())) {
            applyFlight(player);
        }
    }

    /**
     * Strips flight from every eligible online player and clears the eligibility
     * set, so a reconcile can re-grant it from scratch. Called by the engine at
     * the start of milestone-unlock reconciliation so a de-level or reset cannot
     * leave stale flight on the player.
     */
    public static void stripAll() {
        for (UUID id : ELIGIBLE) {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                player.setAllowFlight(false);
            }
        }
        ELIGIBLE.clear();
    }

    /**
     * Removes a player from the flight-eligibility set on quit, so their
     * capability is rebuilt fresh from their level on next join.
     *
     * @param playerId the leaving player's UUID
     */
    public static void clear(UUID playerId) {
        ELIGIBLE.remove(playerId);
    }
}
