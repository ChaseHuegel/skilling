package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.feedback.BossBarPool;
import io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the hot-path trigger gating: off-hand interacts and sprint/sneak
 * toggle-off events must not fire abilities.
 */
class SkillEventListenerTriggerGatingTest {

    /** Counts executions so gating is observable. */
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
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
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
                xp_sources: []
                abilities:
                  - id: interact_ability
                    display_name: "Interact"
                    unlock_level: 1
                    trigger: "player_interact"
                    mechanics:
                      - { type: "test:count" }
                    feedback: { notify: { action_bar: false } }
                  - id: sprint_ability
                    display_name: "Sprint"
                    unlock_level: 1
                    trigger: "sprint"
                    mechanics:
                      - { type: "test:count" }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        UUID uuid = UUID.randomUUID();
        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        PlayerProfile profile = profileManager.loadProfile(uuid).join();
        profile.setXp("test_skill", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        var tagResolver = new TagResolver(new CustomTagLoader());
        RequirementEngine requirementEngine = new RequirementEngine(tagResolver, new StateFilterRegistry());
        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("test:count", CountingMechanic.class, java.util.List.of());

        Skilling plugin = mock(Skilling.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(mock(org.bukkit.scheduler.BukkitScheduler.class));

        listener = new SkillEventListener(plugin, skillManager, profileManager, tagResolver,
                requirementEngine, mechReg, new FeedbackDebouncer(500),
                mock(BossBarPool.class), new StateFilterRegistry());
    }

    @Test
    void offHandInteractDoesNotFireAbility() {
        var offHand = mock(PlayerInteractEvent.class);
        when(offHand.getAction()).thenReturn(org.bukkit.event.block.Action.RIGHT_CLICK_AIR);
        when(offHand.getHand()).thenReturn(org.bukkit.inventory.EquipmentSlot.OFF_HAND);
        when(offHand.getPlayer()).thenReturn(player);

        listener.onPlayerInteract(offHand);
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "an off-hand interact must not double-fire the player_interact ability");
    }

    @Test
    void mainHandInteractFiresAbility() {
        var mainHand = mock(PlayerInteractEvent.class);
        when(mainHand.getAction()).thenReturn(org.bukkit.event.block.Action.RIGHT_CLICK_AIR);
        when(mainHand.getHand()).thenReturn(org.bukkit.inventory.EquipmentSlot.HAND);
        when(mainHand.getPlayer()).thenReturn(player);

        listener.onPlayerInteract(mainHand);
        assertEquals(1, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void sprintToggleOffDoesNotFireAbility() {
        var off = mock(PlayerToggleSprintEvent.class);
        when(off.isSprinting()).thenReturn(false);
        when(off.getPlayer()).thenReturn(player);

        listener.onSprint(off);
        assertEquals(0, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void sprintToggleOnFiresAbility() {
        var on = mock(PlayerToggleSprintEvent.class);
        when(on.isSprinting()).thenReturn(true);
        when(on.getPlayer()).thenReturn(player);

        listener.onSprint(on);
        assertEquals(1, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void sneakToggleOffDoesNotFireAbility() {
        var off = mock(PlayerToggleSneakEvent.class);
        when(off.isSneaking()).thenReturn(false);
        when(off.getPlayer()).thenReturn(player);

        listener.onSneak(off);
        assertEquals(0, CountingMechanic.EXECUTIONS.get());
    }
}
