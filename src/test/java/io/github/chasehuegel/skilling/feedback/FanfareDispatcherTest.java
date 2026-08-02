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
}
