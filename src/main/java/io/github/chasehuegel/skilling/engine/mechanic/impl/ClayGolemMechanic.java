package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import java.util.Map;

/**
 * Builds a scaled "clay golem" when a player completes a carved-pumpkin golem
 * scaffold out of clay blocks instead of iron blocks.
 *
 * <p>On removing the ability's {@code block_place} trigger the meadow-building
 * iron golems never fire because the scaffold uses clay, not iron (vanilla spawns
 * a golem only from the iron-block layout). Placing a carved pumpkin as the head
 * asks this mechanic to scan the four expected scaffold positions relative to the
 * head:
 * <ul>
 *   <li>{@code (0,-1,0)} upper body clay</li>
 *   <li>{@code (-1,-1,0)} left arm clay</li>
 *   <li>{@code (1,-1,0)} right arm clay</li>
 *   <li>{@code (0,-2,0)} lower body clay</li>
 * </ul>
 * When all four are clay the head and the four scaffold blocks are cleared and an
 * {@link IronGolem} is spawned with its feet on the block below the structure,
 * centered over the footprint. A pattern miss is a no-op (returns {@code false}),
 * so any other carved-pumpkin placement spends nothing.
 *
 * <p>The golem's stats come from evaluator parameters constructed from ratio
 * presets that land below/above vanilla: {@code scale} (0.7 = 30% smaller,
 * applied to {@code minecraft:scale}), {@code speed_multiplier} (1.5 = 50%
 * faster, applied to {@code minecraft:movement_speed}) and {@code damage_multiplier}
 * (0.7 = 30% weaker, applied to {@code minecraft:attack_damage}). Each modifier is
 * a multiply-scalar-1 value of {@code (preset - 1)} on a stable key, so re-scaling
 * replaces rather than stacks. Because the unlock is the level-100 capstone the
 * presets are constant by design; the evaluator wrapper keeps the schema uniform
 * and lets server owners retune them. The optional {@code name} constant names
 * the golem (e.g. {@code Clay Golem}) so it stays identifiable as a clay build
 * rather than a vanilla iron golem.
 *
 * <p>Modifiers are written as persistent entity NBT via {@code addModifier}, so a
 * loaded-and-reloaded chunk keeps the scaled stats; the result is still a normal
 * vanilla {@link IronGolem}, so uninstalling the plugin leaves safe vanilla
 * entity state (safe unplug). The five cleared blocks are set to air directly
 * (never routed through {@code BlockBreakEvent}), so they do not re-trigger XP or
 * ability processing.
 *
 * <p><b>YAML key:</b> {@code core:clay_golem}
 * <br>Params: {@code scale} (default 0.7), {@code speed_multiplier} (default 1.5),
 * {@code damage_multiplier} (default 0.7), {@code name} (optional custom name)
 */
public final class ClayGolemMechanic implements SkillMechanic {

    /** Lower-bound sanity clamp so a bad multiplier cannot unbalance a golem. */
    private static final double MIN_MULTIPLIER = 0.1;
    private static final double MAX_MULTIPLIER = 3.0;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof BlockPlaceEvent placed)) return false;
        if (placed.getBlockPlaced().getType() != Material.CARVED_PUMPKIN) return false;

        Block head = placed.getBlockPlaced();
        World world = head.getWorld();
        if (world == null) return false;

        Block body = head.getRelative(0, -1, 0);
        Block left = head.getRelative(-1, -1, 0);
        Block right = head.getRelative(1, -1, 0);
        Block legs = head.getRelative(0, -2, 0);
        if (body.getType() != Material.CLAY
                || left.getType() != Material.CLAY
                || right.getType() != Material.CLAY
                || legs.getType() != Material.CLAY) {
            return false;
        }

        clear(head);
        clear(body);
        clear(left);
        clear(right);
        clear(legs);

        double scale = preset(params, "scale", 0.7);
        double speed = preset(params, "speed_multiplier", 1.5);
        double damage = preset(params, "damage_multiplier", 0.7);

        // Feet rest on the block below the structure: head y - 3 is the floor, so
        // the entity's feet are one block up at head y - 2.
        Location spawn = new Location(world,
                head.getX() + 0.5, head.getY() - 2.0, head.getZ() + 0.5,
                player.getLocation().getYaw(), 0.0f);
        IronGolem golem = world.spawn(spawn, IronGolem.class, CreatureSpawnEvent.SpawnReason.CUSTOM, false, g -> { });
        apply(golem, Attribute.SCALE, "skilling:scale", scale);
        apply(golem, Attribute.MOVEMENT_SPEED, "skilling:movement_speed", speed);
        apply(golem, Attribute.ATTACK_DAMAGE, "skilling:attack_damage", damage);

        Object name = params.get("name");
        if (name != null && !String.valueOf(name).isBlank()) {
            golem.customName(Component.text(String.valueOf(name)));
        }
        return true;
    }

    /**
     * Applies a multiply-scalar-1 modifier to {@code attrib} under a stable key, so
     * the golem's stat lands at {@code base * multiplier} and any prior modifier
     * carrying the same key is replaced instead of stacking.
     *
     * @param golem      the spawned golem
     * @param attrib     the attribute to scale
     * @param multiplier the scale value (1.0 leaves the attribute at base)
     */
    private static void apply(IronGolem golem, Attribute attrib, String keyString, double multiplier) {
        AttributeInstance inst = golem.getAttribute(attrib);
        if (inst == null) return;
        NamespacedKey key = NamespacedKey.fromString(keyString);
        AttributeModifier existing = inst.getModifier(key);
        if (existing != null) {
            inst.removeModifier(key);
        }
        inst.addModifier(new AttributeModifier(key,
                multiplier - 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    /**
     * Reads a preset parameter as a double, clamped to a sane range so a bad
     * constant cannot spawn an absurdly oversized or unscalable golem.
     *
     * @param params  the mechanic parameters
     * @param key     the parameter key
     * @param fallback the default value when absent
     * @return the clamped multiplier
     */
    private static double preset(Map<String, Object> params, String key, double fallback) {
        Object raw = params.get(key);
        double value = raw instanceof Number n ? n.doubleValue() : fallback;
        return Math.max(MIN_MULTIPLIER, Math.min(value, MAX_MULTIPLIER));
    }

    private static void clear(Block block) {
        block.setType(Material.AIR, false);
    }
}