package io.github.chasehuegel.skilling.requirements;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.evaluator.ParameterEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import io.github.chasehuegel.skilling.engine.evaluator.impl.LinearEvaluator;
import io.github.chasehuegel.skilling.engine.requirements.FailureReason;
import io.github.chasehuegel.skilling.engine.requirements.RequirementEngine;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequirementEngineCooldownTest {

    private RequirementEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RequirementEngine(mock(TagResolver.class),
                new io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry());
    }

    private Player mockPlayer() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }

    private SkillDefinition.Requirements requirements(ParameterEvaluator cooldown) {
        return new SkillDefinition.Requirements(cooldown, List.of(), List.of(), null);
    }

    @Test
    void linearCooldownDecreasesAsLevelGapGrows() {
        var cooldown = new LinearEvaluator(10.0, -1.0, 0.0, 10.0);
        assertEquals(10.0, cooldown.evaluate(25, 25), 1e-9);
        assertEquals(9.0, cooldown.evaluate(26, 25), 1e-9);
        assertEquals(5.0, cooldown.evaluate(30, 25), 1e-9);
        assertEquals(0.0, cooldown.evaluate(35, 25), 1e-9);
    }

    @Test
    void scalarCooldownWrapsToConstantEvaluator() {
        var req = requirements(new ConstantEvaluator(5.0));
        assertEquals(5.0, req.cooldown().evaluate(10, 5), 1e-9);
    }

    @Test
    void scalarCooldownBlocksSecondUse() {
        var req = requirements(new ConstantEvaluator(5.0));
        var player = mockPlayer();
        assertTrue(engine.check(player, "ability", req, 10, 5).success());
        engine.consume(player, "ability", req, 10, 5);
        var result = engine.check(player, "ability", req, 10, 5);
        assertEquals(FailureReason.COOLDOWN, result.failureReason());
    }

    @Test
    void zeroCooldownSkipsCooldownGate() {
        var req = requirements(new ConstantEvaluator(0.0));
        var player = mockPlayer();
        assertTrue(engine.check(player, "ability", req, 10, 5).success());
        engine.consume(player, "ability", req, 10, 5);
        assertTrue(engine.check(player, "ability", req, 10, 5).success());
    }

    @Test
    void cooldownSurvivesQuitRelogUntilExpiry() {
        // A cooldown applied via consume must keep blocking the same player after a
        // quit/relog (the engine no longer clears cooldowns on quit). The only thing
        // that lifts it is time, not a relog.
        var req = requirements(new ConstantEvaluator(0.2));
        var player = mockPlayer();
        assertTrue(engine.check(player, "ability", req, 10, 5).success());
        engine.consume(player, "ability", req, 10, 5);

        // Simulate a relog: nothing happens to cooldown state.
        assertEquals(FailureReason.COOLDOWN, engine.check(player, "ability", req, 10, 5).failureReason());
    }

    @Test
    void pruneExpiredCooldownsRemovesExpiredButKeepsActive() throws Exception {
        var player = mockPlayer();
        var shortReq = requirements(new ConstantEvaluator(0.05));
        var longReq = requirements(new ConstantEvaluator(10.0));

        engine.consume(player, "short", shortReq, 10, 5);
        engine.consume(player, "long", longReq, 10, 5);

        // Let the short cooldown expire, then prune.
        Thread.sleep(80);
        engine.pruneExpiredCooldowns();

        assertEquals(FailureReason.COOLDOWN, engine.check(player, "long", longReq, 10, 5).failureReason(),
                "an active cooldown must survive pruning");
        assertTrue(engine.check(player, "short", shortReq, 10, 5).success(),
                "an expired cooldown must be released after pruning");
    }
}
