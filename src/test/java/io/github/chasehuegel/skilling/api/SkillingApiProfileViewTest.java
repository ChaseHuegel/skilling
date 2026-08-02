package io.github.chasehuegel.skilling.api;

import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfileView;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.ui.SkillMenuBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillingApiProfileViewTest {

    private SkillingAPI newApi(ProfileManager profileManager) {
        return new SkillingAPI(
                new Registries(new MechanicRegistry(), new TriggerRegistry(), new EvaluatorRegistry()),
                profileManager,
                mock(SkillManager.class),
                mock(SkillMenuBuilder.class),
                mock(RequirementEngine.class),
                mock(FeedbackDebouncer.class),
                mock(BossBarPool.class));
    }

    @Test
    void getProfileReturnsReadOnlyView() {
        UUID uuid = UUID.randomUUID();
        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        profileManager.loadProfile(uuid).join();

        // Compile-time contract: the API hands out the read-only view type,
        // never the mutable engine profile.
        PlayerProfileView view = newApi(profileManager).getProfile(uuid);

        assertNotNull(view);
        assertEquals(uuid, view.getPlayerId());
        assertEquals(0L, view.getXp("mining"));
    }

    @Test
    void viewContractExposesNoMutationMethods() {
        Set<String> methods = Arrays.stream(PlayerProfileView.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertFalse(methods.contains("addXp"));
        assertFalse(methods.contains("setXp"));
        assertFalse(methods.contains("setPreferences"));
        assertFalse(methods.contains("setPreferencesFromJson"));
        assertFalse(methods.contains("markSaved"));
    }

    @Test
    void getProfileReturnsNullWhenNotLoaded() {
        ProfileManager profileManager = new ProfileManager(mock(DatabaseManager.class));
        assertNull(newApi(profileManager).getProfile(UUID.randomUUID()));
    }
}
