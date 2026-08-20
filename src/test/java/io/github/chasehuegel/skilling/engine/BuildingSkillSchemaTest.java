package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the bundled {@code building.yml} against schema drift: it must parse
 * cleanly through the production registrar, expose the expected six-milestone
 * ability shape, and wire the mechanics added for the construction-skill
 * redesign.
 */
class BuildingSkillSchemaTest {

    @Test
    void constructionMechanicsAreRegistered() {
        var mechReg = new MechanicRegistry();
        Skilling.registerBuiltinMechanics(mechReg);
        assertTrue(mechReg.contains("core:block_refund"));
        assertTrue(mechReg.contains("core:marked_demolition"));
        assertTrue(mechReg.contains("core:elytra_flight"));
    }

    @Test
    void buildingSkillParsesAndWiresConstructionAbilities() throws Exception {
        var manager = TestSkillManager.newBuiltIn();

        SkillDefinition skill;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("skills/building.yml")) {
            assertNotNull(in, "bundled skills/building.yml must exist on the classpath");
            Path skillPath = Files.createTempFile("building", ".yml");
            Files.writeString(skillPath, new String(in.readAllBytes()));
            skill = manager.parseSkill(skillPath.toFile());
        }

        assertEquals("building", skill.id());
        assertEquals(6, skill.abilities().size(),
                "the construction build keeps the six-milestone ability shape");

        var abilityTypes = skill.abilities().stream()
                .flatMap(a -> a.mechanics().stream())
                .map(m -> m.type())
                .toList();
        assertTrue(abilityTypes.contains("core:block_refund"), "L1 must use core:block_refund");
        assertTrue(abilityTypes.contains("core:area_harvest"), "L15 must use core:area_harvest");
        assertTrue(abilityTypes.contains("core:marked_demolition"), "L75 must use core:marked_demolition");
        assertTrue(abilityTypes.contains("core:elytra_flight"), "L100 must use core:elytra_flight");
    }
}
