package dev.sleepwithyourmom.linx.linpet.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reflection-based Folia scheduler adapter that keeps business services platform-neutral.
 */
public class FoliaSchedulerAdapter implements SchedulerAdapter {
    private final JavaPlugin plugin;

    /**
     * Creates a Folia scheduler adapter.
     *
     * @param plugin owning plugin
     */
    public FoliaSchedulerAdapter(JavaPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable runnable) {
        Object scheduler = invokeNoArgs(Bukkit.getServer(), "getAsyncScheduler");
        invoke(scheduler, "runNow", new Class<?>[] { Plugin.class, Consumer.class }, plugin, consumer(runnable));
    }

    @Override
    public void runGlobal(Runnable runnable) {
        Object scheduler = invokeNoArgs(Bukkit.getServer(), "getGlobalRegionScheduler");
        invoke(scheduler, "run", new Class<?>[] { Plugin.class, Consumer.class }, plugin, consumer(runnable));
    }

    @Override
    public void runOnEntity(Player player, Runnable runnable) {
        if (player == null) {
            runGlobal(runnable);
            return;
        }
        Object scheduler = invokeNoArgs(player, "getScheduler");
        invoke(
            scheduler,
            "run",
            new Class<?>[] { Plugin.class, Consumer.class, Runnable.class },
            plugin,
            consumer(runnable),
            (Runnable) () -> runGlobal(runnable)
        );
    }

    @Override
    public TaskHandle runAsyncTimer(Runnable runnable, long initialDelayTicks, long periodTicks) {
        Object scheduler = invokeNoArgs(Bukkit.getServer(), "getAsyncScheduler");
        Object task = invoke(
            scheduler,
            "runAtFixedRate",
            new Class<?>[] { Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class },
            plugin,
            consumer(runnable),
            ticksToMillis(initialDelayTicks),
            ticksToMillis(periodTicks),
            TimeUnit.MILLISECONDS
        );
        return () -> invoke(task, "cancel", new Class<?>[0]);
    }

    private long ticksToMillis(long ticks) {
        if (ticks <= 0L) {
            return 50L;
        }
        if (ticks > Long.MAX_VALUE / 50L) {
            return Long.MAX_VALUE;
        }
        return ticks * 50L;
    }

    private Consumer<Object> consumer(Runnable runnable) {
        return ignored -> {
            try {
                runnable.run();
            } catch (RuntimeException ex) {
                plugin.getLogger().severe("Scheduled LinPet task failed: " + ex.getMessage());
                throw ex;
            }
        };
    }

    private Object invokeNoArgs(Object target, String methodName) {
        return invoke(target, methodName, new Class<?>[0]);
    }

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            throw new IllegalStateException("Folia scheduler method unavailable: " + methodName, ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Folia scheduler method failed: " + methodName, cause);
        }
    }
}
