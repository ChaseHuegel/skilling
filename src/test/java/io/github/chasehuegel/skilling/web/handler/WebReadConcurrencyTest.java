package io.github.chasehuegel.skilling.web.handler;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.web.staging.StagingManager;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;

/**
 * Web handlers read shared engine state on Jetty threads while /skills reload
 * mutates it on the main thread; iteration must never throw CME or see a torn view.
 */
class WebReadConcurrencyTest {

    @TempDir
    Path tempDir;

    private SkillManager skillManager;
    private MechanicRegistry mechReg;
    private TriggerRegistry trigReg;
    private SkillHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        skillManager = TestSkillManager.newBuiltIn();
        mechReg = new MechanicRegistry();
        trigReg = new TriggerRegistry();
        Skilling.registerBuiltinMechanics(mechReg);
        Skilling.registerBuiltinTriggers(trigReg);

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

        handler = new SkillHandler(skillManager, mock(StagingManager.class), skillsDir.toFile());
    }

    @Test
    void webReadsDuringReloadNeverThrow() throws Exception {
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread reader = new Thread(() -> {
            try {
                while (running.get()) {
                    Context ctx = mock(Context.class, RETURNS_SELF);
                    handler.list(ctx);
                    mechReg.getAllParameterNames();
                    mechReg.keys();
                    trigReg.keys();
                }
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "web-reader");
        reader.start();

        try {
            for (int i = 0; i < 300; i++) {
                skillManager.clear();
                skillManager.loadSkills(tempDir.resolve("skills").toFile());
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
        assertNull(t, "web reads during reload must never throw: " + t);
    }
}
