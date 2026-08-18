package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.TestSkillManager;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the bundled Acrobatics skill: it must parse with the new {@code jump}
 * trigger, the {@code success_only} feedback flag, and the permanent-attribute
 * capstone, so a regression in any of those features fails fast here.
 */
class AcrobaticsSkillYamlTest {

    private SkillDefinition.Ability ability(String id) {
        SkillDefinition skill = TestSkillManager.newBuiltIn().parseSkill(
                new java.io.File(Objects.requireNonNull(getClass().getClassLoader().getResource("skills/acrobatics.yml")).getFile()));
        return skill.abilities().stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("acrobatics.yml has no ability '" + id + "'"));
    }

    @Test
    void xpSourcesReferenceJumpAndElytra() {
        SkillDefinition skill = TestSkillManager.newBuiltIn().parseSkill(
                new java.io.File(Objects.requireNonNull(getClass().getClassLoader().getResource("skills/acrobatics.yml")).getFile()));
        var triggers = skill.xpSources().stream().map(SkillDefinition.XpSource::trigger).toList();
        assertTrue(triggers.contains("jump"), "acrobatics must source XP from sprint-jumps");
        assertTrue(triggers.contains("elytra_glide"), "acrobatics must source XP from gliding");
        assertTrue(triggers.contains("fall_damage"));
        assertTrue(triggers.contains("launch_projectile"));
    }

    @Test
    void leapBindsToJumpTriggerWithHungerCost() {
        SkillDefinition.Ability leap = ability("leap");
        assertEquals("jump", leap.trigger());
        assertNotNull(leap.requirements().exhaustion(), "leap must be gated by hunger");
        assertEquals(1.0, leap.requirements().exhaustion().amount());
        assertTrue(leap.requirements().state().contains("is_sneaking"));
    }

    @Test
    void softLandingAndAgileUseSuccessOnlyFeedback() {
        assertTrue(ability("soft_landing").feedback().successOnly());
        assertTrue(ability("agile").feedback().successOnly());
    }

    @Test
    void gravityDefierUsesTwoPersistentAttributeMechanics() {
        SkillDefinition.Ability cap = ability("gravity_defier");
        assertEquals("level_up", cap.trigger());
        assertEquals(2, cap.mechanics().stream()
                .filter(m -> "core:persistent_attribute".equals(m.type())).count());
    }
}
