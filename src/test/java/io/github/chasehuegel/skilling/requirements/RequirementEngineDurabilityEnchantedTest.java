package io.github.chasehuegel.skilling.requirements;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.requirements.FailureReason;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.EnumSet;
import java.util.UUID;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequirementEngineDurabilityEnchantedTest {

    private RequirementEngine engine;
    private TagResolver tagResolver;

    @BeforeEach
    void setUp() {
        tagResolver = mock(TagResolver.class);
        var stateFilters = new StateFilterRegistry();
        stateFilters.register("is_sneaking", (p, e, v) -> false);
        engine = new RequirementEngine(tagResolver, stateFilters);
    }

    private Player playerWithHand(ItemStack item) {
        var player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        var inventory = mock(PlayerInventory.class);
        when(inventory.getItem(EquipmentSlot.HAND)).thenReturn(item);
        when(player.getInventory()).thenReturn(inventory);
        return player;
    }

    private SkillDefinition.Durability durability(int amount) {
        return new SkillDefinition.Durability(
                new io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator(amount),
                "MAIN_HAND");
    }

    @Test
    void durabilityCheckPassesWithDamageableItemInSlot() {
        var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(item.getAmount()).thenReturn(1);
        when(item.getItemMeta()).thenReturn(mock(org.bukkit.inventory.meta.Damageable.class));
        var player = playerWithHand(item);

        var req = new SkillDefinition.Requirements(new ConstantEvaluator(0.0), List.of(), List.of(), null, durability(30));
        var result = engine.check(player, "s", "a", req, 10, 5);
        assertTrue(result.success(), () -> "expected pass, got " + result.failureReason());
    }

    @Test
    void durabilityCheckFailsWithoutDamageableItem() {
        var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(item.getAmount()).thenReturn(1);
        when(item.getItemMeta()).thenReturn(mock(ItemMeta.class));
        var player = playerWithHand(item);

        var req = new SkillDefinition.Requirements(new ConstantEvaluator(0.0), List.of(), List.of(), null, durability(30));
        var result = engine.check(player, "s", "a", req, 10, 5);
        assertEquals(FailureReason.DURABILITY, result.failureReason());
    }

    @Test
    void durabilityConsumeDamagesTheItem() {
        var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(item.getAmount()).thenReturn(1);
        var damageable = mock(org.bukkit.inventory.meta.Damageable.class);
        when(damageable.getDamage()).thenReturn(100);
        when(item.getItemMeta()).thenReturn(damageable);
        var player = playerWithHand(item);

        var req = new SkillDefinition.Requirements(new ConstantEvaluator(0.0), List.of(), List.of(), null, durability(30));
        engine.consume(player, "s", "a", req, 10, 5);
        verify(damageable).setDamage(130);
    }

    @Test
    void enchantedPredicateRejectsUnenchantedItem() {
        when(tagResolver.resolve("#c:enchantable")).thenReturn(EnumSet.of(Material.DIAMOND_SWORD));
        var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(item.getAmount()).thenReturn(1);
        var meta = mock(ItemMeta.class);
        when(meta.getEnchants()).thenReturn(Map.of());
        when(item.getItemMeta()).thenReturn(meta);
        var player = playerWithHand(item);

        var itemReq = new SkillDefinition.ItemRequirement(
                "possession", "#c:enchantable", "MAIN_HAND", 1, 0.0, true);
        var req = new SkillDefinition.Requirements(new ConstantEvaluator(0.0), List.of(), List.of(itemReq), null, null);
        var result = engine.check(player, "s", "a", req, 10, 5);
        assertEquals(FailureReason.MISSING_ITEM, result.failureReason());
    }

    @Test
    void enchantedPredicateAcceptsEnchantedItem() {
        when(tagResolver.resolve("#c:enchantable")).thenReturn(EnumSet.of(Material.DIAMOND_SWORD));
        var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(item.getAmount()).thenReturn(1);
        var meta = mock(ItemMeta.class);
        // Enchantment cannot be instrumented in a plain-JUnit JVM, so a singleton
        // map with a null key stands in for a non-empty enchantment set.
        when(meta.getEnchants()).thenReturn(
                Collections.<org.bukkit.enchantments.Enchantment, Integer>singletonMap(null, 1));
        when(item.getItemMeta()).thenReturn(meta);
        var player = playerWithHand(item);

        var itemReq = new SkillDefinition.ItemRequirement(
                "possession", "#c:enchantable", "MAIN_HAND", 1, 0.0, true);
        var req = new SkillDefinition.Requirements(new ConstantEvaluator(0.0), List.of(), List.of(itemReq), null, null);
        assertTrue(engine.check(player, "s", "a", req, 10, 5).success());
    }
}
