package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillEventListenerDamagePriorityTest {

    private SkillEventListener newListener(ProfileManager profileManager) {
        return new SkillEventListener(
                mock(Skilling.class), mock(SkillManager.class), profileManager,
                mock(TagResolver.class), mock(RequirementEngine.class), mock(MechanicRegistry.class),
                mock(FeedbackDebouncer.class), mock(BossBarPool.class), mock(StateFilterRegistry.class));
    }

    @Test
    void damageTakenHandlerRunsAtLowestWithoutIgnoreCancelled() throws Exception {
        Method method = SkillEventListener.class.getMethod("onEntityDamageTaken", EntityDamageEvent.class);
        EventHandler annotation = method.getAnnotation(EventHandler.class);
        assertEquals(EventPriority.LOWEST, annotation.priority(),
                "dodge/block/cancel must run before other plugins observe the damage");
        assertFalse(annotation.ignoreCancelled(), "cancellation must propagate from the handler");
    }

    @Test
    void previouslyCancelledDamageDoesNotDispatchAbilities() {
        var profileManager = mock(ProfileManager.class);
        var listener = newListener(profileManager);

        var event = mock(EntityDamageEvent.class);
        when(event.isCancelled()).thenReturn(true);
        when(event.getEntity()).thenReturn(mock(Player.class));

        listener.onEntityDamageTaken(event);

        verify(profileManager, never()).getProfile(any());
    }

    @Test
    void skillingFireworkDamageDoesNotDispatchAbilities() {
        var profileManager = mock(ProfileManager.class);
        var listener = newListener(profileManager);

        var event = mock(EntityDamageByEntityEvent.class);
        var firework = mock(Firework.class);
        var pdc = mock(PersistentDataContainer.class);
        when(firework.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.has(Skilling.FIREWORK_KEY, PersistentDataType.BOOLEAN)).thenReturn(true);
        when(event.getDamager()).thenReturn(firework);
        when(event.getEntity()).thenReturn(mock(Player.class));

        listener.onEntityDamageTaken(event);

        verify(profileManager, never()).getProfile(any());
    }
}
