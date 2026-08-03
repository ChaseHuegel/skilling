package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Reflects damage back to the attacker.
 *
 * <p>The reflect damages the attacker via {@code attacker.damage(...)}, which
 * synchronously re-enters the damage pipeline. Two reflecting entities would
 * ping-pong reflects until a {@link StackOverflowError} on the main thread, so
 * a re-entrancy guard tracks the entities currently inside a reflect cascade:
 * an attacker already in that set has its reflect suppressed, so a hit →
 * reflect → hit → reflect cycle reflects at most once per entity per incoming
 * hit. Guard state is identity-scoped and cleared in a {@code finally}, so a
 * later hit reflects again. This mechanic runs on the Bukkit main thread only.
 *
 * <p>YAML key: {@code core:thorns_damage}
 * <br>Params: {@code damage} (flat amount)
 */
public record ThornsDamageMechanic() implements SkillMechanic {

    private static final Set<LivingEntity> REFLECTING =
            Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent de)) return false;
        if (!de.getEntity().equals(player)) return false;
        double damage = ((Number) params.getOrDefault("damage", 0)).doubleValue();
        if (damage <= 0) return false;
        LivingEntity attacker = EntityDamageResolver.resolveDamagerEntity(de);
        if (attacker == null) return false;
        // The attacker is already receiving a reflected hit inside this cascade;
        // reflecting again would ping-pong forever, so suppress the reflect.
        if (!REFLECTING.add(attacker)) return true;
        try {
            attacker.damage(damage, player);
        } finally {
            REFLECTING.remove(attacker);
        }
        return true;
    }
}
