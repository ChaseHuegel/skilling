package io.github.chasehuegel.skilling.trigger;

import io.github.chasehuegel.skilling.engine.event.SkillingLevelUpEvent;
import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.BlockBreakTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.BlockPlaceTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.BreedAnimalsTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.BrewPotionTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.CollectXpTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.ConsumeItemTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.CraftItemTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.CropGrowTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.CureVillagerTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.EnchantItemTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.EntityDamageTakenTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.EntityDamageTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.EntityKillTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.FishingTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.FurnaceExtractTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.LevelUpTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.LaunchProjectileTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.PlayerInteractTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.ProjectileHitTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.RideHorseTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.SneakTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.SprintTrigger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class SkillTriggerTest {

    @ParameterizedTest
    @ArgumentsSource(TriggerProvider.class)
    void getKey(SkillTrigger trigger, String expectedKey, Class<?> expectedEventClass) {
        assertEquals(expectedKey, trigger.getKey());
    }

    @ParameterizedTest
    @ArgumentsSource(TriggerProvider.class)
    void getEventClass(SkillTrigger trigger, String expectedKey, Class<?> expectedEventClass) {
        assertEquals(expectedEventClass, trigger.getEventClass());
    }

    private static final class TriggerProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(new BlockBreakTrigger(), "block_break", BlockBreakEvent.class),
                    Arguments.of(new BlockPlaceTrigger(), "block_place", BlockPlaceEvent.class),
                    Arguments.of(new BreedAnimalsTrigger(), "breed_animals", EntityBreedEvent.class),
                    Arguments.of(new BrewPotionTrigger(), "brew_potion", BrewEvent.class),
                    Arguments.of(new CollectXpTrigger(), "collect_xp", PlayerExpChangeEvent.class),
                    Arguments.of(new ConsumeItemTrigger(), "consume_item", PlayerItemConsumeEvent.class),
                    Arguments.of(new CraftItemTrigger(), "craft_item", CraftItemEvent.class),
                    Arguments.of(new CropGrowTrigger(), "crop_grow", BlockGrowEvent.class),
                    Arguments.of(new EnchantItemTrigger(), "enchant_item", EnchantItemEvent.class),
                    Arguments.of(new EntityDamageTakenTrigger(), "entity_damage_taken", EntityDamageEvent.class),
                    Arguments.of(new EntityDamageTrigger(), "entity_damage", EntityDamageByEntityEvent.class),
                    Arguments.of(new EntityKillTrigger(), "entity_kill", EntityDeathEvent.class),
                    Arguments.of(new FishingTrigger(), "fishing", PlayerFishEvent.class),
                    Arguments.of(new FurnaceExtractTrigger(), "furnace_extract", FurnaceExtractEvent.class),
                    Arguments.of(new LevelUpTrigger(), "level_up", SkillingLevelUpEvent.class),
                    Arguments.of(new LaunchProjectileTrigger(), "launch_projectile", ProjectileLaunchEvent.class),
                    Arguments.of(new PlayerInteractTrigger(), "player_interact", PlayerInteractEvent.class),
                    Arguments.of(new ProjectileHitTrigger(), "projectile_hit", ProjectileHitEvent.class),
                    Arguments.of(new RideHorseTrigger(), "ride_horse", VehicleEnterEvent.class),
                    Arguments.of(new CureVillagerTrigger(), "cure_villager", EntityTransformEvent.class),
                    Arguments.of(new SneakTrigger(), "sneak", PlayerToggleSneakEvent.class),
                    Arguments.of(new SprintTrigger(), "sprint", PlayerToggleSprintEvent.class)
            );
        }
    }
}
