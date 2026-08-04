package io.github.chasehuegel.skilling.feedback;

import io.github.chasehuegel.skilling.engine.feedback.FanfareDispatcher;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FanfareDispatcherTest {

    @Test
    void sendActionBarWithNullDoesNothing() {
        var player = mock(Player.class);
        FanfareDispatcher.sendActionBar(player, null);
        verify(player, never()).sendActionBar(any(Component.class));
    }

    @Test
    void sendActionBarWithBlankDoesNothing() {
        var player = mock(Player.class);
        FanfareDispatcher.sendActionBar(player, "   ");
        verify(player, never()).sendActionBar(any(Component.class));
    }

    @Test
    void sendActionBarWithValidMessageSendsActionBar() {
        var player = mock(Player.class);
        FanfareDispatcher.sendActionBar(player, "&aHello");
        verify(player).sendActionBar(any(Component.class));
    }

    @Test
    void dispatchParticlesWithNullDoesNothing() {
        var player = mock(Player.class);
        FanfareDispatcher.dispatchParticles(player, null, null);
        verifyNoInteractions(player);
    }

    @Test
    void dispatchParticlesWithEmptyListDoesNothing() {
        var player = mock(Player.class);
        FanfareDispatcher.dispatchParticles(player, null, List.of());
        verifyNoInteractions(player);
    }

    @Test
    void dispatchSoundsWithNullDoesNothing() {
        var player = mock(Player.class);
        FanfareDispatcher.dispatchSounds(player, null, null);
        verifyNoInteractions(player);
    }

    @Test
    void dispatchSoundsWithEmptyListDoesNothing() {
        var player = mock(Player.class);
        FanfareDispatcher.dispatchSounds(player, null, List.of());
        verifyNoInteractions(player);
    }

    // dispatchSounds with a valid sound config cannot be tested without a live
    // Paper server because org.bukkit.Sound is registry-backed in 1.21.8
    // and throws ExceptionInInitializerError on class loading in unit tests.

    @Test
    void dispatchSoundsPlaysNamespacedSound() {
        var player = mock(Player.class);
        var world = mock(org.bukkit.World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 0, 0, 0));

        FanfareDispatcher.dispatchSounds(player, null, List.of(Map.of(
                "type", "minecraft:entity.player.levelup", "volume", 1.0, "pitch", 1.0)));

        verify(world).playSound(any(Location.class), any(org.bukkit.Sound.class),
                any(org.bukkit.SoundCategory.class), eq(1.0f), eq(1.0f));
    }

    @Test
    void dispatchParticlesSpawnsNamespacedParticle() {
        var player = mock(Player.class);
        var world = mock(org.bukkit.World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 0, 0, 0));

        FanfareDispatcher.dispatchParticles(player, null, List.of(Map.of(
                "type", "minecraft:happy_villager", "count", 5,
                "offset", List.of(0, 0, 0), "speed", 0.1)));

        verify(world).spawnParticle(any(org.bukkit.Particle.class), any(Location.class),
                eq(5), eq(0.0), eq(0.0), eq(0.0), eq(0.1));
    }
}
