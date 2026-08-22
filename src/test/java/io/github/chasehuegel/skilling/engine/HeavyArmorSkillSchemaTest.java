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
 * Guards the bundled {@code heavy_armor.yml} against schema drift: it must parse
 * cleanly through the production registrar as the heavy-armor redesign, expose
 * the six-milestone ability shape, and wire the two mechanics added for it
 * ({@code core:equipment_attribute} and {@code core:reduce_damage}).
 */
class HeavyArmorSkillSchemaTest {

    @Test
    void combatMechanicsAreRegistered() {
        var mechReg = new MechanicRegistry();
        Skilling.registerBuiltinMechanics(mechReg);
        assertTrue(mechReg.contains("core:equipment_attribute"));
        assertTrue(mechReg.contains("core:reduce_damage"));
    }

    @Test
    void heavyArmorSkillParsesAndWiresAbilities() throws Exception {
        var manager = TestSkillManager.newBuiltIn();

        SkillDefinition skill;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("skills/heavy_armor.yml")) {
            assertNotNull(in, "bundled skills/heavy_armor.yml must exist on the classpath");
            Path skillPath = Files.createTempFile("heavy_armor", ".yml");
            Files.writeString(skillPath, new String(in.readAllBytes()));
            skill = manager.parseSkill(skillPath.toFile());
        }

        assertEquals("heavy_armor", skill.id());
        assertEquals(6, skill.abilities().size(),
                "the heavy-armor redesign keeps the six-milestone ability shape");

        var abilityTypes = skill.abilities().stream()
                .flatMap(a -> a.mechanics().stream())
                .map(m -> m.type())
                .toList();
        assertTrue(abilityTypes.contains("core:equipment_attribute"), "L1/L15 must use core:equipment_attribute");
        assertTrue(abilityTypes.contains("core:reduce_damage"), "L75/L100 must use core:reduce_damage");
        assertEquals(2, abilityTypes.stream().filter("core:equipment_attribute"::equals).count(),
                "armor and armor-toughness mastery are both equipment-gated");
        assertEquals(2, abilityTypes.stream().filter("core:reduce_damage"::equals).count(),
                "environmental and universal reduction are both present");
    }
}