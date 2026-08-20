package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Husbandry mob-carry pair: the bundled skill must bind pickup to
 * {@code right_click_entity} and set-down to {@code right_click_air}, so a
 * player carrying a mob can always drop it with the empty-hand air click.
 */
class HusbandryMobCarryAbilityTest {

    private SkillDefinition.Ability ability(String id) {
        SkillDefinition skill = io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn().parseSkill(
                new java.io.File(Objects.requireNonNull(getClass().getClassLoader().getResource("skills/husbandry.yml")).getFile()));
        return skill.abilities().stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("husbandry.yml has no ability '" + id + "'"));
    }

    @Test
    void mobPorterBindsPickupToRightClickEntity() {
        SkillDefinition.Ability porter = ability("mob_porter");
        assertEquals("right_click_entity", porter.trigger());
        assertTrue(porter.mechanics().stream().anyMatch(m -> "core:pick_up_mob".equals(m.type())),
                "mob_porter must use core:pick_up_mob");
        SkillDefinition.MechanicEntry pickup = porter.mechanics().stream()
                .filter(m -> "core:pick_up_mob".equals(m.type())).findFirst().orElseThrow();
        assertTrue(pickup.filters().stream().anyMatch(f ->
                        f.state() != null && f.state().equals("target_type:#c:portable_mobs")),
                "mob_porter must be gated by target_type:#c:portable_mobs");
    }

    @Test
    void setDownBindsDropToSneak() {
        SkillDefinition.Ability setDown = ability("set_down");
        assertEquals("sneak", setDown.trigger());
        assertTrue(setDown.mechanics().stream().anyMatch(m -> "core:drop_passengers".equals(m.type())),
                "set_down must use core:drop_passengers");
    }

    @Test
    void bothAbilitiesParseFromTheBundledSkill() {
        assertNotNull(ability("mob_porter"));
        assertNotNull(ability("set_down"));
    }
}
