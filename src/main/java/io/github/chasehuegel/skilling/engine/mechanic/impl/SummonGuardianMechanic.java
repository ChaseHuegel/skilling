package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Map;

/**
 * Summons a scaled, friendly "arcane guardian" beside the player on a right-click
 * catalyst cast.
 *
 * <p>The guardian is a normal vanilla {@link IronGolem} — a permanently friendly
 * defender that fights hostile mobs — spawned at the player's feet on a
 * right-click (air or block). Its stats come from evaluator parameter presets
 * that land below/above vanilla: {@code scale} (0.7 = 30% smaller), {@code speed}
 * (1.5 = 50% faster) and {@code damage} (0.7 = 30% weaker), each applied as a
 * multiply-scalar-1 modifier under a stable key so repeated summons replace
 * rather than stack. The optional {@code name} constants the guardian (e.g.
 * {@code Arcane Guardian}).
 *
 * <p>This is the catalyst-summoned counterpart to the block-built {@code
 * core:clay_golem}: instead of reading a clay scaffold it reads the cast
 * interaction, so the wizard summons a guardian with the same tome-and-catalyst
 * gesture as their other magic. The result is a plain vanilla IronGolem, so
 * uninstalling the plugin leaves safe vanilla entity state (safe unplug).
 *
 * <p><b>YAML key:</b> {@code core:summon_guardian}
 * <br>Params: {@code scale} (default 0.7), {@code speed} (default 1.5),
 * {@code damage} (default 0.7), {@code name} (optional custom name)
 */
public final class SummonGuardianMechanic implements SkillMechanic {

    /** Lower-bound sanity clamp so a bad multiplier cannot unbalance a guardian. */
    private static final double MIN_MULTIPLIER = 0.1;
    private static final double MAX_MULTIPLIER = 3.0;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof PlayerInteractEvent interact)) return false;
        if (interact.getAction() != Action.RIGHT_CLICK_AIR
                && interact.getAction() != Action.RIGHT_CLICK_BLOCK) return false;

        double scale = preset(params, "scale", 0.7);
        double speed = preset(params, "speed", 1.5);
        double damage = preset(params, "damage", 0.7);

        Location spawn = player.getLocation();
        IronGolem guardian = player.getWorld().spawn(spawn, IronGolem.class,
                CreatureSpawnEvent.SpawnReason.CUSTOM, false, g -> {});
        apply(guardian, Attribute.SCALE, "skilling:scale", scale);
        apply(guardian, Attribute.MOVEMENT_SPEED, "skilling:speed", speed);
        apply(guardian, Attribute.ATTACK_DAMAGE, "skilling:damage", damage);

        Object name = params.get("name");
        if (name != null && !String.valueOf(name).isBlank()) {
            guardian.customName(Component.text(String.valueOf(name)));
        }
        return true;
    }

    /**
     * Applies a multiply-scalar-1 modifier to {@code attrib} under a stable key, so
     * the guardian's stat lands at {@code base * multiplier} and any prior modifier
     * carrying the same key is replaced instead of stacking.
     *
     * @param guardian   the spawned guardian
     * @param attrib     the attribute to scale
     * @param keyString  the stable modifier key
     * @param multiplier the scale value (1.0 leaves the attribute at base)
     */
    private static void apply(IronGolem guardian, Attribute attrib, String keyString, double multiplier) {
        AttributeInstance inst = guardian.getAttribute(attrib);
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
     * constant cannot spawn an absurdly oversized or unscalable guardian.
     *
     * @param params    the mechanic parameters
     * @param key       the parameter key
     * @param fallback  the default value when absent
     * @return the clamped multiplier
     */
    private static double preset(Map<String, Object> params, String key, double fallback) {
        Object raw = params.get(key);
        double value = raw instanceof Number n ? n.doubleValue() : fallback;
        return Math.max(MIN_MULTIPLIER, Math.min(value, MAX_MULTIPLIER));
    }
}