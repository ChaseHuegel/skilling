package io.github.chasehuegel.skilling.engine;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the archery shot-context state filters: {@code was_sneaking} reads
 * the sneak stamp written on an arrow at release (not the player's live stance
 * at impact), and {@code target_status} checks the event's target for a potion
 * effect. Both fail closed for events they cannot evaluate.
 */
class StateFilterShotContextTest {

    @TempDir
    Path tempDir;

    private StateFilterRegistry registry;
    private Player player;

    @BeforeEach
    void setUp() throws Exception {
        Path tagsFile = tempDir.resolve("tags.yml");
        Files.writeString(tagsFile, "custom_tags:\n  ores:\n    - \"minecraft:coal_ore\"\n");
        var loader = new CustomTagLoader();
        loader.load(tagsFile.toFile());
        registry = new StateFilterRegistry();
        Skilling.registerBuiltinStateFilters(registry, new TagResolver(loader), new EntityTagResolver(loader));
        player = mock(Player.class);
    }

    private Projectile arrowWithSneak(boolean sneaking) {
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.has(Skilling.SHOT_SNEAK_KEY, PersistentDataType.BOOLEAN)).thenReturn(true);
        when(pdc.get(Skilling.SHOT_SNEAK_KEY, PersistentDataType.BOOLEAN)).thenReturn(sneaking);
        Projectile arrow = mock(Projectile.class);
        when(arrow.getPersistentDataContainer()).thenReturn(pdc);
        return arrow;
    }

    private EntityDamageByEntityEvent damageBy(Entity damager) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(damager);
        return event;
    }

    @Test
    void sneakStampedArrowMatchesWasSneaking() {
        assertTrue(registry.evaluate("was_sneaking", player, damageBy(arrowWithSneak(true)), ""));
    }

    @Test
    void nonSneakStampedArrowFailsWasSneaking() {
        assertFalse(registry.evaluate("was_sneaking", player, damageBy(arrowWithSneak(false)), ""));
    }

    @Test
    void meleeEventFailsWasSneakingEvenWhenSneaking() {
        LivingEntity meleeDamager = mock(LivingEntity.class);
        assertFalse(registry.evaluate("was_sneaking", player, damageBy(meleeDamager), ""));
    }

    @Test
    void unstampedArrowFailsWasSneaking() {
        Projectile arrow = mock(Projectile.class);
        when(arrow.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        assertFalse(registry.evaluate("was_sneaking", player, damageBy(arrow), ""));
    }

    @Test
    void targetStatusMatchesGlowingTarget() {
        LivingEntity victim = mock(LivingEntity.class);
        when(victim.hasPotionEffect(any(PotionEffectType.class))).thenReturn(true);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        assertTrue(registry.evaluate("target_status", player, event, "minecraft:glowing"));
    }

    @Test
    void targetStatusRejectsNonGlowingTarget() {
        LivingEntity victim = mock(LivingEntity.class);
        when(victim.hasPotionEffect(any(PotionEffectType.class))).thenReturn(false);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        assertFalse(registry.evaluate("target_status", player, event, "minecraft:glowing"));
    }

    @Test
    void targetStatusFailsClosedForMalformedEffect() {
        LivingEntity victim = mock(LivingEntity.class);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        assertFalse(registry.evaluate("target_status", player, event, "not_a_namespace:glowing"));
        assertFalse(registry.evaluate("target_status", player, event, "glowing"));
    }

    @Test
    void targetStatusFailsClosedForEventsWithoutTarget() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(mock(Entity.class));
        assertFalse(registry.evaluate("target_status", player, event, "minecraft:glowing"));
    }
}
