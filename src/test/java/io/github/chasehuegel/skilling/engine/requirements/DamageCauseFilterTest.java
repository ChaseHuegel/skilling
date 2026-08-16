package io.github.chasehuegel.skilling.engine.requirements;

import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code cause} state-filter keyword mapping: burn covers fire,
 * fire ticks, and lava; fire/lava/drowning/suffocation/cactus/starvation match
 * their single causes; anything else — including a non-damage event — fails
 * closed.
 */
class DamageCauseFilterTest {

    private static EntityDamageEvent event(DamageCause cause) {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getCause()).thenReturn(cause);
        return event;
    }

    @Test
    void burnMatchesFireFireTickAndLava() {
        assertTrue(DamageCauseFilter.evaluate(event(DamageCause.FIRE), "burn"));
        assertTrue(DamageCauseFilter.evaluate(event(DamageCause.FIRE_TICK), "burn"));
        assertTrue(DamageCauseFilter.evaluate(event(DamageCause.LAVA), "burn"));
        assertFalse(DamageCauseFilter.evaluate(event(DamageCause.FALL), "burn"));
    }

    @Test
    void singleCauseKeywordsMatchTheirCauseOnly() {
        assertTrue(DamageCauseFilter.evaluate(event(DamageCause.FIRE), "fire"));
        assertFalse(DamageCauseFilter.evaluate(event(DamageCause.FIRE_TICK), "fire"));
        assertTrue(DamageCauseFilter.evaluate(event(DamageCause.LAVA), "lava"));
        assertTrue(DamageCauseFilter.evaluate(event(DamageCause.DROWNING), "drowning"));
        assertTrue(DamageCauseFilter.evaluate(event(DamageCause.SUFFOCATION), "suffocation"));
        assertTrue(DamageCauseFilter.evaluate(event(DamageCause.CONTACT), "cactus"));
        assertTrue(DamageCauseFilter.evaluate(event(DamageCause.STARVATION), "starvation"));
        assertFalse(DamageCauseFilter.evaluate(event(DamageCause.FALL), "drowning"));
    }

    @Test
    void unknownKeywordFailsClosed() {
        assertFalse(DamageCauseFilter.evaluate(event(DamageCause.FIRE), "explosion"));
        assertFalse(DamageCauseFilter.evaluate(event(DamageCause.FIRE), null));
    }

    @Test
    void nonDamageEventFailsClosed() {
        org.bukkit.event.Event plain = mock(org.bukkit.event.Event.class);
        assertFalse(DamageCauseFilter.evaluate(plain, "burn"));
    }

    @Test
    void valueValidationAcceptsOnlySupportedKeywords() {
        assertTrue(DamageCauseFilter.isValidValue("burn"));
        assertTrue(DamageCauseFilter.isValidValue("drowning"));
        assertFalse(DamageCauseFilter.isValidValue("explosion"));
        assertFalse(DamageCauseFilter.isValidValue(null));
    }
}