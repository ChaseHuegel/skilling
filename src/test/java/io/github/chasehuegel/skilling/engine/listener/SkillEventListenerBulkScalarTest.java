package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.XpBonusMechanic;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that bulk-operation triggers ({@code collect_xp}, {@code consume_item},
 * {@code furnace_extract}) scale XP rewards by the magnitude of the operation,
 * while non-bulk triggers keep the flat configured reward.
 */
class SkillEventListenerBulkScalarTest {

    @Test
    void collectXpScalarUsesExpChangeAmount() {
        PlayerExpChangeEvent event = mock(PlayerExpChangeEvent.class);
        when(event.getAmount()).thenReturn(3);
        assertEquals(3, SkillEventListener.resolveEventBulkScalar(event));
    }

    @Test
    void consumeItemScalarUsesStackSize() {
        PlayerItemConsumeEvent event = mock(PlayerItemConsumeEvent.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getAmount()).thenReturn(3);
        when(event.getItem()).thenReturn(item);
        assertEquals(3, SkillEventListener.resolveEventBulkScalar(event));
    }

    @Test
    void furnaceExtractScalarUsesExpToDrop() {
        FurnaceExtractEvent event = mock(FurnaceExtractEvent.class);
        when(event.getExpToDrop()).thenReturn(3);
        assertEquals(3, SkillEventListener.resolveEventBulkScalar(event));
    }

    @Test
    void nonBulkEventReturnsScalarOne() {
        Event event = mock(BlockBreakEvent.class);
        assertEquals(1, SkillEventListener.resolveEventBulkScalar(event));
    }

    @Test
    void zeroScalarReturnsZero() {
        PlayerExpChangeEvent event = mock(PlayerExpChangeEvent.class);
        when(event.getAmount()).thenReturn(0);
        assertEquals(0, SkillEventListener.resolveEventBulkScalar(event));
    }

    @Test
    void collectXpBulkOfThreeWithRewardTwoGrantsSix() {
        var player = BukkitMock.mockPlayer();
        UUID uuid = player.getUniqueId();
        assertEquals(6, SkillEventListener.computeXpGain(2, 3, 1.0, uuid));
    }

    @Test
    void consumeItemStackOfThreeWithRewardTwoGrantsSix() {
        var player = BukkitMock.mockPlayer();
        assertEquals(6, SkillEventListener.computeXpGain(2, 3, 1.0, player.getUniqueId()));
    }

    @Test
    void furnaceExtractThreeXpWithRewardTwoGrantsSix() {
        var player = BukkitMock.mockPlayer();
        assertEquals(6, SkillEventListener.computeXpGain(2, 3, 1.0, player.getUniqueId()));
    }

    @Test
    void nonBulkRewardGrantsFlatConfiguredAmount() {
        var player = BukkitMock.mockPlayer();
        assertEquals(2, SkillEventListener.computeXpGain(2, 1, 1.0, player.getUniqueId()));
    }

    @Test
    void globalModifierScalesBulkReward() {
        var player = BukkitMock.mockPlayer();
        assertEquals(9, SkillEventListener.computeXpGain(2, 3, 1.5, player.getUniqueId()));
    }

    @Test
    void xpBonusMultiplierScalesBulkReward() {
        var player = BukkitMock.mockPlayer();
        UUID uuid = player.getUniqueId();
        new XpBonusMechanic().execute(player, java.util.Map.of("multiplier", 2.0), mock(Event.class));
        assertEquals(12, SkillEventListener.computeXpGain(2, 3, 1.0, uuid));
    }

    @Test
    void zeroBulkScalarGrantsZeroAndDoesNotRoundUp() {
        var player = BukkitMock.mockPlayer();
        assertEquals(0, SkillEventListener.computeXpGain(2, 0, 1.0, player.getUniqueId()));
    }

    @Test
    void negativeGainReturnsZero() {
        var player = BukkitMock.mockPlayer();
        assertEquals(0, SkillEventListener.computeXpGain(2, -1, 1.0, player.getUniqueId()));
    }
}
