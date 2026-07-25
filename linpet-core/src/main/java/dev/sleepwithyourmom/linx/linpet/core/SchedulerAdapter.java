package dev.sleepwithyourmom.linx.linpet.core;

import org.bukkit.entity.Player;

/**
 * Unified scheduler API used by Lin'Pet services.
 */
public interface SchedulerAdapter {
    /**
     * Runs work asynchronously.
     *
     * @param runnable work to run
     */
    void runAsync(Runnable runnable);

    /**
     * Runs work on the global server context.
     *
     * @param runnable work to run
     */
    void runGlobal(Runnable runnable);

    /**
     * Runs work on the player's owning region when Folia is present, or the main thread otherwise.
     *
     * @param player target player
     * @param runnable work to run
     */
    void runOnEntity(Player player, Runnable runnable);

    /**
     * Runs repeated asynchronous work.
     *
     * @param runnable work to run
     * @param initialDelayTicks initial delay in ticks
     * @param periodTicks repeat period in ticks
     * @return cancellable task handle
     */
    TaskHandle runAsyncTimer(Runnable runnable, long initialDelayTicks, long periodTicks);
}
