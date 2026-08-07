package io.github.chasehuegel.skilling.engine;

import org.bukkit.Bukkit;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of reusable ability definitions loaded from an {@code abilities}
 * data folder.
 *
 * <p>Each {@code .yml} file holds one ability map keyed by its {@code id}.
 * Files load recursively in sorted relative-path order. A file that is not a
 * YAML map or lacks a non-blank string {@code id} logs a warning and is
 * skipped. On an id conflict the first file loaded wins and a warning is
 * logged. A missing directory is an empty registry.
 *
 * <p>The registry stores raw ability maps only. Full schema validation happens
 * when {@link SkillManager} parses a skill that references a registered id, so
 * standalone ability files only need shape plus an id.
 */
public final class AbilityManager {

    private volatile Map<String, Map<String, Object>> abilities = Map.of();

    /**
     * Loads every {@code .yml} file in the given directory (recursively) into
     * the registry keyed by ability id. Files are processed in sorted
     * relative-path order; on an id conflict the first definition wins. A
     * missing directory is an empty registry.
     *
     * @param dir the abilities data directory
     */
    public void loadAbilities(File dir) {
        Map<String, Map<String, Object>> built = new LinkedHashMap<>();
        if (dir.exists() && dir.isDirectory()) {
            for (File file : collectYamlFiles(dir)) {
                try {
                    Map<String, Object> raw = parseAbilityFile(file);
                    Object idObj = raw.get("id");
                    if (!(idObj instanceof String id) || id.isBlank()) {
                        Bukkit.getLogger().warning(
                                "Skipping ability file " + file.getName() + ": missing non-blank 'id'");
                        continue;
                    }
                    if (built.containsKey(id)) {
                        Bukkit.getLogger().warning("Duplicate ability id '" + id
                                + "' in " + file.getName() + "; keeping the first definition");
                        continue;
                    }
                    built.put(id, raw);
                } catch (IOException e) {
                    Bukkit.getLogger().warning(
                            "Skipping unreadable ability file " + file.getName() + ": " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning(
                            "Skipping malformed ability file " + file.getName() + ": " + e.getMessage());
                }
            }
        }
        this.abilities = Collections.unmodifiableMap(built);
    }

    /**
     * Returns the raw ability map registered under the given id.
     *
     * @param id the ability id
     * @return the raw ability map, or null when the id is not registered
     */
    public Map<String, Object> getRaw(String id) {
        return abilities.get(id);
    }

    /**
     * Returns all registered raw ability maps keyed by id.
     *
     * @return an immutable snapshot of the registered abilities
     */
    public Map<String, Map<String, Object>> getAbilities() {
        return abilities;
    }

    /**
     * Drops every registered ability, leaving the registry empty.
     */
    public void clear() {
        abilities = Map.of();
    }

    private static Map<String, Object> parseAbilityFile(File file) throws IOException {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        Object raw;
        try {
            raw = new Yaml().load(content);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed to parse ability file: " + file, e);
        }
        if (raw == null) return Map.of();
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(
                    "top-level YAML is not a map, got " + raw.getClass().getSimpleName());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }

    private static List<File> collectYamlFiles(File dir) {
        List<File> files = new ArrayList<>();
        try (var stream = Files.walk(dir.toPath())) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yml"))
                    .sorted(Comparator.comparing(p -> dir.toPath().relativize(p).toString()))
                    .forEach(p -> files.add(p.toFile()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to walk abilities directory: " + dir, e);
        }
        return files;
    }
}
