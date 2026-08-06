package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ApplyStatusMechanic;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplyStatusMechanicTest {

    private final ApplyStatusMechanic mechanic = new ApplyStatusMechanic();

    @Test
    void returnsFalseForNonEntityEvent() {
        var player = BukkitMock.mockPlayer();
        assertFalse(mechanic.execute(player, Map.of("effect", "speed"), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void returnsFalseWhenDamagerNotPlayer() {
        var player = BukkitMock.mockPlayer();
        var otherPlayer = mock(Player.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(otherPlayer);
        assertFalse(mechanic.execute(player, Map.of("effect", "speed"), event));
    }

    @Test
    void throwsOnMissingEffectParam() {
        var player = BukkitMock.mockPlayer();
        var monster = mock(Monster.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(monster);
        assertThrows(IllegalArgumentException.class,
                () -> mechanic.execute(player, Map.of(), event));
    }

    @Test
    void appliesEffectToDamageVictim() {
        var player = BukkitMock.mockPlayer();
        var monster = mock(Monster.class);
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(monster);

        assertTrue(mechanic.execute(player, Map.of("effect", "minecraft:poison", "duration", 2.0), event));
        var captor = org.mockito.ArgumentCaptor.forClass(PotionEffect.class);
        verify(monster).addPotionEffect(captor.capture());
        assertEquals(40, captor.getValue().getDuration());
        assertEquals(0, captor.getValue().getAmplifier());
    }

    @Test
    void appliesEffectToRightClickedEntity() {
        var player = BukkitMock.mockPlayer();
        var monster = mock(Monster.class);
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(monster);

        assertTrue(mechanic.execute(player, Map.of("effect", "minecraft:poison"), event));
        verify(monster).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void appliesEffectToRaycastTargetOnRightClick() {
        var player = BukkitMock.mockPlayer();
        var monster = mock(Monster.class);
        when(player.getTargetEntity(4)).thenReturn(monster);

        assertTrue(mechanic.execute(player, Map.of("effect", "minecraft:poison"), BukkitMock.mockInteractEvent(player)));
        verify(monster).addPotionEffect(any(PotionEffect.class));
    }

    @Test
    void rightClickedPlayerIsNotAffected() {
        var player = BukkitMock.mockPlayer();
        var other = mock(Player.class);
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(other);

        assertFalse(mechanic.execute(player, Map.of("effect", "minecraft:poison"), event));
        verify(other, never()).addPotionEffect(any(PotionEffect.class));
    }
}
