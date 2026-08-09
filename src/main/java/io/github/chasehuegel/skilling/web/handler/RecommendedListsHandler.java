package io.github.chasehuegel.skilling.web.handler;

import io.javalin.http.Context;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * REST handler exposing exhaustive recommended lists for the Web GUI's
 * searchable editor controls.
 *
 * <p><b>GET /api/materials</b> — {@code {"materials": [...]}} every non-legacy
 * material that exists as an item.
 * <p><b>GET /api/sounds</b> — {@code {"sounds": [...]}} every registered sound
 * event.
 * <p><b>GET /api/particles</b> — {@code {"particles": [...]}} every registered
 * particle type.
 * <p><b>GET /api/entities</b> — {@code {"entities": [...]}} every spawnable
 * entity type.
 * <p><b>GET /api/tags/all</b> — {@code {"tags": [...]}} every vanilla block and
 * item tag key.
 *
 * <p>The lists come from the live Bukkit registries, so they always match the
 * running server's Minecraft version. They are static for the lifetime of a
 * server process, so the handler reads them on demand with no caching.
 */
public final class RecommendedListsHandler {

    /**
     * Entity types that are summonable but not player-facing, so they add noise
     * to a recommendation list. Excluded from the entity endpoint.
     */
    private static final Set<EntityType> NON_PLAYER_FACING = Set.of(
            EntityType.AREA_EFFECT_CLOUD, EntityType.BLOCK_DISPLAY, EntityType.INTERACTION,
            EntityType.ITEM_DISPLAY, EntityType.MARKER, EntityType.TEXT_DISPLAY);

    private final Registry<Material> materials;
    private final Registry<Sound> sounds;
    private final Registry<Particle> particles;
    private final Registry<EntityType> entities;
    private final Supplier<List<String>> vanillaTagKeys;

    /**
     * Wires the handler to the live Bukkit registries and the real vanilla tag
     * enumeration.
     */
    public RecommendedListsHandler() {
        this(Registry.MATERIAL, Registry.SOUND_EVENT, Registry.PARTICLE_TYPE, Registry.ENTITY_TYPE,
                RecommendedListsHandler::allVanillaTagKeys);
    }

    /**
     * Constructs a handler over injected sources. Package-private so unit tests
     * can stub the registries without a live server.
     *
     * @param materials      the material registry to enumerate
     * @param sounds         the sound-event registry to enumerate
     * @param particles      the particle registry to enumerate
     * @param entities       the entity-type registry to enumerate
     * @param vanillaTagKeys supplier of vanilla tag keys
     */
    RecommendedListsHandler(Registry<Material> materials, Registry<Sound> sounds,
                            Registry<Particle> particles, Registry<EntityType> entities,
                            Supplier<List<String>> vanillaTagKeys) {
        this.materials = materials;
        this.sounds = sounds;
        this.particles = particles;
        this.entities = entities;
        this.vanillaTagKeys = vanillaTagKeys;
    }

    /**
     * GET handler: serializes the item materials to JSON. Non-item blocks such
     * as air, water, and wall variants are excluded so the list stays
     * player-facing.
     *
     * @param ctx the current request context
     */
    public void materials(Context ctx) {
        List<String> keys = materials.stream()
                .filter(m -> !m.isLegacy() && m.isItem())
                .map(m -> m.getKey().asString())
                .sorted()
                .toList();
        ctx.json(Map.of("materials", keys));
    }

    /**
     * GET handler: serializes the registered sound events to JSON.
     *
     * @param ctx the current request context
     */
    public void sounds(Context ctx) {
        List<String> keys = sounds.keyStream()
                .map(NamespacedKey::asString)
                .sorted()
                .toList();
        ctx.json(Map.of("sounds", keys));
    }

    /**
     * GET handler: serializes the registered particle types to JSON.
     *
     * @param ctx the current request context
     */
    public void particles(Context ctx) {
        List<String> keys = particles.keyStream()
                .map(NamespacedKey::asString)
                .sorted()
                .toList();
        ctx.json(Map.of("particles", keys));
    }

    /**
     * GET handler: serializes the spawnable entity types to JSON. Non-spawnable
     * and non-player-facing markers such as {@code minecraft:marker} and
     * {@code minecraft:area_effect_cloud} are excluded so the list stays
     * player-facing.
     *
     * @param ctx the current request context
     */
    public void entities(Context ctx) {
        List<String> keys = entities.stream()
                .filter(et -> et.isSpawnable() && !NON_PLAYER_FACING.contains(et))
                .map(e -> e.getKey().asString())
                .sorted()
                .toList();
        ctx.json(Map.of("entities", keys));
    }

    /**
     * GET handler: serializes the vanilla block and item tag keys to JSON.
     * Block and item registries share most keys, so the result is deduplicated.
     *
     * @param ctx the current request context
     */
    public void tags(Context ctx) {
        List<String> keys = vanillaTagKeys.get().stream()
                .distinct()
                .sorted()
                .toList();
        ctx.json(Map.of("tags", keys));
    }

    /**
     * Collects the tag keys of every vanilla block and item tag from the live
     * registries. Package-private and pure for unit testing. Block and item tag
     * registries share most keys, so callers deduplicate.
     *
     * @return the tag keys as strings (e.g. {@code minecraft:logs})
     */
    static List<String> allVanillaTagKeys() {
        RegistryAccess access = RegistryAccess.registryAccess();
        List<String> keys = new ArrayList<>();
        for (var tag : access.getRegistry(RegistryKey.ITEM).getTags()) {
            keys.add(tag.tagKey().key().asString());
        }
        for (var tag : access.getRegistry(RegistryKey.BLOCK).getTags()) {
            keys.add(tag.tagKey().key().asString());
        }
        return keys;
    }
}
