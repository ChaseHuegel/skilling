package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link ModifyPotionAmplifierMechanic} raises each brewed effect's
 * amplifier while preserving its duration, and no-ops on a zero amplifier.
 */
class ModifyPotionAmplifierMechanicTest {

    @Test
    void addsAmplifierToBasePotionTypeEffects() {
        var mechanic = new ModifyPotionAmplifierMechanic();
        var type = mock(PotionEffectType.class);
        var baseType = mock(PotionType.class);
        var meta = mock(PotionMeta.class);
        var item = mock(ItemStack.class);
        var baseEffect = new PotionEffect(type, 3600, 0);

        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.hasBasePotionType()).thenReturn(true);
        when(meta.getBasePotionType()).thenReturn(baseType);
        when(baseType.getPotionEffects()).thenReturn(List.of(baseEffect));

        var inventory = mock(BrewerInventory.class);
        when(inventory.getContents()).thenReturn(new ItemStack[]{item});
        var event = mock(BrewEvent.class);
        when(event.getContents()).thenReturn(inventory);

        assertTrue(mechanic.execute(mock(Player.class), Map.of("amplifier", 1.0), event));

        ArgumentCaptor<PotionEffect> captor = ArgumentCaptor.forClass(PotionEffect.class);
        verify(meta).addCustomEffect(captor.capture(), eq(true));
        assertEquals(1, captor.getValue().getAmplifier());
        assertEquals(3600, captor.getValue().getDuration());
        verify(item).setItemMeta(meta);
    }

    @Test
    void zeroAmplifierIsNoOp() {
        var mechanic = new ModifyPotionAmplifierMechanic();
        var item = mock(ItemStack.class);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(mock(PotionMeta.class));

        var inventory = mock(BrewerInventory.class);
        when(inventory.getContents()).thenReturn(new ItemStack[]{item});
        var event = mock(BrewEvent.class);
        when(event.getContents()).thenReturn(inventory);

        assertFalse(mechanic.execute(mock(Player.class), Map.of("amplifier", 0.0), event));
        verify(item, never()).setItemMeta(any());
    }
}
