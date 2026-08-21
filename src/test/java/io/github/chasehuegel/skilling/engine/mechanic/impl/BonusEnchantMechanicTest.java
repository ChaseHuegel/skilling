package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.testutil.TestEnchantments;
import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BonusEnchantMechanicTest {

    @AfterEach
    void tearDown() {
        BonusEnchantMechanic.reset();
    }

    @Test
    void returnsFalseForNonEnchantEvent() {
        var player = mock(Player.class);
        assertFalse(new BonusEnchantMechanic().execute(player, Map.of("chance", 100.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWhenEnchanterDiffers() {
        var player = mock(Player.class);
        var other = mock(Player.class);
        var event = mock(EnchantItemEvent.class);
        when(event.getEnchanter()).thenReturn(other);
        assertFalse(new BonusEnchantMechanic().execute(player, Map.of("chance", 100.0), event));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var player = mock(Player.class);
        var event = mock(EnchantItemEvent.class);
        when(event.getEnchanter()).thenReturn(player);
        assertFalse(new BonusEnchantMechanic().execute(player, Map.of(), event));
    }

    @Test
    void failedRollCountsAsActivationWithoutAdding() {
        try (MockedStatic<RegistryAccess> registry = TestEnchantments.mockAccess()) {
            BonusEnchantMechanic.setRandomSource(() -> 90.0);
            var player = mock(Player.class);
            var event = mock(EnchantItemEvent.class);
            when(event.getEnchanter()).thenReturn(player);
            var chosen = new HashMap<Enchantment, Integer>();
            when(event.getItem()).thenReturn(mock(ItemStack.class));
            when(event.getEnchantsToAdd()).thenReturn(chosen);
            BonusEnchantMechanic.setEnchantSource(() -> List.of(
                    TestEnchantments.fake("a", 3, true, false)));

            assertTrue(new BonusEnchantMechanic().execute(player, Map.of("chance", 50.0), event));
            assertTrue(chosen.isEmpty());
        }
    }

    @Test
    void successfulRollAddsCompatibleEnchant() {
        try (MockedStatic<RegistryAccess> registry = TestEnchantments.mockAccess()) {
            // 10 < 50 passes the roll; same source picks index 0; levelRandom 0 -> level 1.
            BonusEnchantMechanic.setRandomSource(() -> 10.0);
            BonusEnchantMechanic.setLevelRandom(() -> 0.0);
            var player = mock(Player.class);
            var event = mock(EnchantItemEvent.class);
            when(event.getEnchanter()).thenReturn(player);
            when(event.getItem()).thenReturn(mock(ItemStack.class));
            var chosen = new HashMap<Enchantment, Integer>();
            when(event.getEnchantsToAdd()).thenReturn(chosen);
            var e = TestEnchantments.fake("sharpness", 3, true, false);
            BonusEnchantMechanic.setEnchantSource(() -> List.of(e));

            assertTrue(new BonusEnchantMechanic().execute(player, Map.of("chance", 50.0), event));
            assertEquals(Integer.valueOf(1), chosen.get(e));
        }
    }

    @Test
    void skipsIncompatibleAndConflictingEnchants() {
        try (MockedStatic<RegistryAccess> registry = TestEnchantments.mockAccess()) {
            BonusEnchantMechanic.setRandomSource(() -> 10.0);
            BonusEnchantMechanic.setLevelRandom(() -> 0.0);
            var player = mock(Player.class);
            var event = mock(EnchantItemEvent.class);
            when(event.getEnchanter()).thenReturn(player);
            when(event.getItem()).thenReturn(mock(ItemStack.class));
            var chosen = new HashMap<Enchantment, Integer>();
            when(event.getEnchantsToAdd()).thenReturn(chosen);
            var compatible = TestEnchantments.fake("a", 3, true, true);
            var incompatible = TestEnchantments.fake("b", 3, false, false);
            BonusEnchantMechanic.setEnchantSource(() -> List.of(incompatible, compatible));

            assertTrue(new BonusEnchantMechanic().execute(player, Map.of("chance", 50.0), event));
            assertEquals(Integer.valueOf(1), chosen.get(compatible));
            assertFalse(chosen.containsKey(incompatible));
        }
    }
}
