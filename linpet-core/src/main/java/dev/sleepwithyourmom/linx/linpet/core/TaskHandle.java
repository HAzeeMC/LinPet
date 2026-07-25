package dev.sleepwithyourmom.linx.linpet.core;

/**
 * Handle for a scheduled task that can be cancelled.
 */
@FunctionalInterface
public interface TaskHandle {
    /**
     * Cancels the scheduled task.
     */
    void cancel();
}
