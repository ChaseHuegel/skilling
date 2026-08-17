package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.world.ChunkLoadEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies {@code onChunkLoad}: only first-time chunks dispatch, and the reach
 * is horizontal and spans the player's client view distance, so a player at
 * ground level exploring new terrain is found even though the new chunk
 * generates at the edge of their view rather than at their feet.
 */
class SkillEventListenerChunkLoadTest {

    /** Records the runtime event class of every execution. */
    public static class RecordingMechanic implements SkillMechanic {
        public static final CopyOnWriteArrayList<Class<? extends Event>> EVENTS = new CopyOnWriteArrayList<>();

        @Override
        public boolean execute(Player player, java.util.Map<String, Object> params, Event event) {
            EVENTS.add(event.getClass());
            return true;
        }
    }

    @TempDir
    Path tempDir;

    private SkillEventListener listener;
    private PlayerProfile profile;
    private Player player;
    private World world;
    private Chunk chunk;

    @BeforeEach
    void setUp() throws Exception {
        RecordingMechanic.EVENTS.clear();

        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(
                reg -> reg.register("test:record", RecordingMechanic.class, List.of()));
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("chunk.yml"), """
                id: chunk_skill
                max_level: 100
                display: { name: "Chunk", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: explore
                    display_name: "Explore"
                    unlock_level: 1
                    trigger: "chunk_load"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();
        profile = profileManager.loadProfile(uuid).join();
        profile.setXp("chunk_skill", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getClientViewDistance()).thenReturn(10);

        world = mock(World.class);
        chunk = mock(Chunk.class);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(0);
        when(chunk.getZ()).thenReturn(0);

        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("test:record", RecordingMechanic.class, List.of());

        Skilling plugin = mock(Skilling.class);
        when(plugin.getGlobalXpModifier()).thenReturn(1.0);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("chunk-load-test"));

        listener = new SkillEventListener(plugin, skillManager, profileManager,
                new TagResolver(new CustomTagLoader()),
                new RequirementEngine(new TagResolver(new CustomTagLoader()), new StateFilterRegistry()),
                mechReg, new io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer(500),
                mock(io.github.chasehuegel.skilling.engine.feedback.BossBarPool.class),
                new StateFilterRegistry());
    }

    private ChunkLoadEvent chunkEvent(boolean newChunk) {
        var event = mock(ChunkLoadEvent.class);
        when(event.isNewChunk()).thenReturn(newChunk);
        when(event.getChunk()).thenReturn(chunk);
        return event;
    }

    private void placePlayer(double x, double z, double y) {
        when(player.getLocation()).thenReturn(new Location(world, x, y, z));
        when(world.getPlayers()).thenReturn(List.of(player));
    }

    @Test
    void nonNewChunkDispatchesNothing() {
        placePlayer(8, 8, 64);
        listener.onChunkLoad(chunkEvent(false));
        assertTrue(RecordingMechanic.EVENTS.isEmpty(),
                "a chunk loaded from disk is not exploration");
    }

    @Test
    void exploringPlayerAtGroundLevelReceivesDispatch() {
        // Player at the new chunk's center on the surface (y=64), not at the
        // chunk's ground plane (y=0): the horizontal reach must find them.
        placePlayer(8, 8, 64);
        listener.onChunkLoad(chunkEvent(true));
        assertEquals(1, RecordingMechanic.EVENTS.size());
        assertEquals(ChunkLoadEvent.class, RecordingMechanic.EVENTS.get(0));
    }

    @Test
    void playerWithinViewDistanceReceivesDispatch() {
        // A new chunk at the leading edge of a view-10 player is caught by the
        // (view + 1) chunk reach (176 blocks). The player at x=170 is inside it.
        placePlayer(170, 8, 70);
        listener.onChunkLoad(chunkEvent(true));
        assertEquals(1, RecordingMechanic.EVENTS.size());
    }

    @Test
    void playerBeyondViewDistanceReceivesNothing() {
        // Just past the reach (x=190 vs the 176-block reach): no dispatch.
        placePlayer(190, 8, 64);
        listener.onChunkLoad(chunkEvent(true));
        assertTrue(RecordingMechanic.EVENTS.isEmpty());
    }

    @Test
    void dispatchIsThrottledPerPlayer() {
        // New terrain generates many chunks at once; a second chunk in the same
        // throttle window must not dispatch again for the same player.
        placePlayer(8, 8, 64);
        listener.onChunkLoad(chunkEvent(true));
        assertEquals(1, RecordingMechanic.EVENTS.size());
        listener.onChunkLoad(chunkEvent(true));
        assertEquals(1, RecordingMechanic.EVENTS.size(),
                "a second chunk within the throttle window must not dispatch again");
    }

    @Test
    void farAwayPlayerInSameWorldReceivesNothing() {
        placePlayer(1000, 1000, 64);
        listener.onChunkLoad(chunkEvent(true));
        assertTrue(RecordingMechanic.EVENTS.isEmpty());
    }
}
