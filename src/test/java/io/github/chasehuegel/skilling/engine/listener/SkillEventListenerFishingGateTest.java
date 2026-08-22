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

    /** Counts executions so the new {@code fishing_cast} trigger firing is observable. */
    public static class CastCountingMechanic implements SkillMechanic {
        public static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            EXECUTIONS.incrementAndGet();
            return true;
        }
    }

    /** Counts executions so the {@code fishing_hook} trigger firing is observable. */
    public static class HookCountingMechanic implements SkillMechanic {
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
        CastCountingMechanic.EXECUTIONS.set(0);
        HookCountingMechanic.EXECUTIONS.set(0);

        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(
                reg -> {
                    reg.register("test:count", CountingMechanic.class, java.util.List.of());
                    reg.register("test:cast_count", CastCountingMechanic.class, java.util.List.of());
                    reg.register("test:hook_count", HookCountingMechanic.class, java.util.List.of());
                });

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
                  - id: cast_ability
                    display_name: "Cast Ability"
                    unlock_level: 1
                    trigger: "fishing_cast"
                    mechanics:
                      - { type: "test:cast_count" }
                    feedback: { notify: { action_bar: false } }
                  - id: hook_ability
                    display_name: "Hook Ability"
                    unlock_level: 1
                    trigger: "fishing_hook"
                    mechanics:
                      - { type: "test:hook_count" }
                    feedback: { notify: { action_bar: false } }
                """);
        var bukkitMock = org.mockito.Mockito.mockStatic(org.bukkit.Bukkit.class);
        bukkitMock.when(() -> org.bukkit.Bukkit.getLogger()).thenReturn(Logger.getLogger("fishing-gate-load"));
        skillManager.loadSkills(skillsDir.toFile());
        bukkitMock.close();

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
        mechReg.register("test:cast_count", CastCountingMechanic.class, java.util.List.of());
        mechReg.register("test:hook_count", HookCountingMechanic.class, java.util.List.of());

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
    void nonCatchStatesGrantNoXpAndFireNoFishAbilities() {
        long before = profile.getXp("test_skill");
        for (PlayerFishEvent.State state : new PlayerFishEvent.State[]{
                PlayerFishEvent.State.REEL_IN,
                PlayerFishEvent.State.IN_GROUND,
                PlayerFishEvent.State.FAILED_ATTEMPT}) {
            fireState(state);
        }
        assertEquals(before, profile.getXp("test_skill"),
                "reels and failed attempts must not grant XP");
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "non-catch states must not fire the CAUGHT_FISH-only fishing ability");
        assertEquals(0, CastCountingMechanic.EXECUTIONS.get(),
                "failed/reel states must not fire the cast ability");
        assertEquals(0, HookCountingMechanic.EXECUTIONS.get(),
                "failed/reel states must not fire the hook ability");
    }

    @Test
    void caughtFishGrantsExactlyOneXpAndFiresOnlyTheFishAbility() {
        fireState(PlayerFishEvent.State.CAUGHT_FISH);
        assertEquals(501, profile.getXp("test_skill"),
                "a completed catch must grant exactly one XP reward");
        assertEquals(1, CountingMechanic.EXECUTIONS.get(),
                "a completed catch must fire the fishing ability exactly once");
        assertEquals(0, CastCountingMechanic.EXECUTIONS.get(),
                "a completed catch must not fire the cast ability");
        assertEquals(0, HookCountingMechanic.EXECUTIONS.get(),
                "a completed catch must not fire the hook ability");
    }

    @Test
    void castStateFiresOnlyTheCastAbilityAndGrantsNoXp() {
        long before = profile.getXp("test_skill");
        fireState(PlayerFishEvent.State.FISHING);
        assertEquals(before, profile.getXp("test_skill"),
                "a cast grants no XP (the catch does)");
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "a cast must not fire the CAUGHT_FISH-only ability");
        assertEquals(1, CastCountingMechanic.EXECUTIONS.get(),
                "a cast must fire the fishing_cast ability exactly once");
        assertEquals(0, HookCountingMechanic.EXECUTIONS.get(),
                "a cast must not fire the hook ability");
    }

    @Test
    void hookStateFiresOnlyTheHookAbilityAndGrantsNoXp() {
        long before = profile.getXp("test_skill");
        fireState(PlayerFishEvent.State.CAUGHT_ENTITY);
        assertEquals(before, profile.getXp("test_skill"),
                "hooking a mob grants no XP");
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "hooking a mob must not fire the CAUGHT_FISH-only ability");
        assertEquals(0, CastCountingMechanic.EXECUTIONS.get(),
                "hooking a mob must not fire the cast ability");
        assertEquals(1, HookCountingMechanic.EXECUTIONS.get(),
                "hooking a mob must fire the fishing_hook ability exactly once");
    }
}
