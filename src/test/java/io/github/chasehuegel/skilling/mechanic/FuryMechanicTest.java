package io.github.chasehuegel.skilling.mechanic;

import io.github.chasehuegel.skilling.BukkitMock;
import io.github.chasehuegel.skilling.engine.mechanic.impl.FuryMechanic;
import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FuryMechanicTest {

    private static final long T0 = 1_000_000_000L;

    private final Player player = BukkitMock.mockPlayer();

    @AfterEach
    void reset() {
        FuryMechanic.setClockOverrideNanos(0);
        FuryMechanic.clearAll();
    }

    private EntityDamageByEntityEvent mockHit(double damage) {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);
        when(event.getEntity()).thenReturn(mock(LivingEntity.class));
        when(event.getDamage()).thenReturn(damage);
        return event;
    }

    @Test
    void returnsFalseForNonDamageEvent() {
        var mechanic = new FuryMechanic();
        assertFalse(mechanic.execute(player, Map.of("multiplier_step", 0.1), BukkitMock.mockBlockBreakEvent()));
    }

    @Test
    void rampsUpWithEachHit() {
        FuryMechanic.setClockOverrideNanos(T0);
        var mechanic = new FuryMechanic();
        // First hit: 1 stack -> 1.1x
        var hit1 = mockHit(10.0);
        assertTrue(mechanic.execute(player, Map.of("multiplier_step", 0.1), hit1));
        verify(hit1).setDamage(11.0);
        assertEquals(1, FuryMechanic.stacks(player.getUniqueId()));

        // Second hit: 2 stacks -> 1.2x
        var hit2 = mockHit(10.0);
        assertTrue(mechanic.execute(player, Map.of("multiplier_step", 0.1), hit2));
        verify(hit2).setDamage(12.0);
        assertEquals(2, FuryMechanic.stacks(player.getUniqueId()));
    }

    @Test
    void capsAtMaxStacks() {
        FuryMechanic.setClockOverrideNanos(T0);
        var mechanic = new FuryMechanic();
        Map<String, Object> params = Map.of("multiplier_step", 0.1, "max_stacks", 5);
        double expectedDamage = 0;
        for (int i = 0; i < 7; i++) {
            var hit = mockHit(10.0);
            mechanic.execute(player, params, hit);
            // stacks = min(i+1, 5); multiplier = 1 + stacks*0.1
            int stacks = Math.min(i + 1, 5);
            double mult = 1.0 + stacks * 0.1;
            expectedDamage = 10.0 * mult;
            verify(hit).setDamage(10.0 * mult);
        }
        assertEquals(5, FuryMechanic.stacks(player.getUniqueId()));
        assertEquals(15.0, expectedDamage);
    }

    @Test
    void decaysAfterWindowLapse() {
        FuryMechanic.setClockOverrideNanos(T0);
        var mechanic = new FuryMechanic();
        var hit1 = mockHit(10.0);
        mechanic.execute(player, Map.of("multiplier_step", 0.1, "window", 4.0), hit1);
        verify(hit1).setDamage(11.0);
        assertEquals(1, FuryMechanic.stacks(player.getUniqueId()));

        // Advance beyond the 4s window; the next hit must reset to 1 stack.
        FuryMechanic.setClockOverrideNanos(T0 + 5_000_000_000L);
        var hit2 = mockHit(10.0);
        mechanic.execute(player, Map.of("multiplier_step", 0.1, "window", 4.0), hit2);
        verify(hit2).setDamage(11.0);
        assertEquals(1, FuryMechanic.stacks(player.getUniqueId()));

        // A stale ramp left between hits is also reported as zero until rebuilt.
        FuryMechanic.setClockOverrideNanos(T0 + 10_000_000_000L);
        assertEquals(0, FuryMechanic.stacks(player.getUniqueId()));
    }
}