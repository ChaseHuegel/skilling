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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the union {@code right_click} trigger: it fires for any main-hand
 * right-click regardless of surface (air, block, or entity) so a "right-click
 * use" like playing a goat horn works no matter what the cursor hits, and it
 * never fires for a left-click or an off-hand interact.
 */
class SkillEventListenerRightClickTriggerTest {

    /** Counts executions so routing is observable. */
    public static class CountingMechanic implements SkillMechanic {
        public static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Override
        public boolean execute(Player player, java.util.Map<String, Object> params, Event event) {
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
                reg -> reg.register("test:count", CountingMechanic.class, List.of()));
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("click.yml"), """
                id: click_skill
                max_level: 100
                display: { name: "Clicks", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: right_click
                    display_name: "Right Click"
                    unlock_level: 1
                    trigger: "right_click"
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
        profile.setXp("click_skill", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        var tagResolver = new TagResolver(new CustomTagLoader());
        RequirementEngine requirementEngine = new RequirementEngine(tagResolver, new StateFilterRegistry());
        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("test:count", CountingMechanic.class, List.of());

        Skilling plugin = mock(Skilling.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(mock(org.bukkit.scheduler.BukkitScheduler.class));

        listener = new SkillEventListener(plugin, skillManager, profileManager, tagResolver,
                requirementEngine, mechReg, new FeedbackDebouncer(500),
                mock(BossBarPool.class), new StateFilterRegistry());
    }

    private PlayerInteractEvent interact(Action action) {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(action);
        when(event.getHand()).thenReturn(org.bukkit.inventory.EquipmentSlot.HAND);
        when(event.getPlayer()).thenReturn(player);
        return event;
    }

    @Test
    void rightClickAirFiresRightClick() {
        listener.onClickAction(interact(Action.RIGHT_CLICK_AIR));
        assertEquals(1, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void rightClickBlockFiresRightClick() {
        listener.onClickAction(interact(Action.RIGHT_CLICK_BLOCK));
        assertEquals(1, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void leftClicksNeverFireRightClick() {
        listener.onClickAction(interact(Action.LEFT_CLICK_AIR));
        listener.onClickAction(interact(Action.LEFT_CLICK_BLOCK));
        assertEquals(0, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void rightClickEntityFiresRightClick() {
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getHand()).thenReturn(org.bukkit.inventory.EquipmentSlot.HAND);
        when(event.getPlayer()).thenReturn(player);

        listener.onRightClickEntity(event);
        assertEquals(1, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void offHandRightClickFiresNoRightClick() {
        var event = interact(Action.RIGHT_CLICK_AIR);
        when(event.getHand()).thenReturn(org.bukkit.inventory.EquipmentSlot.OFF_HAND);

        listener.onClickAction(event);
        assertEquals(0, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void offHandEntityClickFiresNoRightClick() {
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getHand()).thenReturn(org.bukkit.inventory.EquipmentSlot.OFF_HAND);
        when(event.getPlayer()).thenReturn(player);

        listener.onRightClickEntity(event);
        assertEquals(0, CountingMechanic.EXECUTIONS.get());
    }
}
