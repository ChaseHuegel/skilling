package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;

class ReloadConcurrencyTest {

    @TempDir
    Path tempDir;

    private SkillManager skillManager;
    private EvaluatorRegistry evalReg;
    private MechanicRegistry mechReg;
    private TriggerRegistry trigReg;

    @BeforeEach
    void setUp() throws Exception {
        skillManager = TestSkillManager.newBuiltIn();
        evalReg = new EvaluatorRegistry();
        mechReg = new MechanicRegistry();
        trigReg = new TriggerRegistry();

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        for (int i = 0; i < 3; i++) {
            Files.writeString(skillsDir.resolve("skill_" + i + ".yml"), """
                id: skill_%d
                max_level: 100
                display: { name: "Skill %d", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                """.formatted(i, i));
        }
        skillManager.loadSkills(skillsDir.toFile());

        Skilling.registerBuiltinEvaluators(evalReg);
        Skilling.registerBuiltinMechanics(mechReg);
        Skilling.registerBuiltinTriggers(trigReg);
    }

    @Test
    void concurrentReloadAndIterationNeverThrows() throws Exception {
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread reader = new Thread(() -> {
            try {
                while (running.get()) {
                    for (var entry : skillManager.getSkills().entrySet()) {
                        entry.getKey().length();
                    }
                    for (String key : mechReg.keys()) {
                        key.length();
                    }
                    for (String key : trigReg.keys()) {
                        key.length();
                    }
                    evalReg.size();
                }
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "registry-reader");
        reader.start();

        try {
            // Simulate /skills reload: clear and repopulate the skill map and
            // every registry on this thread while the reader iterates them.
            for (int i = 0; i < 300; i++) {
                skillManager.clear();
                skillManager.loadSkills(tempDir.resolve("skills").toFile());
                evalReg.clear();
                Skilling.registerBuiltinEvaluators(evalReg);
                mechReg.clear();
                Skilling.registerBuiltinMechanics(mechReg);
                trigReg.clear();
                Skilling.registerBuiltinTriggers(trigReg);
            }
        } finally {
            running.set(false);
            reader.join(5000);
        }

        Throwable t = failure.get();
        assertNull(t, "concurrent registry iteration during reload must not throw: " + t);
    }
}
