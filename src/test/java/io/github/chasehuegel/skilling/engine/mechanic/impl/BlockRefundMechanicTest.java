package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlockRefundMechanicTest {

    @BeforeEach
    void setUp() {
        // The refund item would otherwise require a live item registry (absent in
        // a plain-JUnit JVM), so produce a mock ItemStack per placed material.
        BlockRefundMechanic.setItemFactory(material -> {
            var stack = mock(ItemStack.class);
            when(stack.getType()).thenReturn(material);
            when(stack.getAmount()).thenReturn(1);
            return stack;
        });
    }

    @AfterEach
    void tearDown() {
        BlockRefundMechanic.reset();
        BlockRefundMechanic.resetItemFactory();
    }

    private Block placedBlock(World world, Material material) {
        var block = mock(Block.class);
        when(block.getType()).thenReturn(material);
        when(block.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        return block;
    }

    private Player player(org.bukkit.inventory.PlayerInventory inv) {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getInventory()).thenReturn(inv);
        return player;
    }

    @Test
    void returnsFalseForNonBlockPlaceEvent() {
        var player = mock(Player.class);
        assertFalse(new BlockRefundMechanic().execute(player, Map.of("chance", 50.0),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }

    @Test
    void returnsFalseWithZeroChance() {
        var player = mock(Player.class);
        assertFalse(new BlockRefundMechanic().execute(player, Map.of("chance", 0.0),
                mock(BlockPlaceEvent.class)));
    }

    @Test
    void refundsThePlacedMaterialOnSuccessfulRoll() {
        var world = mock(World.class);
        var block = placedBlock(world, Material.SCAFFOLDING);
        var event = mock(BlockPlaceEvent.class);
        when(event.getBlockPlaced()).thenReturn(block);

        var inv = mock(org.bukkit.inventory.PlayerInventory.class);
        when(inv.addItem(any(ItemStack.class))).thenReturn(new HashMap<>());
        var player = player(inv);

        // Force the roll to succeed on a 5% chance.
        BlockRefundMechanic.setRandomSource(() -> 4.99);
        assertTrue(new BlockRefundMechanic().execute(player, Map.of("chance", 5.0), event));

        var captor = forClass(ItemStack.class);
        verify(inv).addItem(captor.capture());
        assertEquals(Material.SCAFFOLDING, captor.getValue().getType());
        assertEquals(1, captor.getValue().getAmount());
    }

    @Test
    void doesNotRefundOnFailedRoll() {
        var world = mock(World.class);
        var block = placedBlock(world, Material.STONE_BRICKS);
        var event = mock(BlockPlaceEvent.class);
        when(event.getBlockPlaced()).thenReturn(block);

        var inv = mock(org.bukkit.inventory.PlayerInventory.class);
        var player = player(inv);

        // Force the roll to fail on a 5% chance.
        BlockRefundMechanic.setRandomSource(() -> 5.01);
        new BlockRefundMechanic().execute(player, Map.of("chance", 5.0), event);
        verify(inv, never()).addItem(any(ItemStack.class));
    }

    @Test
    void failedRollStillReturnsTrueAsAnActivationAttempt() {
        var world = mock(World.class);
        var block = placedBlock(world, Material.STONE_BRICKS);
        var event = mock(BlockPlaceEvent.class);
        when(event.getBlockPlaced()).thenReturn(block);

        var inv = mock(org.bukkit.inventory.PlayerInventory.class);
        when(inv.addItem(any(ItemStack.class))).thenReturn(new HashMap<>());
        var player = player(inv);

        BlockRefundMechanic.setRandomSource(() -> 100.0);
        assertTrue(new BlockRefundMechanic().execute(player, Map.of("chance", 5.0), event),
                "a place of an eligible block is an activation attempt even when the roll misses");
    }

    @Test
    void dropsRefundWhenInventoryFull() {
        var world = mock(World.class);
        var block = placedBlock(world, Material.CHAIN);
        var event = mock(BlockPlaceEvent.class);
        when(event.getBlockPlaced()).thenReturn(block);

        var inv = mock(org.bukkit.inventory.PlayerInventory.class);
        when(inv.addItem(any(ItemStack.class)))
                .thenAnswer(inv2 -> new HashMap<>(Map.of(0, (ItemStack) inv2.getArgument(0))));
        var player = player(inv);
        when(player.getWorld()).thenReturn(world);

        BlockRefundMechanic.setRandomSource(() -> 0.0);
        new BlockRefundMechanic().execute(player, Map.of("chance", 100.0), event);

        var captor = forClass(ItemStack.class);
        verify(world).dropItemNaturally(eq(block.getLocation()), captor.capture());
        assertEquals(Material.CHAIN, captor.getValue().getType());
    }
}
