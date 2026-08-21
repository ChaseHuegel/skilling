package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeepOnDeathMechanicTest {

    @AfterEach
    void tearDown() {
        KeepOnDeathMechanic.reset();
    }

    @Test
    void returnsFalseForNonDeathEvent() {
        var player = mock(Player.class);
        assertFalse(new KeepOnDeathMechanic().execute(player, Map.of("chance", 100.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var player = mock(Player.class);
        assertFalse(new KeepOnDeathMechanic().execute(player, Map.of(),
                mock(PlayerDeathEvent.class)));
    }

    @Test
    void failedRollDropsEverythingNormally() {
        KeepOnDeathMechanic.setRandomSource(() -> 99.0);
        var player = mock(Player.class);
        var event = mock(PlayerDeathEvent.class);
        when(event.getDrops()).thenReturn(new ArrayList<org.bukkit.inventory.ItemStack>());

        assertTrue(new KeepOnDeathMechanic().execute(player, Map.of("chance", 50.0), event));
        verify(event, never()).setKeepInventory(true);
    }

    @Test
    void successfulRollKeepsInventoryAndClearsDrops() {
        KeepOnDeathMechanic.setRandomSource(() -> 10.0);
        var player = mock(Player.class);
        var event = mock(PlayerDeathEvent.class);
        var drops = new ArrayList<org.bukkit.inventory.ItemStack>();
        drops.add(mock(org.bukkit.inventory.ItemStack.class));
        when(event.getDrops()).thenReturn(drops);

        assertTrue(new KeepOnDeathMechanic().execute(player, Map.of("chance", 50.0), event));
        verify(event).setKeepInventory(true);
        assertTrue(drops.isEmpty());
    }
}
