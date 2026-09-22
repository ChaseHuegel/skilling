package io.github.chasehuegel.skilling.tag;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * End-to-end guard over the bundled content: every custom tag defined in
 * {@code tags/base.yml} must load, and every bundled skill must parse against
 * those tags.
 *
 * <p>This closes a coverage hole that let a stale material name slip into the
 * bundled {@code tame_offerings} tag: {@code CustomTagLoader} treats a single
 * bad entry as a malformed file and drops every tag in {@code base.yml}, so the
 * whole tag store goes empty and every skill referencing a {@code #c:} tag is
 * skipped at load. The parse-focused {@code SkillYamlValidationTest} uses a
 * never-loaded {@link CustomTagLoader} (which defers tag existence checks) and
 * therefore cannot see this class of breakage; this test loads the real bundled
 * tags first.
 */
class BundledTagsAndSkillsConsistencyTest {

    /** Every {@code custom_tags} key shipped in {@code tags/base.yml}. */
    private static final Set<String> BUNDLED_TAGS = Set.of(
            "ores", "logs", "gems", "stone", "excavatable", "veinminer",
            "stone_products", "redstone_components", "herbs", "heavy_weapons", "light_weapons", "tools",
            "leather_armor", "light_armor", "medium_armor", "heavy_armor",
            "unarmored", "bows", "instruments", "fishing_rods", "holy_blocks",
            "crops", "raw_crops", "tame_offerings", "foods", "campfire_foods",
            "campfires", "potions", "shields",
            "wooden_products", "carpentry_joinery", "shovels", "suspicious_blocks",
            "pressure_plates", "sculk_sensors", "trip_traps",
            "construction_blocks", "tailoring_products"
    );

    @TempDir
    Path tempDir;

    @Test
    void bundledTagsAndSkillsAreConsistent() throws Exception {
        Path tagsDir = tempDir.resolve("tags");
        Files.createDirectories(tagsDir);
        Files.copy(bundledResource("tags/base.yml"), tagsDir.resolve("base.yml"));

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        for (File file : bundledResourceFiles("skills")) {
            Files.copy(file.toPath(), skillsDir.resolve(file.getName()));
        }

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
            @SuppressWarnings("unchecked")
            Tag<Material> tag = mock(Tag.class);
            when(tag.getValues()).thenReturn(Set.of(Material.STONE));
            when(Bukkit.getTag(anyString(), any(NamespacedKey.class), eq(Material.class))).thenReturn(tag);
            @SuppressWarnings("unchecked")
            Tag<EntityType> entityTag = mock(Tag.class);
            when(entityTag.getValues()).thenReturn(Set.of(EntityType.ZOMBIE));
            when(Bukkit.getTag(anyString(), any(NamespacedKey.class), eq(EntityType.class)))
                    .thenReturn(entityTag);

            CustomTagLoader loader = new CustomTagLoader();
            loader.loadDirectory(tagsDir.toFile());

            for (String key : BUNDLED_TAGS) {
                assertTrue(loader.getKeys().contains("#c:" + key),
                        "bundled tag #c:" + key + " did not load from tags/base.yml");
            }
            assertTrue(loader.getEntityKeys().contains("#c:undead"),
                    "bundled entity tag #c:undead did not load from tags/base.yml");
            for (String key : List.of("humanoid", "pickpocket_piglin", "pickpocket_undead",
                    "pickpocket_raid", "pickpocket_ender", "pickpocket_pocket")) {
                assertTrue(loader.getEntityKeys().contains("#c:" + key),
                        "bundled entity tag #c:" + key + " did not load from tags/base.yml");
            }

            SkillManager manager = io.github.chasehuegel.skilling.TestSkillManager.newWith(reg -> {},
                    new TagResolver(loader));
            manager.loadSkills(skillsDir.toFile());

            for (File file : bundledResourceFiles("skills")) {
                String id = file.getName().replace(".yml", "");
                assertNotNull(manager.getSkills().get(id),
                        "bundled skill '" + id + "' was skipped; its tags likely failed to load");
            }
        }
    }

    private static java.io.InputStream bundledResource(String path) {
        var stream = BundledTagsAndSkillsConsistencyTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path + " not found on the classpath");
        return stream;
    }

    private static List<File> bundledResourceFiles(String dir) {
        java.net.URL url = BundledTagsAndSkillsConsistencyTest.class.getClassLoader().getResource(dir);
        assertNotNull(url, dir + " not found on the classpath");
        try {
            File folder = new File(url.toURI());
            File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
            assertNotNull(files, dir + " resource folder is empty or unreadable");
            return Arrays.asList(files);
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("Invalid resource URI for " + dir, e);
        }
    }
}
