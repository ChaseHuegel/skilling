package io.github.chasehuegel.skilling.requirements;

import io.github.chasehuegel.skilling.engine.requirements.FailureReason;
import io.github.chasehuegel.skilling.engine.requirements.RequirementResult;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class RequirementEngineTest {

    @TempDir
    java.nio.file.Path tempDir;

    private RequirementEngine engine;
    private TagResolver tagResolver;

    @BeforeEach
    void setUp() {
        tagResolver = mock(TagResolver.class);
        engine = new RequirementEngine(tagResolver, newStateFilterRegistry());
    }

    private static io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry newStateFilterRegistry() {
        var registry = new io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry();
        registry.register("is_sneaking", (p, e, v) -> p.isSneaking());
        registry.register("dimension", (p, e, v) -> {
            var env = p.getWorld().getEnvironment();
            return switch (v) {
                case "overworld" -> env == org.bukkit.World.Environment.NORMAL;
                case "nether" -> env == org.bukkit.World.Environment.NETHER;
                case "end" -> env == org.bukkit.World.Environment.THE_END;
                default -> false;
            };
        });
        return registry;
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
        assertEquals(5.0, req.cooldown().evaluate(10, 5), 1e-9);
        assertTrue(req.state().contains("is_sneaking"));
        assertEquals(1, req.items().size());
    }

    @Test
    void checkWithNoRequirementsReturnsPassed() {
        var requirements = new SkillDefinition.Requirements(0, List.of(), List.of());
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        var result = engine.check(player, "test_ability", requirements, 10, 5);
        assertTrue(result.success());
    }

    @Test
    void checkWithMatchingStateReturnsPassed() {
        var requirements = new SkillDefinition.Requirements(0, List.of("is_sneaking"), List.of());
        var player = mock(Player.class);
        when(player.isSneaking()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        var result = engine.check(player, "test_ability", requirements, 10, 5);
        assertTrue(result.success());
    }

    @Test
    void checkWithNonMatchingStateReturnsMissingState() {
        var requirements = new SkillDefinition.Requirements(0, List.of("is_sneaking"), List.of());
        var player = mock(Player.class);
        when(player.isSneaking()).thenReturn(false);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        var result = engine.check(player, "test_ability", requirements, 10, 5);
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
        when(inventory.getItem(org.bukkit.inventory.EquipmentSlot.HAND)).thenReturn(coal);
        var contents = new ItemStack[36];
        contents[0] = coal;
        when(inventory.getContents()).thenReturn(contents);

        engine.consume(player, "test_ability", requirements, 10, 5);
        verify(coal).setAmount(4);
    }

    @Test
    void consumeAppliesCooldown() {
        var requirements = new SkillDefinition.Requirements(5.0, List.of(), List.of());
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        engine.consume(player, "test_ability", requirements, 10, 5);

        var result = engine.check(player, "test_ability", requirements, 10, 5);
        assertEquals(FailureReason.COOLDOWN, result.failureReason());
    }

    @Test
    void clearCooldownsRemovesCooldowns() {
        var requirements = new SkillDefinition.Requirements(5.0, List.of(), List.of());
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        engine.consume(player, "test_ability", requirements, 10, 5);
        engine.clearCooldowns(player);

        var result = engine.check(player, "test_ability", requirements, 10, 5);
        assertTrue(result.success());
    }

    @Test
    void possessionWithOffHandSlotOnlyMatchesOffHandStack() {
        var requirements = new SkillDefinition.Requirements(
                0, List.of(),
                List.of(new SkillDefinition.ItemRequirement("possession", "minecraft:shield", "OFF_HAND", 1, 0.0))
        );
        var player = mock(Player.class);
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        var offHandShield = mock(ItemStack.class);
        when(offHandShield.getType()).thenReturn(Material.SHIELD);
        when(offHandShield.getAmount()).thenReturn(1);
        when(inventory.getItem(org.bukkit.inventory.EquipmentSlot.OFF_HAND)).thenReturn(offHandShield);

        // Main hand has the item but the requirement is OFF_HAND.
        var mainHandShield = mock(ItemStack.class);
        when(mainHandShield.getType()).thenReturn(Material.SHIELD);
        when(inventory.getItem(org.bukkit.inventory.EquipmentSlot.HAND)).thenReturn(mainHandShield);

        // Off-hand absent -> fail.
        when(inventory.getItem(org.bukkit.inventory.EquipmentSlot.OFF_HAND)).thenReturn(null);
        assertTrue(engine.check(player, "test_ability", requirements, 10, 5).failureReason() != null);

        // Off-hand present -> pass even though only the off-hand slot is inspected.
        when(inventory.getItem(org.bukkit.inventory.EquipmentSlot.OFF_HAND)).thenReturn(offHandShield);
        assertTrue(engine.check(player, "test_ability", requirements, 10, 5).success());
    }

    @Test
    void possessionWithAmountRequiresThatManyItems() {
        var requirements = new SkillDefinition.Requirements(
                0, List.of(),
                List.of(new SkillDefinition.ItemRequirement("possession", "minecraft:coal", "HAND", 3, 0.0))
        );
        var player = mock(Player.class);
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        // 2 coal in the inventory < required 3 -> fail.
        var coal2 = mock(ItemStack.class);
        when(coal2.getType()).thenReturn(Material.COAL);
        when(coal2.getAmount()).thenReturn(2);
        var contents = new ItemStack[36];
        contents[0] = coal2;
        when(inventory.getContents()).thenReturn(contents);
        assertFalse(engine.check(player, "test_ability", requirements, 10, 5).success());

        // 3 coal -> pass.
        when(coal2.getAmount()).thenReturn(3);
        assertTrue(engine.check(player, "test_ability", requirements, 10, 5).success());
    }

    @Test
    void costRemovalRespectsSlotAndAmount() {
        var requirements = new SkillDefinition.Requirements(
                0, List.of(),
                List.of(new SkillDefinition.ItemRequirement("cost", "minecraft:coal", "MAIN_HAND", 2, 0.0))
        );
        var player = mock(Player.class);
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        var coal = mock(ItemStack.class);
        when(coal.getType()).thenReturn(Material.COAL);
        when(coal.getAmount()).thenReturn(5);
        when(inventory.getItem(org.bukkit.inventory.EquipmentSlot.HAND)).thenReturn(coal);

        engine.consume(player, "test_ability", requirements, 10, 5);
        // Amount 2 removed from the main-hand stack (5 -> 3), not the whole inventory.
        verify(coal).setAmount(3);
    }

    @Test
    void malformedSlotFailsFast() {
        var requirements = new SkillDefinition.Requirements(
                0, List.of(),
                List.of(new SkillDefinition.ItemRequirement("possession", "minecraft:coal", "not_a_slot", 1, 0.0))
        );
        var player = mock(Player.class);
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        assertThrows(IllegalArgumentException.class,
                () -> engine.check(player, "test_ability", requirements, 10, 5));
    }

    @Test
    void sneakingRequirementAndFilterBehaveIdentically() {
        var registry = newStateFilterRegistry();
        var engine = new RequirementEngine(mock(TagResolver.class), registry);
        var requirements = new SkillDefinition.Requirements(0, List.of("is_sneaking"), List.of());

        var sneakingPlayer = mock(Player.class);
        when(sneakingPlayer.isSneaking()).thenReturn(true);
        when(sneakingPlayer.getUniqueId()).thenReturn(UUID.randomUUID());

        var standingPlayer = mock(Player.class);
        when(standingPlayer.isSneaking()).thenReturn(false);
        when(standingPlayer.getUniqueId()).thenReturn(UUID.randomUUID());

        for (Player p : List.of(sneakingPlayer, standingPlayer)) {
            boolean requirementPasses = engine.check(p, "a", requirements, 10, 5).success();
            boolean filterPasses = registry.evaluate("is_sneaking", p, null, "");
            assertEquals(filterPasses, requirementPasses, "requirement and filter must agree");
        }
    }

    @Test
    void dimensionRequirementAndFilterBehaveIdentically() {
        var registry = newStateFilterRegistry();
        var engine = new RequirementEngine(mock(TagResolver.class), registry);

        var world = mock(org.bukkit.World.class);
        when(world.getEnvironment()).thenReturn(org.bukkit.World.Environment.NETHER);
        var player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        assertTrue(engine.check(player, "a",
                new SkillDefinition.Requirements(0, List.of("dimension:nether"), List.of()), 10, 5).success());
        assertFalse(engine.check(player, "a",
                new SkillDefinition.Requirements(0, List.of("dimension:overworld"), List.of()), 10, 5).success());
        assertTrue(registry.evaluate("dimension", player, null, "nether"));
        assertFalse(registry.evaluate("dimension", player, null, "overworld"));
    }

    @Test
    void unknownStateFailsLikeFilterPath() {
        var registry = newStateFilterRegistry();
        var engine = new RequirementEngine(mock(TagResolver.class), registry);
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        assertFalse(engine.check(player, "a",
                new SkillDefinition.Requirements(0, List.of("not_a_state"), List.of()), 10, 5).success());
        assertFalse(registry.evaluate("not_a_state", player, null, ""));
    }

    @Test
    void tagRequirementReusesCachedResolutionAcrossChecks() throws Exception {
        java.nio.file.Path tagsFile = tempDir.resolve("tags.yml");
        java.nio.file.Files.writeString(tagsFile, "custom_tags:\n  ores:\n    - \"minecraft:coal\"\n");
        var loader = new CustomTagLoader();
        loader.load(tagsFile.toFile());
        TagResolver realResolver = new TagResolver(loader);
        // Load-time pre-flattening, mirroring SkillManager's eager warmup.
        realResolver.warm("#c:ores");

        var engine = new RequirementEngine(realResolver, newStateFilterRegistry());
        var requirements = new SkillDefinition.Requirements(
                0, List.of(),
                List.of(new SkillDefinition.ItemRequirement("possession", "#c:ores", "HAND", 1, 0.0))
        );

        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        var coal = mock(ItemStack.class);
        when(coal.getType()).thenReturn(Material.COAL);
        when(coal.getAmount()).thenReturn(1);
        when(inventory.getContents()).thenReturn(new ItemStack[]{coal});

        long before = realResolver.resolutionCount();
        assertTrue(engine.check(player, "a", requirements, 10, 5).success());
        assertTrue(engine.check(player, "a", requirements, 10, 5).success());
        assertEquals(before, realResolver.resolutionCount(),
                "the flattened tag set must be reused, not re-resolved per check or slot");
    }

    @Test
    void exhaustionBelowMinimumFailsCheck() {
        var requirements = new SkillDefinition.Requirements(
                new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator(0.0),
                List.of(), List.of(),
                new SkillDefinition.Exhaustion(2.0, 5.0)
        );
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getFoodLevel()).thenReturn(3);

        var result = engine.check(player, "a", requirements, 10, 5);
        assertEquals(FailureReason.EXHAUSTION, result.failureReason());
    }

    @Test
    void exhaustionAtMinimumPassesCheck() {
        // The minimum is inclusive: a food level equal to the minimum activates.
        var requirements = new SkillDefinition.Requirements(
                new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator(0.0),
                List.of(), List.of(),
                new SkillDefinition.Exhaustion(2.0, 5.0)
        );
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getFoodLevel()).thenReturn(5);

        assertTrue(engine.check(player, "a", requirements, 10, 5).success());
    }

    @Test
    void exhaustionAboveMinimumPassesCheck() {
        var requirements = new SkillDefinition.Requirements(
                new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator(0.0),
                List.of(), List.of(),
                new SkillDefinition.Exhaustion(2.0, 5.0)
        );
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getFoodLevel()).thenReturn(6);

        assertTrue(engine.check(player, "a", requirements, 10, 5).success());
    }

    @Test
    void exhaustionConsumeReducesFoodLevelByAmount() {
        var requirements = new SkillDefinition.Requirements(
                new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator(0.0),
                List.of(), List.of(),
                new SkillDefinition.Exhaustion(2.0, 0.0)
        );
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getFoodLevel()).thenReturn(10);

        engine.consume(player, "a", requirements, 10, 5);
        verify(player).setFoodLevel(8);
    }

    @Test
    void exhaustionConsumeClampsAtZero() {
        var requirements = new SkillDefinition.Requirements(
                new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator(0.0),
                List.of(), List.of(),
                new SkillDefinition.Exhaustion(5.0, 0.0)
        );
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getFoodLevel()).thenReturn(2);

        engine.consume(player, "a", requirements, 10, 5);
        verify(player).setFoodLevel(0);
    }

    @Test
    void missingCostItemFailsCheckWithMissingItem() {
        var requirements = new SkillDefinition.Requirements(
                0, List.of(),
                List.of(new SkillDefinition.ItemRequirement("cost", "minecraft:coal", "HAND", 1, 0.0))
        );
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getContents()).thenReturn(new ItemStack[36]);

        var result = engine.check(player, "a", requirements, 10, 5);
        assertEquals(FailureReason.MISSING_ITEM, result.failureReason());
    }

    @Test
    void nullTagIsNullSafeInResolver() {
        // Defense-in-depth: a programmatically built null tag yields an empty
        // match set rather than NPEing; the YAML path rejects it at load.
        var requirements = new SkillDefinition.Requirements(
                0, List.of(),
                List.of(new SkillDefinition.ItemRequirement("possession", null, "HAND", 1, 0.0))
        );
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getContents()).thenReturn(new ItemStack[36]);

        var result = engine.check(player, "a", requirements, 10, 5);
        assertEquals(FailureReason.MISSING_ITEM, result.failureReason());
    }

    @Test
    void tagCostConsumesOnlyMatchingItems() throws Exception {
        java.nio.file.Path tagsFile = tempDir.resolve("tags.yml");
        java.nio.file.Files.writeString(tagsFile,
                "custom_tags:\n  ores:\n    - \"minecraft:coal\"\n    - \"minecraft:iron_ingot\"\n");
        var loader = new CustomTagLoader();
        loader.load(tagsFile.toFile());
        TagResolver realResolver = new TagResolver(loader);
        realResolver.warm("#c:ores");

        var engine = new RequirementEngine(realResolver, newStateFilterRegistry());
        var requirements = new SkillDefinition.Requirements(
                0, List.of(),
                List.of(new SkillDefinition.ItemRequirement("cost", "#c:ores", "MAIN_HAND", 1, 0.0))
        );

        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        var coal = mock(ItemStack.class);
        when(coal.getType()).thenReturn(Material.COAL);
        when(coal.getAmount()).thenReturn(5);
        when(inventory.getItem(org.bukkit.inventory.EquipmentSlot.HAND)).thenReturn(coal);

        engine.consume(player, "a", requirements, 10, 5);
        // Only the ores-tagged stack is consumed (5 -> 4); non-matching stacks untouched.
        verify(coal).setAmount(4);
    }
}