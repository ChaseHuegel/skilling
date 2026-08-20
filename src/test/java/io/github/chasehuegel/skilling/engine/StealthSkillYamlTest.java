package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.TestSkillManager;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Guards the bundled Stealth skill: it must parse with the new {@code trip_trap}
 * trigger, the {@code target_unaware} state filter, the {@code core:sneak_speed}
 * / {@code core:cancel_event} / {@code core:drop_loot} mechanics, and a
 * pickpocket ability whose drop_loot mechanics are target_type-filtered.
 */
class StealthSkillYamlTest {

    @TempDir
    Path tempDir;

    private SkillDefinition.Ability ability(String id) {
        SkillDefinition skill = parseStealth();
        return skill.abilities().stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("stealth.yml has no ability '" + id + "'"));
    }

    private SkillDefinition parseStealth() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
            @SuppressWarnings("unchecked")
            Tag<Material> tag = mock(Tag.class);
            when(tag.getValues()).thenReturn(Set.of(Material.STONE));
            when(Bukkit.getTag(anyString(), any(NamespacedKey.class), eq(Material.class))).thenReturn(tag);
            @SuppressWarnings("unchecked")
            Tag<EntityType> entityTag = mock(Tag.class);
            when(entityTag.getValues()).thenReturn(Set.of(EntityType.ZOMBIE));
            when(Bukkit.getTag(anyString(), any(NamespacedKey.class), eq(EntityType.class)))
                    .thenReturn(entityTag);

            SkillManager manager = managerWithBundledTags();
            File skillFile = new File(Objects.requireNonNull(
                    getClass().getClassLoader().getResource("skills/stealth.yml")).getFile());
            return manager.parseSkill(skillFile);
        }
    }

    private SkillManager managerWithBundledTags() {
        try {
            Path tagsDir = tempDir.resolve("tags");
            Files.createDirectories(tagsDir);
            try (var in = getClass().getClassLoader().getResourceAsStream("tags/base.yml")) {
                Files.copy(in, tagsDir.resolve("base.yml"));
            }
            CustomTagLoader loader = new CustomTagLoader();
            loader.loadDirectory(tagsDir.toFile());
            return TestSkillManager.newWith(reg -> {}, new TagResolver(loader));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void xpSourcesReferenceSneakCombat() {
        var triggers = parseStealth().xpSources().stream().map(SkillDefinition.XpSource::trigger).toList();
        assertTrue(triggers.contains("entity_damage"));
        assertTrue(triggers.contains("entity_kill"));
        assertTrue(triggers.contains("loot"));
    }

    @Test
    void sneakSpeedBindsToSneakTrigger() {
        SkillDefinition.Ability speed = ability("sneak_speed");
        assertEquals("sneak", speed.trigger());
        assertEquals(1, speed.mechanics().stream()
                .filter(m -> "core:sneak_speed".equals(m.type())).count());
    }

    @Test
    void silentStepsUsesTripTrapAndCancelEvent() {
        SkillDefinition.Ability silent = ability("silent_steps");
        assertEquals("trip_trap", silent.trigger());
        assertTrue(silent.requirements().state().contains("is_sneaking"));
        assertEquals(1, silent.mechanics().stream()
                .filter(m -> "core:cancel_event".equals(m.type())).count());
    }

    @Test
    void backstabRequiresSneakingAndUnawareTarget() {
        SkillDefinition.Ability backstab = ability("backstab");
        assertEquals("entity_damage", backstab.trigger());
        assertTrue(backstab.requirements().state().contains("is_sneaking"));
        assertTrue(backstab.requirements().state().contains("target_unaware"));
        assertNotNull(backstab.requirements().exhaustion());
    }

    @Test
    void pickpocketHasFiveTargetFilteredDropLootMechanics() {
        SkillDefinition.Ability pickpocket = ability("pickpocket");
        assertEquals("right_click_entity", pickpocket.trigger());
        assertEquals(5, pickpocket.mechanics().stream()
                .filter(m -> "core:drop_loot".equals(m.type())).count());
    }

    @Test
    void umbralMantleCloaksWithFieldAura() {
        SkillDefinition.Ability mantle = ability("umbral_mantle");
        assertEquals("sneak", mantle.trigger());
        assertEquals(1, mantle.mechanics().stream()
                .filter(m -> "core:field_aura".equals(m.type())).count());
    }
}
