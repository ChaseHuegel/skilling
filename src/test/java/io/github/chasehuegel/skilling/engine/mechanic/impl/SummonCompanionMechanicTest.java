package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:summon_companion} recalls a previously marked companion
 * from its snapshot on a right-click (air or block) holding the matching treat,
 * and that each prerequisite miss is a silent no-op.
 */
class SummonCompanionMechanicTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();

    private final PlayerProfile profile = new PlayerProfile(PLAYER_ID);
    private final ProfileManager manager = mock(ProfileManager.class);
    private final Skilling plugin = plugin(manager);

    private Player setupPlayer(Material held) {
        when(manager.getProfile(PLAYER_ID)).thenReturn(profile);
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(PLAYER_ID);
        ItemStack heldStack = mock(ItemStack.class);
        when(heldStack.getType()).thenReturn(held);
        PlayerInventory inv = mock(PlayerInventory.class);
        when(inv.getItemInMainHand()).thenReturn(heldStack);
        when(p.getInventory()).thenReturn(inv);
        return p;
    }

    private static PlayerInteractEvent airClick() {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        return event;
    }

    private static PlayerInteractEvent blockClick() {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        return event;
    }

    private static Skilling plugin(ProfileManager manager) {
        Skilling plugin = mock(Skilling.class);
        when(plugin.getProfileManager()).thenReturn(manager);
        return plugin;
    }

    private void seedBoundWolf() {
        PetCompanionStore.markTamed(PLAYER_ID, PetCompanionStore.Species.WOLF);
        PetCompanionStore.CompanionSnapshot snapshot = new PetCompanionStore.CompanionSnapshot(
                "WOLF", "Rex", 1.0, 0.3, null, null, 0.0, null, null, null);
        PetCompanionStore.setSnapshot(PLAYER_ID, PetCompanionStore.Species.WOLF, snapshot);
    }

    private World stubWorld() {
        World world = mock(World.class);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(mock(Material.class)); // non-collidable by default
        when(world.getBlockAt(any(Location.class))).thenReturn(block);
        return world;
    }

    @Test
    void summonsBoundWolfFromSnapshot() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            Player p = setupPlayer(Material.BONE);
            World world = stubWorld();
            when(p.getLocation()).thenReturn(new Location(world, 0, 64, 0));
            when(p.getWorld()).thenReturn(world);
            Wolf wolf = mock(Wolf.class);
            when(wolf.getUniqueId()).thenReturn(UUID.randomUUID());
            when(world.spawn(any(Location.class), eq(Wolf.class),
                    eq(CreatureSpawnEvent.SpawnReason.CUSTOM), eq(false), any()))
                    .thenReturn(wolf);
            seedBoundWolf();

            PlayerInteractEvent event = airClick();
            assertTrue(new SummonCompanionMechanic().execute(
                    p, Map.of("mob_type", "wolf"), event));
            // A confirmed summon must cancel the interact so a food treat (apple)
            // does not start a vanilla eat on top of the ability's own cost.
            org.mockito.Mockito.verify(event).setCancelled(true);
        }
    }

    @Test
    void summonsBoundWolfOnBlockClick() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            // The recall must also fire when the crosshair grazes a block while
            // the treat is held: Minecraft classifies that as RIGHT_CLICK_BLOCK.
            Player p = setupPlayer(Material.BONE);
            World world = stubWorld();
            when(p.getLocation()).thenReturn(new Location(world, 0, 64, 0));
            when(p.getWorld()).thenReturn(world);
            Wolf wolf = mock(Wolf.class);
            when(wolf.getUniqueId()).thenReturn(UUID.randomUUID());
            when(world.spawn(any(Location.class), eq(Wolf.class),
                    eq(CreatureSpawnEvent.SpawnReason.CUSTOM), eq(false), any()))
                    .thenReturn(wolf);
            seedBoundWolf();

            assertTrue(new SummonCompanionMechanic().execute(
                    p, Map.of("mob_type", "wolf"), blockClick()));
        }
    }

    @Test
    void leftClickIsNoOp() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            Player p = setupPlayer(Material.BONE);
            var event = mock(PlayerInteractEvent.class);
            when(event.getAction()).thenReturn(Action.LEFT_CLICK_AIR);

            assertFalse(new SummonCompanionMechanic().execute(
                    p, Map.of("mob_type", "wolf"), event));
        }
    }

    @Test
    void wrongTreatIsNoOp() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            // Bound a wolf but the player right-clicks air holding an apple.
            Player p = setupPlayer(Material.APPLE);
            seedBoundWolf();

            assertFalse(new SummonCompanionMechanic().execute(
                    p, Map.of("mob_type", "wolf"), airClick()));
        }
    }

    @Test
    void unmountedSpeciesIsNoOp() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            // No wolf flag or snapshot yet.
            Player p = setupPlayer(Material.BONE);

            assertFalse(new SummonCompanionMechanic().execute(
                    p, Map.of("mob_type", "wolf"), airClick()));
        }
    }

    @Test
    void nullOrWrongEventIsNoOp() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            Player p = setupPlayer(Material.BONE);
            assertFalse(new SummonCompanionMechanic().execute(
                    p, Map.of("mob_type", "wolf"), null));
        }
    }

    @Test
    void debugMessageGoesThroughPluginOnNoOp() {
        try (MockedStatic<Skilling> sk = mockStatic(Skilling.class)) {
            sk.when(Skilling::getInstance).thenReturn(plugin);

            // No wolf bound; the summoner must report the no-op with a reason.
            Player p = setupPlayer(Material.BONE);
            new SummonCompanionMechanic().execute(p, Map.of("mob_type", "wolf"), airClick());
            org.mockito.Mockito.verify(plugin).debug(matches(".*\\[summon_companion\\].*"));
        }
    }
}