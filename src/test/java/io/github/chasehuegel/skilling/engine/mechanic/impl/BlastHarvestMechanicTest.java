package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlastHarvestMechanicTest {

    @AfterEach
    void tearDown() {
        BlastHarvestMechanic.clearAll();
    }

    private World world() {
        return mock(World.class);
    }

    private Player player(World world) {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }

    /** A real ItemStack cannot be built in a plain-JUnit JVM, so drops are mocked. */
    private ItemStack drop() {
        return mock(ItemStack.class);
    }

    /** A TNT placement event whose block sits at the given coordinates. */
    private BlockPlaceEvent tntPlacement(World world, int x, int z, Player player) {
        var block = mock(Block.class);
        when(block.getType()).thenReturn(Material.TNT);
        when(block.getLocation()).thenReturn(new Location(world, x, 64, z));
        var event = mock(BlockPlaceEvent.class);
        when(event.getBlockPlaced()).thenReturn(block);
        return event;
    }

    private boolean mark(World world, int x, int z, Player player, String targetRef, EnumSet<Material> targets) {
        var plugin = mock(Skilling.class);
        var resolver = mock(TagResolver.class);
        when(resolver.resolve(anyString())).thenReturn(targets);
        when(plugin.getTagResolver()).thenReturn(resolver);
        try (MockedStatic<Skilling> skilling = mockStatic(Skilling.class)) {
            when(Skilling.getInstance()).thenReturn(plugin);
            return new BlastHarvestMechanic().execute(player,
                    Map.of("target", targetRef), tntPlacement(world, x, z, player));
        }
    }

    /** A natural (never-placed) block at the given coordinates with a drop set. */
    private Block naturalBlock(World world, int x, int z, Material material, List<ItemStack> drops) {
        var block = mock(Block.class);
        when(block.getType()).thenReturn(material);
        when(block.hasMetadata("player_placed")).thenReturn(false);
        when(block.getLocation()).thenReturn(new Location(world, x, 64, z));
        when(block.getDrops()).thenReturn(drops);
        return block;
    }

    /** A player-placed block; these must never be harvested. */
    private Block placedBlock(World world, int x, int z, Material material, List<ItemStack> drops) {
        var block = mock(Block.class);
        when(block.getType()).thenReturn(material);
        when(block.hasMetadata("player_placed")).thenReturn(true);
        when(block.getLocation()).thenReturn(new Location(world, x, 64, z));
        when(block.getDrops()).thenReturn(drops);
        return block;
    }

    private EntityExplodeEvent explosion(World world, int x, int z, boolean tnt, Block... blocks) {
        var event = mock(EntityExplodeEvent.class);
        when(event.getLocation()).thenReturn(new Location(world, x, 64, z));
        if (tnt) {
            when(event.getEntity()).thenReturn(mock(TNTPrimed.class));
        } else {
            when(event.getEntity()).thenReturn(mock(org.bukkit.entity.EnderCrystal.class));
        }
        when(event.blockList()).thenReturn(new ArrayList<>(List.of(blocks)));
        return event;
    }

    @Test
    void returnsFalseForNonBlockPlaceEvent() {
        var player = mock(Player.class);
        assertFalse(new BlastHarvestMechanic().execute(player, Map.of("target", "#c:mineables"),
                mock(EntityExplodeEvent.class)));
    }

    @Test
    void returnsFalseWhenPlacingNonTnt() {
        var world = world();
        var player = player(world);
        var nonTnt = mock(BlockPlaceEvent.class);
        var block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);
        when(nonTnt.getBlockPlaced()).thenReturn(block);
        var plugin = mock(Skilling.class);
        when(plugin.getTagResolver()).thenReturn(mock(TagResolver.class));
        try (MockedStatic<Skilling> skilling = mockStatic(Skilling.class)) {
            when(Skilling.getInstance()).thenReturn(plugin);
            assertFalse(new BlastHarvestMechanic().execute(player, Map.of("target", "#c:mineables"), nonTnt));
        }
    }

    @Test
    void marksAnyPlacedTntAndHarvestsNaturalTargetDropsOnly() {
        var world = world();
        var player = player(world);
        // A player-placed TNT charge, no sneaking required.
        assertTrue(mark(world, 1, 1, player, "#c:mineables", EnumSet.of(Material.STONE)));

        ItemStack stoneDrop = drop();
        ItemStack dirtDrop = drop();
        ItemStack placedDrop = drop();
        var naturalStone = naturalBlock(world, 5, 5, Material.STONE, List.of(stoneDrop));
        var naturalDirt = naturalBlock(world, 6, 6, Material.DIRT, List.of(dirtDrop));
        var placedStone = placedBlock(world, 7, 7, Material.STONE, List.of(placedDrop));

        var event = explosion(world, 1, 1, true, naturalStone, naturalDirt, placedStone);
        BlastHarvestMechanic.handleExplosion(event);

        // Precisely one block is harvested: natural stone that is in the target set.
        verify(world).dropItemNaturally(new Location(world, 5.5, 64.5, 5.5), stoneDrop);
        // The natural dirt (not in the target set) and player-placed stone (never
        // harvested) drops are not spawned.
        verify(world, times(1)).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }

    @Test
    void unmarkedExplosionHarvestsNothing() {
        var world = world();
        var naturalStone = naturalBlock(world, 5, 5, Material.STONE, List.of(drop()));
        var event = explosion(world, 9, 9, true, naturalStone);
        BlastHarvestMechanic.handleExplosion(event);
        verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }

    @Test
    void nonTntExplosionIsIgnored() {
        var world = world();
        var player = player(world);
        assertTrue(mark(world, 1, 1, player, "#c:mineables", EnumSet.of(Material.STONE)));
        var naturalStone = naturalBlock(world, 5, 5, Material.STONE, List.of(drop()));
        var event = explosion(world, 1, 1, false, naturalStone);
        BlastHarvestMechanic.handleExplosion(event);
        verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }

    @Test
    void chargeIsConsumedOnDetonation() {
        var world = world();
        var player = player(world);
        assertTrue(mark(world, 2, 2, player, "#c:mineables", EnumSet.of(Material.STONE)));

        var naturalStone = naturalBlock(world, 5, 5, Material.STONE, List.of(drop()));
        BlastHarvestMechanic.handleExplosion(explosion(world, 2, 2, true, naturalStone));
        verify(world, times(1)).dropItemNaturally(any(Location.class), any(ItemStack.class));

        // The charge is gone: a second blast at the same spot now harvests nothing.
        var again = naturalBlock(world, 5, 5, Material.STONE, List.of(drop()));
        BlastHarvestMechanic.handleExplosion(explosion(world, 2, 2, true, again));
        verify(world, times(1)).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }

    @Test
    void defusedChargeMarkIsClearedOnBreak() {
        var world = world();
        var player = player(world);
        assertTrue(mark(world, 4, 4, player, "#c:mineables", EnumSet.of(Material.STONE)));

        var tntBlock = mock(Block.class);
        when(tntBlock.getLocation()).thenReturn(new Location(world, 4, 64, 4));
        BlastHarvestMechanic.clearMark(tntBlock);

        var naturalStone = naturalBlock(world, 5, 5, Material.STONE, List.of(drop()));
        BlastHarvestMechanic.handleExplosion(explosion(world, 4, 4, true, naturalStone));
        verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
    }

    @Test
    void shieldsItemsInsideTheHarvestBlastRadiusOnly() {
        var world = world();
        var player = player(world);
        assertTrue(mark(world, 1, 1, player, "#c:mineables", EnumSet.of(Material.STONE)));
        // A destroyed block at (5,5) yields a computed blast radius of ~6 around (1,1).
        var naturalStone = naturalBlock(world, 5, 5, Material.STONE, List.of(drop()));
        BlastHarvestMechanic.handleExplosion(explosion(world, 1, 1, true, naturalStone));

        var inside = mock(Item.class);
        when(inside.getLocation()).thenReturn(new Location(world, 2, 64, 1));
        var insideEvent = mock(EntityDamageEvent.class);
        when(insideEvent.getEntity()).thenReturn(inside);
        when(insideEvent.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION);
        BlastHarvestMechanic.handleItemDamage(insideEvent);
        verify(insideEvent).setCancelled(true);

        // Far outside the computed radius: left to die like a normal explosion.
        var outside = mock(Item.class);
        when(outside.getLocation()).thenReturn(new Location(world, 9, 64, 9));
        var outsideEvent = mock(EntityDamageEvent.class);
        when(outsideEvent.getEntity()).thenReturn(outside);
        when(outsideEvent.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION);
        BlastHarvestMechanic.handleItemDamage(outsideEvent);
        verify(outsideEvent, never()).setCancelled(true);
    }
}