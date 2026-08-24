package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.evaluator.impl.ConstantEvaluator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the bundled Throwing skill: it must parse with the {@code was_sneaking}
 * requirement state, the {@code target_type} mechanic filter, the new
 * throw-haste mechanic, the no-cooldown active, the constant fury capstone, and
 * the remodeled XP-source set, so a regression in any of those features fails
 * fast here.
 */
class ThrowingSkillYamlTest {

    private static SkillDefinition parse() {
        return TestSkillManager.newBuiltIn().parseSkill(
                new File(Objects.requireNonNull(ThrowingSkillYamlTest.class.getClassLoader()
                        .getResource("skills/throwing.yml")).getFile()));
    }

    private static SkillDefinition.Ability ability(String id) {
        SkillDefinition skill = parse();
        return skill.abilities().stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("throwing.yml has no ability '" + id + "'"));
    }

    @Test
    void xpSourcesRewardHitsKillsAndTridentProcurementNotFreeTridentSpam() {
        SkillDefinition skill = parse();
        var sources = skill.xpSources();
        assertTrue(sources.stream().anyMatch(s -> s.trigger().equals("entity_damage")),
                "throwing must earn XP from landing thrown tridents");
        assertTrue(sources.stream().anyMatch(s -> s.trigger().equals("entity_kill")
                        && s.filters().stream().anyMatch(f -> f.target() == null
                                && f.state() != null && f.state().equals("target_type:minecraft:drowned"))),
                "throwing must earn XP from hunting drowned (the trident's source)");
        // No free-and-spammable per-throw trident source: with a Loyalty trident
        // that would out-earn landing a shot.
        boolean tridentLaunch = sources.stream()
                .filter(s -> s.trigger().equals("launch_projectile"))
                .anyMatch(s -> s.filters().stream().anyMatch(f -> "minecraft:trident".equals(f.target())));
        assertTrue(!tridentLaunch, "throwing must not reward merely throwing a trident");
        // The pre-trident on-ramp is a deliberately low-return grind on the free
        // throwables, per the cheap-material low-return rule.
        assertTrue(sources.stream().anyMatch(s -> s.trigger().equals("launch_projectile")
                        && s.filters().stream().anyMatch(f -> "minecraft:snowball".equals(f.target()))),
                "throwing must offer a low-return snowball throw grind");
    }

    @Test
    void quickReleaseUsesTheThrowHasteMechanic() {
        SkillDefinition.Ability release = ability("quick_release");
        assertEquals(15, release.unlockLevel());
        assertEquals("launch_projectile", release.trigger());
        assertNotNull(release.mechanics().get(0).parameters().get("reduction"));
        SkillDefinition.MechanicEntry entry = release.mechanics().get(0);
        assertEquals("core:throw_haste", entry.type());
    }

    @Test
    void skewerGatesOnThrowTimeSneakWithHungerAndNoCooldown() {
        SkillDefinition.Ability skewer = ability("skewer");
        assertEquals("entity_damage", skewer.trigger());
        assertTrue(skewer.requirements().state().contains("was_sneaking"),
                "skewer must read the sneak stance captured at release");
        assertNotNull(skewer.requirements().exhaustion(), "skewer must be gated by hunger");
        assertEquals(1.0, skewer.requirements().exhaustion().amount().evaluate(100, 25), 1e-9);
        assertEquals(0.0, skewer.requirements().cooldown().evaluate(100, 25), 1e-9,
                "skewer must not use a cooldown (hunger is the gate)");
        assertTrue(skewer.mechanics().stream().anyMatch(m -> "core:true_damage".equals(m.type())),
                "skewer must pierce armor");
    }

    @Test
    void impalingMasteryGatesOnAquaticTargets() {
        SkillDefinition.Ability impaling = ability("impaling_mastery");
        assertEquals(50, impaling.unlockLevel());
        boolean gatesOnAquatic = impaling.mechanics().stream()
                .flatMap(m -> m.filters().stream())
                .anyMatch(f -> f.state() != null && f.state().equals("target_type:#c:aquatic"));
        assertTrue(gatesOnAquatic, "impaling mastery must scale against aquatic targets");
    }

    @Test
    void stormVolleyCapstoneIsAConstantFuryRampNotASecondScalar() {
        SkillDefinition.Ability volley = ability("storm_volley");
        assertEquals(100, volley.unlockLevel());
        assertTrue(volley.mechanics().stream().anyMatch(m -> "core:fury".equals(m.type())),
                "the level-100 capstone must be a fury ramp");
        SkillDefinition.MechanicEntry fury = volley.mechanics().stream()
                .filter(m -> "core:fury".equals(m.type())).findFirst().orElseThrow();
        assertTrue(fury.parameters().get("multiplier_step") instanceof ConstantEvaluator,
                "the level-100 capstone must be a constant strong value (no levels left to grow)");
    }
}