package io.github.chasehuegel.skilling;

import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.datapack.DatapackRegistrar;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plugin bootstrap: discovers this plugin's bundled datapacks from the plugin
 * data folder.
 *
 * <p>On every {@code DATAPACK_DISCOVERY} pass the engine scans
 * {@code plugins/Skilling/datapacks/*.zip} and registers each pack, so the
 * server can load them. The default packs are copied there on first run by
 * {@link Skilling#onEnable} and are ordinary, admin-deletable files: an admin
 * who removes a zip simply stops it being discovered. This is a general,
 * extensible home for plugin-provided datapacks, not just the Stealth loot
 * tables.
 */
public final class SkillingBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.DATAPACK_DISCOVERY.newHandler(event ->
                        discoverBundledDatapacks(context, event.registrar())));
    }

    /**
     * Discovers every {@code *.zip} datapack in the plugin data folder.
     *
     * @param context   the bootstrap context providing the data directory and logger
     * @param registrar the datapack registrar to register packs with
     */
    private static void discoverBundledDatapacks(BootstrapContext context, DatapackRegistrar registrar) {
        Path datapacks = context.getDataDirectory().resolve("datapacks");
        if (!Files.isDirectory(datapacks)) return;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(datapacks, "*.zip")) {
            for (Path zip : stream) {
                String id = zip.getFileName().toString().replaceFirst("\\.zip$", "");
                registrar.discoverPack(zip, id, c -> c
                        .autoEnableOnServerStart(true)
                        .position(true, Datapack.Position.BOTTOM));
            }
        } catch (IOException ex) {
            context.getLogger().error("Failed to discover bundled datapacks in {}: {}", datapacks, ex);
        }
    }
}
