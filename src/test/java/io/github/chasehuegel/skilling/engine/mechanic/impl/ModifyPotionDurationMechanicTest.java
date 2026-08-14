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
 * Verifies that {@link ModifyPotionDurationMechanic} scales the base potion
 * type's effects — the vanilla path a normal brewed potion (e.g. Swiftness)
 * stores its effect under — not only custom effects.
 */
class ModifyPotionDurationMechanicTest {

    @Test
    void scalesBasePotionTypeEffects() {
        var mechanic = new ModifyPotionDurationMechanic();
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

        assertTrue(mechanic.execute(mock(Player.class), Map.of("multiplier", 2.0), event));

        ArgumentCaptor<PotionEffect> captor = ArgumentCaptor.forClass(PotionEffect.class);
        verify(meta).addCustomEffect(captor.capture(), eq(true));
        // 3600 ticks (3:00) x 2.0 = 7200 ticks (6:00)
        assertEquals(7200, captor.getValue().getDuration());
        verify(item).setItemMeta(meta);
    }

    @Test
    void ignoresNonPotionItems() {
        var mechanic = new ModifyPotionDurationMechanic();
        var item = mock(ItemStack.class);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(mock(org.bukkit.inventory.meta.ItemMeta.class));

        var inventory = mock(BrewerInventory.class);
        when(inventory.getContents()).thenReturn(new ItemStack[]{item});
        var event = mock(BrewEvent.class);
        when(event.getContents()).thenReturn(inventory);

        assertFalse(mechanic.execute(mock(Player.class), Map.of("multiplier", 2.0), event));
        verify(item, never()).setItemMeta(any());
    }
}
