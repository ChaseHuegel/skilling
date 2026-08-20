package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code target_unaware} state filter: a backstab only applies to a
 * hostile {@link Mob} that is not currently targeting the player, and fails
 * closed for non-mob victims.
 */
class TargetUnawareStateFilterTest {

    private StateFilterRegistry builtins() {
        StateFilterRegistry sf = new StateFilterRegistry();
        var loader = new io.github.chasehuegel.skilling.engine.tag.CustomTagLoader();
        Skilling.registerBuiltinStateFilters(sf, new TagResolver(loader), new EntityTagResolver(loader));
        return sf;
    }

    private static EntityDamageByEntityEvent hitOn(LivingEntity victim) {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        return event;
    }

    @Test
    void trueWhenMobTargetsNothing() {
        var player = mock(Player.class);
        var mob = mock(Mob.class);
        when(mob.getTarget()).thenReturn(null);
        var filter = builtins();
        assertTrue(filter.evaluate("target_unaware", player, hitOn(mob), ""));
    }

    @Test
    void trueWhenMobTargetsSomeoneElse() {
        var player = mock(Player.class);
        var other = mock(LivingEntity.class);
        var mob = mock(Mob.class);
        when(mob.getTarget()).thenReturn(other);
        var filter = builtins();
        assertTrue(filter.evaluate("target_unaware", player, hitOn(mob), ""));
    }

    @Test
    void falseWhenMobTargetsThePlayer() {
        var player = mock(Player.class);
        var mob = mock(Mob.class);
        when(mob.getTarget()).thenReturn(player);
        var filter = builtins();
        assertFalse(filter.evaluate("target_unaware", player, hitOn(mob), ""));
    }

    @Test
    void falseForNonMobVictim() {
        var player = mock(Player.class);
        var victim = mock(LivingEntity.class);
        var filter = builtins();
        assertFalse(filter.evaluate("target_unaware", player, hitOn(victim), ""));
    }
}
