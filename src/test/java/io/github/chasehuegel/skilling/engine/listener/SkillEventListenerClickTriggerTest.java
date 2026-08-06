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
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the six action-specific click triggers route only the event they
 * claim: each right/left click on air, block, or entity fires its own trigger
 * and no other, the off-hand duplicate is skipped, and projectile attacks do not
 * count as {@code left_click_entity}.
 */
class SkillEventListenerClickTriggerTest {

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
        Files.writeString(skillsDir.resolve("clicks.yml"), """
                id: click_skill
                max_level: 100
                display: { name: "Clicks", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: right_air
                    display_name: "Right Air"
                    unlock_level: 1
                    trigger: "right_click_air"
                    mechanics:
                      - { type: "test:count" }
                    feedback: { notify: { action_bar: false } }
                  - id: right_block
                    display_name: "Right Block"
                    unlock_level: 1
                    trigger: "right_click_block"
                    mechanics:
                      - { type: "test:count" }
                    feedback: { notify: { action_bar: false } }
                  - id: right_entity
                    display_name: "Right Entity"
                    unlock_level: 1
                    trigger: "right_click_entity"
                    mechanics:
                      - { type: "test:count" }
                    feedback: { notify: { action_bar: false } }
                  - id: left_air
                    display_name: "Left Air"
                    unlock_level: 1
                    trigger: "left_click_air"
                    mechanics:
                      - { type: "test:count" }
                    feedback: { notify: { action_bar: false } }
                  - id: left_block
                    display_name: "Left Block"
                    unlock_level: 1
                    trigger: "left_click_block"
                    mechanics:
                      - { type: "test:count" }
                    feedback: { notify: { action_bar: false } }
                  - id: left_entity
                    display_name: "Left Entity"
                    unlock_level: 1
                    trigger: "left_click_entity"
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
    void rightClickAirFiresOnlyRightClickAir() {
        listener.onClickAction(interact(Action.RIGHT_CLICK_AIR));
        assertEquals(1, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void rightClickBlockFiresOnlyRightClickBlock() {
        listener.onClickAction(interact(Action.RIGHT_CLICK_BLOCK));
        assertEquals(1, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void leftClickAirFiresOnlyLeftClickAir() {
        listener.onClickAction(interact(Action.LEFT_CLICK_AIR));
        assertEquals(1, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void leftClickBlockFiresOnlyLeftClickBlock() {
        listener.onClickAction(interact(Action.LEFT_CLICK_BLOCK));
        assertEquals(1, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void offHandClickFiresNoClickTrigger() {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getHand()).thenReturn(org.bukkit.inventory.EquipmentSlot.OFF_HAND);
        when(event.getPlayer()).thenReturn(player);

        listener.onClickAction(event);
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "an off-hand interact must not fire an action click trigger");
    }

    @Test
    void rightClickEntityFiresRightClickEntity() {
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getHand()).thenReturn(org.bukkit.inventory.EquipmentSlot.HAND);
        when(event.getPlayer()).thenReturn(player);

        listener.onRightClickEntity(event);
        assertEquals(1, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void offHandEntityClickFiresNoTrigger() {
        var event = mock(PlayerInteractEntityEvent.class);
        when(event.getHand()).thenReturn(org.bukkit.inventory.EquipmentSlot.OFF_HAND);
        when(event.getPlayer()).thenReturn(player);

        listener.onRightClickEntity(event);
        assertEquals(0, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void leftClickEntityDirectAttackFiresLeftClickEntity() {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(player);

        listener.onLeftClickEntity(event);
        assertEquals(1, CountingMechanic.EXECUTIONS.get());
    }

    @Test
    void leftClickEntityProjectileAttackFiresNoTrigger() {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(mock(Projectile.class));

        listener.onLeftClickEntity(event);
        assertEquals(0, CountingMechanic.EXECUTIONS.get(),
                "a projectile attack is not a left-click on the entity");
    }

    @Test
    void resolveEventMaterialMatchesLeftClickBlockOnlyForLeftClickBlock() {
        var block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);

        assertEquals(Material.STONE, SkillEventListener.resolveEventMaterial(event, "left_click_block"));
        assertNull(SkillEventListener.resolveEventMaterial(event, "right_click_block"));
        assertNull(SkillEventListener.resolveEventMaterial(event, "player_interact"));
    }

    @Test
    void resolveEventMaterialMatchesRightClickBlockForLegacyAndDedicatedTriggers() {
        var block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);

        assertEquals(Material.STONE, SkillEventListener.resolveEventMaterial(event, "right_click_block"));
        assertEquals(Material.STONE, SkillEventListener.resolveEventMaterial(event, "player_interact"));
        assertNull(SkillEventListener.resolveEventMaterial(event, "left_click_block"));
    }

    @Test
    void resolveEventMaterialReturnsNullForAirClicks() {
        var event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getClickedBlock()).thenReturn(null);

        assertNull(SkillEventListener.resolveEventMaterial(event, "right_click_air"));
        assertNull(SkillEventListener.resolveEventMaterial(event, "left_click_air"));
    }
}
