package io.github.chasehuegel.skilling.web.dto;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the web progression serialization contract: the engine derives every
 * curve from {@code base_xp} (plus {@code exponent} for polynomial), so a skill
 * saved by the web GUI must keep its curve parameters instead of silently
 * resetting to the engine defaults after reload.
 */
class SkillSerializerProgressionRoundTripTest {

    private SkillDefinition engineParse(String yaml) {
        SkillManager sm = TestSkillManager.newBuiltIn();
        return sm.parseSkill(YamlConfiguration.loadConfiguration(new StringReader(yaml)));
    }

    @Test
    void linearCurveSurvivesWebRoundTrip() {
        SkillDetailDTO dto = SkillSerializer.fromYaml("""
                id: test
                max_level: 100
                progression: { curve: linear, base_xp: 100 }
                """);
        assertEquals("linear", dto.progression().curve());
        assertEquals(100.0, dto.progression().baseXp());

        SkillDefinition def = engineParse(SkillSerializer.toYaml(dto));
        assertEquals("linear", def.progression().curve());
        // Engine linear = base_xp, step = base_xp * 0.1 (base 100, step 10).
        assertEquals(140.0, def.progression().evaluator().evaluate(5, 1), 1e-9);
    }

    @Test
    void constantCurveSurvivesWebRoundTrip() {
        SkillDetailDTO dto = SkillSerializer.fromYaml("""
                id: test
                max_level: 100
                progression: { curve: constant, base_xp: 250 }
                """);
        assertEquals("constant", dto.progression().curve());
        assertEquals(250.0, dto.progression().baseXp());

        SkillDefinition def = engineParse(SkillSerializer.toYaml(dto));
        assertEquals("constant", def.progression().curve());
        assertEquals(250.0, def.progression().evaluator().evaluate(10, 1), 1e-9);
    }

    @Test
    void polynomialRoundTripStillPreservesBaseAndExponent() {
        SkillDefinition def = engineParse(SkillSerializer.toYaml(
                SkillSerializer.fromYaml("""
                        id: test
                        max_level: 100
                        progression: { curve: polynomial, base_xp: 60, exponent: 3.0 }
                        """)));
        assertEquals("polynomial", def.progression().curve());
        assertEquals(60.0, def.progression().baseXp(), 1e-9);
        assertEquals(3.0, def.progression().exponent(), 1e-9);
    }

    @Test
    void linearProgressionDoesNotResetToDefaults() {
        // The regression: a linear skill saved by the web must not reload as
        // base_xp 50 (the engine default) because curve parameters were dropped.
        SkillDefinition def = engineParse(SkillSerializer.toYaml(
                SkillSerializer.fromYaml("""
                        id: test
                        max_level: 100
                        progression: { curve: linear, base_xp: 100 }
                        """)));
        assertEquals(100.0, def.progression().baseXp(), 1e-9);
    }
}
