package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerFishEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code fishing} trigger only dispatches on the {@code CAUGHT_FISH}
 * state: a single cast-and-catch grants exactly one XP reward and fires fishing
 * abilities exactly once, while casts, bites, reels, and failed attempts grant
 * nothing.
 */
class SkillEventListenerFishingGateTest {

    public static class CountingMechanic implements SkillMechanic {
        public static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            EXECUTIONS.incrementAndGet();
            return true;
        }
    }

    @TempDir
    Path tempDir;

    private SkillEventListener listener;
    private PlayerProfile profile;
    private Player player;

    @BeforeEach
    void setUp() throws IOException {
        CountingMechanic.EXECUTIONS.set(0);

        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(
                reg -> reg.register("test:count", CountingMechanic.class, java.util.List.of()));

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("test.yml"), """
                id: test_skill
                max_level: 100
                display: { name: "Test", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources:
                  - { trigger: "fishing", reward: { constant: 1.0 } }
                abilities:
                  - id: test_ability
                    display_name: "Test Ability"
                    unlock_level: 1
                    trigger: "fishing"
                    mechanics:
                      - { type: "test:count" }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();
        profile = profileManager.loadProfile(uuid).join();
        profile.setXp("test_skill", 500);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("test:count", CountingMechanic.class, java.util.List.of());

        Skilling plugin = mock(Skilling.class);
        when(plugin.getGlobalXpModifier()).thenReturn(1.0);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("fishing-gate-test"));

        listener = new SkillEventListener(plugin, skillManager, profileManager,
                new TagResolver(new CustomTagLoader()),
                new io.github.chasehuegel.skilling.engine.requirements.RequirementEngine(
                        new TagResolver(new CustomTagLoader()), new StateFilterRegistry()),
                mechReg, new io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer(500),
                mock(io.github.chasehuegel.skilling.engine.feedback.BossBarPool.class),
                new StateFilterRegistry());
    }

    private void fireState(PlayerFishEvent.State state) {
        PlayerFishEvent event = mock(PlayerFishEvent.class);
        when(event.getState()).thenReturn(state);
        when(event.getPlayer()).thenReturn(player);
        listener.onFish(event);
    }

    @Test
    void nonCatchStatesGrantNoXpAndFireNoAbilities() {
        long before = profile.getXp("test_skill");
        for (PlayerFishEvent.State state : new PlayerFishEvent.State[]{
                PlayerFishEvent.State.FISHING,
                PlayerFishEvent.State.CAUGHT_ENTITY,
                PlayerFishEvent.State.REEL_IN,
                PlayerFishEvent.State.IN_GROUND,
                PlayerFishEvent.State.FAILED_ATTEMPT}) {
            fireState(state);
        }
        assertEquals(before, profile.getXp("test_skill"),
                "casts, bites, reels, and failed attempts must not grant XP");
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "non-catch states must not fire fishing abilities");
    }

    @Test
    void caughtFishGrantsExactlyOneXpAndFiresAbilityOnce() {
        fireState(PlayerFishEvent.State.CAUGHT_FISH);
        assertEquals(501, profile.getXp("test_skill"),
                "a completed catch must grant exactly one XP reward");
        assertEquals(1, CountingMechanic.EXECUTIONS.get(),
                "a completed catch must fire fishing abilities exactly once");
    }
}
