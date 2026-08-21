package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code map_explore} trigger dispatch: a player carrying a map into
 * newly generated terrain fires the map_explore ability, and one without a map in
 * hand does not (the map-fill reward is gated on actually holding a map).
 */
class SkillEventListenerMapExploreTest {

    @TempDir
    Path tempDir;

    private SkillEventListener listener;
    private PlayerProfile profile;
    private Player player;
    private World world;
    private Chunk chunk;
    private List<AttributeModifier> activeModifiers;

    @BeforeEach
    void setUp() throws Exception {
        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(reg -> {});
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("explore.yml"), """
                id: explore
                max_level: 100
                display: { name: "Explore", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: cartographer
                    display_name: "Cartographer"
                    unlock_level: 1
                    trigger: "map_explore"
                    mechanics:
                      - type: "core:speed_bonus"
                        parameters:
                          multiplier: { constant: 1.1 }
                          uuid: { constant: "8d96db25-38ce-5f64-a2ca-60a4bc6bcb4e" }
                          duration: { constant: 20.0 }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();
        profile = profileManager.loadProfile(uuid).join();
        profile.setXp("explore", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getClientViewDistance()).thenReturn(10);

        // Player holds a filled map in the main hand by default. ItemStacks are
        // mocked (never constructed) so the test JVM does not touch Bukkit.server.
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack map = mock(ItemStack.class);
        when(map.getType()).thenReturn(Material.FILLED_MAP);
        ItemStack empty = mock(ItemStack.class);
        when(empty.getType()).thenReturn(Material.AIR);
        when(inventory.getItemInMainHand()).thenReturn(map);
        when(inventory.getItemInOffHand()).thenReturn(empty);
        when(player.getInventory()).thenReturn(inventory);

        activeModifiers = new ArrayList<>();
        AttributeInstance inst = mock(AttributeInstance.class);
        when(inst.getBaseValue()).thenReturn(0.1);
        doAnswer(inv -> {
            AttributeModifier m = inv.getArgument(0);
            activeModifiers.removeIf(am -> am.getUniqueId().equals(m.getUniqueId()));
            activeModifiers.add(m);
            return null;
        }).when(inst).addTransientModifier(any(AttributeModifier.class));
        when(player.getAttribute(Attribute.MOVEMENT_SPEED)).thenReturn(inst);
        EntityScheduler scheduler = mock(EntityScheduler.class);
        when(scheduler.runDelayed(any(), any(), any(), anyLong())).thenReturn(mock(ScheduledTask.class));
        when(player.getScheduler()).thenReturn(scheduler);

        world = mock(World.class);
        chunk = mock(Chunk.class);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(0);
        when(chunk.getZ()).thenReturn(0);
        when(player.getLocation()).thenReturn(new Location(world, 8, 64, 8));
        when(world.getPlayers()).thenReturn(List.of(player));

        MechanicRegistry mechReg = new MechanicRegistry();
        Skilling.registerBuiltinMechanics(mechReg);
        Skilling plugin = mock(Skilling.class);
        when(plugin.getGlobalXpModifier()).thenReturn(1.0);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("map-explore-test"));

        listener = new SkillEventListener(plugin, skillManager, profileManager,
                new TagResolver(new CustomTagLoader()),
                new RequirementEngine(new TagResolver(new CustomTagLoader()), new StateFilterRegistry()),
                mechReg, new io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer(500),
                mock(io.github.chasehuegel.skilling.engine.feedback.BossBarPool.class),
                new StateFilterRegistry());
    }

    private ChunkLoadEvent chunkEvent() {
        var event = mock(ChunkLoadEvent.class);
        when(event.isNewChunk()).thenReturn(true);
        when(event.getChunk()).thenReturn(chunk);
        return event;
    }

    private void noMapInHand() {
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack empty = mock(ItemStack.class);
        when(empty.getType()).thenReturn(Material.AIR);
        when(inventory.getItemInMainHand()).thenReturn(empty);
        when(inventory.getItemInOffHand()).thenReturn(empty);
        when(player.getInventory()).thenReturn(inventory);
    }

    @Test
    void mapExploreFiresForAPlayerHoldingAMap() {
        listener.onChunkLoad(chunkEvent());
        assertFalse(activeModifiers.isEmpty(),
                "map_explore must fire for a player carrying a map into new terrain");
    }

    @Test
    void mapExploreDoesNotFireWithoutAMapInHand() {
        noMapInHand();
        listener.onChunkLoad(chunkEvent());
        assertTrue(activeModifiers.isEmpty(),
                "map_explore must not fire for a player not holding a map");
    }
}