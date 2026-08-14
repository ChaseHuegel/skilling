package io.github.chasehuegel.skilling.skill;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.SkillManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies the shipped {@code template-skill.yml} (copied to server data folders
 * on first run) is a valid starting point: it parses cleanly through the skill
 * parser, including the required ability {@code trigger} values.
 */
class TemplateSkillYamlTest {

    @Test
    void shippedTemplateParsesCleanly() {
        InputStream in = TemplateSkillYamlTest.class.getClassLoader()
                .getResourceAsStream("template-skill.yml");
        assertNotNull(in, "template-skill.yml must ship as a resource");
        var config = YamlConfiguration.loadConfiguration(new InputStreamReader(in));

        SkillManager skillManager = TestSkillManager.newBuiltIn();
        var def = skillManager.parseSkill(config);

        assertNotNull(def, "the shipped template must parse");
        assertEquals(2, def.abilities().size(),
                "the shipped template must keep both inlined abilities");
        def.abilities().forEach(ability ->
                assertEquals("block_break", ability.trigger(),
                        "every inlined ability must carry its required trigger"));
    }
}
