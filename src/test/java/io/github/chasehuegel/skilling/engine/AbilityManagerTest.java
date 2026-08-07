package io.github.chasehuegel.skilling.engine;

import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link AbilityManager} directory loading: recursion into
 * subfolders, first-wins on a duplicate id, warn-and-skip for malformed or
 * id-less files, and an empty registry for a missing directory.
 */
class AbilityManagerTest {

    @TempDir
    Path tempDir;

    private static final String ABILITY = """
            id: vein_miner
            display_name: "Vein Miner"
            trigger: "block_break"
            unlock_level: 25
            """;

    @Test
    void loadsRecursivelyFromSubfolders() throws Exception {
        Path dir = tempDir.resolve("abilities");
        Files.createDirectories(dir.resolve("sub"));
        Files.writeString(dir.resolve("a.yml"), ABILITY);
        Files.writeString(dir.resolve("sub/b.yml"), "id: auto_smelt\ntrigger: block_break\n");

        var manager = new AbilityManager();
        manager.loadAbilities(dir.toFile());

        assertEquals(Set.of("vein_miner", "auto_smelt"), manager.getAbilities().keySet(),
                "abilities in subfolders must register under the same registry");
    }

    @Test
    void firstDefinitionWinsOnDuplicateId() throws Exception {
        Path dir = tempDir.resolve("abilities");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("a.yml"), ABILITY);
        Files.writeString(dir.resolve("b.yml"), "id: vein_miner\nunlock_level: 50\n");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
            var manager = new AbilityManager();
            manager.loadAbilities(dir.toFile());

            assertEquals(25,
                    ((Number) manager.getRaw("vein_miner").get("unlock_level")).intValue(),
                    "the first loaded file must win on a duplicate id");
        }
    }

    @Test
    void malformedFileIsSkipped() throws Exception {
        Path dir = tempDir.resolve("abilities");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("good.yml"), ABILITY);
        Files.writeString(dir.resolve("notamap.yml"), "- just\n- a list\n");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
            var manager = new AbilityManager();
            manager.loadAbilities(dir.toFile());

            assertEquals(Set.of("vein_miner"), manager.getAbilities().keySet(),
                    "a non-map file must be skipped, not fail the load");
        }
    }

    @Test
    void fileWithoutIdIsSkipped() throws Exception {
        Path dir = tempDir.resolve("abilities");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("good.yml"), ABILITY);
        Files.writeString(dir.resolve("no-id.yml"), "display_name: No Id\n");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
            var manager = new AbilityManager();
            manager.loadAbilities(dir.toFile());

            assertNull(manager.getRaw("no-id"),
                    "a file without a non-blank string id must be skipped");
            assertEquals(Set.of("vein_miner"), manager.getAbilities().keySet());
        }
    }

    @Test
    void missingDirectoryIsEmptyRegistry() {
        var manager = new AbilityManager();
        manager.loadAbilities(tempDir.resolve("does-not-exist").toFile());
        assertTrue(manager.getAbilities().isEmpty());
        assertNull(manager.getRaw("anything"));
    }

    @Test
    void clearDropsEveryAbility() throws Exception {
        Path dir = tempDir.resolve("abilities");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("a.yml"), ABILITY);

        var manager = new AbilityManager();
        manager.loadAbilities(dir.toFile());
        assertTrue(manager.getAbilities().containsKey("vein_miner"));

        manager.clear();
        assertTrue(manager.getAbilities().isEmpty());
    }
}
