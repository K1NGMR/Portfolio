package com.itemedit.light.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class SchedulerUtils {
    private static final boolean IS_FOLIA;
    private static final SchedulerProvider provider;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            // Not Folia
        }
        IS_FOLIA = folia;

        if (IS_FOLIA) {
            try {
                provider = (SchedulerProvider) Class.forName("com.itemedit.light.utils.FoliaSchedulerProvider")
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Folia scheduler provider", e);
            }
        } else {
            provider = new BukkitSchedulerProvider();
        }
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static SchedulerProvider getProvider() {
        return provider;
    }

    public static Object runTask(Plugin plugin, Runnable runnable) {
        return provider.runTask(plugin, runnable);
    }

    public static Object runTaskLater(Plugin plugin, Runnable runnable, long delay) {
        return provider.runTaskLater(plugin, runnable, delay);
    }

    public static Object runTaskTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        return provider.runTaskTimer(plugin, runnable, delay, period);
    }

    public static Object runTask(Plugin plugin, Entity entity, Runnable runnable) {
        return provider.runTask(plugin, entity, runnable);
    }

    public static Object runTaskLater(Plugin plugin, Entity entity, Runnable runnable, long delay) {
        return provider.runTaskLater(plugin, entity, runnable, delay);
    }

    public static Object runTaskTimer(Plugin plugin, Entity entity, Runnable runnable, long delay, long period) {
        return provider.runTaskTimer(plugin, entity, runnable, delay, period);
    }

    public static Object runTask(Plugin plugin, Location location, Runnable runnable) {
        return provider.runTask(plugin, location, runnable);
    }

    public static Object runTaskLater(Plugin plugin, Location location, Runnable runnable, long delay) {
        return provider.runTaskLater(plugin, location, runnable, delay);
    }

    public static Object runTaskTimer(Plugin plugin, Location location, Runnable runnable, long delay, long period) {
        return provider.runTaskTimer(plugin, location, runnable, delay, period);
    }

    public static Object runTaskAsynchronously(Plugin plugin, Runnable runnable) {
        return provider.runTaskAsynchronously(plugin, runnable);
    }

    public static Object runTaskLaterAsynchronously(Plugin plugin, Runnable runnable, long delay) {
        return provider.runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    public static Object runTaskTimerAsynchronously(Plugin plugin, Runnable runnable, long delay, long period) {
        return provider.runTaskTimerAsynchronously(plugin, runnable, delay, period);
    }

    public static void cancelTask(Object task) {
        if (task != null) {
            provider.cancelTask(task);
        }
    }
}
