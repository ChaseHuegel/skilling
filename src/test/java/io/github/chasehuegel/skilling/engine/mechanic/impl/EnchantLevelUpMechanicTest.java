package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.testutil.TestEnchantments;
import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnchantLevelUpMechanicTest {

    @AfterEach
    void tearDown() {
        EnchantLevelUpMechanic.reset();
    }

    @Test
    void returnsFalseForNonEnchantEvent() {
        var player = mock(Player.class);
        assertFalse(new EnchantLevelUpMechanic().execute(player, Map.of("chance", 100.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var player = mock(Player.class);
        var event = mock(EnchantItemEvent.class);
        when(event.getEnchanter()).thenReturn(player);
        assertFalse(new EnchantLevelUpMechanic().execute(player, Map.of(), event));
    }

    @Test
    void successfulRollRaisesLevelByOne() {
        try (MockedStatic<RegistryAccess> registry = TestEnchantments.mockAccess()) {
            EnchantLevelUpMechanic.setRandomSource(() -> 10.0);
            var player = mock(Player.class);
            var event = mock(EnchantItemEvent.class);
            when(event.getEnchanter()).thenReturn(player);
            var e = TestEnchantments.fake("sharpness", 5, true, false);
            var chosen = new HashMap<Enchantment, Integer>();
            chosen.put(e, 2);
            when(event.getEnchantsToAdd()).thenReturn(chosen);

            assertTrue(new EnchantLevelUpMechanic().execute(player, Map.of("chance", 50.0), event));
            assertEquals(Integer.valueOf(3), chosen.get(e));
        }
    }

    @Test
    void failedRollLeavesLevelUnchanged() {
        try (MockedStatic<RegistryAccess> registry = TestEnchantments.mockAccess()) {
            EnchantLevelUpMechanic.setRandomSource(() -> 99.0);
            var player = mock(Player.class);
            var event = mock(EnchantItemEvent.class);
            when(event.getEnchanter()).thenReturn(player);
            var e = TestEnchantments.fake("sharpness", 5, true, false);
            var chosen = new HashMap<Enchantment, Integer>();
            chosen.put(e, 2);
            when(event.getEnchantsToAdd()).thenReturn(chosen);

            assertTrue(new EnchantLevelUpMechanic().execute(player, Map.of("chance", 50.0), event));
            assertEquals(Integer.valueOf(2), chosen.get(e));
        }
    }

    @Test
    void capsAtMaxLevel() {
        try (MockedStatic<RegistryAccess> registry = TestEnchantments.mockAccess()) {
            EnchantLevelUpMechanic.setRandomSource(() -> 10.0);
            var player = mock(Player.class);
            var event = mock(EnchantItemEvent.class);
            when(event.getEnchanter()).thenReturn(player);
            var e = TestEnchantments.fake("sharpness", 3, true, false);
            var chosen = new HashMap<Enchantment, Integer>();
            chosen.put(e, 3);
            when(event.getEnchantsToAdd()).thenReturn(chosen);

            assertTrue(new EnchantLevelUpMechanic().execute(player, Map.of("chance", 50.0), event));
            assertEquals(Integer.valueOf(3), chosen.get(e));
        }
    }
}
