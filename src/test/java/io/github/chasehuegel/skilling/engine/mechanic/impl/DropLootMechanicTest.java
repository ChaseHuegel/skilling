package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:drop_loot}: resolves a target from the triggering event,
 * rolls the referenced loot table, and drops the result naturally at the
 * target. A failed chance roll counts as an activation attempt without dropping.
 */
class DropLootMechanicTest {

    @AfterEach
    void tearDown() {
        DropLootMechanic.setRandomSource(() -> ThreadLocalRandom.current().nextDouble(100));
    }

    private static PlayerInteractEntityEvent clickOn(LivingEntity target) {
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(target);
        return event;
    }

    private static LivingEntity targetIn(World world) {
        var target = mock(LivingEntity.class);
        when(target.getLocation()).thenReturn(new Location(world, 10, 64, 20));
        return target;
    }

    @Test
    void returnsFalseWithoutTable() {
        assertFalse(new DropLootMechanic().execute(mock(Player.class), Map.of(),
                clickOn(mock(LivingEntity.class))));
    }

    @Test
    void returnsFalseWhenTableMissing() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getLootTable(any())).thenReturn(null);
            assertFalse(new DropLootMechanic().execute(mock(Player.class),
                    Map.of("table", "stealth:pickpocket/pocket"), clickOn(mock(LivingEntity.class))));
        }
    }

    @Test
    void returnsFalseWithoutTarget() {
        assertFalse(new DropLootMechanic().execute(mock(Player.class),
                Map.of("table", "stealth:pickpocket/pocket"), mock(Event.class)));
    }

    @Test
    void successfulRollDropsLootAtTarget() {
        var world = mock(World.class);
        var target = targetIn(world);
        var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.EMERALD);

        DropLootMechanic.setRandomSource(() -> 0.0); // always succeeds
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            var loot = mock(LootTable.class);
            when(loot.populateLoot(any(), any(LootContext.class))).thenReturn(List.of(item));
            when(Bukkit.getLootTable(any())).thenReturn(loot);

            var mechanic = new DropLootMechanic();
            assertTrue(mechanic.execute(mock(Player.class),
                    Map.of("table", "stealth:pickpocket/ender", "chance", 50.0), clickOn(target)));
            assertTrue(mechanic.didProc(), "a landed roll must report a proc");
            verify(world).dropItemNaturally(any(Location.class), eq(item));
        }
    }

    @Test
    void failedRollCountsAsActivationWithoutDropping() {
        var world = mock(World.class);
        var target = targetIn(world);

        DropLootMechanic.setRandomSource(() -> 99.0); // always fails
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getLootTable(any())).thenReturn(mock(LootTable.class));

            var mechanic = new DropLootMechanic();
            assertTrue(mechanic.execute(mock(Player.class),
                    Map.of("table", "stealth:pickpocket/ender", "chance", 50.0), clickOn(target)),
                    "a failed roll still counts as an activation attempt");
            assertFalse(mechanic.didProc(), "a missed roll must not report a proc");
            verify(world, never()).dropItemNaturally(any(Location.class), any(ItemStack.class));
        }
    }
}
