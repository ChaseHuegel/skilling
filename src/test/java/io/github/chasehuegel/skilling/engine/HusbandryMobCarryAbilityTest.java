package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.SkillDefinition;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Husbandry mob-carry and companion-call abilities: mob pickup is bound
 * to {@code right_click_entity} (with drop folded into the pickup toggle), and the
 * L100 companion calls are bound to {@code right_click_air} with {@code
 * core:summon_companion}.
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
    void pickupConsolidatesDrop() {
        // Set-down is folded into the pickup ability (right-clicking a carried
        // passenger toggles it down); there must be no separate sneak-triggered
        // set_down ability.
        SkillDefinition skill = io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn().parseSkill(
                new java.io.File(Objects.requireNonNull(getClass().getClassLoader().getResource("skills/husbandry.yml")).getFile()));
        assertTrue(skill.abilities().stream().noneMatch(a -> a.id().equals("set_down")),
                "consolidated mob_porter should leave no separate 'set_down' ability");
    }

    @Test
    void companionCallsBindToRightClickAirWithSummonMechanic() {
        for (String id : new String[]{"wolf_call", "horse_call"}) {
            SkillDefinition.Ability call = ability(id);
            assertEquals("right_click_air", call.trigger(), id + " must trigger on right_click_air");
            assertTrue(call.mechanics().stream().anyMatch(m -> "core:summon_companion".equals(m.type())),
                    id + " must use core:summon_companion");
        }
        assertNotNull(ability("mark_companion"));
    }
}