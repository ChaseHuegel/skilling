package io.github.chasehuegel.skilling.engine.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import io.github.chasehuegel.skilling.TestSkillManager;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a thrown trident (which never passes through the bow path) is
 * stamped with the release-time sneak state, so the {@code was_sneaking} state
 * filter evaluates a sneak-throw truthfully at impact. Without this stamp the
 * trident's {@code was_sneaking} reads would fail closed and the throwing
 * skill's Skewer could never fire.
 */
class SkillEventListenerTridentSneakStampTest {

    private SkillEventListener listener;
    private Player player;
    private UUID uuid;

    @BeforeEach
    void setUp() {
        SkillManager skillManager = TestSkillManager.newBuiltIn();
        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        uuid = UUID.randomUUID();
        profileManager.loadProfile(uuid).join();

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        var tagResolver = new TagResolver(new CustomTagLoader());
        RequirementEngine requirementEngine = new RequirementEngine(tagResolver, new StateFilterRegistry());
        listener = new SkillEventListener(mock(io.github.chasehuegel.skilling.Skilling.class),
                skillManager, profileManager, tagResolver, requirementEngine,
                new MechanicRegistry(), new FeedbackDebouncer(500), mock(BossBarPool.class),
                new StateFilterRegistry());
    }

    private ProjectileLaunchEvent launch(PersistentDataContainer pdc, boolean sneaking) {
        Trident trident = mock(Trident.class);
        when(trident.getShooter()).thenReturn(player);
        when(trident.getPersistentDataContainer()).thenReturn(pdc);
        when(player.isSneaking()).thenReturn(sneaking);
        var event = mock(ProjectileLaunchEvent.class);
        when(event.getEntity()).thenReturn(trident);
        return event;
    }

    @Test
    void sneakThrowStampsTheReleasedStanceAsSneaking() {
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        listener.onLaunchProjectile(launch(pdc, true));
        verify(pdc).set(Skilling.SHOT_SNEAK_KEY, PersistentDataType.BOOLEAN, true);
    }

    @Test
    void normalThrowStampsTheReleasedStanceAsNotSneaking() {
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        listener.onLaunchProjectile(launch(pdc, false));
        verify(pdc).set(Skilling.SHOT_SNEAK_KEY, PersistentDataType.BOOLEAN, false);
    }
}