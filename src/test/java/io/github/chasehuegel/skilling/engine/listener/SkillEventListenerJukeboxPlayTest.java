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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code jukebox_play} trigger fires only when a music disc is
 * actually inserted into an empty jukebox. The jukebox tile entity is not
 * reliable at the moment the {@code PlayerInteractEvent} listener runs, so the
 * state is re-read on the next tick (the test's scheduler runs the deferred
 * task synchronously). An ejection or a right-click with no disc must never fire.
 */
class SkillEventListenerJukeboxPlayTest {

    /** Counts executions so a dispatch is observable. */
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
    private Block jukeboxBlock;
    private Jukebox jukeboxState;

    @BeforeEach
    void setUp() throws Exception {
        CountingMechanic.EXECUTIONS.set(0);

        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(
                reg -> reg.register("test:count", CountingMechanic.class, List.of()));
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("jukebox.yml"), """
                id: jukebox_skill
                max_level: 100
                display: { name: "Jukebox", color: "PURPLE", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: stage
                    display_name: "Stage"
                    unlock_level: 1
                    trigger: "jukebox_play"
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
        profile.setXp("jukebox_skill", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOnline()).thenReturn(true);

        // The jukebox block state, whose hasRecord() reflects whether a disc is in it.
        jukeboxState = mock(Jukebox.class);
        World world = mock(World.class);
        Location loc = mock(Location.class);
        when(loc.toBlockLocation()).thenReturn(loc);
        jukeboxBlock = mock(Block.class);
        when(jukeboxBlock.getType()).thenReturn(Material.JUKEBOX);
        when(jukeboxBlock.getWorld()).thenReturn(world);
        when(jukeboxBlock.getLocation()).thenReturn(loc);
        when(jukeboxBlock.getState()).thenReturn(jukeboxState);
        when(world.getBlockAt(ArgumentMatchers.any(Location.class))).thenReturn(jukeboxBlock);

        var tagResolver = new TagResolver(new CustomTagLoader());
        RequirementEngine requirementEngine = new RequirementEngine(tagResolver, new StateFilterRegistry());
        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("test:count", CountingMechanic.class, List.of());

        Skilling plugin = mock(Skilling.class);
        Server server = mock(Server.class);
        org.bukkit.scheduler.BukkitScheduler scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        // Execute the deferred next-tick check synchronously for this test.
        doAnswer(inv -> {
            Runnable r = inv.getArgument(1);
            r.run();
            return mock(org.bukkit.scheduler.BukkitTask.class);
        }).when(scheduler).runTask(any(Skilling.class), any(Runnable.class));

        listener = new SkillEventListener(plugin, skillManager, profileManager, tagResolver,
                requirementEngine, mechReg, new FeedbackDebouncer(500),
                mock(BossBarPool.class), new StateFilterRegistry());
    }

    private PlayerInteractEvent rightClickJukebox() {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getHand()).thenReturn(org.bukkit.inventory.EquipmentSlot.HAND);
        when(event.getPlayer()).thenReturn(player);
        when(event.getClickedBlock()).thenReturn(jukeboxBlock);
        return event;
    }

    @Test
    void insertingADiscFiresJukeboxPlay() {
        when(jukeboxState.hasRecord()).thenReturn(true);
        listener.onJukeboxInsert(rightClickJukebox());
        assertEquals(1, CountingMechanic.EXECUTIONS.get(),
                "a right-click that left a record in the jukebox must fire jukebox_play");
    }

    @Test
    void ejectingADiscDoesNotFireJukeboxPlay() {
        when(jukeboxState.hasRecord()).thenReturn(false);
        listener.onJukeboxInsert(rightClickJukebox());
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "an ejection (no record after the click) must never fire jukebox_play");
    }

    @Test
    void emptyJukeboxClickWithNoDiscDoesNotFireJukeboxPlay() {
        when(jukeboxState.hasRecord()).thenReturn(false);
        listener.onJukeboxInsert(rightClickJukebox());
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "right-clicking an empty jukebox without a disc must never fire");
    }

    @Test
    void nonJukeboxRightClickDoesNotFireJukeboxPlay() {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getHand()).thenReturn(org.bukkit.inventory.EquipmentSlot.HAND);
        when(event.getPlayer()).thenReturn(player);
        var stone = mock(Block.class);
        when(stone.getType()).thenReturn(Material.STONE);
        when(event.getClickedBlock()).thenReturn(stone);

        listener.onJukeboxInsert(event);
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "right-clicking a non-jukebox block must never fire");
    }
}
