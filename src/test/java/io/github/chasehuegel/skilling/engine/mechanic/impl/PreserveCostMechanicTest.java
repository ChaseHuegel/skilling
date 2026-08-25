package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies {@code core:preserve_cost} reports a successful roll via
 * {@code didProc()} while always counting as an activation attempt, and is a
 * no-op when no preserve chance is configured.
 */
class PreserveCostMechanicTest {

    private static final Map<String, Object> CHANCE = Map.of("chance", 50.0);

    @AfterEach
    void restoreRandom() {
        PreserveCostMechanic.setRandomSource(() -> java.util.concurrent.ThreadLocalRandom.current().nextDouble(100));
    }

    @Test
    void procWhenRollLands() {
        PreserveCostMechanic.setRandomSource(() -> 10.0);
        var mechanic = new PreserveCostMechanic();
        assertTrue(mechanic.execute(mock(Player.class), CHANCE, mock(PlayerInteractEvent.class)));
        assertTrue(mechanic.didProc());
    }

    @Test
    void missWhenRollMisses() {
        PreserveCostMechanic.setRandomSource(() -> 90.0);
        var mechanic = new PreserveCostMechanic();
        assertTrue(mechanic.execute(mock(Player.class), CHANCE, mock(PlayerInteractEvent.class)));
        org.junit.jupiter.api.Assertions.assertFalse(mechanic.didProc());
    }

    @Test
    void zeroChanceIsNoOp() {
        var mechanic = new PreserveCostMechanic();
        assertFalse(mechanic.execute(mock(Player.class), Map.of("chance", 0.0), mock(PlayerInteractEvent.class)));
        assertFalse(mechanic.didProc());
    }
}