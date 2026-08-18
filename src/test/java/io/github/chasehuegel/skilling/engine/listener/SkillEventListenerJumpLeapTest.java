package io.github.chasehuegel.skilling.engine.listener;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
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
 * Verifies the Acrobatics Leap path end-to-end: a {@code jump} dispatch to an
 * unlocked, sneaking player applies the {@code core:modify_jump} strength
 * modifier and consumes the hunger cost, proving the ability fires through the
 * new {@code jump} trigger.
 */
class SkillEventListenerJumpLeapTest {

    @TempDir
    Path tempDir;

    private SkillEventListener listener;
    private PlayerProfile profile;
    private Player player;
    private List<AttributeModifier> jumpModifiers;

    @BeforeEach
    void setUp() throws Exception {
        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(reg -> {});
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("acrobatics.yml"), """
                id: acro
                max_level: 100
                display: { name: "Acrobatics", color: "WHITE", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources:
                  - trigger: "jump"
                    filters: [ { state: "is_sprinting" } ]
                    reward: { constant: 15.0 }
                abilities:
                  - id: leap
                    display_name: "Leap"
                    unlock_level: 1
                    trigger: "jump"
                    display:
                      lore:
                        - "&7Sneak+Jump to leap &a{multiplier}x&7 higher."
                        - "&7Costs 1 hunger."
                        - "&8Requires: Sneaking, 3+ hunger."
                    requirements:
                      state: [ "is_sneaking" ]
                      exhaustion: { amount: 1.0, minimum: 3.0 }
                    mechanics:
                      - type: "core:modify_jump"
                        parameters:
                          multiplier: { constant: 1.3 }
                          uuid: { constant: "5ce1a199-41ed-5b8e-80d8-474db73cbee6" }
                          duration: { constant: 1.5 }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();
        profile = profileManager.loadProfile(uuid).join();
        profile.setXp("acro", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isSneaking()).thenReturn(true);
        when(player.getFoodLevel()).thenReturn(10);

        // Record every transient jump-strength modifier applied.
        jumpModifiers = new ArrayList<>();
        AttributeInstance jumpInst = mock(AttributeInstance.class);
        when(jumpInst.getBaseValue()).thenReturn(0.42);
        doAnswer(inv -> {
            AttributeModifier m = inv.getArgument(0);
            jumpModifiers.removeIf(am -> am.getUniqueId().equals(m.getUniqueId()));
            jumpModifiers.add(m);
            return null;
        }).when(jumpInst).addTransientModifier(any(AttributeModifier.class));
        when(player.getAttribute(Attribute.JUMP_STRENGTH)).thenReturn(jumpInst);

        EntityScheduler scheduler = mock(EntityScheduler.class);
        when(scheduler.runDelayed(any(), any(), any(), anyLong())).thenReturn(mock(ScheduledTask.class));
        when(player.getScheduler()).thenReturn(scheduler);

        MechanicRegistry mechReg = new MechanicRegistry();
        Skilling.registerBuiltinMechanics(mechReg);
        var listenerStateFilters = new StateFilterRegistry();
        Skilling.registerBuiltinStateFilters(listenerStateFilters, new TagResolver(new CustomTagLoader()),
                new io.github.chasehuegel.skilling.engine.tag.EntityTagResolver(new CustomTagLoader()));
        Skilling plugin = mock(Skilling.class);
        when(plugin.getGlobalXpModifier()).thenReturn(1.0);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("jump-leap-test"));

        listener = new SkillEventListener(plugin, skillManager, profileManager,
                new TagResolver(new CustomTagLoader()),
                new RequirementEngine(new TagResolver(new CustomTagLoader()), listenerStateFilters),
                mechReg, new io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer(500),
                mock(io.github.chasehuegel.skilling.engine.feedback.BossBarPool.class),
                listenerStateFilters);
    }

    private PlayerJumpEvent jumpEvent() {
        World world = mock(World.class);
        var event = mock(PlayerJumpEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getFrom()).thenReturn(new Location(world, 0, 64, 0));
        when(event.getTo()).thenReturn(new Location(world, 0, 64, 0));
        return event;
    }

    @Test
    void unlockedSneakingPlayerLeapsOnJump() {
        listener.onPlayerJump(jumpEvent());
        assertFalse(jumpModifiers.isEmpty(),
                "jump must apply the jump-strength modifier to an unlocked sneaking player");
        assertTrue(jumpModifiers.stream().allMatch(m ->
                        m.getOperation() == AttributeModifier.Operation.ADD_NUMBER),
                "the leap must be an additive jump-strength modifier");
    }

    @Test
    void leapConsumesHunger() {
        listener.onPlayerJump(jumpEvent());
        org.mockito.Mockito.verify(player).setFoodLevel(9);
    }
}
