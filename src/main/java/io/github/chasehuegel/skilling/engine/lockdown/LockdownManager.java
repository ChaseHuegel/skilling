package io.github.chasehuegel.skilling.engine.lockdown;

import io.github.chasehuegel.skilling.Skilling;
import io.github.chasehuegel.skilling.engine.SkillManager;
import io.github.chasehuegel.skilling.engine.db.AsyncBatchWorker;
import io.github.chasehuegel.skilling.engine.profile.PlayerProfile;
import io.github.chasehuegel.skilling.engine.profile.ProfileManager;
import io.github.chasehuegel.skilling.engine.tag.CustomTagLoader;
import io.github.chasehuegel.skilling.engine.tag.EntityTagResolver;
import io.github.chasehuegel.skilling.engine.tag.TagResolver;
import io.github.chasehuegel.skilling.engine.ui.GuiLayoutConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Orchestrates the six-phase reload lockdown sequence for {@code /skills reload}.
 *
 * <ol>
 *   <li>Freeze — set reloading flag, all event listeners short-circuit</li>
 *   <li>Close GUIs — force-close skill overview inventories for online players</li>
 *   <li>Flush DB — asynchronous drain of dirty profiles (never blocks the main thread)</li>
 *   <li>Rebuild — clear registries, re-register built-ins, reload YAML</li>
 *   <li>Invalidate — clear UI caches in player profiles</li>
 *   <li>Unlock — clear reloading flag to resume normal operation</li>
 * </ol>
 *
 * <p>The flush runs on a worker thread and the rebuild/invalidate phases are
 * scheduled back onto the main thread only after it completes, so the
 * flush-before-rebuild ordering and the {@code markSaved} invariant hold without
 * ever blocking the Bukkit main thread on JDBC work.
 */
public final class LockdownManager {

    /** Bounded wait for the asynchronous DB flush (worker thread, not the main thread). */
    private static final long FLUSH_WAIT_SECONDS = 5;

    private final Skilling plugin;
    private final ProfileManager profileManager;
    private final AsyncBatchWorker asyncBatchWorker;
    private final SkillManager skillManager;
    private final Executor continuationExecutor;

    public LockdownManager(Skilling plugin, ProfileManager profileManager,
                           AsyncBatchWorker asyncBatchWorker, SkillManager skillManager) {
        this(plugin, profileManager, asyncBatchWorker, skillManager, ForkJoinPool.commonPool());
    }

