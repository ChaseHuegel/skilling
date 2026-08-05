package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code scaling: damage} XP sources: {@code resolveEventDamage} reads
 * the raw base damage of {@link EntityDamageEvent} (incoming and outgoing), and
 * {@code resolveSourceScalar} wires damage-scaled sources to that value while
 * flat sources keep the bulk-operation scalar.
 */
class SkillEventListenerDamageScalarTest {

    private static SkillDefinition.XpSource source(SkillDefinition.XpScaling scaling) {
        return new SkillDefinition.XpSource("fall_damage", List.of(),
                new ConstantEvaluator(1.0), scaling);
    }

    @Test
    void resolveEventDamageReturnsRawBaseDamage() {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getDamage()).thenReturn(10.0);
        assertEquals(10.0, SkillEventListener.resolveEventDamage(event),
                "a 5-heart fall must scale by its raw base damage (10 half-hearts)");
    }

    @Test
    void resolveEventDamageAppliesToOutgoingDamage() {
        // EntityDamageByEntityEvent extends EntityDamageEvent, so outgoing
        // entity_damage sources scale off the same getDamage() value.
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamage()).thenReturn(7.0);
        assertEquals(7.0, SkillEventListener.resolveEventDamage(event));
    }

    @Test
    void resolveEventDamageIsZeroForNonDamageEvent() {
        assertEquals(0.0, SkillEventListener.resolveEventDamage(mock(BlockBreakEvent.class)));
    }

    @Test
    void resolveEventDamageClampsNegativeDamageToZero() {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getDamage()).thenReturn(-1.0);
        assertEquals(0.0, SkillEventListener.resolveEventDamage(event),
                "a negative scalar must never produce negative XP");
    }

    @Test
    void flatSourceKeepsBulkScalar() {
        PlayerExpChangeEvent event = mock(PlayerExpChangeEvent.class);
        when(event.getAmount()).thenReturn(3);
        assertEquals(3.0, SkillEventListener.resolveSourceScalar(source(SkillDefinition.XpScaling.NONE), event));
    }

    @Test
    void flatSourceDefaultsToScalarOneOnPlainEvents() {
        assertEquals(1.0, SkillEventListener.resolveSourceScalar(
                source(SkillDefinition.XpScaling.NONE), mock(BlockBreakEvent.class)));
    }

    @Test
    void damageSourceUsesEventDamage() {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getDamage()).thenReturn(8.0);
        assertEquals(8.0, SkillEventListener.resolveSourceScalar(source(SkillDefinition.XpScaling.DAMAGE), event));
    }
}
