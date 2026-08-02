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
 * Verifies that bulk-operation triggers ({@code collect_xp}, {@code craft_item},
 * {@code furnace_extract}) scale XP rewards by the magnitude of the operation,
 * while {@code consume_item} and other non-bulk triggers keep the flat
 * configured reward.
 */
class SkillEventListenerBulkScalarTest {

    @Test
    void collectXpScalarUsesExpChangeAmount() {
        PlayerExpChangeEvent event = mock(PlayerExpChangeEvent.class);
        when(event.getAmount()).thenReturn(3);
        assertEquals(3, SkillEventListener.resolveEventBulkScalar(event));
    }

    @Test
    void consumeItemScalarIsAlwaysOne() {
        // Vanilla consumption removes exactly one item from the stack per event,
        // so a consume must never scale by the stack size.
        PlayerItemConsumeEvent event = mock(PlayerItemConsumeEvent.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getAmount()).thenReturn(3);
        when(event.getItem()).thenReturn(item);
        assertEquals(1, SkillEventListener.resolveEventBulkScalar(event));
    }

    @Test
    void consumeOfStackOfSixtyFourGrantsScalarOne() {
        PlayerItemConsumeEvent event = mock(PlayerItemConsumeEvent.class);
        ItemStack item = mock(ItemStack.class);
        when(item.getAmount()).thenReturn(64);
        when(event.getItem()).thenReturn(item);
        assertEquals(1, SkillEventListener.resolveEventBulkScalar(event),
                "a stack of 64 must not grant 64x the configured reward");
    }

    @Test
    void craftScalarUsesResultStackCount() {
        var event = mock(org.bukkit.event.inventory.CraftItemEvent.class);
        ItemStack result = mock(ItemStack.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.getAmount()).thenReturn(4);
        when(event.getCurrentItem()).thenReturn(result);
        assertEquals(4, SkillEventListener.resolveEventBulkScalar(event));
    }

    @Test
    void shiftClickCraftScalarUsesBatchTotal() {
        var event = mock(org.bukkit.event.inventory.CraftItemEvent.class);
        ItemStack result = mock(ItemStack.class);
        when(result.isEmpty()).thenReturn(false);
        when(result.getAmount()).thenReturn(8);
        when(event.getCurrentItem()).thenReturn(result);
        assertEquals(8, SkillEventListener.resolveEventBulkScalar(event));
    }

    @Test
    void craftScalarFallsBackToRecipeResult() {
        var event = mock(org.bukkit.event.inventory.CraftItemEvent.class);
        when(event.getCurrentItem()).thenReturn(null);
        var recipe = mock(org.bukkit.inventory.Recipe.class);
        ItemStack recipeResult = mock(ItemStack.class);
        when(recipeResult.getAmount()).thenReturn(2);
        when(recipe.getResult()).thenReturn(recipeResult);
        when(event.getRecipe()).thenReturn(recipe);
        assertEquals(2, SkillEventListener.resolveEventBulkScalar(event));
    }

    @Test
    void furnaceExtractScalarUsesItemCount() {
        FurnaceExtractEvent event = mock(FurnaceExtractEvent.class);
        when(event.getItemAmount()).thenReturn(4);
        assertEquals(4, SkillEventListener.resolveEventBulkScalar(event),
                "4 extracted ingots must scale XP by 4, not by the XP orbs dropped");
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
    void consumeItemGrantsPerActionRewardNotStackScaled() {
        var player = BukkitMock.mockPlayer();
        assertEquals(2, SkillEventListener.computeXpGain(2, 1, 1.0, player.getUniqueId()));
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
