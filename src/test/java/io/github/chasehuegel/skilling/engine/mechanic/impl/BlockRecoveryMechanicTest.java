package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link BlockRecoveryMechanic}: it records an activation attempt for a
 * blocked hit so the ability reads as an endurance scalar, and no-ops for
 * non-damage events, damage not on the player, an unblocking stance, or an empty
 * reduction. The one-tick cooldown rewrite needs a live plugin, so in plain-JUnit
 * it reduces to a validated activation signal.
 */
class BlockRecoveryMechanicTest {

    private EntityDamageEvent damageOn(Player player) {
        var event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        return event;
    }

    @Test
    void returnsFalseForNonDamageEvent() {
        var player = mock(Player.class);
        assertFalse(new BlockRecoveryMechanic().execute(player, Map.of("reduction", 20.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWhenDamageNotOnPlayer() {
        var player = mock(Player.class);
        var other = mock(Player.class);
        assertFalse(new BlockRecoveryMechanic().execute(player, Map.of("reduction", 20.0), damageOn(other)));
    }

    @Test
    void returnsFalseWhenNotBlocking() {
        var player = mock(Player.class);
        when(player.isBlocking()).thenReturn(false);
        assertFalse(new BlockRecoveryMechanic().execute(player, Map.of("reduction", 20.0), damageOn(player)));
    }

    @Test
    void returnsFalseWhenReductionIsZero() {
        var player = mock(Player.class);
        when(player.isBlocking()).thenReturn(true);
        assertFalse(new BlockRecoveryMechanic().execute(player, Map.of(), damageOn(player)));
    }

    @Test
    void blockedHitWithReductionCountsAsActivation() {
        var player = mock(Player.class);
        when(player.isBlocking()).thenReturn(true);
        assertTrue(new BlockRecoveryMechanic().execute(player, Map.of("reduction", 20.0), damageOn(player)));
    }
}