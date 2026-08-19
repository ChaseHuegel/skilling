package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.PotionSplashEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link PotionSelfImmunityMechanic} shields the thrower and allied
 * players from the thrown potion while leaving hostile mobs affected.
 */
class PotionSelfImmunityMechanicTest {

    @Test
    void shieldsThrowerAndAllyButNotHostile() {
        var mechanic = new PotionSelfImmunityMechanic();
        var thrower = mock(Player.class);
        var ally = mock(Player.class);
        var zombie = mock(Zombie.class);

        var event = mock(PotionSplashEvent.class);
        when(event.getAffectedEntities()).thenReturn(List.of(thrower, ally, zombie));

        assertTrue(mechanic.execute(thrower, Map.of(), event));
        verify(event).setIntensity(thrower, 0.0);
        verify(event).setIntensity(ally, 0.0);
        // Hostile mobs still take the potion.
        verify(event, org.mockito.Mockito.never()).setIntensity(zombie, 0.0);
    }

    @Test
    void returnsFalseForNonSplashEvent() {
        var mechanic = new PotionSelfImmunityMechanic();
        assertFalse(mechanic.execute(mock(Player.class), Map.of(), mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }
}
