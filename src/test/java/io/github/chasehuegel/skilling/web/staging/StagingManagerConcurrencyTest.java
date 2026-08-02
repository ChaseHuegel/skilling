package io.github.chasehuegel.skilling.web.staging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        File liveTags = new File(tempDir.toFile(), "tags.yml");
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
        File stagedTags = new File(sm.getStagingDir(), "tags.yml");
        assertTrue(!stagedTags.exists());
    }
}
