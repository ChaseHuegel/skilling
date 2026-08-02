package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectileReturnMechanicTest {

    private ProjectileHitEvent hitEvent(Object projectile, org.bukkit.projectiles.ProjectileSource shooter) {
        var event = mock(ProjectileHitEvent.class);
        when(event.getEntity()).thenReturn((org.bukkit.entity.Projectile) projectile);
        when(((org.bukkit.entity.Projectile) projectile).getShooter()).thenReturn(shooter);
        return event;
    }

    private Player throwingPlayer() {
        var player = mock(Player.class);
        var world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(mock(Location.class));
        return player;
    }

    @Test
    void returnsFalseForNonProjectileHitEvent() {
        var player = throwingPlayer();
        assertFalse(new ProjectileReturnMechanic().execute(player, Map.of("chance", 100.0),
                mock(org.bukkit.event.block.BlockBreakEvent.class)));
    }

    @Test
    void returnsFalseWithChanceZero() {
        var player = throwingPlayer();
        var trident = mock(Trident.class);
        assertFalse(new ProjectileReturnMechanic().execute(player, Map.of(),
                hitEvent(trident, player)));
    }

    @Test
    void returnsFalseWhenShooterIsNotThePlayer() {
        var player = throwingPlayer();
        var trident = mock(Trident.class);
        var other = mock(org.bukkit.entity.Zombie.class);
        assertFalse(new ProjectileReturnMechanic().execute(player, Map.of("chance", 100.0),
                hitEvent(trident, other)));
    }

    @Test
    void enchantedTridentReturnsItsItemAndIsRemoved() {
        var mechanic = new ProjectileReturnMechanic();
        var player = throwingPlayer();
        var trident = mock(Trident.class);
        var tridentItem = mock(ItemStack.class);
        when(tridentItem.isEmpty()).thenReturn(false);
        var returned = mock(ItemStack.class);
        when(tridentItem.clone()).thenReturn(returned);
        when(trident.getItemStack()).thenReturn(tridentItem);

        assertTrue(mechanic.execute(player, Map.of("chance", 100.0), hitEvent(trident, player)));

        verify(trident).getItemStack();
        verify(trident).remove();
        verify(player.getWorld()).dropItemNaturally(any(Location.class), eq(returned));
    }

    @Test
    void arrowTypesAreReturnedAndRemoved() {
        var mechanic = new ProjectileReturnMechanic();
        var player = throwingPlayer();

        var arrow = mock(Arrow.class);
        var arrowItem = mock(ItemStack.class);
        when(arrowItem.isEmpty()).thenReturn(false);
        var returnedArrow = mock(ItemStack.class);
        when(arrowItem.clone()).thenReturn(returnedArrow);
        when(arrow.getItemStack()).thenReturn(arrowItem);

        assertTrue(mechanic.execute(player, Map.of("chance", 100.0), hitEvent(arrow, player)));
        verify(arrow).remove();
        verify(player.getWorld()).dropItemNaturally(any(Location.class), eq(returnedArrow));

        var spectral = mock(SpectralArrow.class);
        var spectralItem = mock(ItemStack.class);
        when(spectralItem.isEmpty()).thenReturn(false);
        var returnedSpectral = mock(ItemStack.class);
        when(spectralItem.clone()).thenReturn(returnedSpectral);
        when(spectral.getItemStack()).thenReturn(spectralItem);

        assertTrue(mechanic.execute(player, Map.of("chance", 100.0), hitEvent(spectral, player)));
        verify(spectral).remove();
        verify(player.getWorld()).dropItemNaturally(any(Location.class), eq(returnedSpectral));
    }

    @Test
    void tippedArrowPreservesItsEffectsViaItemStack() {
        var mechanic = new ProjectileReturnMechanic();
        var player = throwingPlayer();
        var arrow = mock(Arrow.class);
        // The tipped arrow's item stack carries the potion effects; it must be
        // returned verbatim (cloned), not replaced by a fresh plain arrow.
        var tippedItem = mock(ItemStack.class);
        when(tippedItem.isEmpty()).thenReturn(false);
        var returned = mock(ItemStack.class);
        when(tippedItem.clone()).thenReturn(returned);
        when(arrow.getItemStack()).thenReturn(tippedItem);

        assertTrue(mechanic.execute(player, Map.of("chance", 100.0), hitEvent(arrow, player)));
        verify(player.getWorld()).dropItemNaturally(any(Location.class), eq(returned));
    }

    @Test
    void snowballIsNotRemovedAndUsesFreshMaterial() {
        var snowball = mock(Snowball.class);
        when(snowball.getType()).thenReturn(EntityType.SNOWBALL);

        assertFalse(ProjectileReturnMechanic.isPickableItem(snowball),
                "snowballs vanish on impact and must not be removed");
        assertEquals(Material.SNOWBALL, ProjectileReturnMechanic.freshMaterial(snowball));
    }

    @Test
    void unknownProjectileTypeResolvesNoFreshMaterial() {
        var projectile = mock(org.bukkit.entity.Projectile.class);
        when(projectile.getType()).thenReturn(EntityType.FIREBALL);
        assertNull(ProjectileReturnMechanic.freshMaterial(projectile));
    }
}
