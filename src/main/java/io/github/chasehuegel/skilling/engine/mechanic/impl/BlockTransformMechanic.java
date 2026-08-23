package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

/**
 * Transforms a clicked block into another block when the player right-clicks it
 * while holding a matching catalyst, consuming one catalyst per affected block.
 *
 * <p>This is the terrain analogue of {@code core:transmute}: instead of swapping
 * a held item for a product stack, it swaps the clicked <em>block</em> for a
 * {@code result} block type. The catalyst (held main-hand item matching
 * {@code catalyst}) is consumed on a successful transform and the vanilla click
 * is cancelled so the block interaction does not also run. Which blocks may be
 * transformed is scoped by the ability's own {@code target} filter (e.g. a
 * {@code #c:herbal_soil} tag), keeping the engine's no-op-does-not-consume rule:
 * a mechanic whose catalyst does not match the held item spends nothing.
 *
 * <p><b>YAML key:</b> {@code core:block_transform}
 * <br>Params: {@code catalyst} (required held-item material, consumed per
 * transform), {@code result} (required block material the clicked block becomes),
 * {@code catalyst_count} (default 1, how many catalysts are consumed)
 */
public final class BlockTransformMechanic implements SkillMechanic {

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interact)) return false;
        if (interact.getAction() != Action.RIGHT_CLICK_BLOCK) return false;

        Material catalyst = Material.matchMaterial(String.valueOf(params.getOrDefault("catalyst", "")));
        if (catalyst == null) return false;
        Material result = Material.matchMaterial(String.valueOf(params.getOrDefault("result", "")));
        if (result == null) return false;

        int catalystCount = count(params.get("catalyst_count"), 1);
        if (catalystCount < 1) return false;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != catalyst) return false;
        if (held.getAmount() < catalystCount) return false;

        Block clicked = interact.getClickedBlock();
        if (clicked == null || isAir(clicked.getType())) return false;

        // Consume the catalyst batch and cancel the vanilla terrain interaction.
        held.setAmount(held.getAmount() - catalystCount);
        player.getInventory().setItemInMainHand(held);
        interact.setCancelled(true);

        clicked.setType(result, false);
        return true;
    }

    private static boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    private static int count(Object raw, int fallback) {
        if (raw instanceof Number n) return n.intValue();
        return fallback;
    }
}