    /**
     * Test seam: supplies the executor that runs the flush-await + main-thread
     * handoff after phase 3. Production uses the common pool; tests pass a
     * synchronous executor so the continuation runs on the calling thread.
     */
    LockdownManager(Skilling plugin, ProfileManager profileManager,
                    AsyncBatchWorker asyncBatchWorker, SkillManager skillManager, Executor continuationExecutor) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.asyncBatchWorker = asyncBatchWorker;
        this.skillManager = skillManager;
        this.continuationExecutor = continuationExecutor;
    }

    /**
     * Performs the reload lockdown sequence without blocking the caller.
     *
     * <p>The freeze, GUI close, and asynchronous DB flush are kicked off on the
     * calling (main) thread; a worker thread awaits the flush with a bounded
     * timeout, then the rebuild and UI invalidation run back on the main thread.
     *
     * <p>Must be called from the main thread (freeze + GUI close are
     * main-thread-only). The returned future completes when the reload finishes
     * or fails, so callers on other threads can await it with a bounded timeout.
     *
     * @return a future completing when the reload sequence completes
     */
    public CompletableFuture<Void> reloadAsync() {
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

        // Phase 3: Flush DB asynchronously — never block the main thread on JDBC.
        CompletableFuture<Void> flush = asyncBatchWorker.flushDirtyProfilesAsync();
        plugin.debug("Phase 3/6: Database flush started asynchronously.");

        CompletableFuture<Void> done = new CompletableFuture<>();
        // Await the flush on a worker thread (bounded), then finish the reload on
        // the main thread so the registry/UI rebuild stays main-thread-consistent.
        CompletableFuture.runAsync(() -> {
            boolean flushed;
            try {
                flush.get(FLUSH_WAIT_SECONDS, TimeUnit.SECONDS);
                flushed = true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Timed out flushing profiles during reload", e);
                flushed = false;
            }
            boolean flushedFinal = flushed;
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    rebuild();
                    invalidate(flushedFinal);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to rebuild during reload", e);
                    done.completeExceptionally(e);
                } finally {
                    // Phase 6: Unlock — always runs so a reload that throws (e.g. a
                    // bad gui.yml) never leaves the plugin permanently frozen.
                    plugin.setReloading(false);
                    plugin.debug("Phase 6/6: Unlocked.");
                    plugin.getLogger().info("Reload complete.");
                    done.complete(null);
                }
            });
        }, continuationExecutor);
        return done;
    }

    private void rebuild() {
        plugin.debug("Phase 3/6: Database flushed.");

        plugin.reloadConfigSettings();

        // Load the new tags before clearing anything, so a malformed tags.yml
        // aborts with all prior state intact.
        var customTagLoader = new CustomTagLoader();
        customTagLoader.load(new File(plugin.getDataFolder(), "tags.yml"));
        var tagResolver = new TagResolver(customTagLoader);
        var entityTagResolver = new EntityTagResolver(customTagLoader);

        var oldTagResolver = plugin.getTagResolver();
        var oldEntityTagResolver = plugin.getEntityTagResolver();
        var oldCustomTagLoader = plugin.getCustomTagLoader();

        // Hold the registry write lock for the whole clear/rebuild so a concurrent
        // staged-skill validation (Jetty worker, read lock) never sees the shared
        // registries momentarily emptied mid-parse and spuriously reject valid
        // content with "unknown mechanic/trigger/state".
        java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock registryWrite = skillManager.registryLock().writeLock();
        registryWrite.lock();
        try {
            plugin.getRegistries().getEvaluatorRegistry().clear();
            plugin.getRegistries().getMechanicRegistry().clear();
            plugin.getRegistries().getTriggerRegistry().clear();

            // Rebuild the registries around the new resolvers, then parse the new
            // skills. loadSkills() swaps the skill set atomically only after every
            // file parses, so a malformed skill throws here with the previous skills
            // preserved — but if the resolvers were already committed, those surviving
            // skills would silently stop matching against the new tags. Roll the
            // resolver swap back on failure so the previous set keeps running against
            // the previous tags.
            plugin.setTagResolver(tagResolver);
            plugin.setEntityTagResolver(entityTagResolver);
            plugin.setCustomTagLoader(customTagLoader);
            plugin.registerBuiltins();
            skillManager.setTagResolver(tagResolver);
            plugin.getRequirementEngine().setTagResolver(tagResolver);
            plugin.getSkillEventListener().setTagResolver(tagResolver);
            try {
                skillManager.loadSkills(new File(plugin.getDataFolder(), "skills"));
            } catch (Exception e) {
                plugin.setTagResolver(oldTagResolver);
                plugin.setEntityTagResolver(oldEntityTagResolver);
                plugin.setCustomTagLoader(oldCustomTagLoader);
                skillManager.setTagResolver(oldTagResolver);
                plugin.getRequirementEngine().setTagResolver(oldTagResolver);
                plugin.getSkillEventListener().setTagResolver(oldTagResolver);
                // State filters capture the resolver at registration; re-register
                // them with the old resolver so gating stays consistent.
                plugin.getStateFilterRegistry().clear();
                plugin.registerBuiltinStateFilters(
                        plugin.getStateFilterRegistry(), oldTagResolver, oldEntityTagResolver);
                throw e;
            }
        } finally {
            registryWrite.unlock();
        }
        plugin.debug("Phase 4/6: Registries rebuilt.");
    }

    private void invalidate(boolean flushed) {
        io.github.chasehuegel.skilling.engine.mechanic.impl.XpBonusMechanic.clearAll();
        io.github.chasehuegel.skilling.engine.mechanic.impl.AttributeModifierHelper.clearAll();
        plugin.getSkillMenuBuilder().setGuiLayoutConfig(GuiLayoutConfig.load());
        for (PlayerProfile profile : profileManager.getAllProfiles().values()) {
            // The Phase 3 flush already persisted everything up to its snapshot
            // markers; interactions are frozen, so marking clean at the current
            // counter is safe. If the flush failed/timed out, leave profiles dirty
            // so the periodic flush retries instead of losing data.
            if (flushed) {
                profile.markSaved(profile.getModCount());
            }
            profile.invalidatePageCache();
        }
        reconcileOnlineUnlocks();
        plugin.debug("Phase 5/6: UI caches invalidated.");
    }

    /**
     * Re-runs persistent unlock mechanics (e.g. {@code core:unlock_recipe}) for
     * online players against the freshly rebuilt skill set, so a player whose
     * milestone was added or raised by a reload — or whose level was set while
     * they were online via admin commands — is caught up immediately. Runs on
     * the main thread inside the reload sequence; the idempotency guard on each
     * unlock makes already-granted unlocks no-ops.
     */
    private void reconcileOnlineUnlocks() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = profileManager.getProfile(player.getUniqueId());
            if (profile == null) continue;
            plugin.getSkillEventListener().reconcileMilestoneUnlocks(player, profile);
        }
    }
}
