package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AuraTargetFilterTest {

    @Test
    void alliesExcludeHostileMobs() {
        assertFalse(AuraTargetFilter.accepts("allies", mock(Monster.class)),
                "hostile mobs must never be buffed by the default allies filter");
        assertTrue(AuraTargetFilter.accepts("allies", mock(LivingEntity.class)));
        assertTrue(AuraTargetFilter.accepts("allies", mock(Player.class)));
    }

    @Test
    void hostilesSelectOnlyEnemies() {
        assertTrue(AuraTargetFilter.accepts("hostiles", mock(Monster.class)));
        assertFalse(AuraTargetFilter.accepts("hostiles", mock(LivingEntity.class)));
        assertFalse(AuraTargetFilter.accepts("hostiles", mock(Player.class)));
    }

    @Test
    void allAcceptsEverything() {
        assertTrue(AuraTargetFilter.accepts("all", mock(Monster.class)));
        assertTrue(AuraTargetFilter.accepts("all", mock(Player.class)));
    }

    @Test
    void unknownValueFallsBackToAllies() {
        assertFalse(AuraTargetFilter.accepts("alleis", mock(Monster.class)),
                "a typo must fall back to the safe allies default");
        assertTrue(AuraTargetFilter.accepts("alleis", mock(LivingEntity.class)));
    }
}
