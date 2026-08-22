package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.engine.mechanic.UnlockMechanic;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * Grants a level-scaled persistent attribute modifier only while the player
 * wears a named armor set, and strips it as soon as any slot is swapped out.
 *
 * <p>This is the armor-gated cousin of {@code core:persistent_attribute}: the
 * bonus (e.g. the heavy-armor skill's always-on armor and armor-toughness
 * mastery) is only ever active while the player is actually armored in the
 * {@code equip_tag} set, not as a free unconditional stat. Equipment state is
 * reconciled event-driven — on the {@code level_up} trigger, join, reload, and
 * {@code /skills setlevel}/{@code reset} via {@link UnlockMechanic}, and on
 * armor-changing inventory events through {@link #reevaluate(Player)} — with no
 * per-tick task, per the zero-constant-ticking pillar.
 *
 * <p>The reward itself is applied through
 * {@link PersistentAttributeMechanic#applyPersistent}, so it shares the same
 * transient-modifier semantics: a stable marker key, replace-not-stack per
 * {@code uuid}, idempotence per amount, and removal on a non-positive amount.
 * The level-scaled {@code amount} is captured on each {@link #execute} (which
 * the engine runs at the player's current level); an armor change re-uses that
 * captured value because a level change always triggers another reconcile.
 *
 * <p><b>YAML key:</b> {@code core:equipment_attribute}
 * <br>Params: {@code attribute} (namespaced key), {@code amount} (level-scaled
 * evaluator), {@code uuid} (required stable modifier UUID), {@code equip_tag}
 * (a material or {@code #...} tag reference; every armor slot must match)
 */
public final class EquipmentAttributeMechanic implements UnlockMechanic {

    /** Per-player, per-modifier bindings captured from the last reconcile. */
    private static final Map<UUID, Map<UUID, Binding>> BINDINGS = new ConcurrentHashMap<>();

    /** Tag resolver wired by the engine at enable; null keeps the mechanic inert. */
    private static volatile TagResolver tagResolver;

    /** A single ability's captured armor-gated reward. */
    private record Binding(Attribute attribute, double amount, EnumSet<Material> materials) {}

    /**
     * Wires the tag resolver used to flatten an {@code equip_tag} into an armor
     * set. Called by the engine after the resolver is built; a null resolver
     * makes the mechanic inert (never grants), mirroring the fail-closed state
     * filter behavior.
     *
     * @param resolver the active tag resolver
     */
    public static void setTagResolver(TagResolver resolver) {
        tagResolver = resolver;
    }

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        Object rawUuid = params.get("uuid");
        if (rawUuid == null) return false;
        Attribute attribute = ModifyAttributeMechanic.resolveAttribute(params.get("attribute"));
        double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        EnumSet<Material> materials = resolveMaterials(params.get("equip_tag"));
        UUID uuid = AttributeModifierHelper.resolveUuid(rawUuid);

        BINDINGS.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(uuid, new Binding(attribute, amount, materials));
        reconcile(player, uuid, new Binding(attribute, amount, materials));
        return true;
    }

    /**
     * Re-reads every captured binding for the player and applies or strips each
     * modifier to match the player's current equipment. Event-driven per
     * inventory change; a player with no recorded ability is a no-op.
     *
     * @param player the player whose equipment may have changed
     */
    public static void reevaluate(Player player) {
        Map<UUID, Binding> bindings = BINDINGS.get(player.getUniqueId());
        if (bindings == null) return;
        bindings.forEach((uuid, binding) -> reconcile(player, uuid, binding));
    }

    /**
     * Drops every captured binding on reconcile so a de-level, reset, or removed
     * skill is recomputed from scratch. The modifiers themselves are cleared by
     * {@link PersistentAttributeMechanic#stripPersistentModifiers} before the
     * reconcile re-runs the active unlocks.
     */
    public static void stripAll() {
        BINDINGS.clear();
    }

    /**
     * Removes a player's captured bindings on quit, so they are rebuilt fresh
     * from their level on next join.
     *
     * @param playerId the leaving player's UUID
     */
    public static void clear(UUID playerId) {
        BINDINGS.remove(playerId);
    }

    /**
     * Test-only seam: the number of captured bindings for a player.
     *
     * @param playerId the player's UUID
     * @return the number of recorded armor-gated bonuses
     */
    static int bindingCount(UUID playerId) {
        Map<UUID, Binding> bindings = BINDINGS.get(playerId);
        return bindings == null ? 0 : bindings.size();
    }

    private static void reconcile(Player player, UUID uuid, Binding binding) {
        boolean armored = matchesEquipped(player, binding.materials());
        // A non-positive amount removes the modifier: strip when unarmored,
        // apply the captured reward when armored. Idempotent per amount.
        PersistentAttributeMechanic.applyPersistent(
                player, binding.attribute(), uuid, armored ? binding.amount() : 0.0);
    }

    private static boolean matchesEquipped(Player player, EnumSet<Material> materials) {
        if (materials == null || materials.isEmpty()) return false;
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            Material type = piece == null ? Material.AIR : piece.getType();
            if (!materials.contains(type)) return false;
        }
        return true;
    }

    private static EnumSet<Material> resolveMaterials(Object rawEquipTag) {
        TagResolver resolver = tagResolver;
        if (resolver == null || rawEquipTag == null) return EnumSet.noneOf(Material.class);
        try {
            Set<Material> resolved = resolver.resolve(rawEquipTag.toString());
            return resolved.isEmpty() ? EnumSet.noneOf(Material.class) : EnumSet.copyOf(resolved);
        } catch (IllegalArgumentException ex) {
            return EnumSet.noneOf(Material.class);
        }
    }
}