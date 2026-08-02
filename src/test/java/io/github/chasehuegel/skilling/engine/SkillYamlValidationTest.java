package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.engine.registry.EvaluatorRegistry;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.TriggerRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression-guard sweep over every bundled skill YAML in
 * {@code src/main/resources/skills/}.
 *
 * <p>Each file is parsed with the real {@link SkillManager} and asserted against
 * the Phase 4 remediation rules:
 * <ol>
 *   <li>every ability carries a non-blank {@code trigger} (P2-1 / C14);</li>
 *   <li>no {@code player_interact} XP source is left unfiltered (C4);</li>
 *   <li>no {@code entity_damage} ability uses a {@code target} weapon tag —
 *       melee weapon detection must use {@code tool} (C3);</li>
 *   <li>every {@code effect} parameter on status/aura mechanics is a namespaced
 *       key or a legacy numeric ID (P2-11).</li>
 * </ol>
 */
class SkillYamlValidationTest {

    private static final Set<String> EFFECT_MECHANIC_TYPES = Set.of(
            "core:apply_status", "core:crowd_control", "core:aoe_effect",
            "core:field_aura", "core:ally_aura"
    );

    /** Heuristic for weapon-tag targets that only match via the held item. */
    private static final Set<String> WEAPON_TAGS = Set.of(
            "#c:light_weapons", "#c:heavy_weapons"
    );

    private SkillManager skillManager;

    @BeforeEach
    void setUp() {
        skillManager = io.github.chasehuegel.skilling.TestSkillManager.newBuiltIn();
    }

    @Test
    void everyAbilityHasNonBlankTrigger() {
        for (File file : skillFiles()) {
            SkillDefinition def = skillManager.parseSkill(file);
            for (SkillDefinition.Ability ability : def.abilities()) {
                assertNotNull(ability.trigger(),
                        file.getName() + ": ability '" + ability.id() + "' has a null trigger");
                assertFalse(ability.trigger().isBlank(),
                        file.getName() + ": ability '" + ability.id() + "' has a blank trigger");
            }
        }
    }

    @Test
    void playerInteractXpSourceIsFiltered() {
        for (File file : skillFiles()) {
            SkillDefinition def = skillManager.parseSkill(file);
            for (SkillDefinition.XpSource source : def.xpSources()) {
                if ("player_interact".equals(source.trigger())) {
                    assertFalse(source.filters().isEmpty(),
                            file.getName() + ": player_interact XP source has no filters (C4)");
                }
            }
        }
    }

    @Test
    void entityDamageAbilitiesUseToolNotWeaponTarget() {
        for (File file : skillFiles()) {
            SkillDefinition def = skillManager.parseSkill(file);
            for (SkillDefinition.Ability ability : def.abilities()) {
                if (!"entity_damage".equals(ability.trigger())) continue;
                for (SkillDefinition.MechanicEntry entry : ability.mechanics()) {
                    for (SkillDefinition.Filter filter : entry.filters()) {
                        String target = filter.target();
                        if (target == null || !target.startsWith("#c:")) continue;
                        boolean isWeaponTag = WEAPON_TAGS.contains(target) || target.contains("weapon");
                        assertFalse(isWeaponTag,
                                file.getName() + ": ability '" + ability.id()
                                        + "' uses target '" + target + "' for weapon detection; "
                                        + "held-weapon detection on melee must use 'tool' (C3)");
                    }
                }
            }
        }
    }

    @Test
    void effectParametersUseNamespacedKeysOrNumericIds() {
        for (File file : skillFiles()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<?> abilities = config.getList("abilities", List.of());
            for (int i = 0; i < abilities.size(); i++) {
                Map<String, Object> ability = castMap(abilities.get(i));
                String abilityId = String.valueOf(ability.getOrDefault("id", "?"));
                Object mechanicsRaw = ability.get("mechanics");
                if (!(mechanicsRaw instanceof List<?> mechanics)) continue;
                for (int m = 0; m < mechanics.size(); m++) {
                    Map<String, Object> mechanic = castMap(mechanics.get(m));
                    String type = (String) mechanic.get("type");
                    if (type == null || !EFFECT_MECHANIC_TYPES.contains(type)) continue;
                    Map<String, Object> params = castMap(mechanic.get("parameters"));
                    assertTrue(params.containsKey("effect"),
                            file.getName() + ": " + type + " mechanic #" + m + " of ability '"
                                    + abilityId + "' is missing the required 'effect' parameter");
                    assertEffectValueIsValid(file, abilityId, type, params.get("effect"));
                }
            }
        }
    }

    private void assertEffectValueIsValid(File file, String abilityId, String mechanicType, Object rawEffect) {
        Map<String, Object> effect = castMap(rawEffect);
        if (effect.isEmpty()) {
            fail(file.getName() + ": ability '" + abilityId + "' " + mechanicType
                    + " has a null/empty effect parameter");
        }
        Object value = effect.getOrDefault("constant", effect);
        if (value instanceof String s) {
            assertTrue(s.startsWith("minecraft:"),
                    file.getName() + ": ability '" + abilityId + "' " + mechanicType
                            + " uses non-namespaced effect '" + s + "' (must start with 'minecraft:')");
        } else if (value instanceof Number) {
            // legacy numeric ID (deprecated but accepted)
        } else {
            fail(file.getName() + ": ability '" + abilityId + "' " + mechanicType
                    + " uses unsupported effect value: " + value);
        }
    }

    private List<File> skillFiles() {
        File dir = new File("src/main/resources/skills");
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        assertNotNull(files, "skills directory not found at src/main/resources/skills");
        return List.of(files);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return Map.of();
    }
}
