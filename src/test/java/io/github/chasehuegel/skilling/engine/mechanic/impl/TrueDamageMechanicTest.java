package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:true_damage} applies damage directly to the target's
 * health (bypassing mitigations), combines {@code damage} and {@code percent},
 * never damages another player, and never sets health below zero.
 */
class TrueDamageMechanicTest {

    private final TrueDamageMechanic mechanic = new TrueDamageMechanic();

    private Monster monster(double maxHp, double currentHp) {
        Monster monster = mock(Monster.class);
        if (maxHp > 0) {
            AttributeInstance attr = mock(AttributeInstance.class);
            when(attr.getValue()).thenReturn(maxHp);
            when(monster.getAttribute(Attribute.MAX_HEALTH)).thenReturn(attr);
        }
        when(monster.getHealth()).thenReturn(currentHp);
        return monster;
    }

    private EntityDamageByEntityEvent attack(org.bukkit.entity.LivingEntity victim, Player attacker) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(attacker);
        return event;
    }

    @Test
    void damageAppliedDirectlyToHealth() {
        Player player = mock(Player.class);
        Monster monster = monster(-1, 20.0);
        assertTrue(mechanic.execute(player, Map.of("damage", 2.0), attack(monster, player)));
        verify(monster).setHealth(18.0);
    }

    @Test
    void percentDamageAppliedDirectlyToHealth() {
        Player player = mock(Player.class);
        Monster monster = monster(100.0, 50.0);
        assertTrue(mechanic.execute(player, Map.of("percent", 0.1), attack(monster, player)));
        verify(monster).setHealth(40.0);
    }

    @Test
    void zeroAmountIsNoOp() {
        Player player = mock(Player.class);
        Monster monster = monster(-1, 20.0);
        assertFalse(mechanic.execute(player, Map.of("damage", 0.0, "percent", 0.0), attack(monster, player)));
        verify(monster, never()).setHealth(anyDouble());
    }

    @Test
    void playerVictimIsNeverDamaged() {
        Player player = mock(Player.class);
        Player victim = mock(Player.class);
        assertFalse(mechanic.execute(player, Map.of("damage", 6.0), attack(victim, player)));
        verify(victim, never()).setHealth(anyDouble());
    }

    @Test
    void invulnerableTargetIsNoOp() {
        Player player = mock(Player.class);
        Monster monster = monster(-1, 20.0);
        when(monster.isInvulnerable()).thenReturn(true);
        assertFalse(mechanic.execute(player, Map.of("damage", 6.0), attack(monster, player)));
        verify(monster, never()).setHealth(anyDouble());
    }

    @Test
    void healthNeverDropsBelowZero() {
        Player player = mock(Player.class);
        Monster monster = monster(-1, 2.0);
        assertTrue(mechanic.execute(player, Map.of("damage", 10.0), attack(monster, player)));
        verify(monster).setHealth(0.0);
    }
}
