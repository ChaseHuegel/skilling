package io.github.chasehuegel.skilling.engine.ui;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

class UIProtectionListenerTest {

    private MockedStatic<Skilling> skillingStatic;
    private MockedStatic<RegistryAccess> registryMock;
    private Skilling plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(Skilling.class);
        when(plugin.namespace()).thenReturn("skilling");
        skillingStatic = mockStatic(Skilling.class);
        skillingStatic.when(Skilling::getInstance).thenReturn(plugin);

        registryMock = mockStatic(RegistryAccess.class);
        RegistryAccess access = mock(RegistryAccess.class);
        registryMock.when(RegistryAccess::registryAccess).thenReturn(access);
        when(access.getRegistry(any(Class.class))).thenAnswer(inv -> emptyRegistry());
        when(access.getRegistry(any(RegistryKey.class))).thenAnswer(inv -> emptyRegistry());
    }

    @AfterEach
    void tearDown() {
        skillingStatic.close();
        registryMock.close();
    }

    @SuppressWarnings("unchecked")
    private static Registry<Keyed> emptyRegistry() {
        return new Registry<>() {
            @Override
            public Keyed get(NamespacedKey key) {
                return null;
            }

            @Override
            public Iterator<Keyed> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public NamespacedKey getKey(Keyed value) {
                return null;
            }

            @Override
            public boolean hasTag(TagKey<Keyed> key) {
                return false;
            }

            @Override
            public Tag<Keyed> getTag(TagKey<Keyed> key) {
                return null;
            }

            @Override
            public Collection<Tag<Keyed>> getTags() {
                return Collections.emptyList();
            }

            @Override
            public Stream<Keyed> stream() {
                return Stream.empty();
            }

            @Override
            public Stream<NamespacedKey> keyStream() {
                return Stream.empty();
            }
        };
    }

    private ItemStack taggedStack() {
        return stackWithTag("ui_item");
    }

    private ItemStack guideBookStack() {
        return stackWithTag("guide_book");
    }

    private ItemStack stackWithTag(String tagKey) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.hasItemMeta()).thenReturn(true);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(pdc.has(any(NamespacedKey.class), eq(PersistentDataType.BYTE)))
                .thenAnswer(inv -> inv.<NamespacedKey>getArgument(0).getKey().equals(tagKey));
        ItemMeta meta = mock(ItemMeta.class);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        when(stack.getItemMeta()).thenReturn(meta);
        return stack;
    }

    @Test
    void bottomInventoryClickOnFourRowPageDoesNotNavigate() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        SkillInventoryHolder holder = new SkillInventoryHolder(player, 0, List.of("page1"), 2);
        Inventory top = mock(Inventory.class);
        when(top.getHolder()).thenReturn(holder);
        when(top.getSize()).thenReturn(36);

        Inventory bottom = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getView()).thenReturn(view);
        when(event.getClickedInventory()).thenReturn(bottom);
        when(event.getSlot()).thenReturn(27);
        when(event.getClick()).thenReturn(ClickType.LEFT);
        when(event.getAction()).thenReturn(InventoryAction.PICKUP_ALL);

        new UIProtectionListener().onInventoryClick(event);

        verify(player, never()).openInventory(any(Inventory.class));
    }

    @Test
    void taggedCursorDepositedIntoChestIsVaporized() {
        Player player = mock(Player.class);
        Inventory chest = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(chest);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getView()).thenReturn(view);
        ItemStack cursor = taggedStack();
        when(event.getCursor()).thenReturn(cursor);

        new UIProtectionListener().onInventoryClick(event);

        verify(event).setCursor(null);
    }

    @Test
    void hopperMoveOfTaggedItemIsVaporized() {
        ItemStack stack = taggedStack();
        InventoryMoveItemEvent event = mock(InventoryMoveItemEvent.class);
        when(event.getItem()).thenReturn(stack);

        new UIProtectionListener().onInventoryMove(event);

        verify(stack).setAmount(0);
    }

    @Test
    void guideBookCursorDepositIntoNormalChestIsNotVaporized() {
        Player player = mock(Player.class);
        Inventory chest = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(chest);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getView()).thenReturn(view);
        ItemStack book = guideBookStack();
        when(event.getCursor()).thenReturn(book);

        new UIProtectionListener().onInventoryClick(event);

        verify(event, never()).setCursor(null);
    }

    @Test
    void playerPickupOfTaggedItemIsVaporized() {
        org.bukkit.entity.Item item = mock(org.bukkit.entity.Item.class);
        ItemStack stack = taggedStack();
        when(item.getItemStack()).thenReturn(stack);
        PlayerAttemptPickupItemEvent event = mock(PlayerAttemptPickupItemEvent.class);
        doReturn(item).when(event).getItem();

        new UIProtectionListener().onPlayerAttemptPickup(event);

        verify(item).remove();
        verify(event).setCancelled(true);
    }

    @Test
    void inventoryCloseWithTaggedCursorIsCleanedUp() {
        Player player = mock(Player.class);
        ItemStack cursor = taggedStack();
        when(player.getItemOnCursor()).thenReturn(cursor);
        InventoryCloseEvent event = mock(InventoryCloseEvent.class);
        when(event.getPlayer()).thenReturn(player);

        new UIProtectionListener().onInventoryClose(event);

        verify(player).setItemOnCursor(null);
    }

    @Test
    void topInventoryClickOnNextSlotOpensTargetPage() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        SkillInventoryHolder holder = new SkillInventoryHolder(player, 0, List.of("page1", "page2"), 2);
        Inventory top = mock(Inventory.class);
        when(top.getHolder()).thenReturn(holder);
        when(top.getSize()).thenReturn(36);

        Inventory target = mock(Inventory.class);
        PlayerProfile profile = mock(PlayerProfile.class);
        when(profile.getCachedPageInventories()).thenReturn(Map.of(1, target));
        ProfileManager profileManager = mock(ProfileManager.class);
        when(profileManager.getProfile(player.getUniqueId())).thenReturn(profile);
        when(plugin.getProfileManager()).thenReturn(profileManager);

        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getView()).thenReturn(view);
        when(event.getClickedInventory()).thenReturn(top);
        when(event.getSlot()).thenReturn(35);
        when(event.getClick()).thenReturn(ClickType.LEFT);
        when(event.getAction()).thenReturn(InventoryAction.PICKUP_ALL);

        new UIProtectionListener().onInventoryClick(event);

        verify(player).openInventory(target);
    }
}
