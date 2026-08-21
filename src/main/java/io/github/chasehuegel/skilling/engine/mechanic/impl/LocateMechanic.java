package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.LodestoneTracker;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StructureSearchResult;

/**
 * Re-points the held compass to the nearest structure of a configured type,
 * persistently: the structure's position is written into the compass's
 * {@code lodestone_tracker} data component, so the needle stays locked on it
 * (vanilla {@code Cartography} flavor — a "Wayfinder's Compass") until it is
 * re-located or rebound to a real lodestone.
 *
 * <p>Fires only on a main-hand right-click with a {@code minecraft:compass}.
 * A non-compass held item or a cancelled/a blocked interact is a no-op. Locating
 * the structure uses {@link World#locateNearestStructure}; a structure with no
 * nearby instance within the search radius is a no-op (nothing is written).
 *
 * <p><b>YAML key:</b> {@code core:locate}
 * <br>Params: {@code structure} (a built-in structure type key, e.g.
 * {@code monument}, {@code stronghold}, {@code shipwreck})
 */
public record LocateMechanic() implements SkillMechanic {

    /** Built-in StructureType lookup (engine capability table, not skill data). */
    private static final Map<String, StructureType> STRUCTURE_TYPES = Map.ofEntries(
            Map.entry("buried_treasure", StructureType.BURIED_TREASURE),
            Map.entry("desert_pyramid", StructureType.DESERT_PYRAMID),
            Map.entry("end_city", StructureType.END_CITY),
            Map.entry("fortress", StructureType.FORTRESS),
            Map.entry("igloo", StructureType.IGLOO),
            Map.entry("jungle_temple", StructureType.JUNGLE_TEMPLE),
            Map.entry("mineshaft", StructureType.MINESHAFT),
            Map.entry("nether_fossil", StructureType.NETHER_FOSSIL),
            Map.entry("monument", StructureType.OCEAN_MONUMENT),
            Map.entry("ocean_ruin", StructureType.OCEAN_RUIN),
            Map.entry("ruined_portal", StructureType.RUINED_PORTAL),
            Map.entry("shipwreck", StructureType.SHIPWRECK),
            Map.entry("stronghold", StructureType.STRONGHOLD),
            Map.entry("swamp_hut", StructureType.SWAMP_HUT),
            Map.entry("mansion", StructureType.WOODLAND_MANSION));

    private static final int SEARCH_RADIUS = 10000;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent ie)) return false;
        if (ie.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return false;
        Action action = ie.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return false;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != Material.COMPASS) return false;
        StructureType type = STRUCTURE_TYPES.get(String.valueOf(params.get("structure")));
        if (type == null) return false;

        StructureSearchResult result = player.getWorld()
                .locateNearestStructure(player.getLocation(), type, SEARCH_RADIUS, false);
        if (result == null || result.getLocation() == null) return false;

        Location target = result.getLocation().clone();
        target.setY(player.getLocation().getY());
        // tracked=false keeps the pointer even though it references a structure
        // rather than a genuine lodestone block.
        held.setData(DataComponentTypes.LODESTONE_TRACKER,
                LodestoneTracker.lodestoneTracker(target, false));
        return true;
    }
}