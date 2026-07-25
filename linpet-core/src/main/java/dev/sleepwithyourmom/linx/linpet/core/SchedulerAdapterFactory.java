package dev.sleepwithyourmom.linx.linpet.core;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Detects the server scheduler surface and creates the correct adapter.
 */
public final class SchedulerAdapterFactory {
    private SchedulerAdapterFactory() {
    }

    /**
     * Creates a scheduler adapter.
     *
     * @param plugin owning plugin
     * @param enableFolia whether Folia detection is enabled
     * @return scheduler adapter
     */
    public static SchedulerAdapter create(JavaPlugin plugin, boolean enableFolia) {
        if (enableFolia && classExists("io.papermc.paper.threadedregions.RegionizedServer")) {
            plugin.getLogger().info("Detected Folia scheduler surface; using region-aware scheduler adapter.");
            return new FoliaSchedulerAdapter(plugin);
        }
        plugin.getLogger().info("Using Bukkit scheduler adapter.");
        return new BukkitSchedulerAdapter(plugin);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
