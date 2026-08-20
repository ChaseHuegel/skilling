package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.metadata.MetadataValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class MarkedDemolitionMechanicTest {

    private static final UUID OWNER = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        MarkedDemolitionMechanic.clearAll();
    }

    private World world() {
        return mock(World.class);
    }

    /** A TNT placement event whose block sits at the given coordinates. */
    private BlockPlaceEvent tntPlacement(World world, int x, int z, Player player, boolean sneaking) {
        var block = mock(Block.class);
        when(block.getType()).thenReturn(Material.TNT);
        when(block.getLocation()).thenReturn(new Location(world, x, 64, z));
        var event = mock(BlockPlaceEvent.class);
        when(event.getBlockPlaced()).thenReturn(block);
        when(player.isSneaking()).thenReturn(sneaking);
        return event;
    }

    private boolean mark(World world, int x, int z, Player player, boolean sneaking, String targetRef,
            EnumSet<Material> targets) {
        var plugin = mock(Skilling.class);
        var resolver = mock(TagResolver.class);
        when(resolver.resolve(targetRef)).thenReturn(targets);
        when(plugin.getTagResolver()).thenReturn(resolver);
        try (MockedStatic<Skilling> skilling = mockStatic(Skilling.class)) {
            when(Skilling.getInstance()).thenReturn(plugin);
            return new MarkedDemolitionMechanic().execute(player,
                    Map.of("target", targetRef), tntPlacement(world, x, z, player, sneaking));
        }
    }

    /** A block owned by a given player, of the given material, flagged as placed. */
    private Block ownedBlock(World world, int x, int z, Material material, UUID owner) {
        var block = mock(Block.class);
        when(block.getType()).thenReturn(material);
        when(block.hasMetadata("player_placed")).thenReturn(true);
        when(block.hasMetadata(MarkedDemolitionMechanic.OWNER_META_KEY)).thenReturn(true);
        var ownerValue = mock(MetadataValue.class);
        when(ownerValue.value()).thenReturn(owner);
        when(block.getMetadata(MarkedDemolitionMechanic.OWNER_META_KEY)).thenReturn(List.of(ownerValue));
        return block;
    }

    /** A natural (never-placed) block. */
    private Block naturalBlock(Material material) {
        var block = mock(Block.class);
        when(block.getType()).thenReturn(material);
        when(block.hasMetadata("player_placed")).thenReturn(false);
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
        List<Block> blockList = new ArrayList<>(List.of(blocks));
        when(event.blockList()).thenReturn(blockList);
        return event;
    }

    @Test
    void returnsFalseForNonBlockPlaceEvent() {
        var player = mock(Player.class);
        assertFalse(new MarkedDemolitionMechanic().execute(player, Map.of("target", "#c:demolition_blocks"),
                mock(EntityExplodeEvent.class)));
    }

    @Test
    void returnsFalseWhenPlacingNonTnt() {
        var world = world();
        var player = mock(Player.class);
        when(player.isSneaking()).thenReturn(true);
        var nonTnt = mock(BlockPlaceEvent.class);
        var block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);
        when(nonTnt.getBlockPlaced()).thenReturn(block);
        var plugin = mock(Skilling.class);
        try (MockedStatic<Skilling> skilling = mockStatic(Skilling.class)) {
            when(Skilling.getInstance()).thenReturn(plugin);
            assertFalse(new MarkedDemolitionMechanic().execute(player,
                    Map.of("target", "#c:demolition_blocks"), nonTnt));
        }
    }

    @Test
    void returnsFalseWhenNotSneaking() {
        var world = world();
        var player = mock(Player.class);
        assertFalse(mark(world, 3, 3, player, false, "#c:demolition_blocks",
                EnumSet.of(Material.STONE_BRICKS)));
    }

    @Test
    void marksSneakPlacedTntAndPrunesExplosionToOwnersBlocks() {
        var world = world();
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(OWNER);
        assertTrue(mark(world, 1, 1, player, true, "#c:demolition_blocks",
                EnumSet.of(Material.STONE_BRICKS)));

        var ownerBlock = ownedBlock(world, 5, 5, Material.STONE_BRICKS, OWNER);
        var naturalGround = naturalBlock(Material.STONE);
        var someoneElsesBuild = ownedBlock(world, 6, 6, Material.STONE_BRICKS, UUID.randomUUID());
        var otherMaterial = ownedBlock(world, 7, 7, Material.DIRT, OWNER);

        var event = explosion(world, 1, 1, true, ownerBlock, naturalGround, someoneElsesBuild, otherMaterial);
        MarkedDemolitionMechanic.handleExplosion(event);

        assertEquals(List.of(ownerBlock), event.blockList(),
                "only the owner's construction blocks survive the prune");
    }

    @Test
    void unmarkedExplosionIsUntouched() {
        var world = world();
        var naturalGround = naturalBlock(Material.STONE);
        var event = explosion(world, 9, 9, true, naturalGround);
        MarkedDemolitionMechanic.handleExplosion(event);
        assertEquals(1, event.blockList().size(), "no mark means the block list is left vanilla");
    }

    @Test
    void nonTntExplosionIsIgnored() {
        var world = world();
        var naturalGround = naturalBlock(Material.STONE);
        var event = explosion(world, 9, 9, false, naturalGround);
        MarkedDemolitionMechanic.handleExplosion(event);
        assertEquals(1, event.blockList().size());
    }

    @Test
    void markIsConsumedOnDetonation() {
        var world = world();
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(OWNER);
        assertTrue(mark(world, 2, 2, player, true, "#c:demolition_blocks",
                EnumSet.of(Material.STONE_BRICKS)));

        var ownerBlock = ownedBlock(world, 5, 5, Material.STONE_BRICKS, OWNER);
        var first = explosion(world, 2, 2, true, ownerBlock);
        MarkedDemolitionMechanic.handleExplosion(first);
        assertEquals(List.of(ownerBlock), first.blockList());

        // The mark is gone: a second explosion at the same spot is now vanilla.
        var second = explosion(world, 2, 2, true, naturalBlock(Material.STONE));
        MarkedDemolitionMechanic.handleExplosion(second);
        assertEquals(1, second.blockList().size());
    }

    @Test
    void defusedChargeMarkIsClearedOnBreak() {
        var world = world();
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(OWNER);
        assertTrue(mark(world, 4, 4, player, true, "#c:demolition_blocks",
                EnumSet.of(Material.STONE_BRICKS)));

        // The player mines the marked TNT before it detonates; the mark clears.
        var tntBlock = mock(Block.class);
        when(tntBlock.getLocation()).thenReturn(new Location(world, 4, 64, 4));
        MarkedDemolitionMechanic.clearMark(tntBlock);

        var event = explosion(world, 4, 4, true, naturalBlock(Material.STONE));
        MarkedDemolitionMechanic.handleExplosion(event);
        assertEquals(1, event.blockList().size(), "a defused charge must not prune");
    }
}
