package io.github.chasehuegel.skilling.engine.listener;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.DatabaseManager;
import io.github.chasehuegel.skilling.engine.mechanic.SkillMechanic;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.registry.MechanicRegistry;
import io.github.chasehuegel.skilling.engine.registry.StateFilterRegistry;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Event;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the listener handlers for the gameplay-coverage batch of triggers
 * ({@code compost}, {@code trade}, {@code barter}, {@code recipe_discover},
 * {@code smith}, {@code mend}, {@code map_fill}, {@code cartography},
 * {@code vault_change}, {@code sniffer}, {@code potion_splash}): player-attributed
 * events dispatch to the owning player, playerless block/entity events dispatch
 * to nearby players, and nullable-player events skip when no player is present.
 */
class SkillEventListenerGameplayCoverageTriggerTest {

    /** Records the runtime event class of every execution. */
    public static class RecordingMechanic implements SkillMechanic {
        public static final java.util.List<Class<? extends Event>> EVENTS =
                new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override
        public boolean execute(Player player, Map<String, Object> params, Event event) {
            EVENTS.add(event.getClass());
            return true;
        }
    }

    @TempDir
    Path tempDir;

    private SkillEventListener listener;
    private PlayerProfile profile;
    private Player player;
    private World world;
    private Location location;

    @BeforeEach
    void setUp() throws IOException {
        RecordingMechanic.EVENTS.clear();

        SkillManager skillManager = io.github.chasehuegel.skilling.TestSkillManager.newWith(
                reg -> reg.register("test:record", RecordingMechanic.class, java.util.List.of()));

        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("test.yml"), """
                id: test_skill
                max_level: 100
                display: { name: "Test", color: "GREEN", style: "SOLID" }
                progression: { curve: "constant", base_xp: 100.0 }
                xp_sources: []
                abilities:
                  - id: compost_ability
                    display_name: "Compost"
                    unlock_level: 1
                    trigger: "compost"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: trade_ability
                    display_name: "Trade"
                    unlock_level: 1
                    trigger: "trade"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: barter_ability
                    display_name: "Barter"
                    unlock_level: 1
                    trigger: "barter"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: recipe_ability
                    display_name: "Recipe"
                    unlock_level: 1
                    trigger: "recipe_discover"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: smith_ability
                    display_name: "Smith"
                    unlock_level: 1
                    trigger: "smith"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: mend_ability
                    display_name: "Mend"
                    unlock_level: 1
                    trigger: "mend"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: map_ability
                    display_name: "Map"
                    unlock_level: 1
                    trigger: "map_fill"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: cartography_ability
                    display_name: "Cartography"
                    unlock_level: 1
                    trigger: "cartography"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: vault_ability
                    display_name: "Vault"
                    unlock_level: 1
                    trigger: "vault_change"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: sniffer_ability
                    display_name: "Sniffer"
                    unlock_level: 1
                    trigger: "sniffer"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: potion_ability
                    display_name: "Potion"
                    unlock_level: 1
                    trigger: "potion_splash"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                  - id: loot_ability
                    display_name: "Loot"
                    unlock_level: 1
                    trigger: "loot"
                    mechanics:
                      - { type: "test:record" }
                    feedback: { notify: { action_bar: false } }
                """);
        skillManager.loadSkills(skillsDir.toFile());

