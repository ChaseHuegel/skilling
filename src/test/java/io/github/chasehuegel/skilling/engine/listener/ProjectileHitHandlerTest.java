package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.mechanic.impl.ProjectileMechanic;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectileHitHandlerTest {

    private final SkillEventListener listener = new SkillEventListener(
            mock(Skilling.class), mock(SkillManager.class), mock(ProfileManager.class),
            mock(TagResolver.class), mock(RequirementEngine.class), mock(MechanicRegistry.class),
            mock(FeedbackDebouncer.class), mock(BossBarPool.class), mock(StateFilterRegistry.class));

    @Test
    void hitOnHangingEntityDoesNotThrowAndDispatchesNothing() {
        // An item frame is a Hanging, not a LivingEntity; the handler must return
        // without casting, otherwise a ClassCastException breaks the event chain.
        var frame = mock(ItemFrame.class);
        var event = mock(ProjectileHitEvent.class);
        when(event.getHitEntity()).thenReturn(frame);
        var projectile = mock(Snowball.class);
        when(event.getEntity()).thenReturn(projectile);
        var pdc = mock(PersistentDataContainer.class);
        when(projectile.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.has(ProjectileMechanic.DAMAGE_KEY, PersistentDataType.DOUBLE)).thenReturn(true);
        when(pdc.get(ProjectileMechanic.DAMAGE_KEY, PersistentDataType.DOUBLE)).thenReturn(5.0);

        assertDoesNotThrow(() -> listener.onProjectileHit(event));
        verify(projectile, never()).getShooter();
    }

    @Test
    void hitOnLivingEntityStillDispatchesDamage() {
        var target = mock(LivingEntity.class);
        var shooter = mock(Player.class);
        var event = mock(ProjectileHitEvent.class);
        when(event.getHitEntity()).thenReturn(target);
        var projectile = mock(Snowball.class);
        when(event.getEntity()).thenReturn(projectile);
        var pdc = mock(PersistentDataContainer.class);
        when(projectile.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.has(ProjectileMechanic.DAMAGE_KEY, PersistentDataType.DOUBLE)).thenReturn(true);
        when(pdc.get(ProjectileMechanic.DAMAGE_KEY, PersistentDataType.DOUBLE)).thenReturn(5.0);
        when(projectile.getShooter()).thenReturn(shooter);

        listener.onProjectileHit(event);

        verify(target).damage(5.0, shooter);
    }
}
