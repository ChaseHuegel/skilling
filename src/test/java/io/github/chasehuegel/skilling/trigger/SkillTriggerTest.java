package io.github.chasehuegel.skilling.trigger;

import io.github.chasehuegel.skilling.engine.event.SkillingLevelUpEvent;
import io.github.chasehuegel.skilling.engine.trigger.SkillTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.BlockBreakTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.BlockPlaceTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.BreedAnimalsTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.BrewPotionTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.BarterTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.CartographyTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.ChunkLoadTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.CollectXpTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.CompostTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.ConsumeItemTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.CraftItemTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.CropGrowTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.CureVillagerTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.EnchantItemTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.EntityDamageTakenTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.EntityDamageTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.EntityKillTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.FallDamageTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.FertilizeTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.FishingTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.FurnaceExtractTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.LevelUpTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.LaunchProjectileTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.AnvilPrepareTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.LeftClickAirTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.LeftClickBlockTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.JukeboxPlayTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.LecternPlaceTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.LeftClickEntityTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.LootTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.MapFillTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.MendTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.MountDamageTakenTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.RideDistanceTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.PlayerInteractTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.PotionSplashTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.ProjectileHitTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.RecipeDiscoverTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.RideHorseTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.RightClickAirTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.RightClickBlockTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.RightClickEntityTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.RightClickTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.SleepTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.SignBookTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.SmithTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.SnifferTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.SneakTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.SprintTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.TradeTrigger;
import io.github.chasehuegel.skilling.engine.trigger.impl.VaultChangeTrigger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.LootGenerateEvent;
import io.papermc.paper.event.block.CompostItemEvent;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import io.papermc.paper.event.entity.EntityFertilizeEggEvent;
import io.papermc.paper.event.player.CartographyItemEvent;
import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent;
import io.papermc.paper.event.player.PlayerMapFilledEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;

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
                    Arguments.of(new FallDamageTrigger(), "fall_damage", EntityDamageEvent.class),
                    Arguments.of(new EntityDamageTrigger(), "entity_damage", EntityDamageByEntityEvent.class),
                    Arguments.of(new EntityKillTrigger(), "entity_kill", EntityDeathEvent.class),
                    Arguments.of(new FishingTrigger(), "fishing", PlayerFishEvent.class),
                    Arguments.of(new FurnaceExtractTrigger(), "furnace_extract", FurnaceExtractEvent.class),
                    Arguments.of(new LevelUpTrigger(), "level_up", SkillingLevelUpEvent.class),
                    Arguments.of(new LaunchProjectileTrigger(), "launch_projectile", ProjectileLaunchEvent.class),
                    Arguments.of(new PlayerInteractTrigger(), "player_interact", PlayerInteractEvent.class),
                    Arguments.of(new RightClickAirTrigger(), "right_click_air", PlayerInteractEvent.class),
                    Arguments.of(new RightClickBlockTrigger(), "right_click_block", PlayerInteractEvent.class),
                    Arguments.of(new RightClickEntityTrigger(), "right_click_entity", PlayerInteractEntityEvent.class),
                    Arguments.of(new LeftClickAirTrigger(), "left_click_air", PlayerInteractEvent.class),
                    Arguments.of(new LeftClickBlockTrigger(), "left_click_block", PlayerInteractEvent.class),
                    Arguments.of(new LeftClickEntityTrigger(), "left_click_entity", EntityDamageByEntityEvent.class),
                    Arguments.of(new ProjectileHitTrigger(), "projectile_hit", ProjectileHitEvent.class),
                    Arguments.of(new RideHorseTrigger(), "ride_horse", VehicleEnterEvent.class),
                    Arguments.of(new RideDistanceTrigger(), "ride_distance", VehicleMoveEvent.class),
                    Arguments.of(new MountDamageTakenTrigger(), "mount_damage_taken", EntityDamageEvent.class),
                    Arguments.of(new CureVillagerTrigger(), "cure_villager", EntityTransformEvent.class),
                    Arguments.of(new SneakTrigger(), "sneak", PlayerToggleSneakEvent.class),
                    Arguments.of(new SprintTrigger(), "sprint", PlayerToggleSprintEvent.class),
                    Arguments.of(new ChunkLoadTrigger(), "chunk_load", ChunkLoadEvent.class),
                    Arguments.of(new SleepTrigger(), "sleep", PlayerDeepSleepEvent.class),
                    Arguments.of(new CompostTrigger(), "compost", CompostItemEvent.class),
                    Arguments.of(new FertilizeTrigger(), "fertilize", BlockFertilizeEvent.class),
                    Arguments.of(new TradeTrigger(), "trade", PlayerTradeEvent.class),
                    Arguments.of(new BarterTrigger(), "barter", PiglinBarterEvent.class),
                    Arguments.of(new RecipeDiscoverTrigger(), "recipe_discover", PlayerRecipeDiscoverEvent.class),
                    Arguments.of(new SmithTrigger(), "smith", SmithItemEvent.class),
                    Arguments.of(new MendTrigger(), "mend", PlayerItemMendEvent.class),
                    Arguments.of(new AnvilPrepareTrigger(), "anvil_prepare", PrepareAnvilEvent.class),
                    Arguments.of(new MapFillTrigger(), "map_fill", PlayerMapFilledEvent.class),
                    Arguments.of(new LootTrigger(), "loot", LootGenerateEvent.class),
                    Arguments.of(new CartographyTrigger(), "cartography", CartographyItemEvent.class),
                    Arguments.of(new VaultChangeTrigger(), "vault_change", VaultChangeStateEvent.class),
                    Arguments.of(new SnifferTrigger(), "sniffer", EntityFertilizeEggEvent.class),
                    Arguments.of(new PotionSplashTrigger(), "potion_splash", PotionSplashEvent.class),
                    Arguments.of(new SignBookTrigger(), "sign_book", PlayerEditBookEvent.class),
                    Arguments.of(new JukeboxPlayTrigger(), "jukebox_play", PlayerInteractEvent.class),
                    Arguments.of(new LecternPlaceTrigger(), "lectern_place", PlayerInsertLecternBookEvent.class),
                    Arguments.of(new RightClickTrigger(), "right_click", PlayerInteractEvent.class)
            );
        }
    }
}
