package io.github.chasehuegel.skilling.engine.lockdown;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.AsyncBatchWorker;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import io.github.chasehuegel.skilling.engine.ui.GuiLayoutConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.io.File;
import java.util.logging.Level;

/**
 * Orchestrates the six-phase reload lockdown sequence for {@code /skills reload}.
 *
 * <ol>
 *   <li>Freeze — set reloading flag, all event listeners short-circuit</li>
 *   <li>Close GUIs — force-close skill overview inventories for online players</li>
 *   <li>Flush DB — synchronous drain of dirty profiles</li>
 *   <li>Rebuild — clear registries, re-register built-ins, reload YAML</li>
 *   <li>Invalidate — clear UI caches in player profiles</li>
 *   <li>Unlock — clear reloading flag to resume normal operation</li>
 * </ol>
 */
public final class LockdownManager {

    private final Skilling plugin;
    private final ProfileManager profileManager;
    private final AsyncBatchWorker asyncBatchWorker;
    private final SkillManager skillManager;

    public LockdownManager(Skilling plugin, ProfileManager profileManager,
                           AsyncBatchWorker asyncBatchWorker, SkillManager skillManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.asyncBatchWorker = asyncBatchWorker;
        this.skillManager = skillManager;
    }

    /**
     * Performs the full reload lockdown sequence. Must be called from the main thread.
     */
    public void reload() {
        plugin.getLogger().info("Reloading...");

        // Phase 1: Freeze
        plugin.setReloading(true);
        plugin.debug("Phase 1/6: Freeze — interactions locked.");

        // Phase 2: Close GUIs
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top != null && top.getHolder() instanceof io.github.chasehuegel.skilling.engine.ui.SkillInventoryHolder) {
                player.closeInventory();
            }
        }
        plugin.debug("Phase 2/6: GUIs closed.");

        // Phase 3: Flush DB
        asyncBatchWorker.flushDirtyProfiles();
        plugin.debug("Phase 3/6: Database flushed.");

        // Phase 4: Rebuild
        try {
            plugin.reloadConfigSettings();
            plugin.getRegistries().getEvaluatorRegistry().clear();
            plugin.getRegistries().getMechanicRegistry().clear();
            plugin.getRegistries().getTriggerRegistry().clear();
            plugin.registerBuiltins();
            var customTagLoader = new CustomTagLoader();
            customTagLoader.load(new File(plugin.getDataFolder(), "tags.yml"));
            var tagResolver = new TagResolver(customTagLoader);
            skillManager.setTagResolver(tagResolver);
            plugin.getRequirementEngine().setTagResolver(tagResolver);
            plugin.getSkillEventListener().setTagResolver(tagResolver);
            plugin.setCustomTagLoader(customTagLoader);
            skillManager.clear();
            skillManager.loadSkills(new File(plugin.getDataFolder(), "skills"));
            plugin.debug("Phase 4/6: Registries rebuilt.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to rebuild registries during reload", e);
        }

        // Phase 5: Invalidate UI caches
        plugin.getSkillMenuBuilder().setGuiLayoutConfig(GuiLayoutConfig.load());
        for (PlayerProfile profile : profileManager.getAllProfiles().values()) {
            // The synchronous Phase 3 flush already persisted everything up to
            // its snapshot markers; interactions are frozen, so marking clean at
            // the current counter is safe (matches the old markSaved() semantics).
            profile.markSaved(profile.getModCount());
            profile.invalidatePageCache();
        }
        plugin.debug("Phase 5/6: UI caches invalidated.");

        // Phase 6: Unlock
        plugin.setReloading(false);
        plugin.debug("Phase 6/6: Unlocked.");
        plugin.getLogger().info("Reload complete.");
    }
}
