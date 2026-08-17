package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:villager_xp}: a trade adds the configured experience to
 * the traded {@link Villager}, no-ops on a non-villager merchant, and rejects
 * a non-trade event or non-positive amount.
 */
class VillagerXpMechanicTest {

    private VillagerXpMechanic mechanic = new VillagerXpMechanic();

    private PlayerTradeEvent tradeWith(org.bukkit.entity.AbstractVillager villager) {
        var event = mock(PlayerTradeEvent.class);
        when(event.getVillager()).thenReturn(villager);
        return event;
    }

    @Test
    void returnsFalseForNonTradeEvent() {
        assertFalse(mechanic.execute(mock(Player.class), Map.of("amount", 5.0),
                mock(org.bukkit.event.entity.EntityDamageEvent.class)));
    }

    @Test
    void returnsFalseWithNonPositiveAmount() {
        assertFalse(mechanic.execute(mock(Player.class), Map.of("amount", 0.0),
                mock(PlayerTradeEvent.class)));
    }

    @Test
    void addsExperienceToTradedVillager() {
        var villager = mock(Villager.class);
        when(villager.getVillagerExperience()).thenReturn(20);
        var event = tradeWith(villager);

        assertTrue(mechanic.execute(mock(Player.class), Map.of("amount", 15.0), event));
        verify(villager).setVillagerExperience(35);
    }

    @Test
    void roundsFractionalAmount() {
        var villager = mock(Villager.class);
        when(villager.getVillagerExperience()).thenReturn(0);
        var event = tradeWith(villager);

        assertTrue(mechanic.execute(mock(Player.class), Map.of("amount", 5.6), event));
        verify(villager).setVillagerExperience(6);
    }

    @Test
    void nonVillagerMerchantIsNoOp() {
        var trader = mock(WanderingTrader.class);
        var event = tradeWith(trader);
        assertFalse(mechanic.execute(mock(Player.class), Map.of("amount", 5.0), event),
                "a merchant without villager experience cannot tier up");
    }
}
