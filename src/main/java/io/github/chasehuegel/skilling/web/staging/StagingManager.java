package io.github.chasehuegel.skilling.web.staging;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StagingManager {

    private static final Logger LOGGER = Logger.getLogger(StagingManager.class.getName());

    /**
     * Serializes all staging mutations (edits, status updates, apply, clear) so
     * concurrent web requests never lose entries or race each other.
     */
    private final ReentrantLock lock = new ReentrantLock();

    private final File stagingDir;
    private final File skillsDir;
    private final File configFile;
    private final File tagsFile;
    private final File guiFile;

    public StagingManager(File dataFolder) {
        this.stagingDir = new File(dataFolder, ".web_staging");
        this.skillsDir = new File(dataFolder, "skills");
        this.configFile = new File(dataFolder, "config.yml");
        this.tagsFile = new File(new File(dataFolder, "tags"), "base.yml");
        this.guiFile = new File(dataFolder, "gui.yml");
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

    /**
     * The staged gui.yml file, if a layout edit is pending.
     *
     * @return the staged gui.yml file
     */
    public File stagedGuiFile() {
        return new File(stagingDir, "gui.yml");
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
        lock.lock();
        try {
            statusFile().getParentFile().mkdirs();
            var map = new LinkedHashMap<String, Object>();
            map.put("hasPendingChanges", true);
            map.put("fileCount", stagedFiles.size());
            map.put("files", stagedFiles);
            map.put("lastModified", Instant.now().toString());
            // Snapshot live file fingerprints (content hashes, including "absent")
            // so conflict detection catches same-second and new-file edits.
            Map<String, String> fingerprints = new LinkedHashMap<>();
            for (String f : stagedFiles) {
                fingerprints.put(f, fingerprint(resolveLiveFile(f)));
            }
            map.put("fileFingerprints", fingerprints);
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(map);
            atomicWrite(statusFile(), json);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write staging status", e);
        } finally {
            lock.unlock();
        }
    }

    public List<String> checkConflicts() {
        if (!statusFile().exists()) return List.of();
        try {
            String json = Files.readString(statusFile().toPath(), StandardCharsets.UTF_8);
            var map = new com.google.gson.Gson().fromJson(json, Map.class);
            List<String> conflicts = new ArrayList<>();
            Object rawFingerprints = map.get("fileFingerprints");
            if (!(rawFingerprints instanceof Map<?, ?> fingerprints)) return List.of();
            for (var entry : fingerprints.entrySet()) {
                String stagedPath = entry.getKey().toString();
                String snapshot = entry.getValue() == null ? "absent" : entry.getValue().toString();
                String current = fingerprint(resolveLiveFile(stagedPath));
                if (!snapshot.equals(current)) {
                    conflicts.add(stagedPath);
                }
            }
            return conflicts;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to read staging conflicts", e);
            return List.of();
        }
    }

    public void clear() {
        lock.lock();
        try {
            if (stagingDir.exists()) {
                // Preserve the backup tree so a successful reload leaves rollback
                // snapshots intact; only staged pending edits are discarded.
                Path backupRoot = new File(stagingDir, "backup").toPath();
                try (var stream = Files.walk(stagingDir.toPath())) {
                    stream.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .filter(f -> !f.toPath().startsWith(backupRoot))
                            .forEach(File::delete);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to clear staging directory", e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private File resolveLiveFile(String stagedPath) {
        if (stagedPath.equals("tags/base.yml")) return tagsFile;
        if (stagedPath.equals("config.yml")) return configFile;
        if (stagedPath.equals("gui.yml")) return guiFile;
        if (stagedPath.startsWith("skills/")) {
            return new File(skillsDir, stagedPath.substring(7));
        }
        return new File(stagingDir, stagedPath);
    }

    /**
     * Reads the live-file path recorded in a deletion marker, falling back to the
     * flat {@code {skillId}.yml} path for markers written before the change.
     *
     * @param marker  the deletion marker file
     * @param skillId the skill id (for the fallback path)
     * @return the live-file path relative to the skills dir
     */
    private static String readMarkerPath(File marker, String skillId) {
        try {
            String content = Files.readString(marker.toPath(), StandardCharsets.UTF_8).trim();
            return content.isEmpty() ? skillId + ".yml" : content;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read deletion marker " + marker.getName(), e);
            return skillId + ".yml";
        }
    }

    /**
     * Resolves a skills-relative path into a live file, rejecting any path that
     * escapes the skills directory (defense in depth for the recorded deletion
     * marker, which is otherwise derived from an id-validated skill).
     *
     * @param relative the path relative to the skills dir
     * @return the confined live file
     * @throws IllegalArgumentException if the path escapes the skills dir
     */
    private File confinedLiveSkillFile(String relative) {
        Path root = skillsDir.toPath().toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Deletion path escapes the skills directory: " + relative);
        }
        return target.toFile();
    }

    /**
     * Stages the deletion of a skill. The marker records the resolved live-file
     * path (relative to the skills dir) so {@link #applyAndBackup} deletes the
     * real file, including skills nested in subfolders whose file name differs
     * from the flat {@code skills/{id}.yml} path.
     *
     * @param skillId          the skill id
     * @param liveRelativePath the live file path relative to the skills dir
     */
    public void stageSkillDeletion(String skillId, String liveRelativePath) {
        lock.lock();
        try {
            File markerDir = new File(stagingDir, "deleted_skills");
            markerDir.mkdirs();
            File marker = new File(markerDir, skillId + ".yml.deleted");
            atomicWrite(marker, liveRelativePath);
            updateStatusAdd("deleted_skills/" + skillId + ".yml.deleted");
            // Remove any previously staged file for this skill so it doesn't
            // get resurrected by applyAndBackup() copying all staged .yml files
            File staged = stagedSkillFile(skillId);
            if (staged.exists()) staged.delete();
        } catch (IOException e) {
            throw new RuntimeException("Failed to stage deletion for skill: " + skillId, e);
        } finally {
            lock.unlock();
        }
    }

    public List<String> applyAndBackup() {
        lock.lock();
        try {
            if (!hasPendingChanges()) return List.of();

            // Re-check conflicts under the lock (closing the TOCTOU window against
            // other staging mutations before any file is copied into place).
            List<String> conflicts = checkConflicts();
            if (!conflicts.isEmpty()) {
                LOGGER.warning("Conflict detected: live files modified since staging: " + conflicts);
                return List.of(); // caller can check conflicts separately
            }

            List<String> applied = new ArrayList<>();
            try {
                // Collision-free backup directory so concurrent reloads never
                // overwrite each other's backups.
                File backupDir = new File(stagingDir,
                        "backup/" + System.nanoTime() + "-" + UUID.randomUUID());
                backupDir.mkdirs();

                // Process deletions before applying new files
                File deletedSkillsDir = new File(stagingDir, "deleted_skills");
                if (deletedSkillsDir.exists()) {
                    File[] deletionMarkers = deletedSkillsDir.listFiles((d, n) -> n.endsWith(".yml.deleted"));
                    if (deletionMarkers != null) {
                        for (File marker : deletionMarkers) {
                            String name = marker.getName();
                            String skillId = name.substring(0, name.length() - ".yml.deleted".length());
                            // The marker content carries the resolved live-file path
                            // (relative to the skills dir); fall back to the flat
                            // path for markers written before the change.
                            String relative = readMarkerPath(marker, skillId);
                            File live = confinedLiveSkillFile(relative);
                            backupFile(live, backupDir);
                            if (live.exists() && live.delete()) {
                                LOGGER.info("Deleted live skill file: " + relative);
                            }
                            applied.add("deleted_skills/" + skillId + ".yml");
                        }
                    }
                }

                // Apply staged skills
                File stagedSkillsDir = new File(stagingDir, "skills");
                if (stagedSkillsDir.exists()) {
                    File[] stagedFiles = stagedSkillsDir.listFiles((d, n) -> n.endsWith(".yml"));
                    if (stagedFiles != null) {
                        for (File f : stagedFiles) {
                            File live = new File(skillsDir, f.getName());
                            backupFile(live, backupDir);
                            atomicCopy(f, live);
                            applied.add("skills/" + f.getName());
                        }
                    }
                }

                // Apply staged tags/base.yml
                File stagedTags = new File(new File(stagingDir, "tags"), "base.yml");
                if (stagedTags.exists()) {
                    backupFile(tagsFile, backupDir);
                    atomicCopy(stagedTags, tagsFile);
                    applied.add("tags/base.yml");
                }

                // Apply staged config.yml
                File stagedConfig = new File(stagingDir, "config.yml");
                if (stagedConfig.exists()) {
                    backupFile(configFile, backupDir);
                    atomicCopy(stagedConfig, configFile);
                    applied.add("config.yml");
                }

                // Apply staged gui.yml
                File stagedGui = new File(stagingDir, "gui.yml");
                if (stagedGui.exists()) {
                    backupFile(guiFile, backupDir);
                    atomicCopy(stagedGui, guiFile);
                    applied.add("gui.yml");
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to apply staged changes", e);
                // Signal failure so the caller preserves staging for retry instead
                // of clearing it; earlier files remain backed up for manual restore.
                throw new IllegalStateException("Failed to apply staged changes; staging preserved for retry", e);
            }
            // Re-snapshot the now-live files so a reload that preserves staging on
            // failure (for retry) does not see its own Apply as an external
            // modification. Genuine post-staging edits still conflict.
            writeStatus(status().files());
            return applied;
        } finally {
            lock.unlock();
        }
    }

    private static void backupFile(File source, File backupDir) {
        if (!source.exists()) return;
        try {
            File target = new File(backupDir, source.getName());
            target.getParentFile().mkdirs();
            atomicCopy(source, target);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to backup " + source, e);
        }
    }

    public void stageSkillFile(String skillId, String yamlContent) {
        lock.lock();
        try {
            File f = stagedSkillFile(skillId);
            f.getParentFile().mkdirs();
            atomicWrite(f, yamlContent);
            updateStatusAdd("skills/" + skillId + ".yml");
        } catch (IOException e) {
            throw new RuntimeException("Failed to stage skill: " + skillId, e);
        } finally {
            lock.unlock();
        }
    }

    public void stageTagsFile(String yamlContent) {
        lock.lock();
        try {
            File f = new File(new File(stagingDir, "tags"), "base.yml");
            atomicWrite(f, yamlContent);
            updateStatusAdd("tags/base.yml");
        } catch (IOException e) {
            throw new RuntimeException("Failed to stage tags/base.yml", e);
        } finally {
            lock.unlock();
        }
    }

    public void stageConfigFile(String yamlContent) {
        lock.lock();
        try {
            File f = new File(stagingDir, "config.yml");
            atomicWrite(f, yamlContent);
            updateStatusAdd("config.yml");
        } catch (IOException e) {
            throw new RuntimeException("Failed to stage config.yml", e);
        } finally {
            lock.unlock();
        }
    }

    public void stageGuiFile(String yamlContent) {
        lock.lock();
        try {
            File f = new File(stagingDir, "gui.yml");
            atomicWrite(f, yamlContent);
            updateStatusAdd("gui.yml");
        } catch (IOException e) {
            throw new RuntimeException("Failed to stage gui.yml", e);
        } finally {
            lock.unlock();
        }
    }

    private void updateStatusAdd(String filePath) {
        lock.lock();
        try {
            var current = status();
            List<String> files = new ArrayList<>(current.files());
            if (!files.contains(filePath)) {
                files.add(filePath);
            }
            writeStatus(files);
        } finally {
            lock.unlock();
        }
    }

    private File statusFile() {
        return new File(stagingDir, "status.json");
    }

    /**
     * Writes a file atomically (temp file + atomic move) so a concurrent reload
     * or status read never observes a truncated/partial file.
     */
    private static void atomicWrite(File file, String content) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        File tmp = new File(parent, file.getName() + ".tmp-" + UUID.randomUUID());
        Files.writeString(tmp.toPath(), content, StandardCharsets.UTF_8);
        atomicMove(tmp, file);
    }

    /**
     * Copies a file into place atomically (temp + atomic move) so readers see the
     * complete old or new file, never a partial copy.
     */
    private static void atomicCopy(File source, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null) parent.mkdirs();
        File tmp = new File(parent, target.getName() + ".tmp-" + UUID.randomUUID());
        Files.copy(source.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        atomicMove(tmp, target);
    }

    private static void atomicMove(File tmp, File target) throws IOException {
        try {
            Files.move(tmp.toPath(), target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Content fingerprint of a file, or {@code "absent"} when it does not exist.
     * Using a content hash (not just mtime) catches same-second edits and new files.
     */
    private static String fingerprint(File file) {
        if (!file.exists() || !file.isFile()) return "absent";
        try (InputStream in = Files.newInputStream(file.toPath())) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                md.update(buffer, 0, n);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            return "absent";
        }
    }

    public record StagingStatus(
        boolean hasPendingChanges,
        int fileCount,
        List<String> files,
        String lastModified
    ) {}
}