        DatabaseManager db = mock(DatabaseManager.class);
        when(db.isInitialized()).thenReturn(false);
        ProfileManager profileManager = new ProfileManager(db);
        UUID uuid = UUID.randomUUID();
        profile = profileManager.loadProfile(uuid).join();
        profile.setXp("test_skill", 10000);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);

        world = mock(World.class);
        location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);

        MechanicRegistry mechReg = new MechanicRegistry();
        mechReg.register("test:record", RecordingMechanic.class, java.util.List.of());

        Skilling plugin = mock(Skilling.class);
        when(plugin.getGlobalXpModifier()).thenReturn(1.0);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("gameplay-coverage-trigger-test"));

        listener = new SkillEventListener(plugin, skillManager, profileManager,
                new TagResolver(new CustomTagLoader()),
                new io.github.chasehuegel.skilling.engine.requirements.RequirementEngine(
                        new TagResolver(new CustomTagLoader()), new StateFilterRegistry()),
                mechReg, new io.github.chasehuegel.skilling.engine.feedback.FeedbackDebouncer(500),
                mock(io.github.chasehuegel.skilling.engine.feedback.BossBarPool.class),
                new StateFilterRegistry());
    }

    private void assertDispatched(Class<? extends Event> eventClass) {
        assertEquals(1, RecordingMechanic.EVENTS.size(),
                "exactly one ability execution expected");
        assertEquals(eventClass, RecordingMechanic.EVENTS.get(0));
    }

    @Test
    void compostDispatchesToNearbyPlayers() {
        var event = mock(io.papermc.paper.event.block.CompostItemEvent.class);
        var block = mock(Block.class);
        when(event.getBlock()).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(world.getNearbyPlayers(any(Location.class), eq(5.0), any())).thenReturn(java.util.List.of(player));
        listener.onCompost(event);
        assertDispatched(io.papermc.paper.event.block.CompostItemEvent.class);
    }

    @Test
    void tradeDispatchesToTradingPlayer() {
        var event = mock(io.papermc.paper.event.player.PlayerTradeEvent.class);
        when(event.getPlayer()).thenReturn(player);
        listener.onTrade(event);
        assertDispatched(io.papermc.paper.event.player.PlayerTradeEvent.class);
    }

    @Test
    void barterDispatchesToNearbyPlayers() {
        var event = mock(PiglinBarterEvent.class);
        var piglin = mock(Piglin.class);
        when(event.getEntity()).thenReturn(piglin);
        when(piglin.getLocation()).thenReturn(location);
        when(world.getNearbyPlayers(any(Location.class), eq(5.0), any())).thenReturn(java.util.List.of(player));
        listener.onBarter(event);
        assertDispatched(PiglinBarterEvent.class);
    }

    @Test
    void recipeDiscoverDispatchesToPlayer() {
        var event = mock(PlayerRecipeDiscoverEvent.class);
        when(event.getPlayer()).thenReturn(player);
        listener.onRecipeDiscover(event);
        assertDispatched(PlayerRecipeDiscoverEvent.class);
    }

    @Test
    void smithDispatchesToViewPlayer() {
        var event = mock(SmithItemEvent.class);
        var view = mock(InventoryView.class);
        when(event.getView()).thenReturn(view);
        when(view.getPlayer()).thenReturn(player);
        listener.onSmith(event);
        assertDispatched(SmithItemEvent.class);
    }

    @Test
    void mendDispatchesToPlayer() {
        var event = mock(PlayerItemMendEvent.class);
        when(event.getPlayer()).thenReturn(player);
        listener.onMend(event);
        assertDispatched(PlayerItemMendEvent.class);
    }

    @Test
    void mapFillDispatchesToPlayer() {
        var event = mock(io.papermc.paper.event.player.PlayerMapFilledEvent.class);
        when(event.getPlayer()).thenReturn(player);
        listener.onMapFill(event);
        assertDispatched(io.papermc.paper.event.player.PlayerMapFilledEvent.class);
    }

    @Test
    void cartographyDispatchesToViewPlayer() {
        var event = mock(io.papermc.paper.event.player.CartographyItemEvent.class);
        var view = mock(InventoryView.class);
        when(event.getView()).thenReturn(view);
        when(view.getPlayer()).thenReturn(player);
        listener.onCartography(event);
        assertDispatched(io.papermc.paper.event.player.CartographyItemEvent.class);
    }

    @Test
    void vaultChangeDispatchesWhenPlayerPresent() {
        var event = mock(io.papermc.paper.event.block.VaultChangeStateEvent.class);
        when(event.getPlayer()).thenReturn(player);
        listener.onVaultChange(event);
        assertDispatched(io.papermc.paper.event.block.VaultChangeStateEvent.class);
    }

    @Test
    void vaultChangeSkipsWhenPlayerAbsent() {
        var event = mock(io.papermc.paper.event.block.VaultChangeStateEvent.class);
        when(event.getPlayer()).thenReturn(null);
        listener.onVaultChange(event);
        assertTrue(RecordingMechanic.EVENTS.isEmpty(),
                "a playerless vault change must not dispatch");
    }

    @Test
    void snifferDispatchesWhenBreederIsPlayer() {
        var event = mock(io.papermc.paper.event.entity.EntityFertilizeEggEvent.class);
        when(event.getBreeder()).thenReturn(player);
        listener.onSniffer(event);
        assertDispatched(io.papermc.paper.event.entity.EntityFertilizeEggEvent.class);
    }

    @Test
    void snifferSkipsWhenBreederAbsent() {
        var event = mock(io.papermc.paper.event.entity.EntityFertilizeEggEvent.class);
        when(event.getBreeder()).thenReturn(null);
        listener.onSniffer(event);
        assertTrue(RecordingMechanic.EVENTS.isEmpty(),
                "a breederless egg fertilization must not dispatch");
    }

    @Test
    void potionSplashDispatchesToPlayerThrower() {
        var event = mock(PotionSplashEvent.class);
        var potion = mock(ThrownPotion.class);
        when(event.getEntity()).thenReturn(potion);
        when(potion.getShooter()).thenReturn(player);
        listener.onPotionSplash(event);
        assertDispatched(PotionSplashEvent.class);
    }

    @Test
    void lingeringPotionSplashDispatchesToPlayerThrower() {
        var event = mock(LingeringPotionSplashEvent.class);
        var potion = mock(ThrownPotion.class);
        when(event.getEntity()).thenReturn(potion);
        when(potion.getShooter()).thenReturn(player);
        listener.onLingeringPotionSplash(event);
        assertDispatched(LingeringPotionSplashEvent.class);
    }

    @Test
    void potionSplashSkipsWhenThrowerIsNotPlayer() {
        var event = mock(PotionSplashEvent.class);
        var potion = mock(ThrownPotion.class);
        when(event.getEntity()).thenReturn(potion);
        when(potion.getShooter()).thenReturn(mock(org.bukkit.entity.Creeper.class));
        listener.onPotionSplash(event);
        assertTrue(RecordingMechanic.EVENTS.isEmpty(),
                "a non-player thrower must not dispatch");
    }

    @Test
    void lootDispatchesToNearbyPlayers() {
        var event = mock(org.bukkit.event.world.LootGenerateEvent.class);
        var context = mock(org.bukkit.loot.LootContext.class);
        when(event.getLootContext()).thenReturn(context);
        when(context.getLocation()).thenReturn(location);
        when(world.getNearbyPlayers(any(Location.class), eq(16.0), any())).thenReturn(java.util.List.of(player));
        listener.onLoot(event);
        assertDispatched(org.bukkit.event.world.LootGenerateEvent.class);
    }

    @Test
    void lootSkipsWhenContextIsNull() {
        var event = mock(org.bukkit.event.world.LootGenerateEvent.class);
        when(event.getLootContext()).thenReturn(null);
        listener.onLoot(event);
        assertTrue(RecordingMechanic.EVENTS.isEmpty(),
                "a loot event without a context must not dispatch");
    }

    @Test
    void compostSkipsWhenWorldIsNull() {
        var event = mock(io.papermc.paper.event.block.CompostItemEvent.class);
        var block = mock(Block.class);
        when(event.getBlock()).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(location.getWorld()).thenReturn(null);
        listener.onCompost(event);
        assertTrue(RecordingMechanic.EVENTS.isEmpty(),
                "a compost event without a world must not dispatch");
    }

    @Test
    void handlersIgnoreCancelledEvents() throws Exception {
        assertTrue(handler("onCompost").ignoreCancelled());
        assertTrue(handler("onTrade").ignoreCancelled());
        assertTrue(handler("onBarter").ignoreCancelled());
        assertTrue(handler("onRecipeDiscover").ignoreCancelled());
        assertTrue(handler("onSmith").ignoreCancelled());
        assertTrue(handler("onMend").ignoreCancelled());
        assertTrue(handler("onMapFill").ignoreCancelled());
        assertTrue(handler("onCartography").ignoreCancelled());
        assertTrue(handler("onVaultChange").ignoreCancelled());
        assertTrue(handler("onSniffer").ignoreCancelled());
        assertTrue(handler("onPotionSplash").ignoreCancelled());
        assertTrue(handler("onLingeringPotionSplash").ignoreCancelled());
        assertTrue(handler("onLoot").ignoreCancelled());
        assertFalse(handler("onPotionSplash").priority().name().equals("LOWEST"),
                "the potion handlers must not run early");
    }

    private org.bukkit.event.EventHandler handler(String name) throws Exception {
        var method = SkillEventListener.class.getDeclaredMethod(name, expectedEventType(name));
        return method.getAnnotation(org.bukkit.event.EventHandler.class);
    }

    private Class<?> expectedEventType(String name) {
        return switch (name) {
            case "onCompost" -> io.papermc.paper.event.block.CompostItemEvent.class;
            case "onTrade" -> io.papermc.paper.event.player.PlayerTradeEvent.class;
            case "onBarter" -> PiglinBarterEvent.class;
            case "onRecipeDiscover" -> PlayerRecipeDiscoverEvent.class;
            case "onSmith" -> SmithItemEvent.class;
            case "onMend" -> PlayerItemMendEvent.class;
            case "onMapFill" -> io.papermc.paper.event.player.PlayerMapFilledEvent.class;
            case "onCartography" -> io.papermc.paper.event.player.CartographyItemEvent.class;
            case "onVaultChange" -> io.papermc.paper.event.block.VaultChangeStateEvent.class;
            case "onSniffer" -> io.papermc.paper.event.entity.EntityFertilizeEggEvent.class;
            case "onPotionSplash" -> PotionSplashEvent.class;
            case "onLingeringPotionSplash" -> LingeringPotionSplashEvent.class;
            case "onLoot" -> org.bukkit.event.world.LootGenerateEvent.class;
            default -> throw new IllegalArgumentException("unknown handler: " + name);
        };
    }
}
