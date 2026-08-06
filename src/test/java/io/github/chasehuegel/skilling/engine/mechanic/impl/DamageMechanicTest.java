package io.github.chasehuegel.skilling.engine.mechanic.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code core:damage} deals damage through the normal damage pipeline
 * to the event's target, combines {@code damage} and {@code percent}, and never
 * damages another player.
 */
class DamageMechanicTest {

    private final DamageMechanic mechanic = new DamageMechanic();

    private Monster monster(double maxHp) {
        Monster monster = mock(Monster.class);
        if (maxHp > 0) {
            AttributeInstance attr = mock(AttributeInstance.class);
            when(attr.getValue()).thenReturn(maxHp);
            when(monster.getAttribute(Attribute.MAX_HEALTH)).thenReturn(attr);
        }
        return monster;
    }

    private EntityDamageByEntityEvent attack(org.bukkit.entity.LivingEntity victim, org.bukkit.entity.Entity attacker) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(attacker);
        return event;
    }

    @Test
    void entityDamageDealsHeartsDamageToVictim() {
        Player player = mock(Player.class);
        Monster monster = monster(-1);
        assertTrue(mechanic.execute(player, Map.of("damage", 6.0), attack(monster, player)));
        verify(monster).damage(6.0, player);
    }

    @Test
    void percentDealsFractionOfTargetMaxHealth() {
        Player player = mock(Player.class);
        Monster monster = monster(100.0);
        assertTrue(mechanic.execute(player, Map.of("percent", 0.1), attack(monster, player)));
        verify(monster).damage(10.0, player);
    }

    @Test
    void damageAndPercentCombine() {
        Player player = mock(Player.class);
        Monster monster = monster(100.0);
        assertTrue(mechanic.execute(player, Map.of("damage", 3.0, "percent", 0.1), attack(monster, player)));
        verify(monster).damage(13.0, player);
    }

    @Test
    void zeroAmountIsNoOp() {
        Player player = mock(Player.class);
        Monster monster = monster(-1);
        assertFalse(mechanic.execute(player, Map.of("damage", 0.0, "percent", 0.0), attack(monster, player)));
        verify(monster, never()).damage(anyDouble(), any(org.bukkit.entity.Entity.class));
    }

    @Test
    void playerVictimIsNeverDamaged() {
        Player player = mock(Player.class);
        Player victim = mock(Player.class);
        assertFalse(mechanic.execute(player, Map.of("damage", 6.0), attack(victim, player)));
        verify(victim, never()).damage(anyDouble(), any(org.bukkit.entity.Entity.class));
    }

    @Test
    void casterIsNotDamagedWhenVictimOfAnotherAttacker() {
        Player player = mock(Player.class);
        Monster attacker = monster(-1);
        assertFalse(mechanic.execute(player, Map.of("damage", 6.0), attack(player, attacker)));
        verify(player, never()).damage(anyDouble(), any(org.bukkit.entity.Entity.class));
    }

    @Test
    void rightClickRaycastDamagesLookedAtEntity() {
        Player player = mock(Player.class);
        Monster monster = monster(-1);
        when(player.getTargetEntity((int) OffhandStrikeMechanic.MAX_REACH)).thenReturn(monster);

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);

        assertTrue(mechanic.execute(player, Map.of("damage", 4.0), event));
        verify(monster).damage(4.0, player);
    }

    @Test
    void rightClickEntityDamagesClickedEntity() {
        Player player = mock(Player.class);
        Monster monster = monster(-1);

        PlayerInteractEntityEvent event = mock(PlayerInteractEntityEvent.class);
        when(event.getRightClicked()).thenReturn(monster);

        assertTrue(mechanic.execute(player, Map.of("damage", 2.0), event));
        verify(monster).damage(2.0, player);
    }
}
