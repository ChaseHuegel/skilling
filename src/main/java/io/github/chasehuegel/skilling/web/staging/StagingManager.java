package io.github.chasehuegel.skilling.web.staging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StagingManager {

    private static final Logger LOGGER = Logger.getLogger(StagingManager.class.getName());

    private final File stagingDir;
    private final File skillsDir;
    private final File configFile;
    private final File tagsFile;

    public StagingManager(File dataFolder) {
        this.stagingDir = new File(dataFolder, ".web_staging");
        this.skillsDir = new File(dataFolder, "skills");
        this.configFile = new File(dataFolder, "config.yml");
        this.tagsFile = new File(dataFolder, "tags.yml");
    }

    public File getStagingDir() {
        return stagingDir;
    }

    public File stagedSkillFile(String skillId) {
        File dir = new File(stagingDir, "skills");
        dir.mkdirs();
        return new File(dir, skillId + ".yml");
    }

    public File liveSkillFile(String skillId) {
        return new File(skillsDir, skillId + ".yml");
    }

    public boolean hasPendingChanges() {
        return statusFile().exists();
    }

    public StagingStatus status() {
        if (!hasPendingChanges()) {
            return new StagingStatus(false, 0, List.of(), null);
        }
        try {
            String json = Files.readString(statusFile().toPath(), StandardCharsets.UTF_8);
            var map = new com.google.gson.Gson().fromJson(json, java.util.Map.class);
            boolean pending = (boolean) map.getOrDefault("hasPendingChanges", false);
            double count = ((Number) map.getOrDefault("fileCount", 0)).doubleValue();
            List<String> files = (List<String>) map.getOrDefault("files", List.of());
            String lastModified = (String) map.get("lastModified");
            return new StagingStatus(pending, (int) count, files, lastModified);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to read staging status", e);
            return new StagingStatus(false, 0, List.of(), null);
        }
    }

    public void writeStatus(List<String> stagedFiles) {
        try {
            statusFile().getParentFile().mkdirs();
            var map = new LinkedHashMap<String, Object>();
            map.put("hasPendingChanges", true);
            map.put("fileCount", stagedFiles.size());
            map.put("files", stagedFiles);
            map.put("lastModified", Instant.now().toString());
            // Snapshot live file timestamps for conflict detection
            Map<String, Long> timestamps = new LinkedHashMap<>();
            for (String f : stagedFiles) {
                File live = resolveLiveFile(f);
                if (live.exists()) {
                    timestamps.put(f, live.lastModified());
                }
            }
            map.put("fileTimestamps", timestamps);
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(map);
            Files.writeString(statusFile().toPath(), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write staging status", e);
        }
    }

    public List<String> checkConflicts() {
        List<String> conflicts = new ArrayList<>();
        if (!statusFile().exists()) return conflicts;
        try {
            String json = Files.readString(statusFile().toPath(), StandardCharsets.UTF_8);
            var map = new com.google.gson.Gson().fromJson(json, Map.class);
            Map<String, Double> timestamps = (Map<String, Double>) map.get("fileTimestamps");
            if (timestamps == null) return conflicts;
            for (var entry : timestamps.entrySet()) {
                File live = resolveLiveFile(entry.getKey());
                if (live.exists() && live.lastModified() > entry.getValue().longValue()) {
                    conflicts.add(entry.getKey());
                }
            }
        } catch (Exception ignored) {}
        return conflicts;
    }

    public void clear() {
        if (stagingDir.exists()) {
            try {
                Files.walk(stagingDir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to clear staging directory", e);
            }
        }
    }

    private File resolveLiveFile(String stagedPath) {
        if (stagedPath.equals("tags.yml")) return tagsFile;
        if (stagedPath.equals("config.yml")) return configFile;
        if (stagedPath.startsWith("skills/")) {
            return new File(skillsDir, stagedPath.substring(7));
        }
        return new File(stagingDir, stagedPath);
    }

    public List<String> applyAndBackup() {
        if (!hasPendingChanges()) return List.of();

        // Check for conflicts first
        List<String> conflicts = checkConflicts();
        if (!conflicts.isEmpty()) {
            LOGGER.warning("Conflict detected: live files modified since staging: " + conflicts);
            return List.of(); // caller can check conflicts separately
        }

        List<String> applied = new ArrayList<>();
        try {
            File backupDir = new File(stagingDir, "backup/" + java.time.LocalDateTime.now().toString()
                .replace(":", "-"));
            backupDir.mkdirs();

            // Apply staged skills
            File stagedSkillsDir = new File(stagingDir, "skills");
            if (stagedSkillsDir.exists()) {
                File[] stagedFiles = stagedSkillsDir.listFiles((d, n) -> n.endsWith(".yml"));
                if (stagedFiles != null) {
                    for (File f : stagedFiles) {
                        File live = new File(skillsDir, f.getName());
                        backupFile(live, backupDir);
                        Files.copy(f.toPath(), live.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        applied.add("skills/" + f.getName());
                    }
                }
            }

            // Apply staged tags.yml
            File stagedTags = new File(stagingDir, "tags.yml");
            if (stagedTags.exists()) {
                backupFile(tagsFile, backupDir);
                Files.copy(stagedTags.toPath(), tagsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                applied.add("tags.yml");
            }

            // Apply staged config.yml
            File stagedConfig = new File(stagingDir, "config.yml");
            if (stagedConfig.exists()) {
                backupFile(configFile, backupDir);
                Files.copy(stagedConfig.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                applied.add("config.yml");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to apply staged changes", e);
        }
        return applied;
    }

    private static void backupFile(File source, File backupDir) {
        if (!source.exists()) return;
        try {
            File target = new File(backupDir, source.getName());
            target.getParentFile().mkdirs();
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to backup " + source, e);
        }
    }

    public void stageSkillFile(String skillId, String yamlContent) {
        try {
            File f = stagedSkillFile(skillId);
            f.getParentFile().mkdirs();
            Files.writeString(f.toPath(), yamlContent, StandardCharsets.UTF_8);
            updateStatusAdd("skills/" + skillId + ".yml");
        } catch (IOException e) {
            throw new RuntimeException("Failed to stage skill: " + skillId, e);
        }
    }

    public void stageTagsFile(String yamlContent) {
        try {
            File f = new File(stagingDir, "tags.yml");
            f.getParentFile().mkdirs();
            Files.writeString(f.toPath(), yamlContent, StandardCharsets.UTF_8);
            updateStatusAdd("tags.yml");
        } catch (IOException e) {
            throw new RuntimeException("Failed to stage tags.yml", e);
        }
    }

    public void stageConfigFile(String yamlContent) {
        try {
            File f = new File(stagingDir, "config.yml");
            f.getParentFile().mkdirs();
            Files.writeString(f.toPath(), yamlContent, StandardCharsets.UTF_8);
            updateStatusAdd("config.yml");
        } catch (IOException e) {
            throw new RuntimeException("Failed to stage config.yml", e);
        }
    }

    private void updateStatusAdd(String filePath) {
        var current = status();
        List<String> files = new ArrayList<>(current.files());
        if (!files.contains(filePath)) {
            files.add(filePath);
        }
        writeStatus(files);
    }

    private File statusFile() {
        return new File(stagingDir, "status.json");
    }

    public record StagingStatus(
        boolean hasPendingChanges,
        int fileCount,
        List<String> files,
        String lastModified
    ) {}
}
