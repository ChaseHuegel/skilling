package io.github.chasehuegel.skilling.requirements;

import io.github.chasehuegel.skilling.engine.requirements.FailureReason;
import io.github.chasehuegel.skilling.engine.requirements.RequirementResult;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class RequirementEngineTest {

    private RequirementEngine engine;
    private TagResolver tagResolver;

    @BeforeEach
    void setUp() {
        tagResolver = mock(TagResolver.class);
        engine = new RequirementEngine(tagResolver);
    }


    @Test
    void passedResultObjectStructure() {
        var result = RequirementResult.PASSED;
        assertTrue(result.success());
        assertNull(result.failureReason());
    }

    @Test
    void failedResultContainsReasonAndPlaceholders() {
        var result = RequirementResult.failed(
                FailureReason.COOLDOWN,
                Map.of("time", "3.5")
        );
        assertFalse(result.success());
        assertEquals(FailureReason.COOLDOWN, result.failureReason());
        assertEquals("3.5", result.placeholders().get("time"));
    }

    @Test
    void itemRequirementRecord() {
        var itemReq = new SkillDefinition.ItemRequirement(
                "cost", "minecraft:coal", "MAIN_HAND", 1, 0.0
        );
        assertEquals("cost", itemReq.action());
        assertEquals("minecraft:coal", itemReq.tag());
        assertEquals("MAIN_HAND", itemReq.slot());
        assertEquals(1, itemReq.amount());
        assertEquals(0.0, itemReq.itemCooldown(), 1e-9);
    }

    @Test
    void requirementsRecordWithDefaults() {
        var req = new SkillDefinition.Requirements(
                5.0,
                List.of("is_sneaking"),
                List.of(new SkillDefinition.ItemRequirement("possession", "#minecraft:pickaxes", "MAIN_HAND", 1, 0.0))
        );
        assertEquals(5.0, req.cooldown(), 1e-9);
        assertTrue(req.state().contains("is_sneaking"));
        assertEquals(1, req.items().size());
    }

    @Test
    void checkWithNoRequirementsReturnsPassed() {
        var requirements = new SkillDefinition.Requirements(0, List.of(), List.of());
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        var result = engine.check(player, "test_ability", requirements);
        assertTrue(result.success());
    }

    @Test
    void checkWithMatchingStateReturnsPassed() {
        var requirements = new SkillDefinition.Requirements(0, List.of("is_sneaking"), List.of());
        var player = mock(Player.class);
        when(player.isSneaking()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        var result = engine.check(player, "test_ability", requirements);
        assertTrue(result.success());
    }

    @Test
    void checkWithNonMatchingStateReturnsMissingState() {
        var requirements = new SkillDefinition.Requirements(0, List.of("is_sneaking"), List.of());
        var player = mock(Player.class);
        when(player.isSneaking()).thenReturn(false);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        var result = engine.check(player, "test_ability", requirements);
        assertEquals(FailureReason.MISSING_STATE, result.failureReason());
    }

    @Test
    void consumeRemovesCostItems() {
        var requirements = new SkillDefinition.Requirements(
                0, List.of(),
                List.of(new SkillDefinition.ItemRequirement("cost", "minecraft:coal", "MAIN_HAND", 1, 0.0))
        );
        var player = mock(Player.class);
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        var coal = mock(ItemStack.class);
        when(coal.getType()).thenReturn(Material.COAL);
        when(coal.getAmount()).thenReturn(5);
        var contents = new ItemStack[36];
        contents[0] = coal;
        when(inventory.getContents()).thenReturn(contents);

        engine.consume(player, "test_ability", requirements);
        verify(coal).setAmount(4);
    }

    @Test
    void consumeAppliesCooldown() {
        var requirements = new SkillDefinition.Requirements(5.0, List.of(), List.of());
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        engine.consume(player, "test_ability", requirements);

        var result = engine.check(player, "test_ability", requirements);
        assertEquals(FailureReason.COOLDOWN, result.failureReason());
    }

    @Test
    void clearCooldownsRemovesCooldowns() {
        var requirements = new SkillDefinition.Requirements(5.0, List.of(), List.of());
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        engine.consume(player, "test_ability", requirements);
        engine.clearCooldowns(player);

        var result = engine.check(player, "test_ability", requirements);
        assertTrue(result.success());
    }
}