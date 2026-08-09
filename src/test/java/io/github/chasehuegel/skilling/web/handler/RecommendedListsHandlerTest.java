package io.github.chasehuegel.skilling.web.handler;

import io.javalin.http.Context;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the recommended-list endpoints filter and serialize the live
 * registries. Registries are injected as mocks so the filter rules are
 * asserted without a live server.
 *
 * <p>{@link Material} is mocked per instance because {@code isItem()} reads the
 * live ITEM registry, which the test {@code FakeRegistryAccess} leaves empty.
 */
class RecommendedListsHandlerTest {

    private Context ctx() {
        return mock(Context.class, RETURNS_SELF);
    }

    @SuppressWarnings("unchecked")
    private Registry<Material> materialRegistry() {
        return mock(Registry.class);
    }

    private Material itemMaterial(String key) {
        Material m = mock(Material.class);
        when(m.isLegacy()).thenReturn(false);
        when(m.isItem()).thenReturn(true);
        when(m.getKey()).thenReturn(NamespacedKey.minecraft(key));
        return m;
    }

    @Test
    void materialsKeepsItemMaterialsAndDropsAir() {
        Registry<Material> registry = materialRegistry();
        Material stone = itemMaterial("stone");
        Material air = mock(Material.class);
        when(air.isItem()).thenReturn(false);
        when(registry.stream()).thenReturn(Stream.of(stone, air));

        Context ctx = ctx();
        new RecommendedListsHandler(registry, mock(Registry.class), mock(Registry.class),
                mock(Registry.class), List::of).materials(ctx);

        verify(ctx).json(Map.of("materials", List.of("minecraft:stone")));
    }

    @Test
    void materialsAreSortedAlphabetically() {
        Registry<Material> registry = materialRegistry();
        Material pickaxe = itemMaterial("diamond_pickaxe");
        Material apple = itemMaterial("apple");
        Material bow = itemMaterial("bow");
        when(registry.stream()).thenReturn(Stream.of(pickaxe, apple, bow));

        Context ctx = ctx();
        new RecommendedListsHandler(registry, mock(Registry.class), mock(Registry.class),
                mock(Registry.class), List::of).materials(ctx);

        verify(ctx).json(Map.of("materials",
                List.of("minecraft:apple", "minecraft:bow", "minecraft:diamond_pickaxe")));
    }

    @Test
    void soundsSerializeRegistryKeys() {
        Registry<Sound> registry = mock(Registry.class);
        when(registry.keyStream()).thenReturn(Stream.of(NamespacedKey.minecraft("entity.experience_orb.pickup")));

        Context ctx = ctx();
        new RecommendedListsHandler(mock(Registry.class), registry, mock(Registry.class),
                mock(Registry.class), List::of).sounds(ctx);

        verify(ctx).json(Map.of("sounds", List.of("minecraft:entity.experience_orb.pickup")));
    }

    @Test
    void particlesSerializeRegistryKeys() {
        Registry<Particle> registry = mock(Registry.class);
        when(registry.keyStream()).thenReturn(Stream.of(NamespacedKey.minecraft("flame")));

        Context ctx = ctx();
        new RecommendedListsHandler(mock(Registry.class), mock(Registry.class), registry,
                mock(Registry.class), List::of).particles(ctx);

        verify(ctx).json(Map.of("particles", List.of("minecraft:flame")));
    }

    @Test
    void entitiesKeepPlayerFacingTypesAndDropMarkers() {
        Registry<EntityType> registry = mock(Registry.class);
        when(registry.stream()).thenReturn(Stream.of(EntityType.ZOMBIE, EntityType.MARKER));

        Context ctx = ctx();
        new RecommendedListsHandler(mock(Registry.class), mock(Registry.class), mock(Registry.class),
                registry, List::of).entities(ctx);

        verify(ctx).json(Map.of("entities", List.of("minecraft:zombie")));
    }

    @Test
    void entitiesDropSpawnableButNonPlayerFacingAreaEffectCloud() {
        Registry<EntityType> registry = mock(Registry.class);
        when(registry.stream()).thenReturn(Stream.of(EntityType.ZOMBIE, EntityType.AREA_EFFECT_CLOUD));

        Context ctx = ctx();
        new RecommendedListsHandler(mock(Registry.class), mock(Registry.class), mock(Registry.class),
                registry, List::of).entities(ctx);

        verify(ctx).json(Map.of("entities", List.of("minecraft:zombie")));
    }

    @Test
    void tagsAreDeduplicatedAndSorted() {
        Context ctx = ctx();
        new RecommendedListsHandler(mock(Registry.class), mock(Registry.class), mock(Registry.class),
                mock(Registry.class), () -> List.of("minecraft:logs", "minecraft:planks", "minecraft:logs"))
                .tags(ctx);

        verify(ctx).json(Map.of("tags", List.of("minecraft:logs", "minecraft:planks")));
    }

    @Test
    void defaultHandlerWireUpSmokeTest() {
        RecommendedListsHandler handler = new RecommendedListsHandler();

        Context matCtx = ctx();
        handler.materials(matCtx);
        @SuppressWarnings("unchecked")
        Map<String, Object> materialsBody = jsonBody(matCtx);
        assertInstanceOf(List.class, materialsBody.get("materials"),
                "the materials endpoint must return a list");

        Context ctx = ctx();
        handler.tags(ctx);
        @SuppressWarnings("unchecked")
        Map<String, Object> tagsBody = jsonBody(ctx);
        assertEquals(List.of(), tagsBody.get("tags"),
                "the fake registry access provides no tags in unit tests");

        Context entCtx = ctx();
        handler.entities(entCtx);
        @SuppressWarnings("unchecked")
        Map<String, Object> entitiesBody = jsonBody(entCtx);
        assertTrue(entitiesBody.get("entities") instanceof List,
                "the entities endpoint must return a list");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonBody(Context ctx) {
        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(ctx).json(captor.capture());
        return (Map<String, Object>) captor.getValue();
    }
}
