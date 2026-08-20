package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillDefinition;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the bundled {@code bard.yml} parses through the engine, exercising
 * the new {@code sign_book}, {@code jukebox_play}, and {@code lectern_place}
 * triggers and the {@code instrument} state filter that gates each goat-horn
 * song. This is the load-time surface for the per-instrument Anthem/Symphony
 * mechanics (one field_aura entry per horn, selected by an instrument state
 * filter) that keeps the design inside the scalar evaluator schema.
 */
class BardSkillYamlTest {

    private SkillDefinition parseBard() {
        InputStream in = BardSkillYamlTest.class.getClassLoader()
                .getResourceAsStream("skills/bard.yml");
        assertNotNull(in, "skills/bard.yml must ship as a resource");
        var config = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
        SkillManager skillManager = TestSkillManager.newBuiltIn();
        return skillManager.parseSkill(config);
    }

    private static long countByInstrument(SkillDefinition.Ability ability) {
        return ability.mechanics().stream()
                .filter(m -> m.filters() != null && m.filters().stream()
                        .anyMatch(f -> f.state() != null && f.state().startsWith("instrument:")))
                .count();
    }

    @Test
    void bundledBardParsesWithAllSixTiers() {
        SkillDefinition def = parseBard();
        assertNotNull(def);
        assertEquals(6, def.abilities().size(),
                "bard must keep the 6-tier milestone structure");
        assertEquals(1, def.abilities().stream().filter(a -> a.unlockLevel() == 1).count());
        assertEquals(1, def.abilities().stream().filter(a -> a.unlockLevel() == 100).count());
    }

    @Test
    void xpSourcesIncludeWrittenWordAndJukeboxLoops() {
        SkillDefinition def = parseBard();
        assertTrue(def.xpSources().stream().anyMatch(s -> s.trigger().equals("sign_book")),
                "signing a book must be a bard XP source");
        assertTrue(def.xpSources().stream().anyMatch(s -> s.trigger().equals("jukebox_play")),
                "inserting a disc into a jukebox must be a bard XP source");
        assertTrue(def.xpSources().stream().anyMatch(s -> s.trigger().equals("right_click_air")),
                "playing an instrument must be a bard XP source");
    }

    @Test
    void anthemAndSymphonyEachCarryEightInstrumentSongs() {
        SkillDefinition def = parseBard();
        SkillDefinition.Ability anthem = def.abilities().stream()
                .filter(a -> a.id().equals("anthem")).findFirst().orElseThrow();
        SkillDefinition.Ability symphony = def.abilities().stream()
                .filter(a -> a.id().equals("symphony")).findFirst().orElseThrow();
        assertEquals(8, countByInstrument(anthem),
                "Anthem must map one mechanic per goat-horn variant");
        assertEquals(8, countByInstrument(symphony),
                "Symphony must empower one mechanic per goat-horn variant");
    }

    @Test
    void lecternStoryIsLonglastingAndNonDefensive() {
        SkillDefinition def = parseBard();
        SkillDefinition.Ability lectern = def.abilities().stream()
                .filter(a -> a.id().equals("lectern_story")).findFirst().orElseThrow();
        assertEquals(3, lectern.mechanics().size(), "Lectern Story grants three buffs");
        assertTrue(lectern.mechanics().stream()
                .allMatch(m -> hasConstant(m, "duration", 1200.0)),
                "Lectern Story must last a full Minecraft day (1200s)");
    }

    @Test
    void unknownInstrumentValueFailsFastAtLoad() {
        var config = YamlConfiguration.loadConfiguration(new StringReader("""
                id: badhorns
                max_level: 100
                progression: { curve: constant, base_xp: 100 }
                abilities:
                  - id: x
                    display_name: "X"
                    unlock_level: 1
                    trigger: right_click_air
                    mechanics:
                      - type: "core:field_aura"
                        filters: [ { state: "instrument:minecraft:not_a_horn" } ]
                        parameters:
                          effect: { constant: "minecraft:speed" }
                          radius: { constant: 8.0 }
                          duration: { constant: 5.0 }
                          amplifier: { constant: 0.0 }
                    feedback: { notify: { action_bar: false } }
                """));
        SkillManager skillManager = TestSkillManager.newBuiltIn();
        assertThrows(IllegalArgumentException.class, () -> skillManager.parseSkill(config),
                "an unknown instrument variant must be rejected at load, not silently never fire");
    }

    private static boolean hasConstant(SkillDefinition.MechanicEntry m, String key, double value) {
        var params = m.parameters();
        if (!(params.get(key) instanceof io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator ce)) {
            return false;
        }
        return Double.parseDouble(String.valueOf(ce.rawValue())) == value;
    }
}
