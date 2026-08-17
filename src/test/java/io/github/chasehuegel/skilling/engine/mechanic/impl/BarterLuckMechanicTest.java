package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:barter_luck}: a successful roll appends a bonus emerald
 * to the barter outcome list, a failed roll leaves the outcome untouched but
 * still counts as an activation attempt, and a non-barter event is a no-op.
 */
class BarterLuckMechanicTest {

    @AfterEach
    void tearDown() {
        BarterLuckMechanic.setRandomSource(null);
        BarterLuckMechanic.setEmeraldSource(null);
    }

    private PiglinBarterEvent barterEvent(List<ItemStack> outcome) {
        var event = mock(PiglinBarterEvent.class);
        when(event.getOutcome()).thenReturn(outcome);
        return event;
    }

    private static ItemStack emerald() {
        var emerald = mock(ItemStack.class);
        when(emerald.getType()).thenReturn(Material.EMERALD);
        return emerald;
    }

    @Test
    void returnsFalseForNonBarterEvent() {
        assertFalse(new BarterLuckMechanic().execute(mock(Player.class), Map.of("chance", 100.0),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }

    @Test
    void returnsFalseWithNonPositiveChance() {
        assertFalse(new BarterLuckMechanic().execute(mock(Player.class), Map.of("chance", 0.0),
                mock(PiglinBarterEvent.class)));
    }

    private static ItemStack enderPearl() {
        var pearl = mock(ItemStack.class);
        when(pearl.getType()).thenReturn(Material.ENDER_PEARL);
        return pearl;
    }

    @Test
    void successfulRollAppendsBonusEmerald() {
        List<ItemStack> outcome = new ArrayList<>(List.of(enderPearl()));
        BarterLuckMechanic.setRandomSource(() -> 0.0); // always succeed
        BarterLuckMechanic.setEmeraldSource(BarterLuckMechanicTest::emerald);

        assertTrue(new BarterLuckMechanic().execute(mock(Player.class), Map.of("chance", 50.0),
                barterEvent(outcome)));

        assertTrue(outcome.stream().anyMatch(i -> i.getType() == Material.EMERALD),
                "the barter outcome must gain an emerald");
    }

    @Test
    void failedRollLeavesOutcomeUntouched() {
        List<ItemStack> outcome = new ArrayList<>(List.of(enderPearl()));
        BarterLuckMechanic.setRandomSource(() -> 0.99); // always fail

        assertTrue(new BarterLuckMechanic().execute(mock(Player.class), Map.of("chance", 50.0),
                barterEvent(outcome)),
                "a failed roll still counts as an activation attempt");
        assertTrue(outcome.stream().noneMatch(i -> i.getType() == Material.EMERALD),
                "a failed roll must not alter the outcome");
    }
}
