package io.github.chasehuegel.skilling.engine.mechanic.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.junit.jupiter.api.Test;

class ThrowHasteMechanicTest {

    private final ThrowHasteMechanic mechanic = new ThrowHasteMechanic();

    private ProjectileLaunchEvent launch(org.bukkit.entity.Projectile projectile,
                                         org.bukkit.projectiles.ProjectileSource shooter) {
        var event = mock(ProjectileLaunchEvent.class);
        when(event.getEntity()).thenReturn(projectile);
        when(projectile.getShooter()).thenReturn(shooter);
        return event;
    }

    private Trident trident() {
        Trident trident = mock(Trident.class);
        when(trident.getType()).thenReturn(EntityType.TRIDENT);
        return trident;
    }

    @Test
    void shortensCooldownOnPlayersThrownTrident() {
        Player player = mock(Player.class);

        assertTrue(mechanic.execute(player, Map.of("reduction", 50.0), launch(trident(), player)));
        verify(player).setCooldown(Material.TRIDENT, 10);
    }

    @Test
    void fullReductionStillLeavesARealCooldown() {
        Player player = mock(Player.class);

        assertTrue(mechanic.execute(player, Map.of("reduction", 100.0), launch(trident(), player)));
        verify(player).setCooldown(Material.TRIDENT, 4);
    }

    @Test
    void returnsFalseForNonTridentThrow() {
        Player player = mock(Player.class);
        var arrow = mock(org.bukkit.entity.Arrow.class);
        when(arrow.getType()).thenReturn(EntityType.ARROW);

        assertFalse(mechanic.execute(player, Map.of("reduction", 50.0), launch(arrow, player)));
        verify(player, never()).setCooldown(any(Material.class), anyInt());
    }

    @Test
    void returnsFalseWhenNotThrownByThePlayer() {
        Player player = mock(Player.class);
        var other = mock(org.bukkit.entity.Zombie.class);

        assertFalse(mechanic.execute(player, Map.of("reduction", 50.0), launch(trident(), other)));
    }

    @Test
    void returnsFalseWithoutAReduction() {
        Player player = mock(Player.class);
        assertFalse(mechanic.execute(player, Map.of(), launch(trident(), player)));
    }

    @Test
    void returnsFalseForNonLaunchEvent() {
        Player player = mock(Player.class);
        assertFalse(mechanic.execute(player, Map.of("reduction", 50.0),
                mock(org.bukkit.event.entity.ProjectileHitEvent.class)));
    }
}