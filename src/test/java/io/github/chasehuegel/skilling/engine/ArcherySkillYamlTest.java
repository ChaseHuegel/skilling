package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.TestSkillManager;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the bundled Archery skill: it must parse with the {@code was_sneaking}
 * requirement state, the {@code target_status} mechanic filter, the
 * no-cooldown design, and the new XP-source set, so a regression in any of
 * those features fails fast here.
 */
class ArcherySkillYamlTest {

    private static SkillDefinition.Ability ability(String id) {
        SkillDefinition skill = TestSkillManager.newBuiltIn().parseSkill(
                new File(Objects.requireNonNull(ArcherySkillYamlTest.class.getClassLoader()
                        .getResource("skills/archery.yml")).getFile()));
        return skill.abilities().stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("archery.yml has no ability '" + id + "'"));
    }

    @Test
    void xpSourcesRewardHittingAndCraftingNotJustFiring() {
        SkillDefinition skill = TestSkillManager.newBuiltIn().parseSkill(
                new File(Objects.requireNonNull(ArcherySkillYamlTest.class.getClassLoader()
                        .getResource("skills/archery.yml")).getFile()));
        var triggers = skill.xpSources().stream().map(SkillDefinition.XpSource::trigger).toList();
        assertTrue(triggers.contains("entity_damage"), "archery must earn XP from landing arrows");
        assertTrue(triggers.contains("entity_kill"));
        assertTrue(triggers.contains("craft_item"), "archery must earn XP from making its ammo");
        // No free-and-spammable "fired a shot" source: with an Infinity bow that
        // would out-earn landing a shot.
        assertTrue(!triggers.contains("shoot_bow"), "archery must not reward merely firing a bow");
    }

    @Test
    void aimedShotGatesOnShotTimeSneakWithHungerAndNoCooldown() {
        SkillDefinition.Ability aimed = ability("aimed_shot");
        assertEquals("entity_damage", aimed.trigger());
        assertTrue(aimed.requirements().state().contains("was_sneaking"),
                "aimed shot must read the sneak stance captured at release");
        assertNotNull(aimed.requirements().exhaustion(), "aimed shot must be gated by hunger");
        assertEquals(1.0, aimed.requirements().exhaustion().amount());
        assertEquals(0.0, aimed.requirements().cooldown().evaluate(100, 25), 1e-9,
                "aimed shot must not use a cooldown (hunger is the gate)");
        assertTrue(aimed.mechanics().stream()
                .anyMatch(m -> "core:true_damage".equals(m.type())), "aimed shot must pierce armor");
    }

    @Test
    void huntingProwessSynergizesWithMarkedTargets() {
        SkillDefinition.Ability prowess = ability("hunting_prowess");
        assertEquals("entity_damage", prowess.trigger());
        boolean gatesOnGlowing = prowess.mechanics().stream()
                .flatMap(m -> m.filters().stream())
                .anyMatch(f -> "target_status:minecraft:glowing".equals(f.state()));
        assertTrue(gatesOnGlowing, "hunting prowess must scale against Glowing (marked) targets");
    }

    @Test
    void huntersMarkEnhancesSpectralArrowsBeyondEveryArrow() {
        SkillDefinition.Ability mark = ability("hunters_mark");
        assertEquals(50, mark.unlockLevel());
        var statuses = mark.mechanics().stream()
                .filter(m -> "core:apply_status".equals(m.type())).toList();
        assertEquals(2, statuses.size(), "hunters mark must cover every arrow and spectral arrows");

        boolean everyArrow = statuses.stream()
                .flatMap(m -> m.filters().stream())
                .anyMatch(f -> "#minecraft:arrows".equals(f.target()));
        boolean spectralEnhanced = statuses.stream()
                .flatMap(m -> m.filters().stream())
                .anyMatch(f -> "minecraft:spectral_arrow".equals(f.target()));
        assertTrue(everyArrow, "every arrow must apply the mark");
        assertTrue(spectralEnhanced, "spectral arrows must be enhanced beyond the regular mark");
    }

    @Test
    void heartseekerCapstoneIsConstantNotScaling() {
        SkillDefinition.Ability cap = ability("heartseeker");
        assertEquals(100, cap.unlockLevel());
        var trueDamage = cap.mechanics().stream()
                .filter(m -> "core:true_damage".equals(m.type()))
                .findFirst().orElseThrow();
        assertTrue(trueDamage.parameters().get("percent") instanceof io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator,
                "the level-100 capstone must be a constant strong value (no levels left to grow)");
    }

    @Test
    void steadyHandAndArrowRecoveryScaleFromTheirUnlock() {
        SkillDefinition.Ability steady = ability("steady_hand");
        assertEquals(1, steady.unlockLevel());
        assertTrue(steady.mechanics().get(0).parameters().containsKey("multiplier"));

        SkillDefinition.Ability recovery = ability("arrow_recovery");
        assertEquals(15, recovery.unlockLevel());
        assertTrue(recovery.mechanics().get(0).parameters().containsKey("chance"));
    }
}
