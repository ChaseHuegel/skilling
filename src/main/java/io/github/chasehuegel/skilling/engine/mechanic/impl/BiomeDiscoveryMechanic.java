package io.github.chasehuegel.skilling.engine.mechanic.impl;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.mechanic.UnlockMechanic;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Grants a scaling movement-speed bonus per distinct biome a player has entered,
 * capped at {@code max_biomes}. Discovering a new biome (on the {@code map_explore}
 * trigger, so it rewards mapping the world) adds it to the player's persisted
 * progress and re-applies the attribute; the bonus grows both as the player
 * discovers more land and as the ability's per-biome {@code amount} levels.
 *
 * <p>The discovered set is stored in the player's generic per-player progress
 * store under {@link #PROGRESS_KEY} (a comma-joined list of biome keys), so it
 * survives restarts. Because it implements {@link UnlockMechanic}, the engine
 * re-runs it on join, reload, and level change, re-applying the current bonus
 * from the persisted count; {@code event} may be null then, in which case only
 * the attribute re-apply happens (no discovery).
 *
 * <p>The movement-speed modifier is transient and held under a stable UUID that
 * replace-not-stacks, mirroring {@code core:persistent_attribute}.
 *
 * <p><b>YAML key:</b> {@code core:biome_discovery}
 * <br>Params: {@code attribute} (attribute to grow, default {@code minecraft:movement_speed}),
 * {@code amount} (level-scaled additive amount per discovered biome), {@code max_biomes}
 * (cap on how many distinct biomes count), {@code uuid} (stable modifier UUID)
 */
public record BiomeDiscoveryMechanic() implements UnlockMechanic {

    /** Progress-store key under which the discovered biome set is persisted. */
    public static final String PROGRESS_KEY = "biome_discovery";

    private static final int DEFAULT_MAX_BIOMES = 40;

    @Override
    public boolean execute(Player player, Map<String, Object> params, Event event) {
        PlayerProfile profile = profile(player.getUniqueId());
        if (profile == null) return false;

        Attribute attribute = ModifyAttributeMechanic
                .resolveAttribute(params.getOrDefault("attribute", "minecraft:movement_speed"));
        double perBiome = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
        int maxBiomes = ((Number) params.getOrDefault("max_biomes", DEFAULT_MAX_BIOMES))
                .intValue();
        Object rawUuid = params.get("uuid");
        if (rawUuid == null || perBiome <= 0) return false;
        UUID uuid = AttributeModifierHelper.resolveUuid(rawUuid);

        Set<String> biomes = load(profile);
        if (event != null) {
            Biome biome = player.getWorld().getBiome(player.getLocation());
            if (biomes.add(biome.getKey().asString())) {
                store(profile, biomes);
            }
        }

        double total = perBiome * Math.min(biomes.size(), maxBiomes);
        return PersistentAttributeMechanic.applyPersistent(player, attribute, uuid, total);
    }

    private static PlayerProfile profile(UUID uuid) {
        Skilling plugin = Skilling.getInstance();
        if (plugin == null) return null;
        return plugin.getProfileManager().getProfile(uuid);
    }

    private static Set<String> load(PlayerProfile profile) {
        Set<String> set = new LinkedHashSet<>();
        String raw = profile.getProgress().get(PROGRESS_KEY);
        if (raw != null && !raw.isBlank()) {
            set.addAll(Arrays.asList(raw.split(",")));
        }
        return set;
    }

    private static void store(PlayerProfile profile, Set<String> biomes) {
        profile.getProgress().put(PROGRESS_KEY, String.join(",", biomes));
        profile.markProgressDirty();
    }
}