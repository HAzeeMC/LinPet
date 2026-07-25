package dev.sleepwithyourmom.linx.linpet.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Scheduler implementation for traditional Paper and Spigot servers.
 */
public class BukkitSchedulerAdapter implements SchedulerAdapter {
    private final JavaPlugin plugin;

    /**
     * Creates a Bukkit scheduler adapter.
     *
     * @param plugin owning plugin
     */
    public BukkitSchedulerAdapter(JavaPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public void runGlobal(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void runOnEntity(Player player, Runnable runnable) {
        if (player == null) {
            runGlobal(runnable);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public TaskHandle runAsyncTimer(Runnable runnable, long initialDelayTicks, long periodTicks) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, initialDelayTicks, periodTicks);
        return task::cancel;
    }
}
