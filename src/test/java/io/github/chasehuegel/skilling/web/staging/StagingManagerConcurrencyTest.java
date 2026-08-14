package io.github.chasehuegel.skilling.web.staging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StagingManagerConcurrencyTest {

    @TempDir
    Path tempDir;

    @Test
    void concurrentPutsProduceDistinctFilesAndCorrectStatus() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        int count = 10;

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int idx = i;
            threads.add(new Thread(() -> sm.stageSkillFile("skill_" + idx, "id: skill_" + idx + "\n")));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join(5000);

        StagingManager.StagingStatus status = sm.status();
        assertEquals(count, status.fileCount(), "no concurrent PUT may lose an entry");
        assertTrue(status.files().stream().allMatch(f -> f.startsWith("skills/")));

        File stagedSkills = new File(sm.getStagingDir(), "skills");
        assertEquals(count, stagedSkills.listFiles().length);
    }

    @Test
    void backupsUseCollisionFreeNames() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        String content = "debug_logging: true\n";

        sm.stageTagsFile(content);
        sm.applyAndBackup();
        // Re-stage the same content and apply again; the live fingerprint matches
        // the snapshot so no conflict, and the backup must land in a NEW directory
        // (never overwrite the previous reload's backup).
        sm.stageTagsFile(content);
        sm.applyAndBackup();

        File backups = new File(sm.getStagingDir(), "backup");
        assertEquals(2, backups.listFiles().length,
                "each reload must write a distinct backup directory");
    }

    @Test
    void stagedWriteConcurrentWithReloadNeverProducesPartialLiveFile() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        File liveTags = new File(new File(tempDir.toFile(), "tags"), "base.yml");
        String content = "debug_logging: true\n".repeat(5000);

        sm.stageTagsFile(content);
        sm.applyAndBackup();

        String read = Files.readString(liveTags.toPath());
        assertEquals(content, read, "the applied live file must be complete, never truncated");
    }

    @Test
    void clearRemovesEverythingIncludingStatus() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        sm.stageTagsFile("debug_logging: true\n");
        assertTrue(sm.hasPendingChanges());

        sm.clear();

        assertTrue(!sm.hasPendingChanges());
        File stagedTags = new File(new File(sm.getStagingDir(), "tags"), "base.yml");
        assertTrue(!stagedTags.exists());
    }

    @Test
    void backupSurvivesClearAfterSuccessfulReload() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        // A live file exists so apply creates a real backup.
        File liveTags = new File(new File(tempDir.toFile(), "tags"), "base.yml");
        Files.createDirectories(liveTags.toPath().getParent());
        Files.writeString(liveTags.toPath(), "debug_logging: false\n");

        sm.stageTagsFile("debug_logging: true\n");
        sm.applyAndBackup();
        File backupDir = new File(sm.getStagingDir(), "backup");
        assertEquals(1, backupDir.listFiles().length, "one backup before clear");

        // ReloadHandler calls clear() on success; the backup must survive.
        sm.clear();

        assertTrue(backupDir.exists(), "backup must survive a successful reload's clear()");
        assertEquals(1, backupDir.listFiles().length);
        assertTrue(!sm.hasPendingChanges(), "pending edits are discarded, backups are not");
    }

    @Test
    void midApplyFailureThrowsAndPreservesStagingForRetry() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        sm.stageSkillFile("foo", "id: foo\n");

        // Make the live target a directory so the copy/move fails mid-apply.
        File skillsDir = new File(tempDir.toFile(), "skills");
        skillsDir.mkdirs();
        Files.createDirectory(skillsDir.toPath().resolve("foo.yml"));

        assertThrows(IllegalStateException.class, sm::applyAndBackup);
        assertTrue(sm.hasPendingChanges(), "staging must be preserved for retry after a failed apply");
    }

    @Test
    void applyThenRetryDoesNotReportFalseConflict() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        sm.stageTagsFile("debug_logging: true\n");
        // Apply, then simulate the reload failing/timing out with staging preserved.
        sm.applyAndBackup();

        // A retry after the failed reload must not see the Apply itself as an
        // external modification of the live files.
        assertTrue(sm.checkConflicts().isEmpty(),
                "applied files must not be reported as conflicts on retry");
        sm.applyAndBackup(); // the retry reload must apply cleanly
        assertTrue(sm.checkConflicts().isEmpty());
    }

    @Test
    void externalModificationAfterApplyStillConflicts() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        sm.stageTagsFile("debug_logging: true\n");
        sm.applyAndBackup();

        // A genuine post-apply edit (e.g. FTP) must still be flagged on retry.
        File liveTags = new File(new File(tempDir.toFile(), "tags"), "base.yml");
        Files.createDirectories(liveTags.toPath().getParent());
        Files.writeString(liveTags.toPath(), "debug_logging: externally_edited\n");

        List<String> conflicts = sm.checkConflicts();
        assertTrue(conflicts.contains("tags/base.yml"),
                "a genuine external edit after Apply must still be detected as a conflict");
    }

    @Test
    void clearAppliedPreservesAnEditStagedDuringTheReloadWindow() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        sm.stageSkillFile("alpha", "id: alpha\nmax_level: 100\n");
        List<String> applied = sm.applyAndBackup();

        // An admin saves another skill while the reload rebuild is in flight.
        sm.stageSkillFile("gamma", "id: gamma\nmax_level: 100\n");

        sm.clearApplied(applied);

        File stagedAlpha = new File(sm.getStagingDir(), "skills/alpha.yml");
        File stagedGamma = new File(sm.getStagingDir(), "skills/gamma.yml");
        assertTrue(!stagedAlpha.exists(), "the applied entry's staged source must be dropped");
        assertTrue(stagedGamma.exists(), "an edit staged during the window must survive");
        assertTrue(sm.hasPendingChanges(), "the surviving edit must still be pending");
        StagingManager.StagingStatus status = sm.status();
        assertTrue(status.files().contains("skills/gamma.yml"));
        assertTrue(!status.files().contains("skills/alpha.yml"));
    }

    @Test
    void clearAppliedKeepsAReSaveOfTheSameFileDuringTheWindow() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        sm.stageSkillFile("alpha", "id: alpha\nmax_level: 100\n");
        List<String> applied = sm.applyAndBackup();

        // The admin re-saves the very same skill while the reload is in flight;
        // its staged content now differs from the just-applied live file.
        sm.stageSkillFile("alpha", "id: alpha\nmax_level: 200\n");

        sm.clearApplied(applied);

        File stagedAlpha = new File(sm.getStagingDir(), "skills/alpha.yml");
        assertTrue(stagedAlpha.exists(), "a re-save during the window must not be dropped");
        assertEquals("id: alpha\nmax_level: 200\n", Files.readString(stagedAlpha.toPath()));
        assertTrue(sm.status().files().contains("skills/alpha.yml"),
                "the re-saved edit must remain pending");
    }

    @Test
    void clearAppliedRemovesConsumedDeletionMarkers() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        File live = new File(new File(tempDir.toFile(), "skills"), "blasting.yml");
        Files.createDirectories(live.toPath().getParent());
        Files.writeString(live.toPath(), "id: blasting\n");

        sm.stageSkillDeletion("blasting", "blasting.yml");
        List<String> applied = sm.applyAndBackup();
        assertTrue(!live.exists(), "the live file must be deleted by apply");

        sm.clearApplied(applied);

        File marker = new File(sm.getStagingDir(), "deleted_skills/blasting.yml.deleted");
        assertTrue(!marker.exists(), "a consumed deletion marker must be dropped");
    }

    @Test
    void restoreAppliedRollsBackLiveFilesFromTheBackup() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        File liveTags = new File(new File(tempDir.toFile(), "tags"), "base.yml");
        Files.createDirectories(liveTags.toPath().getParent());
        Files.writeString(liveTags.toPath(), "debug_logging: false\n");

        sm.stageTagsFile("debug_logging: true\n");
        List<String> applied = sm.applyAndBackup();
        assertEquals("debug_logging: true\n", Files.readString(liveTags.toPath()));

        sm.restoreApplied(applied);
        assertEquals("debug_logging: false\n", Files.readString(liveTags.toPath()),
                "a failed reload must restore the live file from the apply backup");
    }

    @Test
    void restoreAppliedRemovesFilesThatDidNotExistBeforeApply() throws Exception {
        StagingManager sm = new StagingManager(tempDir.toFile());
        File skillsDir = new File(tempDir.toFile(), "skills");
        sm.stageSkillFile("mining", "id: mining\nmax_level: 100\n");
        List<String> applied = sm.applyAndBackup();
        assertTrue(new File(skillsDir, "mining.yml").exists());

        sm.restoreApplied(applied);
        assertFalse(new File(skillsDir, "mining.yml").exists(),
                "a file that did not exist before apply must be removed on rollback");
    }
}
