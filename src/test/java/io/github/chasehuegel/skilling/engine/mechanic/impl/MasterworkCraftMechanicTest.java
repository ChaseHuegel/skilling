package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.testutil.TestEnchantments;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MasterworkCraftMechanicTest {

    @AfterEach
    void tearDown() {
        MasterworkCraftMechanic.reset();
    }

    @Test
    void returnsFalseForNonCraftEvent() {
        var player = mock(Player.class);
        assertFalse(new MasterworkCraftMechanic().execute(player, Map.of("chance", 100.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWithZeroChance() {
        var player = mock(Player.class);
        var event = mock(CraftItemEvent.class);
        assertFalse(new MasterworkCraftMechanic().execute(player, Map.of(), event));
    }

    @Test
    void failedRollCountsAsActivationWithoutEnchanting() {
        MasterworkCraftMechanic.setRandomSource(() -> 90.0);
        var player = mock(Player.class);
        var result = mock(ItemStack.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.getEnchantments()).thenReturn(new HashMap<>());
        var event = mock(CraftItemEvent.class);
        when(event.getCurrentItem()).thenReturn(result);
        MasterworkCraftMechanic.setEnchantSource(() -> List.of(TestEnchantments.fake("a", 3, true, false)));

        assertTrue(new MasterworkCraftMechanic().execute(player, Map.of("chance", 50.0), event));
        verify(result, never()).addEnchantment(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
        verify(event, never()).setCurrentItem(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void successfulRollAddsCompatibleEnchant() {
        MasterworkCraftMechanic.setRandomSource(() -> 10.0);
        MasterworkCraftMechanic.setLevelRandom(() -> 0.0);
        var player = mock(Player.class);
        var result = mock(ItemStack.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.getEnchantments()).thenReturn(new HashMap<>());
        var event = mock(CraftItemEvent.class);
        when(event.getCurrentItem()).thenReturn(result);
        var e = TestEnchantments.fake("sharpness", 3, true, false);
        MasterworkCraftMechanic.setEnchantSource(() -> List.of(e));

        assertTrue(new MasterworkCraftMechanic().execute(player, Map.of("chance", 50.0), event));
        verify(result).addEnchantment(e, 1);
        verify(event).setCurrentItem(result);
    }

    @Test
    void skippedOnEmptyResultButStillCountsAsActivation() {
        MasterworkCraftMechanic.setRandomSource(() -> 10.0);
        var player = mock(Player.class);
        var event = mock(CraftItemEvent.class);
        when(event.getCurrentItem()).thenReturn(null);

        // With a null result usage is a no-op grant but the roll already passed.
        assertTrue(new MasterworkCraftMechanic().execute(player, Map.of("chance", 50.0), event));
    }

    @Test
    void skipsIncompatibleEnchants() {
        MasterworkCraftMechanic.setRandomSource(() -> 10.0);
        MasterworkCraftMechanic.setLevelRandom(() -> 0.0);
        var player = mock(Player.class);
        var result = mock(ItemStack.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.getEnchantments()).thenReturn(new HashMap<>());
        var event = mock(CraftItemEvent.class);
        when(event.getCurrentItem()).thenReturn(result);
        var incompatible = TestEnchantments.fake("b", 3, false, false);
        var compatible = TestEnchantments.fake("a", 3, true, true);
        MasterworkCraftMechanic.setEnchantSource(() -> List.of(incompatible, compatible));

        assertTrue(new MasterworkCraftMechanic().execute(player, Map.of("chance", 50.0), event));
        verify(result).addEnchantment(compatible, 1);
        verify(result, never()).addEnchantment(incompatible, 1);
    }
}